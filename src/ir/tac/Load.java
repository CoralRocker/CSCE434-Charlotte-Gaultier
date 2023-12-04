package ir.tac;



public class Load extends TAC{

    public Assignable dest;
    public Value base;
    public Value offset;
    public Load(TacID id, Assignable dest, Value base, Value offset) {
        super(id);
        if(dest instanceof Variable){
            ((Variable) dest).setInitialized(true);
        }
        this.dest = dest;
        this.base = base;
        this.offset = offset;
        if( base == null ) {
            throw new NullPointerException();
        }
    }

    public Load(TacID id, Assignable dest, Value offset) {
        super(id);
        if(dest instanceof Variable){
            ((Variable) dest).setInitialized(true);
        }
        this.dest = dest;
        this.offset = offset;
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String genDot() {

        if (base == null) {
            return String.format("load %s %s %s", dest.toString(), "base", offset.toString());
        }
        return String.format("load %s %s[ %s ]", dest.toString(), base.toString(), offset.toString());
    }
}
