package ir.tac;

public abstract class Assign extends TAC{
    

    public Value left; // operand_1
    public Value right; // operand_2

    protected Assign(TacID id, Assignable dest, Value left, Value right) {
        super(id);
        if( dest == null ) {
            throw new NullPointerException();
        }
        if(dest instanceof Variable){
            ((Variable) dest).setInitialized(true);
        }
        this.dest = dest;
        this.left = left;
        if( left == null ) {
            if(left instanceof Variable && !((Variable)left).isInitialized()){
                ((Variable) left).getSym().setNullValue();
                this.left = left;
            }else{
                throw new NullPointerException();
            }
        }
        this.right = right;
        if( right == null ) {
            if(right instanceof Variable && !((Variable)right).isInitialized()){
                ((Variable) left).getSym().setNullValue();
                this.right = right;
            }else{
                throw new NullPointerException();
            }
        }
    }

    public abstract Literal calculate();

    public boolean hasImmediate() {
        return left instanceof Literal || right instanceof Literal;
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
