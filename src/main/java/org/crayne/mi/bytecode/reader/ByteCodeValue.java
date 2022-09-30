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

public record ByteCodeValue(ByteDatatype type, Byte[] value) {

    private static ByteCodeValue boolValue(final boolean b) {
        return new ByteCodeValue(ByteDatatype.BOOL, ArrayUtils.toObject(ByteCode.intToBytes(b ? 1 : 0)));
    }

    private static boolean boolValue(final Byte[] val) {
        return ByteCode.bytesToInt(ArrayUtils.toPrimitive(val)) != 0;
    }

    private static ByteCodeValue charValue(final int c) {
        return new ByteCodeValue(ByteDatatype.CHAR, ArrayUtils.toObject(ByteCode.intToBytes(c)));
    }

    private static ByteCodeValue intValue(final int i) {
        return new ByteCodeValue(ByteDatatype.INT, ArrayUtils.toObject(ByteCode.intToBytes(i)));
    }

    private static int intValue(final Byte[] val) {
        return ByteCode.bytesToInt(ArrayUtils.toPrimitive(val));
    }

    private static ByteCodeValue longValue(final long l) {
        return new ByteCodeValue(ByteDatatype.LONG, ArrayUtils.toObject(ByteCode.longToBytes(l)));
    }

    private static long longValue(final Byte[] val) {
        return ByteCode.bytesToLong(ArrayUtils.toPrimitive(val));
    }

    private static ByteCodeValue floatValue(final float f) {
        return new ByteCodeValue(ByteDatatype.FLOAT, ArrayUtils.toObject(ByteCode.floatToBytes(f)));
    }

    private static float floatValue(final Byte[] val) {
        return ByteCode.bytesToFloat(ArrayUtils.toPrimitive(val));
    }

    private static ByteCodeValue doubleValue(final double d) {
        return new ByteCodeValue(ByteDatatype.DOUBLE, ArrayUtils.toObject(ByteCode.doubleToBytes(d)));
    }

    private static double doubleValue(final Byte[] val) {
        return ByteCode.bytesToInt(ArrayUtils.toPrimitive(val));
    }

    private static ByteCodeValue stringValue(@NotNull final String s) {
        final Byte[] codes = ByteCode.string(s).codes();
        return new ByteCodeValue(ByteDatatype.STRING, Arrays.stream(codes).toList().subList(1, codes.length - 1).toArray(new Byte[0]));
    }

    private static String stringValue(final Byte[] val) {
        return ByteCode.bytesToString(ArrayUtils.toPrimitive(val), true);
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
        if (noneMatchType(ByteDatatype.BOOL)) throw new ByteCodeException("Expected boolean value for 'not' operator");
        final int val = ByteCode.bytesToInt(ArrayUtils.toPrimitive(value));
        return boolValue(val == 0);
    }

    public ByteCodeValue bit_not() {
        if (notAnInteger()) throw new ByteCodeException("Expected integer value for 'bit-not' operator");
        return switch (type.id()) {
            case 0x02 -> intValue(~intValue(value));
            case 0x01 -> charValue(~intValue(value));
            case 0x03 -> longValue(~longValue(value));
            default -> this;
        };
    }

    public ByteCodeValue equal(@NotNull final ByteCodeValue other) {
        final ByteDatatype heavier = heavier(type, other.type);
        final boolean comparingEnums = type.id() == ByteDatatype.ENUM.id() && other.type.id() == ByteDatatype.ENUM.id();
        if (comparingEnums) return boolValue(Arrays.equals(value, other.value));
        if (heavier == null) return boolValue(false);
        final ByteCodeValue safeCastX = cast(heavier);
        final ByteCodeValue safeCastY = other.cast(heavier);
        return boolValue(Arrays.equals(safeCastX.value, safeCastY.value));
    }

    public ByteCodeValue plus(@NotNull final ByteCodeValue other) {
        final ByteDatatype heavier = heavier(type, other.type);
        if (heavier == null) return this;
        final ByteCodeValue safeCastX = cast(heavier);
        final ByteCodeValue safeCastY = other.cast(heavier);
        if (safeCastX.notANumber() && safeCastX.noneMatchType(ByteDatatype.STRING))
            throw new ByteCodeException("Expected string or number value for 'plus' operator");

        return switch (safeCastX.type.id()) {
            case 0x02 -> intValue(intValue(safeCastX.value) + intValue(safeCastY.value));
            case 0x01 -> charValue(intValue(safeCastX.value) + intValue(safeCastY.value));
            case 0x03 -> longValue(longValue(safeCastX.value) + longValue(safeCastY.value));
            case 0x04 -> floatValue(floatValue(safeCastX.value) + floatValue(safeCastY.value));
            case 0x05 -> doubleValue(doubleValue(safeCastX.value) + doubleValue(safeCastY.value));
            case 0x06 -> stringValue(stringValue(safeCastX.value) + stringValue(safeCastY.value));
            default -> this;
        };
    }

