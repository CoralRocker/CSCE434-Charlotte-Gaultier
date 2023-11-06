package ir.cfg.optimizations;

import ir.cfg.BasicBlock;
import ir.cfg.CFG;
import ir.cfg.CFGVisitor;
import ir.tac.*;

import javax.swing.plaf.basic.BasicViewportUI;
import java.util.*;

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

    public AvailableExpression( CFG cfg, boolean do_cse, boolean do_cpp ) {
        this.cfg = cfg;

        var changed = new Object(){ boolean b = true; };

        cfg.markUnvisited();

        cfg.allNodes.forEach(blk -> {
            blk.entry = new HashMap<Expression, Expression>();
            blk.exit = null;
        });

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
                    blk.entry = iter.next().exit;
                    while( iter.hasNext() ) {
                        merge((Map<Expression, Expression>) blk.entry, (Map<Expression, Expression>) iter.next().exit);
                    }
                }
                // for (BasicBlock p : blk.getPredecessors()) {
                //     if( p.exit != null )
                //         // Not performing proper intersect-then-set
                //         changed.b |= merge((Map<Expression, Expression>) blk.entry, (Map<Expression, Expression>) p.exit);
                // }

                changed.b |= ExprInBlock.ExprInBlock(blk, false, false);
            });
        }

        for( BasicBlock b : cfg.allNodes ) {
            ExprInBlock.ExprInBlock(b, do_cse, do_cpp);
        }

    }

    @Override
    public Object visit(BasicBlock blk) {
        return null;
    }

}

