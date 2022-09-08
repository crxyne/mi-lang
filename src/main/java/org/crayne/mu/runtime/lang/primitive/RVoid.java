package org.crayne.mu.runtime.lang.primitive;

import org.crayne.mu.runtime.SyntaxTreeExecution;
import org.jetbrains.annotations.NotNull;

public class RVoid implements ROperand {

    private final SyntaxTreeExecution runtime;


    public SyntaxTreeExecution runtime() {
        return runtime;
    }

    public RVoid(@NotNull final SyntaxTreeExecution runtime) {
        this.runtime = runtime;
    }

}
