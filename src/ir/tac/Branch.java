package ir.tac;

public class Branch extends TAC {
    private Value cond;
    private Label instr;

    protected Branch(int id, Value val, Label lbl) {
        super(id);

        this.cond = val;
        this.instr = lbl;
    }

    @Override
    public void accept(TACVisitor visitor) {

    }
}
