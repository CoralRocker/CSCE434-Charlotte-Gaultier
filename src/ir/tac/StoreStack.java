package ir.tac;

public class StoreStack extends TAC {


    public Spill loc;

    public Value src;

    public Value offset;


    public TAC cause;

    public StoreStack(TacID id, Assignable val, Spill l, TAC cause) {
        super(id);
        this.dest = val;
        this.loc = l;
        this.cause = cause;
    }

    public StoreStack (TacID id, Value src, Value offset){
        super(id);
        this.src = src;
        this.offset = offset;
    }

    @Override
    public String genDot() {

        if (this.dest == null){
            return String.format("StoreStack[%s] %s", offset, src);
        }


        return String.format("StoreStack[%d from %s] %s (Caused by: %s)", -4 * loc.spillNo, loc.reg.name(), dest, cause.genDot());

    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
