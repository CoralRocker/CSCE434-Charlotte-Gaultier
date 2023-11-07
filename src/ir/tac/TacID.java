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
    }

    public void moveToEnd() {
        parent.moveToEnd( this );
    }
}
