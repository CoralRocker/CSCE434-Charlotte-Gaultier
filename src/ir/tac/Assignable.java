package ir.tac;

public abstract class Assignable extends Value {

    public abstract String name();

    public abstract int hashCode();

    public Spill spilled = null;

    public int saveLocation = -1;
}
