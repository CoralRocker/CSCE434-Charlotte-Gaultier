package coco;

import java.util.ArrayList;

public class FunctionSymbol extends Symbol {

    public FunctionSymbol(String name) {
        super(name);
        this.types = new ArrayList<>();
    }

    public FunctionSymbol(String name, ArrayList<ArrayType> types) {
        super(name);
        this.types = types;
    }

    public FunctionSymbol(String name, ArrayType type ) {
        super(name);
        this.types = new ArrayList<>();
        add(type);
    }

    public void add(ArrayType type) {
        this.types.add(type);
    }

    private ArrayList<ArrayType> types;

    @Override
    public Object value() {
        return types;
    }

    @Override
    public ArrayType type() {
        return null;
    }

    @Override
    public boolean hasType() {
        return types != null && !types.isEmpty();
    }

    public String typeSignatures() {
        StringBuilder builder = new StringBuilder();
        if( types == null ) {
            builder.append("NO TYPES");
            return builder.toString();
        }
        for( int i = 0; i < types.size(); i++ ) {
            builder.append(types.get(i));
            if( (i+1) != types.size() ) {
                builder.append(',');
            }
        }

        return builder.toString();

    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("Symbol(%s:", super.name));
        builder.append( typeSignatures() );
        builder.append(")");

        return builder.toString();
    }
}
