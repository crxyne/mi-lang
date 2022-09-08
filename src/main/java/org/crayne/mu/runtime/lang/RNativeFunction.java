package org.crayne.mu.runtime.lang;

import org.crayne.mu.lang.Datatype;
import org.crayne.mu.lang.FunctionParameter;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.List;

public class RNativeFunction extends RFunction {
    private final Method nativeMethod;
    private final Class<?> nativeCallClass;

    public RNativeFunction(@NotNull final String name, @NotNull final Datatype returnType, @NotNull final List<FunctionParameter> definedParams, @NotNull final Method nativeMethod, @NotNull final Class<?> nativeCallClass) {
        super(name, returnType, definedParams, null);
        this.nativeMethod = nativeMethod;
        this.nativeCallClass = nativeCallClass;
    }

    public Method getNativeMethod() {
        return nativeMethod;
    }

    public Class<?> getNativeCallClass() {
        return nativeCallClass;
    }

    @Override
    public String toString() {
        return "RNativeFunction{\n" +
                ("name='" + getName() + '\'' +
                ",\nreturnType=" + getReturnType() +
                ",\ndefinedParams=" + getDefinedParams() +
                ",\nnativeMethod=" + nativeMethod).indent(4) +
                "}";
    }

}
