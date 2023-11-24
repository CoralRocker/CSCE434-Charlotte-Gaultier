package ir.tac;

import java.util.List;

public class DLX {
    enum FORMAT {
        F1,
        F2,
        F3;
    }
    private String opcode;
    private int regA;
    private int regB;
    private int regC;
    private int immediate;

    private FORMAT format;


    public static DLX immediateOp(String opcode, int regA, int regB, int regC, int immediate) {
        DLX dlx = new DLX();

        dlx.opcode = opcode;
        dlx.regA = regA;
        dlx.regB = regB;
        dlx.immediate = immediate;

        dlx.format = FORMAT.F2;

        return dlx;
    }

    public static DLX regOp(String opcode, int regA, int regB, int regC) {
        DLX dlx = new DLX();

        dlx.opcode = opcode;
        dlx.regA = regA;
        dlx.regB = regB;
        dlx.regC = regC;

        dlx.format = FORMAT.F1;

        return dlx;
    }

    public static DLX jumpOp(String opcode, int regC) {
        DLX dlx = new DLX();

        dlx.opcode = opcode;
        dlx.regC = regC;

        dlx.format = FORMAT.F3;

        return dlx;
    }

    @Override
    public String toString() {
        return this.generateAssembly();
    }

    private String opName() {
        switch(opcode) {

        };

        return null;
    }

    public String generateAssembly() {
        // todo: add registers to this once they're linked
        return this.opcode + "\t";
    }

    public int[] generateInstruction() {
        // TODO translate DLX object into machine code
        return new int[]{};
    }
}




