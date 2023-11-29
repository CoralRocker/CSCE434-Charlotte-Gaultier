package ir.tac;

import coco.Symbol;

public class ArrayIndex extends Variable implements Visitable{

    private Value index;

    public ArrayIndex(Symbol sym, Value index) {
        super(sym);
        this.sym = sym;
        this.index = index;
    }


    public ArrayIndex(Symbol sym, int asnNum ) {
        super(sym, asnNum);
        this.sym = sym;
        this.asnNum = asnNum;
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

    @Override
    public String name() {
        return sym.name();
    }
}
