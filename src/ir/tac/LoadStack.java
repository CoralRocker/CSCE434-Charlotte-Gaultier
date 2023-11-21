package ir.tac;

public class LoadStack extends TAC {

    public final Assignable val;
    public final Spill loc;

    public LoadStack(TacID id, Assignable val, Spill loc) {
        super(id);
        this.val = val;
        this.loc = loc;
    }

    @Override
    public String genDot() {
        return String.format("LoadStack %s, #%d", val, loc.spillNo);
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
