package ir.tac;

public class Mod extends Assign {
    protected Mod(int id, Variable dest, Value left, Value right) {
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
