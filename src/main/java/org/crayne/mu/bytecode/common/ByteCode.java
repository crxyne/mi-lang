package org.crayne.mu.bytecode.common;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import org.apache.commons.lang3.ArrayUtils;
import org.crayne.mu.parsing.lexer.Tokenizer;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

@SuppressWarnings("unused")
public enum ByteCode {

    INSTRUCT_FINISH((byte) 0xFF),
    PROGRAM_HEADER((byte) 0x01),
    JUMP((byte) 0x03),
    JUMP_IF((byte) 0x04),
    PUSH_SAVED_LABEL((byte) 0x05),
    POP_SAVED_LABEL((byte) 0x06),
    PUSH((byte) 0x07),
    POP((byte) 0x08),

    RELATIVE_TO_ABSOLUTE_ADDRESS((byte) 0x09),

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
    CAST((byte) 0xB2),
    NATIVE_FUNCTION_DEFINITION_BEGIN((byte) 0xB3),

    DECLARE_VARIABLE((byte) 0xC0),
    DEFINE_VARIABLE((byte) 0xC1),
    VALUE_AT_ADDRESS((byte) 0xC2),
    FUNCTION_DEFINITION_BEGIN((byte) 0xC3),
    FUNCTION_DEFINITION_END((byte) 0xC4),
    FUNCTION_CALL((byte) 0xC5),
    RETURN_STATEMENT((byte) 0xC6),
    MUTATE_VARIABLE((byte) 0xC7),

    STRING_VALUE((byte) 0xC8),
    INTEGER_VALUE((byte) 0xC9),
    FLOAT_VALUE((byte) 0xCA),
    ENUM_VALUE((byte) 0xCB),
        /* NOTE for enums, map all enums to a unique id which will be a replacement for an entire ass name,
               taking up less bytes. for names, save those somewhere, so that you can cast an enum ordinal to string to get the name back, instead of a number
       */
    ENUM_DEFINITION((byte) 0xCC);

    public static final int BYTECODE_VERSION = 1;

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

    public static byte[] floatToBytes(final double d) {
        final ByteBuffer buffer = ByteBuffer.allocate(Double.BYTES);
        buffer.putDouble(d);
        return buffer.array();
    }

    public static double bytesToFloat(final byte[] bytes) {
        final ByteBuffer buffer = ByteBuffer.allocate(Double.BYTES);
        buffer.put(bytes);
        buffer.flip();
        return buffer.getDouble();
    }

