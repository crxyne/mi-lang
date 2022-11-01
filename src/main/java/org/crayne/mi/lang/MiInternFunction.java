package org.crayne.mi.lang;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class MiInternFunction extends MiFunctionScope implements MiFunction {

    private final String name;
    private final MiDatatype returnType;
    private final MiModule module;
    private final Set<MiModifier> modifiers;
    private final Set<MiVariable> parameters;

    public MiInternFunction(@NotNull final Collection<MiModifier> modifiers, @NotNull final String name, @NotNull final MiDatatype returnType, @NotNull final MiModule module, @NotNull final Collection<MiVariable> parameters) {
        super();
        addAll(parameters.stream().peek(v -> v.container(this)).toList());
        this.parameters = new HashSet<>(parameters);
        this.name = name;
        this.returnType = returnType;
        this.module = module;
        this.modifiers = new HashSet<>(modifiers);
        function(this);
    }

    public MiInternFunction(@NotNull final Collection<MiModifier> modifiers, @NotNull final String name, @NotNull final MiDatatype returnType, @NotNull final MiModule module, @NotNull final MiVariable... parameters) {
        super();
        addAll(Arrays.stream(parameters).peek(v -> v.container(this)).toList());
        this.parameters = new HashSet<>(List.of(parameters));
        this.name = name;
        this.returnType = returnType;
        this.module = module;
        this.modifiers = new HashSet<>(modifiers);
        function(this);
    }

    public MiModule module() {
        return module;
    }

    public MiDatatype returnType() {
        return returnType;
    }

    public Set<MiModifier> modifiers() {
        return modifiers;
    }

    public String name() {
        return name;
    }

    public Set<MiVariable> parameters() {
        return parameters;
    }

    public Set<MiDatatype> parameterTypes() {
        return parameters.stream().map(MiVariable::type).collect(Collectors.toSet());
    }

}
