package org.crayne.mi.bytecode.common;

import org.jetbrains.annotations.NotNull;

public class ByteCodeException extends RuntimeException {

    public ByteCodeException(@NotNull final String message) {
        super(message);
    }

}
