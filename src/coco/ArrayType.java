package coco;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @brief Helper class to manage an array of some datatype. Also handles non-array identifiers
 *
 * Used to generate unique identifiers for each position in the array, given a basename
 */
public class ArrayType {
    private Token type;

    private boolean function = false;

    private ArrayList< ArrayType > arglist = null;

    private ArrayList< Integer > dims;



    public ArrayType( Token type, ArrayList< Integer > d) {
        dims = d;
        this.type = type;

        if( dims == null ) {
            return;
        }

        for( Integer dim : dims ) {
            if( dim <= 0 ) {
                throw new RuntimeException("All dimensions must be >= 0!");
            }
        }
    }

    public ArrayType( Token.Kind type ) {
        dims = null;
        this.type = new Token(type, 0, 0);
    }

    public  static ArrayType makeFunctionType( Token.Kind ret ) {
        return makeFunctionType(new ArrayType(ret), null);
    }

    public static ArrayType makeFunctionType( Token.Kind ret, Token.Kind[] args) {
        ArrayList< ArrayType> types = new ArrayList<>();
        for( Token.Kind kind : args ) {
            types.add( new ArrayType(kind) );
        }
        return makeFunctionType(
                new ArrayType(ret),
                types
        );
    }
    public static ArrayType makeFunctionType( ArrayType ret, ArrayList<ArrayType> args ) {
        ArrayType func = new ArrayType(ret.type, ret.dims);
        func.function = true;
        if( args != null ) {
            func.arglist = (ArrayList<ArrayType>) args.clone();
        }
        else {
            func.arglist = new ArrayList<>();
        }

        return func;
    }

    /**
     * @brief Get the token type. Upon creation, this is the datatype. After setting the identifier, this is a IDENT
     * @return
     */
    public Token.Kind getType() {
        return type.kind();
    }

    public boolean isArray() {
        return dims != null;
    }

    /**
     * @brief Return the "base" name. Either the ident for non-array types, or
     * the first array location for array types.
     */
    public String baseIdent( Token ident ) {
        if( dims == null ) {
            return ident.lexeme();
        }
        else {
            ArrayList<Integer> idx = new ArrayList<>(Collections.nCopies(dims.size(), 0));
            return at( ident, idx );
        }
    }

    public static String genIdent( String base, ArrayList< Integer > idx ) {
        if( idx == null ) {
            return base;
        }

        StringBuilder varname = new StringBuilder(base);

        for( Integer i : idx ) {
            if( i < 0 ) {
                throw new RuntimeException("Cannot index at %d! Index is < 0!".formatted(i));
            }

            varname.append("_{%d}".formatted(i));
        }

        return varname.toString();
    }

    /**
     * @brief Get the unique identifier for a position in the array
     * @param idx An arraylist of the indeces to access
     * @return The String identifier for the position
     */
    public String at( Token ident, ArrayList< Integer > idx ) {
        if( dims == null && idx != null ) {
            throw new RuntimeException("Cannot array-index non-array type!");
        }
        else if( dims == null && idx == null ) {
            return ident.lexeme();
        }

        if( idx.size() != dims.size() ) {
            throw new RuntimeException("All index dimensions must be specified!: %d %d".formatted( ident.lineNumber(), ident.charPosition()));
        }

        StringBuilder varname = new StringBuilder( ident.lexeme() );

        for( int i = 0; i < dims.size(); i++ ) {
            if( idx.get(i) < 0 || idx.get(i) >= dims.get(0) ) {
                throw new RuntimeException("Index [%d] is greater than dimension [%d]! (%d dimension)".formatted(idx.get(i), dims.get(i), i ) );
            }

            // IMPORTANT: Use illegal characters in the identifier to prevent clashes with legal CoCo names.
            varname.append("_{%d}".formatted(idx.get(i)) );
        }

        // System.out.printf("For index %s: %s\n", idx.toString(), varname.toString());

        return varname.toString();
    }

    /**
     * @brief Generate a list of the identifiers for every position in the array
     */
    public ArrayList< String > allIdents( Token ident ) {
        if( dims == null ) {
            return new ArrayList<>(Arrays.asList(ident.lexeme()));
        }

        ArrayList<String> results = new ArrayList<>();
        ArrayList<ArrayList<Integer>> indexes = new ArrayList<>();

        boolean firstRun = true;


        // Algorithm for generating every combination of array indeces:
        // For first dimension, add all index positions to individual arraylists.
        // For each subsequent dimension, given a dimension [0 - n), for each extant ArrayList of indeces A,
        //     Remove A from the array of indeces, and add n new copies of A to the array, with each having one of the
        //     values from the range [0 - n) added to it.
        // Repeat until all dimensions are traversed.
        for( Integer dim : dims ) {
            ArrayList< Integer > idx = new ArrayList<>();
            NumberSequence seq = new NumberSequence(dim);
            for ( Integer i : seq ) {
                idx.add(i);
                if( firstRun ) {
                    ArrayList<Integer> arr = new ArrayList<>();
                    arr.add( i );
                    indexes.add( arr );
                }
            }

            if( ! firstRun ) {
                ArrayList< ArrayList< Integer > > prevIdx = indexes;
                indexes = new ArrayList<>();

                for( ArrayList<Integer> arr : prevIdx ) {
                    for( Integer i : idx ) {
                        ArrayList< Integer > newArr = (ArrayList<Integer>) arr.clone();
                        newArr.add( i );
                        indexes.add( newArr );
                    }
                }
            }

            firstRun = false;
        }

        // System.out.printf("For Dims: %s\nAll Possible Indexes: %s\n", dims, indexes );
        for( ArrayList< Integer > index : indexes ) {
            results.add( at(ident, index) );
        }

        return results;
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append(type.lexeme());

        if (dims != null) {
            for (int dim : dims) {
                ret.append("[");
                ret.append(dim);
                ret.append("]");
            }
        }

        if( function ) {
            StringBuilder builder = new StringBuilder();
            builder.append("TypeList(");
            for( int i = 0; i < arglist.size(); i++ ) {
                builder.append(arglist.get(i));
                if( (i + 1) < arglist.size() ) {
                    builder.append(",");
                }
            }
            builder.append(")->");
            builder.append(ret.toString());

            return builder.toString();
        }
        else {
            return ret.toString();
        }
    }
}
