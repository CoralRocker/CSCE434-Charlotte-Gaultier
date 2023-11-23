package ir.tac;

import java.util.List;

public class DLX {
    private String opcode;
    private int regA;
    private int regB;
    private int regC;
    private int immediate;

    public DLX(String opcode, int regA, int regB, int regC, int immediate) {
        this.opcode = opcode;
        this.regA = regA;
        this.regB = regB;
        this.regC = regC;
        this.immediate = immediate;
    }

    public DLX(String opcode, int regA, int regB, int regC) {
        this.opcode = opcode;
        this.regA = regA;
        this.regB = regB;
        this.regC = regC;
    }

    public DLX(String opcode, int regC) {
        this.opcode = opcode;
        this.regC = regC;
    }
}




