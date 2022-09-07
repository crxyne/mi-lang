package org.crayne.mu.runtime.lang;

import org.crayne.mu.lang.Datatype;
import org.crayne.mu.lang.PrimitiveDatatype;
import org.crayne.mu.parsing.ast.Node;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

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

    public static RDatatype of(@NotNull final String s) {
        return new RDatatype(s);
    }

    public static RDatatype of(@NotNull final Node node) {
        return RDatatype.of(node.type().getAsDataType() != null ? node.type().getAsDataType().getName() : node.value().token());
    }

    private static final Map<String, Integer> datatypeRanking = new HashMap<>() {{
        this.put(PrimitiveDatatype.NULL.name(), 0);
        this.put(PrimitiveDatatype.BOOL.name(), 1);
        this.put(PrimitiveDatatype.STRING.name(), 2);
        this.put(PrimitiveDatatype.DOUBLE.name(), 3);
        this.put(PrimitiveDatatype.FLOAT.name(), 4);
        this.put(PrimitiveDatatype.LONG.name(), 5);
        this.put(PrimitiveDatatype.INT.name(), 6);
        this.put(PrimitiveDatatype.CHAR.name(), 6);
    }};

    public static RDatatype getHeavierType(@NotNull final RDatatype d1, @NotNull final RDatatype d2) {
        if ((d1.primitive() && !d2.primitive()) || (!d1.primitive() && d2.primitive())) return null;
        if (d1.getPrimitive() == null || d2.getPrimitive() == null) return d2;

        final Integer r1 = datatypeRanking.get(d1.getName().toUpperCase());
        final Integer r2 = datatypeRanking.get(d2.getName().toUpperCase());
        if (r1 == null || r2 == null) return null;
        return r1 < r2 ? d1 : d2;
    }

    @Override
    public String toString() {
        return "RDatatype{" +
                "name='" + name + '\'' +
                '}';
    }

}
