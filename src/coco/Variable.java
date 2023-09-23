package coco;

import java.awt.desktop.QuitEvent;

public class Variable {
    private Object value = null;
    public enum Type {
        INT,
        FLOAT,
        BOOL,
        VOID,
    };

    private final Type type;

    public Variable( Token lit ) {
        switch( lit.kind() ) {
            case FLOAT_VAL -> {
                this.type = Type.FLOAT;
                this.value = Float.parseFloat( lit.lexeme() );
            }
            case INT_VAL -> {
                this.type = Type.INT;
                this.value = Integer.parseInt( lit.lexeme() );
            }
            case TRUE -> {
                this.type = Type.BOOL;
                this.value = Boolean.valueOf(true);
            }
            case FALSE -> {
                this.type = Type.BOOL;
                this.value = Boolean.valueOf(false);
            }
            default -> throw new IllegalStateException("Unexpected value: " + lit.kind());
        }
    }

    public Variable( Token.Kind type ) {
        switch( type ) {
            case FLOAT -> {
                this.value = Float.valueOf(0.f);
                this.type = Type.FLOAT;
            }
            case INT -> {
                this.value = Integer.valueOf(0);
                this.type = Type.INT;
            }
            case BOOL -> {
                this.value = Boolean.valueOf(false);
                this.type = Type.BOOL;
            }
            case VOID -> {
                this.value = null;
                this.type = Type.VOID;
            }
            default -> throw new IllegalStateException("Variable: Unexpected value: " + type);
        }
    }

    public Variable( Integer i ) {
        type = Type.INT;
        value = i;
    }

    public Variable( int i ) {
        type = Type.INT;
        value = Integer.valueOf(i);
    }
    public Variable( Float f ) {
        type = Type.FLOAT;
        value = f;
    }

    public Variable( float f ) {
        type = Type.FLOAT;
        value = Float.valueOf(f);
    }

    public Variable( Boolean b ) {
        type = Type.BOOL;
        value = b;
    }

    public Variable( boolean b ) {
        type = Type.BOOL;
        value = Boolean.valueOf(b);
    }

    public Variable() {
        this.type = Type.VOID;
        this.value = null;
    }

    public Variable( Variable var ) {
        this.type = var.type;
        switch( type ) {
            case INT -> {
                this.value = Integer.valueOf( var.getInt().intValue() );
            }
            case FLOAT -> {
                this.value = Float.valueOf( var.getFloat().floatValue() );
            }
            case BOOL -> {
                this.value = Boolean.valueOf( var.getBool().booleanValue() );
            }
        }
    }

    /**
     * INT * FLOAT is illegal ( cannot store float in int )
     * SAME * SAME is legal always
     * FLOAT * INT is legal ( float can store int )
     */
    private boolean interoperable( Variable var ) {
        if( var.type == type && type != Type.VOID ) {
            return true;
        }

        switch( type ) {
            case INT, BOOL, VOID -> {
                return false;
            }
            case FLOAT -> {
                return var.type == Type.INT;
            }
        }

        return false;
    }


