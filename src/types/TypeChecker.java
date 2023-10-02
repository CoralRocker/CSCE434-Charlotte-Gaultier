package types;

import ast.*;
import coco.ArrayType;
import coco.FunctionSymbol;
import coco.Symbol;
import coco.Token;

import java.util.ArrayList;
import java.util.Objects;

public class TypeChecker implements NodeVisitor {


    private StringBuilder errorBuffer;
    private FunctionSymbol currentFunction = null;
    private FuncCall currentFuncCall = null;

    public TypeChecker(){
        errorBuffer = new StringBuilder();
    }
    /*
     * Useful error strings:
     *
     * "Call with args " + argTypes + " matches no function signature."
     * "Call with args " + argTypes + " matches multiple function signatures."
     *
     * "IfStat requires relation condition not " + cond.getClass() + "."
     * "WhileStat requires relation condition not " + cond.getClass() + "."
     * "RepeatStat requires relation condition not " + cond.getClass() + "."
     *
     * "Function " + currentFunction.name() + " returns " + statRetType + " 3instead of " + funcRetType + "."
     *
     * "Variable " + var.name() + " has invalid type " + var.type() + "."
     * "Array " + var.name() + " has invalid base type " + baseType + "."
     *
     *
     * "Function " + currentFunction.name() + " has a void arg at pos " + i + "."
     * "Function " + currentFunction.name() + " has an error in arg at pos " + i + ": " + ((ErrorType) t).message())
     * "Not all paths in function " + currentFunction.name() + " return."
     */

    public boolean check(AST ast) {
        visit((RootAST) ast);
        return !hasError();
    }

    private void reportError(Token tkn, String message ) {
        reportError(tkn.lineNumber(), tkn.charPosition(), message);
    }

    private void reportError (int lineNum, int charPos, String message) {
        errorBuffer.append("TypeError(" + lineNum + "," + charPos + ")");
        errorBuffer.append("[" + message + "]" + "\n");
    }

    public boolean hasError () {
        return errorBuffer.length() != 0;
    }


    public String errorReport () {
        return errorBuffer.toString();
    }

//    @Override
//    public void visit (Computation node) {
//        throw new RuntimeException("implement visit (Computation)");
//    }

    @Override
    public void visit(Addition add) {
        add.getLvalue().accept(this);
        add.getRvalue().accept(this);

        add.setType(add.getLvalue().typeClass().add(add.getRvalue().typeClass()));
        if(add.typeClass() instanceof ErrorType){
            reportError(add.lineNumber(), add.charPosition(), ((ErrorType) add.typeClass()).message);
        }
    }

    @Override
    public void visit(ArgList list) {
        for( AST ast : list.getArgs() ) {
            ast.accept(this);
        }
    }

    @Override
    public void visit(ArrayIndex idx) {
        AST arr = idx.getArray();
        AST index = idx.getIndex();
        arr.accept(this);
        index.accept(this);
        Type arrType = arr.typeClass();
        Type idxType = index.typeClass();

        boolean err = false;
        Integer constIdx = null;

        if( !idxType.tryDeref().equals(new IntType()) ) {
            String msg = String.format("Cannot index %s with %s.", arrType.tryDeref(), idxType);
            if( currentFuncCall != null ) {
                reportError(currentFuncCall.getEndParen(), msg);
            }
            else {
                reportError(index.token(), msg);
            }
            idx.setType(new ErrorType(msg));

            err = true;
        }
        else {
            AST cval = index.constEvaluate();
            if( cval instanceof IntegerLiteral ) {
                if( cval.getIntLiteral() < 0) {
                   System.err.println("Negative Array Index Here");
                }
            }
        }

        if( !(arrType.tryDeref() instanceof AryType) ) {
            String msg = String.format("Cannot index %s with %s.", arrType, idxType);
            reportError(arr.token(), msg);
            idx.setType(new ErrorType(msg));

            err = true;
            return;
        }

        if( arrType instanceof ErrorType ) {
            String msg = String.format("Cannot dereference %s", arrType);
            reportError(arr.token(), msg);
            idx.setType(new ErrorType(msg));

            err = true;
        }
        else if( idx.typeClass() instanceof ErrorType ) {
            String msg = String.format("Cannot dereference %s", idx.typeClass());
            reportError(arr.token(), msg);
            idx.setType(new ErrorType(msg));

            err = true;

        }

        if( !err ) {
            idx.setType(new PtrType(((AryType) arrType.tryDeref()).popDimension()));
        }
    }

