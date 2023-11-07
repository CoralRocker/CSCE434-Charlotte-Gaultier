package ir.tac;

public class Cmp extends Assign implements Visitable{

    private Value rhs, lhs;
    private Assignable target;
    private String op;

    public Cmp(TacID id, Value rhs, Value lhs, Assignable target, String op) {
        super(id, target, lhs, rhs);
        this.rhs = rhs;
        this.lhs = lhs;
        this.op = op;
        this.target = target;
    }

    @Override
    public String genDot() {
        return this.toString();
    }

    @Override
    public String toString() {
        return String.format("cmp%s %s %s %s", op, target, rhs, lhs);
    }


    @Override
    public Literal calculate() {
        if(!( left.isConst() && right.isConst() ) ) {
            return null;
        }

        Literal rhs = (Literal) right, lhs = (Literal) left;


        switch ( op ) {
            case ">=" -> {
                return Literal.get( lhs.getInt() >= rhs.getInt() );
            }
            case ">" -> {
                return Literal.get( lhs.getInt() > rhs.getInt() );
            }
            case "<=" -> {
                return Literal.get( lhs.getInt() <= rhs.getInt() );
            }
            case "<" -> {
                return Literal.get( lhs.getInt() < rhs.getInt() );
            }
            case "=="-> {
                if( lhs.typeString().equals("bool") ) {
                    return Literal.get( lhs.getBool() == rhs.getBool() );
                }
                else {
                    return Literal.get( lhs.getInt() == rhs.getInt() );
                }
            }
            case "!="-> {
                if( lhs.typeString().equals("bool") ) {
                    return Literal.get( lhs.getBool() != rhs.getBool() );
                }
                else {
                    return Literal.get( lhs.getInt() != rhs.getInt() );
                }
            }
        }


        return null;
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
