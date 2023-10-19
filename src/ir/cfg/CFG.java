package ir.cfg;


public class CFG implements Visitable {

    private BasicBlock head;

    public String asDotGraph() {
        CFGPrinter printer = new CFGPrinter();
        return printer.genDotGraph(this);
    }

    public CFG(BasicBlock head) {
        this.head = head;
    }

    public BasicBlock getHead() {
        return head;
    }

    @Override
    public void accept(CFGVisitor visitor) {

    }
}
