package ir.tac;

import ir.cfg.BasicBlock;

import java.util.*;
import java.util.stream.Collectors;

class BiHashMap<K, V> {
    private HashMap<K, V> forward = new HashMap<>();
    private HashMap<V, K> inverse = new HashMap<>();

    public void put(K k, V v) {
        forward.put(k, v);
        inverse.put(v, k);

    }

    public V remove(K k) {
        V v = forward.remove(k);
        inverse.remove(v, k);
        return v;
    }

    public K getInverse(V v) {
        return inverse.get(v);
    }

    public V get(K k) {
        return forward.get(k);
    }

    public boolean contains(K k) {
        return forward.containsKey(k);
    }

    public boolean containsInverse(V v) {
        return inverse.containsKey(v);
    }

}

public class TacIDGenerator {

    private LinkedList< TacID > ids = new LinkedList<>();
    private BiHashMap<Integer, TacID> blockMap = new BiHashMap<>();

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

    public TacID pushAfter(TacID id) {
        TacID newID = new TacID(this);
        ids.add( id.getNum(), newID );

        genNum();
        return newID;
    }
    public TacID pushBefore(TacID id) {
        Integer blkId = blockMap.getInverse(id);

        TacID newID = new TacID(this);
        ids.add( id.getNum()-1, newID );

        genNum();
        if( blkId != null ) {
            blockMap.put(blkId, newID);
        }
        return newID;
    }

    public void removeBlock( BasicBlock blk ) {
        int num = blk.getNum();
        blockMap.remove(num);

        ids.removeAll( blk.getInstructions().stream().map(TAC::getIdObj).toList() );

        genNum();
    }

    // Require that the list in cont is in order
    public <C extends Collection<TacID>> void removeAll( C cont ) {
        if( cont.isEmpty() ) return;

        var genIter = ids.listIterator();
        var contIter = cont.iterator();

        TacID genID = null, contID = null;
        while( genIter.hasNext() ) {
            if( genID == null ) genID = genIter.next();
            if( contID == null ) contID = contIter.next();

            if( genID == contID ) {
                contID = null;
                genIter.remove();
                if( blockMap.containsInverse(genID) ) {
                    Integer blk = blockMap.getInverse(genID);
                    if( genIter.hasNext() ) genID = genIter.next();
                    else genID = null;

                    if( !(genID != null && blockMap.containsInverse(genID)) )
                        blockMap.put(blk, genID);

                }
                if( !contIter.hasNext() ) {
                    genNum();
                    return;
                }
            }
            else {
                genID = null;
            }
        }

        if( contID != null ) throw new RuntimeException("Not all TacIDs given existed!");

    }
}
