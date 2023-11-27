package ir.tac;

public class Store extends TAC{


    public Value source;
    public Store(TacID id, Assignable dest, Value source) {
        super(id);
        if(dest instanceof Variable){
            ((Variable) dest).setInitialized(true);
        }
        this.dest = dest;
        this.source = source;
        if( source == null ) {
            throw new NullPointerException();
        }else if(source instanceof Variable && !((Variable)source).isInitialized()){
            ((Variable) source).getSym().setNullValue();
            this.source = source;
        }
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String genDot() {
        return String.format("store %s %s", dest.toString(), source.toString());
    }
}
