package ir.tac;

public class Return extends TAC{
    
    private Value var;

    protected Return(TacID id) {
        super(id);
    }

    @Override
    public String genDot() {
        return null;
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
