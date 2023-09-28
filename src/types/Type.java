package types;

public abstract class Type {

    // arithmetic
    public Type mul (Type that) {
        if(this instanceof IntType && that instanceof IntType){
            return new IntType();
        }else if (this instanceof FloatType && that instanceof FloatType){
            return new FloatType();
        }
        return new ErrorType("Cannot multiply " + this + " with " + that + ".");
    }

    public Type div (Type that) {
        if(this instanceof IntType && that instanceof IntType){
            return new IntType();
        }else if (this instanceof FloatType && that instanceof FloatType){
            return new FloatType();
        }
        return new ErrorType("Cannot divide " + this + " by " + that + ".");
    }

    public Type mod (Type that) {
        if(this instanceof IntType || this instanceof FloatType) {
            if(that instanceof IntType){
                return new IntType();
            }
        }
        return new ErrorType("Cannot mod " + this + " by " + that + ".");
    }

    public Type pwr (Type that) {
        // TODO check actual type rules for power. not sure if this is correct
        if(this instanceof IntType && that instanceof IntType){
            return new IntType();
        }else if (this instanceof FloatType && that instanceof FloatType){
            return new FloatType();
        }
        return new ErrorType("Cannot pow " + this + " with " + that + ".");
    }

    public Type add (Type that) {
        if(this instanceof IntType && that instanceof IntType){
            return new IntType();
        }else if (this instanceof FloatType && that instanceof FloatType){
            return new FloatType();
        }
        return new ErrorType("Cannot add " + this + " to " + that + ".");
    }

    public Type sub (Type that) {
        if(this instanceof IntType && that instanceof IntType){
            return new IntType();
        }else if (this instanceof FloatType && that instanceof FloatType){
            return new FloatType();
        }
        return new ErrorType("Cannot subtract " + that + " from " + this + ".");
    }

    // boolean
    public Type and (Type that) {
        if(this instanceof BoolType && that instanceof BoolType){
            return new BoolType();
        }
        return new ErrorType("Cannot compute " + this + " and " + that + ".");
    }

    public Type or (Type that) {
        if(this instanceof BoolType && that instanceof BoolType){
            return new BoolType();
        }
        return new ErrorType("Cannot compute " + this + " or " + that + ".");
    }

    public Type not () {
        if(this instanceof BoolType){
            return new BoolType();
        }
        return new ErrorType("Cannot negate " + this + ".");
    }

    // relational
    public Type compare (Type that) {
        if(this instanceof IntType && that instanceof IntType){
            return new BoolType();
        }else if (this instanceof FloatType && that instanceof FloatType){
            return new BoolType();
        }else if (this instanceof BoolType && that instanceof BoolType){
            return new BoolType();
        }
        return new ErrorType("Cannot compare " + this + " with " + that + ".");
    }

    // designator
    public Type deref () {
        // TODO: implement
        return new ErrorType("Cannot dereference " + this);
    }

    public Type index (Type that) {
        // TODO: implement array type checking (and maybe bounds?)
        return new ErrorType("Cannot index " + this + " with " + that + ".");
    }

    // statements
    public Type assign (Type source) {
        if(this instanceof IntType && source instanceof IntType){
            return new IntType();
        }else if (this instanceof FloatType && source instanceof FloatType){
            return new FloatType();
        }else if (this instanceof BoolType && source instanceof BoolType) {
            return new BoolType();
        }
        return new ErrorType("Cannot assign " + source + " to " + this + ".");
    }

    public Type call (Type args) {
        // TODO: implement arg checking
        return new ErrorType("Cannot call " + this + " using " + args + ".");
    }

}
