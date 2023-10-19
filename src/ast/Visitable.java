package ast;

public interface Visitable {
    public <E> E accept( NodeVisitor<E> visitor );

}
