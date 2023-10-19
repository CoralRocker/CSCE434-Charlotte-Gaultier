package ir.tac;

import ir.cfg.BasicBlock;

public class Branch extends TAC {
    private String rel;

    public void setRel(String rel) {
        this.rel = rel;
    }

    private BasicBlock jumpTo;

    public Branch(int id, String op) {
        super(id);

        this.rel = op;
        this.jumpTo = null;
    }

    public void setDestination(BasicBlock block) {
        this.jumpTo = block;
    }

    @Override
    public void accept(TACVisitor visitor) {

    }

    @Override
    public String genDot() {
        String jumpType;
        switch( rel ) {
            case ">" -> {
                jumpType = "bgt";
            }
            case ">=" -> {
                jumpType = "bge";
            }
            case "==" -> {
                jumpType = "beq";
            }
            case "!=" -> {
                jumpType = "bne";
            }
            case "<" -> {
                jumpType = "blt";
            }
            case "<=" -> {
                jumpType = "ble";
            }
            default -> {
                jumpType = "bra";
            }
        }

        return String.format("%s BB%d", jumpType, jumpTo.getNum());
    }
}
