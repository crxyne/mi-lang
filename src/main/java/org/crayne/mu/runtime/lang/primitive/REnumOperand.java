package org.crayne.mu.runtime.lang.primitive;

import org.crayne.mu.runtime.SyntaxTree;
import org.crayne.mu.runtime.lang.RValue;
import org.jetbrains.annotations.NotNull;

public class REnumOperand implements ROperand {
    private final SyntaxTree runtime;
    public SyntaxTree runtime() {
        return runtime;
    }

    public REnumOperand(@NotNull final SyntaxTree runtime) {
        this.runtime = runtime;
    }

    public RValue equals(final RValue x, final RValue y) {
        return x.getType().equals(y.getType()) && x.getValue().equals(y.getValue()) ? RValue.TRUE : RValue.FALSE;
    }

    public RValue notEquals(final RValue x, final RValue y) {
        return !x.getType().equals(y.getType()) && x.getValue().equals(y.getValue()) ? RValue.TRUE : RValue.FALSE;
    }

}
