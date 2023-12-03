package ir.cfg.CodeGen;

import ir.tac.TAC;

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

    public enum FORMAT {
        F1,
        F2,
        F3,
        UNRESOLVED_CALL,

        UNRESOLVED_BRANCH;
    }
    protected OPCODE opcode;
    public OPCODE getOpcode() {
        return opcode;
    }
    protected int regA;
    protected int regB;
    protected int regC;
    protected int immediate;

    public TAC getSource() {
        return source;
    }

    protected TAC source = null;

    public String getFuncSig() {
        return funcSig;
    }

    protected String funcSig = null;


    public FORMAT getFormat() {
        return format;
    }

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
        if( regA >= 32 || regA < 0 ) throw new RuntimeException(String.format("Register A is out of range: %d\n", regA));
        if( regB >= 32 || regB < 0 ) throw new RuntimeException(String.format("Register B is out of range: %d\n", regB));

        switch(format) {
            case F1 -> {
                if( regC >= 32 || regC < 0 ) throw new RuntimeException(String.format("Register C is out of range: %d\n", regC));
                if( immediate >= 65536 ) throw new RuntimeException(String.format("Immediate is out of range: %d\n", immediate));
                if( regC != 0 ) throw new RuntimeException(String.format("Register C should be 0: %d\n", regC));

                if( !isImmediate.apply(opcode) ) throw new RuntimeException(String.format("Opcode %s(%d) is not valid for format 1!", opcode.name(), opcode.opcode));
            }
            case F2 -> {
                if( regC >= 32 || regC < 0 ) throw new RuntimeException(String.format("Register C is out of range: %d\n", regC));
                if( immediate != 0 ) throw new RuntimeException(String.format("Immediate should be 0: %d\n", immediate));
                if( isImmediate.apply(opcode) || opcode.name().equals("JSR") ) throw new RuntimeException(String.format("Opcode %s(%d) is not valid for format 2!", opcode.name(), opcode.opcode));

            }
            case F3, UNRESOLVED_CALL -> {
                if( regC >= (Math.pow(2, 26))) throw new RuntimeException(String.format("Register C is out of range: %d\n", regC));
                if( !opcode.name().equals("JSR") ) throw new RuntimeException(String.format("Opcode %s(%d) is not valid for format 3!", opcode.name(), opcode.opcode));
            }

            case UNRESOLVED_BRANCH -> {
                if( immediate >= 65536 ) throw new RuntimeException(String.format("Immediate is out of range: %d\n", immediate));
                if( regC != 0 ) throw new RuntimeException(String.format("Register C should be 0: %d\n", regC));

                if( (opcode.opcode > 53) || (opcode.opcode < 47) ) throw new RuntimeException(String.format("Opcode %s(%d) is not valid for unresolved branch!", opcode.name(), opcode.opcode));

            }
        }
    }


    public static DLXCode immediateOp(OPCODE opcode, int regA, int regB, int immediate, TAC tac) {
        DLXCode dlx = new DLXCode();

        dlx.source = tac;
        dlx.opcode = opcode;
        dlx.regA = regA;
        dlx.regB = regB;
        dlx.immediate = immediate;

        dlx.format = FORMAT.F1;

        dlx.verifyValues();

        return dlx;
    }

    public static DLXCode regOp(OPCODE opcode, int regA, int regB, int regC, TAC tac) {
        DLXCode dlx = new DLXCode();

        dlx.source = tac;
        dlx.opcode = opcode;
        dlx.regA = regA;
        dlx.regB = regB;
        dlx.regC = regC;

        dlx.format = FORMAT.F2;

        dlx.verifyValues();

        return dlx;
    }

    public static DLXCode jumpOp(OPCODE opcode, int regC, TAC tac) {
        DLXCode dlx = new DLXCode();

        dlx.source = tac;
        dlx.opcode = opcode;
        dlx.regC = regC;

        dlx.format = FORMAT.F3;

        dlx.verifyValues();

        return dlx;
    }

    public static DLXCode unresolvedCall(OPCODE opcode, String funcSig, TAC tac ) {
        DLXCode dlx = new DLXCode();
        dlx.source = tac;
        dlx.opcode = opcode;
        dlx.funcSig = funcSig;
        dlx.format = FORMAT.UNRESOLVED_CALL;

        dlx.verifyValues();

        return dlx;
    }

    public static DLXCode unresolvedBranch(OPCODE opcode, int regA, int C, TAC tac) {
        DLXCode dlx = new DLXCode();
        dlx.source = tac;
        dlx.format = FORMAT.UNRESOLVED_BRANCH;
        dlx.opcode = opcode;
        dlx.regA = regA;
        dlx.regB = 0;
        dlx.regC = 0;
        dlx.immediate = C;
        dlx.verifyValues();

        return dlx;
    }

    @Override
    public String toString() {
        return this.generateAssembly();
    }


    public String generateAssembly(boolean srcline) {
        String debug = (source == null || !srcline) ? "" : String.format(":: %s", source.genDot());
        switch( format ) {
            case F1 -> {
                return String.format("%-4s R%-2d, R%-2d,  %-2d %s", opcode.name(), regA, regB, immediate, debug);
            }
            case F2 -> {
                return String.format("%-4s R%-2d, R%-2d, R%-2d %s", opcode.name(), regA, regB, regC, debug);
            }

            case F3 -> {
                return String.format("%-4s R%-2d               %s", opcode.name(), regC, debug);
            }

            case UNRESOLVED_CALL -> {
                return String.format("%-4s -> %s               %s", opcode.name(), funcSig, debug);
            }

            case UNRESOLVED_BRANCH -> {
                return String.format("%-4s R%-2d, BB%d         %s", opcode.name(), regA, immediate, debug);
            }
        }
        throw new RuntimeException("Unknown DLX format?");
    }

    public String generateAssembly() {
        return generateAssembly(false);
    }

    public int generateInstruction() {

        int instruction = 0;

        switch( format ) {
            case F1 -> {
                instruction = (opcode.opcode << 26) + (regA << 21) + (regB << 16) + (immediate & 0xFFFF);
            }
            case F2 -> {
                instruction = (opcode.opcode << 26) + (regA << 21) + (regB << 16) + (regC);
            }
            case F3 -> {
                instruction = (opcode.opcode << 26) + (regC);
            }
            default -> {
                throw new RuntimeException("Cannot generate code for format " + format.name());
            }
        }

        return instruction;
    }
}




