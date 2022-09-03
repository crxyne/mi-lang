package org.crayne.mu.lang;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class Variable {

    private final List<Modifier> modifiers;
    private final Datatype type;
    private final String name;

    public Variable(@NotNull final String name, @NotNull final Datatype type, @NotNull final List<Modifier> modifiers) {
        this.name = name;
        this.type = type;
        this.modifiers = modifiers;
    }

    public static Variable findVariableByName(@NotNull final List<Variable> globalModuleVariables, @NotNull final String name) {
        for (final Variable var : globalModuleVariables) if (var.name().equals(name)) return var;
        return null;
    }

    public static LocalVariable findLocalVariableByName(@NotNull final List<LocalVariable> localVariables, @NotNull final String name) {
        for (final LocalVariable var : localVariables) if (var.name().equals(name)) return var;
        return null;
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
                '}';
    }
}
