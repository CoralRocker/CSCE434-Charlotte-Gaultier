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
    public void accept(TACVisitor visitor) {

    }
}
