package ir.tac;

public class Mul extends Assign {
    public Mul(int id, Assignable dest, Value left, Value right) {
        super(id, dest, left, right);
    }


    @Override
    public String genDot() {
        return String.format("mul %s %s %s", super.dest, super.left, super.right);
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
