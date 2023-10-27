package coco;

public class VariableSymbol extends Symbol implements Comparable<VariableSymbol>{

    private ArrayType type;
    private Object value;

    public VariableSymbol(String name, ArrayType type) {
        super(name);
        this.type = type;
    }

    @Override
    public Object value() {
        return value;
    }

    public void setValue(Object val ) {
        value = val;
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

    @Override
    public boolean equals(Object o) {
        if( o == null )
            return false;
        if( !(o instanceof VariableSymbol) )
            return false;

        final VariableSymbol var = (VariableSymbol) o;

        return var.name.equals(name) && type.equals(var.type);
    }

    @Override
    public int compareTo(VariableSymbol variableSymbol) {
        return String.format("%s:%s", name, type).compareTo(String.format("%s:%s", variableSymbol.name, variableSymbol.type));
    }
}
