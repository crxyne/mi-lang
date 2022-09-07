package org.crayne.mu.runtime.lang;

import org.crayne.mu.lang.Datatype;
import org.crayne.mu.runtime.lang.primitive.RPrimitiveType;
import org.jetbrains.annotations.NotNull;

public class RValue {

    private final RDatatype type;
    private final Object value;

    public static final RValue TRUE = new RValue(RDatatype.of(Datatype.BOOL), true);
    public static final RValue FALSE = new RValue(RDatatype.of(Datatype.BOOL), false);

    public RValue(@NotNull final RDatatype type, @NotNull final Object value) {
        this.type = type;
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public RDatatype getType() {
        return type;
    }

    public RValue cast(@NotNull final RDatatype cast) {
        return new RValue(cast, REvaluator.safecast(cast, this));
    }

    public RValue equals(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final RPrimitiveType primitiveType = RPrimitiveType.of(heavier, evaluator.tree);

        if (heavier.primitive() && primitiveType != null) return primitiveType.equals(cast(heavier), y.cast(heavier));
        return null;
    }

    public RValue notEquals(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final RPrimitiveType primitiveType = RPrimitiveType.of(heavier, evaluator.tree);

        if (heavier.primitive() && primitiveType != null) return primitiveType.notEquals(cast(heavier), y.cast(heavier));
        return null;
    }

    public RValue lessThan(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final RPrimitiveType primitiveType = RPrimitiveType.of(heavier, evaluator.tree);

        if (heavier.primitive() && primitiveType != null) return primitiveType.lessThan(cast(heavier), y.cast(heavier));
        return null;
    }

    public RValue greaterThan(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final RPrimitiveType primitiveType = RPrimitiveType.of(heavier, evaluator.tree);

        if (heavier.primitive() && primitiveType != null) return primitiveType.greaterThan(cast(heavier), y.cast(heavier));
        return null;
    }

    public RValue lessThanOrEquals(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final RPrimitiveType primitiveType = RPrimitiveType.of(heavier, evaluator.tree);

        if (heavier.primitive() && primitiveType != null) return primitiveType.lessThanOrEquals(cast(heavier), y.cast(heavier));
        return null;
    }

    public RValue greaterThanOrEquals(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final RPrimitiveType primitiveType = RPrimitiveType.of(heavier, evaluator.tree);

        if (heavier.primitive() && primitiveType != null) return primitiveType.greaterThanOrEquals(cast(heavier), y.cast(heavier));
        return null;
    }

    public RValue add(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final RPrimitiveType primitiveType = RPrimitiveType.of(heavier, evaluator.tree);

        if (heavier.primitive() && primitiveType != null) return primitiveType.add(cast(heavier), y.cast(heavier));
        return null;
    }

    public RValue subtract(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final RPrimitiveType primitiveType = RPrimitiveType.of(heavier, evaluator.tree);

        if (heavier.primitive() && primitiveType != null) return primitiveType.subtract(cast(heavier), y.cast(heavier));
        return null;
    }

    public RValue multiply(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final RPrimitiveType primitiveType = RPrimitiveType.of(heavier, evaluator.tree);

        if (heavier.primitive() && primitiveType != null) return primitiveType.multiply(cast(heavier), y.cast(heavier));
        return null;
    }

    public RValue divide(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final RPrimitiveType primitiveType = RPrimitiveType.of(heavier, evaluator.tree);

        if (heavier.primitive() && primitiveType != null) return primitiveType.divide(cast(heavier), y.cast(heavier));
        return null;
    }

    public RValue modulus(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final RPrimitiveType primitiveType = RPrimitiveType.of(heavier, evaluator.tree);

        if (heavier.primitive() && primitiveType != null) return primitiveType.modulus(cast(heavier), y.cast(heavier));
        return null;
    }

    public RValue logicalAnd(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final RPrimitiveType primitiveType = RPrimitiveType.of(heavier, evaluator.tree);

        if (heavier.primitive() && primitiveType != null) return primitiveType.logicalAnd(cast(heavier), y.cast(heavier));
        return null;
    }

    public RValue logicalOr(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final RPrimitiveType primitiveType = RPrimitiveType.of(heavier, evaluator.tree);

        if (heavier.primitive() && primitiveType != null) return primitiveType.logicalOr(cast(heavier), y.cast(heavier));
        return null;
    }

    public RValue bitXor(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final RPrimitiveType primitiveType = RPrimitiveType.of(heavier, evaluator.tree);

        if (heavier.primitive() && primitiveType != null) return primitiveType.bitXor(cast(heavier), y.cast(heavier));
        return null;
    }

    public RValue bitAnd(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final RPrimitiveType primitiveType = RPrimitiveType.of(heavier, evaluator.tree);

        if (heavier.primitive() && primitiveType != null) return primitiveType.bitAnd(cast(heavier), y.cast(heavier));
        return null;
    }

    public RValue bitOr(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final RPrimitiveType primitiveType = RPrimitiveType.of(heavier, evaluator.tree);

        if (heavier.primitive() && primitiveType != null) return primitiveType.bitOr(cast(heavier), y.cast(heavier));
        return null;
    }

    public RValue bitShiftLeft(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final RPrimitiveType primitiveType = RPrimitiveType.of(heavier, evaluator.tree);

        if (heavier.primitive() && primitiveType != null) return primitiveType.bitShiftLeft(cast(heavier), y.cast(heavier));
        return null;
    }

    public RValue bitShiftRight(@NotNull final REvaluator evaluator, @NotNull final RValue y) {
        final RDatatype heavier = RDatatype.getHeavierType(type, y.type).orElseThrow(NullPointerException::new);
        final RPrimitiveType primitiveType = RPrimitiveType.of(heavier, evaluator.tree);

        if (heavier.primitive() && primitiveType != null) return primitiveType.bitShiftRight(cast(heavier), y.cast(heavier));
        return null;
    }

    @Override
    public String toString() {
        return type.getName() + ": " + value;
    }

}
