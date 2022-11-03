package org.crayne.mi.parsing.ast;

import org.crayne.mi.lang.MiDatatype;
import org.crayne.mi.parsing.lexer.Tokenizer;
import org.crayne.mi.parsing.lexer.Token;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public enum NodeType {

    //anything without direct value
    PARENT(null),
    SCOPE(null),
    STATEMENT(null),
    NOOP(null),

    STANDARDLIB_MI_FINISH_CODE("STANDARDLIB_MI_FINISH_CODE"),

    //keywords
    LITERAL_INT("int", MiDatatype.INT),
    LITERAL_DOUBLE("double", MiDatatype.DOUBLE),
    LITERAL_LONG("long", MiDatatype.LONG),
    SHORT_UNUSED("short"),
    BYTE_UNUSED("byte"),
    GOTO_UNUSED("goto"),
    CLASS_UNUSED("class"),
    LITERAL_FLOAT("float", MiDatatype.FLOAT),
    LITERAL_BOOL("bool", MiDatatype.BOOL),
    LITERAL_STRING("string", MiDatatype.STRING),
    LITERAL_CHAR("char", MiDatatype.CHAR),
    LITERAL_VOID("void", MiDatatype.VOID),
    LITERAL_NULL("null", MiDatatype.NULL),
    LITERAL_MODULE("mod"),
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
    LITERAL_RETURN("return"),
    LITERAL_ENUM("enum"),
    LITERAL_USE("use"),
    LITERAL_NAT("nat"),
    LITERAL_INTERN("intern"),
    LITERAL_STRUCT("struct"),
    LITERAL_NEW("new"),
    TRY_UNUSED("try"),
    CATCH_UNUSED("catch"),
    LITERAL_MACRO("macro"),
    LITERAL_IMPL("impl"),
    LITERAL_ASSERT("assert"),
    LITERAL_TYPEDEF("typedef"),
    LITERAL_NULLABLE("nullable"),
    LITERAL_NONNULL("nonnull"),

    //statements simplified
    FUNCTION_CALL(null),
    FUNCTION_DEFINITION(null),
    NATIVE_FUNCTION_DEFINITION(null),
    NATIVE_JAVA_FUNCTION_STR(null),
    PARAMETERS(null),
    PARAMETER(null),
    CREATE_MODULE(null),
    CREATE_ENUM(null),
    ENUM_VALUES(null),
    DECLARE_VARIABLE(null),
    MUTATE_VARIABLE(null),
    DEFINE_VARIABLE(null),
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
    BREAK_STATEMENT(null),
    CONTINUE_STATEMENT(null),
    USE_STATEMENT(null),
    RETURN_STATEMENT(null),
    CAST_VALUE(null),
    CREATE_STRUCT(null),
    STRUCT_CONSTRUCT(null),
    NEGATE(null),
    BOOL_NOT(null),
    INCREMENT(null),
    DECREMENT(null),
    GET_ENUM_MEMBER(null),
    INTEGER_NUM_LITERAL(null, MiDatatype.INT),
    DOUBLE_NUM_LITERAL(null, MiDatatype.DOUBLE),
    FLOAT_NUM_LITERAL(null, MiDatatype.FLOAT),
    LONG_NUM_LITERAL(null, MiDatatype.LONG),
    BOOL_LITERAL(null, MiDatatype.BOOL),
    CHAR_LITERAL(null, MiDatatype.CHAR),
    STRING_LITERAL(null, MiDatatype.STRING),

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
    BIT_NOT(null),
    BIT_AND("&"),
    BIT_OR("|"),
    LSHIFT("<<"),
    RSHIFT(">>"),
    ARROW("->"),
    DOUBLE_ARROW("=>"),
    DOUBLE_DOT(".."),
    TRIPLE_DOT("..."),
    HASHTAG("#"),
    DOLLAR("$"),
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
    private MiDatatype type;

    NodeType(final String asString) {
        this.asString = asString;
    }
    NodeType(final String asString, final MiDatatype miDatatype) {
        this.asString = asString;
        this.type = miDatatype;
    }

    public String getAsString() {
        return asString;
    }

    public MiDatatype getAsDataType() {
        return type;
    }

    public static MiDatatype getAsDataType(@NotNull final Node node) {
        final MiDatatype primitive = node.type().getAsDataType();

        if (primitive == null) return new MiDatatype(node.value().token());
        return primitive;
    }

    public boolean isDatatype() {
        return switch (this) {
            case LITERAL_INT, LITERAL_DOUBLE, LITERAL_LONG, LITERAL_FLOAT, LITERAL_BOOL, LITERAL_STRING, LITERAL_CHAR, QUESTION_MARK -> true;
            default -> false;
        };
    }

    public boolean isKeyword() {
        return switch (this) {
            case LITERAL_FN, LITERAL_PUB, LITERAL_RETURN, LITERAL_BOOL, LITERAL_BREAK,
                    LITERAL_CHAR, LITERAL_CONST, LITERAL_ELSE, LITERAL_CONTINUE,
                    LITERAL_IF, LITERAL_DOUBLE, LITERAL_FLOAT, LITERAL_FOR,
                    LITERAL_INT, LITERAL_NAT, LITERAL_LONG, LITERAL_MODULE,
                    LITERAL_MUT, LITERAL_NULL, LITERAL_PRIV, LITERAL_PROT, LITERAL_USE, LITERAL_OWN,
                    LITERAL_STRING, LITERAL_VOID, LITERAL_WHILE -> true;
            default -> false;
        };
    }

    public boolean incrementDecrement() {
        return switch (this) {
            case INCREMENT, INCREMENT_LITERAL, DECREMENT, DECREMENT_LITERAL -> true;
            default -> false;
        };
    }

    public boolean isModifier() {
        return switch (this) {
            case LITERAL_PUB, LITERAL_PRIV, LITERAL_OWN, LITERAL_MUT, LITERAL_CONST, LITERAL_PROT, LITERAL_NAT, LITERAL_INTERN, LITERAL_NULLABLE, LITERAL_NONNULL -> true;
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
