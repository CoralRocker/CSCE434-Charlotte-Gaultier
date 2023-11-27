package ir;

import ast.*;
import ast.Return;
import coco.Symbol;
import coco.Token;
import coco.VariableSymbol;
import ir.cfg.BasicBlock;
import ir.cfg.CFG;
import ir.tac.*;

import java.util.*;

//Traverse the AST - generate a CFG for each function
public class IRGenerator implements ast.NodeVisitor<Value>, Iterable<ir.cfg.CFG> {

    private CFG curCFG;
    private BasicBlock curBlock;
    private List<CFG> funcs;
    private CFG mainFunc;

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

        Add tac = new Add(curCFG.instrNumberer.push(), target, lval, rval);
        curBlock.add(tac);

        return target;
    }

    @Override
    public Value visit(ArgList list) {
        tempNum = 0;
        List<Assignable> args = new ArrayList<>();
        int ctr = -1;
        for( AST ast : list.getArgs() ) {
            ctr++;
            tempNum = ctr;
            Value val = ast.accept(this);

            if( val instanceof Temporary && ((Temporary)val).num == ctr ) {
                args.add((Assignable) val);
                continue;
            }
            else {
                tempNum = ctr;
                var temp = new Temporary(tempNum);
                curBlock.add(new Store(curCFG.instrNumberer.push(), temp, val));
                args.add(temp);
            }
        }

        list.argTAC = args;

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
        if(destSym instanceof VariableSymbol){
            ((VariableSymbol) destSym).isInitialized = true;
        }
        Variable dst = new Variable(destSym, instr);

        AST astSource = asn.getRvalue();
        Value src = null;

        asnDest = dst;
        src = astSource.accept(this);
        if( src == null ) {
            throw new RuntimeException(String.format("%s does not work!", astSource));
        }
        asnDest = null;


        if( src != dst ) {
            Store tac = new Store(curCFG.instrNumberer.push(), dst, src);
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

        if( curCFG.getSymbols() != null && !(list.getContained().get(0) instanceof FuncDecl) ) {
            throw new RuntimeException("Symbols list already made for CFG " + curCFG.toString());
        }
        else if( list.getContained().get(0) instanceof VariableDeclaration ) {
            curCFG.setSymbols(new HashMap<>());
        }

        for( AST decl : list.getContained() ) {
            decl.accept(this);
        }

        return null;
    }

    @Override
    public Value visit(Designator des) {
        if(des.getSymbol() instanceof VariableSymbol){
            if(!((VariableSymbol)(des.getSymbol())).isInitialized){
                // if variable is uninitialized
                switch(des.type()){
                    case "int":
                        Store tac = new Store(curCFG.instrNumberer.push(), new Variable(des.getSymbol()), new Literal(new IntegerLiteral(des.token(), 0)));
                        curBlock.add(tac);
                        break;
                    case "float":
                        Store tac3 = new Store(curCFG.instrNumberer.push(), new Variable(des.getSymbol()), new Literal(new FloatLiteral(des.token(), 0)));
                        curBlock.add(tac3);
                        break;
                    case "bool":
                        Store tac2 = new Store(curCFG.instrNumberer.push(), new Variable(des.getSymbol()), new Literal(new BoolLiteral(des.token(), false)));
                        curBlock.add(tac2);
                        break;
                }
                ((VariableSymbol)(des.getSymbol())).isInitialized = true;
        }}
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

        Div tac = new Div(curCFG.instrNumberer.push(), target, lval, rval);
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


        tempNum = 0;
        Assignable retval = asnDest;
        Call tac;
        if( retval == null ) {
            retval = new Temporary(tempNum++);
        }
        tac = new Call(curCFG.instrNumberer.push(), fc.getFunc(), retval, fc.getArgs().argTAC);
        curBlock.add(tac);

        //  Call Returns in temporary
        return retval;
    }

    @Override
    public Value visit(FuncDecl fd) {
        // save curCFG as parent
        CFG parent = curCFG;
        // update curCFG to ths func
        // unsure if this is the right way to deal w block
        curBlock = new BasicBlock(blockNo++, fd.funcName());
        curCFG = new CFG(curBlock, fd.getSymbol().typeSignatures());
        curCFG.instrNumberer.newBlock(curBlock.getNum());
        funcs.add(curCFG);
        // add curCFG to funcs list

        // visit function body
        fd.getBody().accept(this);

        curCFG.genAllNodes();

        // reset curCFG to parent
        curCFG = parent;
        // TODO remove the curCFG resetting stuff, is unnecessary
        // reset block number?? unsure
        blockNo = 1;
        return null;
    }

    @Override
    public Value visit(IfStat is) {

        if( is.getIfrel() instanceof Relation )
            ((Relation) is.getIfrel()).isBranchRel = true;

        Value val = is.getIfrel().accept(this);

        BasicBlock nextBlock = new BasicBlock(-1, "Post-If");
        BasicBlock ifblock = new BasicBlock(-1, "If");

        Branch bra = new Branch(curCFG.instrNumberer.push(), is.getIfrel().token().lexeme());
        if( val instanceof Variable ) {
            Temporary storage = new Temporary(tempNum++);
            Cmp cmp = new Cmp(curCFG.instrNumberer.push(),
                              val,
                              new Literal(new BoolLiteral(new Token(Token.Kind.TRUE, 0, 0))),
                              storage,
                              "eq" );
            // If We're at the start of the block "rehead" it. Else just move back
            if( curBlock.getInstructions().isEmpty() )
                cmp.getIdObj().moveToBlockFront(curBlock.getNum());
            else
                cmp.getIdObj().moveRelative( -1 );

            curBlock.add(cmp);
            bra.setRel("==");
            bra.setVal( storage );
        }
        else {
            bra.setVal(val);
        }

        bra.getIdObj().moveToEnd();
        bra.setDestination(ifblock);
        curBlock.add(bra);
        Branch elsebra = new Branch(curCFG.instrNumberer.push(), "");
        elsebra.setDestination(nextBlock);
        curBlock.add(elsebra);

        BasicBlock elseblock = null, entryBlock = curBlock;
        ifblock.setNum(blockNo++);
        ifblock.addPredecessor(curBlock);
        curBlock.addSuccessor(ifblock);


        if( is.getElseseq() != null ) {
            elseblock = new BasicBlock(blockNo++, "Else");
            elseblock.addPredecessor(curBlock);
            curBlock.addSuccessor(elseblock);
        }
        nextBlock.setNum(blockNo++);


        curBlock = ifblock;
        curCFG.instrNumberer.newBlock(ifblock.getNum());
        is.getIfseq().accept(this);
        bra = new Branch(curCFG.instrNumberer.push(), "");
        bra.setDestination(nextBlock);
        curBlock.add( bra );

        if( is.getElseseq() != null ) {
            curBlock = elseblock;
            curCFG.instrNumberer.newBlock(elseblock.getNum());
            is.getElseseq().accept(this);
            bra = new Branch(curCFG.instrNumberer.push(), "");
            bra.setDestination(nextBlock);
            curBlock.add( bra );
            elsebra.setDestination(elseblock);
        }


        ifblock.addSuccessor(nextBlock);
        nextBlock.addPredecessor(ifblock);
        if( elseblock != null ) {
            elseblock.addSuccessor(nextBlock);
            nextBlock.addPredecessor(elseblock);
        }
        else {
            entryBlock.addSuccessor(nextBlock);
            nextBlock.addPredecessor(entryBlock);
        }

        curBlock = nextBlock;
        curCFG.instrNumberer.newBlock(nextBlock.getNum());

        return null;

    }

    @Override
    public Value visit(IntegerLiteral il) {
        return new Literal(il);
    }

    @Override
    public Value visit(LogicalAnd la) {
        Assignable tmpdest = asnDest;

        asnDest = null;
        Value lval = la.getLvalue().accept(this);
        tempNum += 1;
        Value rval = la.getRvalue().accept(this);
        tempNum -= 1;
        asnDest = tmpdest;

        Assignable target = asnDest == null ? new Temporary(tempNum) : asnDest;

        And tac = new And(curCFG.instrNumberer.push(), target, lval, rval);
        curBlock.add(tac);

        return target;
    }

    @Override
    public Value visit(LogicalNot ln) {
        Assignable tmpdest = asnDest;

        asnDest = null;
        Value val = ln.getRvalue().accept(this);
        asnDest = tmpdest;

        Assignable target = asnDest == null ? new Temporary(tempNum) : asnDest;

        Not tac = new Not(curCFG.instrNumberer.push(), target, val);
        curBlock.add(tac);

        return target;
    }

    @Override
    public Value visit(LogicalOr lo) {
        Assignable tmpdest = asnDest;

        asnDest = null;
        Value lval = lo.getLvalue().accept(this);
        tempNum += 1;
        Value rval = lo.getRvalue().accept(this);
        tempNum -= 1;
        asnDest = tmpdest;

        Assignable target = asnDest == null ? new Temporary(tempNum) : asnDest;

        Or tac = new Or(curCFG.instrNumberer.push(), target, lval, rval);
        curBlock.add(tac);

        return target;
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

        Mod tac = new Mod(curCFG.instrNumberer.push(), target, lval, rval);
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

        Mul tac = new Mul(curCFG.instrNumberer.push(), target, lval, rval);
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

        Pow tac = new Pow(curCFG.instrNumberer.push(), target, lval, rval);
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


        Cmp cmp = new Cmp(curCFG.instrNumberer.push(), lval, rval, target, "" );
        curBlock.add(cmp);

        if( !rel.isBranchRel ) {
            switch( rel.token().kind() ) {
                case EQUAL_TO -> {
                    curBlock.add( new And(curCFG.instrNumberer.push(), target, target, Literal.get(1) ) );
                    curBlock.add( new Xor(curCFG.instrNumberer.push(), target, target, Literal.get(1) ) );
                }
                case NOT_EQUAL -> {
                    curBlock.add( new And(curCFG.instrNumberer.push(), target, target, Literal.get(1) ) );
                }
                case GREATER_EQUAL -> {
                    curBlock.add( new Add(curCFG.instrNumberer.push(), target, target, Literal.get(2) ) );
                    curBlock.add( new Lsh(curCFG.instrNumberer.push(), target, target, Literal.get(-1) ) );
                }
                case GREATER_THAN -> {
                    curBlock.add( new Add(curCFG.instrNumberer.push(), target, target, Literal.get(1) ) );
                    curBlock.add( new Lsh(curCFG.instrNumberer.push(), target, target, Literal.get(-1) ) );
                }
                case LESS_EQUAL -> {
                    curBlock.add( new Sub(curCFG.instrNumberer.push(), target, target, Literal.get(1) ) );
                    curBlock.add( new Lsh(curCFG.instrNumberer.push(), target, target, Literal.get(-31) ) );
                }
                case LESS_THAN -> {
                    curBlock.add( new Lsh(curCFG.instrNumberer.push(), target, target, Literal.get(-31) ) );
                }
            }
        }

        return target;
    }

    @Override
    public Value visit(RepeatStat rep) {

        BasicBlock repBlk = new BasicBlock(blockNo++, "Repeat");
        BasicBlock postRep = new BasicBlock(-1, "");
        curBlock.addSuccessor(repBlk);
        repBlk.addPredecessor(curBlock);
        Branch bra = new Branch(curCFG.instrNumberer.push(), "");
        bra.setDestination(repBlk);
        curBlock.add(bra);

        curBlock = repBlk;
        curCFG.instrNumberer.newBlock(repBlk);

        rep.getSeq().accept(this);

        Value val = rep.getRelation().accept(this);

        // Get the inverse of the relation (to restart loop)
        String op = null;
        switch( rep.getRelation().token().kind() ) {
            case GREATER_EQUAL -> { op = "<"; }
            case GREATER_THAN -> { op = "<="; }
            case LESS_EQUAL -> { op = ">"; }
            case LESS_THAN -> { op = ">="; }
            case EQUAL_TO -> { op = "!="; }
            case NOT_EQUAL -> { op = "=="; }
        }
        postRep.setNum(blockNo++);
        Branch braEnd = new Branch(curCFG.instrNumberer.push(), op);
        bra = new Branch(curCFG.instrNumberer.push(), "");
        braEnd.setVal(val);
        braEnd.setDestination(repBlk);
        bra.setDestination(postRep);
        curBlock.add( braEnd );
        curBlock.add( bra );

        // Add path back to start of loop
        curBlock.addSuccessor(( repBlk ));
        repBlk.addPredecessor( curBlock );

        // Add exit path
        postRep.addPredecessor( curBlock );
        curBlock.addSuccessor( postRep );
        curBlock = postRep;
        curCFG.instrNumberer.newBlock(postRep);

        return null;
    }

    @Override
    public Value visit(Return ret) {

        Value v = ret.getReturn().accept(this);
        curBlock.add( new ir.tac.Return(curCFG.instrNumberer.push(), v) );

        return null;
    }

    @Override
    public Value visit(RootAST root) {

        BasicBlock tmpBlk = new BasicBlock(0, "Main");
        CFG tmpCFG = new CFG(tmpBlk, "main");


        curCFG = tmpCFG;
        mainFunc = tmpCFG;

        // TODO Functions
        //
        if( root.getVars() != null )
            root.getVars().accept(this);
        if( root.getFuncs() != null ) {
            root.getFuncs().accept(this);
        }
        // TODO Vars

        curCFG = tmpCFG;
        curBlock = tmpBlk;
        tmpBlk.setNum(blockNo++);
        funcs.add(curCFG);

        curCFG.instrNumberer.newBlock(tmpBlk.getNum());

        root.getSeq().accept(this);

        curCFG.genAllNodes();

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

        Sub tac = new Sub(curCFG.instrNumberer.push(), target, lval, rval);
        curBlock.add(tac);

        return target;
    }

    @Override
    public Value visit(VariableDeclaration var) {
        curCFG.getSymbols().put((VariableSymbol) var.symbol(), (VariableSymbol) var.symbol());
        return null;
    }

    @Override
    public Value visit(WhileStat wstat) {

        tempNum = 0;
        Value stmt = wstat.getRelation().accept(this);

        BasicBlock loopBlk = new BasicBlock(blockNo++, "While"),
                   postLoop = new BasicBlock(-1, "Post-While");

        curCFG.instrNumberer.newBlock(loopBlk);
        Temporary cmpStart = new Temporary(tempNum++);
        Cmp cmp = new Cmp(curCFG.instrNumberer.push(), stmt, Literal.get(false), cmpStart, "eq" );
        Branch braEnd = new Branch(curCFG.instrNumberer.push(), "==");
        braEnd.setVal(cmpStart);
        braEnd.setDestination(postLoop);

        Branch failCond = new Branch(curCFG.instrNumberer.push(), "");
        failCond.setDestination(loopBlk);

        curBlock.add(cmp);
        curBlock.add(braEnd);
        curBlock.add(failCond);

        tempNum = 0;

        // Can Either go to loop or post loop
        curBlock.addSuccessor(loopBlk);
        curBlock.addSuccessor(postLoop);

        // Loop inherits from this
        loopBlk.addPredecessor(curBlock);

        // Post Loop Inherits from This and Loopblk
        postLoop.addPredecessor(curBlock);
        // postLoop.addPredecessor(loopBlk);

        // Loop Block falls through to post loop
        //loopBlk.addSuccessor(postLoop);

        curBlock = loopBlk;
        wstat.getSeq().accept(this);
        tempNum = 0;

        stmt = wstat.getRelation().accept(this);
        cmpStart = new Temporary(tempNum++);
        cmp = new Cmp(curCFG.instrNumberer.push(), stmt, Literal.get(true), cmpStart, "eq" );
        braEnd = new Branch(curCFG.instrNumberer.push(), "==");
        braEnd.setVal(cmpStart);
        braEnd.setDestination(loopBlk);

        failCond = new Branch(curCFG.instrNumberer.push(), "");
        failCond.setDestination(postLoop);
        curBlock.add(cmp);
        curBlock.add(braEnd);
        curBlock.add(failCond);
        curBlock.addSuccessor(loopBlk);
        curBlock.addSuccessor(postLoop);
        loopBlk.addPredecessor(curBlock);
        postLoop.addPredecessor(curBlock);



        curBlock = postLoop;
        postLoop.setNum(blockNo++);
        curCFG.instrNumberer.newBlock(postLoop);

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
        return mainFunc;
    }

    public List<CFG> getAllCFGs() {
        return funcs;
    }
}
