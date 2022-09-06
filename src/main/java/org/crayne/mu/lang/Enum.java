package org.crayne.mu.lang;

import org.crayne.mu.runtime.parsing.parser.scope.EnumScope;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Enum {

    private final List<String> members;
    private final String name;
    private final List<Modifier> modifiers;

    public Enum(@NotNull final String name, @NotNull final List<String> members, @NotNull final List<Modifier> modifiers) {
        this.members = new ArrayList<>(members);
        this.name = name;
        this.modifiers = new ArrayList<>(modifiers);
    }

    public List<String> members() {
        return members;
    }

    public static Enum of(@NotNull final EnumScope scope) {
        return new Enum(scope.name(), scope.members(), scope.modifiers());
    }

}
