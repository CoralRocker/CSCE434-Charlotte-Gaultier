package ir.tac;

public class TacID {

    private TacIDGenerator parent;
    private int num;

    protected TacID(TacIDGenerator gen ) {
        parent = gen;
    }

    protected void setNum( int n ) {
        num = n;
    }

    public int getNum() {
        return num;
    }

    public void remove() {
        parent.remove( this );
        num = -1;
    }

    public void moveToEnd() {
        parent.moveToEnd( this );
    }

    public void moveToBlockFront(int blk) {
        parent.moveToBlockFront(blk, this);
    }

    public void moveRelative( int amt ) {
        parent.moveRelative(this, amt);
    }

    @Override
    public String toString() {
        return String.format("Instruction #%d", num);
    }
}
