package ir.tac;

import java.util.List;

public class CodeGenerator extends TACVisitor<DLX> {
    @Override
    public DLX visit(Return ret) {
        return new DLX("ret", 0);
    }

    @Override
    public DLX visit(Literal lit) {
        return null;
    }

    @Override
    public DLX visit(Call call) {
        return null;
    }

    @Override
    public DLX visit(Variable var) {
        return null;
    }

    @Override
    public DLX visit(Add add) {
        return new DLX("add", 0, 0,0 );
    }

    @Override
    public DLX visit(Assign asn) {
        return null;
    }

    @Override
    public DLX visit(Div div) {
        return new DLX("div", 0, 0,0 );
    }

    @Override
    public DLX visit(Mod mod) {
        return new DLX("mod", 0, 0,0 );
    }

    @Override
    public DLX visit(Mul mul) {
        return new DLX("mul", 0, 0,0 );
    }

    @Override
    public DLX visit(Sub sub) {
        return new DLX("sub", 0, 0,0 );
    }

    @Override
    public DLX visit(Branch bra) {
        return null;
    }

    @Override
    public DLX visit(Cmp cmp) {
        return new DLX("cmp", 0, 0,0 );
    }

    @Override
    public DLX visit(Store store) {
        return new DLX("stx", 0, 0,0 );
    }

    @Override
    public DLX visit(Phi phi) {
        return null;
    }

    @Override
    public DLX visit(Temporary temporary) {
        return null;
    }
}

