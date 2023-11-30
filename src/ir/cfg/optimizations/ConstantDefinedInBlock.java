package ir.cfg.optimizations;

import coco.Symbol;
import coco.VariableSymbol;
import ir.cfg.BasicBlock;
import ir.tac.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

// Perform Constant Propagation Within A Basic Block
// Also perform copy propagation
public class ConstantDefinedInBlock implements TACVisitor<SymbolVal> {

    protected HashMap<SymbolVal, SymbolVal> defined = new HashMap<>();
    protected HashMap<String, Literal> temporaries = new HashMap<>();

    private SymbolVal get(Assignable key) {
        return get(new SymbolVal(key.name(), -1));
    }

    private SymbolVal get(SymbolVal key) {
        SymbolVal smv = defined.get(key);
        if( smv == null ) {
            throw new RuntimeException(String.format("%s does not exist", key));
        }
        return smv;
    }

    protected boolean do_prop = false, do_fold = false, do_copy_prop = false;

    public static boolean defInBlock(BasicBlock blk, boolean do_prop, boolean do_fold, boolean do_copy_prop, boolean do_branch_dce, boolean do_print) {
        ConstantDefinedInBlock visitor = new ConstantDefinedInBlock();
        visitor.do_prop = do_prop; // Whether to perform constant propagation
        visitor.do_fold = do_fold; // Whether to perform constant folding
        visitor.do_copy_prop = do_copy_prop;
        visitor.defined = new HashMap<>();
        for (SymbolVal sym : ((HashMap<SymbolVal, SymbolVal>) blk.entry).values() ) {
            SymbolVal cpy = sym.clone();
            visitor.defined.put(cpy, cpy);
        }

        boolean changed = false;

        int ctr = -1;
        var iter = blk.getInstructions().listIterator();
        while( iter.hasNext() ) {
            TAC tac = iter.next();
            ctr++;
            SymbolVal sym = tac.accept(visitor);
            if (sym != null && !sym.isUndefined()) { // Sym is only undefined if returned as marker
                if (visitor.defined.containsKey(sym)) {
                    // Merge into the set
                    visitor.defined.get(sym).assign(sym); // Merge in our slightly different version
                } else if (sym.isTemporary()) {
                    visitor.defined.remove(sym);
                    visitor.defined.put(sym, sym);
                } else {
                    throw new RuntimeException(String.format("Given destination does not contain SymbolVal %s (from #%d %s)", sym, tac.getId(), tac.genDot()));
                }

            }


            // Must replace Assign with Store
            if (do_fold && sym != null ){
                if( sym.isConstant() && tac instanceof Assign){
                    Store str = new Store(tac.getIdObj(), ((Assign)tac).dest, sym.val);
                    if( do_print )
                        System.out.printf("Setting BB%d::%d  %s to %s\n", blk.getNum(), ctr, tac, str);
                    blk.getInstructions().set(ctr, str);
                    changed = true;
                }
            }
            if( do_branch_dce && sym != null && tac instanceof Branch ) {
                SymbolVal cnst = visitor.get( sym );
                if( cnst.isConstant() ) {
                    int v = cnst.val.getInt();
                    var br = ((Branch) tac).calculate(v);
                    List<BasicBlock> blks = blk.getSuccessors();
                    BasicBlock dest, other;
                    dest = ((Branch)tac).getJumpTo();
                    if( blks.size() == 2 ) {
                        other = (blks.get(0) == dest) ? blks.get(1) : blks.get(0);
                    }
                    else {
                        other = null;
                    }

                    BasicBlock selected = (br) ? dest : other;

                    if( br && other != null ) { // Other is Removed
                        // Remove From Successors
                        if( do_print ) {
                            System.out.printf("Disconnecting %s from %s\n", other, blk);
                        }
                        blk.disconnectAfter(other);
                        // blks.remove(other);
                        // other.getPredecessors().remove(blk);

                        ((Branch) tac).setVal(null);
                        ((Branch) tac).setRel("");

                        TAC nbranch = iter.next();
                        iter.remove();
                        nbranch.getIdObj().remove();

                    }
                    else if( other != null ) { // Dest is removed
                        if( do_print ) {
                            System.out.printf("Disconnecting %s from %s\n", dest, blk);
                        }
                        blk.disconnectAfter(dest);
                        // blks.remove( dest );
                        // dest.getPredecessors().remove(blk);

                        // Remove branch operation
                        iter.remove();
                        tac.getIdObj().remove();

                        changed = true;
                        continue;
                    }

                }
            }

            if( do_copy_prop && sym != null && sym.isCopied() && tac instanceof Store ) {
                SymbolVal cpy = visitor.get( sym.copy );
                if( cpy.isCopied() ) {
                    blk.getInstructions().set(ctr, new Store(tac.getIdObj(), ((Store) tac).dest, cpy.copy));
                    changed = true;
                }
            }
        }

        if (blk.exit != null && ((HashMap<SymbolVal, SymbolVal>) blk.exit).size() == visitor.defined.size()) {
            for (SymbolVal sym : ((HashMap<SymbolVal, SymbolVal>) blk.exit).values() ) {
                SymbolVal val = visitor.get(sym);

                boolean diff;
                if (val.val == null) {
                    diff = val.val != sym.val;
                } else {
                    diff = !val.val.equals(sym.val);
                }

                if (diff) {
                    changed = true;
                    if (do_print)
                        System.out.printf("\tChanged In Block: Old( %s ) -> New( %s )\n", sym, val);
                }
            }
        }

        // visitor.defined.keySet().removeIf(SymbolVal::isTemporary);
        blk.exit = visitor.defined;
        if( changed && do_print && (do_copy_prop || do_prop) ) {
            System.err.printf("CFG was changed by ConstDefInBlock for BB%d\n", blk.getNum());
        }
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
        if (call.dest instanceof Variable)
            return new SymbolVal(((VariableSymbol) ((Variable) call.dest).getSym()).name(), call.getId() );
        return null;
    }

