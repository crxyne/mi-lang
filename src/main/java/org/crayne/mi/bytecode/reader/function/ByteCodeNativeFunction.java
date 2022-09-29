package org.crayne.mi.bytecode.reader.function;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

public class ByteCodeNativeFunction extends ByteCodeRuntimeFunction {

    protected ByteCodeNativeFunction(@NotNull final Integer jumpLabel) {
        super(jumpLabel);
        throw new IllegalArgumentException("Cannot use jump labels for native functions");
    }

    public ByteCodeNativeFunction(@NotNull final Method nativeMethod) {
        super(nativeMethod);
    }

    public Method method() {
        return nativeMethod;
    }

}
