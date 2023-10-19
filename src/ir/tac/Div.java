package ir.tac;

public class Div extends Assign {
    protected Div(int id, Variable dest, Value left, Value right) {
        super(id, dest, left, right);
    }

    @Override
    public void accept(TACVisitor visitor) {

    }
}
