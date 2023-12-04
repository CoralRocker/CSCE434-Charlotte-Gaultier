package ir.cfg.registers;

import ir.cfg.BasicBlock;
import ir.cfg.CFG;
import ir.tac.*;

import java.util.ArrayList;
import java.util.List;

class TacPair {
    public final LoadStack before1;
    public final LoadStack before2;

    public final List<LoadStack> args;
    public final StoreStack after;

    public boolean hasTwoBefore() {
        return before2 != null;
    }

    public boolean hasBefore() {
        return before1 != null;
    }

    public boolean hasAfter() {
        return after != null;
    }

    public TacPair(LoadStack before, StoreStack after) {
        this.before1 = before;
        this.before2 = null;
        this.after = after;
        this.args = null;
    }

    public TacPair(LoadStack before1, LoadStack before2, StoreStack after) {
        this.args = null;

        if( before1 == null && before2 != null ) {
            this.before1 = before2;
            this.before2 = null;
            this.after = after;
        }
        else {
            this.before1 = before1;
            this.before2 = before2;
            this.after = after;
        }
    }

    public TacPair(List<LoadStack> args, StoreStack after) {
        this.after = after;
        this.args = args;
        this.before1 = null;
        this.before2 = null;
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
                TAC tac = instructions.get(i);
                TacPair ls = tac.accept(this);
                if( ls != null ) {
                    if( instructions.get(i) instanceof Store ) {
                        if( ls.hasBefore() )
                            instructions.add(i++, ls.before1);
                        if( ls.hasAfter() )
                            instructions.add(++i, ls.after);
                    }
                    else if( instructions.get(i) instanceof Call ) {
                        if( ls.args != null && !ls.args.isEmpty() ) {
                            for( var ld : ls.args ) {
                                instructions.add(i++, ld );
                            }
                        }
                        if( ls.hasAfter() )
                            instructions.add( ++i, ls.after );
                    }
                    else {
                        if( ls.hasBefore() )
                            instructions.add(i++, ls.before1);
                        if( ls.hasTwoBefore() )
                            instructions.add(i++, ls.before2);
                        if( ls.hasAfter() )
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
            var instr = new LoadStack(newId, ret.dest, new Spill(loc, Spill.Register.DEST), ret);
            ret.dest.spilled.reg = Spill.Register.DEST;
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
        List<LoadStack> args = null;
        StoreStack dest = null;

        if( call.dest.equals(toSpill) ) {
            TacID newId = call.getIdObj().pushNext();
            dest = new StoreStack(newId, call.dest, new Spill(loc, Spill.Register.DEST), call);
        }

        for( var arg : call.args ) {
            if( arg.equals(toSpill) ) {
                TacID newId = call.getIdObj().pushPrevious();
                if( args == null ) args = new ArrayList<>();
                // TODO What register for this?
                args.add( new LoadStack(newId, arg, arg, call, Spill.Register.LHS) );
            }
        }

        if( args != null || dest != null )
            return new TacPair(args, dest);

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
        LoadStack load1 = null, load2 = null;
        StoreStack store = null;

        if( asn.left.equals(toSpill) ) {
            TacID newId = asn.getIdObj().pushPrevious();
            load1 = new LoadStack(newId, (Assignable)asn.left, (Assignable) asn.left, asn, Spill.Register.LHS);
        }

        if( asn.right.equals(toSpill) ) {
            TacID newId = asn.getIdObj().pushPrevious();
            load2 = new LoadStack(newId, (Assignable)asn.right, (Assignable) asn.right, asn, Spill.Register.RHS);
        }

        if( asn.dest.equals(toSpill) ) {
            TacID newId = asn.getIdObj().pushNext();
            store = new StoreStack(newId, asn.dest, new Spill(loc, Spill.Register.DEST), asn);
        }

        if( load1 == null && load2 == null && store == null )
            return null;
        else
            return new TacPair(load1, load2, store);
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

    public TacPair visit(Load load) {
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
            after = new StoreStack(newId, store.dest, new Spill(loc, Spill.Register.DEST), store);
        }

        if( store.source.equals(toSpill) ) {
            TacID newId = store.getIdObj().pushPrevious();
            // Store directly from stack to destination
            before = new LoadStack(newId, (Assignable) store.source, store.dest, store);
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
            after = new StoreStack(newId, not.dest, new Spill(loc, Spill.Register.DEST), not);
        }

        if( not.src.equals(toSpill) ) {
            TacID newId = not.getIdObj().pushPrevious();
            before = new LoadStack(newId, not.dest, new Spill(loc, Spill.Register.DEST), not);
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
