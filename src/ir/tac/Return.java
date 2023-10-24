package ir.tac;

public class Return extends TAC{
    
    private Variable var;

    protected Return(int id) {
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
