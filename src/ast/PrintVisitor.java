package ast;

public class PrintVisitor implements NodeVisitor<Void> {

    private StringBuilder builder = new StringBuilder();
    private int depth = 0;

    @Override
    public String toString() {
        return builder.toString();
    }

    public PrintVisitor() {
        builder = new StringBuilder();
    }

    private String getTabs() {
        StringBuilder tabs = new StringBuilder();
        for( int i = 0; i < depth; i++ )
            tabs.append("  ");
        return tabs.toString();
    }

    private Void printf(AST ast) {
        printf(ast.toString());
        return null;

    }
    private Void printf(String format, Object ... args ) {
        builder.append(getTabs());
        builder.append(format.formatted(args));
        builder.append('\n');

        return null;
    }

    @Override
    public Void visit(Addition add) {
        printf(add);
        depth++;
        add.getLvalue().accept(this);
        add.getRvalue().accept(this);
        depth--;

        return null;
    }

    @Override
    public Void visit(ArgList list) {
        printf(list);
        depth++;
        for( AST ast : list.getArgs() ) {
            ast.accept(this);
        }
        depth--;
        return null;
    }

    @Override
    public Void visit(ArrayIndex idx) {
        printf(idx);
        depth++;
        idx.getArray().accept(this);
        idx.getIndex().accept(this);
        depth--;
        return null;
    }

    @Override
    public Void visit(Assignment asn) {
        printf(asn);
        depth++;
        asn.getTarget().accept(this);
        asn.getRvalue().accept(this);
        depth--;
        return null;
    }

    @Override
    public Void visit(BoolLiteral bool) {
        printf(bool);
        return null;
    }

    @Override
    public Void visit(DeclarationList list) {
        printf(list);
        depth++;
        for( AST decl : list.getContained() ) {
            decl.accept(this);
        }
        depth--;

        return null;
    }

    @Override
    public Void visit(Designator des) {
        printf(des);
        return null;
    }

    @Override
    public Void visit(Division div) {
        printf(div);
        depth++;
        div.getLvalue().accept(this);
        div.getRvalue().accept(this);
        depth--;
        return null;
    }

    @Override
    public Void visit(FloatLiteral flt) {
        printf(flt);
        return null;
    }

    @Override
    public Void visit(FuncBody fb) {
        printf(fb);
        depth++;
        if( fb.getVarList() != null )
            fb.getVarList().accept(this);
        fb.getSeq().accept(this);
        depth--;
        return null;
    }

    @Override
    public Void visit(FuncCall fc) {
        printf(fc);
        depth++;
        fc.getArgs().accept(this);
        depth--;
        return null;
    }

    @Override
    public Void visit(FuncDecl fd) {
        printf(fd);
        depth++;
        fd.getBody().accept(this);
        depth--;

        return null;
    }

    @Override
    public Void visit(IfStat is) {
        printf(is);
        depth++;
        is.getIfrel().accept(this);
        is.getIfseq().accept(this);
        if( is.getElseseq() != null ) {
            is.getElseseq().accept(this);
        }
        depth--;

        return null;
    }

    @Override
    public Void visit(IntegerLiteral il) {
        printf(il);
        return null;
    }

    @Override
    public Void visit(LogicalAnd la) {
        printf(la);
        depth++;
        la.getLvalue().accept(this);
        la.getRvalue().accept(this);
        depth--;

        return null;
    }

    @Override
    public Void visit(LogicalNot ln) {
        printf(ln);
        depth++;
        ln.getRvalue().accept(this);
        depth--;

        return null;
    }

    @Override
    public Void visit(LogicalOr lo) {
        printf(lo);
        depth++;
        lo.getLvalue().accept(this);
        lo.getRvalue().accept(this);
        depth--;

        return null;
    }

    @Override
    public Void visit(Modulo mod) {
        printf(mod);
        depth++;
        mod.getLvalue().accept(this);
        mod.getRvalue().accept(this);
        depth--;

        return null;
    }

    @Override
    public Void visit(Multiplication mul) {
        printf(mul);
        depth++;
        mul.getLvalue().accept(this);
        mul.getRvalue().accept(this);
        depth--;

        return null;
    }

    @Override
    public Void visit(Power pwr) {
        printf(pwr);
        depth++;
        pwr.getLvalue().accept(this);
        pwr.getRvalue().accept(this);
        depth--;

        return null;
    }

    @Override
    public Void visit(Relation rel) {
        printf(rel);
        depth++;
        rel.getLvalue().accept(this);
        rel.getRvalue().accept(this);
        depth--;

        return null;
    }


    @Override
    public Void visit(RepeatStat rep) {
        printf(rep);
        depth++;
        rep.getRelation().accept(this);
        rep.getSeq().accept(this);
        depth--;
        return null;
    }

    @Override
    public Void visit(Return ret) {
        printf(ret);
        if( ret.getReturn() != null ) {
            depth++;
            ret.getReturn().accept(this);
            depth--;
        }
        return null;
    }

    @Override
    public Void visit(RootAST root) {
        printf(root);
        depth++;
        if( root.getVars() != null )
            root.getVars().accept(this);
        if( root.getFuncs() != null )
            root.getFuncs().accept(this);
        root.getSeq().accept(this);
        depth--;
        return null;
    }

    @Override
    public Void visit(StatSeq seq) {
        printf(seq);
        depth++;
        for( AST ast : seq.getSequence() ) {
            ast.accept(this);
        }
        depth--;
        return null;
    }

    @Override
    public Void visit(Subtraction sub) {
        printf(sub);
        depth++;
        sub.getLvalue().accept(this);
        sub.getRvalue().accept(this);
        depth--;

        return null;
    }

    @Override
    public Void visit(VariableDeclaration var) {
        printf(var);
        return null;
    }

    @Override
    public Void visit(WhileStat wstat) {
        printf(wstat);
        depth++;
        wstat.getRelation().accept(this);
        wstat.getSeq().accept(this);
        depth--;
        return null;
    }
}
