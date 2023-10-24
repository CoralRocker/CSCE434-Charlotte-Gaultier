package ir.cfg;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;

class DomNode {
   protected  Block node;

   protected DomNode parent;
   protected List< DomNode > children;

   protected boolean visited = false;

   public DomNode( Block blk ) {
       node = blk;
       parent = null;
       children = new ArrayList<>();
   }


}

public class DomTree {

    private DomNode root;

    public DomTree(  Block root ) {
        this.root = new DomNode( root );
    }

    private void breadthFirst( Consumer<DomNode> action ) {
        Queue<DomNode> queue = new LinkedList<>();

        queue.add( root );

        while( !queue.isEmpty() ) {
            DomNode node = queue.remove();
            node.visited = true;

            action.accept(node);

            for( DomNode child : node.children ) {
                if( !child.visited ) {
                    queue.add( child );
                }
            }
        }

        markUnvisited();
    }

    private void markUnvisited() {
        Queue<DomNode> queue = new LinkedList<>();

        queue.add( root );

        while( !queue.isEmpty() ) {
            DomNode node = queue.remove();
            node.visited = false;

            for( DomNode child : node.children ) {
                queue.add( child );
            }
        }

    }

    public void addNode( Block parent, Block newNode ) {
        breadthFirst((DomNode node) -> {
            if( node.node == parent ) {
                DomNode nn = new DomNode(newNode);
                nn.parent = node;
                node.children.add( nn );
            }
        });
    }

    public String genDot() {
        StringBuilder sb = new StringBuilder();

        sb.append("digraph DomTree {\n");

        breadthFirst((DomNode node) -> {
            sb.append(String.format("bb%d [label=\"BB%d\"];\n", node.node.getNum(), node.node.getNum()));
            if( node.parent != null ) {
                sb.append(String.format("bb%d -> bb%d\n", node.parent.node.getNum(), node.node.getNum()));
            }
        });

        sb.append("}\n");

        return sb.toString();
    }

}
