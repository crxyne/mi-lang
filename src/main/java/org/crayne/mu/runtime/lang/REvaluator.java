package org.crayne.mu.runtime.lang;

import org.crayne.mu.lang.Datatype;
import org.crayne.mu.lang.PrimitiveDatatype;
import org.crayne.mu.parsing.ast.Node;
import org.crayne.mu.parsing.ast.NodeType;
import org.crayne.mu.parsing.lexer.Tokenizer;
import org.crayne.mu.runtime.SyntaxTree;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class REvaluator {

    private final SyntaxTree tree;
    public REvaluator(@NotNull final SyntaxTree tree) {
        this.tree = tree;
    }

    public RValue evaluateExpression(@NotNull final Node node) {
        if (node.children().size() == 1 && node.child(0).type().getAsDataType() != null) {
            return ofLiteral(node.child(0));
        } else if (node.children().isEmpty() && node.type().getAsDataType() != null) {
            return ofLiteral(node);
        }
        return operator(node.type(), node.children());
    }

    public RValue operator(@NotNull final NodeType op, @NotNull final List<Node> values) {
        return switch (op) {
            case DIVIDE -> divide(values.get(0), values.get(1));
            case MULTIPLY -> multiply(values.get(0), values.get(1));
            case ADD -> add(values.get(0), values.get(1));
            case SUBTRACT -> subtract(values.get(0), values.get(1));
            case VALUE -> operator(values.get(0).type(), values.get(0).children());
            default -> null;
        };
    }

    public RValue divide(@NotNull final Node v1, @NotNull final Node v2) {
        final RValue x = evaluateExpression(v1);
        final RValue y = evaluateExpression(v2);
        final String value = String.valueOf(toDouble(x) / toDouble(y));
        final RDatatype resultType = heavierSafe(x, y);

        return new RValue(resultType, castValue(RDatatype.of(Datatype.DOUBLE), value, resultType));
    }

    public RValue multiply(@NotNull final Node v1, @NotNull final Node v2) {
        final RValue x = evaluateExpression(v1);
        final RValue y = evaluateExpression(v2);
        final String value = String.valueOf(toDouble(x) * toDouble(y));
        final RDatatype resultType = heavierSafe(x, y);

        return new RValue(resultType, castValue(RDatatype.of(Datatype.DOUBLE), value, resultType));
    }

    public RValue add(@NotNull final Node v1, @NotNull final Node v2) {
        final RValue x = evaluateExpression(v1);
        final RValue y = evaluateExpression(v2);
        if (x.getType().primitive() && x.getType().getPrimitive() == PrimitiveDatatype.STRING) {
            return new RValue(RDatatype.of(Datatype.STRING), "" + x.getValue() + y.getValue());
        }
        final String value = String.valueOf(toDouble(x) + toDouble(y));
        final RDatatype resultType = heavierSafe(x, y);

        return new RValue(resultType, castValue(RDatatype.of(Datatype.DOUBLE), value, resultType));
    }

    public RValue subtract(@NotNull final Node v1, @NotNull final Node v2) {
        final RValue x = evaluateExpression(v1);
        final RValue y = evaluateExpression(v2);
        final String value = String.valueOf(toDouble(x) - toDouble(y));
        final RDatatype resultType = heavierSafe(x, y);

        return new RValue(resultType, castValue(RDatatype.of(Datatype.DOUBLE), value, resultType));
    }

    public static Optional<RDatatype> heavier(@NotNull final RValue x, @NotNull final RValue y) {
        return Optional.ofNullable(RDatatype.getHeavierType(x.getType(), y.getType()));
    }

    public RDatatype heavierSafe(@NotNull final RValue x, @NotNull final RValue y) {
        final Optional<RDatatype> resultType = heavier(x, y);
        if (resultType.isEmpty()) {
            tree.runtimeError("Result type of expression is unknown");
            return null;
        }
        return resultType.get();
    }

    public static Double toDouble(@NotNull final RValue v) {
        return Double.parseDouble(String.valueOf(v.getValue()));
    }

    public static RValue ofLiteral(@NotNull final Node literal) {
        return new RValue(RDatatype.of(literal), valueOfLiteral(literal));
    }

    public static Object castValue(@NotNull final RDatatype type, @NotNull final String value, @NotNull final RDatatype newType) {
        final Object oldValue = valueOfLiteral(type, value);
        if (type.primitive()) {
            return switch (newType.getPrimitive()) {
                case NULL, VOID -> oldValue;
                case CHAR -> (Character) oldValue;
                case INT -> castToInt(type, oldValue);
                case LONG -> (Long) oldValue;
                case FLOAT -> (Float) oldValue;
                case DOUBLE -> (Double) oldValue;
                case STRING -> String.valueOf(oldValue);
                case BOOL -> (Boolean) oldValue;
            };
        }
        return "" + oldValue;
    }

    public static Object castToInt(@NotNull final RDatatype type, @NotNull final Object value) {
        return switch (type.getPrimitive()) {
            case BOOL -> String.valueOf(value).equals("1b") ? 1 : 0;
            case DOUBLE -> (int) Double.parseDouble(String.valueOf(value));
            case STRING -> nullToZero(Tokenizer.isInt(String.valueOf(value)));
            case FLOAT -> (int) Float.parseFloat(String.valueOf(value));
            case LONG -> (int) Long.parseLong(String.valueOf(value));
            case CHAR -> (int) valueOfLiteral(type, String.valueOf(value));
            default -> value;
        };
    }

    public static Integer nullToZero(final Integer i) {
        return i == null ? 0 : i;
    }

    public static Object valueOfLiteral(@NotNull final RDatatype type, @NotNull final String value) {
        if (type.primitive()) {
            return switch (type.getPrimitive()) {
                case BOOL -> value.equals("1b");
                case STRING, NULL, VOID -> value;
                case DOUBLE -> Double.valueOf(value);
                case FLOAT -> Float.valueOf(value);
                case LONG -> Long.valueOf(value);
                case INT -> Integer.valueOf(value);
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
