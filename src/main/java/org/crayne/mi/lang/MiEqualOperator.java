package org.crayne.mi.lang;

import org.crayne.mi.parsing.ast.NodeType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public enum MiEqualOperator {

    ADD, SUB, MULT, DIV, MOD, LSHIFT, RSHIFT, OR, XOR, AND, SET;

    public static Optional<MiEqualOperator> of(@NotNull final String token) {
        return Optional.ofNullable(switch (NodeType.of(token)) {
            case SET_ADD -> ADD;
            case SET_SUB -> SUB;
            case SET -> SET;
            case SET_AND -> AND;
            case SET_DIV -> DIV;
            case SET_MOD -> MOD;
            case SET_LSHIFT -> LSHIFT;
            case SET_MULT -> MULT;
            case SET_OR -> OR;
            case SET_RSHIFT -> RSHIFT;
            case SET_XOR -> XOR;
            default -> null;
        });
    }

}
