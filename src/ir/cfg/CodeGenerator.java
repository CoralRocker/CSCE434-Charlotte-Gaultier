package ir.cfg;

import ir.tac.TAC;
import ir.tac.TACVisitor;
import ir.tac.CodeBlockGenerator;
import ir.tac.Variable;

import java.util.HashSet;


public class CodeGenerator extends CFGVisitor<String> {
    private CFG cfg;
    private String instrList;

    public CodeGenerator(CFG cfg){
        this.cfg = cfg;
        cfg.markUnvisited();
        cfg.breadthFirst((BasicBlock b) -> {
            instrList = instrList + this.visit(b);
        });
    }
    @Override
    public String visit(BasicBlock blk) {
        return CodeBlockGenerator.generate(blk);
    }

    public String getInstrList(){
        return instrList;
    }
}