    public String asString() {
        return type.name() + ":" + switch (type.id()) {
            case 0x00, 0x01, 0x02 -> "" + intValue(value);
            case 0x03 -> "" + longValue(value);
            case 0x04 -> "" + floatValue(value);
            case 0x05 -> "" + doubleValue(value);
            case 0x06 -> stringValue(value);
            case 0x07 -> "unimplemented";
            default -> null;
        };
    }

    private static final Map<Integer, Integer> datatypeRanking = new HashMap<>() {{
        this.put(ByteDatatype.NULL.id(), 0);
        this.put(ByteDatatype.DOUBLE.id(), 3);
        this.put(ByteDatatype.FLOAT.id(), 4);
        this.put(ByteDatatype.LONG.id(), 5);
        this.put(ByteDatatype.INT.id(), 6);
        this.put(ByteDatatype.CHAR.id(), 6);
        this.put(ByteDatatype.BOOL.id(), 7);
        this.put(ByteDatatype.STRING.id(), 8);
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
            default -> null;
        };
    }

    public ByteCodeValue castToBool() {
        return switch (type.id()) {
            case 0x00 -> this;
            case 0x01, 0x02 -> boolValue(intValue(value) != 0);
            case 0x03 -> boolValue(longValue(value) != 0L);
            case 0x04 -> boolValue(floatValue(value) != 0.0f);
            case 0x05 -> boolValue(doubleValue(value) != 0.0d);
            case 0x06 -> boolValue(!stringValue(value).isEmpty());
            case 0x07 -> boolValue(false);
            default -> null;
        };
    }

    public ByteCodeValue castToChar() {
        return switch (type.id()) {
            case 0x00, 0x02 -> charValue(intValue(value));
            case 0x01 -> this;
            case 0x03 -> charValue((int) longValue(value));
            case 0x04 -> charValue((int) floatValue(value));
            case 0x05 -> charValue((int) doubleValue(value));
            case 0x06 -> {
                final String val = stringValue(value);
                yield charValue(val.length() == 1 ? val.charAt(0) : 0);
            }
            case 0x07 -> charValue(0);
            default -> null;
        };
    }

    public ByteCodeValue castToInt() {
        return switch (type.id()) {
            case 0x00, 0x01 -> intValue(intValue(value));
            case 0x02 -> this;
            case 0x03 -> intValue((int) longValue(value));
            case 0x04 -> intValue((int) floatValue(value));
            case 0x05 -> intValue((int) doubleValue(value));
            case 0x06 -> {
                final String val = stringValue(value);
                try {
                    yield intValue(Integer.parseInt(val));
                } catch (final NumberFormatException e) {
                    yield intValue(Integer.MIN_VALUE);
                }
            }
            case 0x07 -> intValue(0); // TODO converting enums into other datatypes, TODO make enums work at all
            default -> null;
        };
    }

    public ByteCodeValue castToLong() {
        return switch (type.id()) {
            case 0x00, 0x01, 0x02 -> longValue(longValue(value));
            case 0x03 -> this;
            case 0x04 -> longValue((long) floatValue(value));
            case 0x05 -> longValue((long) doubleValue(value));
            case 0x06 -> {
                final String val = stringValue(value);
                try {
                    yield longValue(Long.parseLong(val));
                } catch (final NumberFormatException e) {
                    yield longValue(Long.MIN_VALUE);
                }
            }
            case 0x07 -> longValue(0); // TODO converting enums into other datatypes (ordinal indices & member name strings)
            default -> null;
        };
    }

    public ByteCodeValue castToFloat() {
        return switch (type.id()) {
            case 0x00, 0x01, 0x02 -> floatValue(intValue(value));
            case 0x03 -> floatValue(longValue(value));
            case 0x04 -> this;
            case 0x05 -> floatValue((float) doubleValue(value));
            case 0x06 -> {
                final String val = stringValue(value);
                try {
                    yield doubleValue(Float.parseFloat(val));
                } catch (final NumberFormatException e) {
                    yield doubleValue(Float.NaN);
                }
            }
            case 0x07 -> floatValue(0); // TODO converting enums into other datatypes
            default -> null;
        };
    }

    public ByteCodeValue castToDouble() {
        return switch (type.id()) {
            case 0x00, 0x01, 0x02 -> doubleValue(intValue(value));
            case 0x03 -> doubleValue(longValue(value));
            case 0x04 -> doubleValue(floatValue(value));
            case 0x05 -> this;
            case 0x06 -> {
                final String val = stringValue(value);
                try {
                    yield doubleValue(Double.parseDouble(val));
                } catch (final NumberFormatException e) {
                    yield doubleValue(Double.NaN);
                }
            }
            case 0x07 -> doubleValue(0); // TODO converting enums into other datatypes
            default -> null;
        };
    }

    public ByteCodeValue castToString() {
        return switch (type.id()) {
            case 0x00, 0x02 -> stringValue("" + intValue(value));
            case 0x01 -> stringValue(Character.toString((char) intValue(value)));
            case 0x03 -> stringValue("" + longValue(value));
            case 0x04 -> stringValue("" + floatValue(value));
            case 0x05 -> stringValue("" + doubleValue(value));
            case 0x06 -> this;
            case 0x07 -> stringValue(""); // TODO converting enums into other datatypes
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
