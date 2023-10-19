package ir.cfg;


public class CFG implements Visitable {

    private BasicBlock head;

    public String asDotGraph() { return null; }

    @Override
    public void accept(CFGVisitor visitor) {

    }
}
