package ir.tac;

public abstract class TAC implements Visitable{
    
    private TacID id; // instruction id

    private boolean eliminated; 

    protected TAC(TacID id) {
        this.id = id;
        this.eliminated = false;

        // saving code position will be helpful in debugging
    }

    public abstract String genDot();

    public int getId() { return id.getNum(); }

    public TacID getIdObj() { return id; }

    public void setId(int num){
        this.id.setNum(num);
    }

    public String opName() {
        return getClass().getSimpleName();
    }

    public Assignable dest;

}
