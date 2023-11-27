package ir.tac;

/**
 * Spill is used to denote a reserved location on the current stack. The spillNo, is the
 * index of the spill beneath the frame pointer. Must be positive and not 0.
 */
public class Spill {
    public final int spillNo;

    public enum Register {
        NONE(0),
        DEST(27),
        LHS(26),
        RHS(25);

        public final int num;
        private Register(int n) { num = n; }

    }

    public Register reg;

    public Spill(int spilloc, Register reg) {
        if( spilloc <= 0 ) throw new IllegalArgumentException("Spill Number must be >= 1");
       spillNo = spilloc;
       this.reg = reg;
    }

    public Spill(Spill src, Register reg) {
        spillNo = src.spillNo;
        this.reg = reg;
    }
}
