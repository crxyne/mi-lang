package org.crayne.mi.lang;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MiFunctionScope implements MiContainer {

    private final Set<MiVariable> variables;
    private final List<MiFunctionScope> children;
    private final MiFunctionScope parent;
    private MiInternFunction function;
    private boolean reachedScopeEnd;
    private boolean reachedLoopScopeEnd;
    private final boolean conditional;
    private boolean ifReachedEnd;
    private final MiScopeType scopeType;

    public MiFunctionScope(@NotNull final MiScopeType scopeType) {
        this.variables = new HashSet<>();
        this.children = new ArrayList<>();
        this.parent = null;
        reachedScopeEnd = false;
        conditional = false;
        this.scopeType = scopeType;
    }

    public MiFunctionScope(@NotNull final MiScopeType scopeType, @NotNull final MiInternFunction function, @NotNull final MiFunctionScope parent) {
        this.variables = new HashSet<>();
        this.children = new ArrayList<>();
        this.parent = parent;
        this.function = function;
        reachedScopeEnd = false;
        if (parent().isPresent() && scopeType == MiScopeType.FUNCTION_LOCAL) {
            this.scopeType = parent().get().scopeType == MiScopeType.FUNCTION_ROOT ? MiScopeType.FUNCTION_LOCAL : parent().get().type();
            conditional = scopeType.conditional();
            return;
        }
        this.scopeType = scopeType;
        conditional = scopeType.conditional();
    }

    public void ifReachedEnd() {
        if (scopeType == MiScopeType.ELSE) ifReachedEnd = true; // for else scopes specifically
    }

    public boolean ifHasReachedEnd() {
        return ifReachedEnd || (parent().isPresent() && parent().get().ifHasReachedEnd());
    }

    public MiScopeType type() {
        return scopeType;
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

    public boolean rawReachedEnd() {
        return reachedScopeEnd;
    }

    public boolean hasReachedScopeEndSingle() {
        return (!conditional || ifReachedEnd) && reachedScopeEnd;
    }

    public boolean hasReachedScopeEnd() {
        return hasReachedScopeEndSingle() || children
                .stream()
                .anyMatch(MiFunctionScope::hasReachedScopeEnd);
    }

    public void reachedLoopEnd() {
        if (looping()) reachedLoopScopeEnd = true;
    }

    public boolean rawReachedLoopEnd() {
        return reachedLoopScopeEnd;
    }

    public boolean hasReachedLoopEndSingle() {
        return ((!conditional || scopeType.looping()) || ifReachedEnd) && reachedLoopScopeEnd;
    }

    public boolean hasReachedLoopEnd() {
        return hasReachedLoopEndSingle() || (looping() && children
                .stream()
                .anyMatch(MiFunctionScope::hasReachedLoopEnd));
    }

    public boolean conditional() {
        return conditional || parent().isPresent() && parent().get().scopeType != MiScopeType.FUNCTION_LOCAL && parent().get().conditional();
    }

    public boolean looping() {
        return scopeType.looping() || parent().isPresent() && parent().get().scopeType != MiScopeType.FUNCTION_LOCAL && parent().get().looping();
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
