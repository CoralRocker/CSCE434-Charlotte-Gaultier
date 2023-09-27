package coco;

import java.util.ArrayList;

public class Symbol {

    private String name;

    private ArrayType type;

    private Object value;
    // TODO: Add other parameters like type

    public Symbol (String name, ArrayType type) {
        this.name = name;
        this.type = type;
        this.value = null;
    }

    public Symbol (String name, ArrayType type, Object value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }
    public String name () {
        return name;
    }

    public Object value () { return value; }

    public ArrayType type() {
        return type;
    }

    @Override
    public String toString() {
        return String.format("Symbol(%s:%s)=%s", name, type, value);
    }

}
