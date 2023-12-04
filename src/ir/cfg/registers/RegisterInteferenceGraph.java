package ir.cfg.registers;

import ir.cfg.CFG;
import ir.tac.Assignable;
import ir.tac.Variable;

import java.util.*;
import java.util.function.BiFunction;


public class RegisterInteferenceGraph {


    public enum EdgeType {
        NONE,
        INTERFERE,
        MOVE_RELATED;
    }

    class Edge {
        public RegisterInteferenceGraph.EdgeType type;
        public VariableNode n1, n2;

        public Edge(RegisterInteferenceGraph.EdgeType t, VariableNode n1, VariableNode n2) {
            this.type = t;
            this.n1 = n1;
            this.n2 = n2;
        }

        @Override
        public String toString() {
            return String.format("%s -- %s -- %s", n1, type, n2);
        }

        @Override
        public boolean equals(Object other) {
            if( !(other instanceof Edge) ) {
                return false;
            }

            Edge edge = (Edge) other;

            return (n1.hashCode() == edge.n1.hashCode() && n2.hashCode() == edge.n2.hashCode())
                || (n2.hashCode() == edge.n1.hashCode() && n1.hashCode() == edge.n2.hashCode());
        }

        @Override
        public int hashCode() {
            int h1 = n1.hashCode(), h2 = n2.hashCode();
            if( h1 < h2 ) {
                return String.format("%d%d", h1, h2).hashCode();
            }
            else {
                return String.format("%d%d", h2, h1).hashCode();
            }
        }

        public boolean isExcluded() {
            return n1.exclude || n2.exclude;
        }
    }

    private HashMap<VariableNode, HashSet<Edge>> nodes;
    private HashMap<VariableNode, VariableNode> nodeResovler;
    private int nodeNum;


    public RegisterInteferenceGraph() {
        this.nodes = new HashMap<>();
        this.nodeResovler = new HashMap<>();
        nodeNum = 0;
    }

    public void addEdge(VariableNode n1, VariableNode n2 ) {
        Edge edge = new Edge(EdgeType.INTERFERE, n1, n2);

        nodes.get(n1).add(edge);
        nodes.get(n2).add(edge);
    }
    public void addMoveEdge(VariableNode n1, VariableNode n2 ) {
        Edge edge = new Edge(EdgeType.MOVE_RELATED, n1, n2);

        nodes.get(n1).add(edge);
        nodes.get(n2).add(edge);
    }

    public void addVariables(CFG cfg, Collection<Assignable> vars ) {
        var iter = vars.iterator();

        List<VariableNode> added = new ArrayList<>(vars.size());

        while( iter.hasNext() ) {
            Assignable var = iter.next();
            var node = nodeResovler.getOrDefault(new VariableNode(var), null);
            if( node == null ) {
                node = new VariableNode(var);
                nodeResovler.put(node, node);
                if( nodes.containsKey(node) ) {
                    throw new RuntimeException("OOOPS");
                }
                nodes.put(node, new HashSet<>());
            }

            for( VariableNode n : added ) {
                addEdge(node, n);
            }

            added.add( node );

            node.useCount = cfg.useCounts.get(var);
        }

    }

    public String asDotGraph() {
        StringBuilder sb = new StringBuilder();
        sb.append("graph Reg {\n");
        sb.append("node [colorscheme=accent8];\n");

        for( var entry : nodes.entrySet() ) {
            VariableNode node = entry.getKey();
            if( node.exclude ) continue;

            sb.append(String.format("%s [shape=circle style=filled", node.var, node.var,node));
            if( node.assignedRegister != null ) {
                sb.append(String.format(" color=%d label=\"%s %d\"", node.assignedRegister+1, node.var, node.assignedRegister));
            }
            else if( node.spill ) {
                sb.append(" colorscheme=x11 color=red");
            }
            sb.append("];\n");
            for( Edge edge : entry.getValue() ) {
                if( edge.n1.equals(node) && !edge.n2.exclude ) {
                    sb.append(String.format("%s -- %s;\n", edge.n1.var, edge.n2.var));
                }
            }
        }

        sb.append("}\n");
        return sb.toString();
    }

    public boolean isEmpty() {
        for( VariableNode node : nodes.keySet() ) {
            if( !node.exclude ) return false;
        }
        return true;
    }

    public VariableNode nodeDegreeLessThan( int k ) {
        for( var entry : nodes.entrySet() ) {
            if( entry.getKey().exclude ) continue;

            int degree = 0;
            for( var edge : entry.getValue() ) {
                if(edge.isExcluded()) continue;
                degree++;
            }

            if( degree < k ) {
                return entry.getKey();
            }
        }

        return null;
    }

    public VariableNode getNode() {
        for( VariableNode node : nodes.keySet() ) {
            if( !node.exclude ) return node;
        }

        return null;
    }

    public VariableNode getNodeHighDegree() {
        VariableNode res = null;
        int maxDegree = -1;

        for( var entry : nodes.entrySet() ) {
            if( entry.getKey().exclude ) continue;

            int degree = 0;
            for( var edge : entry.getValue() ) {
                if(edge.isExcluded()) continue;
                degree++;
            }

            if( degree > maxDegree ) {
                res = entry.getKey();
                maxDegree = degree;
            }
        }

        return res;
    }

    public VariableNode spillHeuristic() {

        // Heuristic Cost Function
        //
        // Want to penalize spilling commonly used variables, even if it has a lot of connections
        BiFunction<Integer, Integer, Integer> scoreFunc = (deg, uses) -> {
            return 2*deg - uses;
        };

        VariableNode res = null;
        int maxScore = Integer.MIN_VALUE;

        for( var entry : nodes.entrySet() ) {
            if( entry.getKey().exclude ) continue;

            int degree = degree(entry.getKey());

            int score = scoreFunc.apply(degree, entry.getKey().useCount);

            if( score > maxScore ) {
                res = entry.getKey();
                maxScore = score;
            }
            else if( score == maxScore && res.useCount > entry.getKey().useCount ) {
                res = entry.getKey();
                maxScore = score;
            }
        }

        return res;
    }

    public int degree( VariableNode node ) {
        int degree = 0;
        for( var edge : nodes.get(node) ) {
            if(edge.isExcluded()) continue;
            degree++;
        }
        return degree;
    }
    public Set<Integer> connections( VariableNode node ) {
        var degree = new HashSet<Integer>();
        for( Edge edge : nodes.get(node) ) {
            VariableNode other;
            if( !edge.n1.equals(node) ) {
                other = edge.n1;
            }
            else {
                other = edge.n2;
            }

            if( other.assignedRegister != null ) {
                degree.add( other.assignedRegister );
            }
        }

        return degree;
    }

    public void resetExclusion() {
        for( VariableNode var : nodes.keySet() ) {
            var.exclude = false;
        }
    }

    public void mergeNodeInfo( RegisterInteferenceGraph other ) {
        for( VariableNode var : other.nodes.keySet() ) {
            if( nodes.containsKey(var) ) {
                VariableNode ourNode = nodeResovler.get(var);
                ourNode.assignedRegister = var.assignedRegister;
                ourNode.spill = var.spill;
                ourNode.exclude = var.exclude;
            }
        }
    }

    public void updateNode( VariableNode node ) {
        VariableNode inRig = nodeResovler.get(node);
        if( inRig == null ) throw new RuntimeException("VariableNode " + node + " is not in RIG");

        inRig.assign(node);
    }
}
