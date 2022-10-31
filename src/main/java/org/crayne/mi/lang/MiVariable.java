package org.crayne.mi.lang;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class MiVariable {

    private final String name;
    private boolean initialized;
    private final MiDatatype type;
    private final Set<MiModifier> modifiers;
    private MiContainer container;

    public MiVariable(@NotNull final MiContainer container, @NotNull final String name, @NotNull final MiDatatype type, @NotNull final Collection<MiModifier> modifiers) {
        this.container = container;
        this.name = name;
        this.type = type;
        this.modifiers = new HashSet<>(modifiers);
        this.initialized = false;
    }

    public MiVariable(@NotNull final MiContainer container, @NotNull final String name, @NotNull final MiDatatype type, @NotNull final Collection<MiModifier> modifiers, final boolean initialized) {
        this.container = container;
        this.name = name;
        this.type = type;
        this.modifiers = new HashSet<>(modifiers);
        this.initialized = initialized;
    }

    public MiVariable(@NotNull final String name, @NotNull final MiDatatype type, @NotNull final Collection<MiModifier> modifiers) {
        this.name = name;
        this.type = type;
        this.modifiers = new HashSet<>(modifiers);
        this.initialized = true;
    }

    public MiContainer container() {
        return container;
    }

    protected void container(@NotNull final MiContainer container) {
        this.container = container;
    }

    public Set<MiModifier> modifiers() {
        return modifiers;
    }

    public boolean initialized() {
        return initialized;
    }

    public MiDatatype type() {
        return type;
    }

    public String name() {
        return name;
    }

    public void initialize() {
        this.initialized = true;
    }
}
