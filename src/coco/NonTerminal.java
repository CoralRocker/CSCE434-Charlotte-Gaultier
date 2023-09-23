package coco;

import java.util.HashSet;
import java.util.Set;

public enum NonTerminal {


    // nonterminal FIRST sets for grammar

    // operators
    REL_OP(new HashSet<Token.Kind>() {
        private static final long serialVersionUID = 1L;
        {
            add(Token.Kind.EQUAL_TO);
            add(Token.Kind.NOT_EQUAL);
            add(Token.Kind.LESS_THAN);
            add(Token.Kind.LESS_EQUAL);
            add(Token.Kind.GREATER_EQUAL);
            add(Token.Kind.GREATER_THAN);
        }
    }),
    ASSIGN_OP(new HashSet<Token.Kind>() {
        private static final long serialVersionUID = 1L;
        {
            add( Token.Kind.ADD_ASSIGN );
            add( Token.Kind.SUB_ASSIGN );
            add( Token.Kind.MUL_ASSIGN );
            add( Token.Kind.DIV_ASSIGN );
            add( Token.Kind.MOD_ASSIGN );
            add( Token.Kind.POW_ASSIGN );
            add( Token.Kind.ASSIGN );//
        }
    }),
    UNARY_OP(new HashSet<Token.Kind>() {
        private static final long serialVersionUID = 1L;
        {
            add( Token.Kind.UNI_DEC );
            add( Token.Kind.UNI_INC );
            add( Token.Kind.NOT );
        }
    }),

    // literals (integer and float handled by Scanner)
    BOOL_LIT(new HashSet<Token.Kind>() {
        private static final long serialVersionUID = 1L;
        {
            add( Token.Kind.TRUE );
            add( Token.Kind.FALSE );
        }
    }),
    LITERAL(new HashSet<Token.Kind>() {
        private static final long serialVersionUID = 1L;
        {
            addAll( BOOL_LIT.firstSet );
            add( Token.Kind.INT_VAL );
            add( Token.Kind.FLOAT_VAL );
        }
    }),

    // designator (ident handled by Scanner)
    DESIGNATOR(new HashSet<Token.Kind>() {
        private static final long serialVersionUID = 1L;
        {
            add( Token.Kind.IDENT );
        }
    }),

    ADD_OP( new HashSet<>() {
        private static final long serialVersionUID = 1L;
        {
            add( Token.Kind.ADD );
            add( Token.Kind.SUB );
            add( Token.Kind.OR );
        }
    }),

    MUL_OP( new HashSet<>() {
        private static final long serialVersionUID = 1L;
        {
            add( Token.Kind.MUL );
            add( Token.Kind.DIV );
            add( Token.Kind.MOD );
            add( Token.Kind.AND );
        }
    }),

    // TODO: expression-related nonterminals
    TYPE( new HashSet<Token.Kind>() {
       private static final long serialVersionUID = 1L;
        {
            add( Token.Kind.INT );
            add( Token.Kind.FLOAT );
            add( Token.Kind.BOOL );
        }
    }),

    // statements
    ASSIGN(new HashSet<Token.Kind>() {
        private static final long serialVersionUID = 1L;
        {
            addAll( DESIGNATOR.firstSet );
        }
    }),
    FUNC_CALL(new HashSet<Token.Kind>() {
        private static final long serialVersionUID = 1L;
        {
            add( Token.Kind.CALL );
        }
    }),
    IF_STAT(new HashSet<Token.Kind>() {
        private static final long serialVersionUID = 1L;
        {
            add( Token.Kind.IF );
        }
    }),
    WHILE_STAT(new HashSet<Token.Kind>() {
        private static final long serialVersionUID = 1L;
        {
            add( Token.Kind.WHILE );
        }
    }),
    REPEAT_STAT(new HashSet<Token.Kind>() {
        private static final long serialVersionUID = 1L;
        {
            add( Token.Kind.REPEAT );
        }
    }),
    RETURN_STAT(new HashSet<Token.Kind>() {
        private static final long serialVersionUID = 1L;
        {
            add( Token.Kind.RETURN );
        }
    }),
    STATEMENT(new HashSet<Token.Kind>() {
        private static final long serialVersionUID = 1L;
        {
            addAll( ASSIGN.firstSet );
            addAll( IF_STAT.firstSet );
            addAll( REPEAT_STAT.firstSet );
            addAll( RETURN_STAT.firstSet );
            addAll( FUNC_CALL.firstSet );
            addAll( WHILE_STAT.firstSet );
        }
    }),
    STAT_SEQ(new HashSet<Token.Kind>() {
        private static final long serialVersionUID = 1L;
        {
            addAll( STATEMENT.firstSet );
        }
    }),

    // declarations
    TYPE_DECL(new HashSet<Token.Kind>() {
        private static final long serialVersionUID = 1L;
        {
            addAll( TYPE.firstSet );
        }
    }),
    VAR_DECL(new HashSet<Token.Kind>() {
        private static final long serialVersionUID = 1L;
        {
            addAll( TYPE_DECL.firstSet );
        }
    }),
    PARAM_DECL(new HashSet<Token.Kind>() {
        private static final long serialVersionUID = 1L;
        {
            addAll( TYPE.firstSet );
        }
    }),

    // functions
    FORMAL_PARAM(new HashSet<Token.Kind>() {
        private static final long serialVersionUID = 1L;
        {
            add( Token.Kind.OPEN_PAREN );
        }
    }),
    FUNC_BODY(new HashSet<Token.Kind>() {
        private static final long serialVersionUID = 1L;
        {
            add( Token.Kind.OPEN_BRACE );
        }
    }),
    FUNC_DECL(new HashSet<Token.Kind>() {
        private static final long serialVersionUID = 1L;
        {
            add( Token.Kind.FUNC );
        }
    }),

    // computation
    COMPUTATION(new HashSet<Token.Kind>() {
        private static final long serialVersionUID = 1L;
        {
            add( Token.Kind.MAIN );
        }
    }),

    GROUP_EXPR(new HashSet<Token.Kind>() {
        private static final long serialVersionUID = 1L;
        {
            addAll( LITERAL.firstSet );
            add( Token.Kind.IDENT );
            add( Token.Kind.NOT );
            add( Token.Kind.OPEN_PAREN );
            add( Token.Kind.CALL );
        }
    })
    ;


    private static HashSet< Token.Kind > FIRST( Set< Token.Kind >[] sets ) {

        return new HashSet<>();
    }

    private final Set<Token.Kind> firstSet = new HashSet<>();

    private NonTerminal (Set<Token.Kind> set) {
        firstSet.addAll(set);
    }

    public final Set<Token.Kind> firstSet () {
        return firstSet;
    }
}
