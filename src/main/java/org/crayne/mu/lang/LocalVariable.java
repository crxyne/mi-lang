package org.crayne.mu.lang;

import org.crayne.mu.runtime.parsing.ast.Node;
import org.crayne.mu.runtime.parsing.parser.scope.FunctionScope;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LocalVariable extends Variable {

    private final FunctionScope parent;
    private FunctionScope changedAt;

    public LocalVariable(final @NotNull String name, final @NotNull Datatype type, final @NotNull List<Modifier> modifiers, final Node value, @NotNull final FunctionScope parent) {
        super(name, type, modifiers);
        this.parent = parent;
    }

    public LocalVariable(@NotNull final Variable variable, @NotNull final FunctionScope parent) {
        super(variable.name(), variable.type(), variable.modifiers());
        this.parent = parent;
    }

    public FunctionScope parent() {
        return parent;
    }

    public FunctionScope changedAt() {
        return changedAt;
    }

    public Variable variable() {
        return new Variable(name(), type(), modifiers());
    }

    public void changedAt(@NotNull final FunctionScope scope) {
        changedAt = scope;
    }

}
