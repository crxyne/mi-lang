package org.crayne.mi.bytecode.reader;

import org.apache.commons.lang3.ArrayUtils;
import org.crayne.mi.bytecode.common.ByteCode;
import org.crayne.mi.bytecode.common.ByteCodeException;
import org.crayne.mi.bytecode.common.ByteDatatype;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public record ByteCodeValue(ByteDatatype type, Byte[] value, ByteCodeInterpreter runtime) {

    public static ByteCodeValue boolValue(final boolean b, @NotNull final ByteCodeInterpreter runtime) {
        return new ByteCodeValue(ByteDatatype.BOOL, ArrayUtils.toObject(ByteCode.intToBytes(b ? 1 : 0)), runtime);
    }

    private static boolean boolValue(final Byte[] val) {
        return ByteCode.bytesToInt(ArrayUtils.toPrimitive(val)) != 0;
    }

    private static char charValue(final Byte[] val) {
        return (char) ByteCode.bytesToInt(ArrayUtils.toPrimitive(val));
    }

    public static ByteCodeValue charValue(final int c, @NotNull final ByteCodeInterpreter runtime) {
        return new ByteCodeValue(ByteDatatype.CHAR, ArrayUtils.toObject(ByteCode.intToBytes(c)), runtime);
    }

    public static ByteCodeValue intValue(final int i, @NotNull final ByteCodeInterpreter runtime) {
        return new ByteCodeValue(ByteDatatype.INT, ArrayUtils.toObject(ByteCode.intToBytes(i)), runtime);
    }

    private static int intValue(final Byte[] val) {
        return ByteCode.bytesToInt(ArrayUtils.toPrimitive(val));
    }

    public static ByteCodeValue longValue(final long l, @NotNull final ByteCodeInterpreter runtime) {
        return new ByteCodeValue(ByteDatatype.LONG, ArrayUtils.toObject(ByteCode.longToBytes(l)), runtime);
    }

    private static long longValue(final Byte[] val) {
        return ByteCode.bytesToLong(ArrayUtils.toPrimitive(val));
    }

    public static ByteCodeValue floatValue(final float f, @NotNull final ByteCodeInterpreter runtime) {
        return new ByteCodeValue(ByteDatatype.FLOAT, ArrayUtils.toObject(ByteCode.floatToBytes(f)), runtime);
    }

    private static float floatValue(final Byte[] val) {
        return ByteCode.bytesToFloat(ArrayUtils.toPrimitive(val));
    }

    public static ByteCodeValue doubleValue(final double d, @NotNull final ByteCodeInterpreter runtime) {
        return new ByteCodeValue(ByteDatatype.DOUBLE, ArrayUtils.toObject(ByteCode.doubleToBytes(d)), runtime);
    }

    private static double doubleValue(final Byte[] val) {
        return ByteCode.bytesToDouble(ArrayUtils.toPrimitive(val));
    }

    public static ByteCodeValue stringValue(@NotNull final String s, @NotNull final ByteCodeInterpreter runtime) {
        final Byte[] codes = ByteCode.string(s).codes();
        return new ByteCodeValue(ByteDatatype.STRING, Arrays.stream(codes).toList().subList(1, codes.length - 1).toArray(new Byte[0]), runtime);
    }

    private static String stringValue(final Byte[] val) {
        return ByteCode.bytesToString(ArrayUtils.toPrimitive(val), true);
    }

    private static ByteCodeValue nullValue(@NotNull final ByteCodeInterpreter runtime) {
        return new ByteCodeValue(ByteDatatype.NULL, new Byte[0], runtime);
    }

    public boolean noneMatchType(@NotNull final ByteDatatype... types) {
        return !Stream.of(types).map(ByteDatatype::id).toList().contains(type.id());
    }

    public boolean notANumber() {
        return noneMatchType(ByteDatatype.INT, ByteDatatype.LONG, ByteDatatype.CHAR, ByteDatatype.FLOAT, ByteDatatype.DOUBLE);
    }

    public boolean notAnInteger() {
        return noneMatchType(ByteDatatype.INT, ByteDatatype.LONG, ByteDatatype.CHAR);
    }

    public ByteCodeValue not() {
        if (isnull()) throw new ByteCodeException("Expected nonnull value for 'not' operator");
        if (noneMatchType(ByteDatatype.BOOL)) throw new ByteCodeException("Expected boolean value for 'not' operator");
        final int val = ByteCode.bytesToInt(ArrayUtils.toPrimitive(value));
        return boolValue(val == 0, runtime);
    }

    public boolean isnull() {
        return type == ByteDatatype.NULL;
    }

    public ByteCodeValue bit_not() {
        if (isnull()) throw new ByteCodeException("Expected nonnull value for 'bit-not' operator");
        if (notAnInteger()) throw new ByteCodeException("Expected integer value for 'bit-not' operator");
        return switch (type.id()) {
            case 0x02 -> intValue(~intValue(value), runtime);
            case 0x01 -> charValue(~intValue(value), runtime);
            case 0x03 -> longValue(~longValue(value), runtime);
            default -> this;
        };
    }

    public ByteCodeValue equal(@NotNull final ByteCodeValue other) {
        final ByteDatatype heavier = heavier(type, other.type);
        final boolean comparingEnums = type.code() == ByteDatatype.ENUM.code() && other.type.code() == ByteDatatype.ENUM.code();

        if (comparingEnums) return boolValue(Arrays.equals(value, other.value), runtime);
        if (heavier == null) return boolValue(false, runtime);
        final ByteCodeValue safeCastX = cast(heavier);
        final ByteCodeValue safeCastY = other.cast(heavier);

        return boolValue(Arrays.equals(safeCastX.value, safeCastY.value), runtime);
    }

    public ByteCodeValue plus(@NotNull final ByteCodeValue other) {
        final ByteDatatype heavier = heavier(type, other.type);
        if (heavier == null) return this;
        final ByteCodeValue safeCastX = cast(heavier);
        final ByteCodeValue safeCastY = other.cast(heavier);
        if (safeCastX.isnull()) throw new ByteCodeException("Expected nonnull value for 'plus' operator");
        if (safeCastX.notANumber() && safeCastX.noneMatchType(ByteDatatype.STRING))
            throw new ByteCodeException("Expected string or number value for 'plus' operator");

        return switch (safeCastX.type.id()) {
            case 0x02 -> intValue(intValue(safeCastX.value) + intValue(safeCastY.value), runtime);
            case 0x01 -> charValue(intValue(safeCastX.value) + intValue(safeCastY.value), runtime);
            case 0x03 -> longValue(longValue(safeCastX.value) + longValue(safeCastY.value), runtime);
            case 0x04 -> floatValue(floatValue(safeCastX.value) + floatValue(safeCastY.value), runtime);
            case 0x05 -> doubleValue(doubleValue(safeCastX.value) + doubleValue(safeCastY.value), runtime);
            case 0x06 -> stringValue(stringValue(safeCastX.value) + stringValue(safeCastY.value), runtime);
            default -> this;
        };
    }

    public ByteCodeValue minus(@NotNull final ByteCodeValue other) {
        final ByteDatatype heavier = heavier(type, other.type);
        if (heavier == null) return this;
        final ByteCodeValue safeCastX = cast(heavier);
        final ByteCodeValue safeCastY = other.cast(heavier);
        if (safeCastX.isnull()) throw new ByteCodeException("Expected nonnull value for 'minus' operator");
        if (safeCastX.notANumber()) throw new ByteCodeException("Expected number value for 'minus' operator");

        return switch (safeCastX.type.id()) {
            case 0x02 -> intValue(intValue(safeCastX.value) - intValue(safeCastY.value), runtime);
            case 0x01 -> charValue(intValue(safeCastX.value) - intValue(safeCastY.value), runtime);
            case 0x03 -> longValue(longValue(safeCastX.value) - longValue(safeCastY.value), runtime);
            case 0x04 -> floatValue(floatValue(safeCastX.value) - floatValue(safeCastY.value), runtime);
            case 0x05 -> doubleValue(doubleValue(safeCastX.value) - doubleValue(safeCastY.value), runtime);
            default -> this;
        };
    }

    public ByteCodeValue multiply(@NotNull final ByteCodeValue other) {
        final ByteDatatype heavier = heavier(type, other.type);
        if (heavier == null) return this;
        final ByteCodeValue safeCastX = cast(heavier);
        final ByteCodeValue safeCastY = other.cast(heavier);
        if (safeCastX.isnull()) throw new ByteCodeException("Expected nonnull value for 'multiply' operator");
        if (safeCastX.notANumber()) throw new ByteCodeException("Expected number value for 'multiply' operator");

        return switch (safeCastX.type.id()) {
            case 0x02 -> intValue(intValue(safeCastX.value) * intValue(safeCastY.value), runtime);
            case 0x01 -> charValue(intValue(safeCastX.value) * intValue(safeCastY.value), runtime);
            case 0x03 -> longValue(longValue(safeCastX.value) * longValue(safeCastY.value), runtime);
            case 0x04 -> floatValue(floatValue(safeCastX.value) * floatValue(safeCastY.value), runtime);
            case 0x05 -> doubleValue(doubleValue(safeCastX.value) * doubleValue(safeCastY.value), runtime);
            default -> this;
        };
    }

    public ByteCodeValue divide(@NotNull final ByteCodeValue other) {
        final ByteDatatype heavier = heavier(type, other.type);
        if (heavier == null) return this;
        final ByteCodeValue safeCastX = cast(heavier);
        final ByteCodeValue safeCastY = other.cast(heavier);
        if (safeCastX.isnull()) throw new ByteCodeException("Expected nonnull value for 'divide' operator");
        if (safeCastX.notANumber()) throw new ByteCodeException("Expected number value for 'divide' operator");

        return switch (safeCastX.type.id()) {
            case 0x02 -> intValue(intValue(safeCastX.value) / intValue(safeCastY.value), runtime);
            case 0x01 -> charValue(intValue(safeCastX.value) / intValue(safeCastY.value), runtime);
            case 0x03 -> longValue(longValue(safeCastX.value) / longValue(safeCastY.value), runtime);
            case 0x04 -> floatValue(floatValue(safeCastX.value) / floatValue(safeCastY.value), runtime);
            case 0x05 -> doubleValue(doubleValue(safeCastX.value) / doubleValue(safeCastY.value), runtime);
            default -> this;
        };
    }

    public ByteCodeValue modulo(@NotNull final ByteCodeValue other) {
        final ByteDatatype heavier = heavier(type, other.type);
        if (heavier == null) return this;
        final ByteCodeValue safeCastX = cast(heavier);
        final ByteCodeValue safeCastY = other.cast(heavier);
        if (safeCastX.isnull()) throw new ByteCodeException("Expected nonnull value for 'modulo' operator");
        if (safeCastX.notANumber()) throw new ByteCodeException("Expected number value for 'modulo' operator");

        return switch (safeCastX.type.id()) {
            case 0x02 -> intValue(intValue(safeCastX.value) % intValue(safeCastY.value), runtime);
            case 0x01 -> charValue(intValue(safeCastX.value) % intValue(safeCastY.value), runtime);
            case 0x03 -> longValue(longValue(safeCastX.value) % longValue(safeCastY.value), runtime);
            case 0x04 -> floatValue(floatValue(safeCastX.value) % floatValue(safeCastY.value), runtime);
            case 0x05 -> doubleValue(doubleValue(safeCastX.value) % doubleValue(safeCastY.value), runtime);
            default -> this;
        };
    }

    public ByteCodeValue bit_and(@NotNull final ByteCodeValue other) {
        final ByteDatatype heavier = heavier(type, other.type);
        if (heavier == null) return this;
        final ByteCodeValue safeCastX = cast(heavier);
        final ByteCodeValue safeCastY = other.cast(heavier);
        if (safeCastX.isnull()) throw new ByteCodeException("Expected nonnull value for 'bit-and' operator");
        if (safeCastX.notAnInteger()) throw new ByteCodeException("Expected integer value for 'bit-and' operator");

        return switch (safeCastX.type.id()) {
            case 0x02 -> intValue(intValue(safeCastX.value) & intValue(safeCastY.value), runtime);
            case 0x01 -> charValue(intValue(safeCastX.value) & intValue(safeCastY.value), runtime);
            case 0x03 -> longValue(longValue(safeCastX.value) & longValue(safeCastY.value), runtime);
            default -> this;
        };
    }

    public ByteCodeValue bit_or(@NotNull final ByteCodeValue other) {
        final ByteDatatype heavier = heavier(type, other.type);
        if (heavier == null) return this;
        final ByteCodeValue safeCastX = cast(heavier);
        final ByteCodeValue safeCastY = other.cast(heavier);
        if (safeCastX.isnull()) throw new ByteCodeException("Expected nonnull value for 'bit-or' operator");
        if (safeCastX.notAnInteger()) throw new ByteCodeException("Expected integer value for 'bit-or' operator");

        return switch (safeCastX.type.id()) {
            case 0x02 -> intValue(intValue(safeCastX.value) | intValue(safeCastY.value), runtime);
            case 0x01 -> charValue(intValue(safeCastX.value) | intValue(safeCastY.value), runtime);
            case 0x03 -> longValue(longValue(safeCastX.value) | longValue(safeCastY.value), runtime);
            default -> this;
        };
    }

    public ByteCodeValue bit_xor(@NotNull final ByteCodeValue other) {
        final ByteDatatype heavier = heavier(type, other.type);
        if (heavier == null) return this;
        final ByteCodeValue safeCastX = cast(heavier);
        final ByteCodeValue safeCastY = other.cast(heavier);
        if (safeCastX.isnull()) throw new ByteCodeException("Expected nonnull value for 'bit-xor' operator");
        if (safeCastX.notAnInteger()) throw new ByteCodeException("Expected integer value for 'bit-xor' operator");

        return switch (safeCastX.type.id()) {
            case 0x02 -> intValue(intValue(safeCastX.value) ^ intValue(safeCastY.value), runtime);
            case 0x01 -> charValue(intValue(safeCastX.value) ^ intValue(safeCastY.value), runtime);
            case 0x03 -> longValue(longValue(safeCastX.value) ^ longValue(safeCastY.value), runtime);
            default -> this;
        };
    }

    public ByteCodeValue bit_shift_left(@NotNull final ByteCodeValue other) {
        final ByteDatatype heavier = heavier(type, other.type);
        if (heavier == null) return this;
        final ByteCodeValue safeCastX = cast(heavier);
        final ByteCodeValue safeCastY = other.cast(heavier);
        if (safeCastX.isnull()) throw new ByteCodeException("Expected nonnull value for 'bit-shift-left' operator");
        if (safeCastX.notAnInteger()) throw new ByteCodeException("Expected integer value for 'bit-shift-left' operator");

        return switch (safeCastX.type.id()) {
            case 0x02 -> intValue(intValue(safeCastX.value) << intValue(safeCastY.value), runtime);
            case 0x01 -> charValue(intValue(safeCastX.value) << intValue(safeCastY.value), runtime);
            case 0x03 -> longValue(longValue(safeCastX.value) << longValue(safeCastY.value), runtime);
            default -> this;
        };
    }

    public ByteCodeValue bit_shift_right(@NotNull final ByteCodeValue other) {
        final ByteDatatype heavier = heavier(type, other.type);
        if (heavier == null) return this;
        final ByteCodeValue safeCastX = cast(heavier);
        final ByteCodeValue safeCastY = other.cast(heavier);
        if (safeCastX.isnull()) throw new ByteCodeException("Expected nonnull value for 'bit-shift-right' operator");
        if (safeCastX.notAnInteger()) throw new ByteCodeException("Expected integer value for 'bit-shift-right' operator");

        return switch (safeCastX.type.id()) {
            case 0x02 -> intValue(intValue(safeCastX.value) >> intValue(safeCastY.value), runtime);
            case 0x01 -> charValue(intValue(safeCastX.value) >> intValue(safeCastY.value), runtime);
            case 0x03 -> longValue(longValue(safeCastX.value) >> longValue(safeCastY.value), runtime);
            default -> this;
        };
    }

    public ByteCodeValue logical_and(@NotNull final ByteCodeValue other) {
        final ByteDatatype heavier = heavier(type, other.type);
        if (heavier == null) return this;
        final ByteCodeValue safeCastX = cast(heavier);
        final ByteCodeValue safeCastY = other.cast(heavier);
        if (safeCastX.isnull()) throw new ByteCodeException("Expected nonnull value for 'logical-and' operator");
        if (safeCastX.noneMatchType(ByteDatatype.BOOL)) throw new ByteCodeException("Expected boolean value for 'logical-and' operator");

        return safeCastX.type.id() == 0x00 ? boolValue(boolValue(safeCastX.value) && boolValue(safeCastY.value), runtime) : this;
    }

    public ByteCodeValue logical_or(@NotNull final ByteCodeValue other) {
        final ByteDatatype heavier = heavier(type, other.type);
        if (heavier == null) return this;
        final ByteCodeValue safeCastX = cast(heavier);
        final ByteCodeValue safeCastY = other.cast(heavier);
        if (safeCastX.isnull()) throw new ByteCodeException("Expected nonnull value for 'logical-or' operator");
        if (safeCastX.noneMatchType(ByteDatatype.BOOL)) throw new ByteCodeException("Expected boolean value for 'logical-or' operator");

        return safeCastX.type.id() == 0x00 ? boolValue(boolValue(safeCastX.value) || boolValue(safeCastY.value), runtime) : this;
    }

    public ByteCodeValue less_than(@NotNull final ByteCodeValue other) {
        final ByteDatatype heavier = heavier(type, other.type);
        if (heavier == null) return this;
        final ByteCodeValue safeCastX = cast(heavier);
        final ByteCodeValue safeCastY = other.cast(heavier);
        if (safeCastX.isnull()) throw new ByteCodeException("Expected nonnull value for 'less-than' operator");
        if (safeCastX.notANumber() && safeCastX.noneMatchType(ByteDatatype.STRING))
            throw new ByteCodeException("Expected string or number value for 'less-than' operator");

        return switch (safeCastX.type.id()) {
            case 0x02, 0x01 -> boolValue(intValue(safeCastX.value) < intValue(safeCastY.value), runtime);
            case 0x03 -> boolValue(longValue(safeCastX.value) < longValue(safeCastY.value), runtime);
            case 0x04 -> boolValue(floatValue(safeCastX.value) < floatValue(safeCastY.value), runtime);
            case 0x05 -> boolValue(doubleValue(safeCastX.value) < doubleValue(safeCastY.value), runtime);
            case 0x06 -> boolValue(stringValue(safeCastX.value).length() < stringValue(safeCastY.value).length(), runtime);
            default -> this;
        };
    }

    public ByteCodeValue less_than_or_equal(@NotNull final ByteCodeValue other) {
        final ByteDatatype heavier = heavier(type, other.type);
        if (heavier == null) return this;
        final ByteCodeValue safeCastX = cast(heavier);
        final ByteCodeValue safeCastY = other.cast(heavier);
        if (safeCastX.isnull()) throw new ByteCodeException("Expected nonnull value for 'less-than-or-equal' operator");
        if (safeCastX.notANumber() && safeCastX.noneMatchType(ByteDatatype.STRING))
            throw new ByteCodeException("Expected string or number value for 'less-than-or-equal' operator");

        return switch (safeCastX.type.id()) {
            case 0x02, 0x01 -> boolValue(intValue(safeCastX.value) <= intValue(safeCastY.value), runtime);
            case 0x03 -> boolValue(longValue(safeCastX.value) <= longValue(safeCastY.value), runtime);
            case 0x04 -> boolValue(floatValue(safeCastX.value) <= floatValue(safeCastY.value), runtime);
            case 0x05 -> boolValue(doubleValue(safeCastX.value) <= doubleValue(safeCastY.value), runtime);
            case 0x06 -> boolValue(stringValue(safeCastX.value).length() <= stringValue(safeCastY.value).length(), runtime);
            default -> this;
        };
    }

    public ByteCodeValue greater_than(@NotNull final ByteCodeValue other) {
        final ByteDatatype heavier = heavier(type, other.type);
        if (heavier == null) return this;
        final ByteCodeValue safeCastX = cast(heavier);
        final ByteCodeValue safeCastY = other.cast(heavier);
        if (safeCastX.isnull()) throw new ByteCodeException("Expected nonnull value for 'greater-than' operator");
        if (safeCastX.notANumber() && safeCastX.noneMatchType(ByteDatatype.STRING))
            throw new ByteCodeException("Expected string or number value for 'greater-than' operator");

        return switch (safeCastX.type.id()) {
            case 0x02, 0x01 -> boolValue(intValue(safeCastX.value) > intValue(safeCastY.value), runtime);
            case 0x03 -> boolValue(longValue(safeCastX.value) > longValue(safeCastY.value), runtime);
            case 0x04 -> boolValue(floatValue(safeCastX.value) > floatValue(safeCastY.value), runtime);
            case 0x05 -> boolValue(doubleValue(safeCastX.value) > doubleValue(safeCastY.value), runtime);
            case 0x06 -> boolValue(stringValue(safeCastX.value).length() > stringValue(safeCastY.value).length(), runtime);
            default -> this;
        };
    }

    public ByteCodeValue greater_than_or_equal(@NotNull final ByteCodeValue other) {
        final ByteDatatype heavier = heavier(type, other.type);
        if (heavier == null) return this;
        final ByteCodeValue safeCastX = cast(heavier);
        final ByteCodeValue safeCastY = other.cast(heavier);
        if (safeCastX.isnull()) throw new ByteCodeException("Expected nonnull value for 'greater-than-or-equal' operator");
        if (safeCastX.notANumber() && safeCastX.noneMatchType(ByteDatatype.STRING))
            throw new ByteCodeException("Expected string or number value for 'greater-than-or-equal' operator");

        return switch (safeCastX.type.id()) {
            case 0x02, 0x01 -> boolValue(intValue(safeCastX.value) >= intValue(safeCastY.value), runtime);
            case 0x03 -> boolValue(longValue(safeCastX.value) >= longValue(safeCastY.value), runtime);
            case 0x04 -> boolValue(floatValue(safeCastX.value) >= floatValue(safeCastY.value), runtime);
            case 0x05 -> boolValue(doubleValue(safeCastX.value) >= doubleValue(safeCastY.value), runtime);
            case 0x06 -> boolValue(stringValue(safeCastX.value).length() >= stringValue(safeCastY.value).length(), runtime);
            default -> this;
        };
    }

    public String asString() {
        return type.name() + ":" + asObject();
    }

    public Object asObject() {
        return switch (type.id()) {
            case 0x00 -> boolValue(value);
            case 0x01 -> charValue(value);
            case 0x02 -> intValue(value);
            case 0x03 -> longValue(value);
            case 0x04 -> floatValue(value);
            case 0x05 -> doubleValue(value);
            case 0x06 -> stringValue(value);
            default -> null;
        };
    }

    private static final Map<Integer, Integer> datatypeRanking = new HashMap<>() {{
        this.put(ByteDatatype.DOUBLE.id(), 3);
        this.put(ByteDatatype.FLOAT.id(), 4);
        this.put(ByteDatatype.LONG.id(), 5);
        this.put(ByteDatatype.INT.id(), 6);
        this.put(ByteDatatype.CHAR.id(), 6);
        this.put(ByteDatatype.BOOL.id(), 7);
        this.put(ByteDatatype.STRING.id(), 8);
        this.put(ByteDatatype.NULL.id(), 9);
    }};

    public static ByteDatatype heavier(@NotNull final ByteDatatype d1, @NotNull final ByteDatatype d2) {
        if (d1.id() == ByteDatatype.STRING.id()) return d1;
        if (d2.id() == ByteDatatype.STRING.id()) return d2;

        final Integer r1 = datatypeRanking.get(d1.id());
        final Integer r2 = datatypeRanking.get(d2.id());
        if (r1 == null || r2 == null) return null;
        return r1 < r2 ? d1 : d2;
    }

    public ByteCodeValue cast(@NotNull final ByteDatatype newType) {
        return switch (newType.id()) {
            case 0x00 -> castToBool();
            case 0x01 -> castToChar();
            case 0x02 -> castToInt();
            case 0x03 -> castToLong();
            case 0x04 -> castToFloat();
            case 0x05 -> castToDouble();
            case 0x06 -> castToString();
            case 0x08 -> nullValue(runtime);
            default -> null;
        };
    }

    public ByteCodeValue castToBool() {
        return switch (type.id()) {
            case 0x00 -> this;
            case 0x01, 0x02 -> boolValue(intValue(value) != 0, runtime);
            case 0x03 -> boolValue(longValue(value) != 0L, runtime);
            case 0x04 -> boolValue(floatValue(value) != 0.0f, runtime);
            case 0x05 -> boolValue(doubleValue(value) != 0.0d, runtime);
            case 0x06 -> boolValue(!stringValue(value).isEmpty(), runtime);
            case 0x07 -> boolValue(false, runtime);
            case 0x08 -> nullValue(runtime);
            default -> null;
        };
    }

    public ByteCodeValue castToChar() {
        if (type.code() == ByteDatatype.ENUM.code()) return charValue(runtime.ordinalOfEnumMember(this), runtime);
        return switch (type.id()) {
            case 0x00, 0x02 -> charValue(intValue(value), runtime);
            case 0x01 -> this;
            case 0x03 -> charValue((int) longValue(value), runtime);
            case 0x04 -> charValue((int) floatValue(value), runtime);
            case 0x05 -> charValue((int) doubleValue(value), runtime);
            case 0x06 -> {
                final String val = stringValue(value);
                yield charValue(val.length() == 1 ? val.charAt(0) : 0, runtime);
            }
            case 0x08 -> nullValue(runtime);
            default -> null;
        };
    }

    public ByteCodeValue castToInt() {
        if (type.code() == ByteDatatype.ENUM.code()) return intValue(runtime.ordinalOfEnumMember(this), runtime);
        return switch (type.id()) {
            case 0x00, 0x01 -> intValue(intValue(value), runtime);
            case 0x02 -> this;
            case 0x03 -> intValue((int) longValue(value), runtime);
            case 0x04 -> intValue((int) floatValue(value), runtime);
            case 0x05 -> intValue((int) doubleValue(value), runtime);
            case 0x06 -> {
                final String val = stringValue(value);
                try {
                    yield intValue(Integer.parseInt(val), runtime);
                } catch (final NumberFormatException e) {
                    yield intValue(Integer.MIN_VALUE, runtime);
                }
            }
            case 0x08 -> nullValue(runtime);
            default -> null;
        };
    }

    public ByteCodeValue castToLong() {
        if (type.code() == ByteDatatype.ENUM.code()) return longValue(runtime.ordinalOfEnumMember(this), runtime);
        return switch (type.id()) {
            case 0x00, 0x01, 0x02 -> longValue(longValue(value), runtime);
            case 0x03 -> this;
            case 0x04 -> longValue((long) floatValue(value), runtime);
            case 0x05 -> longValue((long) doubleValue(value), runtime);
            case 0x06 -> {
                final String val = stringValue(value);
                try {
                    yield longValue(Long.parseLong(val), runtime);
                } catch (final NumberFormatException e) {
                    yield longValue(Long.MIN_VALUE, runtime);
                }
            }
            case 0x08 -> nullValue(runtime);
            default -> null;
        };
    }

    public ByteCodeValue castToFloat() {
        if (type.code() == ByteDatatype.ENUM.code()) return floatValue(runtime.ordinalOfEnumMember(this), runtime);
        return switch (type.id()) {
            case 0x00, 0x01, 0x02 -> floatValue(intValue(value), runtime);
            case 0x03 -> floatValue(longValue(value), runtime);
            case 0x04 -> this;
            case 0x05 -> floatValue((float) doubleValue(value), runtime);
            case 0x06 -> {
                final String val = stringValue(value);
                try {
                    yield doubleValue(Float.parseFloat(val), runtime);
                } catch (final NumberFormatException e) {
                    yield doubleValue(Float.NaN, runtime);
                }
            }
            case 0x08 -> nullValue(runtime);
            default -> null;
        };
    }

    public ByteCodeValue castToDouble() {
        if (type.code() == ByteDatatype.ENUM.code()) return doubleValue(runtime.ordinalOfEnumMember(this), runtime);
        return switch (type.id()) {
            case 0x00, 0x01, 0x02 -> doubleValue(intValue(value), runtime);
            case 0x03 -> doubleValue(longValue(value), runtime);
            case 0x04 -> doubleValue(floatValue(value), runtime);
            case 0x05 -> this;
            case 0x06 -> {
                final String val = stringValue(value);
                try {
                    yield doubleValue(Double.parseDouble(val), runtime);
                } catch (final NumberFormatException e) {
                    yield doubleValue(Double.NaN, runtime);
                }
            }
            case 0x08 -> nullValue(runtime);
            default -> null;
        };
    }

    public ByteCodeValue castToString() {
        if (type.code() == ByteDatatype.ENUM.code()) return stringValue(runtime.nameOfEnumMember(this), runtime);
        return switch (type.id()) {
            case 0x00, 0x02 -> stringValue("" + intValue(value), runtime);
            case 0x01 -> stringValue(Character.toString((char) intValue(value)), runtime);
            case 0x03 -> stringValue("" + longValue(value), runtime);
            case 0x04 -> stringValue("" + floatValue(value), runtime);
            case 0x05 -> stringValue("" + doubleValue(value), runtime);
            case 0x06 -> this;
            case 0x08 -> nullValue(runtime);
            default -> null;
        };
    }

    public String toString() {
        return "ByteCodeValue{" +
                "type=" + type +
                ", value=" + String.join(", ", Arrays.stream(value).map(ByteCodeReader::byteToHexString).toList()) +
                '}';
    }
}
