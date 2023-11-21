package ir.cfg.registers;

import ir.tac.Assignable;
import ir.tac.Variable;

import java.util.*;


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
        public boolean equals(Object other) {
            if( !(other instanceof Edge) ) {
                return false;
            }

            Edge edge = (Edge) other;

            return (n1.hashCode() == edge.n1.hashCode() && n2.hashCode() == edge.n2.hashCode())
                || (n2.hashCode() == edge.n1.hashCode() && n1.hashCode() == edge.n2.hashCode());
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

    public void addVariables( Collection<Assignable> vars ) {
        var iter = vars.iterator();

        List<VariableNode> added = new ArrayList<>(vars.size());

        while( iter.hasNext() ) {
            Assignable var = iter.next();
            var node = nodeResovler.getOrDefault(var, null);
            if( node == null ) {
                node = new VariableNode(nodeNum++, var);
                nodeResovler.put(node, node);
                nodes.put(node, new HashSet<>());
            }

            for( VariableNode n : added ) {
                addEdge(node, n);
            }

            added.add( node );
        }

    }

    public String asDotGraph() {
        StringBuilder sb = new StringBuilder();
        sb.append("graph Reg {\n");

        for( var entry : nodes.entrySet() ) {
            VariableNode node = entry.getKey();
            sb.append(String.format("%s [shape=oval];\n", node.var));
            for( Edge edge : entry.getValue() ) {
                if( edge.n1.equals(node) ) {
                    sb.append(String.format("%s -- %s;\n", edge.n1.var, edge.n2.var));
                }
            }
        }

        sb.append("}\n");
        return sb.toString();
    }

}
