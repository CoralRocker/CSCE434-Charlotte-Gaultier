package ir.tac;

import coco.Symbol;

public class Call extends TAC{
    
    private Symbol function;
    // private ValueList args;

    public Call(int id, Symbol func) {
        super(id);
        function = func;
    }

    @Override
    public String genDot() {
        return String.format("call %s", function.name());
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
