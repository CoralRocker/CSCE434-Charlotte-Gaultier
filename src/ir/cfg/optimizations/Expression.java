package ir.cfg.optimizations;

import ir.tac.*;

public class Expression {
    protected Assignable dest;
    protected TAC op;
    protected Value[] args;

    public boolean isCopy() {
        return args.length == 1 && op instanceof Store;
    }

    public boolean isSubexpression() {
        return args.length == 2 && (op instanceof Assign || op instanceof Not);
    }

    protected void setExprNotDest( Expression expr ) {
        if( !expr.dest.equals(dest) ) throw new RuntimeException("Cannot merge two expressions with differing destinations!");

        op = expr.op;
        args = expr.args;
    }

    private boolean isValidOp(TAC op) {
        if (op instanceof Assign) {
            return true;
        } else if (op instanceof Store) {
            return true;
        }
        return false;
    }

    public boolean contains(Value arg) {
        if (args.length == 1) {
            return args[0].toString().equals(arg.toString());
        } else {
            return args[0].toString().equals(arg.toString()) || args[1].toString().equals(arg.toString());
        }
    }

    // Create an expression to search with
    public Expression(Assignable srch) {
        this.dest = null;
        this.op = null;
        this.args = new Value[1];
        this.args[0] = srch;
    }

    public Expression(Assignable dest, Store op, Value store) {
        this.dest = dest;
        this.op = op;
        this.args = new Value[1];
        this.args[0] = store;
    }

    public Expression(Assignable dest, Not op, Value store) {
        this.dest = dest;
        this.op = op;
        this.args = new Value[1];
        this.args[0] = store;
    }


    public Expression(Assignable dest, Assign op, Value lhs, Value rhs) {
        this.dest = dest;
        this.op = op;
        this.args = new Value[2];
        this.args[0] = lhs;
        this.args[1] = rhs;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Expression))
            return false;

        Expression expr = (Expression) o;
        boolean equal = this.op.getClass() == expr.op.getClass();
        if (!equal) return false;
        equal = args.length == expr.args.length;
        if (!equal) return false;
        if (args.length == 1) {
            return args[0].equals(expr.args[0]);
        } else {
            return args[0].equals(expr.args[0]) && args[1].equals(expr.args[1]);
        }
    }

    @Override
    public String toString() {
        if (args.length == 1)
            return String.format("%s = %s %s", dest, op.opName(), args[0]);
        return String.format("%s = %s %s %s", dest, op.opName(), args[0], args[1]);
    }

    public String exprString() {
        if (args.length == 1)
            return String.format("%s %s", op.opName(), args[0]);
        return String.format("%s %s %s", op.opName(), args[0], args[1]);
    }

    @Override
    public int hashCode() {
        return this.exprString().hashCode();
    }

    public boolean isTemporary() {
        return dest instanceof Temporary;
    }
}
