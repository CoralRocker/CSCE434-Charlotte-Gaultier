package ir.cfg;

import ir.tac.*;

public class AvailableExpression extends CFGVisitor {

    private CFG cfg;

    public AvailableExpression( CFG cfg, boolean do_cse, boolean do_cpp ) {
        this.cfg = cfg;

        cfg.markUnvisited();

    }

    @Override
    public Object visit(BasicBlock blk) {
        return null;
    }

}

class Expression {
    protected Variable dest;
    protected TAC op;
    protected Value[] args;

    private boolean isValidOp(TAC op) {
        if( op instanceof Assign ) {
            return true;
        }
        else if( op instanceof Store ) {
            return true;
        }
        return false;
    }

    public Expression(Variable dest, TAC op, Value store) {
        this.dest = dest;
        this.op = op;
        this.args = new Value[1];
        this.args[0] = store;
    }

    public Expression(Variable dest, TAC op, Value lhs, Value rhs) {
        this.dest = dest;
        this.op = op;
        this.args = new Value[2];
        this.args[0] = lhs;
        this.args[1] = rhs;
    }

    @Override
    public boolean equals(Object o) {
        if( !(o instanceof Expression) )
            return false;

        Expression expr = (Expression) o;
        boolean equal = this.op.getClass() == expr.op.getClass();
        if( !equal ) return false;
        equal = args.length == expr.args.length;
        if( !equal ) return false;
        if( args.length == 1 ) {
            return args[0].equals(expr.args[0]);
        }
        else {
            equal = false;
            if( args[0].equals(expr.args[0]) && args[1].equals(expr.args[1])) {
                 return true;
            }

            if( args[0].equals(expr.args[1]) && args[1].equals(expr.args[0])) {
                return true;
            }
            return equal;
        }
    }

}

class LiveInBlock extends TACVisitor<Object> {



    @Override
    public Object visit(Return ret) {
        return null;
    }

    @Override
    public Object visit(Literal lit) {
        return null;
    }

    @Override
    public Object visit(Call call) {
        return null;
    }

    @Override
    public Object visit(Variable var) {
        return null;
    }

    @Override
    public Object visit(Add add) {
        return null;
    }

    @Override
    public Object visit(Assign asn) {
        return null;
    }

    @Override
    public Object visit(Div div) {
        return null;
    }

    @Override
    public Object visit(Mod mod) {
        return null;
    }

    @Override
    public Object visit(Mul mul) {
        return null;
    }

    @Override
    public Object visit(Sub sub) {
        return null;
    }

    @Override
    public Object visit(Branch bra) {
        return null;
    }

    @Override
    public Object visit(Cmp cmp) {
        return null;
    }

    @Override
    public Object visit(Store store) {
        return null;
    }

    @Override
    public Object visit(Phi phi) {
        return null;
    }

    @Override
    public Object visit(Temporary temporary) {
        return null;
    }
}