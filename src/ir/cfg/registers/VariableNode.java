package ir.cfg.registers;

import ir.tac.Assignable;
import ir.tac.Variable;

public class VariableNode {
    public Assignable var;
    public final int ID;

    public VariableNode(int id, Assignable val) {
        this.ID = id;
        var = val;
    }

    @Override
    public int hashCode() {
        return var.name().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if( other instanceof Variable ) {
            return var.name().equals(((Variable) other).name());
        }
        else if( other instanceof VariableNode ) {
            return var.name().equals(((VariableNode) other).var.name());
        }
        else if( other instanceof Integer ) {
            return ID == ((Integer) other).intValue();
        }

        return false;
    }
}
