package ir.cfg;

public interface Visitable<E> {
    
    public E accept (CFGVisitor<E> visitor);
}
