package org.crayne.mu.runtime.lang.primitive;

import org.crayne.mu.runtime.SyntaxTree;
import org.crayne.mu.runtime.lang.RDatatype;
import org.crayne.mu.runtime.lang.RValue;
import org.jetbrains.annotations.NotNull;

public interface ROperand {

    SyntaxTree runtime();

    default RValue unexpectedOperator(final String op) {
        runtime().runtimeError("Unexpected operator '" + op + "'");
        return null;
    }

    default RValue add(final RValue x, final RValue y) {
        return unexpectedOperator("+");
    }

    default RValue subtract(final RValue x, final RValue y) {
        return unexpectedOperator("-");
    }

    default RValue multiply(final RValue x, final RValue y) {
        return unexpectedOperator("*");
    }

    default RValue divide(final RValue x, final RValue y) {
        return unexpectedOperator("/");
    }

    default RValue modulus(final RValue x, final RValue y) {
        return unexpectedOperator("%");
    }

    default RValue equals(final RValue x, final RValue y) {
        return unexpectedOperator("==");
    }

    default RValue notEquals(final RValue x, final RValue y) {
        return unexpectedOperator("!=");
    }

    default RValue logicalAnd(final RValue x, final RValue y) {
        return unexpectedOperator("&&");
    }

    default RValue logicalOr(final RValue x, final RValue y) {
        return unexpectedOperator("||");
    }

    default RValue bitXor(final RValue x, final RValue y) {
        return unexpectedOperator("^");
    }

    default RValue bitAnd(final RValue x, final RValue y) {
        return unexpectedOperator("&");
    }

    default RValue bitOr(final RValue x, final RValue y) {
        return unexpectedOperator("|");
    }

    default RValue bitShiftLeft(final RValue x, final RValue y) {
        return unexpectedOperator("<<");
    }

    default RValue bitShiftRight(final RValue x, final RValue y) {
        return unexpectedOperator(">>");
    }

    default RValue lessThan(final RValue x, final RValue y) {
        return unexpectedOperator("<");
    }

    default RValue greaterThan(final RValue x, final RValue y) {
        return unexpectedOperator(">");
    }

    default RValue lessThanOrEquals(final RValue x, final RValue y) {
        return unexpectedOperator("<=");
    }

    default RValue greaterThanOrEquals(final RValue x, final RValue y) {
        return unexpectedOperator(">=");
    }

    static ROperand of(@NotNull final RDatatype type, @NotNull final SyntaxTree tree) {
        if (!type.primitive()) return new REnumOperand(tree); // because we currently only have enums as non primitive types

        return switch (type.getPrimitive()) {
            case BOOL -> new RBoolean(tree);
            case CHAR -> new RChar(tree);
            case NULL -> new RNull(tree);
            case INT -> new RInt(tree);
            case LONG -> new RLong(tree);
            case FLOAT -> new RFloat(tree);
            case STRING -> new RString(tree);
            case DOUBLE -> new RDouble(tree);
            case VOID -> new RVoid(tree);
        };
    }


}
