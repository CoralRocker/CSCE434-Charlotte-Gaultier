package ir.tac;

import coco.Symbol;

public abstract class Value implements Visitable {

    public abstract boolean isConst();

    public boolean isGlobal = false;


//    public abstract Symbol getSym();
    public abstract boolean equals(Object other);
}
