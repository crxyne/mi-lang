package org.crayne.mi.lang;

import org.crayne.mi.parsing.ast.NodeType;
import org.crayne.mi.parsing.lexer.Token;
import org.crayne.mi.parsing.parser.IdentifierType;
import org.crayne.mi.parsing.parser.Parser;
import org.crayne.mi.parsing.parser.ParserEvaluator;
import org.crayne.mi.parsing.parser.scope.FunctionScope;
import org.crayne.mi.parsing.parser.scope.Scope;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Datatype {

    private final boolean primitive;
    private final PrimitiveDatatype primitiveDatatype;
    private final Enum enumDatatype;
    private final boolean nullable;

    public static final Datatype BOOL = new Datatype(PrimitiveDatatype.BOOL, false);
    public static final Datatype STRING = new Datatype(PrimitiveDatatype.STRING, false);
    public static final Datatype DOUBLE = new Datatype(PrimitiveDatatype.DOUBLE, false);
    public static final Datatype FLOAT = new Datatype(PrimitiveDatatype.FLOAT, false);
    public static final Datatype LONG = new Datatype(PrimitiveDatatype.LONG, false);
    public static final Datatype INT = new Datatype(PrimitiveDatatype.INT, false);
    public static final Datatype CHAR = new Datatype(PrimitiveDatatype.CHAR, false);
    public static final Datatype VOID = new Datatype(PrimitiveDatatype.VOID, false);
    public static final Datatype NULL = new Datatype(PrimitiveDatatype.NULL, true);


    public Datatype(@NotNull final PrimitiveDatatype primitiveDatatype, final boolean nullable) {
        primitive = true;
        this.primitiveDatatype = primitiveDatatype;
        enumDatatype = null;
        this.nullable = nullable;
    }

    public Datatype(@NotNull final Enum enumDatatype, final boolean nullable) {
        primitive = false;
        primitiveDatatype = null;
        this.enumDatatype = enumDatatype;
        this.nullable = nullable;
    }

    public PrimitiveDatatype getPrimitive() {
        return primitiveDatatype;
    }

    public static Enum findEnumByIdentifier(@NotNull final Parser parser, @NotNull final List<String> usingMods, @NotNull final Token identifier, final boolean panic) {
        final String enumNameStr = identifier.token();
        final Optional<Module> module = parser.findModuleFromIdentifier(enumNameStr, identifier, panic);
        for (final String using : usingMods) {
            final Token findOther = new Token(using + "." + identifier.token(), identifier.actualLine(), identifier.line(), identifier.column());

            final Enum findUsing = findEnumByIdentifier(parser, Collections.emptyList(),
                    findOther, false
            );
            if (findUsing != null) return findUsing;
        }
        if (module.isEmpty()) return null;
        final String enumIdent = ParserEvaluator.identOf(enumNameStr);

        Enum foundEnum = module.get().findEnumByName(enumIdent)
                .orElse(parser.parentModule().findEnumByName(enumIdent).orElse(null));

        if (foundEnum == null && parser.currentParsingModule() != null) foundEnum = parser.currentParsingModule().findEnumByName(enumIdent).orElse(null);

        if (foundEnum == null) {
            if (panic) parser.parserError("Cannot find enum '" + enumNameStr + "'", identifier);
            return null;
        }

        parser.checkAccessValidity(module.get(), IdentifierType.ENUM, identifier, foundEnum.modifiers());
        return foundEnum;
    }

    public Datatype(@NotNull final Parser parser, @NotNull final Token enumDatatype, final boolean nullable) {
        primitive = false;
        primitiveDatatype = null;
        this.nullable = nullable;
        final Optional<Scope> currentScope = parser.scope();
        if (currentScope.isPresent() && currentScope.get() instanceof final FunctionScope functionScope) {
            this.enumDatatype = findEnumByIdentifier(parser, functionScope.using(), enumDatatype, true);
            return;
        }
        this.enumDatatype = findEnumByIdentifier(parser, Collections.emptyList(), enumDatatype, true);
    }

    public Datatype(@NotNull final Parser parser, @NotNull final List<String> usingMods, @NotNull final Token enumDatatype, final boolean nullable) {
        primitive = false;
        primitiveDatatype = null;
        this.nullable = nullable;
        this.enumDatatype = findEnumByIdentifier(parser, usingMods, enumDatatype, true);
    }

    public boolean valid() {
        return primitiveDatatype != null || enumDatatype != null;
    }

    public boolean notPrimitive() {
        return !primitive;
    }

    public boolean primitive() {
        return primitive;
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

    public static Datatype getHeavierType(@NotNull final Datatype d1, @NotNull final Datatype d2) {
        if ((d1.primitive && !d2.primitive) || (!d1.primitive && d2.primitive)) return null;
        if (d1.primitiveDatatype == PrimitiveDatatype.STRING) return d1;
        if (d2.primitiveDatatype == PrimitiveDatatype.STRING) return d2;

        if (d1.primitiveDatatype == null || d2.primitiveDatatype == null) return d2;

        final Integer r1 = datatypeRanking.get(d1.primitiveDatatype.name());
        final Integer r2 = datatypeRanking.get(d2.primitiveDatatype.name());
        if (r1 == null || r2 == null) return null;
        return r1 < r2 ? d1 : d2;
    }

    public static boolean equal(@NotNull final Datatype newType, @NotNull final Datatype oldType) {
        if (newType == oldType) return true;
        if (newType.primitiveDatatype == PrimitiveDatatype.NULL || newType.nullable) return oldType.nullable;

        if ((newType.primitive && !oldType.primitive) || (!newType.primitive && oldType.primitive)) return false;
        if (newType.primitiveDatatype == null) return newType.enumDatatype.equals(oldType.enumDatatype);
        if (oldType.primitiveDatatype == null) return false;

        final Integer newRank = datatypeRanking.get(newType.primitiveDatatype.name());
        final Integer oldRank = datatypeRanking.get(oldType.primitiveDatatype.name());
        if (newRank == null || oldRank == null) return false;
        return newRank.equals(oldRank) || oldRank < newRank;
    }

    public boolean nullable() {
        return nullable;
    }

    public String getName() {
        return primitiveDatatype != null ? primitiveDatatype.name().toLowerCase() : enumDatatype != null ? enumDatatype.asIdentifierToken().token() : null;
    }

    public Datatype heavier(@NotNull final Datatype d) {
        return getHeavierType(this, d);
    }

    public static Datatype of(@NotNull final Parser parser, @NotNull final Token tok, final boolean nullable) {
        if (NodeType.of(tok) == NodeType.IDENTIFIER) return new Datatype(parser, tok, nullable);
        final PrimitiveDatatype primitive = PrimitiveDatatype.of(tok);
        if (primitive == null) return null;
        return new Datatype(primitive, nullable);
    }

    public static Datatype of(@NotNull final Parser parser, @NotNull final List<String> usingMods, @NotNull final Token tok, final boolean nullable) {
        if (NodeType.of(tok) == NodeType.IDENTIFIER) return new Datatype(parser, usingMods, tok, nullable);
        final PrimitiveDatatype primitive = PrimitiveDatatype.of(tok);
        if (primitive == null) return null;
        return new Datatype(primitive, nullable);
    }

    public boolean equals(@NotNull final Datatype y) {
        return (nullable && (y.getName().equals("null") || y.nullable)) || (nullable == y.nullable && getName().equals(y.getName()));
    }

    public boolean operatorDefined(final NodeType op, final Datatype y) {
        return (enumDatatype != null && enumDatatype.equals(y.enumDatatype) && (op == NodeType.EQUALS || op == NodeType.NOTEQUALS)) ||
                (primitive && primitiveDatatype != null && primitiveDatatype.operatorDefined(op, y.primitive && y.primitiveDatatype != null ? y.primitiveDatatype : null));
    }

    public String toString() {
        return (primitiveDatatype != PrimitiveDatatype.NULL && primitiveDatatype != PrimitiveDatatype.VOID ? (nullable ? "nullable" : "nonnull") + " " : "")
                + (primitiveDatatype != null ? primitiveDatatype.name().toLowerCase() : enumDatatype != null ? enumDatatype.name() : null);
    }
}
