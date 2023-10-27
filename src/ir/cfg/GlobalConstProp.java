package ir.cfg;

import coco.Symbol;
import coco.VariableSymbol;
import ir.tac.*;

import java.util.TreeSet;

class SymbolVal implements Comparable<SymbolVal>, Cloneable {

    public final VariableSymbol sym; // Symbol with type
    public int instr; // Where the literal is assigned. Start at -1 and never reset. -1 indicates undefined
    public Literal val; // Null or the Literal Const Value

    private boolean same( SymbolVal o ) {
        return this.sym == o.sym
            && this.instr == o.instr
            && this.val == o.val;
    }

    // Merge Two Symbol Values together. Return whether the value changed.
    public boolean merge( SymbolVal other ) {
        if( !sym.equals(other.sym) )
            throw new IllegalArgumentException(String.format("%s and %s are no the same symbol values!", this, other));

        if( same(other) )
            return false;

        // Undefined + Anything = Anything
        if( instr == -1 ) {
            instr = other.instr;
            val = other.val;
            return true;
        }
        else if( other.instr == -1 ) {
            return false;
        }

        // Not Const + Anything = Not Const
        if( val == null || other.val == null ) {
            boolean changed = val != null;
            val = null;
            if( changed )
                instr = other.instr;
            return changed;
        }


        // If constants are not equal, not constant
        if( !val.equals(other.val) ) {
            val = null;
            instr = 0;
            return true;
        }

        return false;
    }

    public SymbolVal(VariableSymbol s, int i, Literal l) {
        sym = s;
        instr = i;
        val = l;
    }

    public boolean assign( SymbolVal other ) {
        if( !sym.equals( other.sym ) )
            throw new IllegalArgumentException(String.format("%s and %s are no the same symbol values!", this, other));


        instr = other.instr;
        boolean changed;
        if( val != null ) {
            changed = val.equals(other.val);
        }
        else {
            changed = val != other.val;
        }
        val = other.val;
        return changed;
    }

    @Override
    public String toString() {
        return String.format("%s(%s:%d)", sym.name(), val, instr);
    }

    @Override
    public int compareTo(SymbolVal symbolVal) {
        return sym.name().compareTo(symbolVal.sym.name());
    }

    public boolean isConstant() {
        return instr != -1 && val != null;
    }

    @Override
    public SymbolVal clone() {
        var clone = new SymbolVal(sym.clone(), instr, null);
        if( val != null )
            clone.val = val.clone();
        return clone;
    }

    @Override
    public boolean equals( Object o ) {
        if( o == null || !(o instanceof SymbolVal) )
            return false;

        return sym.equals((SymbolVal) o);
    }
}

public class GlobalConstProp extends CFGVisitor {

    protected static boolean mergeSymbolList(TreeSet<SymbolVal> dest, TreeSet<SymbolVal> src) {
        boolean changed = false;
        for( SymbolVal sym : src ) {
            if( dest.contains(sym) ) {
                // Merge into the set
                changed |= dest.subSet(sym, true, sym, true)
                                .first()
                                .merge( sym );
            }
            else {
                throw new RuntimeException(String.format("Given destination does not contain SymbolVal %s", sym));
            }
        }

        return changed;
    }

    private CFG cfg;

