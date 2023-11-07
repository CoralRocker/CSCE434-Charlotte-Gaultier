package ir.cfg;


import coco.Symbol;
import coco.Variable;
import coco.VariableSymbol;
import ir.tac.TacIDGenerator;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class CFG implements Visitable<Object> {

    public TacIDGenerator instrNumberer = new TacIDGenerator();

    protected TreeSet<VariableSymbol> symbols;

    public TreeSet<VariableSymbol> getSymbols() { return symbols; }
    public void setSymbols(TreeSet<VariableSymbol> syms) { symbols = syms; }

    private BasicBlock head;
    public List<BasicBlock> allNodes = null;
    private DomTree tree = null;

    public String asDotGraph() {
        calculateDOMSets();
        CFGPrinter printer = new CFGPrinter(this);
        return printer.genDotGraph();
    }

    public CFG(BasicBlock head) {
        this.head = head;
    }

    public BasicBlock getHead() {
        return head;
    }

    public void markUnvisited() {
        if( allNodes == null ) {
            Queue<BasicBlock> queue = new LinkedList<>();

            queue.add(head);
            while (!queue.isEmpty()) {
                BasicBlock node = queue.remove();
                node.resetVisited();

                for (BasicBlock child : node.getSuccessors()) {
                    if (child.visited()) {
                        queue.add(child);
                    }
                }
            }
        }
        else {
            allNodes.forEach(b -> {
                b.resetVisited();
            });
        }
    }

    public void breadthFirst( Consumer<BasicBlock> function ) {
        Queue<BasicBlock> queue = new LinkedList<>();

        queue.add(head);
        head.markVisited();
        while( !queue.isEmpty() ) {
            BasicBlock node = queue.remove();

            function.accept(node);

            for( BasicBlock child : node.getSuccessors() ) {
                if( !child.visited() ) {
                    queue.add(child);
                    child.markVisited();
                }
            }
        }

        markUnvisited();
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
        allblocks.forEach((BasicBlock) -> {found.add(false);}); // O(n)

        Stack<BasicBlock> stack = new Stack<>();

        if( rem != head ) {
            stack.add(head);
        }

        // O(n)
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

        // O(n)
        for( int i = 0; i < found.size(); i++ ) {
            if( !found.get(i) ) {
                allblocks.get(i).domBy.add( rem );
                rem.domTo.add(allblocks.get(i));
            }
        }

    }

    /**
     * @brief Calculate the DOM sets of each block in the CFG
     *
     * Based on the following paper:
     * http://www.hipersoft.rice.edu/grads/publications/dom14.pdf
     *
     */
    public void calculateDOMSets() {

        List<BasicBlock> dom = new ArrayList<>();
        List<Integer> domVal = new ArrayList<>();


        markUnvisited();

        allNodes = dom;
        breadthFirst( (BasicBlock blk) -> {
            while( dom.size() < blk.getNum() ) {
                dom.add(null);
            }

            blk.domBy = new ArrayList<>();
            blk.domTo = new ArrayList<>();
            blk.domFrontier = new ArrayList<>();

            dom.set( blk.getNum() - 1, blk);
        } );

        for( int i = 0; i < dom.size(); i++ ) {
            domIteration(dom, dom.get(i));
            markUnvisited();
        }

        // for( BasicBlock blk : dom ) {
        //     System.out.printf("BB%d Dominated By:\n", blk.getNum());
        //     for( BasicBlock b : blk.domBy ) {
        //         System.out.printf("\tBB%d\n", b.getNum());
        //     }
        //     System.out.printf("BB%d Dominates:\n", blk.getNum());
        //     for( BasicBlock b : blk.domTo ) {
        //         System.out.printf("\tBB%d\n", b.getNum());
        //     }
        //
        //     if( idom == null )
        //         System.out.printf("BB%d Immediate Dominator: NONE\n", blk.getNum());
        //     else
        //         System.out.printf("BB%d Immediate Dominator: BB%d\n", blk.getNum(), idom.getNum());
        // }

        tree = new DomTree(head);
        for( int i = 1; i < dom.size(); i++ ) {
            BasicBlock node = dom.get(i);
            node.getIDom();
            tree.addNode(node.idom, node);
        }

        // System.out.printf("Dominator Tree:\n %s\n", tree.genDot());

        calculateDomFrontier();

        // System.out.println("Dominance Frontiers:");
        // for( BasicBlock blk : dom ) {
        //     System.out.printf("BB%d: {", blk.getNum());
        //     if( blk.domFrontier != null ) {
        //         blk.domFrontier.forEach((BasicBlock b) -> {
        //             System.out.printf(" BB%d", b.getNum());
        //         });
        //     }
        //     System.out.printf(" }\n");
        // }
    }

    public void calculateDomFrontier() {
        breadthFirst((BasicBlock v) -> {
            if( v.getPredecessors().size() >= 2 ) {
                for( BasicBlock p : v.getPredecessors() ) {
                    BasicBlock runner = p;
                    while( runner != v.idom ) {
                        if( runner.domFrontier == null ) {
                            runner.domFrontier = new ArrayList<>();
                        }
                        runner.domFrontier.add(v);
                        runner = runner.idom;
                    }
                }
            }
        });
    }

    @Override
    public Object accept(CFGVisitor<Object> visitor) {
        return visitor.visit(this.head);
    }
}
