package coco;

import java.util.ArrayList;

public class FunctionSymbol extends Symbol {

    protected Token declTok;

    public Token getDeclarationToken() { return declTok; }

    public FunctionSymbol(Token name) {
        super(name.lexeme());
        this.types = new ArrayList<>();
        this.declTok = name;
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

    public boolean contains( ArrayType type ) {
        for( ArrayType ftype : types ) {
            if(ftype.typeSignature().equals(type.typeSignature())) {
                return true;
            }
        }
        return false;
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
            builder.append(String.format("%s:%s", super.name, types.get(i)));
            if( (i+1) != types.size() ) {
                builder.append(", ");
            }
        }

        return builder.toString();

    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("Symbol("));
        String typesig = typeSignatures();
        if( typesig.isEmpty() ) {
            builder.append(super.name);
        }
        else {
            builder.append(typesig);
        }
        builder.append(")");

        return builder.toString();
    }
}
