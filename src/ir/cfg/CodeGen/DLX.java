package ir.cfg.CodeGen;

import java.util.function.Function;

public class DLXCode {

    public enum OPCODE {
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
    public OPCODE getOpcode() {
        return opcode;
    }
    private int regA;
    private int regB;
    private int regC;
    private int immediate;

    private FORMAT format;

    private void verifyValues() {

        Function<OPCODE, Boolean> isImmediate = (OPCODE oc) -> {
            boolean miscImm;
            switch( oc ) {
                case LDW:
                case POP:
                case STW:
                case PSH:
                case WRL:
                    miscImm = true;
                    break;
                default:
                    miscImm = false;
                    break;
            }

            return (oc.opcode <= 26 && oc.opcode >= 20)
                || (oc.opcode <= 39 && oc.opcode >= 33)
                || (oc.opcode <= 53 && oc.opcode >= 47)
                || miscImm;
        };

        switch(format) {
            case F1 -> {
                if( regA >= 32 ) throw new RuntimeException(String.format("Register A is out of range: %d\n", regA));
                if( regB >= 32 ) throw new RuntimeException(String.format("Register B is out of range: %d\n", regB));
                if( immediate >= 65536 ) throw new RuntimeException(String.format("Immediate is out of range: %d\n", immediate));
                if( regC != 0 ) throw new RuntimeException(String.format("Register C should be 0: %d\n", regC));

                if( !isImmediate.apply(opcode) ) throw new RuntimeException(String.format("Opcode %s(%d) is not valid for format 1!", opcode.name(), opcode.opcode));
            }
            case F2 -> {
                if( regA >= 32 ) throw new RuntimeException(String.format("Register A is out of range: %d\n", regA));
                if( regB >= 32 ) throw new RuntimeException(String.format("Register B is out of range: %d\n", regB));
                if( immediate != 0 ) throw new RuntimeException(String.format("Immediate should be 0: %d\n", immediate));
                if( regC >= 32 ) throw new RuntimeException(String.format("Register C is out of range: %d\n", regC));
                if( isImmediate.apply(opcode) || opcode.name().equals("JSR") ) throw new RuntimeException(String.format("Opcode %s(%d) is not valid for format 2!", opcode.name(), opcode.opcode));

            }
            case F3 -> {
                if( regC >= (Math.pow(2, 26))) throw new RuntimeException(String.format("Register C is out of range: %d\n", regC));
                if( !opcode.name().equals("JSR") ) throw new RuntimeException(String.format("Opcode %s(%d) is not valid for format 3!", opcode.name(), opcode.opcode));
            }
        }
    }


    public static DLXCode immediateOp(OPCODE opcode, int regA, int regB, int immediate) {
        DLXCode dlx = new DLXCode();

        dlx.opcode = opcode;
        dlx.regA = regA;
        dlx.regB = regB;
        dlx.immediate = immediate;

        dlx.format = FORMAT.F1;

        dlx.verifyValues();

        return dlx;
    }

    public static DLXCode regOp(OPCODE opcode, int regA, int regB, int regC) {
        DLXCode dlx = new DLXCode();

        dlx.opcode = opcode;
        dlx.regA = regA;
        dlx.regB = regB;
        dlx.regC = regC;

        dlx.format = FORMAT.F2;

        dlx.verifyValues();

        return dlx;
    }

    public static DLXCode jumpOp(OPCODE opcode, int regC) {
        DLXCode dlx = new DLXCode();

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
                instruction = (opcode.opcode << 26) + (regA << 21) + (regB << 16) + (immediate);
            }
            case F2 -> {
                instruction = (opcode.opcode << 26) + (regA << 21) + (regB << 16) + (regC);
            }
            case F3 -> {
                instruction = (opcode.opcode << 26) + (regC);
            }
        }

        return instruction;
    }
}




