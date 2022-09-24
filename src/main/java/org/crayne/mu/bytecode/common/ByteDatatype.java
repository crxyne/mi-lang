package org.crayne.mu.bytecode.common;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ByteDatatype {

    public static final ByteDatatype BOOL = new ByteDatatype((byte) 0x00, "bool");
    public static final ByteDatatype CHAR = new ByteDatatype((byte) 0x01, "char");
    public static final ByteDatatype INT = new ByteDatatype((byte) 0x02, "int");
    public static final ByteDatatype LONG = new ByteDatatype((byte) 0x03, "long");
    public static final ByteDatatype FLOAT = new ByteDatatype((byte) 0x04, "float");
    public static final ByteDatatype DOUBLE = new ByteDatatype((byte) 0x05, "double");
    public static final ByteDatatype STRING = new ByteDatatype((byte) 0x06, "string");
    public static final ByteDatatype ENUM = new ByteDatatype((byte) 0x07, "enum");
    public static final ByteDatatype UNKNOWN = new ByteDatatype((byte) -1, "");

    private final byte code;
    private final int id;
    private final String name;

    public ByteDatatype(final byte code, @NotNull final String name) {
        this.code = code;
        this.name = name;
        this.id = -1;
    }

    public ByteDatatype(final byte code, final int id, @NotNull final String name) {
        this.code = code;
        this.name = name;
        this.id = id;
    }

    public String name() {
        return name;
    }

    public byte code() {
        return code;
    }

    public static ByteDatatype of(@NotNull final String name, final int id) {
        return switch (name) {
            case "bool" -> BOOL;
            case "char" -> CHAR;
            case "int" -> INT;
            case "long" -> LONG;
            case "float" -> FLOAT;
            case "double" -> DOUBLE;
            case "string" -> STRING;
            default -> ofEnum(name, id);
        };
    }

    public static ByteDatatype ofEnum(@NotNull final String name, final int id) {
        return new ByteDatatype(ENUM.code, id, name);
    }

    public boolean equals(@NotNull final ByteDatatype other) {
        return name.equals(other.name) && code == other.code && id == other.id;
    }

    public int id() {
        return id;
    }

    public Byte[] bytes() {
        if (id == -1) return new Byte[] {code};
        return new ArrayList<Byte>() {{
            this.add(code);
            this.addAll(List.of(ArrayUtils.toObject(ByteCode.intToBytes(id))));
        }}.toArray(new Byte[0]);
    }

    public String toString() {
        return name + ":" + code;
    }

}
