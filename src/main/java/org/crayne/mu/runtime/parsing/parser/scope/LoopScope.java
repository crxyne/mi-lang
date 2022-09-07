package org.crayne.mu.runtime.parsing.parser.scope;

import org.jetbrains.annotations.NotNull;

public class LoopScope extends FunctionScope {

    private boolean reachedBreakOrContinue;

    public LoopScope(@NotNull final ScopeType type, final int scopeIndent, final int actualIndent, final FunctionScope parent) {
        super(type, scopeIndent, actualIndent, parent);
    }

    public void reachedLoopStepBreak() {
        if (parent != null) {
            FunctionScope scope = this;
            if (scope.type != ScopeType.IF) {
                scope = scope.parent;
                if (scope != null) {
                    if ((scope.type == ScopeType.WHILE || scope.type == ScopeType.FOR) && scope instanceof final LoopScope loopScope) loopScope.reachedBreakOrContinue = true;
                    else scope.reachedEnd();
                }
            }
        }
        reachedBreakOrContinue = true;
    }

    public boolean unreachable() {
        return hasReachedEnd() || reachedBreakOrContinue;
    }
}