    public Variable mult( Variable var ) {
        if( ! interoperable(var) || type == Type.BOOL ) {
            throw new IllegalArgumentException("Cannot multiply %s by %s!".formatted(this.type, var.type));
        }

        switch( type ) {
            case INT -> {
                Integer i = (Integer) value;
                return new Variable( i * var.getInt() );
            }
            case FLOAT -> {
                Float f = (Float) value;
                if( var.type == Type.FLOAT ) {
                    return new Variable( f * var.getFloat() );
                }
                else {
                    return new Variable( f * var.getInt() );
                }
            }
        }

        return null;
    }
    public Variable div( Variable var ) {
        if( ! interoperable(var) || type == Type.BOOL ) {
            throw new IllegalArgumentException("Cannot divide %s by %s!".formatted(this.type, var.type));
        }

        switch( type ) {
            case INT -> {
                Integer i = (Integer) value;
                return new Variable( i / var.getInt() );
            }
            case FLOAT -> {
                Float f = (Float) value;
                if( var.type == Type.FLOAT ) {
                    return new Variable( f / var.getFloat() );
                }
                else {
                    return new Variable( f / var.getInt() );
                }
            }
        }

        return null;
    }
    public Variable add( Variable var ) {
        if( ! interoperable(var) || type == Type.BOOL ) {
            throw new IllegalArgumentException("Cannot add %s with %s!".formatted(this.type, var.type));
        }

        switch( type ) {
            case INT -> {
                Integer i = (Integer) value;
                return new Variable( i + var.getInt() );
            }
            case FLOAT -> {
                Float f = (Float) value;
                if( var.type == Type.FLOAT ) {
                    return new Variable( f + var.getFloat() );
                }
                else {
                    return new Variable( f + var.getInt() );
                }
            }
        }

        return null;
    }
    public Variable sub( Variable var ) {
        if( ! interoperable(var) || type == Type.BOOL ) {
            throw new IllegalArgumentException("Cannot subtract %s from %s!".formatted(var.type, this.type));
        }

        switch( type ) {
            case INT -> {
                Integer i = (Integer) value;
                return new Variable( i - var.getInt() );
            }
            case FLOAT -> {
                Float f = (Float) value;
                if( var.type == Type.FLOAT ) {
                    return new Variable( f - var.getFloat() );
                }
                else {
                    return new Variable( f - var.getInt() );
                }
            }
        }

        return null;
    }

    public Variable pow( Variable var ) {
        if( ! interoperable( var ) || type == Type.BOOL ) {
            throw new IllegalArgumentException("Cannot subtract %s from %s!".formatted(var.type, this.type) );
        }

        switch( type ) {
            case INT -> {
                Integer i = (Integer) value;
                Integer p = var.getInt();
                double value = Math.pow( i.floatValue(), p.floatValue() );
                return new Variable( (int)value );
            }
            case FLOAT -> {
                Float f = (Float) value;
                if( var.type == Type.INT ) {
                    return new Variable( (float) Math.pow( f, var.getInt()) );
                }
                else {
                    return new Variable( (float) Math.pow( f, var.getFloat()) );
                }
            }
        }

        return null;
    }

    public Variable mod( Variable var ) {
        if( ! interoperable(var) || type == Type.BOOL ) {
            throw new IllegalArgumentException("Cannot mod %s by %s!".formatted(type, var.type));
        }

        switch( type ) {
            case INT -> {
                Integer i = (Integer) value;
                switch( var.type ) {
                    case INT -> {
                        return new Variable( i % var.getInt() );
                    }
                    case FLOAT -> {
                        return new Variable( i % var.getFloat() );
                    }
                }
            }
            case FLOAT -> {
                Float i = (Float) value;
                switch( var.type ) {
                    case INT -> {
                        return new Variable( i % var.getInt() );
                    }
                    case FLOAT -> {
                        return new Variable( i % var.getFloat() );
                    }
                }
            }
        }

        Integer i = (Integer) value;
        return new Variable( Math.floorMod( i, var.getInt() ) );
    }

    public Variable and( Variable var ) {
        Boolean b = (Boolean) value;
        return new Variable(b && var.coerceBool().getBool() );
    }

    public Variable or( Variable var ) {
        Boolean b = coerceBool().getBool();
        return new Variable(b || var.coerceBool().getBool() );
    }

    public Variable greaterThan( Variable var, boolean orEq ) {
        if( type == Type.BOOL || var.type == Type.BOOL || type == Type.VOID || var.type == Type.VOID ) {
            throw new RuntimeException("Cannot get > for boolean!");
        }

        double lhs, rhs;

        if( type == Type.INT ) {
            lhs = getInt().doubleValue();
        }
        else {
            lhs = getFloat().doubleValue();
        }

        if( var.type == Type.INT ) {
            rhs = var.getInt().doubleValue();
        }
        else {
            rhs = var.getFloat().doubleValue();
        }

        if( orEq ) {
            return new Variable(lhs >= rhs );
        }
        else {
            return new Variable(lhs > rhs );
        }
    }

