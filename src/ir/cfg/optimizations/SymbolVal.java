package ir.cfg.optimizations;

import ir.tac.Assignable;
import ir.tac.Literal;

public class SymbolVal implements Cloneable {

    public final String sym; // Symbol with type
    public int instr; // Where the literal is assigned. Start at -1 and never reset. -1 indicates undefined
    public Literal val; // Null or the Literal Const Value
    public Assignable copy; // In copy prop, used to signify what to replace this symbol with.

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
            copy = other.copy;
            return val != null || copy != null;
        } else if (other.instr == -1) {
            return false;
        }

        // Copied + Copied = Copied if equal
        // Else return undefined.
        if( this.isCopied() && other.isCopied() ) {
            if( copy != other.copy ) {
                copy = null;
                instr = -1;
                return true;
            }
            else {
                return false;
            }
        }
        else if ( this.isCopied() ^ other.isCopied() ) {
            boolean changed = val == null && copy == null;

            copy = null;
            val = null;
            instr = -1;
            return !changed; // If we're already undefined, no change occurred.
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
        copy = null;
    }

    public SymbolVal(String s, int i ) {
        sym = s;
        instr = i;
        copy = null;
        val = null;
    }

    public SymbolVal(String s, int i, Assignable c) {
        sym = s;
        instr = i;
        val = null;
        copy = c;
    }

    public boolean assign(SymbolVal other) {
        if (!sym.equals(other.sym))
            throw new IllegalArgumentException(String.format("%s and %s are no the same symbol values!", this, other));

        if( other.isCopied() ) {
            val = null;
            instr = other.instr;
            copy = other.copy;
            return true;
        }

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
        return String.format("%s(%s:%s:%d)", sym, val, copy, instr);
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

    public boolean isCopied() { return copy != null; }

    @Override
    public SymbolVal clone() {
        var clone = new SymbolVal(sym, instr);
        if (val != null)
            clone.val = val.clone();
        if (copy != null)
            clone.copy = copy;
        return clone;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof SymbolVal))
            return false;

        return sym.equals(((SymbolVal) o).sym);
    }

    @Override
    public int hashCode() {
        return sym.hashCode();
    }
}
