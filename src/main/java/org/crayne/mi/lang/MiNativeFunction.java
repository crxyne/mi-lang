package org.crayne.mi.lang;

import org.crayne.mi.parsing.lexer.Token;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.*;

public class MiNativeFunction implements MiFunction {

    private final String name;
    private final MiDatatype returnType;
    private final MiModule module;
    private final Set<MiModifier> modifiers;
    private final List<MiVariable> parameters;
    private final Method nativeMethod;

    public MiNativeFunction(@NotNull final Method nativeMethod, @NotNull final Collection<MiModifier> modifiers, @NotNull final String name, @NotNull final MiDatatype returnType, @NotNull final MiModule module, @NotNull final Collection<MiVariable> parameters) {
        this.parameters = new ArrayList<>(parameters);
        this.name = name;
        this.returnType = returnType;
        this.module = module;
        this.modifiers = new HashSet<>(modifiers);
        this.nativeMethod = nativeMethod;
    }

    public MiNativeFunction(@NotNull final Method nativeMethod, @NotNull final Collection<MiModifier> modifiers, @NotNull final String name, @NotNull final MiDatatype returnType, @NotNull final MiModule module, @NotNull final MiVariable... parameters) {
        this.parameters = new ArrayList<>(List.of(parameters));
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

    public List<MiVariable> parameters() {
        return parameters;
    }

    public List<MiDatatype> parameterTypes() {
        return parameters.stream().map(MiVariable::type).toList();
    }

    public Method nativeMethod() {
        return nativeMethod;
    }

    public Token identifier() {
        return Token.of(module.identifier().token() + "." + name);
    }

}
