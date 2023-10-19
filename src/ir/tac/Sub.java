package ir.tac;

public class Sub extends Assign {
    protected Sub(int id, Variable dest, Value left, Value right) {
        super(id, dest, left, right);
    }

    @Override
    public void accept(TACVisitor visitor) {

    }

    @Override
    public String genDot() {
        return null;
    }
}
