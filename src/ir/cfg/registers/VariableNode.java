package ir.cfg.registers;

import ir.tac.Assignable;
import ir.tac.Variable;

import java.util.HashSet;

public class VariableNode {
    public Assignable var;

    public boolean spill = false;
    public boolean exclude = false;

    public Integer assignedRegister = null;

    @Override
    public String toString() {
        if( assignedRegister == null ) {
            return var.toString();
        }
        else {
            return String.format("%s -> %%R%d", var, assignedRegister);
        }
    }

    public VariableNode(Assignable val) {
        var = val;
    }

    @Override
    public int hashCode() {
        return var.toString().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if( other instanceof Variable ) {
            return var.name().equals(((Variable) other).name());
        }
        else if( other instanceof VariableNode ) {
            return var.name().equals(((VariableNode) other).var.name());
        }

        return false;
    }
}
