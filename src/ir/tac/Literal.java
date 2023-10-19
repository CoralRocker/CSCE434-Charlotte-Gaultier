package ir.tac;

import ast.AST;

public class Literal implements Value{

    public Literal(AST val) {
        this.val = val;
    }

    @Override
    public boolean isConst() {
        return true;
    }

    @Override
    public void accept(TACVisitor visitor) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'accept'");
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
}




    
