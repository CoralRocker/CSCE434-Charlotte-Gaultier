package ir.cfg;

import ir.tac.*;

import java.util.HashMap;

public class UnitializedVariable implements TACVisitor<Assignable> {

    private HashMap<Assignable, Assignable> initialized;
    private CFG cfg;
    private boolean do_print;

    public static boolean checkUnitializedVars( CFG cfg, boolean do_print ) {

        cfg.breadthFirst(blk -> {
            blk.entry = new HashMap<Assignable, Assignable>();
            blk.exit = new HashMap<Assignable, Assignable>();
        });


        for( var blk : cfg.allNodes ) {
            UnitializedVariable visitor = new UnitializedVariable();
            visitor.initialized = new HashMap<>();
            visitor.cfg = cfg;
            visitor.do_print = do_print;

            visitor.getInitialized(blk);
        }

        return false;
    }

    public HashMap<Assignable, Assignable> getInitialized( BasicBlock blk ) {

        for( TAC tac : blk.getInstructions() ) {
            Assignable asn = tac.accept(this);
            if( asn != null ) {
                if( !initialized.containsKey(asn) )
                    initialized.put(asn, asn);
            }
        }

        return initialized;
    }

    @Override
    public Assignable visit(Return ret) {
        return null;
    }

    @Override
    public Assignable visit(Literal lit) {
        return null;
    }

    @Override
    public Assignable visit(Call call) {
        return call.dest;
    }

    @Override
    public Assignable visit(Variable var) {
        return null;
    }

    @Override
    public Assignable visit(Assign asn) {
        return asn.dest;
    }

    @Override
    public Assignable visit(LoadStack lstack) {
        return lstack.dest;
    }

    @Override
    public Assignable visit(Branch bra) {
        return null;
    }

    @Override
    public Assignable visit(Store store) {
        return store.dest;
    }

    @Override
    public Assignable visit(StoreStack sstack) {
        return null;
    }

    @Override
    public Assignable visit(Phi phi) {
        return null;
    }

    @Override
    public Assignable visit(Temporary temporary) {
        return null;
    }

    @Override
    public Assignable visit(Not not) {
        return not.dest;
    }

    @Override
    public Assignable visit(Load load) {
        return load.dest;
    }
}
