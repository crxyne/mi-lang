package org.crayne.mu.runtime.lang.primitive;

import org.crayne.mu.runtime.SyntaxTree;
import org.crayne.mu.runtime.lang.RValue;
import org.jetbrains.annotations.NotNull;

public class RBoolean implements ROperand {

    private final SyntaxTree runtime;
    public SyntaxTree runtime() {
        return runtime;
    }

    public RBoolean(@NotNull final SyntaxTree runtime) {
        this.runtime = runtime;
    }

    public RValue equals(final RValue x, final RValue y) {
        return (((Boolean) x.getValue()).booleanValue() == ((Boolean) y.getValue()).booleanValue()) ? RValue.TRUE : RValue.FALSE;
    }

    public RValue notEquals(final RValue x, final RValue y) {
        return (((Boolean) x.getValue()).booleanValue() != ((Boolean) y.getValue()).booleanValue()) ? RValue.TRUE : RValue.FALSE;
    }

    public RValue logicalAnd(final RValue x, final RValue y) {
        return ((Boolean) x.getValue() && (Boolean) y.getValue()) ? RValue.TRUE : RValue.FALSE;
    }

    public RValue logicalOr(final RValue x, final RValue y) {
        return ((Boolean) x.getValue() || (Boolean) y.getValue()) ? RValue.TRUE : RValue.FALSE;
    }

    public RValue bitXor(final RValue x, final RValue y) {
        return ((Boolean) x.getValue() ^ (Boolean) y.getValue()) ? RValue.TRUE : RValue.FALSE;
    }

    public RValue bitAnd(final RValue x, final RValue y) {
        return ((Boolean) x.getValue() & (Boolean) y.getValue()) ? RValue.TRUE : RValue.FALSE;
    }

    public RValue bitOr(final RValue x, final RValue y) {
        return ((Boolean) x.getValue() | (Boolean) y.getValue()) ? RValue.TRUE : RValue.FALSE;
    }

}
