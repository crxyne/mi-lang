package org.crayne.mi.lang;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MiFunctionScope implements MiContainer {

    private final Set<MiVariable> variables;
    private final List<MiFunctionScope> children;
    private final MiFunctionScope parent;
    private MiInternFunction function;
    private boolean reachedScopeEnd;

    public MiFunctionScope() {
        this.variables = new HashSet<>();
        this.children = new ArrayList<>();
        this.parent = null;
        reachedScopeEnd = false;
    }

    public MiFunctionScope(@NotNull final MiInternFunction function) {
        this.variables = new HashSet<>();
        this.children = new ArrayList<>();
        this.parent = null;
        this.function = function;
        reachedScopeEnd = false;
    }

    public MiFunctionScope(@NotNull final MiInternFunction function, @NotNull final MiFunctionScope parent) {
        this.variables = new HashSet<>();
        this.children = new ArrayList<>();
        this.parent = parent;
        this.function = function;
        reachedScopeEnd = false;
    }

    public void childScope(@NotNull final MiFunctionScope scope) {
        children.add(scope);
    }

    public List<MiFunctionScope> children() {
        return children;
    }

    public Optional<MiFunctionScope> parent() {
        return Optional.ofNullable(parent);
    }

    public MiInternFunction function() {
        return function;
    }

    public void function(@NotNull final MiInternFunction function) {
        this.function = function;
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

    public void reachedScopeEnd() {
        reachedScopeEnd = true;
    }

    public boolean hasReachedScopeEndSingle() {
        return reachedScopeEnd;
    }

    public boolean hasReachedScopeEnd() {
        return hasReachedScopeEndSingle() || children
                .stream()
                .anyMatch(MiFunctionScope::hasReachedScopeEnd);
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
