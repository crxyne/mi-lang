package org.crayne.mi.bytecode.reader;

import org.jetbrains.annotations.NotNull;

public class ByteCodeReaderException extends RuntimeException {

    public ByteCodeReaderException(@NotNull final String message) {
        super(message);
    }

}
