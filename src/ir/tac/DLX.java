package ir.tac;

import java.util.List;

public class DLX {

    enum OPCODE {
        ADD(0),
        SUB(1),
        MUL(2),
        DIV(3),
        MOD(4),
        POW(5),
        CMP(6),
        OR(13),
        AND(14),
        BIC(15),
        XOR(16),
        LSH(17),
        ASH(18),
        CHK(19),
        ADDI(20),
        SUBI(21),
        MULI(22),
        DIVI(23),
        MODI(24),
        POWI(25),
        CMPI(26),
        ORI( 33),
        ANDI(34),
        BICI(35),
        XORI(36),
        LSHI(37),
        ASHI(38),
        CHKI(39),
        LDW(40),
        LDX(41),
        POP(42),
        STW(43),
        STX(44),
        PSH(45),
        BEQ(47),
        BNE(48),
        BLT(49),
        BGE(50),
        BLE(51),
        BGT(52),
        BSR(53),
        JSR(54),
        RET(55),
        RDI(56),
        RDF(57),
        RDB(58),
        WRI(59),
        WRF(60),
        WRB(61),
        WRL(62);

        private int opcode;
        private OPCODE(int oc) {
            opcode = oc;
        }
    }

    enum FORMAT {
        F1,
        F2,
        F3;
    }
    private OPCODE opcode;
    private int regA;
    private int regB;
    private int regC;
    private int immediate;

    private FORMAT format;

    private void verifyValues() {
        switch(format) {
            case F1 -> {
                if( regA >= 32 ) throw new RuntimeException(String.format("Register A is out of range: %d\n", regA));
                if( regB >= 32 ) throw new RuntimeException(String.format("Register B is out of range: %d\n", regB));
                if( immediate >= 65536 ) throw new RuntimeException(String.format("Immediate is out of range: %d\n", immediate));
                if( regC != 0 ) throw new RuntimeException(String.format("Register C should be 0: %d\n", regC));
            }
            case F2 -> {
                if( regA >= 32 ) throw new RuntimeException(String.format("Register A is out of range: %d\n", regA));
                if( regB >= 32 ) throw new RuntimeException(String.format("Register B is out of range: %d\n", regB));
                if( immediate != 0 ) throw new RuntimeException(String.format("Immediate should be 0: %d\n", immediate));
                if( regC >= 32 ) throw new RuntimeException(String.format("Register C is out of range: %d\n", regC));
            }
            case F3 -> {
                if( regC >= (Math.pow(2, 26))) throw new RuntimeException(String.format("Register C is out of range: %d\n", regC));
            }
        }
    }


    public static DLX immediateOp(OPCODE opcode, int regA, int regB, int immediate) {
        DLX dlx = new DLX();

        dlx.opcode = opcode;
        dlx.regA = regA;
        dlx.regB = regB;
        dlx.immediate = immediate;

        dlx.format = FORMAT.F1;

        dlx.verifyValues();

        return dlx;
    }

    public static DLX regOp(OPCODE opcode, int regA, int regB, int regC) {
        DLX dlx = new DLX();

        dlx.opcode = opcode;
        dlx.regA = regA;
        dlx.regB = regB;
        dlx.regC = regC;

        dlx.format = FORMAT.F2;

        dlx.verifyValues();

        return dlx;
    }

    public static DLX jumpOp(OPCODE opcode, int regC) {
        DLX dlx = new DLX();

        dlx.opcode = opcode;
        dlx.regC = regC;

        dlx.format = FORMAT.F3;

        dlx.verifyValues();

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
        switch( format ) {
            case F1 -> {
                return String.format("%-4s R%d, R%d, %d", opcode.name(), regA, regB, immediate);
            }
            case F2 -> {
                return String.format("%-4s R%d, R%d, R%d", opcode.name(), regA, regB, regC);
            }

            case F3 -> {
                return String.format("%-4s R%d", opcode.name(), regC);
            }
        }
        throw new RuntimeException("Unknown DLX format?");
    }

    public int generateInstruction() {

        int instruction = 0;

        switch( format ) {
            case F1 -> {
                instruction = opcode.opcode + (regA >> 6) + (regB >> 11) + (immediate >> 16);
            }
            case F2 -> {
                instruction = opcode.opcode + (regA >> 6) + (regB >> 11) + (regC >> 27);
            }
            case F3 -> {
                instruction = opcode.opcode + (regC >> 6);
            }
        }

        return instruction;
    }
}




