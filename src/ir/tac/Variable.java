package ir.tac;

import coco.Symbol;

public class Variable implements Value, Visitable, Assignable {

    protected Symbol sym;
    protected int asnNum = -1;
    private boolean isConstant = false;

    public Variable(Symbol sym) {
        this.sym = sym;
    }

    public Variable(Symbol sym, int asnNum ) {
        this.sym = sym;
        this.asnNum = asnNum;
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
        if( asnNum == -1 ) {
            return sym.name();
        }
        else {
            return String.format("%s_%d", sym.name(), asnNum);
        }
    }

    public int getAsnNum() {
        return asnNum;
    }

    public void setAsnNum(int asnNum) {
        this.asnNum = asnNum;
    }
}
