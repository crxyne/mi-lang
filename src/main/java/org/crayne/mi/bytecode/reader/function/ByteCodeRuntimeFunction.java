package org.crayne.mi.bytecode.reader.function;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

public class ByteCodeRuntimeFunction {

    protected final Integer jumpLabel;
    protected final Method nativeMethod;

    protected ByteCodeRuntimeFunction(@NotNull final Integer jumpLabel) {
        this.jumpLabel = jumpLabel;
        this.nativeMethod = null;
    }

    protected ByteCodeRuntimeFunction(@NotNull final Method nativeMethod) {
        this.jumpLabel = null;
        this.nativeMethod = nativeMethod;
    }

    @Override
    public String toString() {
        if (jumpLabel == null) {
            return "ByteCodeRuntimeFunction{" +
                    "nativeMethod=" + nativeMethod +
                    '}';
        }
        return "ByteCodeRuntimeFunction{" +
                "jumpLabel=" + jumpLabel +
                '}';
    }
}
