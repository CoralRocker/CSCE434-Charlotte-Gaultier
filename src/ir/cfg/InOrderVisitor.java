package ir.cfg;

import java.util.LinkedList;
import java.util.Queue;

public class InOrderVisitor extends CFGVisitor<Void> {

    private Queue<BasicBlock> queue = new LinkedList<>();

    @Override
    public Void visit(BasicBlock blk) {
        blk.markVisited();



        return null;
    }
}
