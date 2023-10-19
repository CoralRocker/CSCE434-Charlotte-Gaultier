package ir.tac;

public class Pow extends Assign{
    public Pow(int id, Assignable dest, Value left, Value right) {
        super(id, dest, left, right);
    }

    @Override
    public void accept(TACVisitor visitor) {

    }

    @Override
    public String genDot() {
        return String.format("pow %s %s %s", super.dest, super.left, super.right);
    }
    // either do this way or blend the operator's meaning into Assign
}
