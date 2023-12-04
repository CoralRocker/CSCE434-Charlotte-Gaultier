package ir.tac;

import ir.cfg.CodeGen.CodeGenerator;

/**
 * Spill is used to denote a reserved location on the current stack. The spillNo, is the
 * index of the spill beneath the frame pointer. Must be positive and not 0.
 */
public class Spill {
    public int spillNo;

    public enum Register {
        NONE(0),
        DEST(CodeGenerator.SPILL_DEST),
        LHS(CodeGenerator.SPILL_LHS),
        RHS(CodeGenerator.SPILL_RHS);

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
