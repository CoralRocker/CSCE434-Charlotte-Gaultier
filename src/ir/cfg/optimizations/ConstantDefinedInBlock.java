package ir.cfg.optimizations;

import coco.VariableSymbol;
import ir.cfg.BasicBlock;
import ir.tac.*;

import java.util.HashMap;
import java.util.TreeSet;

// Perform Constant Propagation Within A Basic Block
public class ConstantDefinedInBlock extends TACVisitor<SymbolVal> {

    protected TreeSet<SymbolVal> defined = new TreeSet<>();
    protected HashMap<String, Literal> temporaries = new HashMap<>();

    private SymbolVal get(Assignable key) {
        return get(new SymbolVal(key.name(), -1, null));
    }

    private SymbolVal get(SymbolVal key) {
        return defined.subSet(key, true, key, true).first();
    }

    protected boolean do_prop = false, do_fold = false;

    public static boolean defInBlock(BasicBlock blk, boolean do_prop, boolean do_fold, boolean do_copy_prop, boolean do_print) {
        ConstantDefinedInBlock visitor = new ConstantDefinedInBlock();
        visitor.do_prop = do_prop; // Whether to perform constant propagation
        visitor.do_fold = do_fold; // Whether to perform constant folding
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
            return new SymbolVal(((VariableSymbol) ((Variable) call.dest).getSym()).name(), call.getId(), null);
        return null;
    }

    @Override
    public SymbolVal visit(Variable var) {
        return null;
    }

    @Override
    public SymbolVal visit(Add add) {

        if (do_prop && (add.left instanceof Variable || add.left instanceof Temporary)) {
            SymbolVal val = get((Assignable) add.left);
            if (val.isConstant()) {
                add.left = val.val; // Set to Constant
            }
        }

        if (do_prop && (add.right instanceof Variable || add.right instanceof Temporary)) {
            SymbolVal val = get((Assignable) add.right);
            if (val.isConstant()) {
                add.right = val.val; // Set to Constant
            }
        }

        Literal retVal = null;

        if (do_fold && add.left.isConst() && add.right.isConst()) {
            retVal = add.calculate();
        }

        if (add.dest instanceof Variable) {
            return new SymbolVal(((VariableSymbol) ((Variable) add.dest).getSym()).name(), add.getId(), retVal);
        } else if (add.dest instanceof Temporary) {
            Temporary dest = (Temporary) add.dest;
            return new SymbolVal(dest.toString(), add.getId(), retVal);
        } else {
            return null;
        }
    }

    @Override
    public SymbolVal visit(Assign asn) {
        if (asn.dest instanceof Variable) {
            return new SymbolVal(((VariableSymbol) ((Variable) asn.dest).getSym()).name(), asn.getId(), null);
        } else {
            return null;
        }
    }

    @Override
    public SymbolVal visit(Div div) {
        if (do_prop && (div.left instanceof Variable || div.left instanceof Temporary)) {
            SymbolVal val = get((Assignable) div.left);
            if (val.isConstant()) {
                div.left = val.val; // Set to Constant
            }
        }

        if (do_prop && (div.right instanceof Variable || div.right instanceof Temporary)) {
            SymbolVal val = get((Assignable) div.right);
            if (val.isConstant()) {
                div.right = val.val; // Set to Constant
            }
        }

        Literal retVal = null;

        if (do_fold && div.left.isConst() && div.right.isConst()) {
            retVal = div.calculate();
        }

        if (div.dest instanceof Variable) {
            return new SymbolVal(((VariableSymbol) ((Variable) div.dest).getSym()).name(), div.getId(), retVal);
        } else if (div.dest instanceof Temporary) {
            Temporary dest = (Temporary) div.dest;
            return new SymbolVal(dest.toString(), div.getId(), retVal);
        } else {
            return null;
        }
    }

