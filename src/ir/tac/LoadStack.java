package ir.tac;

public class LoadStack extends TAC {

    public final Spill loc;

    public final TAC cause;

    public final Assignable target;

    public final Spill.Register regHint;

    public boolean isLoadSpill() {
        return loc != null;
    }

    public boolean isLoadDest() {
        return target != null && loc == null;
    }


    public LoadStack(TacID id, Assignable val, Spill loc, TAC cause) {
        super(id);
        this.dest = val;
        this.loc = loc;
        this.cause = cause;
        this.target = null;
        this.regHint = null;
        throw new RuntimeException("Don't use this anymore.");
    }

    public LoadStack(TacID id, Assignable val, Assignable dest, TAC cause, Spill.Register hint) {
        super(id);
        this.loc = null;
        this.cause = cause;
        this.dest = val;
        this.target = dest;
        this.regHint = hint;
    }
    public LoadStack(TacID id, Assignable val, Assignable dest, TAC cause) {
        super(id);
        this.loc = null;
        this.cause = cause;
        this.dest = val;
        this.target = dest;
        this.regHint = null;
    }

    @Override
    public String genDot() {
        if( isLoadDest() ) {
            String hint = "";
            if( regHint != null ) hint = String.format("(Hint: %s)", regHint.name());
            return String.format("LoadStack[ %s %s ] to %s (Caused by: %s)", dest, hint, target, cause.genDot());
        }
        return String.format("LoadStack[%d into %s ] %s (Caused by: %s)", -4 * loc.spillNo, loc.reg.name(), dest, cause.genDot());
    }

    @Override
    public <E> E accept(TACVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
