package ast;

import coco.Token;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class RootAST extends AST {

    private ArrayList< AST > seq;

    public RootAST( Token tkn ) {
        super( tkn );
    }

    public void add( AST ast ) {
        seq.add( ast );
    }

    @Override
    public String type() {
        return "Computation";
    }

    @Override
    public String printPreOrder() {
        return "NOT IMPLEMENTED";
    }

}
