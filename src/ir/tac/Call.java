package ir.tac;

import coco.Symbol;

public class Call extends TAC{
    
    public final Symbol function;
    public final Assignable dest;
    // private ValueList args;

    public Call(int id, Symbol func, Assignable dest) {
        super(id);
        function = func;
        this.dest = dest;
    }

    @Override
    public String genDot() {
        return String.format("call %s %s", dest, function.name());
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
