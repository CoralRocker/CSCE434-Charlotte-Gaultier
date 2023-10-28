package ir.cfg;

import coco.VariableSymbol;
import ir.tac.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

class SymbolVal implements Comparable<SymbolVal>, Cloneable {

    public final String  sym; // Symbol with type
    public int instr; // Where the literal is assigned. Start at -1 and never reset. -1 indicates undefined
    public Literal val; // Null or the Literal Const Value

    private boolean same( SymbolVal o ) {
        return this.sym.equals( o.sym )
            && this.instr == o.instr
            && ( this.val == null
               ? this.val == o.val
               : this.val.equals( o.val ) );
    }

    // Merge Two Symbol Values together. Return whether the value changed.
    public boolean merge( SymbolVal other ) {
        if( !sym.equals(other.sym) )
            throw new IllegalArgumentException(String.format("%s and %s are no the same symbol values!", this, other));

        if( same(other) )
            return false;

        // Undefined + Anything = Anything
        if( instr == -1 && other.instr != -1 ) {
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

    public SymbolVal(String s, int i, Literal l) {
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
        return String.format("%s(%s:%d)", sym, val, instr);
    }

    @Override
    public int compareTo(SymbolVal symbolVal) {
        return sym.compareTo(symbolVal.sym);
    }

    public boolean isConstant() {
        return instr != -1 && val != null;
    }

    public boolean isVariable() {
        return instr != -1 && val == null;
    }

    public boolean isUndefined() {
        return instr == -1;
    }

    public boolean isTemporary() {
        return sym.charAt(0) == '_';
    }

    @Override
    public SymbolVal clone() {
        var clone = new SymbolVal(sym, instr, null);
        if( val != null )
            clone.val = val.clone();
        return clone;
    }

    @Override
    public boolean equals( Object o ) {
        if( o == null || !(o instanceof SymbolVal) )
            return false;

        return sym.equals(((SymbolVal) o).sym);
    }
}

public class GlobalConstProp extends CFGVisitor {

    protected static boolean mergeSymbolList(TreeSet<SymbolVal> dest, TreeSet<SymbolVal> src) {
        boolean changed = false;
        for( SymbolVal sym : src ) {
            if( dest.contains(sym) ) {
                // Merge into the set
                SymbolVal o = dest.subSet(sym, true, sym, true)
                                  .first();
                SymbolVal cpy = o.clone();
                boolean isChg = o.merge( sym );

                if( isChg ) {
                    System.out.printf("\tMerged %s into %s\n", sym, cpy);
                    changed = true;
                }
            }
            else {
                throw new RuntimeException(String.format("Given destination does not contain SymbolVal %s", sym));
            }
        }

        return changed;
    }

    private CFG cfg;

    public GlobalConstProp( CFG cfg, boolean do_prop, boolean do_fold ) {
        this.cfg = cfg;

        System.out.println("CFG: " + cfg.getSymbols());

        cfg.markUnvisited();
        // Set Every Block's Entry/Exit to be null for all variables
        cfg.breadthFirst((BasicBlock b) -> {
            b.entry = new TreeSet<SymbolVal>();
            b.exit = new TreeSet<SymbolVal>();

            cfg.symbols.forEach((VariableSymbol sym)->{
                ((TreeSet<SymbolVal>)b.entry).add( new SymbolVal(sym.name(), -1, null));
                ((TreeSet<SymbolVal>)b.exit).add( new SymbolVal(sym.name(), -1, null));
            });
        });

        var changed = new Object(){ boolean b = true; };
        int iters = 0;
        while( changed.b ) {
           changed.b = false;
            iters++;
            int finalIters = iters;
            cfg.breadthFirst((BasicBlock b) -> {
                System.out.printf("%2d: Processing BB%d\n", finalIters, b.getNum());

                for( BasicBlock p : b.getPredecessors() ) {
                    if( b != p ) {
                        // Merge the incoming changes from "ABOVE"
                        System.out.printf(" -> Merging BB%d -> BB%d\n", finalIters, p.getNum(), b.getNum() );
                        changed.b |= GlobalConstProp.mergeSymbolList((TreeSet<SymbolVal>) b.entry, (TreeSet<SymbolVal>) p.exit);
                    }
                }

                changed.b |= ConstantDefinedInBlock.defInBlock(b, false, do_fold);

                System.out.println();
            });

            System.out.printf("Post Iteration %2d:\n", iters);
            // System.out.println(cfg.asDotGraph());
            // System.out.println("\n");
        }

        System.out.printf("GCP Ran for %d iterations\n", iters);
        for (BasicBlock allNode : cfg.allNodes) {
            ConstantDefinedInBlock.defInBlock(allNode, do_prop, do_fold);
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
    protected HashMap<String, Literal> temporaries = new HashMap<>();

    private SymbolVal get( Assignable key ) {
        return get( new SymbolVal( key.name(), -1, null) );
    }
    private SymbolVal get( SymbolVal key ) {
        return defined.subSet(key, true, key, true).first();
    }

    protected boolean do_prop = false, do_fold = false;

    public static boolean defInBlock(BasicBlock blk, boolean do_prop, boolean do_fold) {
        ConstantDefinedInBlock visitor = new ConstantDefinedInBlock();
        visitor.do_prop = do_prop; // Whether to perform constant propagation
        visitor.do_fold = do_fold; // Whether to perform constant folding
        visitor.defined = new TreeSet<>();
        for( SymbolVal sym : (TreeSet<SymbolVal>) blk.entry ) {
            visitor.defined.add(sym.clone());
        }

        boolean changed = false;

        int ctr = -1;
        for( final TAC tac : blk.getInstructions() ) {
            ctr++;
            SymbolVal sym = tac.accept(visitor);
            if( sym != null ) {
                if( visitor.defined.contains(sym) ) {
                    // Merge into the set
                    visitor.defined.subSet(sym, true, sym, true) // Fetch the element in range [sym, sym) (so whatever is equal to sym)
                                    .first() // Get the first (and only) piece of the list
                                    .assign( sym ); // Merge in our slightly different version
                }
                else if( sym.isTemporary() ) {
                    visitor.defined.remove( sym );
                    visitor.defined.add( sym );
                }
                else {
                    throw new RuntimeException(String.format("Given destination does not contain SymbolVal %s", sym));
                }
            }

            // Must replace Assign with Store
            if( do_fold && sym != null && sym.isConstant() && tac instanceof Assign ) {
                blk.getInstructions().set(ctr, new Store(tac.getId(), ((Assign) tac).dest, sym.val ));
            }
        }

        if( blk.exit != null && ((TreeSet<SymbolVal>)blk.exit).size() == visitor.defined.size() ) {
            changed = false;
            for( SymbolVal sym : ((TreeSet<SymbolVal>)blk.exit) ) {
                SymbolVal val = visitor.get(sym);

                boolean diff;
                if( val.val == null ) {
                    diff = val.val != sym.val;
                }
                else {
                    diff = !val.val.equals(sym.val);
                }

                if( diff ) {
                    changed = true;
                    System.out.printf("\tChanged In Block: Old( %s ) -> New( %s )\n", sym, val);
                }
            }
        }

        visitor.defined.removeIf(SymbolVal::isTemporary);
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
            return new SymbolVal(((VariableSymbol) ((Variable) call.dest).getSym()).name(), call.getId(), null);
        return null;
    }

    @Override
    public SymbolVal visit(Variable var) {
        return null;
    }

    @Override
    public SymbolVal visit(Add add) {

        if( do_prop && ( add.left instanceof Variable || add.left instanceof Temporary ) ) {
            SymbolVal val = get((Assignable) add.left);
            if( val.isConstant() ) {
                add.left = val.val; // Set to Constant
            }
        }

        if( do_prop && ( add.right instanceof Variable || add.right instanceof Temporary ) ) {
            SymbolVal val = get((Assignable) add.right);
            if( val.isConstant() ) {
                add.right = val.val; // Set to Constant
            }
        }

        Literal retVal = null;

        if( do_fold && add.left.isConst() && add.right.isConst() ) {
            retVal = add.calculate();
        }

        if( add.dest instanceof Variable ) {
            return new SymbolVal(((VariableSymbol) ((Variable) add.dest).getSym()).name(), add.getId(), retVal);
        }
        else if( add.dest instanceof Temporary  ) {
            Temporary dest = (Temporary)add.dest;
            return new SymbolVal(dest.toString(), add.getId(), retVal);
        }
        else {
            return null;
        }
    }

    @Override
    public SymbolVal visit(Assign asn) {
        if( asn.dest instanceof Variable ) {
            return new SymbolVal(((VariableSymbol) ((Variable) asn.dest).getSym()).name(), asn.getId(), null);
        }
        else {
            return null;
        }
    }

    @Override
    public SymbolVal visit(Div div) {
        if( do_prop && ( div.left instanceof Variable || div.left instanceof Temporary ) ) {
            SymbolVal val = get((Assignable) div.left);
            if( val.isConstant() ) {
                div.left = val.val; // Set to Constant
            }
        }

        if( do_prop && ( div.right instanceof Variable || div.right instanceof Temporary ) ) {
            SymbolVal val = get((Assignable) div.right);
            if( val.isConstant() ) {
                div.right = val.val; // Set to Constant
            }
        }

        Literal retVal = null;

        if( do_fold && div.left.isConst() && div.right.isConst() ) {
            retVal = div.calculate();
        }

        if( div.dest instanceof Variable ) {
            return new SymbolVal(((VariableSymbol) ((Variable) div.dest).getSym()).name(), div.getId(), retVal);
        }
        else if( div.dest instanceof Temporary  ) {
            Temporary dest = (Temporary)div.dest;
            return new SymbolVal(dest.toString(), div.getId(), retVal);
        }
        else {
            return null;
        }
    }

    @Override
    public SymbolVal visit(Mod mod) {
        if( do_prop && ( mod.left instanceof Variable || mod.left instanceof Temporary ) ) {
            SymbolVal val = get((Assignable) mod.left);
            if( val.isConstant() ) {
                mod.left = val.val; // Set to Constant
            }
        }

        if( do_prop && ( mod.right instanceof Variable || mod.right instanceof Temporary ) ) {
            SymbolVal val = get((Assignable) mod.right);
            if( val.isConstant() ) {
                mod.right = val.val; // Set to Constant
            }
        }

        Literal retVal = null;

        if( do_fold && mod.left.isConst() && mod.right.isConst() ) {
            retVal = mod.calculate();
        }

        if( mod.dest instanceof Variable ) {
            return new SymbolVal(((VariableSymbol) ((Variable) mod.dest).getSym()).name(), mod.getId(), retVal);
        }
        else if( mod.dest instanceof Temporary  ) {
            Temporary dest = (Temporary)mod.dest;
            return new SymbolVal(dest.toString(), mod.getId(), retVal);
        }
        else {
            return null;
        }
    }

    @Override
    public SymbolVal visit(Mul mul) {
        if( do_prop && ( mul.left instanceof Variable || mul.left instanceof Temporary ) ) {
            SymbolVal val = get((Assignable) mul.left);
            if( val.isConstant() ) {
                mul.left = val.val; // Set to Constant
            }
        }

        if( do_prop && ( mul.right instanceof Variable || mul.right instanceof Temporary ) ) {
            SymbolVal val = get((Assignable) mul.right);
            if( val.isConstant() ) {
                mul.right = val.val; // Set to Constant
            }
        }

        Literal retVal = null;

        if( do_fold && mul.left.isConst() && mul.right.isConst() ) {
            retVal = mul.calculate();
        }

        if( mul.dest instanceof Variable ) {
            return new SymbolVal(((VariableSymbol) ((Variable) mul.dest).getSym()).name(), mul.getId(), retVal);
        }
        else if( mul.dest instanceof Temporary  ) {
            Temporary dest = (Temporary)mul.dest;
            return new SymbolVal(dest.toString(), mul.getId(), retVal);
        }
        else {
            return null;
        }
    }

    @Override
    public SymbolVal visit(Sub sub) {
        if( do_prop && ( sub.left instanceof Variable || sub.left instanceof Temporary ) ) {
            SymbolVal val = get((Assignable) sub.left);
            if( val.isConstant() ) {
                sub.left = val.val; // Set to Constant
            }
        }

        if( do_prop && ( sub.right instanceof Variable || sub.right instanceof Temporary ) ) {
            SymbolVal val = get((Assignable) sub.right);
            if( val.isConstant() ) {
                sub.right = val.val; // Set to Constant
            }
        }

        Literal retVal = null;

        if( do_fold && sub.left.isConst() && sub.right.isConst() ) {
            retVal = sub.calculate();
        }

        if( sub.dest instanceof Variable ) {
            return new SymbolVal(((VariableSymbol) ((Variable) sub.dest).getSym()).name(), sub.getId(), retVal);
        }
        else if( sub.dest instanceof Temporary  ) {
            Temporary dest = (Temporary)sub.dest;
            return new SymbolVal(dest.toString(), sub.getId(), retVal);
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
        if( do_prop && ( cmp.left instanceof Variable || cmp.left instanceof Temporary ) ) {
            SymbolVal val = get((Assignable) cmp.left);
            if( val.isConstant() ) {
                cmp.left = val.val; // Set to Constant
            }
        }

        if( do_prop && ( cmp.right instanceof Variable || cmp.right instanceof Temporary ) ) {
            SymbolVal val = get((Assignable) cmp.right);
            if( val.isConstant() ) {
                cmp.right = val.val; // Set to Constant
            }
        }

        Literal retVal = null;

        if( do_fold && cmp.left.isConst() && cmp.right.isConst() ) {
            retVal = cmp.calculate();
        }

        if( cmp.dest instanceof Variable ) {
            return new SymbolVal(((VariableSymbol) ((Variable) cmp.dest).getSym()).name(), cmp.getId(), retVal);
        }
        else if( cmp.dest instanceof Temporary  ) {
            Temporary dest = (Temporary)cmp.dest;
            return new SymbolVal(dest.toString(), cmp.getId(), retVal);
        }
        else {
            return null;
        }
    }

    @Override
    public SymbolVal visit(Store store) {
        if( store.source instanceof Literal ) {
            return new SymbolVal(store.dest.name(), store.getId(), (Literal) store.source);
        }
        else {
            return new SymbolVal(store.dest.name(), store.getId(), null);
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
