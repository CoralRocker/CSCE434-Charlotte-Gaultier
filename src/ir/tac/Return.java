package ir.tac;

public class Return extends TAC{
    
    private Value var;

    public Return(TacID id, Value var) {
        super(id);
        this.var = var;
    }

    @Override
    public String genDot() {
        return String.format("ret %s", var);
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
