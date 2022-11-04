package org.crayne.mi.bytecode.communication;

import org.crayne.mi.bytecode.common.ByteCodeException;
import org.jetbrains.annotations.NotNull;

public class MiExecutionException extends ByteCodeException {

    public MiExecutionException(@NotNull final String message) {
        super(message);
    }

}
