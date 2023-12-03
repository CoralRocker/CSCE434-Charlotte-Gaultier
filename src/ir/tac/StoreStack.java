package ir.tac;

public class StoreStack extends TAC {

    public Assignable val;
    public Spill loc;

    public Value src;

    public Value offset;

    public StoreStack(TacID id, Assignable val, Spill l) {
        super(id);
        this.val = val;
        this.loc = l;
    }

    public StoreStack (TacID id, Value src, Value offset){
        super(id);
        this.src = src;
        this.offset = offset;
    }

    @Override
    public String genDot() {
        if (this.val == null){
            return String.format("StoreStack[%s] %s", offset, src);
        }
        return String.format("StoreStack[%d] %s", loc.spillNo, val);
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
