package types;

public class FuncType extends Type {

    protected TypeList params;
    protected Type returnType;

    public FuncType( Type ret, TypeList args ) {
        params = args;
        returnType = ret;
    }

    public FuncType() {
        params = null;
        returnType = null;
    }
}