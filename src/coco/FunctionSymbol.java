package coco;

import types.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FunctionSymbol extends Symbol {

    protected Token declTok;
    protected Token.Kind returnType;

    private Object value;


    protected Type realReturnType;
    private ArrayList<ArrayType> types;

    public List<TypeList> getTypeLists() {
        ArrayList<TypeList> lists = new ArrayList<>();

        for(ArrayType type : types ) {
            lists.add( type.typeList() );
        }

        return lists;
    }

    public Token getDeclarationToken() { return declTok; }
    public Type getReturnType() { switch(returnType){
        case INT:
            return new IntType();
        case FLOAT:
            return new FloatType();
        case BOOL:
            return new BoolType();
        case VOID:
            return new VoidType();
        default:
            return new ErrorType("Could not resolve function return type"); }}

    public Type getRealReturnType() {
        if(realReturnType != null){
            return realReturnType;
        }
        switch(returnType){
            case INT:
                return new IntType();
            case FLOAT:
                return new FloatType();
            case BOOL:
                return new BoolType();
            case VOID:
                return new VoidType();
            default:
                return new ErrorType("Could not resolve function return type");
        }
    }
    public void setRealReturnType(Type type){
        this.realReturnType = type;
    }

    public FunctionSymbol(Token name) {
        super(name.lexeme());
        this.types = new ArrayList<>();
        this.declTok = name;
        this.value = types;
    }
    public FunctionSymbol(Token name, ArrayType retType) {
        super(name.lexeme());
        this.types = new ArrayList<>();
        this.declTok = name;
        this.returnType = retType.getType();
        this.value = types;
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
    public FunctionSymbol(String name, ArrayType type , ArrayType retType) {
        super(name);
        this.types = new ArrayList<>();
        add(type);
        this.returnType = retType.getType();

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

    public void setNullValue(){
        value = 0;
    }

    @Override
    public Object value() {
        return value;
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
