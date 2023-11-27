package ir.tac;

public class Cmp extends Assign implements Visitable{

    private Value rhs, lhs;
    private Assignable target;
    private String op;

    public Cmp(TacID id, Value lhs, Value rhs, Assignable target, String op) {
        super(id, target, lhs, rhs);
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
        return String.format("cmp %s %s %s", target, lhs, rhs);
    }

    public String getOp() {return op;}


    @Override
    public Literal calculate() {
        if(!( left.isConst() && right.isConst() ) ) {
            return null;
        }

        int rhs = ((Literal) right).getInt(), lhs = ((Literal) left).getInt();

        if( lhs > rhs ) return Literal.get(1);
        if( lhs < rhs ) return Literal.get(-1);

        return Literal.get(0);
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
