package ir.tac;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class TacIDGenerator {

    private LinkedList< TacID > ids = new LinkedList<>();
    private Map<Integer, TacID> blockMap = new HashMap<>();

    public void newBlock(int blk, TacID id) {
        blockMap.put(blk, id);
    }

    public TacID pushFrontBlock(int blk) {
        TacID id = new TacID(this);
        ids.add(blockMap.get(blk).getNum(), id);
        genNum();
        return id;
    }

    public TacID push() {
        TacID id = new TacID(this);
        id.setNum( ids.size() );
        ids.addLast( id );
        return id;
    }

    public TacID pushAfter(TacID id) {
        int idx = ids.indexOf(id);
        TacID newID = new TacID(this);
        ids.add(idx+1, newID );
        genNum(idx);
        return newID;
    }

    public void genNum() {
        int ctr = 1;
        for( TacID id : ids )
            id.setNum(ctr++);
    }

    // Regenerate numbers after the given ID

    public void genNum(int idx) {
        int ctr = -1;
        for( int i = idx; i < ids.size(); i++ ) {
            ids.get(i).setNum(i);
        }
    }

    public void remove( TacID id ) {
        ids.remove( id.getNum() );
        genNum();
    }

    public void moveToEnd( TacID id ) {
        ids.remove( id.getNum() );
        ids.addLast(id);
        genNum();
    }
}
