package org.crayne.mi.lang;

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
        if (op.equals("++")) return ADD;
        if (op.equals("--")) return SUB;
        if (!op.isEmpty() && op.substring(0, op.length() - 1).isEmpty()) {
            if (!op.equals("=")) return null;
            return EQUAL;
        }
        if (op.length() > 1 && op.charAt(1) != '=') return null;
        final EqualOperation result = op.isEmpty() ? null : Arrays.stream(values()).filter(v -> v.op.equals(op.substring(0, op.length() - 1))).findFirst().orElse(null);
        if (result == EQUAL) return null;
        return result;
    }

}
