package org.crayne.mi.bytecode.reader.function;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

public class ByteCodeInternFunction extends ByteCodeRuntimeFunction {

    public ByteCodeInternFunction(@NotNull final Integer jumpLabel) {
        super(jumpLabel);
    }

    protected ByteCodeInternFunction(@NotNull final Method nativeMethod) {
        super(nativeMethod);
        throw new IllegalArgumentException("Cannot use native methods for intern functions");
    }

    public int label() {
        return jumpLabel == null ? -1 : jumpLabel;
    }

}
