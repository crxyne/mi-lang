package org.crayne.mi.bytecode.common;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import org.apache.commons.lang3.ArrayUtils;
import org.crayne.mi.parsing.lexer.Tokenizer;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

@SuppressWarnings("unused")
public enum ByteCode {

    INSTRUCT_FINISH((byte) 0xFF),
    PROGRAM_HEADER((byte) 0x01),
    MAIN_FUNCTION((byte) 0x02),
    JUMP((byte) 0x03),
    JUMP_IF((byte) 0x04),
    PUSH((byte) 0x07),
    POP((byte) 0x08),

    VALUE_AT_RELATIVE_ADDRESS((byte) 0x09),

    // NOT operates on the current top of the stack.
    // a similar approach is taken for more miscellaneous operators like RELATIVE_TO_ABSOLUTE_ADDRESS as for the NOT operator.
    // the rest (plus, minus, etc) pop the top of the stack and use it as the 'y' value for the operator.
    // the 'x' is now at the top, gets popped, operator is applied and mutated value is put back.
    // this is why these operators do not have a function defined in this enum
    NOT((byte) 0xA0),
    PLUS((byte) 0xA1),
    MINUS((byte) 0xA2),
    MULTIPLY((byte) 0xA3),
    DIVIDE((byte) 0xA4),
    MODULO((byte) 0xA5),
    LOGICAL_AND((byte) 0xA6),
    LOGICAL_OR((byte) 0xA7),
    BIT_AND((byte) 0xA8),
    BIT_OR((byte) 0xA9),
    BIT_XOR((byte) 0xAA),
    BITSHIFT_LEFT((byte) 0xAB),
    BITSHIFT_RIGHT((byte) 0xAC),
    EQUALS((byte) 0xAD),
    LESS_THAN((byte) 0xAE),
    GREATER_THAN((byte) 0xAF),
    LESS_THAN_OR_EQUAL((byte) 0xB0),
    GREATER_THAN_OR_EQUAL((byte) 0xB1),
    BIT_NOT((byte) 0xB2),
    CAST((byte) 0xB3),
    NATIVE_FUNCTION_DEFINITION_BEGIN((byte) 0xB4),
    TRACEBACK((byte) 0xB5),
    STDLIB_FINISH_LINE((byte) 0xB6),

    DECLARE_VARIABLE((byte) 0xC0),
    DEFINE_VARIABLE((byte) 0xC1),
    VALUE_AT_ADDRESS((byte) 0xC2),
    FUNCTION_DEFINITION_BEGIN((byte) 0xC3),
    FUNCTION_DEFINITION_END((byte) 0xC4),
    FUNCTION_CALL((byte) 0xC5),
    RETURN_STATEMENT((byte) 0xC6),
    MUTATE_VARIABLE((byte) 0xC7),
    INC_VARIABLE((byte) 0xD7),
    DEC_VARIABLE((byte) 0xD8),
    ENUM_DEFINITION_BEGIN((byte) 0xC8),
    ENUM_DEFINITION_END((byte) 0xC9),
    ENUM_MEMBER_DEFINITION((byte) 0xCA),
    MUTATE_VARIABLE_AND_PUSH((byte) 0xCB),
    INC_VARIABLE_AND_PUSH((byte) 0xD9),
    DEC_VARIABLE_AND_PUSH((byte) 0xDA),

    STRING_VALUE((byte) 0xCC),
    INTEGER_VALUE((byte) 0xCD),
    LONG_INTEGER_VALUE((byte) 0xCE),
    FLOAT_VALUE((byte) 0xCF),
    DOUBLE_VALUE((byte) 0xD0),
    BYTE_VALUE((byte) 0xD1),
    BOOL_VALUE((byte) 0xD2),
    ENUM_VALUE((byte) 0xD3),
    NULL_VALUE((byte) 0xD4),
    CHARACTER_VALUE((byte) 0xD5),

    RELATIVE_TO_ABSOLUTE_ADDRESS((byte) 0xD6);

    public static final byte BYTECODE_VERSION = 1;

    private final byte code;

    ByteCode(final byte code) {
        this.code = code;
    }

    public byte code() {
        return code;
    }

    public static Optional<ByteCode> of(final char code) {
        return of((byte) code);
    }

    public static Optional<ByteCode> of(final byte code) {
        return Arrays.stream(values()).filter(b -> b.code == code).findFirst();
    }

    public static byte[] longToBytes(final long l) {
        return Longs.toByteArray(l);
    }

    public static long bytesToLong(final byte[] bytes) {
        return Longs.fromByteArray(bytes);
    }

    public static byte[] intToBytes(final int i) {
        return Ints.toByteArray(i);
    }

    public static int bytesToInt(final byte[] bytes) {
        return Ints.fromByteArray(bytes);
    }

    public static byte[] floatToBytes(final float f) {
        final ByteBuffer buffer = ByteBuffer.allocate(Float.BYTES);
        buffer.putFloat(f);
        return buffer.array();
    }