    @Override
    public SymbolVal visit(Variable var) {
        return null;
    }

    @Override
    public SymbolVal visit(Add add) {
        return visit((Assign) add);
    }

    @Override
    public SymbolVal visit(Assign asn) {

        if( asn.left instanceof Assignable ) {
            boolean made_const = false;
            if ( do_prop ) {
                SymbolVal val = get((Assignable) asn.left);
                if (val.isConstant()) {
                    asn.left = val.val; // Set to Constant
                    made_const = true;
                }
            }
            if( do_copy_prop && !made_const ) {
                SymbolVal cpy = get((Assignable) asn.left);
                if( cpy != null && cpy.isCopied() ) {
                    asn.left = cpy.copy;
                }
            }
        }

        if ( asn.right instanceof Assignable ) {
            boolean made_const = false;
            if (do_prop ) {
                SymbolVal val = get((Assignable) asn.right);
                if (val.isConstant()) {
                    asn.right = val.val; // Set to Constant
                    made_const = true;
                }
            }
            if( do_copy_prop && !made_const ) {
                SymbolVal cpy = get((Assignable) asn.right);
                if( cpy != null && cpy.isCopied() ) {
                    asn.right = cpy.copy;
                }
            }
        }

        Literal retVal = null;

        if (do_fold && asn.left.isConst() && asn.right.isConst()) {
            retVal = asn.calculate();
        }

        if (asn.dest instanceof Variable) {
            return new SymbolVal(((VariableSymbol) ((Variable) asn.dest).getSym()).name(), asn.getId(), retVal);
        } else if (asn.dest instanceof Temporary) {
            Temporary dest = (Temporary) asn.dest;
            return new SymbolVal(dest.toString(), asn.getId(), retVal);
        } else {
            return null;
        }
    }

    @Override
    public SymbolVal visit(Div div) {
        return visit((Assign) div);
    }

    @Override
    public SymbolVal visit(Mod mod) {
        return visit((Assign) mod);
    }

    @Override
    public SymbolVal visit(Mul mul) {
        return visit((Assign) mul);
    }

    @Override
    public SymbolVal visit(Sub sub) {
        return visit((Assign) sub);
    }

    @Override
    public SymbolVal visit(LoadStack lstack) {
        return null;
    }

    @Override
    public SymbolVal visit(Branch bra) {
        if( bra.getVal() == null ) return null;

        return new SymbolVal(((Assignable)bra.getVal()).name(), -1 ); // Return the symbol as undefined
    }

    @Override
    public SymbolVal visit(Cmp cmp) {
        return visit((Assign) cmp);
    }

    @Override
    public SymbolVal visit(Store store) {
        if (store.source instanceof Literal) {
            return new SymbolVal( store.dest.name(), store.getId(), (Literal) store.source);
        }
        else if ( store.source instanceof Assignable ) {
            if( do_prop ) {
                SymbolVal constprop = get((Assignable) store.source);
                if( constprop.isConstant() ) {
                    store.source = constprop.val;
                    return  new SymbolVal( store.dest.name(), store.getId(), (Literal) store.source);
                }
            }
            return  new SymbolVal( store.dest.name(), store.getId(), (Assignable) store.source);
        }
        else {
            return new SymbolVal( store.dest.name(), store.getId() );
        }
    }

    @Override
    public SymbolVal visit(StoreStack sstack) {
        return null;
    }

    @Override
    public SymbolVal visit(Phi phi) {
        return null;
    }

    @Override
    public SymbolVal visit(Pow pow) {
        return visit((Assign) pow);
    }

    @Override
    public SymbolVal visit(Temporary temporary) {
        return null;
    }

    @Override
    public SymbolVal visit(Not not) {
        if (not.getSrc() instanceof Literal) {
            return new SymbolVal( not.dest.name(), not.getId(), (Literal) not.getSrc());
        }
        else if ( not.getSrc() instanceof Assignable ) {
            if( do_prop ) {
                SymbolVal constprop = get((Assignable) not.getSrc());
                if( constprop.isConstant() ) {
                    not.src = constprop.val;
                    return  new SymbolVal( not.dest.name(), not.getId(), (Literal) not.src);
                }
            }
            return  new SymbolVal( not.dest.name(), not.getId(), (Assignable) not.src);
        }
        else {
            return new SymbolVal( not.dest.name(), not.getId() );
        }
    }

    @Override
    public SymbolVal visit(And and) {
        return visit((Assign) and);
    }

    @Override
    public SymbolVal visit(Or or) {
        return visit((Assign) or);
    }
}
