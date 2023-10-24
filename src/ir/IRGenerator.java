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
    private int instr = 1;

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
        Variable dst = new Variable(destSym, instr);

        AST astSource = asn.getRvalue();
        Value src = null;

        asnDest = dst;
        src = astSource.accept(this);
        asnDest = null;

        if( src != dst ) {
            Store tac = new Store(instr++, dst, src);
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
        for( AST decl : list.getContained() ) {
            decl.accept(this);
        }
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
        if( fb.getVarList() != null )
            fb.getVarList().accept(this);
        fb.getSeq().accept(this);
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
        // save curCFG as parent
        CFG parent = curCFG;
        // update curCFG to ths func
        // unsure if this is the right way to deal w block
        curBlock = new BasicBlock(blockNo++);
        curCFG = new CFG(curBlock);
        funcs.add(curCFG);
        // add curCFG to funcs list

        // visit function body
        fd.getBody().accept(this);

        // reset curCFG to parent
        curCFG = parent;
        // TODO remove the curCFG resetting stuff, is unnecessary
        // reset block number?? unsure
        blockNo = 1;
        return null;
    }

    @Override
    public Value visit(IfStat is) {

        Value val = is.getIfrel().accept(this);

        Branch bra = new Branch(0, is.getIfrel().token().lexeme());
        if( val instanceof Variable ) {
            Temporary storage = new Temporary(tempNum++);
            Cmp cmp = new Cmp(instr++,
                              val,
                              new Literal(new BoolLiteral(new Token(Token.Kind.TRUE, 0, 0))),
                              storage,
                              "eq" );
            curBlock.add(cmp);
            bra.setRel("==");
            bra.setVal( storage );
        }
        else {
            bra.setVal(val);
        }

        bra.setId( instr++ );

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

        // tempNum = 0;
        Assignable tempdest = asnDest;

        asnDest = null;
        Value lval = rel.getLvalue().accept(this);
        Value rval = rel.getRvalue().accept(this);
        asnDest = tempdest;

        Assignable target = asnDest == null ? new Temporary(tempNum++) : asnDest;

        String op = null;
        switch( rel.token().kind() ) {
            case GREATER_EQUAL -> { op = "ge"; }
            case GREATER_THAN -> { op = "gt"; }
            case LESS_EQUAL -> { op = "le"; }
            case LESS_THAN -> { op = "lt"; }
            case EQUAL_TO -> { op = "eq"; }
            case NOT_EQUAL -> { op = "ne"; }
        }

        Cmp cmp = new Cmp(instr++, lval, rval, target, op );
        curBlock.add(cmp);

        return target;
    }

    @Override
    public Value visit(RepeatStat rep) {

        BasicBlock repBlk = new BasicBlock(blockNo++);
        curBlock.addSuccessor(repBlk);
        repBlk.addPredecessor(curBlock);
        curBlock = repBlk;

        rep.getSeq().accept(this);

        Value val = rep.getRelation().accept(this);

        Branch braEnd = new Branch(instr++, "!=");
        braEnd.setVal(val);
        braEnd.setDestination(repBlk);
        curBlock.add( braEnd );

        // Add path back to start of loop
        curBlock.addSuccessor(( repBlk ));
        repBlk.addPredecessor( curBlock );

        // Add exit path
        BasicBlock postRep = new BasicBlock(blockNo++);
        postRep.addPredecessor( curBlock );
        curBlock.addSuccessor( postRep );
        curBlock = postRep;

        return null;
    }

    @Override
    public Value visit(Return ret) {

        return null;
    }

    @Override
    public Value visit(RootAST root) {

        // TODO Functions
        //
        if( root.getVars() != null )
            root.getVars().accept(this);
        if( root.getFuncs() != null ) {
            root.getFuncs().accept(this);
        }
        // TODO Vars

        curBlock = new BasicBlock(blockNo++);
        curCFG = new CFG(curBlock);
        funcs.add(curCFG);

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

        tempNum = 0;
        Value stmt = wstat.getRelation().accept(this);

        BasicBlock loopBlk = new BasicBlock(blockNo++),
                   postLoop = new BasicBlock(-1);

        Temporary cmpStart = new Temporary(tempNum++);
        Cmp cmp = new Cmp(instr++, stmt, Literal.get(false), cmpStart, "eq" );
        Branch braEnd = new Branch(instr++, "==");
        braEnd.setVal(cmpStart);
        braEnd.setDestination(postLoop);

        curBlock.add(cmp);
        curBlock.add(braEnd);

        tempNum = 0;

        // Can Either go to loop or post loop
        curBlock.addSuccessor(loopBlk);
        curBlock.addSuccessor(postLoop);

        // Loop inherits from this
        loopBlk.addPredecessor(curBlock);

        // Post Loop Inherits from This and Loopblk
        postLoop.addPredecessor(curBlock);
        postLoop.addPredecessor(loopBlk);

        // Loop Block falls through to post loop
        loopBlk.addSuccessor(postLoop);

        curBlock = loopBlk;
        wstat.getSeq().accept(this);
        tempNum = 0;

        stmt = wstat.getRelation().accept(this);
        cmpStart = new Temporary(tempNum++);
        cmp = new Cmp(instr++, stmt, Literal.get(true), cmpStart, "eq" );
        braEnd = new Branch(instr++, "==");
        braEnd.setVal(cmpStart);
        braEnd.setDestination(loopBlk);
        curBlock.add(cmp);
        curBlock.add(braEnd);
        curBlock.addSuccessor(loopBlk);
        loopBlk.addPredecessor(curBlock);

        curBlock = postLoop;
        postLoop.setNum(blockNo++);

        return null;
    }

    @Override
    public Iterator<ir.cfg.CFG> iterator() {
        return null;
    }

    public CFG getCurCFG() {
        return curCFG;
    }

    public CFG getMainCFG() {
        return funcs.get( funcs.size()-1 );
    }

    public List<CFG> getAllCFGs() {
        return funcs;
    }
}
