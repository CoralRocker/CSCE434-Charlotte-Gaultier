package ast;

public interface NodeVisitor {

    public void visit(Addition add);

    public void visit(ArgList list);

    public void visit(ArrayIndex idx);

    public void visit(Assign asn);
    public void visit(Assignment asn);
    public void visit(BoolLiteral bool);
    public void visit(DeclarationList list);
    public void visit(Designator des);
    public void visit(Division div);
    public void visit(FloatLiteral flt);
    public void visit(FuncBody fb);
    public void visit(FuncCall fc);
    public void visit(FuncDecl fd);
    public void visit(IfStat is);
    public void visit(IntegerLiteral il);
    public void visit(LogicalAnd la);
    public void visit(LogicalNot ln);
    public void visit(LogicalOr lo);
    public void visit(Modulo mod);
    public void visit(Multiplication mul);
    public void visit(Power pwr);
    public void visit(Relation rel);
    public void visit(RelExpr rel);
    public void visit(RepeatStat rep);
    public void visit(Return ret);
    public void visit(ReturnStat stat);
    public void visit(RootAST root);
    public void visit(StatSeq seq);
    public void visit(Subtraction sub);
    public void visit(VariableDeclaration var);
    public void visit(WhileStat wstat);
}
