package ir.tac;

public class Store extends TAC{

    public final Assignable dest;
    public final Value source;
    public Store(TacID id, Assignable dest, Value source) {
        super(id);
        this.dest = dest;
        this.source = source;
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String genDot() {
        return String.format("store %s %s", dest.toString(), source.toString());
    }
}
