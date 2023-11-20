package ir.tac;

import coco.Symbol;

public class Variable extends Assignable implements Visitable {

    protected Symbol sym;

    public Symbol getSym() {
        return sym;
    }

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
    public int hashCode() {
        return sym.name().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if( !(other instanceof Variable) )
            return false;

        return sym.equals(((Variable) other).sym);
    }

    @Override
    public String toString() {
        if( asnNum == -1 ) {
            return sym.name();
        }
        else {
            return String.format("%s", sym.name());

            // return String.format("%s_%d", sym.name(), asnNum);
        }
    }

    public int getAsnNum() {
        return asnNum;
    }

    public void setAsnNum(int asnNum) {
        this.asnNum = asnNum;
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String name() {
        return sym.name();
    }
}
