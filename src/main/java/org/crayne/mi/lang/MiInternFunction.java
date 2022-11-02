package org.crayne.mi.lang;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MiInternFunction extends MiFunctionScope implements MiFunction {

    private final String name;
    private final MiDatatype returnType;
    private final MiModule module;
    private final Set<MiModifier> modifiers;
    private final List<MiVariable> parameters;

    public MiInternFunction(@NotNull final Collection<MiModifier> modifiers, @NotNull final String name, @NotNull final MiDatatype returnType, @NotNull final MiModule module, @NotNull final Collection<MiVariable> parameters) {
        super();
        addAll(parameters.stream().peek(v -> v.container(this)).toList());
        this.parameters = new ArrayList<>(parameters);
        this.name = name;
        this.returnType = returnType;
        this.module = module;
        this.modifiers = new HashSet<>(modifiers);
        function(this);
    }

    public MiInternFunction(@NotNull final Collection<MiModifier> modifiers, @NotNull final String name, @NotNull final MiDatatype returnType, @NotNull final MiModule module, @NotNull final MiVariable... parameters) {
        super();
        addAll(Arrays.stream(parameters).peek(v -> v.container(this)).toList());
        this.parameters = new ArrayList<>(List.of(parameters));
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

    public List<MiVariable> parameters() {
        return parameters;
    }

    public List<MiDatatype> parameterTypes() {
        return parameters.stream().map(MiVariable::type).toList();
    }

}
