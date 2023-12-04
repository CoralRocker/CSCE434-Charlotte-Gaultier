package ir;

import ast.*;
import ast.ArrayIndex;
import ast.Return;
import coco.Symbol;
import coco.VariableSymbol;
import coco.*;

import ir.cfg.BasicBlock;
import ir.cfg.CFG;
import ir.cfg.SSACreator;
import ir.tac.*;
import ir.tac.Variable;
import types.AryType;
import types.VoidType;

import java.sql.Array;
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
        if( lval instanceof ArrayValue ) {
            ArrayValue arr = (ArrayValue) lval;
            lval = new Temporary(tempNum++);
            curBlock.add( arr.genLoad(curCFG, (Assignable) lval) );
        }
        tempNum += 1;
        Value rval = add.getRvalue().accept(this);
        if( rval instanceof ArrayValue ) {
            ArrayValue arr = (ArrayValue) rval;
            rval = new Temporary(tempNum++);
            curBlock.add( arr.genLoad(curCFG, (Assignable) rval) );
        }
        tempNum -= 1;
        asnDest = tmpdest;

        Assignable target = asnDest == null ? new Temporary(tempNum) : asnDest;

        Add tac = new Add(curCFG.instrNumberer.push(), target, lval, rval);
        curBlock.add(tac);

        return target;
    }

    @Override
    public Value visit(ArgList list) {
        List<Assignable> args = new ArrayList<>();
        for( AST ast : list.getArgs() ) {
            Value val = ast.accept(this);

            if( val instanceof Assignable ) {
                args.add( (Assignable) val );
            }
            else if( val instanceof ArrayValue ) {
                ArrayValue arrval = (ArrayValue) val;
                Temporary dest = new Temporary(tempNum++);
                Load ld = arrval.genLoad(curCFG, dest); // new Load(curCFG.instrNumberer.push(), dest, arrval.array, arrval.offset);
                curBlock.add(ld);
                args.add( dest );
            }
            else {
                var temp = new Temporary(tempNum++);
                curBlock.add(new Store(curCFG.instrNumberer.push(), temp, val));
                args.add(temp);
            }
        }

        list.argTAC = args;

        return null;
    }

    @Override
    public Value visit(ArrayIndex idx) {

        Value index = idx.getIndex().accept(this);
        tempNum += 1;
        Value array = idx.getArray().accept(this);

        if( array instanceof Variable ) {
            ArrayValue arrval = new ArrayValue();
            arrval.array = new Variable(((Variable)array).getSym());
            arrval.dimensions = new LinkedList<>(((AryType)idx.getArray().getType()).getDimensions());

            // Pop front of dimensions
            arrval.dimensions.removeFirst();
            int perLine = arrval.dimensions.stream().mapToInt(i -> i).reduce(1, (a, b) -> a * b);
            arrval.offset = new Temporary(tempNum++);

            Mul mul = new Mul(curCFG.instrNumberer.push(), arrval.offset, index, Literal.get(perLine * -4));
            curBlock.add(mul);

            // System.out.printf("Array val: %s\n", arrval);
            return arrval;
        }
        else if( array instanceof ArrayValue ) {
            ArrayValue arrval = (ArrayValue) array;

            arrval.dimensions.removeFirst();

            if( arrval.dimensions.isEmpty() ) {
                Temporary multemp = new Temporary(tempNum);
                Mul mul = new Mul(curCFG.instrNumberer.push(), multemp, index, Literal.get(-4) );
                curBlock.add(mul);
                Add add = new Add(curCFG.instrNumberer.push(), arrval.offset, arrval.offset,  multemp);
                curBlock.add(add);

                return arrval;
            }
            else {
                int perLine = arrval.dimensions.stream().mapToInt(i -> i).reduce(1, (a, b) -> a * b);
                Temporary newOffset = new Temporary(tempNum++);
                Mul mul = new Mul(curCFG.instrNumberer.push(), newOffset, index, Literal.get(perLine * -4)); // perLine + index items * 4 bytes / item
                curBlock.add(mul);
                Add add = new Add(curCFG.instrNumberer.push(), arrval.offset, arrval.offset, newOffset );
                curBlock.add(add);

            }

            return arrval;
        }
        else {
            throw new RuntimeException("Array return not Variable or ArrayValue?");
        }

    }

    @Override
    public Value visit(Assignment asn) {
        Symbol destSym = null;
        Assignable dst = null;
        tempNum = 0;

        AST astSource = asn.getRvalue();
        Value src = null;

        asnDest = dst;

        src = astSource.accept(this);
        if( src == null ) {
            throw new RuntimeException(String.format("%s does not work!", astSource));
        }
        if( src instanceof ArrayValue ) {
            ArrayValue arrval = (ArrayValue) src;
            src = new Temporary(tempNum++);
            Load ld = arrval.genLoad(curCFG, (Assignable) src);
            curBlock.add(ld);
        }
        asnDest = null;

        if (asn.getTarget() instanceof Designator){
            Designator dest = (Designator) asn.getTarget();
            destSym = dest.getSymbol();

            destSym.isInitialized = true;

            dst = new Variable(destSym, instr);
        }else if(asn.getTarget() instanceof ast.ArrayIndex){

            // ARRAY CASE
            ArrayValue arrval = (ArrayValue) asn.getTarget().accept(this);

            StoreStack store = arrval.genStore(curCFG, src); // new StoreStack(curCFG.instrNumberer.push(), arrval.array, src, arrval.offset);
            curBlock.add( store );

            return null;
        }

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
        if( list.getContained().get(0) instanceof VariableDeclaration ) {
            if( curCFG.getSymbols() == null ) {
                curCFG.setSymbols(new HashMap<>());
            }
        }

        // if( curCFG.getSymbols() != null && !(list.getContained().get(0) instanceof FuncDecl) ) {
        //     throw new RuntimeException("Symbols list already made for CFG " + curCFG.toString());
        // }
        // else if( list.getContained().get(0) instanceof VariableDeclaration ) {
        //     curCFG.setSymbols(new HashMap<>());
        // }

        for( AST decl : list.getContained() ) {
            decl.accept(this);
        }

        return null;
    }

    @Override
    public Value visit(Designator des) {
        // TODO: Initialization use-def chain
        // if(des.getSymbol() instanceof VariableSymbol){
        //     var varsym = ((VariableSymbol)des.getSymbol());
        //     if(!varsym.isInitialized && !varsym.isFunctionParam ){
        //         // if variable is uninitialized
        //         switch(des.type()){
        //             case "int":
        //                 Store tac = new Store(curCFG.instrNumberer.push(), new Variable(des.getSymbol()), new Literal(new IntegerLiteral(des.token(), 0)));
        //                 curBlock.add(tac);
        //                 break;
        //             case "float":
        //                 Store tac3 = new Store(curCFG.instrNumberer.push(), new Variable(des.getSymbol()), new Literal(new FloatLiteral(des.token(), 0)));
        //                 curBlock.add(tac3);
        //                 break;
        //             case "bool":
        //                 Store tac2 = new Store(curCFG.instrNumberer.push(), new Variable(des.getSymbol()), new Literal(new BoolLiteral(des.token(), false)));
        //                 curBlock.add(tac2);
        //                 break;
        //         }
        //         varsym.isInitialized = true;
        // }}
        return new Variable(des.getSymbol());
    }

    @Override
    public Value visit(Division div) {

        Assignable tmpdest = asnDest;

        asnDest = null;
        Value lval = div.getLvalue().accept(this);
        if( lval instanceof ArrayValue ) {
            ArrayValue arr = (ArrayValue) lval;
            lval = new Temporary(tempNum++);
            curBlock.add( arr.genLoad(curCFG, (Assignable) lval) );
        }
        tempNum += 1;
        Value rval = div.getRvalue().accept(this);
        if( rval instanceof ArrayValue ) {
            ArrayValue arr = (ArrayValue) rval;
            rval = new Temporary(tempNum++);
            curBlock.add( arr.genLoad(curCFG, (Assignable) rval) );
        }
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

        Assignable retval = asnDest;
        Call tac;
        if( retval == null ) {
            retval = new Temporary(tempNum++);
        }
        tac = new Call(curCFG.instrNumberer.push(), fc.getFunc(), retval, fc.getArgs().argTAC);
        curBlock.add(tac);

        curCFG.func.calls.add((FunctionSymbol)fc.getFunc());

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

        curCFG = new CFG(curBlock, fd.getSymbol().typeSignatures(), fd);
        curCFG.func = fd.getSymbol();

        curCFG.instrNumberer.newBlock(curBlock.getNum());
        funcs.add(curCFG);

        HashMap<VariableSymbol, VariableSymbol> syms;
        if( curCFG.getSymbols() == null ) {
            syms = new HashMap<>();
            curCFG.setSymbols(syms);
        }
        else {
            syms = curCFG.getSymbols();
        }

        for( var var : fd.getArgList() ) {
            syms.put((VariableSymbol) var, (VariableSymbol) var);
            ((VariableSymbol) var).isFunctionParam = true;
        }

        // add curCFG to funcs list

        // visit function body
        fd.getBody().accept(this);

        curCFG.genAllNodes();

        // Add main symbols, respecting shadowing
        if( parent.getSymbols() != null ) {
            for (VariableSymbol var : parent.getSymbols().keySet() ) {
                if (!syms.containsKey(var)) {
                    syms.put(var, var);
                }
            }
        }

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
        if( val instanceof ArrayValue ) {
            ArrayValue arr = (ArrayValue) val;
            val = new Temporary(tempNum++);
            curBlock.add( arr.genLoad(curCFG, (Assignable) val) );
        }

        BasicBlock nextBlock = new BasicBlock(-1, "Post-If"); // Block After If and Else
        BasicBlock ifblock = new BasicBlock(-1, "If"); // Block with if case
        BasicBlock endIf = null; // Last block of if case
        BasicBlock elseblock = new BasicBlock(-1, "Else"); // Block with else case
        BasicBlock endElse = null; // Last block of else case
        BasicBlock cmpblock = curBlock; // Block with original CMP

        boolean isVariable = false;
        if( !(val instanceof Assignable) ) {
            var temp = new Temporary(tempNum++);
            cmpblock.add( new Store(curCFG.instrNumberer.push(), temp, val ));
            val = temp;
            isVariable = true;
        }
        Branch bra = new Branch(curCFG.instrNumberer.push(), is.getIfrel().token().lexeme());
        bra.setVal((Assignable) val);
        bra.setDestination(ifblock);
        if( isVariable ) bra.setRel("!=");
        cmpblock.add( bra );
        if( is.getElseseq() != null ) {
            bra = new Branch(curCFG.instrNumberer.push(), "");
            bra.setDestination(elseblock);
            cmpblock.add(bra);
            cmpblock.connectAfter(elseblock);
        }
        else {
            bra = new Branch(curCFG.instrNumberer.push(), "");
            bra.setDestination(nextBlock);
            cmpblock.add(bra);
            cmpblock.connectAfter(nextBlock);
        }

        /*
            IF CASE
         */

        cmpblock.connectAfter(ifblock);
        ifblock.setNum(blockNo++);
        curBlock = ifblock;
        is.getIfseq().accept(this);
        endIf = curBlock;

        bra = new Branch(curCFG.instrNumberer.push(), "");
        bra.setDestination(nextBlock);
        endIf.add( bra );
        endIf.connectAfter(nextBlock);

        /*
            ELSE CASE
         */
        if( is.getElseseq() != null ) {
            elseblock.setNum(blockNo++);
            curBlock = elseblock;
            is.getElseseq().accept(this);
            endElse = curBlock;

            bra = new Branch(curCFG.instrNumberer.push(), "");
            bra.setDestination(nextBlock);
            endElse.add( bra );
            endElse.connectAfter(nextBlock);
        }

        curBlock = nextBlock;
        nextBlock.setNum(blockNo++);

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
        if( lval instanceof ArrayValue ) {
            ArrayValue arr = (ArrayValue) lval;
            lval = new Temporary(tempNum++);
            curBlock.add( arr.genLoad(curCFG, (Assignable) lval) );
        }
        tempNum += 1;
        Value rval = la.getRvalue().accept(this);
        if( rval instanceof ArrayValue ) {
            ArrayValue arr = (ArrayValue) rval;
            rval = new Temporary(tempNum++);
            curBlock.add( arr.genLoad(curCFG, (Assignable) rval) );
        }
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
        if( val instanceof ArrayValue ) {
            ArrayValue arr = (ArrayValue) val;
            val = new Temporary(tempNum++);
            curBlock.add( arr.genLoad(curCFG, (Assignable) val) );
        }
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
        if( lval instanceof ArrayValue ) {
            ArrayValue arr = (ArrayValue) lval;
            lval = new Temporary(tempNum++);
            curBlock.add( arr.genLoad(curCFG, (Assignable) lval) );
        }
        tempNum += 1;
        Value rval = lo.getRvalue().accept(this);
        if( rval instanceof ArrayValue ) {
            ArrayValue arr = (ArrayValue) rval;
            rval = new Temporary(tempNum++);
            curBlock.add( arr.genLoad(curCFG, (Assignable) rval) );
        }
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
        if( lval instanceof ArrayValue ) {
            ArrayValue arr = (ArrayValue) lval;
            lval = new Temporary(tempNum++);
            curBlock.add( arr.genLoad(curCFG, (Assignable) lval) );
        }
        tempNum += 1;
        Value rval = mod.getRvalue().accept(this);
        if( rval instanceof ArrayValue ) {
            ArrayValue arr = (ArrayValue) rval;
            rval = new Temporary(tempNum++);
            curBlock.add( arr.genLoad(curCFG, (Assignable) rval) );
        }
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
        if( lval instanceof ArrayValue ) {
            ArrayValue arr = (ArrayValue) lval;
            lval = new Temporary(tempNum++);
            curBlock.add( arr.genLoad(curCFG, (Assignable) lval) );
        }
        tempNum+=1;
        Value rval = mul.getRvalue().accept(this);
        if( rval instanceof ArrayValue ) {
            ArrayValue arr = (ArrayValue) rval;
            rval = new Temporary(tempNum++);
            curBlock.add( arr.genLoad(curCFG, (Assignable) rval) );
        }
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
        if( lval instanceof ArrayValue ) {
            ArrayValue arr = (ArrayValue) lval;
            lval = new Temporary(tempNum++);
            curBlock.add( arr.genLoad(curCFG, (Assignable) lval) );
        }
        Value rval = pwr.getRvalue().accept(this);
        if( rval instanceof ArrayValue ) {
            ArrayValue arr = (ArrayValue) rval;
            rval = new Temporary(tempNum++);
            curBlock.add( arr.genLoad(curCFG, (Assignable) rval) );
        }
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
        if( lval instanceof ArrayValue ) {
            ArrayValue arr = (ArrayValue) lval;
            lval = new Temporary(tempNum);
            curBlock.add( arr.genLoad(curCFG, (Assignable) lval) );
        }
        Value rval = rel.getRvalue().accept(this);
        if( rval instanceof ArrayValue ) {
            ArrayValue arr = (ArrayValue) rval;
            rval = new Temporary(tempNum);
            curBlock.add( arr.genLoad(curCFG, (Assignable) rval) );
        }
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
        curBlock.connectAfter(repBlk);
        // curBlock.addSuccessor(repBlk);
        // repBlk.addPredecessor(curBlock);
        Branch bra = new Branch(curCFG.instrNumberer.push(), "");
        bra.setDestination(repBlk);
        curBlock.add(bra);

        curBlock = repBlk;
        curCFG.instrNumberer.newBlock(repBlk);

        rep.getSeq().accept(this);

        if( rep.getRelation() instanceof Relation )
            ((Relation) rep.getRelation()).isBranchRel = true;
        Value val = rep.getRelation().accept(this);
        if( val instanceof ArrayValue ) {
            ArrayValue arr = (ArrayValue) val;
            val = new Temporary(tempNum++);
            curBlock.add( arr.genLoad(curCFG, (Assignable) val) );
        }

        // Get the inverse of the relation (to restart loop)
        postRep.setNum(blockNo++);
        Branch braEnd = new Branch(curCFG.instrNumberer.push(), rep.getRelation().token().lexeme());
        braEnd.invertRelation();
        bra = new Branch(curCFG.instrNumberer.push(), "");
        braEnd.setVal((Assignable) val); // TODO: Make work for boolean var / literal
        braEnd.setDestination(repBlk);
        bra.setDestination(postRep);
        curBlock.add( braEnd );
        curBlock.add( bra );

        // Add path back to start of loop
        curBlock.connectAfter(repBlk);
        // curBlock.addSuccessor(( repBlk ));
        // repBlk.addPredecessor( curBlock );

        // Add exit path
        curBlock.connectAfter(postRep);
        // postRep.addPredecessor( curBlock );
        // curBlock.addSuccessor( postRep );

        curBlock = postRep;
        curCFG.instrNumberer.newBlock(postRep);

        return null;
    }

    @Override
    public Value visit(Return ret) {

        if( ret.getReturn() == null ) {
            curBlock.add( new ir.tac.Return(curCFG.instrNumberer.push(), null));
            return null;
        }
        Value v = ret.getReturn().accept(this);
        if( v instanceof ArrayValue ) {
            ArrayValue arr = (ArrayValue) v;
            v = new Temporary(tempNum++);
            curBlock.add( arr.genLoad(curCFG, (Assignable) v) );
        }
        curBlock.add( new ir.tac.Return(curCFG.instrNumberer.push(), v) );

        return null;
    }

    @Override
    public Value visit(RootAST root) {

        BasicBlock tmpBlk = new BasicBlock(0, "Main");
        CFG tmpCFG = new CFG(tmpBlk, "main", null);

        tmpCFG.func = new FunctionSymbol("main", new ArrayType(Token.Kind.VOID));
        tmpCFG.func.setCalled(true);
        curCFG = tmpCFG;
        mainFunc = tmpCFG;

        // TODO Functions
        //
        if( root.getVars() != null )
            root.getVars().accept(this);


        int i = 0;
        if( curCFG.getSymbols() != null ) {
            for( var sym : curCFG.getSymbols().keySet() ) {
                sym.globalLoc = i++; // Global counter
                if(curCFG.cfgID.equals("main")) sym.isGlobal = true;
            }
        }
        else {
            curCFG.setSymbols(new HashMap<>());
        }

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

        // System.out.println(curCFG.asDotGraph());

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
        if( lval instanceof ArrayValue ) {
            ArrayValue arr = (ArrayValue) lval;
            lval = new Temporary(tempNum++);
            curBlock.add( arr.genLoad(curCFG, (Assignable) lval) );
        }
        tempNum += 1;
        Value rval = sub.getRvalue().accept(this);
        if( rval instanceof ArrayValue ) {
            ArrayValue arr = (ArrayValue) rval;
            rval = new Temporary(tempNum++);
            curBlock.add( arr.genLoad(curCFG, (Assignable) rval) );
        }
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
        if( wstat.getRelation() instanceof Relation )
            ((Relation) wstat.getRelation()).isBranchRel = true;
        Value stmt = wstat.getRelation().accept(this);
        if( stmt instanceof ArrayValue ) {
            ArrayValue arr = (ArrayValue) stmt;
            stmt = new Temporary(tempNum++);
            curBlock.add( arr.genLoad(curCFG, (Assignable) stmt) );
        }

        BasicBlock loopBlk = new BasicBlock(blockNo++, "While"),
                   postLoop = new BasicBlock(-1, "Post-While");

        curCFG.instrNumberer.newBlock(loopBlk);
        if( !(stmt instanceof Assignable) ) {
            Temporary tmp = new Temporary( tempNum++);
            Store str = new Store(curCFG.instrNumberer.push(), tmp, stmt);
            stmt = tmp;
            curBlock.add(str);
            throw new RuntimeException(String.format("While relation returned %s not Assignable", stmt));
        }
        Branch braEnd = new Branch(curCFG.instrNumberer.push(), wstat.getRelation().token().lexeme());
        braEnd.invertRelation(); // Branch if relation is false
        braEnd.setVal((Assignable) stmt);
        braEnd.setDestination(postLoop);

        Branch failCond = new Branch(curCFG.instrNumberer.push(), "");
        failCond.setDestination(loopBlk);

        curBlock.add(braEnd);
        curBlock.add(failCond);

        tempNum = 0;

        // Can Either go to loop or post loop
        curBlock.connectAfter(loopBlk);
        // curBlock.addSuccessor(loopBlk);
        // loopBlk.addPredecessor(curBlock);

        curBlock.connectAfter(postLoop);
        // curBlock.addSuccessor(postLoop);
        // postLoop.addPredecessor(curBlock);

        // Loop inherits from this

        // Post Loop Inherits from This and Loopblk
        // postLoop.addPredecessor(loopBlk);

        // Loop Block falls through to post loop
        //loopBlk.addSuccessor(postLoop);

        curBlock = loopBlk;
        wstat.getSeq().accept(this);
        tempNum = 0;

        stmt = wstat.getRelation().accept(this);
        if( stmt instanceof ArrayValue ) {
            ArrayValue arr = (ArrayValue) stmt;
            stmt = new Temporary(tempNum++);
            curBlock.add( arr.genLoad(curCFG, (Assignable) stmt) );
        }
        if( !(stmt instanceof Assignable) ) {
            Temporary tmp = new Temporary( tempNum++);
            Store str = new Store(curCFG.instrNumberer.push(), tmp, stmt);
            stmt = tmp;
            curBlock.add(str);
            throw new RuntimeException(String.format("While relation returned %s not Assignable (second time??)", stmt));
        }
        braEnd = new Branch(curCFG.instrNumberer.push(), wstat.getRelation().token().lexeme());
        braEnd.setVal((Assignable) stmt);
        braEnd.setDestination(loopBlk);

        failCond = new Branch(curCFG.instrNumberer.push(), "");
        failCond.setDestination(postLoop);
        curBlock.add(braEnd);
        curBlock.add(failCond);

        curBlock.connectAfter(loopBlk);
        // curBlock.addSuccessor(loopBlk);
        // loopBlk.addPredecessor(curBlock);

        curBlock.connectAfter(postLoop);
        // postLoop.addPredecessor(curBlock);
        // curBlock.addSuccessor(postLoop);



        curBlock = postLoop;
        postLoop.setNum(blockNo++);
        curCFG.instrNumberer.newBlock(postLoop);

        // System.out.println(curCFG.asDotGraph());
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

    public void removeOrphans() {
        //

        markCalls(mainFunc.func);
        var iter = funcs.listIterator();
        while( iter.hasNext() ) {
            var func = iter.next();
            if( !func.func.getIsCalled() ) {
                iter.remove();
            }
        }
        // for (int i = 0; i < funcs.size(); i++){
        //     if(!funcs.get(i).func.getIsCalled()){
        //         funcs.remove(i);
        //     }
        // }
    }

    public void markCalls(FunctionSymbol sym){
        for(FunctionSymbol call : sym.calls){
            if(!call.getIsCalled()){
                call.setCalled(true);
                markCalls(call);
            }
        }
    }
}
