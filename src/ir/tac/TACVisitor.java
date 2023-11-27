package ir.tac;

public interface TACVisitor<E> {

    E visit(Return ret);
    E visit(Literal lit);

    E visit(Call call);
    E visit(Variable var);

    default E visit(Add add) { return visit(((Assign) add)); }
    default E visit(Assign asn) { throw new RuntimeException("Assign is not implemented! : " + asn); }

    default E visit(Div div) { return visit(((Assign) div)); }
    default E visit(Mod mod) { return visit((Assign) mod); }
    default E visit(Mul mul) { return visit((Assign) mul); }
    default E visit(Sub sub) { return visit((Assign) sub); }
    E visit(LoadStack lstack);

    E visit(Branch bra);

    default E visit(Cmp cmp) { return visit((Assign) cmp); }

    E visit(Store store);

    E visit(StoreStack sstack);

    E visit(Phi phi);

    default E visit(Pow pow) { return visit((Assign) pow); }

    E visit(Temporary temporary);

    E visit(Not not);
    default E visit(And and) { return visit((Assign) and); }
    default E visit(Or or) { return visit((Assign) or); }

    default E visit(Xor xor) { return visit((Assign) xor); }
    default E visit(Lsh lsh) { return visit((Assign) lsh); }
    default E visit(Ash rsh) { return visit((Assign) rsh); }
}
