package org.crayne.mu.bytecode.common;

import org.jetbrains.annotations.NotNull;

public class ByteDatatype {

    public static final ByteDatatype BOOL = new ByteDatatype((byte) 0x00, "bool");
    public static final ByteDatatype CHAR = new ByteDatatype((byte) 0x01, "char");
    public static final ByteDatatype INT = new ByteDatatype((byte) 0x02, "int");
    public static final ByteDatatype LONG = new ByteDatatype((byte) 0x03, "long");
    public static final ByteDatatype FLOAT = new ByteDatatype((byte) 0x04, "float");
    public static final ByteDatatype DOUBLE = new ByteDatatype((byte) 0x05, "double");
    public static final ByteDatatype STRING = new ByteDatatype((byte) 0x06, "string");
    public static final ByteDatatype ENUM = new ByteDatatype((byte) 0x07, "enum");

    private final byte code;
    private final String name;

    public ByteDatatype(final byte code, @NotNull final String name) {
        this.code = code;
        this.name = name;
    }

    public String name() {
        return name;
    }

    public byte code() {
        return code;
    }

    public static ByteDatatype of(@NotNull final String name) {
        return switch (name) {
            case "bool" -> BOOL;
            case "char" -> CHAR;
            case "int" -> INT;
            case "long" -> LONG;
            case "float" -> FLOAT;
            case "double" -> DOUBLE;
            case "string" -> STRING;
            default -> new ByteDatatype(ENUM.code, name);
        };
    }

    public boolean equals(@NotNull final ByteDatatype other) {
        return name.equals(other.name) && code == other.code;
    }

    public String toString() {
        return name + ":" + code;
    }

}
