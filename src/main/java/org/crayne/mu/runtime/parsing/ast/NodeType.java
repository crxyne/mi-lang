package org.crayne.mu.runtime.parsing.ast;

import org.crayne.mu.lang.Datatype;
import org.crayne.mu.lang.EqualOperation;
import org.crayne.mu.lang.PrimitiveDatatype;
import org.crayne.mu.runtime.parsing.lexer.Token;
import org.crayne.mu.runtime.parsing.lexer.Tokenizer;
import org.crayne.mu.runtime.parsing.parser.Parser;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public enum NodeType {

    //anything without direct value
    PARENT(null),
    SCOPE(null),
    STATEMENT(null),
    NOOP(null),

    STANDARDLIB_MU_FINISH_CODE("STANDARDLIB_MU_FINISH_CODE"),

    //keywords
    LITERAL_INT("int", Datatype.INT),
    LITERAL_DOUBLE("double", Datatype.DOUBLE),
    LITERAL_LONG("long", Datatype.LONG),
    LITERAL_FLOAT("float", Datatype.FLOAT),
    LITERAL_BOOL("bool", Datatype.BOOL),
    LITERAL_STRING("string", Datatype.STRING),
    LITERAL_CHAR("char", Datatype.CHAR),
    LITERAL_VOID("void", Datatype.VOID),
    LITERAL_NULL("null"),
    LITERAL_MODULE("module"),
    LITERAL_PUB("pub"),
    LITERAL_PRIV("priv"),
    LITERAL_PROT("prot"),
    LITERAL_OWN("own"),
    LITERAL_FN("fn"),
    LITERAL_IF("if"),
    LITERAL_ELSE("else"),
    LITERAL_WHILE("while"),
    LITERAL_FOR("for"),
    LITERAL_DO("do"),
    LITERAL_BREAK("break"),
    LITERAL_CONTINUE("continue"),
    LITERAL_MUT("mut"),
    LITERAL_CONST("const"),
    LITERAL_RET("ret"),
    LITERAL_ENUM("enum"),
    LITERAL_USE("use"),
    LITERAL_NAT("nat"),
    LITERAL_INTERN("intern"),

    //statements simplified
    FUNCTION_CALL(null),
    FUNCTION_DEFINITION(null),
    NATIVE_FUNCTION_DEFINITION(null),
    NATIVE_JAVA_FUNCTION_STR(null),
    PARAMETERS(null),
    CREATE_MODULE(null),
    CREATE_ENUM(null),
    ENUM_VALUES(null),
    VAR_DEFINITION(null),
    VAR_SET_VALUE(null),
    VAR_DEF_AND_SET_VALUE(null),
    VALUE(null),
    OPERATOR(null),
    TYPE(null),
    IDENTIFIER(null),
    MEMBER(null),
    MODIFIERS(null),
    IF_STATEMENT(null),
    ELSE_STATEMENT(null),
    DO_STATEMENT(null),
    FOR_STATEMENT(null),
    FOR_FAKE_SCOPE(null),
    CONDITION(null),
    FOR_INSTRUCT(null),
    TERNARY_OPERATOR(null),
    TERNARY_OPERATOR_IF(null),
    TERNARY_OPERATOR_ELSE(null),
    WHILE_STATEMENT(null),
    WHILE_STATEMENT_UNSCOPED(null),
    USE_STATEMENT(null),
    RETURN_VALUE(null),
    CAST_VALUE(null),
    NEGATE(null),
    BOOL_NOT(null),
    INCREMENT(null),
    DECREMENT(null),
    GET_ENUM_MEMBER(null),
    INTEGER_NUM_LITERAL(null, Datatype.INT),
    DOUBLE_NUM_LITERAL(null, Datatype.DOUBLE),
    FLOAT_NUM_LITERAL(null, Datatype.FLOAT),
    LONG_NUM_LITERAL(null, Datatype.LONG),
    BOOL_LITERAL(null, Datatype.BOOL),
    CHAR_LITERAL(null, Datatype.CHAR),
    STRING_LITERAL(null, Datatype.STRING),

    //literals
    ADD("+"),
    MULTIPLY("*"),
    DIVIDE("/"),
    SUBTRACT("-"),
    EXCLAMATION_MARK("!"),
    QUESTION_MARK("?"),
    COLON(":"),
    LBRACE("{"),
    RBRACE("}"),
    LBRACKET("["),
    RBRACKET("]"),
    LPAREN("("),
    RPAREN(")"),
    TILDE("~"),
    BIT_AND("&"),
    BIT_OR("|"),
    LSHIFT("<<"),
    RSHIFT(">>"),
    BECOMES("->"),
    LOGICAL_AND("&&"),
    LOGICAL_OR("||"),
    XOR("^"),
    MODULUS("%"),
    EQUALS("=="),
    NOTEQUALS("!="),
    DOUBLE_COLON("::"),
    LESS_THAN("<"),
    GREATER_THAN(">"),
    LESS_THAN_EQ("<="),
    GREATER_THAN_EQ(">="),
    INCREMENT_LITERAL("++"),
    DECREMENT_LITERAL("--"),
    SET("="),
    SET_ADD("+="),
    SET_MULT("*="),
    SET_DIV("/="),
    SET_SUB("-="),
    SET_MOD("%="),
    SET_AND("&="),
    SET_OR("|="),
    SET_XOR("^="),
    SET_LSHIFT("<<="),
    SET_RSHIFT(">>="),
    COMMA(","),
    SEMI(";");

    private static final Map<String, NodeType> tokenToNode = new HashMap<>() {{
        for (final NodeType type : NodeType.values()) {
            final String asString = type.asString;
            if (asString != null) this.put(asString, type);
        }
    }};

    private final String asString;
    private Datatype type;

    NodeType(final String asString) {
        this.asString = asString;
    }
    NodeType(final String asString, final Datatype datatype) {
        this.asString = asString;
        this.type = datatype;
    }

    public String getAsString() {
        return asString;
    }

    private Datatype getAsDataType() {
        return type;
    }

    public static Datatype getAsDataType(@NotNull final Parser parser, @NotNull final Node node) {
        final Datatype primitive = node.type().getAsDataType();
        if (primitive == null) return new Datatype(parser, node.value());
        return primitive;
    }

    public static NodeType of(@NotNull final PrimitiveDatatype type) {
        return switch (type) {
            case INT -> LITERAL_INT;
            case CHAR -> LITERAL_CHAR;
            case LONG -> LITERAL_LONG;
            case FLOAT -> LITERAL_FLOAT;
            case DOUBLE -> LITERAL_DOUBLE;
            case STRING -> LITERAL_STRING;
            case BOOL -> LITERAL_BOOL;
            case VOID -> LITERAL_VOID;
        };
    }

    public static NodeType of(@NotNull final EqualOperation eq) {
        return switch (eq) {
            case EQUAL -> null;
            case OR -> BIT_OR;
            case ADD -> ADD;
            case AND -> BIT_AND;
            case DIV -> DIVIDE;
            case MOD -> MODULUS;
            case SUB -> SUBTRACT;
            case MULT -> MULTIPLY;
            case SHIFTL -> LSHIFT;
            case SHIFTR -> RSHIFT;
            case XOR -> XOR;
        };
    }

    public boolean isDatatype() {
        return switch (this) {
            case LITERAL_INT, LITERAL_DOUBLE, LITERAL_LONG, LITERAL_FLOAT, LITERAL_BOOL, LITERAL_STRING, LITERAL_CHAR, QUESTION_MARK -> true;
            default -> false;
        };
    }

    public boolean isKeyword() {
        return switch (this) {
            case LITERAL_FN, LITERAL_PUB, LITERAL_RET, LITERAL_BOOL, LITERAL_BREAK,
                    LITERAL_CHAR, LITERAL_CONST, LITERAL_ELSE, LITERAL_CONTINUE,
                    LITERAL_IF, LITERAL_DOUBLE, LITERAL_FLOAT, LITERAL_FOR,
                    LITERAL_INT, LITERAL_NAT, LITERAL_LONG, LITERAL_MODULE,
                    LITERAL_MUT, LITERAL_NULL, LITERAL_PRIV, LITERAL_PROT, LITERAL_USE, LITERAL_OWN,
                    LITERAL_STRING, LITERAL_VOID, LITERAL_WHILE -> true;
            default -> false;
        };
    }

    public boolean isModifier() {
        return switch (this) {
            case LITERAL_PUB, LITERAL_PRIV, LITERAL_OWN, LITERAL_MUT, LITERAL_CONST, LITERAL_PROT, LITERAL_NAT, LITERAL_INTERN -> true;
            default -> false;
        };
    }

    public boolean isVisibilityModifier() {
        return switch (this) {
            case LITERAL_PUB, LITERAL_PRIV, LITERAL_PROT, LITERAL_OWN -> true;
            default -> false;
        };
    }

    public boolean isMutabilityModifier() {
        return switch (this) {
            case LITERAL_MUT, LITERAL_CONST, LITERAL_OWN -> true;
            default -> false;
        };
    }

    public static NodeType of(@NotNull final Token token) {
        return of(token.token());
    }

    public static NodeType of(@NotNull final String token) {
        if (Tokenizer.isBool(token) != null) return BOOL_LITERAL;
        if (Tokenizer.isInt(token) != null) return INTEGER_NUM_LITERAL;
        if (Tokenizer.isLong(token) != null) return LONG_NUM_LITERAL;
        if (Tokenizer.isDouble(token) != null) return DOUBLE_NUM_LITERAL;
        if (Tokenizer.isFloat(token) != null) return FLOAT_NUM_LITERAL;
        if (Tokenizer.isChar(token) != null) return CHAR_LITERAL;
        if (Tokenizer.isString(token) != null) return STRING_LITERAL;

        final NodeType type = tokenToNode.get(token);
        return type == null ? IDENTIFIER : type;
    }

    public String toString() {
        return asString == null ? name() : asString;
    }
}
