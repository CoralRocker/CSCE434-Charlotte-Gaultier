package ir.cfg.optimizations;

import ir.cfg.BasicBlock;
import ir.cfg.CFG;
import ir.tac.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ProgramPointLiveness {


    private final CFG cfg;

    public ProgramPointLiveness(CFG cfg) {
        this.cfg = cfg;
    }

    public boolean doDCE(boolean do_print) {
        return DeadCode.doDCE(cfg, do_print);
    }

    public void calculate(boolean do_print) {
        cfg.breadthFirst(blk -> {
            blk.live_in = new HashSet<>();  // Live in: Variable live at block entry
            blk.live_out = new HashSet<>(); // Live out: Variables live at block exit
        });

        var changed = new Object(){ boolean b = true; };
        int iterations = 0;
        while( changed.b ) {
            changed.b = false;
            iterations++;
            cfg.reverseBreadthFirst(blk -> {
                HashSet<Assignable> old_live = blk.live_out;
                blk.live_out = new HashSet<>(old_live.size());
                for (var pred : blk.getSuccessors()) {
                    blk.live_out.addAll(pred.live_in);
                }
                if( old_live.size() != blk.live_out.size() )
                    changed.b = true;
                else {
                    old_live.removeAll(blk.live_out);
                    if( !old_live.isEmpty() ) {
                        changed.b = true;
                    }
                }

                changed.b = TACLiveness.BlockLiveness(blk);
            });

            if( do_print ) {
                System.out.printf("Liveness Analysis Iteration %d:\n", iterations);
                System.out.printf("Done?: %b\n", !changed.b);

                for (BasicBlock blk : cfg.allNodes) {
                    System.out.printf("BB%d:\n", blk.getNum());
                    for (TAC tac : blk.getInstructions()) {
                        System.out.printf("%3d: %-20s %15s -> %-15s\n", tac.getId(), tac.genDot(), tac.liveBeforePP, tac.liveAfterPP);
                    }
                }
                System.out.println("\n");
            }
        }


    }

}

class LiveData {
    public final Assignable dest;
    public final Assignable use1, use2;

    public final List<Assignable> funcArgs;

    public LiveData(Assignable d, Assignable u1, Assignable u2) {
        dest = d;
        use1 = u1;
        use2 = u2;
        funcArgs = null;
    }

    public LiveData(Assignable d, Value v1, Value v2) {
        dest = d;
        if( v1 instanceof Assignable ) {
            use1 = (Assignable) v1;
            if( v2 instanceof Assignable ) {
                use2  = (Assignable) v2;
            }
            else {
                use2 = null;
            }
        }
        else if( v2 instanceof Assignable ) {
            use1 = (Assignable) v2;
            use2 = null;
        }
        else {
            use1 = null;
            use2 = null;
        }

        funcArgs = null;
    }

    public LiveData(Assignable d, Value v) {
        dest = d;
        use2 = null;
        if( v instanceof Assignable ) {
            use1 = (Assignable) v;
        }
        else {
            use1 = null;
        }

        funcArgs = null;
    }

    public LiveData(Assignable d, List<Assignable> args) {
        dest = d;
        use1 = null;
        use2 = null;
        funcArgs = args;
    }

}

class TACLiveness extends TACVisitor<LiveData> {

    public static boolean BlockLiveness(BasicBlock blk) {
        TACLiveness analysis = new TACLiveness();

        HashSet<Assignable> prevLive = blk.live_out;

        var iter = blk.getInstructions().listIterator(blk.getInstructions().size());

        boolean changed = false;

        while( iter.hasPrevious() ) {
            var tac = iter.previous();
            tac.liveAfterPP = prevLive;

            LiveData atPoint = tac.accept(analysis);

            HashSet<Assignable> prevPP = tac.liveBeforePP;
            tac.liveBeforePP = (HashSet<Assignable>) prevLive.clone();

            if( atPoint != null ) {
                if (atPoint.dest != null) {
                    tac.liveBeforePP.remove(atPoint.dest);
                }
                if( atPoint.funcArgs != null ) {
                    tac.liveBeforePP.addAll(atPoint.funcArgs);
                }
                else {
                    if (atPoint.use1 != null) {
                        tac.liveBeforePP.add(atPoint.use1);
                    }
                    if (atPoint.use2 != null) {
                        tac.liveBeforePP.add(atPoint.use2);
                    }
                }
            }

            if( prevPP == null || prevPP.size() != tac.liveBeforePP.size() ) {
                changed = true;
            }
            else {
                for( Assignable node : tac.liveBeforePP) {
                    if( !prevPP.contains(node) ) {
                        changed = true;
                        break;
                    }
                }
            }
            prevLive = tac.liveBeforePP;
        }

        blk.live_in = (HashSet<Assignable>) prevLive.clone();

        return changed;
    }

