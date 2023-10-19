package ir.cfg;

import ir.tac.TAC;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

// To print basic block in Dot language
public class CFGPrinter extends CFGVisitor {

    private StringBuilder builder;

    private CFG cfg;

    public CFGPrinter() {
        builder = new StringBuilder();
    }

    private void addLn(String f) {
        builder.append(f);
        builder.append('\n');
    }

    private void addLnf(String format, Object ... objs ) {
        builder.append(String.format(format, objs));
        builder.append('\n');
    }

    public String genDotGraph(CFG cfg) {
        this.cfg = cfg;

        addLn("digraph G {");

        Queue<BasicBlock> queue = new LinkedList<>();

        queue.add(cfg.getHead());

        int num = 1;
        while( !queue.isEmpty() ) {
            BasicBlock blk = queue.remove();

            addLnf("bb%d [shape=record, label=\"<b>BB%d | {", num, num++);

            Iterator<TAC> instructions = blk.getInstructions().listIterator();

            while( instructions.hasNext() ) {
                TAC tac = instructions.next();

                builder.append(String.format("%d: %s", tac.getId(), tac.genDot()));
                if( instructions.hasNext() ) {
                    builder.append('|');
                }
            }

            addLn("}\"];");
            addLn("}");
        }


        return builder.toString();
    }

}
