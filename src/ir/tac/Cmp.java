package ir.tac;

public class Cmp extends TAC implements Visitable{

    private Value rhs, lhs;
    private Assignable target;
    private String op;

    public Cmp(int id, Value rhs, Value lhs, Assignable target, String op) {
        super(id);
        this.rhs = rhs;
        this.lhs = lhs;
        this.op = op;
        this.target = target;
    }

    @Override
    public String genDot() {
        return this.toString();
    }

    @Override
    public String toString() {
        return String.format("cmp%s %s %s %s", op, target, rhs, lhs);
    }



    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
