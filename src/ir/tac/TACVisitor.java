package ir.tac;

public abstract class TACVisitor<E> {

    public abstract E visit(Return ret);
    public abstract E visit(Literal lit);

    public abstract E visit(Call call);
    public abstract E visit(Variable var);

    public abstract E visit(Add add);
    public abstract E visit(Assign asn);

    public abstract E visit(Div div);
    public abstract E visit(Mod mod);
    public abstract E visit(Mul mul);
    public abstract E visit(Sub sub);
    public abstract E visit(LoadStack lstack);

    public abstract E visit(Branch bra);

    public abstract E visit(Cmp cmp);

    public abstract E visit(Store store);

    public abstract E visit(StoreStack sstack);

    public abstract E visit(Phi phi);

    public abstract E visit(Temporary temporary);
}
