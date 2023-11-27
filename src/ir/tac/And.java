package ir.tac;

public class And extends Assign {
    public And(TacID id, Assignable dest, Value left, Value right) {
        super(id, dest, left, right);
    }

    @Override
    public Literal calculate() {
        return Literal.get( ((Literal)left).getBool() && ((Literal)right).getBool() );
    }

    @Override
    public String genDot() {
        return String.format("AND %s %s %s", dest, left, right);
    }
}