    public Variable lessThan( Variable var, boolean orEq ) {
        if( type == Type.BOOL || var.type == Type.BOOL || type == Type.VOID || var.type == Type.VOID ) {
            throw new RuntimeException("Cannot get < for boolean!");
        }

        double lhs, rhs;

        if( type == Type.INT ) {
            lhs = getInt().doubleValue();
        }
        else {
            lhs = getFloat().doubleValue();
        }

        if( var.type == Type.INT ) {
            rhs = var.getInt().doubleValue();
        }
        else {
            rhs = var.getFloat().doubleValue();
        }

        if( orEq ) {
            return new Variable(lhs <= rhs );
        }
        else {
            return new Variable(lhs < rhs );
        }
    }

    public void set( Variable var ) {
        if( var.type == this.type ) {
            value = var.value;
            return;
        }
        else if( var.type == Type.INT && this.type == Type.FLOAT ) {
            value = Float.valueOf( var.getInt() );
        }
        else {
            throw new RuntimeException("Cannot set a variable of type %s to type %s!".formatted(type, var.type));
        }
    }

    public void set( Integer i ) {
        if( type != Type.INT && type != Type.FLOAT ) {
            throw new RuntimeException("Cannot set int with non-int-or-float type!");
        }

        if( type == Type.FLOAT ) {
            value = Float.valueOf(i);
        }
        else {
            this.value = i;
        }
    }

    public void set( Float f ) {
        if( type != Type.FLOAT ) {
            throw new RuntimeException("Cannot set float with non-float type!");
        }

        this.value = f;
    }

    public void set( Boolean b ) {
        if( type != Type.BOOL ) {
            throw new RuntimeException("Cannot set bool with non-bool type!");
        }

        this.value = b;
    }

    public Variable equals( Variable var ) {
        if( type == Type.BOOL && var.type != Type.BOOL ) {
            throw new RuntimeException("Cannot compare bool to non-bool type!");
        }

        if( type == Type.VOID || var.type == Type.VOID ) {
            throw new RuntimeException("Cannot act on void objects!");
        }

        switch( type ) {
            case BOOL -> {
                Boolean other = var.getBool();
                Boolean val = getBool();

                return new Variable( other.booleanValue() == val.booleanValue() );
            }
            case INT -> {
                Integer val = getInt();
                switch( var.type ) {
                    case INT -> {
                        return new Variable( var.getInt().intValue() == val.intValue() );
                    }
                    case FLOAT -> {
                        return new Variable( var.getFloat().floatValue() == val.intValue() );
                    }
                }
            }
            case FLOAT -> {
                Float val = getFloat();
                switch( var.type ) {
                    case INT -> {
                        return new Variable( var.getInt().intValue() == val.floatValue() );
                    }
                    case FLOAT -> {
                        return new Variable( var.getFloat().floatValue() == val.floatValue() );
                    }

                }
            }
        }

        // Unreachable
        return new Variable( false );
    }

    public Variable notEquals( Variable var ) {
        Variable res = equals(var);
        return new Variable( !res.getBool().booleanValue() );
    }


    public Integer getInt() {
        if( type != Type.INT ) {
            return null;
        }

        return (Integer) value;
    }

    public Float getFloat() {
        if( type != Type.FLOAT ) {
            return null;
        }

        return (Float) value;
    }

    public Boolean getBool() {
        if( type != Type.BOOL ) {
            return null;
        }

        return (Boolean) value;
    }

    public Variable coerceBool() {
        switch( type ) {
            case INT -> {
                return new Variable( getInt().intValue() != 0 );
            }
            case FLOAT -> {
                return new Variable( getFloat().floatValue() != 0 );
            }
            case BOOL -> {
                return new Variable(this);
            }
            case VOID -> {
                throw new RuntimeException("Cannot coerce VOID to BOOL");
            }
            default -> {
                throw new RuntimeException("Unknown type: " + type);
            }
        }
    }

    public boolean isInt() {
        return type == Type.INT;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return type.toString() + " " + value;
    }
};