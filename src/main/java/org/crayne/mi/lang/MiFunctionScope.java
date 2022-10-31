package org.crayne.mi.lang;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MiFunctionScope implements MiContainer {

    private final Set<MiVariable> variables;
    private final MiFunctionScope parent;

    public MiFunctionScope() {
        this.variables = new HashSet<>();
        this.parent = null;
    }

    public MiFunctionScope(@NotNull final MiFunctionScope parent) {
        this.variables = new HashSet<>();
        this.parent = parent;
    }

    public Optional<MiFunctionScope> parent() {
        return Optional.ofNullable(parent);
    }

    public Set<MiVariable> variables() {
        return variables;
    }

    public void add(@NotNull final MiVariable var) {
        variables.add(var);
    }

    public void addAll(@NotNull final Collection<MiVariable> vars) {
        variables.addAll(vars);
    }

    public void addAll(@NotNull final MiVariable... vars) {
        variables.addAll(List.of(vars));
    }

    public void pop() {
        variables.clear();
    }

    public Optional<MiVariable> find(@NotNull final String name) {
        return Optional
                .ofNullable(
                        variables
                                .stream()
                                .filter(v -> v.name().equals(name))
                                .findAny()
                                .orElse(
                                        parent().isPresent() ? parent().get().find(name).orElse(null) : null
                                )
                );
    }
}
