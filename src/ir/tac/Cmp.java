package ir.tac;

public class Cmp extends TAC implements Visitable{

    private Value rhs, lhs;

    public Cmp(int id, Value rhs, Value lhs) {
        super(id);
        this.rhs = rhs;
        this.lhs = lhs;
    }

    @Override
    public String genDot() {
        return this.toString();
    }

    @Override
    public String toString() {
        return String.format("cmp %s %s", rhs, lhs);
    }


    @Override
    public void accept(TACVisitor visitor) {

    }
}
