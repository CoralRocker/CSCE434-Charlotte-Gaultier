package ir.tac;

import ir.cfg.BasicBlock;

public class Branch extends TAC {
    private String rel;
    private Value val = null;

    public void setRel(String rel) {
        this.rel = rel;
    }

    private BasicBlock jumpTo;

    public Branch(TacID id, String op) {
        super(id);

        this.rel = op;
        this.jumpTo = null;
    }

    public void setDestination(BasicBlock block) {
        this.jumpTo = block;
    }


    public void setVal( Value val ) {
        this.val = val;
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

        if( val == null )
            return String.format("%s BB%d", jumpType, jumpTo.getNum());
        return String.format("%s %s BB%d", jumpType, val.toString(), jumpTo.getNum());
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
