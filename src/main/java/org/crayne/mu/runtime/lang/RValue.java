package org.crayne.mu.runtime.lang;

import org.crayne.mu.lang.Datatype;
import org.crayne.mu.runtime.SyntaxTreeExecution;
import org.crayne.mu.runtime.lang.primitive.ROperand;
import org.jetbrains.annotations.NotNull;

public class RValue {

    private final RDatatype type;
    private final Object value;

    public static final RValue TRUE = new RValue(RDatatype.of(Datatype.BOOL), true);
    public static final RValue FALSE = new RValue(RDatatype.of(Datatype.BOOL), false);

    public RValue(@NotNull final RDatatype type, final Object value) {
        this.type = type;
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public RDatatype getType() {
        return type;
    }

    public RValue cast(@NotNull final SyntaxTreeExecution tree, @NotNull final RDatatype cast) {
        return new RValue(cast, tree.getEvaluator().safecast(cast, this));
    }

    public RValue cast(@NotNull final REvaluator evaluator, @NotNull final RDatatype cast) {
        return new RValue(cast, evaluator.safecast(cast, this));
    }

    public RValue equals(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final ROperand operand = ROperand.of(heavier, evaluator.tree);

        if (heavier.primitive()) return operand.equals(cast(evaluator, heavier), y.cast(evaluator, heavier));
        return operand.equals(cast(evaluator, heavier), y);
    }

    public RValue notEquals(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final ROperand operand = ROperand.of(heavier, evaluator.tree);

        if (heavier.primitive()) return operand.notEquals(cast(evaluator, heavier), y.cast(evaluator, heavier));
        return operand.notEquals(cast(evaluator, heavier), y);
    }

    public RValue lessThan(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final ROperand operand = ROperand.of(heavier, evaluator.tree);

        if (heavier.primitive()) return operand.lessThan(cast(evaluator, heavier), y.cast(evaluator, heavier));
        return operand.lessThan(cast(evaluator, heavier), y);
    }

    public RValue greaterThan(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final ROperand operand = ROperand.of(heavier, evaluator.tree);

        if (heavier.primitive()) return operand.greaterThan(cast(evaluator, heavier), y.cast(evaluator, heavier));
        return operand.greaterThan(cast(evaluator, heavier), y);
    }

    public RValue lessThanOrEquals(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final ROperand operand = ROperand.of(heavier, evaluator.tree);

        if (heavier.primitive()) return operand.lessThanOrEquals(cast(evaluator, heavier), y.cast(evaluator, heavier));
        return operand.lessThanOrEquals(cast(evaluator, heavier), y);
    }

    public RValue greaterThanOrEquals(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final ROperand operand = ROperand.of(heavier, evaluator.tree);

        if (heavier.primitive()) return operand.greaterThanOrEquals(cast(evaluator, heavier), y.cast(evaluator, heavier));
        return operand.greaterThanOrEquals(cast(evaluator, heavier), y);
    }

    public RValue add(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final ROperand operand = ROperand.of(heavier, evaluator.tree);

        if (heavier.primitive()) return operand.add(cast(evaluator, heavier), y.cast(evaluator, heavier));
        return operand.add(cast(evaluator, heavier), y);
    }

    public RValue subtract(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final ROperand operand = ROperand.of(heavier, evaluator.tree);

        if (heavier.primitive()) return operand.subtract(cast(evaluator, heavier), y.cast(evaluator, heavier));
        return operand.subtract(cast(evaluator, heavier), y);
    }

    public RValue multiply(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final ROperand operand = ROperand.of(heavier, evaluator.tree);

        if (heavier.primitive()) return operand.multiply(cast(evaluator, heavier), y.cast(evaluator, heavier));
        return operand.multiply(cast(evaluator, heavier), y);
    }

    public RValue divide(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final ROperand operand = ROperand.of(heavier, evaluator.tree);

        if (heavier.primitive()) return operand.divide(cast(evaluator, heavier), y.cast(evaluator, heavier));
        return operand.divide(cast(evaluator, heavier), y);
    }

    public RValue modulus(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final ROperand operand = ROperand.of(heavier, evaluator.tree);

        if (heavier.primitive()) return operand.modulus(cast(evaluator, heavier), y.cast(evaluator, heavier));
        return operand.modulus(cast(evaluator, heavier), y);
    }

    public RValue logicalAnd(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final ROperand operand = ROperand.of(heavier, evaluator.tree);

        if (heavier.primitive()) return operand.logicalAnd(cast(evaluator, heavier), y.cast(evaluator, heavier));
        return operand.logicalAnd(cast(evaluator, heavier), y);
    }

    public RValue logicalOr(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final ROperand operand = ROperand.of(heavier, evaluator.tree);

        if (heavier.primitive()) return operand.logicalOr(cast(evaluator, heavier), y.cast(evaluator, heavier));
        return operand.logicalOr(cast(evaluator, heavier), y);
    }

    public RValue bitXor(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final ROperand operand = ROperand.of(heavier, evaluator.tree);

        if (heavier.primitive()) return operand.bitXor(cast(evaluator, heavier), y.cast(evaluator, heavier));
        return operand.bitXor(cast(evaluator, heavier), y);
    }

    public RValue bitAnd(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final ROperand operand = ROperand.of(heavier, evaluator.tree);

        if (heavier.primitive()) return operand.bitAnd(cast(evaluator, heavier), y.cast(evaluator, heavier));
        return operand.bitAnd(cast(evaluator, heavier), y);
    }

    public RValue bitOr(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final ROperand operand = ROperand.of(heavier, evaluator.tree);

        if (heavier.primitive()) return operand.bitOr(cast(evaluator, heavier), y.cast(evaluator, heavier));
        return operand.bitOr(cast(evaluator, heavier), y);
    }

    public RValue bitShiftLeft(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final ROperand operand = ROperand.of(heavier, evaluator.tree);

        if (heavier.primitive()) return operand.bitShiftLeft(cast(evaluator, heavier), y.cast(evaluator, heavier));
        return operand.bitShiftLeft(cast(evaluator, heavier), y);
    }

    public RValue bitShiftRight(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final ROperand operand = ROperand.of(heavier, evaluator.tree);

        if (heavier.primitive()) return operand.bitShiftRight(cast(evaluator, heavier), y.cast(evaluator, heavier));
        return operand.bitShiftRight(cast(evaluator, heavier), y);
    }

    @Override
    public String toString() {
        return type.getName() + ": " + value;
    }

}
