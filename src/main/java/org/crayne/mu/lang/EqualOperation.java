package org.crayne.mu.lang;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public enum EqualOperation {

    EQUAL(""),
    ADD("+"),
    SUB("-"),
    MULT("*"),
    DIV("/"),
    MOD("%"),
    SHIFTL("<<"),
    SHIFTR(">>"),
    AND("&"),
    OR("|"),
    XOR("^");

    private final String op;

    EqualOperation(@NotNull final String op) {
        this.op = op;
    }

    public String op() {
        return op;
    }

    public static EqualOperation of(@NotNull final String op) {
        return op.isEmpty() ? null : Arrays.stream(values()).filter(v -> v.op.equals(op.substring(0, op.length() - 1))).findFirst().orElse(null);
    }

}
