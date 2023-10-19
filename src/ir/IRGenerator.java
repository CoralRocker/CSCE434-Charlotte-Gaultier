package ir;

import ast.*;
import ast.Return;
import coco.Symbol;
import coco.Token;
import ir.cfg.BasicBlock;
import ir.cfg.CFG;
import ir.tac.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

//Traverse the AST - generate a CFG for each function
public class IRGenerator implements ast.NodeVisitor<Value>, Iterable<ir.cfg.CFG> {

    private CFG curCFG;
    private BasicBlock curBlock;
    private List<CFG> funcs;

    private Assignable asnDest;

    private int tempNum = 0;
    private int instr = 0;

    private int blockNo = 1;

    public IRGenerator() {
        funcs = new ArrayList<>();
    }


    @Override
    public Value visit(Addition add) {

        Assignable tmpdest = asnDest;

        asnDest = null;
        Value lval = add.getLvalue().accept(this);
        tempNum += 1;
        Value rval = add.getRvalue().accept(this);
        tempNum -= 1;
        asnDest = tmpdest;

        Assignable target = asnDest == null ? new Temporary(tempNum) : asnDest;

        Add tac = new Add(instr++, target, lval, rval);
        curBlock.add(tac);

        return target;
    }

    @Override
    public Value visit(ArgList list) {
        for( AST ast : list.getArgs() ) {
            Value val = ast.accept(this);

        }

        return null;
    }

    @Override
    public Value visit(ArrayIndex idx) {

        return null;
    }

    @Override
    public Value visit(Assignment asn) {

        tempNum = 0;

        Designator dest = (Designator) asn.getTarget();
        Symbol destSym = dest.getSymbol();
        Variable dst = new Variable(destSym);

        AST astSource = asn.getRvalue();
        Value src = null;

        asnDest = dst;
        src = astSource.accept(this);
        asnDest = null;

        if( src != dst ) {
            Store tac = new Store(++instr, dst, src);
            curBlock.add(tac);
        }

        return null;
    }

    @Override
    public Value visit(BoolLiteral bool) {

        return new Literal(bool);
    }

    @Override
    public Value visit(DeclarationList list) {

        return null;
    }

    @Override
    public Value visit(Designator des) {
        return new Variable(des.getSymbol());
    }

    @Override
    public Value visit(Division div) {

        Assignable tmpdest = asnDest;

        asnDest = null;
        Value lval = div.getLvalue().accept(this);
        tempNum += 1;
        Value rval = div.getRvalue().accept(this);
        tempNum -= 1;
        asnDest = tmpdest;

        Assignable target = asnDest == null ? new Temporary(tempNum) : asnDest;

        Div tac = new Div(instr++, target, lval, rval);
        curBlock.add(tac);

        return target;
    }

    @Override
    public Value visit(FloatLiteral flt) {
        return new Literal(flt);
    }

    @Override
    public Value visit(FuncBody fb) {

        return null;
    }

    @Override
    public Value visit(FuncCall fc) {

        fc.getArgs().accept(this);

        Call tac = new Call(instr++, fc.getFunc());
        curBlock.add(tac);

        return null;
    }

    @Override
    public Value visit(FuncDecl fd) {
        return null;
    }

    @Override
    public Value visit(IfStat is) {

        Value val = is.getIfrel().accept(this);

        Branch bra = new Branch(instr++, is.getIfrel().token().lexeme());
        if( val instanceof Variable ) {
            Cmp cmp = new Cmp(instr++, val, new Literal(new BoolLiteral(new Token(Token.Kind.TRUE, 0, 0))));
            curBlock.add(cmp);
            bra.setRel("==");
        }


        curBlock.add(bra);

        BasicBlock ifblock, elseblock = null, entryBlock = curBlock;
        ifblock = new BasicBlock(blockNo++);
        ifblock.addPredecessor(curBlock);
        curBlock.addSuccessor(ifblock);

        bra.setDestination(ifblock);

        if( is.getElseseq() != null ) {
            elseblock = new BasicBlock(blockNo++);
            elseblock.addPredecessor(curBlock);
            curBlock.addSuccessor(elseblock);
        }


        curBlock = ifblock;
        is.getIfseq().accept(this);
        if( is.getElseseq() != null ) {
            curBlock = elseblock;
            is.getElseseq().accept(this);
        }

        BasicBlock nextBlock = new BasicBlock(blockNo++);

        ifblock.addSuccessor(nextBlock);
        nextBlock.addPredecessor(ifblock);
        if( elseblock != null ) {
            elseblock.addSuccessor(nextBlock);
            nextBlock.addPredecessor(elseblock);
        }
        else {
            entryBlock.addSuccessor(nextBlock);
        }



        curBlock = nextBlock;

        return null;

    }

    @Override
    public Value visit(IntegerLiteral il) {
        return new Literal(il);
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

        Assignable tmpdest = asnDest;

        asnDest = null;
        Value lval = mod.getLvalue().accept(this);
        tempNum += 1;
        Value rval = mod.getRvalue().accept(this);
        tempNum -= 1;
        asnDest = tmpdest;

        Assignable target = asnDest == null ? new Temporary(tempNum) : asnDest;

        Mod tac = new Mod(instr++, target, lval, rval);
        curBlock.add(tac);

        return target;
    }

    @Override
    public Value visit(Multiplication mul) {

        Assignable tmpdest = asnDest;

        asnDest = null;
        Value lval = mul.getLvalue().accept(this);
        tempNum+=1;
        Value rval = mul.getRvalue().accept(this);
        tempNum-=1;
        asnDest = tmpdest;

        Assignable target = asnDest == null ? new Temporary(tempNum) : asnDest;

        Mul tac = new Mul(instr++, target, lval, rval);
        curBlock.add(tac);

        return target;

    }

    @Override
    public Value visit(Power pwr) {

        Assignable tmpdest = asnDest;

        asnDest = null;
        Value lval = pwr.getLvalue().accept(this);
        Value rval = pwr.getRvalue().accept(this);
        asnDest = tmpdest;

        Assignable target = asnDest == null ? new Temporary(tempNum) : asnDest;

        Pow tac = new Pow(instr++, target, lval, rval);
        curBlock.add(tac);

        return target;
    }

    @Override
    public Value visit(Relation rel) {
        tempNum = 0;

        Value lval = rel.getLvalue().accept(this);
        Value rval = rel.getRvalue().accept(this);

        Cmp cmp = new Cmp(instr++, lval, rval );
        curBlock.add(cmp);

        switch( rel.token().kind() ) {
            case GREATER_EQUAL -> {}
            case GREATER_THAN -> {}
            case LESS_EQUAL -> {}
            case LESS_THAN -> {}
            case EQUAL_TO -> {}
            case NOT_EQUAL -> {}
        }


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

        curBlock = new BasicBlock(blockNo++);
        curCFG = new CFG(curBlock);

        root.getSeq().accept(this);

        return null;
    }

    @Override
    public Value visit(StatSeq seq) {
        for( AST ast : seq.getSequence() ) {
            tempNum = 0;
            ast.accept(this);
        }

        return null;
    }

    @Override
    public Value visit(Subtraction sub) {
        Assignable tmpdest = asnDest;

        asnDest = null;
        Value lval = sub.getLvalue().accept(this);
        tempNum += 1;
        Value rval = sub.getRvalue().accept(this);
        tempNum -= 1;
        asnDest = tmpdest;

        Assignable target = asnDest == null ? new Temporary(tempNum) : asnDest;

        Sub tac = new Sub(instr++, target, lval, rval);
        curBlock.add(tac);

        return target;
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
