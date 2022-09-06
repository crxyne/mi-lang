package org.crayne.mu.lang;

import org.crayne.mu.runtime.parsing.parser.scope.EnumScope;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public class Enum {

    private final List<String> members;
    private final String name;
    private final List<Modifier> modifiers;
    private final Module module;

    public Enum(@NotNull final String name, @NotNull final Module module, @NotNull final List<String> members, @NotNull final List<Modifier> modifiers) {
        this.members = new ArrayList<>(members);
        this.module = module;
        this.name = name;
        this.modifiers = new ArrayList<>(modifiers);
    }

    public List<String> members() {
        return members;
    }

    public static Enum of(@NotNull final EnumScope scope) {
        return new Enum(scope.name(), scope.module(), scope.members(), scope.modifiers());
    }

    public boolean equals(@NotNull final Enum other) {
        return name.equals(other.name) && module == other.module;
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

    public static Optional<Enum> findEnumByName(@NotNull final HashSet<Enum> moduleEnums, @NotNull final String name) {
        Enum result = null;
        for (final Enum var : moduleEnums) if (var.name.equals(name)) result = var;
        return Optional.ofNullable(result);
    }

}
