package ir.cfg;

import ir.tac.TAC;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
        return successors;
    }

    public List<BasicBlock> getPredecessors() {
        return predecessors;
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

    public void addPredecessor( BasicBlock block ) {
        predecessors.add(block);
    }

    public void addSuccessor( BasicBlock block ) {
        successors.add(block);
    }

    public BasicBlock createSuccessor(int id, String name) {
        BasicBlock blck = new BasicBlock(id, name);
        successors.add(blck);
        blck.addPredecessor(this);
        return blck;
    }

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