package ir.cfg;

import ir.tac.TAC;

import java.util.*;

public class BasicBlock extends Block implements Iterable<TAC> {

    private int num; // block number;

    private String label = null;
    private List<TAC> instructions;

    public List<TAC> getInstructions() {
        return instructions;
    }


    private List<BasicBlock> predecessors;
    private List<BasicBlock> successors;


    public List<BasicBlock> getSuccessors() {
        // if( successors.size() > 2 ) {
        //     throw new RuntimeException("Basic Block has more than 2 direct successors!");
        // }
        return Collections.unmodifiableList(successors);
    }

    /**
     * Connect the given block as a successor to this one
     * @param blk
     */
    public void connectAfter( BasicBlock blk ) {
        successors.add( blk );
        if( successors.size() > 2 ) {
            throw new RuntimeException("A basic block cannot have more than two ancestors");
        }
        blk.predecessors.add(this);
    }

    public void disconnectAfter( BasicBlock blk ) {
        successors.remove(blk);
        blk.predecessors.remove(this);
    }

    public boolean isSuccessor( BasicBlock blk ) {
        return successors.contains(blk);
    }

    public boolean isPredecessor( BasicBlock blk ) {
        return predecessors.contains(blk);
    }

    public void connectBefore( BasicBlock blk ) {
        predecessors.add(blk);
        blk.successors.add(this);
    }

    public List<BasicBlock> getPredecessors() {
        return Collections.unmodifiableList(predecessors);
    }

    // public BasicBlock(String name) {
    //     num = -1;
    //     label = name;
    //     this.successors = new ArrayList<>();
    //     this.predecessors = new ArrayList<>();
    //     this.instructions = new ArrayList<>();
    // }

    public BasicBlock(int id, String name) {
        super(name);
        num = id;
        label = "";
        this.successors = new ArrayList<>();
        this.predecessors = new ArrayList<>();
        this.instructions = new ArrayList<>();
    }

    public void add( TAC tac ) {
        instructions.add(tac);
    }

    // public void addPredecessor( BasicBlock block ) {
    //     predecessors.add(block);
    // }

    // public void addSuccessor( BasicBlock block ) {
    //     successors.add(block);
    // }

    // public BasicBlock createSuccessor(int id, String name) {
    //     BasicBlock blck = new BasicBlock(id, name);
    //     successors.add(blck);
    //     blck.addPredecessor(this);
    //     return blck;
    // }

    @Override
    public Iterator<TAC> iterator() {
        return instructions.iterator();
    }

    @Override
    public void resetVisited() {
        visited = false;
    }

    @Override
    public int getNum() {
        return num;
    }

    public void setNum(int i){
        num = i;
    }

    @Override
    public Object accept(CFGVisitor<Object> visitor) {
        visitor.visit(this);
        return null;
    }

    @Override
    public String toString() {
        if( visited ) {
            return String.format("BB%d VISITED", getNum());
        }
        return String.format("BB%d", getNum());
    }

}