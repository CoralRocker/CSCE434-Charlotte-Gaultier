package ir.tac;

public class Temporary extends Assignable implements Visitable {



    public final int num;

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
    public boolean equals(Object other) {
        if( !(other instanceof Temporary) )
            return false;

        return num == ((Temporary) other).num;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
