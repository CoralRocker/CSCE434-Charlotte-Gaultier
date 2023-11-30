package coco;

import ir.tac.Value;

import java.util.Objects;

public class ArraySymbol extends Symbol implements Comparable<VariableSymbol>, Cloneable {

    private final ArrayType type;
    private Object value;

    private Value index;
    // list of index
    public boolean isInitialized = false;

    public ArraySymbol(String name, ArrayType type, Value index) {
        super(name);
        this.type = type;
        this.index = index;
    }

    @Override
    public Object value() {
        return null;
    }

    @Override
    public ArrayType type() {
        return null;
    }

    @Override
    public boolean hasType() {
        return false;
    }

    @Override
    public void setNullValue() {

    }

    public boolean equals(ArraySymbol o) {
        return (Objects.equals(this.name, o.name) && this.index == o.index);
    }

    @Override
    public int compareTo(VariableSymbol o) {
        return 0;
    }
}
