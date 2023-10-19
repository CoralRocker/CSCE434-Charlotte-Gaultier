package ir.cfg;

import ir.tac.TAC;

import java.util.Iterator;
import java.util.List;

public class BasicBlock extends Block implements Iterable<TAC> {

    private int num; // block number;
    private List<TAC> instructions;


    private List<BasicBlock> predecessors;
    private List<BasicBlock> successors;
    
    
    
    @Override
    public Iterator<TAC> iterator() {
        return instructions.iterator();
    }

    @Override
    public void resetVisited() {

    }

    @Override
    public void accept(CFGVisitor visitor) {

    }
}