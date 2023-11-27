package coco;

import ir.tac.Variable;

public abstract class Symbol {

    protected String name;

    public Symbol (String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public abstract Object value();

    public abstract ArrayType type();

    public abstract boolean hasType();

    public abstract void setNullValue();

    @Override
    public boolean equals(Object other) {
        if( !(other instanceof Symbol) )
            return false;
        return name.equals(((Symbol) other).name) && type().equals(((Symbol) other).type());
    }
}

