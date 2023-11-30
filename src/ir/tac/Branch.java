package ir.tac;

import ir.cfg.BasicBlock;

public class Branch extends TAC {
    public String getRel() {
        return rel;
    }

    private String rel;

    public Assignable getVal() {
        return val;
    }

    private Assignable val = null;

    public void setRel(String rel) {
        this.rel = rel;
    }

    public BasicBlock getJumpTo() {
        return jumpTo;
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

    public boolean isConditional() {
        switch( rel ) {
            case ">" :
            case ">=":
            case "==":
            case "!=":
            case "<" :
            case "<=":
                return true;
            default  :
                return false;
        }
    }

    public void setVal( Assignable val ) {
        this.val = val;
    }

    public void invertRelation() {
        switch( rel ) {
            case ">" -> {
                rel = "<=";
            }
            case ">=" -> {
                rel = "<";
            }
            case "==" -> {
                rel = "!=";
            }
            case "!=" -> {
                rel = "==";
            }
            case "<" -> {
                rel = ">=";
            }
            case "<=" -> {
                rel = ">";
            }
        }
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

    /**
     * Given the output from a cmp, return the decision made
     *
     * @return true if branch is always taken, false if always not, null if it cannot be determined
     */
    public Boolean calculate( int v ) {
        switch( rel ) {
            case ">" -> {
                return v > 0;
            }
            case ">=" -> {
                return v >= 0;
            }
            case "==" -> {
                return v == 0;
            }
            case "!=" -> {
                return v != 0;
            }
            case "<" -> {
                return v < 0;
            }
            case "<=" -> {
                return v <= 0;
            }
            default -> {
                throw new RuntimeException("Uknown branch type: " + rel);
            }
        }
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