    @Override
    public LiveData visit(Return ret) {
        return new LiveData(null, ret.dest, null);
    }

    @Override
    public LiveData visit(Literal lit) {
        return null;
    }

    @Override
    public LiveData visit(Call call) {
        return new LiveData(call.dest, call.args);
    }

    @Override
    public LiveData visit(Variable var) {
        return null;
    }

    @Override
    public LiveData visit(Add add) {
        return visit((Assign)add);
    }

    @Override
    public LiveData visit(Assign asn) {
        return new LiveData(asn.dest, asn.right, asn.left);
    }

    @Override
    public LiveData visit(Div div) {
        return visit((Assign) div);
    }

    @Override
    public LiveData visit(Mod mod) {
        return visit((Assign) mod);
    }

    @Override
    public LiveData visit(Mul mul) {
        return visit((Assign) mul);
    }

    @Override
    public LiveData visit(Sub sub) {
        return visit((Assign) sub);
    }

    @Override
    public LiveData visit(LoadStack lstack) {
        return new LiveData(null, lstack.val, null);
    }

    @Override
    public LiveData visit(Branch bra) {
        return new LiveData(null, bra.getVal());
    }

    @Override
    public LiveData visit(Cmp cmp) {
        return visit((Assign) cmp);
    }

    @Override
    public LiveData visit(Store store) {
        return new LiveData(store.dest, store.source);
    }

    @Override
    public LiveData visit(StoreStack sstack) {
        return new LiveData(sstack.dest, null, null);
    }

    @Override
    public LiveData visit(Phi phi) {
        return null;
    }

    @Override
    public LiveData visit(Temporary temporary) {
        return null;
    }

    @Override
    public LiveData visit(Not not) {
        return new LiveData(not.dest, not.getSrc());
    }

    @Override
    public LiveData visit(And and) {
        return new LiveData(and.dest, and.left, and.right);
    }

    @Override
    public LiveData visit(Or or) {
        return new LiveData(or.dest, or.left, or.right);
    }
}

class DeadCode extends TACVisitor<Boolean> {

    private boolean do_print;
    private TAC curTac;

    public static boolean doDCE(CFG cfg, boolean do_print) {
        DeadCode dce = new DeadCode();
        dce.do_print = do_print;

        List<TacID> removed = new ArrayList<>();

        boolean changed = false;
        for( BasicBlock blk : cfg.allNodes ) {
            var iter = blk.getInstructions().listIterator();
            while( iter.hasNext() ) {
                var instr = iter.next();
                dce.curTac = instr;
                boolean dead = instr.accept(dce);
                changed |= dead;
                if( dead ) {
                    iter.remove();
                    removed.add(instr.getIdObj());
                }
            }
        }

        cfg.instrNumberer.removeAll(removed);

        return changed;
    }

    @Override
    public Boolean visit(Return ret) {
        return false;
    }

    @Override
    public Boolean visit(Literal lit) {
        return false;
    }

    @Override
    public Boolean visit(Call call) {
        return false;
    }

    @Override
    public Boolean visit(Variable var) {
        return false;
    }

    @Override
    public Boolean visit(Add add) {
        return visit((Assign) add);
    }

    @Override
    public Boolean visit(Assign asn) {
        // Return whether the destination is live after the instruction
        if( !curTac.liveAfterPP.contains(asn.dest) ) {
            return true;
        }
        return false;
    }

    @Override
    public Boolean visit(Div div) {
        return visit((Assign) div);
    }

    @Override
    public Boolean visit(Mod mod) {
        return visit((Assign) mod);
    }

    @Override
    public Boolean visit(Mul mul) {
        return visit((Assign) mul);
    }

    @Override
    public Boolean visit(Sub sub) {
        return visit((Assign) sub);
    }

    @Override
    public Boolean visit(LoadStack lstack) {
        return false;
    }

    @Override
    public Boolean visit(Branch bra) {
        return false;
    }

    @Override
    public Boolean visit(Cmp cmp) {
        return visit((Assign) cmp);
    }

    @Override
    public Boolean visit(Store store) {
        // Return whether the destination is live after the instruction
        if( !curTac.liveAfterPP.contains(store.dest) ) {
            return true;
        }
        return false;
    }

    @Override
    public Boolean visit(StoreStack sstack) {
        return false;
    }

    @Override
    public Boolean visit(Phi phi) {
        return false;
    }

    @Override
    public Boolean visit(Temporary temporary) {
        return false;
    }

    @Override
    public Boolean visit(Not not) {
        if( !curTac.liveAfterPP.contains(not.dest) ) {
            return true;
        }
        return false;
    }

    @Override
    public Boolean visit(And and) {
        return visit((Assign) and);
    }

    @Override
    public Boolean visit(Or or) {
        return visit((Assign)or);
    }
}