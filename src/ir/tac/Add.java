package ir.tac;

public class Add extends Assign{
    protected Add(int id, Variable dest, Value left, Value right) {
        super(id, dest, left, right);
    }

    @Override
    public void accept(TACVisitor visitor) {

    }

    @Override
    public String genDot() {
        return null;
    }
    // either do this way or blend the operator's meaning into Assign
}
