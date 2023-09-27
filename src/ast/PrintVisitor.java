package ast;

public class PrintVisitor implements NodeVisitor {

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

    private void printf(AST ast) {
        printf(ast.toString());
    }
    private void printf(String format, Object ... args ) {
        builder.append(getTabs());
        builder.append(format.formatted(args));
        builder.append('\n');
    }

    @Override
    public void visit(Addition add) {
        printf(add);
        depth++;
        add.getLvalue().accept(this);
        add.getRvalue().accept(this);
        depth--;
    }

    @Override
    public void visit(ArgList list) {
        printf(list);
        depth++;
        for( AST ast : list.getArgs() ) {
            ast.accept(this);
        }
        depth--;
    }

    @Override
    public void visit(ArrayIndex idx) {
        printf(idx);
        depth++;
        idx.getArray().accept(this);
        idx.getIndex().accept(this);
        depth--;
    }

    @Override
    public void visit(Assignment asn) {
        printf(asn);
        depth++;
        asn.getTarget().accept(this);
        asn.getRvalue().accept(this);
        depth--;
    }

    @Override
    public void visit(BoolLiteral bool) {
        printf(bool);
    }

    @Override
    public void visit(DeclarationList list) {
        printf(list);
        depth++;
        for( AST decl : list.getContained() ) {
            decl.accept(this);
        }
        depth--;

    }

    @Override
    public void visit(Designator des) {
        printf(des);
    }

    @Override
    public void visit(Division div) {
        printf(div);
        depth++;
        div.getLvalue().accept(this);
        div.getRvalue().accept(this);
        depth--;
    }

    @Override
    public void visit(FloatLiteral flt) {
        printf(flt);
    }

    @Override
    public void visit(FuncBody fb) {
        printf(fb);
        depth++;
        fb.getVarList().accept(this);
        fb.getSeq().accept(this);
        depth--;
    }

    @Override
    public void visit(FuncCall fc) {
        printf(fc);
        depth++;
        fc.getArgs().accept(this);
        depth--;
    }

    @Override
    public void visit(FuncDecl fd) {
        printf(fd);
        depth++;
        fd.getBody().accept(this);
        depth--;
    }

    @Override
    public void visit(IfStat is) {
        printf(is);
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
        printf(il);
    }

    @Override
    public void visit(LogicalAnd la) {
        printf(la);
        depth++;
        la.getLvalue().accept(this);
        la.getRvalue().accept(this);
        depth--;
    }

    @Override
    public void visit(LogicalNot ln) {
        printf(ln);
        depth++;
        ln.getRvalue().accept(this);
        depth--;
    }

    @Override
    public void visit(LogicalOr lo) {
        printf(lo);
        depth++;
        lo.getLvalue().accept(this);
        lo.getRvalue().accept(this);
        depth--;
    }

    @Override
    public void visit(Modulo mod) {
        printf(mod);
        depth++;
        mod.getLvalue().accept(this);
        mod.getRvalue().accept(this);
        depth--;
    }

    @Override
    public void visit(Multiplication mul) {
        printf(mul);
        depth++;
        mul.getLvalue().accept(this);
        mul.getRvalue().accept(this);
        depth--;
    }

    @Override
    public void visit(Power pwr) {
        printf(pwr);
        depth++;
        pwr.getLvalue().accept(this);
        pwr.getRvalue().accept(this);
        depth--;

    }

    @Override
    public void visit(Relation rel) {
        printf(rel);
        depth++;
        rel.getLvalue().accept(this);
        rel.getRvalue().accept(this);
        depth--;

    }


    @Override
    public void visit(RepeatStat rep) {
        printf(rep);
        depth++;
        rep.getRelation().accept(this);
        rep.getSeq().accept(this);
        depth--;
    }

    @Override
    public void visit(Return ret) {
        printf(ret);
        depth++;
        ret.getReturn().accept(this);
        depth--;
    }

    @Override
    public void visit(RootAST root) {
        printf(root);
        depth++;
        root.getVars().accept(this);
        root.getFuncs().accept(this);
        root.getSeq().accept(this);
        depth--;
    }

    @Override
    public void visit(StatSeq seq) {
        printf(seq);
        depth++;
        for( AST ast : seq.getSequence() ) {
            ast.accept(this);
        }
        depth--;
    }

    @Override
    public void visit(Subtraction sub) {
        printf(sub);
        depth++;
        sub.getLvalue().accept(this);
        sub.getRvalue().accept(this);
        depth--;

    }

    @Override
    public void visit(VariableDeclaration var) {
        printf(var);
    }

    @Override
    public void visit(WhileStat wstat) {
        printf(wstat);
        depth++;
        wstat.getRelation().accept(this);
        wstat.getSeq().accept(this);
        depth--;
    }
}
