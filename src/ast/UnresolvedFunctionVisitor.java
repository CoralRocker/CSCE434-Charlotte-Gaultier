package ast;

import coco.Token;

import java.util.ArrayList;

public class UnresolvedFunctionVisitor extends VisitorBase {

    private ArrayList<Token> unresolvedTokens;

    public ArrayList<Token> errors() {
        return unresolvedTokens;
    }

    public UnresolvedFunctionVisitor() {
        unresolvedTokens = new ArrayList<Token>();
    }

    @Override
    public void exprAction(AST expr) {
        if( expr instanceof FuncCall ) {
            FuncCall call = (FuncCall) expr;
            if( !call.func.hasType() ) {
                unresolvedTokens.add(call.funcTok);
            }

        }

    }

    @Override
    public void statAction(AST stat) {

    }

    @Override
    public void compAction(RootAST comp) {

    }

}
