package ir.tac;

public class Sub extends Assign {
    public Sub(int id, Assignable dest, Value left, Value right) {
        super(id, dest, left, right);
    }


    @Override
    public String genDot() {
        return String.format("sub %s %s %s", dest,left,right);
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }

    @Override
    public Literal calculate() {
        if(!( left.isConst() && right.isConst() ) ) {
            return null;
        }

        Literal rhs = (Literal) right, lhs = (Literal) left;

        if( lhs.typeString() == "bool" ) {
            throw new RuntimeException("Cannot sub bools");
        }

        return Literal.get( lhs.getInt() - rhs.getInt() );
    }
}
