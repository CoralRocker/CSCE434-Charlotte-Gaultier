package ir.tac;

public class Pow extends Assign{
    public Pow(TacID id, Assignable dest, Value left, Value right) {
        super(id, dest, left, right);
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String genDot() {
        return String.format("pow %s %s %s", super.dest, super.left, super.right);
    }
    // either do this way or blend the operator's meaning into Assign

    @Override
    public Literal calculate() {
        if(!( left.isConst() && right.isConst() ) ) {
            return null;
        }

        Literal rhs = (Literal) right, lhs = (Literal) left;

        if( lhs.typeString() == "bool" ) {
            throw new RuntimeException("Cannot mul bools");
        }

        Double pow = Math.pow( lhs.getInt(), rhs.getInt() );
        if( pow > Integer.MAX_VALUE ) {
            throw new RuntimeException(String.format("In constant folding of [%d : %s] : %f is larger than max value of int", getId(), toString(), pow));
        }


        return Literal.get( pow.intValue() );
    }
}