    @Override
    public SymbolVal visit(Mod mod) {
        if (do_prop && (mod.left instanceof Variable || mod.left instanceof Temporary)) {
            SymbolVal val = get((Assignable) mod.left);
            if (val.isConstant()) {
                mod.left = val.val; // Set to Constant
            }
        }

        if (do_prop && (mod.right instanceof Variable || mod.right instanceof Temporary)) {
            SymbolVal val = get((Assignable) mod.right);
            if (val.isConstant()) {
                mod.right = val.val; // Set to Constant
            }
        }

        Literal retVal = null;

        if (do_fold && mod.left.isConst() && mod.right.isConst()) {
            retVal = mod.calculate();
        }

        if (mod.dest instanceof Variable) {
            return new SymbolVal(((VariableSymbol) ((Variable) mod.dest).getSym()).name(), mod.getId(), retVal);
        } else if (mod.dest instanceof Temporary) {
            Temporary dest = (Temporary) mod.dest;
            return new SymbolVal(dest.toString(), mod.getId(), retVal);
        } else {
            return null;
        }
    }

    @Override
    public SymbolVal visit(Mul mul) {
        if (do_prop && (mul.left instanceof Variable || mul.left instanceof Temporary)) {
            SymbolVal val = get((Assignable) mul.left);
            if (val.isConstant()) {
                mul.left = val.val; // Set to Constant
            }
        }

        if (do_prop && (mul.right instanceof Variable || mul.right instanceof Temporary)) {
            SymbolVal val = get((Assignable) mul.right);
            if (val.isConstant()) {
                mul.right = val.val; // Set to Constant
            }
        }

        Literal retVal = null;

        if (do_fold && mul.left.isConst() && mul.right.isConst()) {
            retVal = mul.calculate();
        }

        if (mul.dest instanceof Variable) {
            return new SymbolVal(((VariableSymbol) ((Variable) mul.dest).getSym()).name(), mul.getId(), retVal);
        } else if (mul.dest instanceof Temporary) {
            Temporary dest = (Temporary) mul.dest;
            return new SymbolVal(dest.toString(), mul.getId(), retVal);
        } else {
            return null;
        }
    }

    @Override
    public SymbolVal visit(Sub sub) {
        if (do_prop && (sub.left instanceof Variable || sub.left instanceof Temporary)) {
            SymbolVal val = get((Assignable) sub.left);
            if (val.isConstant()) {
                sub.left = val.val; // Set to Constant
            }
        }

        if (do_prop && (sub.right instanceof Variable || sub.right instanceof Temporary)) {
            SymbolVal val = get((Assignable) sub.right);
            if (val.isConstant()) {
                sub.right = val.val; // Set to Constant
            }
        }

        Literal retVal = null;

        if (do_fold && sub.left.isConst() && sub.right.isConst()) {
            retVal = sub.calculate();
        }

        if (sub.dest instanceof Variable) {
            return new SymbolVal(((VariableSymbol) ((Variable) sub.dest).getSym()).name(), sub.getId(), retVal);
        } else if (sub.dest instanceof Temporary) {
            Temporary dest = (Temporary) sub.dest;
            return new SymbolVal(dest.toString(), sub.getId(), retVal);
        } else {
            return null;
        }

    }

    @Override
    public SymbolVal visit(Branch bra) {
        return null;
    }

    @Override
    public SymbolVal visit(Cmp cmp) {
        if (do_prop && (cmp.left instanceof Variable || cmp.left instanceof Temporary)) {
            SymbolVal val = get((Assignable) cmp.left);
            if (val.isConstant()) {
                cmp.left = val.val; // Set to Constant
            }
        }

        if (do_prop && (cmp.right instanceof Variable || cmp.right instanceof Temporary)) {
            SymbolVal val = get((Assignable) cmp.right);
            if (val.isConstant()) {
                cmp.right = val.val; // Set to Constant
            }
        }

        Literal retVal = null;

        if (do_fold && cmp.left.isConst() && cmp.right.isConst()) {
            retVal = cmp.calculate();
        }

        if (cmp.dest instanceof Variable) {
            return new SymbolVal(((VariableSymbol) ((Variable) cmp.dest).getSym()).name(), cmp.getId(), retVal);
        } else if (cmp.dest instanceof Temporary) {
            Temporary dest = (Temporary) cmp.dest;
            return new SymbolVal(dest.toString(), cmp.getId(), retVal);
        } else {
            return null;
        }
    }

    @Override
    public SymbolVal visit(Store store) {
        if (store.source instanceof Literal) {
            return new SymbolVal(store.dest.name(), store.getId(), (Literal) store.source);
        } else {
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
