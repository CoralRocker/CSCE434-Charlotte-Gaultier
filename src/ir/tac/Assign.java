package ir.tac;

public abstract class Assign extends TAC{
    
    protected Assignable dest; // lhs
    protected Value left; // operand_1
    protected Value right; // operand_2

    protected Assign(int id, Assignable dest, Value left, Value right) {
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

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
