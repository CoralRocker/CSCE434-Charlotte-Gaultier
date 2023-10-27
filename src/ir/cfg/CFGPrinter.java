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

    private void addf(String format, Object ... objs ) {
        builder.append(String.format(format, objs));
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

            if( !blk.name.isEmpty() ) {
                addf("bb%d [shape=record, label=\"<B> %s \\n BB%d | {", blk.getNum(), blk.name, blk.getNum());
            }
            else {
                addf("bb%d [shape=record, label=\"<B> %s \\n BB%d | {", blk.getNum(), blk.name, blk.getNum());
            }

            List<TAC> instructions = blk.getInstructions();
            Iterator<TAC> iter = instructions.listIterator();

            if( instructions.size() != 1 )
            builder.append("<entry>\n");

            if( !iter.hasNext() )
                builder.append("|<exit>\n");

            while( iter.hasNext() ) {
                TAC tac = iter.next();

                if( !iter.hasNext() )
                    builder.append("<exit>");

                builder.append(String.format("\t%d: %s \n", tac.getId(), tac.genDot()));
                if( iter.hasNext() ) {
                    builder.append('|');
                }



            }

            addLn("}\"];");

            for( BasicBlock block : blk.getSuccessors()) {
                queue.add(block);
                addLnf("bb%d:exit -> bb%d:entry", blk.getNum(), block.getNum());
            }

            if( blk.idom != null ) {
                addLnf("bb%d:exit -> bb%d:entry [style=dotted, color=blue, label=idom];", blk.idom.getNum(), blk.getNum());
            }
            else {
                System.err.println(String.format("Block %d has no idom", blk.getNum()));

            }

        }
        addLn("}");


        return builder.toString();
    }
}
