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
    public boolean isConst() {
        return false;
    }

    @Override
    public void accept(TACVisitor visitor) {

    }
}
