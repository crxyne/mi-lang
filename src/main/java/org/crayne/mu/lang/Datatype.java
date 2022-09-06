package org.crayne.mu.lang;

import org.crayne.mu.runtime.parsing.ast.NodeType;
import org.crayne.mu.runtime.parsing.lexer.Token;
import org.crayne.mu.runtime.parsing.parser.ParserEvaluator;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class Datatype {

    private final boolean primitive;
    private final PrimitiveDatatype primitiveDatatype;
    private final String enumDatatype;

    public static final Datatype BOOL = new Datatype(PrimitiveDatatype.BOOL);
    public static final Datatype STRING = new Datatype(PrimitiveDatatype.STRING);
    public static final Datatype DOUBLE = new Datatype(PrimitiveDatatype.DOUBLE);
    public static final Datatype FLOAT = new Datatype(PrimitiveDatatype.FLOAT);
    public static final Datatype LONG = new Datatype(PrimitiveDatatype.LONG);
    public static final Datatype INT = new Datatype(PrimitiveDatatype.INT);
    public static final Datatype CHAR = new Datatype(PrimitiveDatatype.CHAR);
    public static final Datatype VOID = new Datatype(PrimitiveDatatype.VOID);


    public Datatype(@NotNull final PrimitiveDatatype primitiveDatatype) {
        primitive = true;
        this.primitiveDatatype = primitiveDatatype;
        enumDatatype = null;
    }

    public Datatype(@NotNull final String enumDatatype) {
        primitive = true;
        primitiveDatatype = null;
        this.enumDatatype = enumDatatype;
    }

    private static final Map<String, Integer> datatypeRanking = new HashMap<>() {{
        this.put(PrimitiveDatatype.BOOL.name(), 1);
        this.put(PrimitiveDatatype.STRING.name(), 2);
        this.put(PrimitiveDatatype.DOUBLE.name(), 3);
        this.put(PrimitiveDatatype.FLOAT.name(), 4);
        this.put(PrimitiveDatatype.LONG.name(), 5);
        this.put(PrimitiveDatatype.INT.name(), 6);
        this.put(PrimitiveDatatype.CHAR.name(), 6);
    }};

    public static Datatype getHeavierType(@NotNull final Datatype d1, @NotNull final Datatype d2) {
        if ((d1.primitive && !d2.primitive) || (!d1.primitive && d2.primitive)) return null;
        if (d1.primitiveDatatype == null || d2.primitiveDatatype == null) return d2;

        final Integer r1 = datatypeRanking.get(d1.primitiveDatatype.name());
        final Integer r2 = datatypeRanking.get(d2.primitiveDatatype.name());
        if (r1 == null || r2 == null) return null;
        return r1 < r2 ? d1 : d2;
    }

    public static boolean equal(@NotNull final Datatype newType, @NotNull final Datatype oldType) {
        if (newType == oldType) return true;
        if ((newType.primitive && !oldType.primitive) || (!newType.primitive && oldType.primitive)) return false;
        if (newType.primitiveDatatype == null) return ParserEvaluator.identOf(newType.enumDatatype).equals(ParserEvaluator.identOf(oldType.enumDatatype));
        if (oldType.primitiveDatatype == null) return false;

        final Integer newRank = datatypeRanking.get(newType.primitiveDatatype.name());
        final Integer oldRank = datatypeRanking.get(oldType.primitiveDatatype.name());
        if (newRank == null || oldRank == null) return false;
        return newRank.equals(oldRank) || oldRank < newRank;
    }

    public String getName() {
        return primitiveDatatype != null ? primitiveDatatype.getName() : enumDatatype;
    }

    public Datatype heavier(@NotNull final Datatype d) {
        return getHeavierType(this, d);
    }

    public static Datatype of(@NotNull final Token tok) {
        if (NodeType.of(tok) == NodeType.IDENTIFIER) return new Datatype(tok.token());
        final PrimitiveDatatype primitive = PrimitiveDatatype.of(tok);
        if (primitive == null) return null;
        return new Datatype(primitive);
    }

    public boolean equals(@NotNull final Datatype y) {
        return equal(this, y);
    }

    public boolean operatorDefined(final NodeType op, final Datatype y) throws Exception {
        return primitive && primitiveDatatype != null && primitiveDatatype.operatorDefined(op, y.primitive && y.primitiveDatatype != null ? y.primitiveDatatype : null);
    }

    public String toString() {
        return primitiveDatatype != null ? primitiveDatatype.getName() : enumDatatype;
    }
}
