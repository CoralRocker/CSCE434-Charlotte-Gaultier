package ir.cfg;

public class SSACreator extends CFGVisitor<Object> {

    public void modify( CFG cfg ) {
        visit( cfg.getHead() );
    }

    @Override
    public Object visit(BasicBlock blk) {
        blk.markVisited();

        System.out.println(blk);

        for( BasicBlock child : blk.getSuccessors() ) {
            if( !child.visited() ) {
                child.accept(this);
            }
        }

        return null;
    }
}
