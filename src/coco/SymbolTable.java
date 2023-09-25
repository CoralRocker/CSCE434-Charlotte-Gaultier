package coco;


import java.util.ArrayList;
import java.util.HashMap;

public class SymbolTable {

    // TODO: Create Symbol Table structure

    private final SymbolTable parent;
    private HashMap<String, Symbol> map;

    public SymbolTable (SymbolTable parent) {
        this.parent = parent;
        this.map = new HashMap<String, Symbol>();
    }

    public SymbolTable pushScope(){
        // add a new scope, enter it, with parent as this (current scope)
        return new SymbolTable(this);
    }

    public SymbolTable popScope(){
        // enter parent scope
        return parent;
    }

    // lookup name in SymbolTable
    public Symbol lookup (String name) throws SymbolNotFoundError {
        // look in current scope and then look in parents, call lookup on this.parent
        // recursive, base case returns null or symbol
        // if found in self: return

        if(map.get(name) != null){
            return map.get(name);
        }else{
            if(parent == null) {throw new SymbolNotFoundError(name);}
            return parent.lookup(name);
        }
        // else: return lookup parent
    }

    // insert name in SymbolTable
    public Symbol insert (Token ident, Symbol value) throws RedeclarationError {
        if(map.get(ident.lexeme()) == null){
            map.put(ident.lexeme(), value);
        }else{
            throw new RedeclarationError(ident.lexeme());
        }
        return map.get(ident.lexeme());
    }

    public Symbol insert (String str, Symbol value) throws RedeclarationError {
        if(map.get(str) == null){
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

    public SymbolNotFoundError (String name) {
        super("Symbol " + name + " not found.");
        this.name = name;
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
