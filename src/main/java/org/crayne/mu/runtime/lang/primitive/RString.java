package org.crayne.mu.runtime.lang.primitive;

import org.crayne.mu.runtime.SyntaxTreeExecution;
import org.crayne.mu.runtime.lang.REvaluator;
import org.crayne.mu.runtime.lang.RValue;
import org.jetbrains.annotations.NotNull;

public class RString implements ROperand {

    private final SyntaxTreeExecution runtime;


    public SyntaxTreeExecution runtime() {
        return runtime;
    }

    public RString(@NotNull final SyntaxTreeExecution runtime) {
        this.runtime = runtime;
    }

    public RValue add(final RValue x, final RValue y) {
        return REvaluator.concat(x, y);
    }

    public RValue equals(final RValue x, final RValue y) {
        return (String.valueOf(x.getValue())).equals(String.valueOf(y.getValue())) ? RValue.TRUE : RValue.FALSE;
    }

    public RValue notEquals(final RValue x, final RValue y) {
        return !(String.valueOf(x.getValue())).equals(String.valueOf(y.getValue())) ? RValue.TRUE : RValue.FALSE;
    }

}
