package types;

import java.util.Objects;

public abstract class Type {

    // arithmetic
    public Type mul (Type that) {
        if (this instanceof PtrType){
            return this.deref().mul(that);
        }
        if (that instanceof PtrType){
            that = that.deref();
        }
        if(this instanceof IntType && that instanceof IntType){
            return new IntType();
        }else if (this instanceof FloatType && that instanceof FloatType){
            return new FloatType();
        }
        return new ErrorType("Cannot multiply " + this + " with " + that + ".");
    }

    public Type div (Type that) {
        if (this instanceof PtrType){
            return this.deref().div(that);
        }
        if (that instanceof PtrType){
            that = that.deref();
        }
        if(this instanceof IntType && that instanceof IntType){
            return new IntType();
        }else if (this instanceof FloatType && that instanceof FloatType){
            return new FloatType();
        }
        return new ErrorType("Cannot divide " + this + " by " + that + ".");
    }

    public Type mod (Type that) {
        if (this instanceof PtrType){
            return this.deref().mod(that);
        }
        if (that instanceof PtrType){
            that = that.deref();
        }
        if(this instanceof IntType || this instanceof FloatType) {
            if(that instanceof IntType){
                return new IntType();
            }
        }
        return new ErrorType("Cannot modulo " + this + " by " + that + ".");
    }

    public Type pwr (Type that) {
        if (this instanceof PtrType){
            return this.deref().pwr(that);
        }
        if (that instanceof PtrType){
            that = that.deref();
        }
        // TODO check actual type rules for power. not sure if this is correct
        if(this instanceof IntType && that instanceof IntType){
            return new IntType();
        }else if (this instanceof FloatType && that instanceof FloatType){
            return new FloatType();
        }
        return new ErrorType("Cannot raise " + this + " to " + that + ".");
    }

    public Type add (Type that) {
        if (this instanceof PtrType){
            return this.deref().add(that);
        }
        if (that instanceof PtrType){
            that = that.deref();
        }
        if(this instanceof IntType && that instanceof IntType){
            return new IntType();
        }else if (this instanceof FloatType && that instanceof FloatType){
            return new FloatType();
        }
        return new ErrorType("Cannot add " + this + " to " + that + ".");
    }

    public Type sub (Type that) {
        if (this instanceof PtrType){
            return this.deref().sub(that);
        }
        if (that instanceof PtrType){
            that = that.deref();
        }
        if(this instanceof IntType && that instanceof IntType){
            return new IntType();
        }else if (this instanceof FloatType && that instanceof FloatType){
            return new FloatType();
        }
        return new ErrorType("Cannot subtract " + that + " from " + this + ".");
    }

    // boolean
    public Type and (Type that) {
        if (this instanceof PtrType){
            return this.deref().and(that);
        }
        if (that instanceof PtrType){
            that = that.deref();
        }
        if(this instanceof BoolType && that instanceof BoolType){
            return new BoolType();
        }
        return new ErrorType("Cannot compute " + this + " and " + that + ".");
    }

    public Type or (Type that) {
        if (this instanceof PtrType){
            return this.deref().or(that);
        }
        if (that instanceof PtrType){
            that = that.deref();
        }
        if(this instanceof BoolType && that instanceof BoolType){
            return new BoolType();
        }
        return new ErrorType("Cannot compute " + this + " or " + that + ".");
    }

    public Type not () {
        if (this instanceof PtrType){
            return this.deref().not();
        }
        if(this instanceof BoolType){
            return new BoolType();
        }
        return new ErrorType("Cannot negate " + this + ".");
    }

    // relational
    public Type compare (Type that) {
        if (this instanceof PtrType){
            return this.deref().compare(that);
        }
        if (that instanceof PtrType){
            that = that.deref();
        }
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
        if (this instanceof PtrType){
            return ((PtrType) this).getType();
        }
        return new ErrorType("Cannot dereference " + this);
    }

    public Type tryDeref() {
        if( this instanceof PtrType) {
            return ((PtrType) this).getType();
        }
        return this;
    }

    public Type index (Type that) {
        // TODO: implement array type checking (and maybe bounds?)
        return new ErrorType("Cannot index " + this + " with " + that + ".");
    }

    // statements
    public Type assign (Type source) {
        if (this instanceof PtrType){
            //
        }else{
            return new ErrorType("Cannot assign " + source + " to " + this + ".");
        }
        if (source instanceof PtrType){
            source = source.deref();
        }
        if(this.deref() instanceof IntType && source instanceof IntType){
            return new IntType();
        }else if (this.deref() instanceof FloatType && source instanceof FloatType){
            return new FloatType();
        }else if (this.deref() instanceof BoolType && source instanceof BoolType) {
            return new BoolType();
        }
        return new ErrorType("Cannot assign " + source + " to " + this + ".");
    }

    public boolean equals(Type type){
        if( this.getClass() != type.getClass()) {
            return false;
        }

        if( this instanceof FuncType ) {
            FuncType me = (FuncType) this,
                    other = (FuncType) type;

            return me.params.equals(other.params) && me.returnType.equals(other.returnType);
        }
        else if( this instanceof TypeList ) {
            TypeList me = (TypeList) this,
                    other = (TypeList) type;

            if( me.list.size() != other.list.size() ) {
                return false;
            }

            for( int i = 0; i < me.list.size(); i++ ) {
                Type metype = me.list.get(i);
                if( metype instanceof PtrType ) {
                    metype = ((PtrType) metype).getType();
                }
                Type othertype = other.list.get(i);
                if( othertype instanceof PtrType ) {
                    othertype = ((PtrType) othertype).getType();
                }
                if( !metype.equals(othertype)  ) {
                    return false;
                }

                return true;
            }
        }
        else if( this instanceof AryType ) {
            AryType me = (AryType) this,
                    other = (AryType) type;

            if( !me.type.equals(other.type) ) {
                return false;
            }

            if( me.dimensions.size() != other.dimensions.size() ) {
                return false;
            }

            for( int i = 0; i < me.dimensions.size(); i++ ) {
                if( (me.dimensions.get(i) != -1 && other.dimensions.get(i) != -1) && (!Objects.equals(me.dimensions.get(i), other.dimensions.get(i))) ) {
                    return false;
                }
            }
            return true;
        }

        return true;
    }
    public Type call (Type args) {
        // TODO: implement arg checking
        return new ErrorType("Cannot call " + this + " using " + args + ".");
    }

}
