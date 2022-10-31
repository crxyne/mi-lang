package org.crayne.mi.lang;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class MiDatatype {

    private final String name;
    private final boolean nullable;

    public MiDatatype(@NotNull final String name) {
        this.name = name;
        this.nullable = false;
    }

    public MiDatatype(@NotNull final String name, final boolean nullable) {
        this.name = name;
        this.nullable = nullable;
    }

    public static MiDatatype of(@NotNull final String name) {
        return new MiDatatype(name);
    }

    public static MiDatatype of(@NotNull final String name, final boolean nullable) {
        return new MiDatatype(name, nullable);
    }

    public static MiDatatype heavier(@NotNull final MiDatatype d1, @NotNull final MiDatatype d2) {
        return d1;
    }

    public static boolean match(@NotNull final MiDatatype d1, @NotNull final MiDatatype d2) {
        return true;
    }

    public static final MiDatatype INT = new MiDatatype("int");
    public static final MiDatatype LONG = new MiDatatype("long");
    public static final MiDatatype FLOAT = new MiDatatype("float");
    public static final MiDatatype DOUBLE = new MiDatatype("double");
    public static final MiDatatype STRING = new MiDatatype("string");
    public static final MiDatatype CHAR = new MiDatatype("char");
    public static final MiDatatype BOOL = new MiDatatype("bool");
    public static final MiDatatype VOID = new MiDatatype("void");
    public static final MiDatatype NULL = new MiDatatype("null");

    public String name() {
        return name;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final MiDatatype that = (MiDatatype) o;
        return nullable == that.nullable && Objects.equals(name, that.name);
    }

    public int hashCode() {
        return Objects.hash(name, nullable);
    }

    @Override
    public String toString() {
        return (nullable ? "nullable" : "nonnull") + " " + name;
    }
}
