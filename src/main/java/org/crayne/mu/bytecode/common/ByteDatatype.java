package org.crayne.mu.bytecode.common;

import org.jetbrains.annotations.NotNull;

public enum ByteDatatype {

    BOOL((byte) 0x00),
    CHAR((byte) 0x01),
    INT((byte) 0x02),
    LONG((byte) 0x03),
    FLOAT((byte) 0x04),
    DOUBLE((byte) 0x05),
    STRING((byte) 0x06),
    ENUM((byte) 0x07);

    private final byte code;

    ByteDatatype(final byte code) {
        this.code = code;
    }

    public byte code() {
        return code;
    }

    public static ByteDatatype of(@NotNull final String name) {
        return switch (name) {
            case "bool" -> BOOL;
            case "char" -> CHAR;
            case "long" -> LONG;
            case "float" -> FLOAT;
            case "double" -> DOUBLE;
            case "string" -> STRING;
            default -> ENUM;
        };
    }


}
