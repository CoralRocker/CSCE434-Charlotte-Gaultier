package coco;

import java.util.ArrayList;

public class Symbol {

    private String name;

    private ArrayType type;

    // TODO: Add other parameters like type

    public Symbol (String name, ArrayType type) {
        this.name = name;
        this.type = type;
    }
    public String name () {
        return name;
    }

    public ArrayType type() {
        return type;
    }

    @Override
    public String toString() {
        return String.format("Symbol(%s:%s)", name, type);
    }

}
