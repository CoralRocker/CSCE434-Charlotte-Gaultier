package ir.tac;

public class StoreStack extends TAC {


    public Spill loc;

    public Value src;

    public Value offset;

    public TAC cause;

    public boolean isSpill() {
        return dest != null && loc != null && cause != null;
    }

    public boolean isArray() {
        return src != null && offset != null;
    }

    public StoreStack(TacID id, Assignable val, Spill l, TAC cause) {
        super(id);
        this.dest = val;
        this.loc = l;
        this.cause = cause;
    }

    // Store an item (src) onto the stack at some offsett from the contents of dest
    // Used for array storing...
    public StoreStack(TacID id, Assignable dest, Value src, Value offset ) {
        super(id);
        this.dest = dest;
        this.src = src;
        this.offset = offset;
    }

    public StoreStack (TacID id, Value src, Value offset){
        super(id);
        this.src = src;
        this.offset = offset;
    }

    @Override
    public String genDot() {

        if ( isArray() ){
            if( dest != null ) {
                return String.format("StoreStack %s[%s] %s", dest, offset, src);
            }
            return String.format("StoreStack[%s] %s", offset, src);
        }


        return String.format("StoreStack[%d from %s] %s (Caused by: %s)", -4 * loc.spillNo, loc.reg.name(), dest, cause.genDot());

    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
