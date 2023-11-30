package ir.tac;

import coco.ArraySymbol;
import coco.ArrayType;
import coco.Symbol;
import coco.VariableSymbol;

import java.util.Objects;

public class ArrayAccess extends Assignable implements Visitable{

    private Value index;
    private Value array;

    private Symbol sym;

    public void setSymbol(Symbol sym){
        this.sym = sym;
    }

    public Symbol getSymbol(){
        return sym;
    }

    protected int asnNum;
    public ArrayAccess(Value array, Value index, ArrayType type) {
        this.array = array;
        this.index = index;
        this.sym = new ArraySymbol(this.toString(), type, index);
    }

    @Override
    public boolean isConst() {
        return false;
    }

    @Override
    public boolean equals(Object other) {
        if(!(other instanceof ArrayAccess)){
            return false;
        }
        return (Objects.equals(this.sym.name(), ((ArrayAccess)other).sym.name()) && this.index == ((ArrayAccess)other).index);
    }

    @Override
    public String toString() {
        return String.format("%s", array) + "[" + String.format("%s", index) + "]";
            // return String.format("%s_%d", sym.name(), asnNum);
    }

    @Override
    public String name() {
        return sym.name();
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return null;
    }
}
