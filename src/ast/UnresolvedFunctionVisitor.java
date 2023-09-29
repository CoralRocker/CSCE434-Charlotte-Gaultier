package ast;

import coco.Token;

import java.util.ArrayList;

public class UnresolvedFunctionVisitor implements NodeVisitor {

    private int depth = 0;

    private ArrayList<Token> unresolvedTokens;

    public ArrayList<Token> errors() {
        return unresolvedTokens;
    }

    public UnresolvedFunctionVisitor() {
        unresolvedTokens = new ArrayList<Token>();
    }

    private String getTabs() {
        StringBuilder tabs = new StringBuilder();
        for( int i = 0; i < depth; i++ )
            tabs.append("  ");
        return tabs.toString();
    }

    @Override
    public void visit(Addition add) {
        depth++;
        add.getLvalue().accept(this);
        add.getRvalue().accept(this);
        depth--;
    }

    @Override
    public void visit(ArgList list) {
        depth++;
        for( AST ast : list.getArgs() ) {
            ast.accept(this);
        }
        depth--;
    }

    @Override
    public void visit(ArrayIndex idx) {
        depth++;
        idx.getArray().accept(this);
        idx.getIndex().accept(this);
        depth--;
    }

    @Override
    public void visit(Assignment asn) {
        depth++;
        asn.getTarget().accept(this);
        asn.getRvalue().accept(this);
        depth--;
    }

    @Override
    public void visit(BoolLiteral bool) {}

    @Override
    public void visit(DeclarationList list) {
        depth++;
        for( AST decl : list.getContained() ) {
            decl.accept(this);
        }
        depth--;

    }

    @Override
    public void visit(Designator des) {
    }

    @Override
    public void visit(Division div) {
        depth++;
        div.getLvalue().accept(this);
        div.getRvalue().accept(this);
        depth--;
    }

    @Override
    public void visit(FloatLiteral flt) {
    }

    @Override
    public void visit(FuncBody fb) {
        depth++;
        if( fb.getVarList() != null )
            fb.getVarList().accept(this);
        fb.getSeq().accept(this);
        depth--;
    }

    @Override
    public void visit(FuncCall fc) {
        if( !fc.func.hasType() ) {
            unresolvedTokens.add(fc.funcTok);
        }

        depth++;
        fc.getArgs().accept(this);
        depth--;
    }

    @Override
    public void visit(FuncDecl fd) {
        depth++;
        fd.getBody().accept(this);
        depth--;
    }

    @Override
    public void visit(IfStat is) {
        depth++;
        is.getIfrel().accept(this);
        is.getIfseq().accept(this);
        if( is.getElseseq() != null ) {
            is.getElseseq().accept(this);
        }
        depth--;
    }

    @Override
    public void visit(IntegerLiteral il) {
    }

    @Override
    public void visit(LogicalAnd la) {
        depth++;
        la.getLvalue().accept(this);
        la.getRvalue().accept(this);
        depth--;
    }

    @Override
    public void visit(LogicalNot ln) {
        depth++;
        ln.getRvalue().accept(this);
        depth--;
    }

    @Override
    public void visit(LogicalOr lo) {
        depth++;
        lo.getLvalue().accept(this);
        lo.getRvalue().accept(this);
        depth--;
    }

    @Override
    public void visit(Modulo mod) {
        depth++;
        mod.getLvalue().accept(this);
        mod.getRvalue().accept(this);
        depth--;
    }

    @Override
    public void visit(Multiplication mul) {
        depth++;
        mul.getLvalue().accept(this);
        mul.getRvalue().accept(this);
        depth--;
    }

    @Override
    public void visit(Power pwr) {
        depth++;
        pwr.getLvalue().accept(this);
        pwr.getRvalue().accept(this);
        depth--;

    }

    @Override
    public void visit(Relation rel) {
        depth++;
        rel.getLvalue().accept(this);
        rel.getRvalue().accept(this);
        depth--;

    }


    @Override
    public void visit(RepeatStat rep) {
        depth++;
        rep.getRelation().accept(this);
        rep.getSeq().accept(this);
        depth--;
    }

    @Override
    public void visit(Return ret) {
        if( ret.getReturn() != null ) {
            depth++;
            ret.getReturn().accept(this);
            depth--;
        }
    }

    @Override
    public void visit(RootAST root) {
        depth++;
        if( root.getVars() != null )
            root.getVars().accept(this);
        if( root.getFuncs() != null )
            root.getFuncs().accept(this);
        root.getSeq().accept(this);
        depth--;
    }

    @Override
    public void visit(StatSeq seq) {
        depth++;
        for( AST ast : seq.getSequence() ) {
            ast.accept(this);
        }
        depth--;
    }

    @Override
    public void visit(Subtraction sub) {
        depth++;
        sub.getLvalue().accept(this);
        sub.getRvalue().accept(this);
        depth--;

    }

    @Override
    public void visit(VariableDeclaration var) {
    }

    @Override
    public void visit(WhileStat wstat) {
        depth++;
        wstat.getRelation().accept(this);
        wstat.getSeq().accept(this);
        depth--;
    }
}
