package ir.cfg;

import ir.tac.TAC;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

// To print basic block in Dot language
public class CFGPrinter {

    private StringBuilder builder;

    private CFG cfg;

    public CFGPrinter(CFG cfg) {
        builder = new StringBuilder();
        this.cfg = cfg;
    }

    private void addLn(String f) {
        builder.append(f);
        builder.append('\n');
    }

    private void addLnf(String format, Object ... objs ) {
        builder.append(String.format(format, objs));
        builder.append('\n');
    }

    public String genDotGraph() {

        addLn("digraph G {");

        Queue<BasicBlock> queue = new LinkedList<>();

        queue.add(cfg.getHead());

        while( !queue.isEmpty() ) {
            BasicBlock blk = queue.remove();

            if( blk.visited() ) {
                continue;
            }

            blk.markVisited();

            addLnf("bb%d [shape=record, label=\"<b>BB%d | {", blk.getNum(), blk.getNum());

            Iterator<TAC> instructions = blk.getInstructions().listIterator();

            while( instructions.hasNext() ) {
                TAC tac = instructions.next();

                builder.append(String.format("\t%d: %s\n", tac.getId(), tac.genDot()));
                if( instructions.hasNext() ) {
                    builder.append('|');
                }



            }

            addLn("}\"];");

            for( BasicBlock block : blk.getSuccessors()) {
                queue.add(block);
                addLnf("bb%d -> bb%d", blk.getNum(), block.getNum());
            }

            if( blk.idom != null ) {
                addLnf("bb%d -> bb%d [style=dotted, color=blue, label=idom];", blk.idom.getNum(), blk.getNum());
            }

            // for( BasicBlock dom : blk.dom ) {
            //     addLnf("bb%d -> bb%d [style=dotted, color=blue, label=dom];", blk.getNum(), dom.getNum());
            // }
        }
        addLn("}");


        return builder.toString();
    }
}
