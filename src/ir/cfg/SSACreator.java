package ir.cfg;

import ir.tac.DefinedInBlock;
import ir.tac.Variable;

import java.util.List;

public class SSACreator extends CFGVisitor<Object> {

    public void modify( CFG cfg ) {
        cfg.markUnvisited();
        visit( cfg.getHead() );
    }

    @Override
    public Object visit(BasicBlock blk) {
        blk.markVisited();

        List<Variable> def = DefinedInBlock.defInBlock(blk);
        System.out.printf("BB%d Defines the Following: \n", blk.getNum());
        for( Variable var : def ) {
            System.out.printf("\t%s\n", var);
        }


        for( BasicBlock child : blk.getSuccessors() ) {
            if( !child.visited() ) {
                child.accept(this);
            }
        }

        return null;
    }
}
