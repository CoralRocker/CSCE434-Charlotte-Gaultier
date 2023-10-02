package types;

import coco.ArrayType;

import java.util.ArrayList;
import java.util.List;

public class AryType extends Type {

    protected List<Integer> dimensions;
    protected Type type;

    public AryType( Type type, int ndim ) {
        this.type = type;
        dimensions = new ArrayList<>();
        for( int i = 0; i < ndim; i++ ) {
            dimensions.add(-1);
        }
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
        for( Integer dim : dimensions ) {
            if( dim != -1 ) {
                return false;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(type.toString());
        for( Integer dim : dimensions ) {
            if( dim == -1 ) {
                builder.append("[]");
            }
            else {
                builder.append(String.format("[%d]", dim));
            }
        }

        return builder.toString();
    }

    public Type popDimension() {
        if( this.dimensions.size() == 1 ) {
            return type;
        }

        ArrayList<Integer> ndims = new ArrayList<>(dimensions);
        ndims.remove(ndims.size()-1);
        return new AryType(type, ndims);
    }

}