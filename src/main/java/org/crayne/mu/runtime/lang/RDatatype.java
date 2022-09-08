package org.crayne.mu.runtime.lang;

import org.crayne.mu.lang.Datatype;
import org.crayne.mu.lang.PrimitiveDatatype;
import org.crayne.mu.parsing.ast.Node;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RDatatype {

    private final String name;

    public RDatatype(@NotNull final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean primitive() {
        return getPrimitive() != null;
    }

    public PrimitiveDatatype getPrimitive() {
        try {
            return PrimitiveDatatype.valueOf(name.toUpperCase());
        } catch (final Exception e) {
            return null;
        }
    }

    public static RDatatype of(@NotNull final Datatype type) {
        return new RDatatype(type.getName());
    }

    public static RDatatype of(@NotNull final Class<?> c) {
        final RDatatype type = switch (c.getName()) {
            case "java.lang.Integer", "java.lang.Short" -> INT;
            case "java.lang.String" -> STRING;
            case "java.lang.Double" -> DOUBLE;
            case "java.lang.Long" -> LONG;
            case "java.lang.Float" -> FLOAT;
            case "java.lang.Boolean" -> BOOL;
            case "java.lang.Character" -> CHAR;
            default -> null;
        };
        if (type == null) throw new IllegalArgumentException("Unknown mu datatype for java object class " + c.getName());
        return type;
    }

    public static RDatatype of(@NotNull final String s) {
        return new RDatatype(s);
    }

    public static RDatatype of(@NotNull final Node node) {
        return RDatatype.of(node.type().getAsDataType() != null ? node.type().getAsDataType().getName() : node.value().token());
    }

    private static final Map<String, Integer> datatypeRanking = new HashMap<>() {{
        this.put(PrimitiveDatatype.NULL.name(), 0);
        this.put(PrimitiveDatatype.DOUBLE.name(), 3);
        this.put(PrimitiveDatatype.FLOAT.name(), 4);
        this.put(PrimitiveDatatype.LONG.name(), 5);
        this.put(PrimitiveDatatype.INT.name(), 6);
        this.put(PrimitiveDatatype.CHAR.name(), 6);
        this.put(PrimitiveDatatype.BOOL.name(), 7);
        this.put(PrimitiveDatatype.STRING.name(), 8);
    }};

    public static final RDatatype BOOL = RDatatype.of(Datatype.BOOL);
    public static final RDatatype STRING = RDatatype.of(Datatype.STRING);
    public static final RDatatype DOUBLE = RDatatype.of(Datatype.DOUBLE);
    public static final RDatatype FLOAT = RDatatype.of(Datatype.FLOAT);
    public static final RDatatype LONG = RDatatype.of(Datatype.LONG);
    public static final RDatatype INT = RDatatype.of(Datatype.INT);
    public static final RDatatype CHAR = RDatatype.of(Datatype.CHAR);

    public static Optional<RDatatype> getHeavierType(@NotNull final RDatatype d1, @NotNull final RDatatype d2) {
        if ((d1.primitive() && !d2.primitive()) || (!d1.primitive() && d2.primitive())) return Optional.empty();
        if (d1.getPrimitive() == PrimitiveDatatype.STRING) return Optional.of(d1);
        if (d2.getPrimitive() == PrimitiveDatatype.STRING) return Optional.of(d2);

        if (d1.getPrimitive() == null || d2.getPrimitive() == null) return Optional.of(d1);

        final Integer r1 = datatypeRanking.get(d1.getName().toUpperCase());
        final Integer r2 = datatypeRanking.get(d2.getName().toUpperCase());
        if (r1 == null || r2 == null) return Optional.empty();
        return Optional.of(r1 < r2 ? d1 : d2);
    }

    public boolean equals(final RDatatype other) {
        return name.equals(other.name);
    }

    public boolean equals(final Datatype other) {
        return name.equals(other.getName());
    }

    public String toString() {
        return name;
    }

}
