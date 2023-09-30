package types;

public class ErrorType extends Type {

    String message;
    public ErrorType(String msg){
        message = msg;
    }

    @Override
    public String toString(){
        return "ErrorType(" + message + ")";
    }

}