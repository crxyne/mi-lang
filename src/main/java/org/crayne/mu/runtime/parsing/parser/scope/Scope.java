package org.crayne.mu.runtime.parsing.parser.scope;

import org.jetbrains.annotations.NotNull;

public class Scope {

    private final int scopeIndent;
    private final ScopeType type;

    public Scope(@NotNull final ScopeType type, int scopeIndent) {
        this.scopeIndent = scopeIndent;
        this.type = type;
    }

    public int scopeIndent() {
        return scopeIndent;
    }

    public ScopeType type() {
        return type;
    }

    @Override
    public String toString() {
        return "Scope{" +
                "scopeIndent=" + scopeIndent +
                ", type=" + type +
                '}';
    }
}
