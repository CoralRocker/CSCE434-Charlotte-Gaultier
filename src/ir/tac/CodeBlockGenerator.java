package ir.tac;

import ir.cfg.BasicBlock;

public class CodeBlockGenerator extends TACVisitor<DLX> {


    public static String generate(BasicBlock blk) {
        boolean changed = false;

        CodeBlockGenerator visitor = new CodeBlockGenerator();

        String toReturn = "";
        int ctr = -1;
        for( var instr : blk.getInstructions() ) {
            ctr++;
            DLX line = instr.accept(visitor);
            if( line != null ) {
                toReturn = toReturn + line.toString() + "\n";
            }
        }
        return toReturn;
    }
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
    public DLX visit(LoadStack lstack) {
        return null;
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
    public DLX visit(StoreStack sstack) {
        return null;
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

