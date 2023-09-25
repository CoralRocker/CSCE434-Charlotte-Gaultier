package ast;

import coco.ArrayType;
import coco.Token;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class RootAST extends AST {

    private ArrayList< AST > seq;

    public RootAST( Token tkn ) {

        super( tkn );
        seq = new ArrayList<>();
    }

    public void add( AST ast ) {
        seq.add( ast );
    }

    @Override
    public String type() {
        ArrayType mainfunc = ArrayType.makeFunctionType(new ArrayType(new Token(Token.Kind.VOID, 0,0), null), new ArrayList<>() );
        return String.format("Computation[main:%s]", mainfunc);
    }

    @Override
    public String toString() {
        return type();
    }

    @Override
    public String printPreOrder() {
        StringBuilder builder = new StringBuilder();

        builder.append(this);
        builder.append("\n");
        for( AST ast : seq ) {
            String[] lines = ast.printPreOrder().split(System.lineSeparator());
            for( String line : lines ) {
                builder.append("\t");
                builder.append(line);
                builder.append("\n");
            }
        }

        return builder.toString();
    }

}
