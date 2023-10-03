package types;

import coco.ArrayType;

import java.util.ArrayList;
import java.util.List;

public class AryType extends Type {

    protected List<Integer> dimensions;
    protected Type type;
    protected int isParamType = 0;

    public AryType( Type type, int ndim ) {
        this.type = type;
        this.isParamType = ndim;
    }

    public AryType( Type type, ArrayList<Integer> dims) {
        this.type = type;
        for( Integer i : dims ) {
            if( i <= 0 ) {
                throw new IllegalArgumentException(String.format("Dimension cannot be <= 0!: (%d)", i));
            }
        }
        this.dimensions = dims;
    }

    public boolean isArgType() {
        return isParamType == 0;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(type.toString());
        if( isParamType > 0 ) {
            for( int i = 0; i < isParamType; i++ ) {
                builder.append("[]");
            }
        }
        else {
            for (Integer dim : dimensions) {
                builder.append(String.format("[%d]", dim));
            }
        }

        return builder.toString();
    }

    public Type popDimension() {
        if( isParamType > 0 ) {
            if( isParamType == 1 ) {
                return type;
            }
            return new AryType(type, isParamType-1);
        }

        if( this.dimensions.size() == 1 ) {
            return type;
        }

        ArrayList<Integer> ndims = new ArrayList<>(dimensions);


        ndims.remove(ndims.size()-1);
        return new AryType(type, ndims);
    }

    public int nDimensions() {
        if( isParamType > 0 ) {
            return isParamType;
        }
        else {
            return dimensions.size();
        }
    }

    public boolean compareDimension( AryType other, int idx ) {
        if( isParamType > 0 ) {
            if( idx < isParamType && idx >= 0 ) {
                return true;
            }
            return false;
        }

        if( other.isParamType > 0 ) {
           if( idx < other.isParamType && idx >= 0 ) {
               return true;
           }
           return false;
        }

        return idx >= 0 && idx < nDimensions() && dimensions.get(idx) == other.dimensions.get(idx);
    }
}