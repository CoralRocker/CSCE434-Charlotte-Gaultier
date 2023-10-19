package ir;

import ast.*;
import coco.Symbol;
import ir.cfg.BasicBlock;
import ir.cfg.CFG;
import ir.tac.Literal;
import ir.tac.Store;
import ir.tac.Value;
import ir.tac.Variable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

//Traverse the AST - generate a CFG for each function
public class IRGenerator implements ast.NodeVisitor<Value>, Iterable<ir.cfg.CFG> {

    private CFG curCFG;
    private BasicBlock curBlock;
    private List<CFG> funcs;

    private Variable asnDest;

    private int tempNum = 0;
    private int instr = 0;

    public IRGenerator() {
        funcs = new ArrayList<>();
    }


    @Override
    public Value visit(Addition add) {
        AST lval = add.getLvalue();
        AST rval = add.getRvalue();


        return null;
    }

    @Override
    public Value visit(ArgList list) {

        return null;
    }

    @Override
    public Value visit(ArrayIndex idx) {

        return null;
    }

    @Override
    public Value visit(Assignment asn) {

        Designator dest = (Designator) asn.getTarget();
        Symbol destSym = dest.getSymbol();
        Variable dst = new Variable(destSym);

        AST astSource = asn.getRvalue();
        Value src = null;

        if( astSource instanceof ast.BoolLiteral ) {
            src = new Literal(astSource);
        }
        else if( astSource instanceof ast.IntegerLiteral ) {
            src = new Literal(astSource);
        }
        else if( astSource instanceof ast.FloatLiteral ) {
            src = new Literal(astSource);
        }
        else if( astSource instanceof ast.Designator ) {
            src = new Variable( ((Designator) astSource).getSymbol() );
        }
        else {
            asnDest = dst;
            astSource.accept(this);
            asnDest = null;
            return null;
        }


        Store tac = new Store(++instr, dst, src);
        curBlock.add(tac);

        return null;
    }

    @Override
    public Value visit(BoolLiteral bool) {

        return null;
    }

    @Override
    public Value visit(DeclarationList list) {

        return null;
    }

    @Override
    public Value visit(Designator des) {

        return null;
    }

    @Override
    public Value visit(Division div) {

        return null;
    }

    @Override
    public Value visit(FloatLiteral flt) {

        return null;
    }

    @Override
    public Value visit(FuncBody fb) {

        return null;
    }

    @Override
    public Value visit(FuncCall fc) {

        return null;
    }

    @Override
    public Value visit(FuncDecl fd) {
        return null;
    }

    @Override
    public Value visit(IfStat is) {
        return null;

    }

    @Override
    public Value visit(IntegerLiteral il) {

        return null;
    }

    @Override
    public Value visit(LogicalAnd la) {

        return null;
    }

    @Override
    public Value visit(LogicalNot ln) {

        return null;
    }

    @Override
    public Value visit(LogicalOr lo) {

        return null;
    }

    @Override
    public Value visit(Modulo mod) {

        return null;
    }

    @Override
    public Value visit(Multiplication mul) {

        return null;
    }

    @Override
    public Value visit(Power pwr) {

        return null;
    }

    @Override
    public Value visit(Relation rel) {

        return null;
    }

    @Override
    public Value visit(RepeatStat rep) {

        return null;
    }

    @Override
    public Value visit(Return ret) {

        return null;
    }

    @Override
    public Value visit(RootAST root) {

        // TODO Functions

        // TODO Vars

        curBlock = new BasicBlock("main");
        curCFG = new CFG(curBlock);

        root.getSeq().accept(this);

        return null;
    }

    @Override
    public Value visit(StatSeq seq) {
        for( AST ast : seq.getSequence() ) {
            ast.accept(this);
        }

        return null;
    }

    @Override
    public Value visit(Subtraction sub) {

        return null;
    }

    @Override
    public Value visit(VariableDeclaration var) {
        return null;

    }

    @Override
    public Value visit(WhileStat wstat) {
        return null;
    }

    @Override
    public Iterator<ir.cfg.CFG> iterator() {
        return null;
    }

    public CFG getCurCFG() {
        return curCFG;
    }
}