    @Override
    public void visit(Assignment asn) {
        asn.getTarget().accept(this);
        asn.getRvalue().accept(this);

        asn.setType(asn.getTarget().typeClass().assign(asn.getRvalue().typeClass()));
        if(asn.typeClass() instanceof ErrorType){
            reportError(asn.lineNumber(), asn.charPosition(), ((ErrorType) asn.typeClass()).message);
        }
    }

    @Override
    public void visit(BoolLiteral bool) {

    }

    @Override
    public void visit(DeclarationList list) {
        for( AST decl : list.getContained() ) {
            decl.accept(this);
        }
    }

    @Override
    public void visit(Designator des) {
        des.setType(des.typeClass().deref());
        if(des.typeClass() instanceof ErrorType){
            reportError(des.lineNumber(), des.charPosition(), ((ErrorType) des.typeClass()).message);
        }
    }

    @Override
    public void visit(Division div) {
        div.getLvalue().accept(this);
        div.getRvalue().accept(this);

        div.setType(div.getLvalue().typeClass().div(div.getRvalue().typeClass()));
        if(div.typeClass() instanceof ErrorType){
            reportError(div.lineNumber(), div.charPosition(), ((ErrorType) div.typeClass()).message);
        }
    }

    @Override
    public void visit(FloatLiteral flt) {

    }

    @Override
    public void visit(FuncBody fb) {
        if( fb.getVarList() != null )
            fb.getVarList().accept(this);
        fb.getSeq().accept(this);
    }

    @Override
    public void visit(FuncCall fc) {

        FuncCall oldFc = currentFuncCall;
        currentFuncCall = fc;

        fc.getArgs().accept(this);

        ArrayList<AST> args = fc.getArgs().getArgs();
        TypeList params = new TypeList();
        for( AST ast : args ) {
            params.append(ast.typeClass());
        }

        currentFuncCall = oldFc;

        FunctionSymbol func = (FunctionSymbol) fc.getFunc();
        TypeList good = null;
        for( TypeList list : func.getTypeLists() ) {
            if( list.equals(params) ) {
                if( good != null ) {
                    throw new RuntimeException("Already found good type list!");
                }
                good = list;
            }
        }

        if( good == null ) {
            reportError(fc.token().lineNumber(), fc.token().charPosition(), String.format("Call with args %s matches no function signature.", params));
        }
    }

    @Override
    public void visit(FuncDecl fd) {
        FunctionSymbol parent = currentFunction;
        currentFunction = fd.getSymbol();
        // fd.typeClass is epected return type
        // save current func (parent) and reset current back to parent when we leave
        // need to set current function to this
        fd.getBody().accept(this);
        // old implementation
        currentFunction = parent;
    }

    @Override
    public void visit(IfStat is) {
        is.getIfrel().accept(this);
        if(!(is.getIfrel().typeClass().tryDeref() instanceof BoolType)) {
            reportError(is.lineNumber(), is.charPosition(), "IfStat requires bool condition not " + is.getIfrel().typeClass() + ".");
        }
        is.getIfseq().accept(this);
        if( is.getElseseq() != null ) {
            is.getElseseq().accept(this);
        }
    }

    @Override
    public void visit(IntegerLiteral il) {

    }

    @Override
    public void visit(LogicalAnd la) {
        la.getLvalue().accept(this);
        la.getRvalue().accept(this);

        la.setType(la.getLvalue().typeClass().and(la.getRvalue().typeClass()));
        if(la.typeClass() instanceof ErrorType){
            reportError(la.lineNumber(), la.charPosition(), ((ErrorType) la.typeClass()).message);
        }
    }

    @Override
    public void visit(LogicalNot ln) {
        ln.getRvalue().accept(this);
        ln.setType(ln.getRvalue().typeClass().not());
        if(ln.typeClass() instanceof ErrorType){
            reportError(ln.lineNumber(), ln.charPosition(), ((ErrorType) ln.typeClass()).message);
        }
    }

    @Override
    public void visit(LogicalOr lo) {
        lo.getLvalue().accept(this);
        lo.getRvalue().accept(this);

        lo.setType(lo.getLvalue().typeClass().or(lo.getRvalue().typeClass()));
        if(lo.typeClass() instanceof ErrorType){
            reportError(lo.lineNumber(), lo.charPosition(), ((ErrorType) lo.typeClass()).message);
        }
    }

