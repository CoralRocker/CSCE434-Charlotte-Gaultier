package ir.tac;

public class Phi extends TAC {
    private Variable p1, p2;

    public Phi( int id, Variable v1, Variable v2 ) {
        super(id);
        this.p1 = v1;
        this.p2 = v2;

        if( v1.sym != v2.sym ) {
            throw new RuntimeException(String.format("Phi: Variables must be of same symbol: %s VS %s", v1.sym, v2.sym));
        }

        if( v1.asnNum == v2.asnNum || v1.asnNum == -1 || v2.asnNum == -1 ) {
            throw new RuntimeException(String.format("Phi: Variables must have different assignment numbers: %d VS %d", v1.asnNum, v2.asnNum));
        }
    }
    @Override
    public String genDot() {
        return String.format("Phi %s %s", p1, p2);
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
