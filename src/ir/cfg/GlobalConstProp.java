package ir.cfg;

import coco.Symbol;
import coco.VariableSymbol;
import ir.tac.*;

import java.util.TreeSet;

class SymbolVal implements Comparable<SymbolVal> {

    public final VariableSymbol sym; // Symbol with type
    public int instr; // Where the literal is assigned. Start at -1 and never reset. -1 indicates undefined
    public Literal val; // Null or the Literal Const Value

    // Merge Two Symbol Values together. Return whether the value changed.
    public boolean merge( SymbolVal other ) {
        if( sym != other.sym )
            throw new IllegalArgumentException(String.format("%s and %s are no the same symbol values!", this, other));

        // Not Const + Anything = Not Const
        if( val == null || other.val == null ) {
            boolean changed = val != null;
            val = null;
            return changed;
        }

        // Undefined + Anything = Anything
        if( instr == -1 ) {
            instr = other.instr;
            val = other.val;
            return true;
        }
        else if( other.instr == -1 ) {
            return false;
        }

        // If constants are not equal, not constant
        if( !val.equals(other.val) ) {
            val = null;
            return true;
        }

        return false;
    }

    public SymbolVal(VariableSymbol s, int i, Literal l) {
        sym = s;
        instr = i;
        val = l;
    }

    public void assign( SymbolVal other ) {
        if( sym != other.sym )
            throw new IllegalArgumentException(String.format("%s and %s are no the same symbol values!", this, other));

        instr = other.instr;
        val = other.val;
    }

    @Override
    public String toString() {
        return String.format("%s:%d", val, instr);
    }

    @Override
    public int compareTo(SymbolVal symbolVal) {
        return sym.name().compareTo(symbolVal.sym.name());
    }

    public boolean isConstant() {
        return instr != -1 && val != null;
    }
}

public class GlobalConstProp extends CFGVisitor {

    protected static boolean mergeSymbolList(TreeSet<SymbolVal> dest, TreeSet<SymbolVal> src) {
        boolean changed = false;
        for( SymbolVal sym : src ) {
            if( dest.contains(sym) ) {
                // Merge into the set
                changed |= dest.subSet(sym,sym)
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

        boolean changed = true;
        while( changed ) {
            changed = false;

            cfg.breadthFirst((BasicBlock b) -> {
                for( BasicBlock p : b.getPredecessors() ) {
                    if( b != p ) {
                        // Merge the incoming changes from "ABOVE"
                        GlobalConstProp.mergeSymbolList((TreeSet<SymbolVal>) b.entry, (TreeSet<SymbolVal>) p.exit);
                    }
                }

                TreeSet<SymbolVal> set = ConstantDefinedInBlock.defInBlock(b);

            });

        }
    }

    @Override
    public Object visit(BasicBlock blk) {
        return null;
    }
}
class ConstantDefinedInBlock extends TACVisitor<SymbolVal> {

    protected TreeSet<SymbolVal> defined = new TreeSet<>();

    private SymbolVal get( Variable key ) {
        return get( new SymbolVal((VariableSymbol) key.getSym(), -1, null) );
    }
    private SymbolVal get( SymbolVal key ) {
        return defined.subSet(key, true, key, true).first();
    }

    public static TreeSet<SymbolVal> defInBlock(BasicBlock blk) {
        ConstantDefinedInBlock visitor = new ConstantDefinedInBlock();
        visitor.defined = (TreeSet<SymbolVal>) ((TreeSet<SymbolVal>) blk.entry).clone();

        for( TAC tac : blk.getInstructions() ) {
            SymbolVal sym = tac.accept(visitor);
            if( sym != null ) {
                if( visitor.defined.contains(sym) ) {
                    // Merge into the set
                    visitor.defined.subSet(sym, true, sym, true) // Fetch the element in range [sym, sym) (so whatever is equal to sym)
                            .first() // Get the first (and only) piece of the list
                            .assign( sym ); // Merge in our slightly different version
                }
                else {
                    throw new RuntimeException(String.format("Given destination does not contain SymbolVal %s", sym));
                }
            }
        }

        blk.exit = visitor.defined;
        return visitor.defined;
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
        if( add.left instanceof Variable ) {
            SymbolVal val = get((Variable) add.left);
            if( val.isConstant() ) {
                add.left = val.val; // Set to Constant
            }
        }

        if( add.right instanceof Variable ) {
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
        if( div.left instanceof Variable ) {
            SymbolVal val = get((Variable) div.left);
            if( val.isConstant() ) {
                div.left = val.val; // Set to Constant
            }
        }

        if( div.right instanceof Variable ) {
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
        if( mod.left instanceof Variable ) {
            SymbolVal val = get((Variable) mod.left);
            if( val.isConstant() ) {
                mod.left = val.val; // Set to Constant
            }
        }

        if( mod.right instanceof Variable ) {
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
        if( mul.left instanceof Variable ) {
            SymbolVal val = get((Variable) mul.left);
            if( val.isConstant() ) {
                mul.left = val.val; // Set to Constant
            }
        }

        if( mul.right instanceof Variable ) {
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
        if( sub.left instanceof Variable ) {
            SymbolVal val = get((Variable) sub.left);
            if( val.isConstant() ) {
                sub.left = val.val; // Set to Constant
            }
        }

        if( sub.right instanceof Variable ) {
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
        if( cmp.left instanceof Variable ) {
            SymbolVal val = get((Variable) cmp.left);
            if( val.isConstant() ) {
                cmp.left = val.val; // Set to Constant
            }
        }

        if( cmp.right instanceof Variable ) {
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
