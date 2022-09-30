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

    private static ByteCodeValue charValue(final int c) {
        return new ByteCodeValue(ByteDatatype.CHAR, ArrayUtils.toObject(ByteCode.intToBytes(c)));
    }

    private static ByteCodeValue intValue(final int i) {
        return new ByteCodeValue(ByteDatatype.INT, ArrayUtils.toObject(ByteCode.intToBytes(i)));
    }

    private static ByteCodeValue longValue(final long l) {
        return new ByteCodeValue(ByteDatatype.LONG, ArrayUtils.toObject(ByteCode.longToBytes(l)));
    }

    private static ByteCodeValue floatValue(final float f) {
        return new ByteCodeValue(ByteDatatype.FLOAT, ArrayUtils.toObject(ByteCode.floatToBytes(f)));
    }

    private static ByteCodeValue doubleValue(final double d) {
        return new ByteCodeValue(ByteDatatype.DOUBLE, ArrayUtils.toObject(ByteCode.doubleToBytes(d)));
    }

    private static ByteCodeValue stringValue(@NotNull final String s) {
        final Byte[] codes = ByteCode.string(s).codes();
        return new ByteCodeValue(ByteDatatype.STRING, Arrays.stream(codes).toList().subList(0, codes.length - 1).toArray(new Byte[0]));
    }

    public boolean noneMatchType(@NotNull final ByteDatatype... types) {
        return !Stream.of(types).map(ByteDatatype::id).toList().contains(type.id());
    }

    public ByteCodeValue not() {
        if (noneMatchType(ByteDatatype.BOOL)) throw new ByteCodeException("Expected boolean value for 'not' operator");
        final int val = ByteCode.bytesToInt(ArrayUtils.toPrimitive(value));
        return boolValue(val == 0);
    }

    public ByteCodeValue bit_not() {
        if (noneMatchType(ByteDatatype.INT, ByteDatatype.LONG, ByteDatatype.CHAR)) throw new ByteCodeException("Expected integer value for 'bit-not' operator");
        switch (type.id()) {
            case 0x02 -> {
                final int val = ByteCode.bytesToInt(ArrayUtils.toPrimitive(value));
                return intValue(~val);
            }
            case 0x01 -> {
                final int val = ByteCode.bytesToInt(ArrayUtils.toPrimitive(value));
                return charValue(~val);
            }
            case 0x03 -> {
                final long val = ByteCode.bytesToLong(ArrayUtils.toPrimitive(value));
                return longValue(~val);
            }
        }
        return this;
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

    public String asString() {
        return type.name() + ":" + switch (type.id()) {
            case 0x00, 0x01, 0x02 -> "" + ByteCode.bytesToInt(ArrayUtils.toPrimitive(value));
            case 0x03 -> "" + ByteCode.bytesToLong(ArrayUtils.toPrimitive(value));
            case 0x04 -> "" + ByteCode.bytesToFloat(ArrayUtils.toPrimitive(value));
            case 0x05 -> "" + ByteCode.bytesToDouble(ArrayUtils.toPrimitive(value));
            case 0x06 -> ByteCode.ofString(ArrayUtils.toPrimitive(value));
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
            case 0x01, 0x02 -> boolValue(ByteCode.bytesToInt(ArrayUtils.toPrimitive(value)) != 0);
            case 0x03 -> boolValue(ByteCode.bytesToLong(ArrayUtils.toPrimitive(value)) != 0L);
            case 0x04 -> boolValue(ByteCode.bytesToFloat(ArrayUtils.toPrimitive(value)) != 0.0f);
            case 0x05 -> boolValue(ByteCode.bytesToDouble(ArrayUtils.toPrimitive(value)) != 0.0d);
            case 0x06 -> boolValue(!ByteCode.ofString(ArrayUtils.toPrimitive(value)).isEmpty());
            case 0x07 -> boolValue(false);
            default -> null;
        };
    }

    public ByteCodeValue castToChar() {
        return switch (type.id()) {
            case 0x00, 0x02 -> charValue(ByteCode.bytesToInt(ArrayUtils.toPrimitive(value)));
            case 0x01 -> this;
            case 0x03 -> charValue((int) ByteCode.bytesToLong(ArrayUtils.toPrimitive(value)));
            case 0x04 -> charValue((int) ByteCode.bytesToFloat(ArrayUtils.toPrimitive(value)));
            case 0x05 -> charValue((int) ByteCode.bytesToDouble(ArrayUtils.toPrimitive(value)));
            case 0x06 -> {
                final String val = ByteCode.ofString(ArrayUtils.toPrimitive(value));
                yield charValue(val.length() == 1 ? val.charAt(0) : 0);
            }
            case 0x07 -> charValue(0);
            default -> null;
        };
    }

    public ByteCodeValue castToInt() {
        return switch (type.id()) {
            case 0x00, 0x01 -> intValue(ByteCode.bytesToInt(ArrayUtils.toPrimitive(value)));
            case 0x02 -> this;
            case 0x03 -> intValue((int) ByteCode.bytesToLong(ArrayUtils.toPrimitive(value)));
            case 0x04 -> intValue((int) ByteCode.bytesToFloat(ArrayUtils.toPrimitive(value)));
            case 0x05 -> intValue((int) ByteCode.bytesToDouble(ArrayUtils.toPrimitive(value)));
            case 0x06 -> {
                final String val = ByteCode.ofString(ArrayUtils.toPrimitive(value));
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
            case 0x00, 0x01, 0x02 -> longValue(ByteCode.bytesToInt(ArrayUtils.toPrimitive(value)));
            case 0x03 -> this;
            case 0x04 -> longValue((long) ByteCode.bytesToFloat(ArrayUtils.toPrimitive(value)));
            case 0x05 -> longValue((long) ByteCode.bytesToDouble(ArrayUtils.toPrimitive(value)));
            case 0x06 -> {
                final String val = ByteCode.ofString(ArrayUtils.toPrimitive(value));
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
            case 0x00, 0x01, 0x02 -> floatValue(ByteCode.bytesToInt(ArrayUtils.toPrimitive(value)));
            case 0x03 -> floatValue(ByteCode.bytesToLong(ArrayUtils.toPrimitive(value)));
            case 0x04 -> this;
            case 0x05 -> floatValue((float) ByteCode.bytesToDouble(ArrayUtils.toPrimitive(value)));
            case 0x06 -> {
                final String val = ByteCode.ofString(ArrayUtils.toPrimitive(value));
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
            case 0x00, 0x01, 0x02 -> doubleValue(ByteCode.bytesToInt(ArrayUtils.toPrimitive(value)));
            case 0x03 -> doubleValue(ByteCode.bytesToLong(ArrayUtils.toPrimitive(value)));
            case 0x04 -> doubleValue(ByteCode.bytesToFloat(ArrayUtils.toPrimitive(value)));
            case 0x05 -> this;
            case 0x06 -> {
                final String val = ByteCode.ofString(ArrayUtils.toPrimitive(value));
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
            case 0x00, 0x02 -> stringValue("" + ByteCode.bytesToInt(ArrayUtils.toPrimitive(value)));
            case 0x01 -> stringValue("" + (char) ByteCode.bytesToInt(ArrayUtils.toPrimitive(value)));
            case 0x03 -> stringValue("" + ByteCode.bytesToLong(ArrayUtils.toPrimitive(value)));
            case 0x04 -> stringValue("" + ByteCode.bytesToFloat(ArrayUtils.toPrimitive(value)));
            case 0x05 -> stringValue("" + ByteCode.bytesToDouble(ArrayUtils.toPrimitive(value)));
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
