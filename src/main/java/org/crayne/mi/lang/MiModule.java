package org.crayne.mi.lang;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MiModule implements MiContainer {

    private final String name;
    private final MiModule parent;
    private final Set<MiModule> submodules;
    private final Set<MiVariable> variables;
    private final Set<MiFunction> functions;
    private final Set<MiEnum> enums;

    public MiModule(@NotNull final String name, @NotNull final MiModule parent) {
        this.variables = new HashSet<>();
        this.name = name;
        this.parent = parent;
        this.submodules = new HashSet<>();
        this.functions = new HashSet<>();
        this.enums = new HashSet<>();
    }

    public MiModule(@NotNull final String name) {
        this.variables = new HashSet<>();
        this.name = name;
        this.parent = null;
        this.submodules = new HashSet<>();
        this.functions = new HashSet<>();
        this.enums = new HashSet<>();
    }

    public String name() {
        return name;
    }

    public Set<MiModule> submodules() {
        return submodules;
    }

    public Set<MiFunction> functions() {
        return functions;
    }

    public Set<MiEnum> enums() {
        return enums;
    }

    public Optional<MiModule> parent() {
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
        return variables.stream().filter(v -> v.name().equals(name)).findAny();
    }

    public Optional<MiModule> findSubmoduleByName(@NotNull final String name) {
        return submodules.stream().filter(m -> m.name.equals(name)).findAny();
    }

    public Optional<MiEnum> findEnumByName(@NotNull final String name) {
        return enums.stream().filter(e -> e.name().equals(name)).findAny();
    }

    public List<MiFunction> filterFunctionsByName(@NotNull final String name) {
        return functions.stream().filter(f -> f.name().equals(name)).toList();
    }

    public Optional<MiFunction> findFunction(@NotNull final String name, @NotNull final Collection<MiDatatype> parameters) {
        return filterFunctionsByName(name).stream().filter(f -> {
            final Set<MiDatatype> funcParams = f.parameterTypes();
            return new HashSet<>(parameters).containsAll(funcParams) && new HashSet<>(funcParams).containsAll(parameters);
        }).findAny();
    }

}
