package types;

public class PtrType extends Type {
    private Type derefType;

    public PtrType(Type deref){
        derefType = deref;
    }

    public Type getType(){
        return derefType;
    }
    @Override
    public String toString(){
        return "AddressOf("+ derefType + ")";
    }

}