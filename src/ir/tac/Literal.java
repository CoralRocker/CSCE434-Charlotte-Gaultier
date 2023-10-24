package ir.tac;

import ast.AST;
import ast.BoolLiteral;
import ast.IntegerLiteral;
import coco.Token;

public class Literal implements Value{

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

    private AST val;

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
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}




    
