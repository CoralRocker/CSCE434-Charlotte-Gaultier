package ir.tac;

import ir.cfg.CFG;

import java.util.Deque;

public class ArrayValue extends Value {

    public Deque<Integer> dimensions;
    public Variable array;
    public Temporary offset;

    public StoreStack genStore(CFG cfg, Value src) {
        return new StoreStack(cfg.instrNumberer.push(), array, src, offset);
    }

    public Load genLoad(CFG cfg, Assignable dest) {
        return new Load(cfg.instrNumberer.push(), dest, array, offset);
    }

    @Override
    public String toString() {
        return String.format("%s [ %s ] (ndim %s)", array, offset, dimensions);
    }

    @Override
    public boolean isConst() {


        return false;
    }

    @Override
    public boolean equals(Object other) {
        return false;
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        throw new RuntimeException("Should not be attempting to visit an ArrayValue!");
    }
}
