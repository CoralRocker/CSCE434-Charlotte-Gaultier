package ir.cfg.optimizations;

import coco.VariableSymbol;
import ir.cfg.BasicBlock;
import ir.cfg.CFG;
import ir.cfg.CFGVisitor;
import ir.tac.*;

import java.util.HashMap;
import java.util.TreeSet;

public class ReachingDefinition extends CFGVisitor {

    protected static boolean mergeSymbolList(TreeSet<SymbolVal> dest, TreeSet<SymbolVal> src, boolean do_print) {
        boolean changed = false;
        for( SymbolVal sym : src ) {
            if( dest.contains(sym) ) {
                // Merge into the set
                SymbolVal o = dest.subSet(sym, true, sym, true)
                                  .first();
                SymbolVal cpy = o.clone();
                boolean isChg = o.merge( sym );

                if( isChg ) {
                    if( do_print )
                        System.out.printf("\tMerged %s into %s\n", sym, cpy);
                    changed = true;
                }
            }
            else {
                throw new RuntimeException(String.format("Given destination does not contain SymbolVal %s", sym));
            }
        }

        return changed;
    }

    private CFG cfg;
    private int iters = 0;

    public ReachingDefinition(CFG cfg, boolean do_prop, boolean do_fold, boolean do_copy_prop, boolean do_print ) {
        this.cfg = cfg;

        cfg.markUnvisited();
        // Set Every Block's Entry/Exit to be null for all variables
        cfg.breadthFirst((BasicBlock b) -> {
            b.entry = new TreeSet<SymbolVal>();
            b.exit = new TreeSet<SymbolVal>();

            cfg.getSymbols().forEach((VariableSymbol sym)->{
                ((TreeSet<SymbolVal>)b.entry).add( new SymbolVal(sym.name(), -1));
                ((TreeSet<SymbolVal>)b.exit).add( new SymbolVal(sym.name(), -1));
            });
        });

        var changed = new Object(){ boolean b = true; };
        while( changed.b ) {
           changed.b = false;
            iters++;
            int finalIters = iters;
            cfg.breadthFirst((BasicBlock b) -> {
                if( do_print )
                    System.out.printf("%2d: Processing BB%d\n", finalIters, b.getNum());

                for( BasicBlock p : b.getPredecessors() ) {
                    if( b != p ) {
                        // Merge the incoming changes from "ABOVE"
                        if( do_print )
                            System.out.printf(" -> Merging BB%d -> BB%d\n", finalIters, p.getNum(), b.getNum() );
                        changed.b |= ReachingDefinition.mergeSymbolList((TreeSet<SymbolVal>) b.entry, (TreeSet<SymbolVal>) p.exit, do_print);
                    }
                }

                changed.b |= ConstantDefinedInBlock.defInBlock(b, false, do_fold, false, do_print);

                if( do_print )
                    System.out.println();
            });

            if( do_print )
                System.out.printf("Post Iteration %2d:\n", iters);
            // System.out.println(cfg.asDotGraph());
            // System.out.println("\n");
        }

        for (BasicBlock allNode : cfg.allNodes) {
            ConstantDefinedInBlock.defInBlock(allNode, do_prop, do_fold, do_copy_prop, do_print);
        }

        System.out.printf("Post Optimization:\n%s\n", cfg.asDotGraph());
    }

    @Override
    public Object visit(BasicBlock blk) {
        return null;
    }

    public int getIterations() {
        return iters;
    }

}

