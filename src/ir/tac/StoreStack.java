package ir.tac;

public class StoreStack extends TAC {

    public final Assignable val;
    public final Spill loc;

    public StoreStack(TacID id, Assignable val, Spill l) {
        super(id);
        this.val = val;
        this.loc = l;
    }

    @Override
    public String genDot() {
        return String.format("StoreStack[%d] %s", loc.spillNo, val);
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
