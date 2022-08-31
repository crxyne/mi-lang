package org.crayne.mu.lang;

import org.crayne.mu.runtime.parsing.ast.Node;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class Variable {

    private final List<Modifier> modifiers;
    private final Datatype type;
    private final String name;
    private final Node value;

    public Variable(@NotNull final String name, @NotNull final Datatype type, @NotNull final List<Modifier> modifiers, final Node value) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.modifiers = modifiers;
    }

    public String name() {
        return name;
    }

    public Datatype type() {
        return type;
    }

    public List<Modifier> getmodifiers() {
        return modifiers;
    }

    public boolean isConstant() {
        return isConstant(modifiers);
    }

    public static boolean isConstant(@NotNull final Collection<Modifier> modifiers) {
        return modifiers.contains(Modifier.CONSTANT) || !modifiers.contains(Modifier.MUTABLE);
    }

    @Override
    public String toString() {
        return "Variable{" +
                "modifiers=" + modifiers +
                ", type=" + type +
                ", name='" + name + '\'' +
                ", value=" + value +
                '}';
    }
}
