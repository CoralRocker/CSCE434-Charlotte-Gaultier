package ir.cfg;

import ir.cfg.optimizations.ArithmeticSimplification;
import ir.tac.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.BiFunction;

public class UnitializedVariable implements TACVisitor<Assignable> {

    private HashMap<Assignable, Assignable> initialized;
    private HashSet<Assignable> uninitialized = new HashSet<>();
    private CFG cfg;
    private boolean do_print;

    private static HashMap<Assignable, Assignable> intersect(HashMap<Assignable, Assignable> s1, HashMap<Assignable, Assignable> s2) {
        HashMap<Assignable, Assignable> result = new HashMap<>();

        // Short-circuit on null
        if( s1 == null && s2 == null ) return result;
        if( s1 == null ) return new HashMap<>(s2);
        if( s2 == null ) return new HashMap<>(s1);

        for( var var : s1.keySet() ) {
            if( s2.containsKey(var) ) {
                result.put( var, var );
            }
        }

        return result;
    }

    public static boolean checkUnitializedVars( CFG cfg, boolean do_print ) {

        cfg.breadthFirst(blk -> {
            blk.entry = null;
            blk.exit = null;
        });

        HashMap<Assignable, Assignable> funcParams = new HashMap<>();
        if( cfg.function != null ) {
            for( var varsym : cfg.function.getArgList() ) {
                var var = new Variable(varsym);
                funcParams.put(var, var);
            }
        }

        boolean changed = true;
        while( changed ) {
            changed = false;

            for (var blk : cfg.allNodes) {
                var prevExit = (blk.exit != null ) ? new HashMap<Assignable, Assignable>((HashMap<Assignable, Assignable>) blk.exit) : null ;

                UnitializedVariable visitor = new UnitializedVariable();
                visitor.cfg = cfg;
                visitor.do_print = do_print;

                var preds = blk.getPredecessors();
                if (preds.size() == 2) {
                    visitor.initialized = intersect((HashMap<Assignable, Assignable>) preds.get(0).exit, (HashMap<Assignable, Assignable>) preds.get(1).exit);
                } else if (preds.size() == 1) {
                    visitor.initialized = new HashMap<>((HashMap<Assignable, Assignable>) preds.get(0).exit);
                } else if (preds.isEmpty()) {
                    visitor.initialized = new HashMap<>(funcParams);
                }
                else {
                    throw new RuntimeException("Predecessors size for " + blk + " is out of range [0, 2]");
                }

                blk.entry = new HashMap<>(visitor.initialized);

                blk.exit = visitor.getInitialized(blk);

                blk.state = visitor.uninitialized;

                // Check if old exit and new exit are identical
                if( prevExit == null || prevExit.size() != ((HashMap<?, ?>) blk.exit).size() ) {
                    changed = true;
                }
                else {
                    var exit = (HashMap<Assignable, Assignable>) blk.exit;
                    for( var asn : prevExit.keySet() ) {
                        if( !exit.containsKey(asn) ) {
                            changed = true;
                            break;
                        }
                    }
                }


                if (do_print) {
                    System.out.printf("Block %s changed? %s\n", blk, changed);
                    System.out.printf("Initialized at entry of %s : %s\n", blk, ((HashMap<Assignable, Assignable>) blk.entry).keySet());
                    System.out.printf("Initialized at exit of %s  : %s\n", blk, ((HashMap<Assignable, Assignable>) blk.exit).keySet());
                    System.out.printf("Unitialized uses of %s     : %s\n\n", blk, visitor.uninitialized);
                }
            }
        }

        boolean cfgchanged = false;

        // Perform Initialization
        for( var blk : cfg.allNodes ) {
            HashSet<Assignable> uninit = (HashSet<Assignable>) blk.state;

            if( uninit.isEmpty() ) continue;

            cfgchanged = true;

            var preds = blk.getPredecessors();
            // If 1 or less predecessors, trivial to init
            if( preds.size() <= 1 ) {
                int ctr = 0;
                var instructions = blk.getInstructions();
                TacID blkfront = null;
                for( var var : uninit ) {
                    TacID ntac = null;
                    if( blkfront == null ) {
                        ntac = instructions.get(0).getIdObj().pushPrevious();
                        blkfront = ntac;
                    }
                    else {
                        ntac = blkfront.pushNext();
                        blkfront = ntac;
                    }

                    TAC tac = new Store(ntac, var, Literal.get(0) );
                    instructions.add(ctr, tac);
                    if( do_print ) {
                        System.out.printf("Adding %s as instruction %d in block %s\n", tac, ctr, blk);
                    }
                    ctr++;
                }
            }
            else {
                var prev1 = preds.get(0);
                var prev2 = preds.get(1);

                BiFunction<BasicBlock, Assignable, Boolean> isInit = (BasicBlock b, Assignable asn) -> {
                    return ((HashMap<Assignable, Assignable>) b.exit).containsKey(asn);
                };

                List<Assignable>  b1 = new ArrayList<>(), b2 = new ArrayList<>(), curB = new ArrayList<>();

                // Repartition the variables to each block for initialization
                for( var var : uninit ) {
                    boolean p1 = isInit.apply(prev1, var);
                    boolean p2 = isInit.apply(prev2, var);

                    if( p1 && p2 ) {
                        throw new RuntimeException(String.format("Variable %s is defined in %s and %s but not in %s?", var, prev1, prev2, blk));
                    }
                    else if( !p1 && !p2 ) {
                        curB.add( var );
                    }
                    else if( !p1 ) {
                        b1.add(var);
                    }
                    else {
                        b2.add(var);
                    }
                }

                // Add to current block
                if( !curB.isEmpty() ) {
                    TacID blkfront = null;
                    int ctr = 0;
                    var instructions = blk.getInstructions();
                    for (var var : curB) {
                        TacID ntac = null;
                        if (blkfront == null) {
                            ntac = blk.getInstructions().get(0).getIdObj().pushPrevious();
                            blkfront = ntac;
                        } else {
                            ntac = blkfront.pushNext();
                            blkfront = ntac;
                        }

                        TAC tac = new Store(ntac, var, Literal.get(0));
                        instructions.add(ctr, tac);
                        if (do_print) {
                            System.out.printf("Adding %s as instruction %d in block %s\n", tac, ctr, blk);
                        }
                        ctr++;
                    }
                }

                // Add to end of first predecessor
                if( !b1.isEmpty() ) {
                    if( do_print )
                        System.out.printf("Variables %s need to be init'd in %s\n", b1, prev1 );

                    var instructions = prev1.getInstructions();
                    int ctr = 0;
                    TacID blkfront = null;
                    for (var var : b1) {
                        TacID ntac = null;
                        if (blkfront == null) {
                            ntac = instructions.get(0).getIdObj().pushPrevious();
                            blkfront = ntac;
                        } else {
                            ntac = blkfront.pushNext();
                            blkfront = ntac;
                        }

                        TAC tac = new Store(ntac, var, Literal.get(0));
                        instructions.add(ctr, tac);
                        if (do_print) {
                            System.out.printf("Adding %s as instruction %d in block %s\n", tac, ctr, prev1);
                        }
                        ctr++;
                    }
                }

                if( !b2.isEmpty() ) {
                    if( do_print )
                        System.out.printf("Variables %s need to be init'd in %s\n", b2, prev2 );

                    var instructions = prev2.getInstructions();
                    int ctr = 0;
                    TacID blkfront = null;
                    for (var var : b2) {
                        TacID ntac = null;
                        if (blkfront == null) {
                            ntac = instructions.get(0).getIdObj().pushPrevious();
                            blkfront = ntac;
                        } else {
                            ntac = blkfront.pushNext();
                            blkfront = ntac;
                        }

                        TAC tac = new Store(ntac, var, Literal.get(0));
                        instructions.add(ctr, tac);
                        if (do_print) {
                            System.out.printf("Adding %s as instruction %d in block %s\n", tac, ctr, prev2);
                        }
                        ctr++;
                    }
                }
            }
        }

        if( do_print && cfgchanged ) {
            System.out.printf("CFG After Initializing All: \n%s\n", cfg.asDotGraph());
        }

        return cfgchanged;
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
        if( ret.var instanceof Assignable ) {
            if( !initialized.containsKey(ret.var) ) {
                if( do_print )
                    System.out.printf("Variable %s is not initialized : %3d : %s\n", ret.var, ret.getId(), ret);
                uninitialized.add((Assignable) ret.var);
            }
        }
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
        if( asn.left instanceof Assignable ) {
            if( !initialized.containsKey(asn.left) ) {
                if( do_print )
                    System.out.printf("Variable %s is not initialized : %3d : %s\n", asn.left, asn.getId(), asn);
                uninitialized.add((Assignable) asn.left);
            }
        }
        if( asn.right instanceof Assignable ) {
            if( !initialized.containsKey(asn.right) ) {
                if( do_print )
                    System.out.printf("Variable %s is not initialized : %3d : %s\n", asn.right, asn.getId(), asn);
                uninitialized.add((Assignable) asn.right);
            }
        }

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
        if( store.source instanceof Assignable && !initialized.containsKey(store.source) ) {
            if( do_print )
                System.out.printf("Variable %s is not initialized : %3d : %s\n", store.source, store.getId(), store);
            uninitialized.add((Assignable) store.source);
        }
        return store.dest;
    }

    @Override
    public Assignable visit(StoreStack sstack) {
        if( sstack.isSpill() && !initialized.containsKey(sstack.dest) ) {
            if( do_print )
                System.out.printf("Variable %s is not initialized : %3d : %s\n", sstack.dest, sstack.getId(), sstack);
            uninitialized.add(sstack.dest);
        }
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
        if( not.src instanceof Assignable && !initialized.containsKey(not.src) ) {
            if( do_print )
                System.out.printf("Variable %s is not initialized : %3d : %s\n", not.dest, not.getId(), not);
            uninitialized.add((Assignable) not.src);
        }
        return not.dest;
    }

    @Override
    public Assignable visit(Load load) {
        return load.dest;
    }
}
