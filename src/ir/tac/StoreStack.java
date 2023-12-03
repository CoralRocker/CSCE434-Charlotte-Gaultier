package ir.tac;

public class StoreStack extends TAC {
    public final Spill loc;

    public final TAC cause;

    public StoreStack(TacID id, Assignable val, Spill l, TAC cause) {
        super(id);
        this.dest = val;
        this.loc = l;
        this.cause = cause;
    }

    @Override
    public String genDot() {
        return String.format("StoreStack[%d from %s] %s (Caused by: %s)", -4 * loc.spillNo, loc.reg.name(), dest, cause.genDot());
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
