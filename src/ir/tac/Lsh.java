package ir.tac;

public class Lsh extends Assign {
    public Lsh(TacID id, Assignable dest, Value left, Value right) {
        super(id, dest, left, right);
    }

    @Override
    public Literal calculate() {
        return Literal.get( ((Literal)left).getInt() << ((Literal)right).getInt() );
    }

    @Override
    public String genDot() {
        return String.format("Lsh %s %s %s", dest, left, right);
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
