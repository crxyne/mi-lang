package org.crayne.mu.runtime.parsing.parser.scope;

import org.crayne.mu.runtime.parsing.parser.Parser;
import org.jetbrains.annotations.NotNull;

public class Scope {

    private final int scopeIndent;
    protected final int actualIndent;
    protected final ScopeType type;

    public Scope(@NotNull final ScopeType type) {
        this.scopeIndent = -1;
        this.actualIndent = -1;
        this.type = type;
    }

    public Scope(@NotNull final ScopeType type, final int scopeIndent, final int actualIndent) {
        this.scopeIndent = scopeIndent;
        this.actualIndent = actualIndent;
        this.type = type;
    }

    public int scopeIndent() {
        return scopeIndent;
    }

    public ScopeType type() {
        return type;
    }

    public void scopeEnd(@NotNull final Parser parser) {

    }

    @Override
    public String toString() {
        return "Scope{" +
                "scopeIndent=" + scopeIndent +
                ", type=" + type +
                '}';
    }
}
