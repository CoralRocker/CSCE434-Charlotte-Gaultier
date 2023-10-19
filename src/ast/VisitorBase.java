package ast;

public abstract class VisitorBase<E> implements NodeVisitor<E> {

    public VisitorBase() {
    }

    public void exprAction(AST expr) {}
    public void statAction(AST stat) {}

    public void compAction(RootAST comp) {}

    @Override
    public E visit(Addition add) {
        exprAction(add);
        add.getLvalue().accept(this);
        add.getRvalue().accept(this);

        return null;
    }

    @Override
    public E visit(ArgList list) {
        exprAction(list);
        for( AST ast : list.getArgs() ) {
            ast.accept(this);
        }

        return null;
    }

    @Override
    public E visit(ArrayIndex idx) {
        exprAction(idx);
        idx.getArray().accept(this);
        idx.getIndex().accept(this);

        return null;
    }

    @Override
    public E visit(Assignment asn) {
        statAction(asn);
        asn.getTarget().accept(this);
        asn.getRvalue().accept(this);

        return null;
    }

    @Override
    public E visit(BoolLiteral bool) {
        exprAction(bool);
        return null;
    }

    @Override
    public E visit(DeclarationList list) {
        statAction(list);
        for( AST decl : list.getContained() ) {
            decl.accept(this);
        }


        return null;
    }

    @Override
    public E visit(Designator des) {
        exprAction(des);
        return null;
    }

    @Override
    public E visit(Division div) {
        exprAction(div);
        div.getLvalue().accept(this);
        div.getRvalue().accept(this);

        return null;
    }

    @Override
    public E visit(FloatLiteral flt) {
        exprAction(flt);
        return null;
    }

    @Override
    public E visit(FuncBody fb) {
        statAction(fb);
        if( fb.getVarList() != null )
            fb.getVarList().accept(this);
        fb.getSeq().accept(this);

        return null;
    }

    @Override
    public E visit(FuncCall fc) {
        exprAction(fc);
        fc.getArgs().accept(this);

        return null;
    }

    @Override
    public E visit(FuncDecl fd) {
        statAction(fd);
        fd.getBody().accept(this);

        return null;
    }

    @Override
    public E visit(IfStat is) {
        statAction(is);
        is.getIfrel().accept(this);
        is.getIfseq().accept(this);
        if( is.getElseseq() != null ) {
            is.getElseseq().accept(this);
        }

        return null;
    }

    @Override
    public E visit(IntegerLiteral il) {
        exprAction(il);
        return null;
    }

    @Override
    public E visit(LogicalAnd la) {
        exprAction(la);
        la.getLvalue().accept(this);
        la.getRvalue().accept(this);

        return null;
    }

    @Override
    public E visit(LogicalNot ln) {
        exprAction(ln);
        ln.getRvalue().accept(this);

        return null;
    }

    @Override
    public E visit(LogicalOr lo) {
        exprAction(lo);
        lo.getLvalue().accept(this);
        lo.getRvalue().accept(this);

        return null;
    }

    @Override
    public E visit(Modulo mod) {
        exprAction(mod);
        mod.getLvalue().accept(this);
        mod.getRvalue().accept(this);

        return null;
    }

    @Override
    public E visit(Multiplication mul) {
        exprAction(mul);
        mul.getLvalue().accept(this);
        mul.getRvalue().accept(this);

        return null;
    }

    @Override
    public E visit(Power pwr) {
        exprAction(pwr);
        pwr.getLvalue().accept(this);
        pwr.getRvalue().accept(this);


        return null;
    }

    @Override
    public E visit(Relation rel) {
        exprAction(rel);
        rel.getLvalue().accept(this);
        rel.getRvalue().accept(this);


        return null;
    }


    @Override
    public E visit(RepeatStat rep) {
        statAction(rep);
        rep.getRelation().accept(this);
        rep.getSeq().accept(this);

        return null;
    }

    @Override
    public E visit(Return ret) {
        statAction(ret);
        if( ret.getReturn() != null ) {
            ret.getReturn().accept(this);

        }
        return null;
    }

    @Override
    public E visit(RootAST root) {
        compAction(root);
        if( root.getVars() != null )
            root.getVars().accept(this);
        if( root.getFuncs() != null )
            root.getFuncs().accept(this);
        root.getSeq().accept(this);

        return null;
    }

    @Override
    public E visit(StatSeq seq) {
        statAction(seq);
        for( AST ast : seq.getSequence() ) {
            ast.accept(this);
        }

        return null;
    }

    @Override
    public E visit(Subtraction sub) {
        exprAction(sub);
        sub.getLvalue().accept(this);
        sub.getRvalue().accept(this);

        return null;
    }

    @Override
    public E visit(VariableDeclaration var) {
        statAction(var);
        return null;
    }

    @Override
    public E visit(WhileStat wstat) {
        statAction(wstat);
        wstat.getRelation().accept(this);
        wstat.getSeq().accept(this);

        return null;
    }
}
