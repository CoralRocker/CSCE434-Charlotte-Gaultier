package ir.tac;

import coco.Symbol;

public class Call extends TAC{
    
    private Symbol function;
    // private ValueList args;

    protected Call(int id) {
        super(id);
    }

    @Override
    public String genDot() {
        return null;
    }

    @Override
    public void accept(TACVisitor visitor) {

    }
}
