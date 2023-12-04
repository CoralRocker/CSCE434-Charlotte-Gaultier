package ir.tac;

import ast.AST;
import ast.BoolLiteral;
import ast.FloatLiteral;
import ast.IntegerLiteral;
import coco.Token;

import java.util.function.Function;

public class Literal extends Value implements Cloneable{

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
        return new Literal(new BoolLiteral( Token.BOOL_VAL(b) ));
    }

    public static Literal get(float f) {
        return new Literal(new FloatLiteral( Token.FLOAT_VAL(String.valueOf(f), 0, 0)));
    }

    public final AST val;

    private <E> E apply( E b, E i, E f ) {
        if (val instanceof ast.BoolLiteral) {
            return b;
        } else if (val instanceof ast.IntegerLiteral) {
            return i;
        } else if (val instanceof ast.FloatLiteral) {
            return f;
        }
        throw new RuntimeException("");
    }

    public float getFloat() {
        return val.getFloatLiteral();
    }

    public int getInt() {
        return val.getIntLiteral();
    }

    public boolean getBool() {
        return val.getBoolLiteral();
    }

    @Override
    public String toString() {
        if (val instanceof ast.BoolLiteral) {
            return String.valueOf(((ast.BoolLiteral) val).getBoolLiteral());
        } else if (val instanceof ast.IntegerLiteral) {
            return String.valueOf(((ast.IntegerLiteral) val).getIntLiteral());
        } else if (val instanceof ast.FloatLiteral) {
            return String.valueOf(((ast.FloatLiteral) val).getLiteral());
        }
        return "LiteralTypeError";
    }


    public String typeString() {
        return apply("bool", "int", "float");
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
        if (val instanceof ast.BoolLiteral) {
            return get(((BoolLiteral) val).getBoolLiteral());
        } else if (val instanceof ast.IntegerLiteral) {
            return get(((IntegerLiteral) val).getIntLiteral());
        } else if (val instanceof ast.FloatLiteral) {
            return get(((FloatLiteral) val).getFloatLiteral());
        }
        throw new RuntimeException("Invalid Literal Type: " + val.getClass());
    }
}




    
