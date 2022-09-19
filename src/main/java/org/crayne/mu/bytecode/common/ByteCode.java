package org.crayne.mu.bytecode.common;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum ByteCode {

    PROGRAM_HEADER((byte) 0x01) {

        public ByteCodeInstruction header() {
            return new ByteCodeInstruction(
                    PROGRAM_HEADER.code, (byte) 0x0, (byte) 0x6D, (byte) 0x0, (byte) 0x75, (byte) bytecodeVersion
            );
        }

    },
    INSTRUCT_FINISH((byte) 0x02),
    JUMP((byte) 0x03) {

        public ByteCodeInstruction jump(final long to) {
            return new ByteCodeInstruction(new ArrayList<>() {{
                this.add(JUMP.code);
                this.addAll(List.of(ArrayUtils.toObject(longToBytes(to))));
            }});
        }

    },
    JUMP_IF((byte) 0x04),
    SET_SAVED_LABEL((byte) 0x05),
    GET_SAVED_LABEL((byte) 0x06),
    PUSH((byte) 0x07),

    DECLARE_VARIABLE((byte) 0xC0),
    DEFINE_VARIABLE((byte) 0xC1),
    VALUEOF_VARIABLE((byte) 0xC2),
    FUNCTION_DEFINITION_BEGIN((byte) 0xC3),
    FUNCTION_DEFINITION_END((byte) 0xC4),
    FUNCTION_CALL((byte) 0xC5),
    FUNCTION_PASS_ARGUMENT((byte) 0xC6),
    RETURN_STATEMENT((byte) 0xC7),

    STRING_VALUE((byte) 0xB0) {

        public ByteCodeInstruction string(@NotNull final String literal) {
            return new ByteCodeInstruction(new ArrayList<>() {{
                this.add(STRING_VALUE.code);
                this.addAll(Arrays.stream(ArrayUtils.toObject(intToBytes(literal.length()))).toList());
                this.addAll(Arrays.stream(ArrayUtils.toObject(literal.getBytes(StandardCharsets.UTF_8))).toList());
            }});
        }

        public String of(@NotNull final ByteCodeInstruction instr) {                     // 32 bit int length = 4 bytes + 1 byte for the STRING_LITERAL code = 5 bytes offset until the actual string
                                                                                         // but remove last byte, since that will be the INSTRUCTION_FINISH code
            return new String(ArrayUtils.toPrimitive(Arrays.stream(instr.codes()).toList().subList(5, instr.codes().length - 1).toArray(new Byte[0])), StandardCharsets.UTF_8);
        }

    },
    INTEGER_VALUE((byte) 0xB1),
    FLOAT_VALUE((byte) 0xB2),
    ENUM_VALUE((byte) 0xB3);

    private static final int bytecodeVersion = 1;

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
        final ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(l);
        return buffer.array();
    }

    public static long bytesToLong(final byte[] bytes) {
        final ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(bytes);
        buffer.flip();
        return buffer.getLong();
    }

    public static byte[] intToBytes(final int i) {
        final ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.putInt(i);
        return buffer.array();
    }

    public static int bytesToInt(final byte[] bytes) {
        final ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.put(bytes);
        buffer.flip();
        return buffer.getInt();
    }

    public ByteCodeInstruction string(@NotNull final String literal) {
        throw new IllegalArgumentException("Unimplemented");
    }

    public String of(@NotNull final ByteCodeInstruction instr) {
        throw new IllegalArgumentException("Unimplemented");
    }

    public ByteCodeInstruction jump(final long to) {
        throw new IllegalArgumentException("Unimplemented");
    }

    public ByteCodeInstruction header() {
        throw new IllegalArgumentException("Unimplemented");
    }

}
