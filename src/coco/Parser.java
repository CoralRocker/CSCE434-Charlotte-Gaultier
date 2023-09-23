package coco;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

public class Parser {

    private HashMap< String, ArrayList<ArrayList<Token>>> functionTable;
    private HashMap< String, Token > symbolTable;
    private ArrayList<Token> input;

    public Parser( ArrayList<Token> tokens ) {
        input = (ArrayList<Token>) tokens.clone();
        functionTable = new HashMap<>();
        symbolTable = new HashMap<>();
    }

    public void parse() {


    }



}
