package org.crayne.mu.lang;

import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("ClassCanBeRecord")
public class FunctionParameter {

    private final Datatype type;
    private final String name;
    private final List<Modifier> modifiers;
    // a seperate class for this since generics are going to be a thing at some point

    public FunctionParameter(@NotNull final Datatype type, @NotNull final String name, @NotNull final List<Modifier> modifiers) {
        this.type = type;
        this.name = name;
        this.modifiers = modifiers;
    }

    public String name() {
        return name;
    }

    public Datatype type() {
        return type;
    }

    public List<Modifier> modifiers() {
        return modifiers;
    }

    @Override
    public String toString() {
        return "FunctionParameter{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", modifiers=" + modifiers +
                '}';
    }
}
