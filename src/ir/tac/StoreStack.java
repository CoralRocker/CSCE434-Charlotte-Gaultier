package ir.tac;

public class StoreStack extends TAC {

    public final Assignable val;

    public StoreStack(TacID id, Assignable val) {
        super(id);
        this.val = val;
    }

    @Override
    public String genDot() {
        return String.format("StoreStack %s", val);
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
