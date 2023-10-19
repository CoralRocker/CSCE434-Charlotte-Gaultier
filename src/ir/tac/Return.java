package ir.tac;

public class Return extends TAC{
    
    private Variable var;

    protected Return(int id) {
        super(id);
    }

    @Override
    public void accept(TACVisitor visitor) {

    }
}
