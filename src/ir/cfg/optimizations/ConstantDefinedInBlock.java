package ir.cfg.optimizations;

import coco.Symbol;
import coco.VariableSymbol;
import ir.cfg.BasicBlock;
import ir.tac.*;

import java.util.HashMap;
import java.util.TreeSet;

// Perform Constant Propagation Within A Basic Block
// Also perform copy propagation
public class ConstantDefinedInBlock extends TACVisitor<SymbolVal> {

    protected TreeSet<SymbolVal> defined = new TreeSet<>();
    protected HashMap<String, Literal> temporaries = new HashMap<>();

    private SymbolVal get(Assignable key) {
        return get(new SymbolVal(key.name(), -1));
    }

    private SymbolVal get(SymbolVal key) {
        return defined.subSet(key, true, key, true).first();
    }

    protected boolean do_prop = false, do_fold = false, do_copy_prop = false;

    public static boolean defInBlock(BasicBlock blk, boolean do_prop, boolean do_fold, boolean do_copy_prop, boolean do_print) {
        ConstantDefinedInBlock visitor = new ConstantDefinedInBlock();
        visitor.do_prop = do_prop; // Whether to perform constant propagation
        visitor.do_fold = do_fold; // Whether to perform constant folding
        visitor.do_copy_prop = do_copy_prop;
        visitor.defined = new TreeSet<>();
        for (SymbolVal sym : (TreeSet<SymbolVal>) blk.entry) {
            visitor.defined.add(sym.clone());
        }

        boolean changed = false;

        int ctr = -1;
        for (final TAC tac : blk.getInstructions()) {
            ctr++;
            SymbolVal sym = tac.accept(visitor);
            if (sym != null) {
                if (visitor.defined.contains(sym)) {
                    // Merge into the set
                    visitor.defined.subSet(sym, true, sym, true) // Fetch the element in range [sym, sym) (so whatever is equal to sym)
                            .first() // Get the first (and only) piece of the list
                            .assign(sym); // Merge in our slightly different version
                } else if (sym.isTemporary()) {
                    visitor.defined.remove(sym);
                    visitor.defined.add(sym);
                } else {
                    throw new RuntimeException(String.format("Given destination does not contain SymbolVal %s", sym));
                }
            }

            // Must replace Assign with Store
            if (do_fold && sym != null && sym.isConstant() && tac instanceof Assign) {
                blk.getInstructions().set(ctr, new Store(tac.getId(), ((Assign) tac).dest, sym.val));
            }

            if( do_copy_prop && sym != null && sym.isCopied() && tac instanceof Store ) {
                SymbolVal cpy = visitor.get( sym.copy );
                if( cpy.isCopied() ) {
                    blk.getInstructions().set(ctr, new Store(tac.getId(), ((Store) tac).dest, cpy.copy));
                }
            }
        }

        if (blk.exit != null && ((TreeSet<SymbolVal>) blk.exit).size() == visitor.defined.size()) {
            changed = false;
            for (SymbolVal sym : ((TreeSet<SymbolVal>) blk.exit)) {
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
            if ( do_prop ) {
                SymbolVal val = get((Assignable) asn.left);
                if (val.isConstant()) {
                    asn.left = val.val; // Set to Constant
                }
            }
            if( do_copy_prop ) {
                SymbolVal cpy = get((Assignable) asn.left);
                if( cpy != null && cpy.isCopied() ) {
                    asn.left = cpy.copy;
                }
            }
        }

        if ( asn.right instanceof Assignable ) {
            if (do_prop ) {
                SymbolVal val = get((Assignable) asn.right);
                if (val.isConstant()) {
                    asn.right = val.val; // Set to Constant
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
    public SymbolVal visit(Branch bra) {
        return null;
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
            return  new SymbolVal( store.dest.name(), store.getId(), (Assignable) store.source);
        }
        else {
            return new SymbolVal( store.dest.name(), store.getId() );
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
