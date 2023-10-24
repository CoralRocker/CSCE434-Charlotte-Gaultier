package ir.tac;

public class Mod extends Assign {
    public Mod(int id, Assignable dest, Value left, Value right) {
        super(id, dest, left, right);
    }


    @Override
    public String genDot() {
        return String.format("mod %s %s %s", dest, left, right);
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
