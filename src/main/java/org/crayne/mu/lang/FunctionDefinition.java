package org.crayne.mu.lang;

import org.crayne.mu.runtime.parsing.ast.Node;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FunctionDefinition {

    private final String name;
    private final Datatype returnType;
    private final List<FunctionParameter> parameters;
    private final List<Modifier> modifiers;
    private Node scope;

    public FunctionDefinition(@NotNull final String name, @NotNull final Datatype returnType, @NotNull final List<FunctionParameter> parameters, @NotNull final List<Modifier> modifiers) {
        this.name = name;
        this.returnType = returnType;
        this.parameters = parameters;
        this.modifiers = modifiers;
    }

    public FunctionDefinition(@NotNull final String name, @NotNull final Datatype returnType, @NotNull final List<FunctionParameter> parameters, @NotNull final List<Modifier> modifiers, @NotNull final Node scope) {
        this.name = name;
        this.returnType = returnType;
        this.parameters = parameters;
        this.modifiers = modifiers;
        this.scope = scope;
    }

    public void scope(@NotNull final Node scope) {
        this.scope = scope;
    }

    public String name() {
        return name;
    }

    public Datatype returnType() {
        return returnType;
    }

    public List<FunctionParameter> parameters() {
        return parameters;
    }

    public List<Modifier> modifiers() {
        return modifiers;
    }

    public Node scope() {
        return scope;
    }

    @Override
    public String toString() {
        return "FunctionDefinition{" +
                "name='" + name + '\'' +
                ", returnType=" + returnType +
                ", parameters=" + parameters +
                ", modifiers=" + modifiers +
                ", scope=" + scope +
                '}';
    }
}
