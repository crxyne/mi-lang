package org.crayne.mu.runtime.lang;

import org.crayne.mu.lang.Datatype;
import org.crayne.mu.lang.FunctionParameter;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.List;

public class RNativeFunction extends RFunction {
    private final Method nativeMethod;

    public RNativeFunction(@NotNull final String name, @NotNull final Datatype returnType, @NotNull final List<FunctionParameter> definedParams, @NotNull final Method nativeMethod) {
        super(name, returnType, definedParams, null);
        this.nativeMethod = nativeMethod;
    }

    public Method getNativeMethod() {
        return nativeMethod;
    }

    @Override
    public String toString() {
        return "RNativeFunction{" +
                "name='" + getName() + '\'' +
                ", returnType=" + getReturnType() +
                ", definedParams=" + getDefinedParams() +
                ", nativeMethod=" + nativeMethod +
                '}';
    }

}
