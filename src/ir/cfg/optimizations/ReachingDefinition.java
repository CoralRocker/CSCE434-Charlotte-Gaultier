package ir.cfg.optimizations;

import coco.VariableSymbol;
import ir.cfg.BasicBlock;
import ir.cfg.CFG;
import ir.cfg.CFGVisitor;
import ir.tac.*;

import java.util.HashMap;
import java.util.Objects;
import java.util.TreeSet;

public class ReachingDefinition extends CFGVisitor {

    protected static boolean mergeSymbolList(HashMap<SymbolVal, SymbolVal> dest, HashMap<SymbolVal, SymbolVal> src, boolean do_print) {
        boolean changed = false;
        for( SymbolVal sym : src.keySet() ) {
            if( dest.containsKey(sym) ) {
                // Merge into the set
                SymbolVal o = dest.get(sym);
                SymbolVal cpy = o.clone();
                boolean isChg = o.merge( sym );

                if( isChg ) {
                    if( do_print )
                        System.out.printf("\tMerged %s into %s\n", sym, cpy);
                    changed = true;
                }
            }
            else {
                // Merge in temporaries found
                if( sym.isTemporary() ) {
                    dest.put( sym, sym );
                }
                else {
                    throw new RuntimeException(String.format("Given destination does not contain SymbolVal %s", sym));
                }
            }
        }

        return changed;
    }

    private CFG cfg;
    private int iters = 0;

    public boolean cfgchanged = false;

    public ReachingDefinition(CFG cfg, boolean do_prop, boolean do_fold, boolean do_copy_prop, boolean do_print ) {
        this.cfg = cfg;

        cfg.markUnvisited();
        // Set Every Block's Entry/Exit to be null for all variables
        cfg.breadthFirst((BasicBlock b) -> {
            b.entry = new HashMap<SymbolVal, SymbolVal>();
            b.exit = new HashMap<SymbolVal, SymbolVal>();

            cfg.getSymbols().keySet().forEach((VariableSymbol sym)->{
                SymbolVal symval = new SymbolVal(sym.name(), -1);
                ((HashMap<SymbolVal, SymbolVal>)b.entry).put( symval, symval);
                symval = new SymbolVal(sym.name(), -1);
                ((HashMap<SymbolVal, SymbolVal>)b.exit).put( symval, symval);
            });
        });

        var changed = new Object(){ boolean b = true; };
        iters = 0;
        while( changed.b ) {
           changed.b = false;
            iters++;
            int finalIters = iters;

            if( do_print ) {
                System.out.printf("Iteration %d Start \n", iters);
                for( var node : cfg.allNodes ) {
                    System.out.printf("%s:\nEntry:\t%s\nExit:\t%s\n", node, ((HashMap<SymbolVal, SymbolVal>) node.entry).keySet(), ((HashMap<SymbolVal, SymbolVal>) node.exit).keySet() );
                }
            }

            cfg.breadthFirst((BasicBlock b) -> {
                if( do_print )
                    System.out.printf("%2d: Processing BB%d\n", finalIters, b.getNum());

                for( BasicBlock p : b.getPredecessors() ) {
                    // if( b != p ) {
                        // Merge the incoming changes from "ABOVE"
                        if( do_print )
                            System.out.printf(" -> Merging BB%d -> BB%d\n", p.getNum(), b.getNum() );
                        changed.b |= ReachingDefinition.mergeSymbolList((HashMap<SymbolVal, SymbolVal>) b.entry, (HashMap<SymbolVal, SymbolVal>) p.exit, do_print);
                    // }
                }

                changed.b |= ConstantDefinedInBlock.defInBlock(b, false, do_fold, false, false, do_print);
                if( do_fold ) {
                    changed.b |= ArithmeticSimplification.MathSimplify(b);
                }

                if( do_print )
                    System.out.println();
            });

            if( do_print ) {
                System.out.printf("Post Iteration %2d: %s\n", iters, (changed.b) ? "CHANGED" : "NO CHANGE");
                System.out.println(cfg.asDotGraph());
                System.out.println("\n");
            }
        }

        var iter = cfg.allNodes.listIterator();
        while( iter.hasNext() ) {
            BasicBlock allNode = iter.next();
            cfgchanged |= ConstantDefinedInBlock.defInBlock(allNode, do_prop, do_fold, do_copy_prop, do_fold, do_print);
            if( do_fold ) {
                if( (allNode.getPredecessors().isEmpty() || (allNode.getPredecessors().size() == 1 && allNode.isPredecessor(allNode)) ) && allNode.getNum() != 1  ) {
                    if( do_print )
                        System.out.printf("Disconnecting %s\n", allNode);

                    // Delete this block and renumber eveything
                    allNode.disconnectSuccessors();

                    // for( var succ : allNode.getSuccessors() ) {
                    //     succ.getPredecessors().remove(allNode);
                    // }

                    iter.set(null);
                    cfgchanged = true;
                }
                else {
                    cfgchanged |= ArithmeticSimplification.MathSimplify(allNode);
                }
            }
        }
        cfg.allNodes.removeIf(Objects::isNull);
    }

    @Override
    public Object visit(BasicBlock blk) {
        return null;
    }

    public int getIterations() {
        return iters;
    }

}

