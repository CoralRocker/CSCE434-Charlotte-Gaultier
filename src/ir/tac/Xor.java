package ir.tac;

public class Xor extends Assign {
    public Xor(TacID id, Assignable dest, Value left, Value right) {
        super(id, dest, left, right);
    }

    @Override
    public Literal calculate() {
        return Literal.get( ((Literal)left).getBool() ^ ((Literal)right).getBool() );
    }

    @Override
    public String genDot() {
        return String.format("XOR %s %s %s", dest, left, right);
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
