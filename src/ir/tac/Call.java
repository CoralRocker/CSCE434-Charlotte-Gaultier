package ir.tac;

import coco.Symbol;

import java.util.List;

public class Call extends TAC{
    
    public final Symbol function;
    public final Assignable dest;
    public final List<Value> args;
    // private ValueList args;

    public Call(TacID id, Symbol func, Assignable dest, List<Value> args) {
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
