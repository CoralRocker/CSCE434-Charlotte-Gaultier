package coco;


import java.util.ArrayList;
import java.util.HashMap;

public class SymbolTable {

    private final SymbolTable parent;
    private ArrayList<SymbolTable> children;
    private HashMap<String, Symbol> map;

    public SymbolTable globalScope(int index) {
        SymbolTable global = this;
        ArrayList<SymbolTable> scope = new ArrayList<>();
        while( global.parent != null ) {
            scope.add(global);
            global = global.parent;
        }
        scope.add(global);

        return scope.get(scope.size() - 1 - index );
    }
    public SymbolTable (SymbolTable parent) {
        this.parent = parent;
        this.map = new HashMap<String, Symbol>();
        this.children = new ArrayList<>();
    }

    public SymbolTable pushScope(){
        // add a new scope, enter it, with parent as this (current scope)
        SymbolTable child = new SymbolTable(this);
        children.add(child);
        return child;
    }

    protected void removeChild(SymbolTable child) {
        for( SymbolTable table : children) {
            if( table == child ) {
                children.remove(table);
                break;
            }
        }
    }

    public SymbolTable popScope(){
        if( parent != null ) {
            parent.removeChild(this);
        }
        return parent;
    }

    public boolean contains(Token name) {
        return contains(name.lexeme());
    }
    public boolean contains(String name) {
        boolean thismap =  map.containsKey(name);

        if( thismap ) {
            return true;
        }
        else if ( parent != null ) {
            return parent.contains(name);
        }

        return false;
    }

    // lookup name in SymbolTable
    public Symbol lookup (Token name) throws SymbolNotFoundError {
        // look in current scope and then look in parents, call lookup on this.parent
        // recursive, base case returns null or symbol
        // if found in self: return

        if(map.containsKey(name.lexeme())){
            return map.get(name.lexeme());
        }else{
            if(parent == null) {throw new SymbolNotFoundError(name.lexeme(), name.lineNumber(), name.charPosition() );}
            return parent.lookup(name);
        }
        // else: return lookup parent
    }
    public Symbol assign (Token ident, Symbol value) throws SymbolNotFoundError {
        if(map.containsKey(ident.lexeme())){
            map.put(ident.lexeme(), value);
            return map.get(ident.lexeme());
        }else{
            if(parent == null) {throw new SymbolNotFoundError(ident.lexeme(), ident.lineNumber(), ident.charPosition() );}
            return parent.assign(ident, value);
        }
    }

    // insert name in SymbolTable
    public Symbol insert (Token ident, Symbol value) throws RedeclarationError {
        if(!map.containsKey(ident.lexeme())){
            map.put(ident.lexeme(), value);
        }else{
            throw new RedeclarationError(ident.lexeme());
        }
        return map.get(ident.lexeme());
    }

    public Symbol insert (String str, Symbol value) throws RedeclarationError {
        if(!map.containsKey(str)){
            map.put(str, value);
        }else{
            throw new RedeclarationError(str);
        }
        return map.get(str);
    }

    public String toString() {
        return map.toString();
    }
}

class SymbolNotFoundError extends Error {

    private static final long serialVersionUID = 1L;
    private final String name;
    private final int line, cpos;

    public SymbolNotFoundError (String name, int line, int cpos) {
        super(String.format("ResolveSymbolError(%d,%d)[Could not find %s.]", line, cpos, name));
        this.name = name;
        this.line = line;
        this.cpos = cpos;
    }

    public String name () {
        return name;
    }
}

class RedeclarationError extends Error {

    private static final long serialVersionUID = 1L;
    private final String name;

    public RedeclarationError (String name) {
        super("Symbol " + name + " being redeclared.");
        this.name = name;
    }

    public String name () {
        return name;
    }
}
