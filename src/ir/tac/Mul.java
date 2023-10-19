package ir.tac;

public class Mul extends Assign {
    protected Mul(int id, Variable dest, Value left, Value right) {
        super(id, dest, left, right);
    }

    @Override
    public void accept(TACVisitor visitor) {

    }
}
