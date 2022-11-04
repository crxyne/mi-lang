package org.crayne.mi.lang;

import org.crayne.mi.parsing.lexer.Token;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    public Optional<MiFunction> findFunction(@NotNull final String name, @NotNull final List<MiDatatype> parameters, final boolean exactMatch) {
        return filterFunctionsByName(name).stream().filter(f -> {
            final List<MiDatatype> funcParams = f.parameterTypes();
            if (funcParams.size() == parameters.size()) {
                if (funcParams.stream().anyMatch(Objects::isNull) || parameters.stream().anyMatch(Objects::isNull)) return false;

                final Map<MiDatatype, MiDatatype> matchTypes = IntStream.range(0, funcParams.size())
                        .boxed()
                        .collect(Collectors.toMap(funcParams::get, parameters::get, (p1, p2) -> p1));

                return matchTypes // make sure every datatype matches
                        .keySet()
                        .stream()
                        .allMatch(d -> exactMatch ? matchTypes.get(d).name().equals(d.name()) : MiDatatype.match(matchTypes.get(d), d));
            }
            return false;
        }).findFirst();
    }

    public Token identifier() {
        final StringBuilder result = new StringBuilder(name);
        MiModule currentParent = parent;
        while (currentParent != null) {
            result.insert(0, currentParent.name + ".");
            currentParent = currentParent.parent;
        }
        return Token.of(result.toString());
    }

}
