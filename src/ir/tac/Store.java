package ir.tac;

public class Store extends TAC{

    private final Variable dest;
    private final Value source;
    public Store(int id, Variable dest, Value source) {
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
        return String.format("store %s %s", source.toString(), dest.toString());
    }
}
