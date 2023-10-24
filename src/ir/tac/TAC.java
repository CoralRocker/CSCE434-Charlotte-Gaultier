package ir.tac;

public abstract class TAC implements Visitable{
    
    private int id; // instruction id

    private boolean eliminated; 

    protected TAC(int id) {
        this.id = id;
        this.eliminated = false;

        // saving code position will be helpful in debugging
    }

    public abstract String genDot();

    public int getId() { return id; }

    public int setId(int i) {
        id = i;
        return id;
    }
}
