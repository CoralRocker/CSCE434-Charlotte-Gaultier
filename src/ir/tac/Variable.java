package ir.tac;

import coco.Symbol;

public class Variable implements Value {

    private Symbol sym;
    private boolean isConstant = false;


    @Override
    public boolean isConst() {
        return isConstant;
    }

    @Override
    public void accept(TACVisitor visitor) {

    }
}
