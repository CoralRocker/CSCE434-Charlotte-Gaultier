package ir.cfg;

import ir.tac.DefinedInBlock;
import ir.tac.Phi;
import ir.tac.TAC;
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

        if( !def.isEmpty() ) {
            for( BasicBlock df : blk.domFrontier ) {
                System.out.printf("Adding Vars From BB%d to BB%d\n", blk.getNum(), df.getNum());
                List<TAC> instr = df.getInstructions();

                for( Variable var : def ) {
                    Variable v = new Variable(var.getSym(), 0);
                    Phi phi = new Phi(-1, var, v);
                    instr.add(0, phi);
                }
            }
        }

        for( BasicBlock child : blk.getSuccessors() ) {
            if( !child.visited() ) {
                child.accept(this);
            }
        }

        return null;
    }
}
