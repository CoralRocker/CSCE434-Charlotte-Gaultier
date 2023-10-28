package ir.tac;

public class Add extends Assign{
    public Add(int id, Assignable dest, Value left, Value right) {
        super(id, dest, left, right);
    }

    @Override
    public Literal calculate() {
        if(!( left.isConst() && right.isConst() ) ) {
            return null;
        }

        Literal rhs = (Literal) right, lhs = (Literal) left;

        if( lhs.typeString() == "bool" ) {
            throw new RuntimeException("Cannot add bools");
        }

        return Literal.get( rhs.getInt() + lhs.getInt() );
    }

    @Override
    public String genDot() {
        return String.format("add %s %s %s", super.dest, super.left, super.right);
    }
    // either do this way or blend the operator's meaning into Assign
    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