    public static float bytesToFloat(final byte[] bytes) {
        final ByteBuffer buffer = ByteBuffer.allocate(Float.BYTES);
        buffer.put(bytes);
        buffer.flip();
        return buffer.getFloat();
    }

    public static byte[] doubleToBytes(final double d) {
        final ByteBuffer buffer = ByteBuffer.allocate(Double.BYTES);
        buffer.putDouble(d);
        return buffer.array();
    }

    public static double bytesToDouble(final byte[] bytes) {
        final ByteBuffer buffer = ByteBuffer.allocate(Double.BYTES);
        buffer.put(bytes);
        buffer.flip();
        return buffer.getDouble();
    }

    public static ByteCodeInstruction enumMember(@NotNull final ByteCodeEnumMember member) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(ENUM_VALUE.code);
            this.addAll(Arrays.stream(ArrayUtils.toObject(intToBytes(member.enumId()))).toList());
            this.addAll(Arrays.stream(ArrayUtils.toObject(intToBytes(member.ordinal()))).toList());
        }});
    }

    public static ByteCodeEnumMember ofEnumMember(@NotNull final ByteCodeInstruction instr) {
        final List<Byte> sub = List.of(instr.codes()).subList(1, instr.codes().length - 1);
        final int enumId = bytesToInt(ArrayUtils.toPrimitive(sub.subList(0, 4).toArray(new Byte[0])));
        final int member = bytesToInt(ArrayUtils.toPrimitive(sub.subList(4, 8).toArray(new Byte[0])));
        return new ByteCodeEnumMember(enumId, member);
    }

    public static List<ByteCodeInstruction> defineEnum(@NotNull final ByteCodeEnum enumDef) {
        return new ArrayList<>() {
            {
                this.add(new ByteCodeInstruction(ENUM_DEFINITION_BEGIN.code));
                this.addAll(enumDef
                        .members()
                        .stream()
                        .map(m -> new ByteCodeInstruction(new ArrayList<>() {{
                            this.add(ENUM_MEMBER_DEFINITION.code);
                            final List<Byte> memberName = List.of(string(m).codes());
                            this.addAll(memberName.subList(0, memberName.size() - 1));
                        }}))
                        .toList());
                this.add(new ByteCodeInstruction(ENUM_DEFINITION_END.code));
            }
        };
    }

    public static ByteCodeInstruction nullValue() {
        return new ByteCodeInstruction(NULL_VALUE.code);
    }

    public static ByteCodeInstruction floating(final float literal) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(FLOAT_VALUE.code);
            this.addAll(Arrays.stream(ArrayUtils.toObject(floatToBytes(literal))).toList());
        }});
    }

    public static ByteCodeInstruction doubleFloating(final double literal) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(DOUBLE_VALUE.code);
            this.addAll(Arrays.stream(ArrayUtils.toObject(doubleToBytes(literal))).toList());
        }});
    }

    public static Byte[] doubleFloating(@NotNull final String value) {
        return doubleFloating(Tokenizer.isDouble(value) != null ? Double.parseDouble(value) : 0d).codes();
    }

    public static Byte[] floating(@NotNull final String value) {
        return floating(Tokenizer.isFloat(value) != null ? Float.parseFloat(value) : 0f).codes();
    }

    public static double ofFloat(@NotNull final ByteCodeInstruction instr) {
        return bytesToFloat(ArrayUtils.toPrimitive(Arrays.stream(instr.codes()).toList().subList(1, instr.codes().length - 1).toArray(new Byte[0])));
    }

    public static ByteCodeInstruction longInteger(final long literal) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(LONG_INTEGER_VALUE.code);
            this.addAll(Arrays.stream(ArrayUtils.toObject(longToBytes(literal))).toList());
        }});
    }

    public static ByteCodeInstruction integer(final int literal) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(INTEGER_VALUE.code);
            this.addAll(Arrays.stream(ArrayUtils.toObject(intToBytes(literal))).toList());
        }});
    }

    public static ByteCodeInstruction stdlibFinishLine(final int line) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(STDLIB_FINISH_LINE.code);
            this.addAll(Arrays.stream(ArrayUtils.toObject(intToBytes(line))).toList());
        }});
    }

    public static ByteCodeInstruction traceback(final int line) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(TRACEBACK.code);
            this.addAll(Arrays.stream(ArrayUtils.toObject(intToBytes(line))).toList());
        }});
    }

    public static ByteCodeInstruction character(final int literal) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(CHARACTER_VALUE.code);
            this.addAll(Arrays.stream(ArrayUtils.toObject(intToBytes(literal))).toList());
        }});
    }

    public static ByteCodeInstruction byteValue(final byte literal) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(BYTE_VALUE.code);
            this.add(literal);
        }});
    }

    public static ByteCodeInstruction boolValue(final boolean literal) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(BOOL_VALUE.code);
            this.addAll(List.of(ArrayUtils.toObject(intToBytes(literal ? 1 : 0))));
        }});
    }

    public static Byte[] longInteger(@NotNull final String value) {
        return longInteger(Tokenizer.isLong(value) != null ? Tokenizer.isLong(value) : 0L).codes();
    }

    public static Byte[] integer(@NotNull final String value) {
        return integer(Tokenizer.isInt(value) != null ? Tokenizer.isInt(value) : 0).codes();
    }

    public static Byte[] character(@NotNull final String value) {
        return character((value.startsWith("'") ? value.charAt(1) : Integer.parseInt(value))).codes(); // integer, because characters support unicode
    }

    public static Byte[] bool(@NotNull final String value) {
        return boolValue(value.equals("1b")).codes();
    }

    public static ByteCodeInstruction string(@NotNull final String literal) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(STRING_VALUE.code);
            final byte[] stringBytes = literal.getBytes(StandardCharsets.ISO_8859_1);
            this.addAll(Arrays.stream(ArrayUtils.toObject(intToBytes(stringBytes.length))).toList());
            this.addAll(Arrays.stream(ArrayUtils.toObject(stringBytes)).toList());
        }});
    }

    public static Byte[] stringToBytes(@NotNull final String literal) {
        final Byte[] codes = string(literal).codes();
        return Arrays.stream(codes).toList().subList(1, codes.length - 1).toArray(new Byte[0]);
    }

    public static String bytesToString(@NotNull final ByteCodeInstruction instr) { // 32 bit int length = 4 bytes + 1 byte for the STRING_LITERAL code = 5 bytes offset until the actual string
        // but remove last byte, since that will be the INSTRUCTION_FINISH code
        return new String(ArrayUtils.toPrimitive(Arrays.stream(instr.codes()).toList().subList(5, instr.codes().length - 1).toArray(new Byte[0])), StandardCharsets.ISO_8859_1);
    }

    public static String bytesToString(final byte[] arr) {
        return new String(arr, StandardCharsets.ISO_8859_1);
    }

    public static String bytesToString(final byte[] arr, final boolean removeLengthBytes) {
        return new String(ArrayUtils.toPrimitive(Arrays.stream(ArrayUtils.toObject(arr)).toList().subList(4, arr.length).toArray(new Byte[0])), StandardCharsets.ISO_8859_1);
    }

    public static ByteCodeInstruction call(final long id) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(FUNCTION_CALL.code);
            this.addAll(Arrays.stream(ArrayUtils.toObject(longToBytes(id))).toList());
        }});
    }

    public static ByteCodeInstruction function(@NotNull final String name) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(FUNCTION_DEFINITION_BEGIN.code);
            this.addAll(Arrays.stream(string(name).codes()).toList());
        }});
    }

    public static ByteCodeInstruction nativeFunction(@NotNull final String javaMethod) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(NATIVE_FUNCTION_DEFINITION_BEGIN.code);
            this.addAll(Arrays.stream(string(javaMethod).codes()).toList());
            this.add(FUNCTION_DEFINITION_END.code);
        }});
    }

    public static ByteCodeInstruction defineVariable(@NotNull final ByteDatatype type) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(DEFINE_VARIABLE.code);
            this.addAll(List.of(type.bytes()));
        }});
    }

    public static ByteCodeInstruction declareVariable(@NotNull final ByteDatatype type) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(DECLARE_VARIABLE.code);
            this.addAll(List.of(type.bytes()));
        }});
    }

    public static ByteCodeInstruction cast(@NotNull final ByteDatatype type) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(CAST.code);
            this.addAll(List.of(type.bytes()));
        }});
    }

    public static ByteCodeInstruction pop(final int amount) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(POP.code);
            this.addAll(Arrays.stream(ArrayUtils.toObject(intToBytes(amount))).toList());
        }});
    }

    public static ByteCodeInstruction header() {
        return new ByteCodeInstruction(
                PROGRAM_HEADER.code, (byte) 0x00, (byte) 0x6D, (byte) 0x00, (byte) 0x75, BYTECODE_VERSION
        );
    }

    public static ByteCodeInstruction jumpIf(final int to) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(JUMP_IF.code);
            this.addAll(List.of(ArrayUtils.toObject(intToBytes(to))));
        }});
    }

    public static ByteCodeInstruction mainFunction(final long funcId) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(MAIN_FUNCTION.code);
            this.addAll(List.of(ArrayUtils.toObject(longToBytes(funcId))));
        }});
    }

    public static ByteCodeInstruction push(@NotNull final Byte... value) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(PUSH.code);
            this.addAll(List.of(value).subList(0, value.length - 1));
        }});
    }

    public static ByteCodeInstruction jump(final int to) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(JUMP.code);
            this.addAll(List.of(ArrayUtils.toObject(intToBytes(to))));
        }});
    }

}
