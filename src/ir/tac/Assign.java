package ir.tac;

public abstract class Assign extends TAC{
    

    public Value left; // operand_1
    public Value right; // operand_2

    protected Assign(TacID id, Assignable dest, Value left, Value right) {
        super(id);
        this.dest = dest;
        if( dest == null ) {
            throw new NullPointerException();
        }
        this.left = left;
        if( left == null ) {
            throw new NullPointerException();
        }
        this.right = right;
        if( right == null ) {
            throw new NullPointerException();
        }
    }

    public abstract Literal calculate();

    public boolean hasImmediate() {
        return left instanceof Literal || right instanceof Literal;
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
