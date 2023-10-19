package ir;

import ast.*;
import ir.cfg.BasicBlock;
import ir.cfg.CFG;

import java.util.Iterator;
import java.util.List;

//Traverse the AST - generate a CFG for each function
public class IRGenerator implements ast.NodeVisitor, Iterable<ir.cfg.CFG>{
 
    private CFG curCFG;
    private BasicBlock curBlock;
    private List<CFG> funcs;

    @Override
    public void visit(Addition add) {

    }

    @Override
    public void visit(ArgList list) {

    }

    @Override
    public void visit(ArrayIndex idx) {

    }

    @Override
    public void visit(Assignment asn) {

    }

    @Override
    public void visit(BoolLiteral bool) {

    }

    @Override
    public void visit(DeclarationList list) {

    }

    @Override
    public void visit(Designator des) {

    }

    @Override
    public void visit(Division div) {

    }

    @Override
    public void visit(FloatLiteral flt) {

    }

    @Override
    public void visit(FuncBody fb) {

    }

    @Override
    public void visit(FuncCall fc) {

    }

    @Override
    public void visit(FuncDecl fd) {

    }

    @Override
    public void visit(IfStat is) {

    }

    @Override
    public void visit(IntegerLiteral il) {

    }

    @Override
    public void visit(LogicalAnd la) {

    }

    @Override
    public void visit(LogicalNot ln) {

    }

    @Override
    public void visit(LogicalOr lo) {

    }

    @Override
    public void visit(Modulo mod) {

    }

    @Override
    public void visit(Multiplication mul) {

    }

    @Override
    public void visit(Power pwr) {

    }

    @Override
    public void visit(Relation rel) {

    }

    @Override
    public void visit(RepeatStat rep) {

    }

    @Override
    public void visit(Return ret) {

    }

    @Override
    public void visit(RootAST root) {

    }

    @Override
    public void visit(StatSeq seq) {

    }

    @Override
    public void visit(Subtraction sub) {

    }

    @Override
    public void visit(VariableDeclaration var) {

    }

    @Override
    public void visit(WhileStat wstat) {

    }

    @Override
    public Iterator<ir.cfg.CFG> iterator() {
        return null;
    }
}