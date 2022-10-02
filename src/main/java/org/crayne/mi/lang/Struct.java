package org.crayne.mi.lang;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class Struct {

    private final String name;
    private final Module module;

    private final List<Modifier> modifiers;
    private final List<Variable> members;

    public Struct(@NotNull final String name, @NotNull final Module module, @NotNull final List<Modifier> modifiers) {
        this.name = name;
        this.module = module;
        members = new ArrayList<>();
        this.modifiers = new ArrayList<>(modifiers);
    }

    public Struct(@NotNull final String name, @NotNull final Module module, @NotNull final List<Variable> members, @NotNull final List<Modifier> modifiers) {
        this.name = name;
        this.module = module;
        this.members = new ArrayList<>(members);
        this.modifiers = new ArrayList<>(modifiers);
    }

    public static Optional<Struct> findStructByName(@NotNull final Collection<Struct> moduleStructs, @NotNull final String name) {
        Struct result = null;
        for (final Struct var : moduleStructs) if (var.name.equals(name)) result = var;
        return Optional.ofNullable(result);
    }

    public String name() {
        return name;
    }

    public Module module() {
        return module;
    }

    public List<Modifier> modifiers() {
        return modifiers;
    }

    public List<Variable> members() {
        return members;
    }

}