    @Override
    public void visit(Modulo mod) {
        mod.getLvalue().accept(this);
        mod.getRvalue().accept(this);

        mod.setType(mod.getLvalue().typeClass().mod(mod.getRvalue().typeClass()));
        if(mod.typeClass() instanceof ErrorType){
            reportError(mod.lineNumber(), mod.charPosition(), ((ErrorType) mod.typeClass()).message);
        }
    }

    @Override
    public void visit(Multiplication mul) {
        mul.getLvalue().accept(this);
        mul.getRvalue().accept(this);

        mul.setType(mul.getLvalue().typeClass().mul(mul.getRvalue().typeClass()));
        if(mul.typeClass() instanceof ErrorType){
            reportError(mul.lineNumber(), mul.charPosition(), ((ErrorType) mul.typeClass()).message);
        }
    }

    @Override
    public void visit(Power pwr) {
        pwr.getLvalue().accept(this);
        pwr.getRvalue().accept(this);

        pwr.setType(pwr.getLvalue().typeClass().pwr(pwr.getRvalue().typeClass()));
        if(pwr.typeClass() instanceof ErrorType){
            reportError(pwr.lineNumber(), pwr.charPosition(), ((ErrorType) pwr.typeClass()).message);
        }
    }

    @Override
    public void visit(Relation rel) {
        rel.getLvalue().accept(this);
        rel.getRvalue().accept(this);

        rel.setType(rel.getLvalue().typeClass().compare(rel.getRvalue().typeClass()));
        if(rel.typeClass() instanceof ErrorType){
            reportError(rel.lineNumber(), rel.charPosition(), ((ErrorType) rel.typeClass()).message);
        }
    }

    @Override
    public void visit(RepeatStat rep) {
        rep.getRelation().accept(this);
        if(!(rep.getRelation().typeClass() instanceof BoolType)) {
            if (rep.getRelation().typeClass() instanceof PtrType) {
                reportError(rep.lineNumber(), rep.charPosition(), "RepeatStat requires bool condition not " + rep.getRelation().typeClass().deref() + ".");
            } else {
                reportError(rep.lineNumber(), rep.charPosition(), "RepeatStat requires bool condition not " +   rep.getRelation().typeClass() + ".");
            }
        }
        rep.getSeq().accept(this);
    }

    @Override
    public void visit(Return ret) {
        if(currentFunction == null){
            currentFunction = new FunctionSymbol("main", new ArrayType(Token.Kind.VOID), new ArrayType(Token.Kind.VOID));
        }
        // handling returns in main
        if( ret.getReturn() != null ) {
            ret.getReturn().accept(this);
            ret.setType(ret.getReturn().typeClass());
        }
        Type correct = ret.typeClass().funcRet(currentFunction, currentFunction.getReturnType());
        ret.setType(correct);
        if( !(correct instanceof ErrorType) )
            currentFunction.setRealReturnType(correct);
        if(correct instanceof ErrorType){
            reportError(ret.token().lineNumber(), ret.token().charPosition(), ((ErrorType) ret.typeClass()).message);
        }
    }

    @Override
    public void visit(RootAST root) {
        if( root.getVars() != null )
            root.getVars().accept(this);
        if( root.getFuncs() != null )
            root.getFuncs().accept(this);
        root.getSeq().accept(this);
    }

    @Override
    public void visit(StatSeq seq) {
        for( AST ast : seq.getSequence() ) {
            ast.accept(this);
        }
    }

    @Override
    public void visit(Subtraction sub) {
        sub.getLvalue().accept(this);
        sub.getRvalue().accept(this);

        sub.setType(sub.getLvalue().typeClass().sub(sub.getRvalue().typeClass()));
        if(sub.typeClass() instanceof ErrorType){
            reportError(sub.lineNumber(), sub.charPosition(), ((ErrorType) sub.typeClass()).message);
        }
    }

    @Override
    public void visit(VariableDeclaration var) {

    }

    @Override
    public void visit(WhileStat wstat) {
        wstat.getRelation().accept(this);
        if(!(wstat.getRelation().typeClass() instanceof BoolType)) {
            if(wstat.getRelation().typeClass() instanceof PtrType){
                reportError(wstat.lineNumber(), wstat.charPosition(), "WhileStat requires bool condition not " + wstat.getRelation().typeClass().deref() + ".");
            }else {
                reportError(wstat.lineNumber(), wstat.charPosition(), "WhileStat requires bool condition not " + wstat.getRelation().typeClass() + ".");
            }
        }
        wstat.getSeq().accept(this);
    }
}
