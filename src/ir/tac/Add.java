package ir.tac;

public class Add extends Assign{
    public Add(int id, Assignable dest, Value left, Value right) {
        super(id, dest, left, right);
    }

    @Override
    public void accept(TACVisitor visitor) {

    }

    @Override
    public String genDot() {
        return String.format("add %s %s %s", super.dest, super.left, super.right);
    }
    // either do this way or blend the operator's meaning into Assign
}
