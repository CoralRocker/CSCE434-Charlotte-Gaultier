package coco;

import java.util.Iterator;

/**
 * @brief Generate an iterable sequence from [0 - n)
 */
public class NumberSequence implements Iterator<Integer>, Iterable<Integer> {

    private int max;
    private int current = 0;

    public NumberSequence( int max ) {
        this.max = max;
    }

    public void reset() {
        current = 0;
    }

    @Override
    public boolean hasNext() {
        return current != max;
    }

    @Override
    public Integer next() {
        return current++;
    }

    @Override
    public Iterator<Integer> iterator() {
        return this;
    }
}
