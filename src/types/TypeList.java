package types;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TypeList extends Type implements Iterable<Type> {

    protected List<Type> list;

    public TypeList () {
        list = new ArrayList<>();
    }

    public TypeList (List<Type> list) {
        this.list = list;
    }

    public void append (Type type) {
        list.add(type);
    }

    public List<Type> getList () {
        return list;
    }

    @Override
    public Iterator<Type> iterator () {
        return list.iterator();
    }

    @Override
    public String toString() {
        //StringBuilder builder = new StringBuilder("TypeList(");
        StringBuilder builder = new StringBuilder("(");
        for( int i = 0; i < list.size(); i++ ) {
            Type type = list.get(i);
            if( type instanceof PtrType ) {
                builder.append(((PtrType) type).getType());
            }
            else {
                builder.append(type);
            }
            if( (i+1) < list.size() ) {
                builder.append(",");
            }
        }
        builder.append(")");
        return builder.toString();
    }

    //TODO more helper here

}