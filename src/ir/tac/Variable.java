package ir.tac;

import coco.Symbol;

public class Variable implements Value, Visitable, Assignable {

    private Symbol sym;
    private boolean isConstant = false;

    public Variable(Symbol sym) {
        this.sym = sym;
    }


    @Override
    public boolean isConst() {
        return isConstant;
    }

    @Override
    public void accept(TACVisitor visitor) {

    }

    @Override
    public String toString() {
        return sym.name();
    }
}
