package ir.tac;

public class Mul extends Assign {
    public Mul(int id, Assignable dest, Value left, Value right) {
        super(id, dest, left, right);
    }

    @Override
    public void accept(TACVisitor visitor) {

    }

    @Override
    public String genDot() {
        return String.format("mul %s %s %s", super.dest, super.left, super.right);
    }
}
