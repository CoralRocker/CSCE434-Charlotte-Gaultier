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

    protected List<Block> dom;

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
}