    public GlobalConstProp( CFG cfg ) {
        this.cfg = cfg;

        System.out.println("CFG: " + cfg.getSymbols());

        cfg.markUnvisited();
        // Set Every Block's Entry/Exit to be null for all variables
        cfg.breadthFirst((BasicBlock b) -> {
            b.entry = new TreeSet<SymbolVal>();
            b.exit = new TreeSet<SymbolVal>();

            cfg.symbols.forEach((VariableSymbol sym)->{
                ((TreeSet<SymbolVal>)b.entry).add( new SymbolVal(sym, -1, null));
                ((TreeSet<SymbolVal>)b.exit).add( new SymbolVal(sym, -1, null));
            });
        });

        var changed = new Object(){ boolean b = true; };
        int iters = 0;
        while( changed.b ) {
            changed.b = false;
            iters++;
            cfg.breadthFirst((BasicBlock b) -> {
                for( BasicBlock p : b.getPredecessors() ) {
                    if( b != p ) {
                        // Merge the incoming changes from "ABOVE"
                        changed.b |= GlobalConstProp.mergeSymbolList((TreeSet<SymbolVal>) b.entry, (TreeSet<SymbolVal>) p.exit);
                    }
                }

                changed.b |= ConstantDefinedInBlock.defInBlock(b, false);

            });

            // System.out.printf("Post Iteration %2d:\n", iters);
            // System.out.println(cfg.asDotGraph());
            // System.out.println("\n");
        }

        System.out.printf("GCP Ran for %d iterations\n", iters);
        for (BasicBlock allNode : cfg.allNodes) {
            ConstantDefinedInBlock.defInBlock(allNode, true);
        }

    }

    @Override
    public Object visit(BasicBlock blk) {
        return null;
    }
}

// Perform Constant Propagation Within A Basic Block
class ConstantDefinedInBlock extends TACVisitor<SymbolVal> {

    protected TreeSet<SymbolVal> defined = new TreeSet<>();

    private SymbolVal get( Variable key ) {
        return get( new SymbolVal((VariableSymbol) key.getSym(), -1, null) );
    }
    private SymbolVal get( SymbolVal key ) {
        return defined.subSet(key, true, key, true).first();
    }

    protected boolean modify = false;

    public static boolean defInBlock(BasicBlock blk, boolean mod) {
        ConstantDefinedInBlock visitor = new ConstantDefinedInBlock();
        visitor.modify = mod; // Whether to change the actual code
        visitor.defined = new TreeSet<>();
        for( SymbolVal sym : (TreeSet<SymbolVal>) blk.entry ) {
            visitor.defined.add(sym.clone());
        }

        boolean changed = false;

        for( TAC tac : blk.getInstructions() ) {
            SymbolVal sym = tac.accept(visitor);
            if( sym != null ) {
                if( visitor.defined.contains(sym) ) {
                    // Merge into the set
                    visitor.defined.subSet(sym, true, sym, true) // Fetch the element in range [sym, sym) (so whatever is equal to sym)
                                    .first() // Get the first (and only) piece of the list
                                    .merge( sym ); // Merge in our slightly different version
                }
                else {
                    throw new RuntimeException(String.format("Given destination does not contain SymbolVal %s", sym));
                }
            }
        }

        if( blk.exit != null && ((TreeSet<SymbolVal>)blk.exit).size() == visitor.defined.size() ) {
            changed = false;
            for( SymbolVal sym : ((TreeSet<SymbolVal>)blk.exit) ) {
                SymbolVal val = visitor.get(sym);

                changed |= val.val != sym.val;
            }
        }
        blk.exit = visitor.defined;
        return changed;
    }

    @Override
    public SymbolVal visit(Return ret) {
        return null;
    }

    @Override
    public SymbolVal visit(Literal lit) {
        return null;
    }

    @Override
    public SymbolVal visit(Call call) {
        if( call.dest instanceof Variable )
            return new SymbolVal((VariableSymbol) ((Variable) call.dest).getSym(), call.getId(), null);
        return null;
    }

    @Override
    public SymbolVal visit(Variable var) {
        return null;
    }

    @Override
    public SymbolVal visit(Add add) {
        if( modify && add.left instanceof Variable ) {
            SymbolVal val = get((Variable) add.left);
            if( val.isConstant() ) {
                add.left = val.val; // Set to Constant
            }
        }

        if( modify && add.right instanceof Variable ) {
            SymbolVal val = get((Variable) add.right);
            if( val.isConstant() ) {
                add.right = val.val; // Set to Constant
            }
        }
        if( add.dest instanceof Variable ) {
            return new SymbolVal((VariableSymbol) ((Variable) add.dest).getSym(), add.getId(), null);
        }
        else {
            return null;
        }
    }

    @Override
    public SymbolVal visit(Assign asn) {
        if( asn.dest instanceof Variable ) {
            return new SymbolVal((VariableSymbol) ((Variable) asn.dest).getSym(), asn.getId(), null);
        }
        else {
            return null;
        }
    }

