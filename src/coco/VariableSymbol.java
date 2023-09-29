package coco;

public class VariableSymbol extends Symbol {

    private ArrayType type;

    public VariableSymbol(String name, ArrayType type) {
        super(name);
        this.type = type;
    }

    @Override
    public Object value() {
        return null;
    }

    @Override
    public ArrayType type() {
        return type;
    }

    @Override
    public boolean hasType() { return type != null; }


    @Override
    public String toString() {
        return String.format("Symbol(%s:%s)=%s", super.name, type(), value());
    }
}
