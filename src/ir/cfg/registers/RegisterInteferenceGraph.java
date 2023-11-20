package ir.cfg.registers;

import ir.tac.Variable;

import java.util.*;


class AdjacencyGraph<E> {

    private ArrayList<E> matrix;
    private int N;

    public int getSize() {
        return (N * (N-1)) / 2;
    }

    private int index(int i, int j) {
        if( i <= j ) {
            throw new IllegalArgumentException(String.format("Cannot get index at (row: %d, col: %d): Row number must be greater than Column number"));
        }

        return (i * (i - 1) / 2) + j;
    }

    public E get(int i, int j) {
        return matrix.get( index(i, j) );
    }

    public AdjacencyGraph(int numNodes) {
        N = numNodes;
        matrix = new ArrayList<>(getSize());
        for( int i = 0; i < getSize(); i++ ) {
            matrix.set(i, null );
        }
    }

    public AdjacencyGraph(int numNodes, E defaultval) {
        N = numNodes;
        matrix = new ArrayList<>(getSize());
        for( int i = 0; i < getSize(); i++ ) {
            matrix.set(i, defaultval );
        }
    }

    public void setEdge( int i, int j, E val ) {
        matrix.set(index(i, j), val);
    }

    public void addNode() {
        N++;
        int size = getSize();
        while( matrix.size() < size ) {
            matrix.add(null);
        }
    }

    public void addNodes(int n) {
        if( n < 0 ) throw new IllegalArgumentException("Cannot add negative amount of nodes");
        N += n;
        int size = getSize();
        while( matrix.size() < size ) {
            matrix.add(null);
        }
    }

}

public class RegisterInteferenceGraph {

    public enum EdgeType {
        NONE,
        INTERFERE,
        MOVE_RELATED;
    }

    private AdjacencyGraph<EdgeType> adjacency;
    private HashMap<VariableNode, VariableNode> nodes;
    private int nodeNum;


    public RegisterInteferenceGraph(List<Variable> nodes) {
        adjacency = new AdjacencyGraph<>(nodes.size(), EdgeType.NONE);
        this.nodes = new HashMap<>();
        nodeNum = 0;

        addVariables(nodes);
    }

    public void addEdge(VariableNode n1, VariableNode n2 ) {
        if( n1.ID < n2.ID ) {
            adjacency.setEdge(n2.ID, n1.ID, EdgeType.INTERFERE );
        }
        else {
            adjacency.setEdge(n1.ID, n2.ID, EdgeType.INTERFERE );
        }
    }
    public void addMoveEdge(VariableNode n1, VariableNode n2 ) {
        if( n1.ID < n2.ID ) {
            adjacency.setEdge(n2.ID, n1.ID, EdgeType.MOVE_RELATED );
        }
        else {
            adjacency.setEdge(n1.ID, n2.ID, EdgeType.MOVE_RELATED );
        }
    }

    public void addVariables( List<Variable> vars ) {
        var iter = vars.listIterator();

        List<VariableNode> added = new ArrayList<>(vars.size());

        while( iter.hasNext() ) {
            Variable var = iter.next();
            var node = nodes.getOrDefault(var, null);
            if( node == null ) {
                node = new VariableNode(nodeNum++, var);
                adjacency.addNode();
                nodes.put( node, node );
            }

            for( VariableNode n : added ) {
                addEdge(node, n);
            }

            added.add( node );
        }

    }

}
