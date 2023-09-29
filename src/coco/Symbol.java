package coco;

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
}

