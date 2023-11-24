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


    public DLX(String opcode, int regA, int regB, int regC, int immediate) {
        this.opcode = opcode;
        this.regA = regA;
        this.regB = regB;
        this.regC = regC;
        this.immediate = immediate;

        format = FORMAT.F1;
    }

    public DLX(String opcode, int regA, int regB, int regC) {
        this.opcode = opcode;
        this.regA = regA;
        this.regB = regB;
        this.regC = regC;

        format = FORMAT.F2;
    }

    public DLX(String opcode, int regC) {
        this.opcode = opcode;
        this.regC = regC;

        format = FORMAT.F3;
    }

    @Override
    public String toString() {
        // todo: add registers to this once they're linked
        return this.opcode + "\t";
    }

    private String opName() {
        switch(opcode) {

        };

        return null;
    }

    public String generateAssembly() {

    }

    public int generateInstruction() {

    }
}