    public static ByteCodeInstruction enumMember(@NotNull final ByteCodeEnumMember member) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(ENUM_VALUE.code);
            this.addAll(Arrays.stream(ArrayUtils.toObject(intToBytes(member.enumId()))).toList());
            this.addAll(Arrays.stream(ArrayUtils.toObject(longToBytes(member.ordinal()))).toList());
        }});
    }

    public static ByteCodeEnumMember ofEnumMember(@NotNull final ByteCodeInstruction instr) {
        final List<Byte> sub = List.of(instr.codes()).subList(1, instr.codes().length - 1);
        final int enumId = bytesToInt(ArrayUtils.toPrimitive(sub.subList(0, 4).toArray(new Byte[0])));
        final long member = bytesToLong(ArrayUtils.toPrimitive(sub.subList(4, 12).toArray(new Byte[0])));
        return new ByteCodeEnumMember(enumId, member);
    }

    public static ByteCodeInstruction floating(final double literal) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(FLOAT_VALUE.code);
            this.addAll(Arrays.stream(ArrayUtils.toObject(floatToBytes(literal))).toList());
        }});
    }

    public static Byte[] floating(@NotNull final String value) {
        return floating(Tokenizer.isDouble(value) != null ? Double.parseDouble(value) : 0d).codes();
    }

    public static Byte[] doubleFloating(@NotNull final String value) {
        return floating(Tokenizer.isFloat(value) != null ? Float.parseFloat(value) : 0f).codes();
    }

    public static double ofFloat(@NotNull final ByteCodeInstruction instr) {
        return bytesToFloat(ArrayUtils.toPrimitive(Arrays.stream(instr.codes()).toList().subList(1, instr.codes().length - 1).toArray(new Byte[0])));
    }

    public static ByteCodeInstruction integer(final long literal) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(INTEGER_VALUE.code);
            this.addAll(Arrays.stream(ArrayUtils.toObject(longToBytes(literal))).toList());
        }});
    }

    public static Byte[] longInteger(@NotNull final String value) {
        return integer(Tokenizer.isLong(value) != null ? Long.parseLong(value) : 0L).codes();
    }

    public static Byte[] integer(@NotNull final String value) {
        return integer(Tokenizer.isInt(value) != null ? Integer.parseInt(value) : 0).codes();
    }

    public static Byte[] character(@NotNull final String value) {
        return integer((value.startsWith("'") ? value.charAt(1) : Integer.parseInt(value))).codes();
    }

    public static Byte[] bool(@NotNull final String value) {
        return integer(value.equals("1b") ? 1L : 0L).codes();
    }

    public static long ofInteger(@NotNull final ByteCodeInstruction instr) {
        return bytesToLong(ArrayUtils.toPrimitive(Arrays.stream(instr.codes()).toList().subList(1, instr.codes().length - 1).toArray(new Byte[0])));
    }

    public static ByteCodeInstruction string(@NotNull final String literal) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(STRING_VALUE.code);
            this.addAll(Arrays.stream(ArrayUtils.toObject(intToBytes(literal.length()))).toList());
            this.addAll(Arrays.stream(ArrayUtils.toObject(literal.getBytes(StandardCharsets.UTF_8))).toList());
        }});
    }

    public static String ofString(@NotNull final ByteCodeInstruction instr) {                     // 32 bit int length = 4 bytes + 1 byte for the STRING_LITERAL code = 5 bytes offset until the actual string
        // but remove last byte, since that will be the INSTRUCTION_FINISH code
        return new String(ArrayUtils.toPrimitive(Arrays.stream(instr.codes()).toList().subList(5, instr.codes().length - 1).toArray(new Byte[0])), StandardCharsets.UTF_8);
    }

    public static ByteCodeInstruction call(final long id) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(FUNCTION_CALL.code);
            this.addAll(Arrays.stream(ArrayUtils.toObject(longToBytes(id))).toList());
        }});
    }

    public static ByteCodeInstruction function(final long id) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(FUNCTION_DEFINITION_BEGIN.code);
            this.addAll(Arrays.stream(ArrayUtils.toObject(longToBytes(id))).toList());
        }});
    }

    public static ByteCodeInstruction nativeFunction(final long id, @NotNull final String javaMethod) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(NATIVE_FUNCTION_DEFINITION_BEGIN.code);
            this.addAll(Arrays.stream(ArrayUtils.toObject(longToBytes(id))).toList());
            this.addAll(Arrays.stream(string(javaMethod).codes()).toList());
            this.add(FUNCTION_DEFINITION_END.code);
        }});
    }

    public static ByteCodeInstruction defineVariable(@NotNull final ByteDatatype type) {
        return new ByteCodeInstruction(DEFINE_VARIABLE.code, type.code());
    }

    public static ByteCodeInstruction declareVariable(@NotNull final ByteDatatype type) {
        return new ByteCodeInstruction(DECLARE_VARIABLE.code, type.code());
    }

    public static ByteCodeInstruction cast(@NotNull final ByteDatatype type) {
        return new ByteCodeInstruction(CAST.code, type.code());
    }

    public static ByteCodeInstruction pop(final int amount) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(POP.code);
            this.addAll(Arrays.stream(ArrayUtils.toObject(intToBytes(amount))).toList());
        }});
    }

    public static ByteCodeInstruction header() {
        return new ByteCodeInstruction(
                PROGRAM_HEADER.code, (byte) 0x0, (byte) 0x6D, (byte) 0x0, (byte) 0x75, (byte) BYTECODE_VERSION
        );
    }

    public static ByteCodeInstruction jumpIf(final long to) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(JUMP_IF.code);
            this.addAll(List.of(ArrayUtils.toObject(longToBytes(to))));
        }});
    }

    public static ByteCodeInstruction push(@NotNull final Byte... value) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(PUSH.code);
            this.addAll(List.of(value).subList(0, value.length - 1));
        }});
    }

    public static ByteCodeInstruction jump(final long to) {
        return new ByteCodeInstruction(new ArrayList<>() {{
            this.add(JUMP.code);
            this.addAll(List.of(ArrayUtils.toObject(longToBytes(to))));
        }});
    }

}
