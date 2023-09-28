package ast;

import coco.ArrayType;
import coco.Token;

import java.util.ArrayList;

public class RootAST extends AST {


    private DeclarationList funcs = null;
    private DeclarationList vars = null;
    private StatSeq seq = null;

    public RootAST( Token tkn ) {

        super( tkn );
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
        if( vars != null )
            for( String line : vars.preOrderLines() )
                builder.append(String.format("  %s\n", line));
        if( funcs != null )
            for( String line : funcs.preOrderLines() )
                builder.append(String.format("  %s\n", line));
        if( seq != null )
            for( String line : seq.preOrderLines() )
                builder.append(String.format("  %s\n", line));

        if( builder.charAt(builder.length()-1) == '\n' ) {
            builder.deleteCharAt(builder.length()-1);
        }
        return builder.toString();
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    public DeclarationList getFuncs() {
        return funcs;
    }

    public void setFuncs(DeclarationList funcs) {
        this.funcs = funcs;
    }

    public DeclarationList getVars() {
        return vars;
    }

    public void setVars(DeclarationList vars) {
        this.vars = vars;
    }

    public StatSeq getSeq() {
        return seq;
    }

    public void setSeq(StatSeq seq) {
        this.seq = seq;
    }
}
