package ir.tac;

public interface Visitable {
    
    public <E> E accept(TACVisitor<E> visitor);
}
