package ir.cfg.optimizations;

import ir.tac.Literal;

public class SymbolVal implements Comparable<SymbolVal>, Cloneable {

    public final String sym; // Symbol with type
    public int instr; // Where the literal is assigned. Start at -1 and never reset. -1 indicates undefined
    public Literal val; // Null or the Literal Const Value

    private boolean same(SymbolVal o) {
        return this.sym.equals(o.sym)
                && this.instr == o.instr
                && (this.val == null
                ? this.val == o.val
                : this.val.equals(o.val));
    }

    // Merge Two Symbol Values together. Return whether the value changed.
    public boolean merge(SymbolVal other) {
        if (!sym.equals(other.sym))
            throw new IllegalArgumentException(String.format("%s and %s are no the same symbol values!", this, other));

        if (same(other))
            return false;

        // Undefined + Anything = Anything
        if (instr == -1 && other.instr != -1) {
            instr = other.instr;
            val = other.val;
            return true;
        } else if (other.instr == -1) {
            return false;
        }

        // Not Const + Anything = Not Const
        if (val == null || other.val == null) {
            boolean changed = val != null;
            val = null;
            if (changed)
                instr = other.instr;
            return changed;
        }


        // If constants are not equal, not constant
        if (!val.equals(other.val)) {
            val = null;
            instr = 0;
            return true;
        }

        return false;
    }

    public SymbolVal(String s, int i, Literal l) {
        sym = s;
        instr = i;
        val = l;
    }

    public boolean assign(SymbolVal other) {
        if (!sym.equals(other.sym))
            throw new IllegalArgumentException(String.format("%s and %s are no the same symbol values!", this, other));


        instr = other.instr;
        boolean changed;
        if (val != null) {
            changed = val.equals(other.val);
        } else {
            changed = val != other.val;
        }
        val = other.val;
        return changed;
    }

    @Override
    public String toString() {
        return String.format("%s(%s:%d)", sym, val, instr);
    }

    @Override
    public int compareTo(SymbolVal symbolVal) {
        return sym.compareTo(symbolVal.sym);
    }

    public boolean isConstant() {
        return instr != -1 && val != null;
    }

    public boolean isVariable() {
        return instr != -1 && val == null;
    }

    public boolean isUndefined() {
        return instr == -1;
    }

    public boolean isTemporary() {
        return sym.charAt(0) == '_';
    }

    @Override
    public SymbolVal clone() {
        var clone = new SymbolVal(sym, instr, null);
        if (val != null)
            clone.val = val.clone();
        return clone;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof SymbolVal))
            return false;

        return sym.equals(((SymbolVal) o).sym);
    }
}
