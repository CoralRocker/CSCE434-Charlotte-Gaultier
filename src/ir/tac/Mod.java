package ir.tac;

public class Mod extends Assign {
    public Mod(int id, Assignable dest, Value left, Value right) {
        super(id, dest, left, right);
    }

    @Override
    public void accept(TACVisitor visitor) {

    }

    @Override
    public String genDot() {
        return String.format("mod %s %s %s", dest, left, right);
    }
}
