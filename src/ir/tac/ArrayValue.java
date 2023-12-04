package ir.tac;

import java.util.Deque;

public class ArrayValue extends Value {

    public Deque<Integer> dimensions;
    public Variable array;
    public Temporary offset;

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
