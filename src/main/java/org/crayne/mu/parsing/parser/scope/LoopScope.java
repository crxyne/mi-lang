package org.crayne.mu.parsing.parser.scope;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class LoopScope extends FunctionScope {

    private boolean reachedBreakOrContinue;

    public LoopScope(@NotNull final ScopeType type, final int scopeIndent, final int actualIndent, final FunctionScope parent) {
        super(type, scopeIndent, actualIndent, Collections.emptyList(), parent);
    }

    public LoopScope(@NotNull final ScopeType type, final int scopeIndent, final int actualIndent, final FunctionScope parent, @NotNull final List<String> using) {
        super(type, scopeIndent, actualIndent, Collections.emptyList(), parent, using);
    }

    public void reachedLoopStepBreak() {
        if (parent != null) {
            FunctionScope scope = this;
            if (scope.type != ScopeType.IF) {
                scope = scope.parent;
                if (scope != null) {
                    if ((scope.type == ScopeType.WHILE || scope.type == ScopeType.FOR || scope.type == ScopeType.DO) && scope instanceof final LoopScope loopScope) loopScope.reachedBreakOrContinue = true;
                   // else scope.reachedEnd();
                }
            }
        }
        reachedBreakOrContinue = true;
    }

    public boolean unreachable() {
        return hasReachedEnd() || reachedBreakOrContinue;
    }
}
