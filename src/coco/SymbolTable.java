package coco;


import java.util.HashMap;

public class SymbolTable {

    // TODO: Create Symbol Table structure
    private HashMap< String, Symbol > table;

    public SymbolTable () {
        table = new HashMap<>();
    }

    // lookup name in SymbolTable
    public Symbol lookup (String name) throws SymbolNotFoundError {
        Symbol sym = table.get(name);
        if( sym == null ) {
            throw new SymbolNotFoundError(name);
        }

        return sym;
    }

    // insert name in SymbolTable
    public Symbol insert (String name, Symbol sym) throws RedeclarationError {
        if( table.containsKey(name) ) {
            throw new RedeclarationError(name);
        }
        table.put(name, sym);
        return sym;
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
