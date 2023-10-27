package ir.tac;

import ast.AST;
import ast.BoolLiteral;
import ast.FloatLiteral;
import ast.IntegerLiteral;
import coco.Token;

public class Literal implements Value, Cloneable{

    public Literal(AST val) {
        this.val = val;
    }

    @Override
    public boolean isConst() {
        return true;
    }

    public static Literal get(int i) {
        return new Literal(new IntegerLiteral( Token.INT_VAL(String.valueOf(i), 0, 0)));
    }

    public static Literal get(boolean b) {
        return new Literal(new BoolLiteral( new Token(String.valueOf(b), 0, 0)));
    }

    public static Literal get(float f) {
        return new Literal(new FloatLiteral( Token.FLOAT_VAL(String.valueOf(f), 0, 0)));
    }

    private final AST val;

    @Override
    public String toString() {
        if (val instanceof ast.BoolLiteral) {
            return String.valueOf(((ast.BoolLiteral) val).getBoolLiteral());
        } else if (val instanceof ast.IntegerLiteral) {
            return String.valueOf(((ast.IntegerLiteral) val).getIntLiteral());
        } else if (val instanceof ast.FloatLiteral) {
            return String.valueOf(((ast.FloatLiteral) val).getLiteral());
        }
        return "LiteralValueError";
    }

    @Override
    public boolean equals(Object o) {
        if( o == null )
            return false;

        if( !(o instanceof Literal ) )
            return false;

        final Literal other = (Literal) o;

        return other.toString().equals(this.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }

    @Override
    public Literal clone() {
        if (val instanceof BoolLiteral) {
            return get(((BoolLiteral) val).getBoolLiteral());
        } else if (val instanceof IntegerLiteral) {
            return get(((IntegerLiteral) val).getIntLiteral());
        } else if (val instanceof FloatLiteral) {
            return get(((FloatLiteral) val).getFloatLiteral());
        }
        throw new RuntimeException("Value in Literal is not Bool, Int, or Float literal!: " + val.toString());
    }
}




    
