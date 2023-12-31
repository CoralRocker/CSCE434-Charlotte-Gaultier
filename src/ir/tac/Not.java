package ir.tac;

public class Not extends TAC {
    public Value getSrc() {
        return src;
    }

    public Value src;

    public Not(TacID id, Assignable dest, Value src) {
        super(id);
        super.dest = dest;
        this.src = src;
    }

    public Literal calculate() {
        boolean lit = ((Literal)src).getBool();
        return Literal.get( !lit );
    }

    @Override
    public String genDot() {
        return String.format("NOT %s %s", dest, src);
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
