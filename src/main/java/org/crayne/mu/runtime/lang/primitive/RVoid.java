package org.crayne.mu.runtime.lang.primitive;

import org.crayne.mu.runtime.SyntaxTree;
import org.jetbrains.annotations.NotNull;

public class RVoid implements ROperand {

    private final SyntaxTree runtime;


    public SyntaxTree runtime() {
        return runtime;
    }

    public RVoid(@NotNull final SyntaxTree runtime) {
        this.runtime = runtime;
    }

}
