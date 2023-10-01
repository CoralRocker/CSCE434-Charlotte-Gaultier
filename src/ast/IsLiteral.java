package ast;

public interface IsLiteral {
    default int getIntLiteral() {
        throw new RuntimeException("Not implemented");
    }
    default float getFloatLiteral() {
        throw new RuntimeException("Not implemented");
    }

    default boolean getBoolLiteral() {
        throw new RuntimeException("Not implemented");
    }

    default double getLiteral() {
        throw new RuntimeException("Not implemented");
    }
}
