package org.crayne.mu.runtime.lang;

import org.crayne.mu.lang.Datatype;
import org.crayne.mu.lang.PrimitiveDatatype;
import org.crayne.mu.parsing.ast.Node;
import org.crayne.mu.parsing.ast.NodeType;
import org.crayne.mu.parsing.lexer.Token;
import org.crayne.mu.parsing.lexer.Tokenizer;
import org.crayne.mu.runtime.SyntaxTreeExecution;
import org.crayne.mu.runtime.util.MuUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class REvaluator {

    protected final SyntaxTreeExecution tree;
    public REvaluator(@NotNull final SyntaxTreeExecution tree) {
        this.tree = tree;
    }

    public RValue evaluateExpression(final Node node) {
        if (node == null || tree.error()) return null;
        tree.traceback(node.lineDebugging());
        if (node.children().size() == 1 && node.type() == NodeType.VALUE && node.child(0).type().getAsDataType() != null) {
            return ofLiteral(node.child(0));
        } else if (node.children().isEmpty() && node.type().getAsDataType() != null) {
            return ofLiteral(node);
        }
        return operator(node.type(), node.children(), node.value());
    }

    public RValue operator(@NotNull final NodeType op, @NotNull final List<Node> values, final Token nodeVal) {
        final Node x = values.size() > 0 ? values.get(0) : null;
        final Node y = values.size() > 1 ? values.get(1) : null;
        return switch (op) {
            case DIVIDE -> divide(x, y);
            case MULTIPLY -> multiply(x, y);
            case ADD -> add(x, y);
            case SUBTRACT -> subtract(x, y);
            case MODULUS -> modulus(x, y);
            case LOGICAL_AND -> logicalAnd(x, y);
            case LOGICAL_OR -> logicalOr(x, y);
            case XOR -> bitXor(x, y);
            case BIT_AND -> bitAnd(x, y);
            case BIT_OR -> bitOr(x, y);
            case LSHIFT -> bitShiftLeft(x, y);
            case RSHIFT -> bitShiftRight(x, y);
            case LESS_THAN -> lessThan(x, y);
            case LESS_THAN_EQ -> lessThanOrEquals(x, y);
            case GREATER_THAN -> greaterThan(x, y);
            case GREATER_THAN_EQ -> greaterThanOrEquals(x, y);
            case EQUALS -> equals(x, y);
            case NOTEQUALS -> notEquals(x, y);
            case NEGATE -> subtract(Node.of(Token.of("0")), x);
            case GET_ENUM_MEMBER -> {
                final String identifier = values.get(0).value().token();
                final String member = values.get(1).value().token();
                yield new RValue(RDatatype.of(identifier), member);
            }
            case TERNARY_OPERATOR -> {
                final boolean ternaryCondition = isTrue(evaluateExpression(values.get(0).child(0)).getValue());
                if (ternaryCondition) yield evaluateExpression(values.get(1).child(0));
                yield evaluateExpression(values.get(2).child(0));
            }
            case IDENTIFIER -> {
                final Optional<RVariable> find = MuUtil.findVariable(tree, nodeVal.token());
                if (find.isEmpty()) {
                    tree.runtimeError("Cannot find variable '" + nodeVal.token() + "'");
                    yield null;
                }
                yield find.get().getValue();
            }
            case CAST_VALUE -> {
                if (x == null) yield null;
                final RDatatype type = RDatatype.of(nodeVal.token());
                final Object cast = safecast(type, evaluateExpression(x));
                yield new RValue(type, cast);
            }
            case VALUE -> operator(values.get(0).type(), values.get(0).children(), values.get(0).value());
            default -> null;
        };
    }

    public RValue multiply(final Node v1, final Node v2) {
        final RValue x = evaluateExpression(v1);
        final RValue y = evaluateExpression(v2);
        return x.multiply(this, y);
    }

    public RValue divide(final Node v1, final Node v2) {
        final RValue x = evaluateExpression(v1);
        final RValue y = evaluateExpression(v2);
        return x.divide(this, y);
    }

    public RValue subtract(final Node v1, final Node v2) {
        final RValue x = evaluateExpression(v1);
        final RValue y = evaluateExpression(v2);
        return x.subtract(this, y);
    }

    public RValue add(final Node v1, final Node v2) {
        final RValue x = evaluateExpression(v1);
        final RValue y = evaluateExpression(v2);
        return x.add(this, y);
    }

    public RValue modulus(final Node v1, final Node v2) {
        final RValue x = evaluateExpression(v1);
        final RValue y = evaluateExpression(v2);
        return x.modulus(this, y);
    }

    public RValue equals(final Node v1, final Node v2) {
        final RValue x = evaluateExpression(v1);
        final RValue y = evaluateExpression(v2);
        return x.equals(this, y);
    }

    public RValue notEquals(final Node v1, final Node v2) {
        final RValue x = evaluateExpression(v1);
        final RValue y = evaluateExpression(v2);
        return x.notEquals(this, y);
    }

    public RValue lessThan(final Node v1, final Node v2) {
        final RValue x = evaluateExpression(v1);
        final RValue y = evaluateExpression(v2);
        return x.lessThan(this, y);
    }

    public RValue greaterThan(final Node v1, final Node v2) {
        final RValue x = evaluateExpression(v1);
        final RValue y = evaluateExpression(v2);
        return x.greaterThan(this, y);
    }

    public RValue lessThanOrEquals(final Node v1, final Node v2) {
        final RValue x = evaluateExpression(v1);
        final RValue y = evaluateExpression(v2);
        return x.lessThanOrEquals(this, y);
    }

    public RValue greaterThanOrEquals(final Node v1, final Node v2) {
        final RValue x = evaluateExpression(v1);
        final RValue y = evaluateExpression(v2);
        return x.greaterThanOrEquals(this, y);
    }

    public RValue logicalAnd(final Node v1, final Node v2) {
        final RValue x = evaluateExpression(v1);
        final RValue y = evaluateExpression(v2);
        return x.logicalAnd(this, y);
    }

    public RValue logicalOr(final Node v1, final Node v2) {
        final RValue x = evaluateExpression(v1);
        final RValue y = evaluateExpression(v2);
        return x.logicalOr(this, y);
    }

    public RValue bitXor(final Node v1, final Node v2) {
        final RValue x = evaluateExpression(v1);
        final RValue y = evaluateExpression(v2);
        return x.bitXor(this, y);
    }

    public RValue bitAnd(final Node v1, final Node v2) {
        final RValue x = evaluateExpression(v1);
        final RValue y = evaluateExpression(v2);
        return x.bitAnd(this, y);
    }

    public RValue bitOr(final Node v1, final Node v2) {
        final RValue x = evaluateExpression(v1);
        final RValue y = evaluateExpression(v2);
        return x.bitOr(this, y);
    }

    public RValue bitShiftLeft(final Node v1, final Node v2) {
        final RValue x = evaluateExpression(v1);
        final RValue y = evaluateExpression(v2);
        return x.bitShiftLeft(this, y);
    }

    public RValue bitShiftRight(final Node v1, final Node v2) {
        final RValue x = evaluateExpression(v1);
        final RValue y = evaluateExpression(v2);
        return x.bitShiftRight(this, y);
    }

    public static RValue concat(@NotNull final RValue x, @NotNull final RValue y) {
        return new RValue(RDatatype.of(Datatype.STRING), "" + x.getValue() + y.getValue());
    }

    public static String withoutQuotes(@NotNull final Object obj) {
        final String str = String.valueOf(obj);
        return str.startsWith("\"") && str.endsWith("\"") ? str.substring(1, str.length() - 1) : str;
    }

    public static RValue ofLiteral(@NotNull final Node literal) {
        return new RValue(RDatatype.of(literal), valueOfLiteral(literal));
    }

    public Object safecast(@NotNull final RDatatype type, @NotNull final String value, @NotNull final RDatatype newType) {
        final Object oldValue = valueOfLiteral(type, value);
        if (type.primitive()) {
            return switch (newType.getPrimitive()) {
                case NULL, VOID -> oldValue;
                case CHAR -> castToChar(type, oldValue);
                case INT -> castToInt(type, oldValue);
                case LONG -> castToLong(type, oldValue);
                case FLOAT -> castToFloat(type, oldValue);
                case DOUBLE -> castToDouble(type, oldValue);
                case STRING -> castToString(type, oldValue);
                case BOOL -> castToBool(type, oldValue);
            };
        }
        final Optional<REnum> findEnum = MuUtil.findEnum(tree, type.getName());
        if (findEnum.isEmpty()) {
            tree.runtimeError("Cannot find enum '" + MuUtil.identOf(type.getName()) + "'");
            return null;
        }
        final REnum found = findEnum.get();
        final String member = String.valueOf(oldValue);
        if (!found.getMembers().contains(member)) {
            tree.runtimeError("Cannot find member '" + member + "' inside of enum '" + MuUtil.identOf(type.getName()) + "'");
            return null;
        }
        final int index = found.getMembers().indexOf(member);
        return switch (newType.getPrimitive()) {
            case INT -> index;
            case DOUBLE -> (double) index;
            case VOID, NULL, STRING -> castToString(type, oldValue);
            case FLOAT -> (float) index;
            case LONG -> (long) index;
            case CHAR -> (char) index;
            case BOOL -> false;
        };
    }

    public Object safecast(@NotNull final RDatatype type, @NotNull final RValue value) {
        return safecast(value.getType(), String.valueOf(value.getValue()), type);
    }

    public static Object castToInt(@NotNull final RDatatype type, @NotNull final Object value) {
        if (type.primitive()) return switch (type.getPrimitive()) {
            case BOOL -> isTrue(value) ? 1 : 0;
            case DOUBLE -> (int) Double.parseDouble(String.valueOf(value));
            case STRING -> nullToZero(Tokenizer.isInt(String.valueOf(value)));
            case FLOAT -> (int) Float.parseFloat(String.valueOf(value));
            case LONG -> (int) Long.parseLong(String.valueOf(value));
            case CHAR -> (int) valueOfLiteral(type, String.valueOf(value));
            default -> value;
        };
        return 0;
    }

    public static Object castToString(@NotNull final RDatatype type, @NotNull final Object value) {
        return type.getPrimitive() == PrimitiveDatatype.STRING ? withoutQuotes(value) : value;
    }

    public static Object castToBool(@NotNull final RDatatype type, @NotNull final Object value) {
        if (type.primitive()) return switch (type.getPrimitive()) {
            case INT -> Integer.parseInt(String.valueOf(value)) == 1;
            case DOUBLE -> Double.parseDouble(String.valueOf(value)) == 1;
            case STRING -> isTrue(String.valueOf(value));
            case FLOAT -> Float.parseFloat(String.valueOf(value)) == 1;
            case LONG -> Long.parseLong(String.valueOf(value)) == 1;
            case CHAR -> (char) valueOfLiteral(type, String.valueOf(value)) == 1;
            default -> value;
        };
        return false;
    }

    public static boolean isTrue(@NotNull final Object obj) {
        return (obj instanceof final Boolean bool && bool) ||
                withoutQuotes(obj).equals("1b") || withoutQuotes(obj).equals("true");
    }

    public static Object castToLong(@NotNull final RDatatype type, @NotNull final Object value) {
        if (type.primitive()) return switch (type.getPrimitive()) {
            case BOOL -> isTrue(value) ? 1L : 0L;
            case DOUBLE -> (long) Double.parseDouble(String.valueOf(value));
            case STRING -> nullToZero(Tokenizer.isLong(String.valueOf(value)));
            case FLOAT -> (long) Float.parseFloat(String.valueOf(value));
            case INT -> (long) Integer.parseInt(String.valueOf(value));
            case CHAR -> (long) valueOfLiteral(type, String.valueOf(value));
            default -> value;
        };
        return 0L;
    }

    public static Object castToDouble(@NotNull final RDatatype type, @NotNull final Object value) {
        if (type.primitive()) return switch (type.getPrimitive()) {
            case BOOL -> isTrue(value) ? 1d : 0d;
            case INT -> (double) Integer.parseInt(String.valueOf(value));
            case STRING -> nullToZero(Tokenizer.isDouble(String.valueOf(value)));
            case FLOAT -> (double) Float.parseFloat(String.valueOf(value));
            case LONG -> (double) Tokenizer.isLong(String.valueOf(value));
            case CHAR -> (double) valueOfLiteral(type, String.valueOf(value));
            default -> value;
        };
        return 0.0;
    }

    public static Object castToFloat(@NotNull final RDatatype type, @NotNull final Object value) {
        if (type.primitive()) return switch (type.getPrimitive()) {
            case BOOL -> isTrue(value) ? 1f : 0f;
            case DOUBLE -> (float) Double.parseDouble(String.valueOf(value));
            case STRING -> nullToZero(Tokenizer.isFloat(String.valueOf(value)));
            case INT -> (float) Integer.parseInt(String.valueOf(value));
            case LONG -> (float) Tokenizer.isLong(String.valueOf(value));
            case CHAR -> (float) valueOfLiteral(type, String.valueOf(value));
            default -> value;
        };
        return 0.0f;
    }

    public static Object castToChar(@NotNull final RDatatype type, @NotNull final Object value) {
        if (type.primitive()) return switch (type.getPrimitive()) {
            case BOOL -> isTrue(value) ? (char) 1 : (char) 0;
            case DOUBLE -> (char) Double.parseDouble(String.valueOf(value));
            case STRING -> nullToZero(Tokenizer.isChar(String.valueOf(value)));
            case FLOAT -> (char) Float.parseFloat(String.valueOf(value));
            case LONG -> (char) nullToZero(Tokenizer.isLong(String.valueOf(value))).longValue();
            case INT -> (char) Integer.parseInt(String.valueOf(value));
            default -> value;
        };
        return (char) 0;
    }

    public static Integer nullToZero(final Integer i) {
        return i == null ? 0 : i;
    }
    public static Long nullToZero(final Long i) {
        return i == null ? 0 : i;
    }
    public static Float nullToZero(final Float i) {
        return i == null ? 0 : i;
    }
    public static Double nullToZero(final Double i) {
        return i == null ? 0 : i;
    }
    public static Character nullToZero(final Character i) {
        return i == null ? 0 : i;
    }

    public static Object valueOfLiteral(@NotNull final RDatatype type, @NotNull final String value) {
        if (type.primitive()) {
            return switch (type.getPrimitive()) {
                case BOOL -> isTrue(value);
                case NULL, VOID -> value;
                case STRING -> withoutQuotes(value);
                case DOUBLE -> Tokenizer.isDouble(value);
                case FLOAT -> Tokenizer.isFloat(value);
                case LONG -> Tokenizer.isLong(value);
                case INT -> Tokenizer.isInt(value);
                case CHAR -> (value.startsWith("'") ? value.charAt(1) : (char) Integer.parseInt(value));
            };
        }
        return value;
    }

    public static Object valueOfLiteral(@NotNull final Node literal) {
        final RDatatype type = RDatatype.of(literal);
        final String value = literal.value().token();
        return valueOfLiteral(type, value);
    }

}
