package org.crayne.mu.runtime.lang.primitive;

import org.crayne.mu.runtime.SyntaxTreeExecution;
import org.crayne.mu.runtime.lang.RValue;
import org.jetbrains.annotations.NotNull;

public class RNull implements ROperand {

    private final SyntaxTreeExecution runtime;

    public SyntaxTreeExecution runtime() {
        return runtime;
    }

    public RValue unexpectedOperator(final String op) {
        runtime().runtimeError("Mu NullError: Unexpected operator '" + op + "' on a null-value");
        return null;
    }

    public RNull(@NotNull final SyntaxTreeExecution runtime) {
        this.runtime = runtime;
    }

    public RValue equals(final RValue x, final RValue y) {
        return x.getValue().equals(y.getValue()) ? RValue.TRUE : RValue.FALSE;
    }

    public RValue notEquals(final RValue x, final RValue y) {
        return !x.getValue().equals(y.getValue()) ? RValue.TRUE : RValue.FALSE;
    }

    public RValue add(final RValue x, final RValue y) {
        return unexpectedOperator("+");
    }

    public RValue subtract(final RValue x, final RValue y) {
        return unexpectedOperator("-");
    }

    public RValue multiply(final RValue x, final RValue y) {
        return unexpectedOperator("*");
    }

    public RValue divide(final RValue x, final RValue y) {
        return unexpectedOperator("/");
    }

    public RValue modulus(final RValue x, final RValue y) {
        return unexpectedOperator("%");
    }

    public RValue logicalAnd(final RValue x, final RValue y) {
        return unexpectedOperator("&&");
    }

    public RValue logicalOr(final RValue x, final RValue y) {
        return unexpectedOperator("||");
    }

    public RValue bitXor(final RValue x, final RValue y) {
        return unexpectedOperator("^");
    }

    public RValue bitAnd(final RValue x, final RValue y) {
        return unexpectedOperator("&");
    }

    public RValue bitOr(final RValue x, final RValue y) {
        return unexpectedOperator("|");
    }

    public RValue bitShiftLeft(final RValue x, final RValue y) {
        return unexpectedOperator("<<");
    }

    public RValue bitShiftRight(final RValue x, final RValue y) {
        return unexpectedOperator(">>");
    }

    public RValue lessThan(final RValue x, final RValue y) {
        return unexpectedOperator("<");
    }

    public RValue greaterThan(final RValue x, final RValue y) {
        return unexpectedOperator(">");
    }

    public RValue lessThanOrEquals(final RValue x, final RValue y) {
        return unexpectedOperator("<=");
    }

    public RValue greaterThanOrEquals(final RValue x, final RValue y) {
        return unexpectedOperator(">=");
    }

}
