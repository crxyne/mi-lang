package org.crayne.mi.parsing.parser;

import org.jetbrains.annotations.NotNull;

public enum IdentifierType {

    FUNCTION("Function"), VARIABLE("Variable"), ENUM("Enum"), MODULE("Module"), STRUCT("Struct"), ENUM_MEMBER("Enum ordinal");

    private final String toString;

    IdentifierType(@NotNull final String toString) {
        this.toString = toString;
    }

    public String toString() {
        return toString;
    }
}
