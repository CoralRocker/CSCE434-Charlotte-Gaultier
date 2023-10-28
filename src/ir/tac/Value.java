package ir.tac;

public abstract class Value implements Visitable {

    public abstract boolean isConst();

    public abstract boolean equals(Object other);
}
