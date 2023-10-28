package ir.tac;

public class Temporary implements Value, Assignable, Visitable {
    private final int num;

    public Temporary(int n) {
        num = n;
    }

    @Override
    public String toString() {
        return String.format("_t%d", num);
    }

    @Override
    public String name() {
        return this.toString();
    }

    @Override
    public boolean isConst() {
        return false;
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
