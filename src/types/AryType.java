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
    }

    public boolean isArgType() {
        for( Integer dim : dimensions ) {
            if( dim != -1 ) {
                return false;
            }
        }

        return false;
    }

}