package ir.tac;

public class Add extends Assign{
    public Add(int id, Assignable dest, Value left, Value right) {
        super(id, dest, left, right);
    }

    @Override
    public String genDot() {
        return String.format("add %s %s %s", super.dest, super.left, super.right);
    }
    // either do this way or blend the operator's meaning into Assign
    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
