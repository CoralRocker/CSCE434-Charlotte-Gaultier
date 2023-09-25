package ast;

import coco.Token;

import java.util.ArrayList;

public class StatSeq extends AST {

    private ArrayList< AST > seq;

    private void add( AST ast ) {
        seq.add( ast );
    }

    public StatSeq(Token tkn) {
        super(tkn);
    }

    @Override
    public String type() {
        return null;
    }

    @Override
    public String printPreOrder() {
        return null;
    }
}