    @Override
    public SymbolVal visit(Div div) {
        if( modify && div.left instanceof Variable ) {
            SymbolVal val = get((Variable) div.left);
            if( val.isConstant() ) {
                div.left = val.val; // Set to Constant
            }
        }

        if( modify && div.right instanceof Variable ) {
            SymbolVal val = get((Variable) div.right);
            if( val.isConstant() ) {
                div.right = val.val; // Set to Constant
            }
        }
        if( div.dest instanceof Variable ) {
            return new SymbolVal((VariableSymbol) ((Variable) div.dest).getSym(), div.getId(), null);
        }
        else {
            return null;
        }
    }

    @Override
    public SymbolVal visit(Mod mod) {
        if( modify && mod.left instanceof Variable ) {
            SymbolVal val = get((Variable) mod.left);
            if( val.isConstant() ) {
                mod.left = val.val; // Set to Constant
            }
        }

        if(modify &&  mod.right instanceof Variable ) {
            SymbolVal val = get((Variable) mod.right);
            if( val.isConstant() ) {
                mod.right = val.val; // Set to Constant
            }
        }
        if( mod.dest instanceof Variable ) {
            return new SymbolVal((VariableSymbol) ((Variable) mod.dest).getSym(), mod.getId(), null);
        }
        else {
            return null;
        }
    }

    @Override
    public SymbolVal visit(Mul mul) {
        if( modify && mul.left instanceof Variable ) {
            SymbolVal val = get((Variable) mul.left);
            if( val.isConstant() ) {
                mul.left = val.val; // Set to Constant
            }
        }

        if( modify && mul.right instanceof Variable ) {
            SymbolVal val = get((Variable) mul.right);
            if( val.isConstant() ) {
                mul.right = val.val; // Set to Constant
            }
        }
        if( mul.dest instanceof Variable ) {
            return new SymbolVal((VariableSymbol) ((Variable) mul.dest).getSym(), mul.getId(), null);
        }
        else {
            return null;
        }
    }

    @Override
    public SymbolVal visit(Sub sub) {
        if( modify && sub.left instanceof Variable ) {
            SymbolVal val = get((Variable) sub.left);
            if( val.isConstant() ) {
                sub.left = val.val; // Set to Constant
            }
        }

        if( modify && sub.right instanceof Variable ) {
            SymbolVal val = get((Variable) sub.right);
            if( val.isConstant() ) {
                sub.right = val.val; // Set to Constant
            }
        }
        if( sub.dest instanceof Variable ) {
            return new SymbolVal((VariableSymbol) ((Variable) sub.dest).getSym(), sub.getId(), null);
        }
        else {
            return null;
        }
    }

    @Override
    public SymbolVal visit(Branch bra) {
        return null;
    }

    @Override
    public SymbolVal visit(Cmp cmp) {
        if( modify && cmp.left instanceof Variable ) {
            SymbolVal val = get((Variable) cmp.left);
            if( val.isConstant() ) {
                cmp.left = val.val; // Set to Constant
            }
        }

        if( modify && cmp.right instanceof Variable ) {
            SymbolVal val = get((Variable) cmp.right);
            if( val.isConstant() ) {
                cmp.right = val.val; // Set to Constant
            }
        }
        if( cmp.dest instanceof Variable ) {
            return new SymbolVal((VariableSymbol) ((Variable) cmp.dest).getSym(), cmp.getId(), null);
        }
        else {
            return null;
        }
    }

    @Override
    public SymbolVal visit(Store store) {
        if( store.source instanceof Literal ) {
            return new SymbolVal((VariableSymbol) store.dest.getSym(), store.getId(), (Literal) store.source);
        }
        else {
            return new SymbolVal((VariableSymbol) store.dest.getSym(), store.getId(), null);
        }
    }

    @Override
    public SymbolVal visit(Phi phi) {
        return null;
    }

    @Override
    public SymbolVal visit(Temporary temporary) {
        return null;
    }
}
