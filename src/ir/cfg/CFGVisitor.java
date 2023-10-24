package ir.cfg;

public abstract class CFGVisitor<E> {
    public abstract E visit(BasicBlock blk);
}
