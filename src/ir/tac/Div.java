package ir.tac;

public class Div extends Assign {
    public Div(int id, Assignable dest, Value left, Value right) {
        super(id, dest, left, right);
    }

    @Override
    public void accept(TACVisitor visitor) {

    }

    @Override
    public String genDot() {
        return String.format("div %s %s %s", dest, left, right);
    }
}
