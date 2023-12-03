package ir.tac;

public class LoadStack extends TAC {

    public final Spill loc;

    public final TAC cause;

    public LoadStack(TacID id, Assignable val, Spill loc, TAC cause) {
        super(id);
        this.dest = val;
        this.loc = loc;
        this.cause = cause;
    }

    @Override
    public String genDot() {
        return String.format("LoadStack[%d into %s ] %s (Caused by: %s)", -4 * loc.spillNo, loc.reg.name(), dest, cause.genDot());
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
