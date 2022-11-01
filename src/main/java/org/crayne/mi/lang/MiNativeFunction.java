package org.crayne.mi.lang;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class MiNativeFunction implements MiFunction {

    private final String name;
    private final MiDatatype returnType;
    private final MiModule module;
    private final Set<MiModifier> modifiers;
    private final Set<MiVariable> parameters;
    private final Method nativeMethod;

    public MiNativeFunction(@NotNull final Method nativeMethod, @NotNull final Collection<MiModifier> modifiers, @NotNull final String name, @NotNull final MiDatatype returnType, @NotNull final MiModule module, @NotNull final Collection<MiVariable> parameters) {
        this.parameters = new HashSet<>(parameters);
        this.name = name;
        this.returnType = returnType;
        this.module = module;
        this.modifiers = new HashSet<>(modifiers);
        this.nativeMethod = nativeMethod;
    }

    public MiNativeFunction(@NotNull final Method nativeMethod, @NotNull final Collection<MiModifier> modifiers, @NotNull final String name, @NotNull final MiDatatype returnType, @NotNull final MiModule module, @NotNull final MiVariable... parameters) {
        this.parameters = new HashSet<>(List.of(parameters));
        this.name = name;
        this.returnType = returnType;
        this.module = module;
        this.modifiers = new HashSet<>(modifiers);
        this.nativeMethod = nativeMethod;
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

    public Method nativeMethod() {
        return nativeMethod;
    }

}
