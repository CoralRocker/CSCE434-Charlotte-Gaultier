package ir.tac;

import ir.cfg.BasicBlock;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class TacIDGenerator {

    private LinkedList< TacID > ids = new LinkedList<>();
    private Map<Integer, TacID> blockMap = new HashMap<>();

    private int markNewBlock = -1;

    public void newBlock(int blk, TacID id) {
        blockMap.put(blk, id);
    }

    public void newBlock( int blk ) {
        markNewBlock = blk;
        blockMap.put(blk, null);
    }

    public void newBlock(BasicBlock blk) {
        if( blk.getNum() == -1 ) {
            throw new RuntimeException("Cannot mark new block with an uninitialized one!");
        }

        markNewBlock = blk.getNum();
        blockMap.put(blk.getNum(), null);
    }

    public TacID pushFrontBlock(int blk) {
        TacID id = new TacID(this);
        ids.add(blockMap.get(blk).getNum(), id);
        genNum();
        return id;
    }

    public TacID push() {
        TacID id = new TacID(this);
        ids.addLast( id );
        id.setNum( ids.size() );
        if( markNewBlock != -1 ) {
            blockMap.put(markNewBlock, id);
            markNewBlock = -1;
        }
        return id;
    }

    public void genNum() {
        int ctr = 1;
        for( TacID id : ids )
            id.setNum(ctr++);
    }

    public void remove( TacID id ) {
        ids.remove( id.getNum()-1 );
        genNum();
    }

    // Move the given ID some number of positions forward or backward.
    // A negative value indicates rearward movement and vice versa
    public void moveRelative( TacID id, int amt ) {
        ids.add(id.getNum()-1+amt, id);
        if( amt < 0 ) {
            ids.remove(id.getNum());
        }
        else {
            ids.remove(id.getNum()-1);
        }
        genNum();
    }

    public void moveToEnd( TacID id ) {
        ids.remove( id.getNum()-1 );
        ids.addLast(id);
        genNum();
    }

    public void moveToBlockFront( int blk, TacID id ) {
        var obj = blockMap.get(blk);
        ids.remove( id );
        ids.add( obj.getNum()-1, id );
        blockMap.put(blk, id);
        genNum();
    }
}
