package ir.tac;

public abstract class TACVisitor {

    public abstract void visit(Return ret);
    public abstract void visit(Literal lit);

    public abstract void visit(Call call);
    public abstract void visit(Variable var);

    public abstract void visit(Add add);
    public abstract void visit(Assign asn);

    public abstract void visit(Div div);
    public abstract void visit(Mod mod);
    public abstract void visit(Mul mul);
    public abstract void visit(Sub sub);

    public abstract void visit(Branch bra);

}
