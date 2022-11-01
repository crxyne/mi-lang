package org.crayne.mi.lang;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
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

    public static final MiDatatype INT = new MiDatatype("int");
    public static final MiDatatype LONG = new MiDatatype("long");
    public static final MiDatatype FLOAT = new MiDatatype("float");
    public static final MiDatatype DOUBLE = new MiDatatype("double");
    public static final MiDatatype STRING = new MiDatatype("string");
    public static final MiDatatype CHAR = new MiDatatype("char");
    public static final MiDatatype BOOL = new MiDatatype("bool");
    public static final MiDatatype VOID = new MiDatatype("void");
    public static final MiDatatype NULL = new MiDatatype("null");

    private static final Map<String, Integer> datatypeRanking = new HashMap<>() {{
        this.put(MiDatatype.NULL.name(), 0);
        this.put(MiDatatype.STRING.name(), 1);
        this.put(MiDatatype.DOUBLE.name(), 3);
        this.put(MiDatatype.FLOAT.name(), 4);
        this.put(MiDatatype.LONG.name(), 5);
        this.put(MiDatatype.INT.name(), 6);
        this.put(MiDatatype.CHAR.name(), 6);
        this.put(MiDatatype.BOOL.name(), 7);
    }};

    public boolean primitive() {
        return datatypeRanking.containsKey(name);
    }

    public static MiDatatype heavier(@NotNull final MiDatatype d1, @NotNull final MiDatatype d2) {
        if ((d1.primitive() && !d2.primitive()) || (!d1.primitive() && d2.primitive())) return null;
        if (d1.name.equals(STRING.name)) return d1;
        if (d2.name.equals(STRING.name)) return d2;

        if (!d1.primitive() || !d2.primitive()) return d2;

        final Integer r1 = datatypeRanking.get(d1.name());
        final Integer r2 = datatypeRanking.get(d2.name());
        if (r1 == null || r2 == null) return null;
        return r1 < r2 ? d1 : d2;
    }

    public static boolean match(@NotNull final MiDatatype newType, @NotNull final MiDatatype oldType) {
        if (newType == oldType) return true;
        if (newType.name.equals(NULL.name) || newType.nullable) return oldType.nullable;

        if ((newType.primitive() && !oldType.primitive()) || (!newType.primitive() && oldType.primitive())) return false;
        if (!oldType.primitive()) return false;

        final Integer newRank = datatypeRanking.get(newType.name());
        final Integer oldRank = datatypeRanking.get(oldType.name());
        if (newRank == null || oldRank == null) return newType.name.equals(oldType.name);
        return newRank.equals(oldRank) || oldRank < newRank;
    }

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
