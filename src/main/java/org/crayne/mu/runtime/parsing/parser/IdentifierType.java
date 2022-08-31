package org.crayne.mu.runtime.parsing.parser;

import org.jetbrains.annotations.NotNull;

public enum IdentifierType {

    FUNCTION("Function"), VARIABLE("Variable"), ENUM("Enum");

    private final String toString;

    IdentifierType(@NotNull final String toString) {
        this.toString = toString;
    }

    public String toString() {
        return toString;
    }
}
