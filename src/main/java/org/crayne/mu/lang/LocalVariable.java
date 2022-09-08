package org.crayne.mu.lang;

import org.crayne.mu.parsing.parser.scope.FunctionScope;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class LocalVariable extends Variable {

    private final FunctionScope parent;
    private FunctionScope changedAt;

    public LocalVariable(@NotNull final String name, @NotNull final Datatype type, @NotNull final List<Modifier> modifiers, @NotNull final Module module, @NotNull final FunctionScope parent) {
        super(name, type, modifiers, module);
        this.parent = parent;
    }

    public LocalVariable(@NotNull final Variable variable, @NotNull final FunctionScope parent) {
        super(variable.name(), variable.type(), variable.modifiers(), variable.module(), variable.initialized());
        this.parent = parent;
        if (variable().initialized()) changedAt(this.parent);
    }

    public FunctionScope parent() {
        return parent;
    }

    public Optional<FunctionScope> changedAt() {
        return Optional.ofNullable(changedAt);
    }

    public Variable variable() {
        return new Variable(name(), type(), modifiers(),module(), initialized());
    }

    public void changedAt(@NotNull final FunctionScope scope) {
        changedAt = scope;
    }

}
