package org.crayne.mu.lang;

import org.crayne.mu.runtime.parsing.ast.NodeType;
import org.jetbrains.annotations.NotNull;

public enum Modifier {
    PUBLIC, PRIVATE, PROTECTED, OWN, MUTABLE, CONSTANT;

    public static Modifier of(@NotNull final NodeType nodeType) {
        return switch (nodeType) {
            case LITERAL_PUB -> PUBLIC;
            case LITERAL_PRIV -> PRIVATE;
            case LITERAL_PROT -> PROTECTED;
            case LITERAL_OWN -> OWN;
            case LITERAL_MUT -> MUTABLE;
            case LITERAL_CONST -> CONSTANT;
            default -> null;
        };
    }

}
