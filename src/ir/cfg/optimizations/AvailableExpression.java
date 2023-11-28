package ir.cfg.optimizations;

import ir.cfg.BasicBlock;
import ir.cfg.CFG;
import ir.cfg.CFGVisitor;
import ir.tac.*;

import javax.swing.plaf.basic.BasicViewportUI;
import java.util.*;

// TODO: Available Expression Converts
// store a 2
// store _t0 2
//
// TODO: to the incorrect
// store a 2
// store _t0 a
//
public class AvailableExpression extends CFGVisitor {

    // Perform a set intersection
    // Return true if one of the keys in dest are not in src.
    public static boolean merge(Map<Expression, Expression> dest, Map<Expression, Expression> src ) {
        if( src == dest )
            throw new RuntimeException("Merge Cannot Be Done To Itself!");

        var keys = dest.keySet();

        return keys.retainAll(src.keySet());
    }

    private CFG cfg;
    private int iters = 0;

    protected boolean isChanged = false;

    private boolean do_print;

    public boolean isChanged() {
        return isChanged;
    }

    public AvailableExpression(CFG cfg, boolean do_cse, boolean do_cpp, boolean do_print ) {
        this.cfg = cfg;

        var changed = new Object(){ boolean b = true; };

        cfg.markUnvisited();

        cfg.allNodes.forEach(blk -> {
            blk.entry = new HashMap<Expression, Expression>();
            blk.exit = null;
        });

        iters = 0;
        while( changed.b ) {
            changed.b = false;
            iters++;

            cfg.breadthFirst(blk -> {
                List<BasicBlock> preds = blk.getPredecessors();
                if( preds.size() ==  1 ) {
                    blk.entry = preds.get(0).exit;
                }
                else if( preds.isEmpty() ) {
                    blk.entry = new HashMap<Expression, Expression>();
                }
                else {
                    var iter = preds.iterator();
                    while( iter.hasNext() ) {
                        var src = iter.next();
                        if( src.exit != null ) {
                            blk.entry = ((HashMap<Expression, Expression>)src.exit).clone(); // Needs to be a deep copy
                            break;
                        }
                    }
                    while( iter.hasNext() ) {
                        var src = iter.next();
                        if( src.exit != null )
                            merge((Map<Expression, Expression>) blk.entry, (Map<Expression, Expression>) src.exit);
                    }
                }
                // for (BasicBlock p : blk.getPredecessors()) {
                //     if( p.exit != null )
                //         // Not performing proper intersect-then-set
                //         changed.b |= merge((Map<Expression, Expression>) blk.entry, (Map<Expression, Expression>) p.exit);
                // }

                changed.b |= ExprInBlock.ExprInBlock(blk, false, false, do_print);
            });
        }

        for( BasicBlock b : cfg.allNodes ) {
            isChanged |= ExprInBlock.ExprInBlock(b, do_cse, do_cpp, do_print);
        }

    }

    @Override
    public Object visit(BasicBlock blk) {
        return null;
    }

    public int getIters() {
        return iters;
    }
}

