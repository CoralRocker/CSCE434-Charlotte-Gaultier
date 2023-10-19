package ast;

import coco.Token;
import types.Type;

import java.util.ArrayList;

public class ArgList extends AST implements Visitable {

    private ArrayList<AST> args;

    public ArgList(Token tkn) {
        super(tkn);
        args = new ArrayList<>();
    }

    public void add( AST arg ) {
        getArgs().add(arg);
    }

    @Override
    public String type() {
        return null;
    }

    @Override
    public Type typeClass() {
        return null;
    }

    @Override
    public String printPreOrder() {
        StringBuilder builder = new StringBuilder();
        builder.append(this);
        builder.append("\n");
        for( AST arg : getArgs()) {
            String[] lines = arg.printPreOrder().split(System.lineSeparator());
            for( String line : lines ) {
                builder.append("  ");
                builder.append( line );
                builder.append("\n");
            }
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        return "ArgumentList";
    }

    @Override
    public <E> E accept(NodeVisitor<E> visitor) {
        return visitor.visit(this);
    }

    @Override
    public boolean isConstEvaluable() {
        return false;
    }

    @Override
    public AST constEvaluate() {
        return null;
    }

    public ArrayList<AST> getArgs() {
        return args;
    }
}
