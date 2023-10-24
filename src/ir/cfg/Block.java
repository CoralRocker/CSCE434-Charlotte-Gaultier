package ir.cfg;

import java.util.HashMap;
import java.util.List;

public abstract class Block implements Visitable<Void> {

    boolean visited;

    protected HashMap<BasicBlock, Boolean> visitMap = new HashMap<>();

    public void setVisited( BasicBlock blk) {
        if( visitMap.containsKey(blk) ) {
            visitMap.replace(blk, true);
        }
    }

    public boolean allVisited() {
        boolean visited = true;
        for( Boolean visit : visitMap.values() ) {
            visited &= visit;
        }

        return visited;
    }

    protected List<BasicBlock> domBy, domTo, domFrontier;
    protected BasicBlock idom = null;

    protected Block () {
        visited = false;
    }

    public boolean visited () {
        return visited;
    }

    public void markVisited () {
        visited = true;
    }

    public abstract void resetVisited ();

    public abstract int getNum();

    public BasicBlock getIDom() {

        int minblk = -1, minidx = -1;
        for( int i = 0; i < domBy.size(); i++ ) {
            BasicBlock blk = domBy.get(i);

            if( blk == this )
                continue;

            if( minidx == -1 ) {
                minidx = i;
                minblk = blk.getNum();
            }
            else if( (getNum() - blk.getNum()) < (getNum() - minblk) ) {
                minidx = i;
                minblk = blk.getNum();
            }
        }

        if( minidx == -1 ) {
            return null;
        }

        idom = domBy.get(minidx);
        return idom;
    }
}
