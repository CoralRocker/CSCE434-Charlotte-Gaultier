package ir;

import java.util.List;

//Traverse the AST - generate a CFG for each function
public class IRGenerator implements ast.NodeVisitor, Iterable<ir.cfg.CFG>{
 
    private CFG curCFG;
    private List<CFG> funcs;
}
