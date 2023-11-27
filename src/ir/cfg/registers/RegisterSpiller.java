package ir.cfg.registers;

import ir.cfg.BasicBlock;
import ir.cfg.CFG;
import ir.tac.*;

import java.util.List;

class TacPair {
    public final LoadStack before;
    public final StoreStack after;

    public TacPair(LoadStack before, StoreStack after) {
        this.before = before;
        this.after = after;
    }
}

public class RegisterSpiller implements TACVisitor<TacPair> {

    private Assignable toSpill;
    private Spill loc;
    private CFG cfg;


    public RegisterSpiller(CFG cfg, Assignable toSpill, Spill loc) {
        this.cfg = cfg;
        this.toSpill = toSpill;
        this.loc = loc;
    }

    public void generateLoadStores() {

        for(BasicBlock blk : cfg.allNodes) {
            int i = 0;
            List<TAC> instructions = blk.getInstructions();
            while( i < instructions.size() ) {
                TacPair ls = instructions.get(i).accept(this);
                if( ls != null ) {
                    if( ls.before != null ) {
                        instructions.add(i++, ls.before);
                    }

                    if( ls.after != null ) {
                        instructions.add(++i, ls.after);
                    }
                }
                i++;
            }
        }


    }

    @Override
    public TacPair visit(Return ret) {
        if( ret.dest.equals(toSpill) ) {
            TacID newId = ret.getIdObj().pushPrevious();
            var instr = new LoadStack(newId, ret.dest, loc);
            return new TacPair(instr, null);
        }
        return null;
    }

    @Override
    public TacPair visit(Literal lit) {
        return null;
    }

    @Override
    public TacPair visit(Call call) {
        if( call.dest.equals(toSpill) ) {
            TacID newId = call.getIdObj().pushNext();
            var instr = new StoreStack(newId, call.dest, loc);
            return new TacPair(null, instr);
        }
        return null;
    }

    @Override
    public TacPair visit(Variable var) {
        return null;
    }

    @Override
    public TacPair visit(Add add) {
        return visit((Assign) add);
    }

    @Override
    public TacPair visit(Assign asn) {
        LoadStack load = null;
        StoreStack store = null;

        if( asn.left.equals(toSpill) ) {
            TacID newId = asn.getIdObj().pushPrevious();
            load = new LoadStack(newId, (Assignable)asn.left, new Spill(loc, Spill.Register.LHS));
        }
        else if( asn.right.equals(toSpill) ) {
            TacID newId = asn.getIdObj().pushPrevious();
            load = new LoadStack(newId, (Assignable)asn.right, new Spill(loc, Spill.Register.RHS));
        }

        if( asn.dest.equals(toSpill) ) {
            TacID newId = asn.getIdObj().pushNext();
            store = new StoreStack(newId, asn.dest, new Spill(loc, Spill.Register.DEST));
        }

        if( load == null && store == null )
            return null;
        else
            return new TacPair(load, store);
    }

    @Override
    public TacPair visit(Div div) {
        return visit((Assign) div);
    }

    @Override
    public TacPair visit(Mod mod) {
        return visit((Assign) mod);
    }

    @Override
    public TacPair visit(Mul mul) {
        return visit((Assign) mul);
    }

    @Override
    public TacPair visit(Sub sub) {
        return visit((Assign) sub);
    }

    @Override
    public TacPair visit(LoadStack lstack) {
        return null;
    }

    @Override
    public TacPair visit(Branch bra) {
        return null;
    }

    @Override
    public TacPair visit(Cmp cmp) {
        return visit((Assign) cmp);
    }

    @Override
    public TacPair visit(Store store) {
        StoreStack after = null;
        LoadStack before = null;
        if( store.dest.equals(toSpill) ) {
            TacID newId = store.getIdObj().pushNext();
            after = new StoreStack(newId, store.dest, new Spill(loc, Spill.Register.DEST));
        }

        if( store.source.equals(toSpill) ) {
            TacID newId = store.getIdObj().pushPrevious();
            before = new LoadStack(newId, store.dest, new Spill(loc, Spill.Register.DEST));
        }

        if( after == null && before == null ) {
            return null;
        }
        else {
            return new TacPair(before, after);
        }
    }

    @Override
    public TacPair visit(StoreStack sstack) {
        return null;
    }

    @Override
    public TacPair visit(Phi phi) {
        return null;
    }

    @Override
    public TacPair visit(Temporary temporary) {
        return null;
    }

    @Override
    public TacPair visit(Not not) {
        StoreStack after = null;
        LoadStack before = null;
        if( not.dest.equals(toSpill) ) {
            TacID newId = not.getIdObj().pushNext();
            after = new StoreStack(newId, not.dest, new Spill(loc, Spill.Register.DEST));
        }

        if( not.src.equals(toSpill) ) {
            TacID newId = not.getIdObj().pushPrevious();
            before = new LoadStack(newId, not.dest, new Spill(loc, Spill.Register.DEST));
        }

        if( after == null && before == null ) {
            return null;
        }
        else {
            return new TacPair(before, after);
        }
    }

    @Override
    public TacPair visit(And and) {
        return visit((Assign) and);
    }

    @Override
    public TacPair visit(Or or) {
        return visit((Assign) or);
    }
}
