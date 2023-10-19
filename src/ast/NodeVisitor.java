package ast;

public interface NodeVisitor<E> {

    public E visit(Addition add);

    public E visit(ArgList list);

    public E visit(ArrayIndex idx);
    public E visit(Assignment asn);
    public E visit(BoolLiteral bool);
    public E visit(DeclarationList list);
    public E visit(Designator des);
    public E visit(Division div);
    public E visit(FloatLiteral flt);
    public E visit(FuncBody fb);
    public E visit(FuncCall fc);
    public E visit(FuncDecl fd);
    public E visit(IfStat is);
    public E visit(IntegerLiteral il);
    public E visit(LogicalAnd la);
    public E visit(LogicalNot ln);
    public E visit(LogicalOr lo);
    public E visit(Modulo mod);
    public E visit(Multiplication mul);
    public E visit(Power pwr);
    public E visit(Relation rel);
    public E visit(RepeatStat rep);
    public E visit(Return ret);
    public E visit(RootAST root);
    public E visit(StatSeq seq);
    public E visit(Subtraction sub);
    public E visit(VariableDeclaration var);
    public E visit(WhileStat wstat);
}
