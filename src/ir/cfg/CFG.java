package ir.cfg;


import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

class Pair<U, V> {
    private U u;
    private V v;
    public Pair(U u, V v) {
        this.u = u;
        this.v = v;
    }

    public U first() {
        return u;
    }

    public V second() {
        return v;
    }
}

public class CFG implements Visitable<Void> {

    private BasicBlock head;

    public String asDotGraph() {
        calculateDOMSets();
        CFGPrinter printer = new CFGPrinter();
        return printer.genDotGraph(this);
    }

    public CFG(BasicBlock head) {
        this.head = head;
    }

    public BasicBlock getHead() {
        return head;
    }

    public void markUnvisited() {
        Queue<BasicBlock> queue = new LinkedList<>();

        queue.add(head);
        while( !queue.isEmpty() ) {
            BasicBlock node = queue.remove();
            node.resetVisited();

            for( BasicBlock child : node.getSuccessors() ) {
                if( child.visited() ) {
                    queue.add(child);
                }
            }
        }
    }

    public void breadthFirst( Consumer<BasicBlock> function ) {
        Queue<BasicBlock> queue = new LinkedList<>();

        queue.add(head);
        while( !queue.isEmpty() ) {
            BasicBlock node = queue.remove();
            node.markVisited();

            function.accept(node);

            for( BasicBlock child : node.getSuccessors() ) {
                if( !child.visited() ) {
                    queue.add(child);
                }
            }
        }
    }

    public void depthFirst( Consumer<BasicBlock> function ) {
        Stack<BasicBlock> stack = new Stack<>();

        stack.add(head);
        while( !stack.isEmpty() ) {
            BasicBlock node = stack.pop();
            node.markVisited();

            function.accept(node);

            for( BasicBlock child : node.getSuccessors() ) {
                if( !child.visited() ) {
                    stack.push(child);
                }
            }
        }
    }

    public void domIteration(List<BasicBlock> allblocks, BasicBlock rem) {
        List<Boolean> found = new ArrayList<>();
        allblocks.forEach((BasicBlock) -> {found.add(false);});

        Stack<BasicBlock> stack = new Stack<>();

        if( rem != head ) {
            stack.add(head);
        }

        while( !stack.isEmpty() ) {
            BasicBlock node = stack.pop();
            node.markVisited();

            found.set( node.getNum()-1, true );

            for( BasicBlock child : node.getSuccessors() ) {
                if( child != rem && !child.visited() ) {
                    stack.push(child);
                }
            }

        }

        System.out.printf("BB%d dominates: \n", rem.getNum());
        for( int i = 0; i < found.size(); i++ ) {
            if( !found.get(i) ) {
                System.out.printf("\tBB%d: %s\n", i+1, allblocks.get(i) );
            }
        }

    }

    public List<BasicBlock> getReversePostOrder() {
        Stack<Pair<BasicBlock, BasicBlock>> stack = new Stack<>();
        Stack<BasicBlock> out = new Stack<>();

        breadthFirst((BasicBlock blk) -> {
            blk.visitMap.clear();
            blk.resetVisited();
            for( BasicBlock parent : blk.getPredecessors() ) {
                blk.visitMap.put(parent, false);
            }
        });

        stack.push( new Pair<>(null, head) );

        BasicBlock prev = null, curr = null;
        while( !stack.isEmpty() ) {
            Pair<BasicBlock, BasicBlock> pair = stack.pop();
            curr = pair.second();
            prev = pair.first();
            out.push(curr);
            curr.setVisited(prev);
            curr.markVisited();

            for( BasicBlock child : curr.getSuccessors() ) {
                if( !child.visited() ) { // !child.allVisited() ) {
                    stack.push(new Pair<>(curr, child));
                }
            }

        }

        stack.clear();
        Stack<BasicBlock> outlist = new Stack<>();
        while( !out.isEmpty() ) {
            BasicBlock node = out.pop();
            node.resetVisited();
            node.visitMap.clear();
            outlist.push(node);
        }

        return outlist;
    }


    /**
     * @brief Calculate the DOM sets of each block in the CFG
     *
     * Based on the following paper:
     * http://www.hipersoft.rice.edu/grads/publications/dom14.pdf
     *
     */
    public void calculateDOMSets() {
        // Add Head to it's own dominance set
        head.dom = new ArrayList<>();
        head.dom.add( head );

        List<BasicBlock> dom = new ArrayList<>();
        List<Integer> domVal = new ArrayList<>();
        List<BasicBlock> blocks = getReversePostOrder();

        markUnvisited();
        for( int i = 0; i < blocks.size() - 1; i++ ) {
            BasicBlock b1 = blocks.get(i);
            BasicBlock b2 = blocks.get(i+1);

            System.out.printf("bb%d -> bb%d [style=dotted]\n", b1.getNum(), b2.getNum());
        }


        BiFunction<Integer, Integer, Integer> intersect = (Integer b1, Integer b2) -> {
            int finger1 = b1;
            int finger2 = b2;

            while( finger1 != finger2 ) {
                while( domVal.get(finger1) < domVal.get(finger2) ) {
                    finger1 = domVal.get(finger1);
                }
                while( domVal.get(finger2) < domVal.get(finger1) ) {
                    finger2 = domVal.get(finger2);
                }
            }

            return finger1;
        };

        breadthFirst( (BasicBlock blk) -> {
            while( dom.size() < blk.getNum() ) {
                dom.add(null);
            }

            dom.set( blk.getNum() - 1, blk);
        } );
        markUnvisited();

        domIteration(dom, dom.get(2));

        boolean changed = true;

        while( changed ) {
            changed = false;
            for( BasicBlock b : blocks ) {
               int new_idom = domVal.size();
               for( BasicBlock p : b.getPredecessors() ) {
                    int pval = domVal.get( p.getNum() );
                    if( pval != -1 && p.getNum() < new_idom ) {
                        new_idom = p.getNum();
                    }
               }

               if( new_idom == domVal.size() ) {
                   continue;
               }

               for( BasicBlock p : b.getPredecessors() ) {
                   if( p.getNum() == new_idom ) {
                       continue;
                   }

                   if( domVal.get(p.getNum()) != -1 ) {
                        new_idom = intersect.apply(p.getNum(), new_idom);
                   }
               }

               if( domVal.get(b.getNum()) != new_idom ) {
                   domVal.set(b.getNum(), new_idom);
                   changed = true;
               }


            }

        }
    }

    @Override
    public Void accept(CFGVisitor<Void> visitor) {
        return visitor.visit(head);
    }
}
