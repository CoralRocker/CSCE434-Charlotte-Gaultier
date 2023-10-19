package ir.tac;

import ir.cfg.BasicBlock;

public class Branch extends TAC {
    private Value cond;
    private String label;

    protected Branch(int id, Value val, String label) {
        super(id);

        this.cond = val;
        this.label = label;
    }

    @Override
    public void accept(TACVisitor visitor) {

    }

    @Override
    public String genDot() {
        return null;
    }
}
