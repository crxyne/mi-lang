package org.crayne.mu.bytecode.common;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public enum ByteCode {

    PROGRAM_HEADER((byte) 0x01) {

        public ByteCodeInstruction header() {
            return new ByteCodeInstruction(
                    PROGRAM_HEADER.code, (byte) 0x0, (byte) 0x6D, (byte) 0x0, (byte) 0x75, (byte) BYTECODE_VERSION
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
    JUMP_IF((byte) 0x04) {

        public ByteCodeInstruction jump(final long to) {
            return new ByteCodeInstruction(new ArrayList<>() {{
                this.add(JUMP_IF.code);
                this.addAll(List.of(ArrayUtils.toObject(longToBytes(to))));
            }});
        }

    },
    PUSH_SAVED_LABEL((byte) 0x05),
    POP_SAVED_LABEL((byte) 0x06),
    PUSH((byte) 0x07) {

        public ByteCodeInstruction push(@NotNull final Byte... value) {
            return new ByteCodeInstruction(new ArrayList<>() {{
                this.add(PUSH.code);
                this.addAll(List.of(value));
            }});
        }

    },
    POP((byte) 0x08) {

        public ByteCodeInstruction pop(final int amount) {
            return new ByteCodeInstruction(new ArrayList<>() {{
                this.add(POP.code);
                this.addAll(Arrays.stream(ArrayUtils.toObject(intToBytes(amount))).toList());
            }});
        }

    },

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
    CAST((byte) 0xB2) {

        public ByteCodeInstruction cast(@NotNull final ByteDatatype type) {
            return new ByteCodeInstruction(CAST.code, type.code());
        }

    },

    DECLARE_VARIABLE((byte) 0xC0) {

        public ByteCodeInstruction declareVariable(@NotNull final ByteDatatype type) {
            return new ByteCodeInstruction(DECLARE_VARIABLE.code, type.code());
        }

    },
    DEFINE_VARIABLE((byte) 0xC1) {

        public ByteCodeInstruction defineVariable(@NotNull final ByteDatatype type) {
            return new ByteCodeInstruction(DEFINE_VARIABLE.code, type.code());
        }

    },
    VALUE_AT_ADDRESS((byte) 0xC2),
    FUNCTION_DEFINITION_BEGIN((byte) 0xC3) {

        public ByteCodeInstruction function(final int id) {
            return new ByteCodeInstruction(new ArrayList<>() {{
                this.add(FUNCTION_DEFINITION_BEGIN.code);
                this.addAll(Arrays.stream(ArrayUtils.toObject(intToBytes(id))).toList());
            }});
        }

    },
    FUNCTION_DEFINITION_END((byte) 0xC4),
    FUNCTION_CALL((byte) 0xC5) {

        public ByteCodeInstruction call(final int id) {
            return new ByteCodeInstruction(new ArrayList<>() {{
                this.add(FUNCTION_CALL.code);
                this.addAll(Arrays.stream(ArrayUtils.toObject(intToBytes(id))).toList());
            }});
        }

    },
    RETURN_STATEMENT((byte) 0xC6),
    MUTATE_VARIABLE((byte) 0xC7),

    STRING_VALUE((byte) 0xC8) {

        public ByteCodeInstruction string(@NotNull final String literal) {
            return new ByteCodeInstruction(new ArrayList<>() {{
                this.add(STRING_VALUE.code);
                this.addAll(Arrays.stream(ArrayUtils.toObject(intToBytes(literal.length()))).toList());
                this.addAll(Arrays.stream(ArrayUtils.toObject(literal.getBytes(StandardCharsets.UTF_8))).toList());
            }});
        }

        public String ofString(@NotNull final ByteCodeInstruction instr) {                     // 32 bit int length = 4 bytes + 1 byte for the STRING_LITERAL code = 5 bytes offset until the actual string
                                                                                         // but remove last byte, since that will be the INSTRUCTION_FINISH code
            return new String(ArrayUtils.toPrimitive(Arrays.stream(instr.codes()).toList().subList(5, instr.codes().length - 1).toArray(new Byte[0])), StandardCharsets.UTF_8);
        }

    },
    INTEGER_VALUE((byte) 0xC9) {

        public ByteCodeInstruction integer(final long literal) {
            return new ByteCodeInstruction(new ArrayList<>() {{
                this.add(INTEGER_VALUE.code);
                this.addAll(Arrays.stream(ArrayUtils.toObject(longToBytes(literal))).toList());
            }});
        }

        public long ofInteger(@NotNull final ByteCodeInstruction instr) {
            return bytesToLong(ArrayUtils.toPrimitive(Arrays.stream(instr.codes()).toList().subList(1, instr.codes().length - 1).toArray(new Byte[0])));
        }

    },
    FLOAT_VALUE((byte) 0xCA) {

        public ByteCodeInstruction decimal(final double literal) {
            return new ByteCodeInstruction(new ArrayList<>() {{
                this.add(FLOAT_VALUE.code);
                this.addAll(Arrays.stream(ArrayUtils.toObject(floatToBytes(literal))).toList());
            }});
        }

        public double ofDecimal(@NotNull final ByteCodeInstruction instr) {
            return bytesToFloat(ArrayUtils.toPrimitive(Arrays.stream(instr.codes()).toList().subList(1, instr.codes().length - 1).toArray(new Byte[0])));
        }

    },
    ENUM_VALUE((byte) 0xCB) {

        public ByteCodeInstruction enumMember(@NotNull final ByteCodeEnumMember member) {
            return new ByteCodeInstruction(new ArrayList<>() {{
                this.add(ENUM_VALUE.code);
                this.addAll(Arrays.stream(ArrayUtils.toObject(intToBytes(member.enumId()))).toList());
                this.addAll(Arrays.stream(ArrayUtils.toObject(longToBytes(member.ordinal()))).toList());
            }});
        }

        public ByteCodeEnumMember ofEnumMember(@NotNull final ByteCodeInstruction instr) {
            final List<Byte> sub = List.of(instr.codes()).subList(1, instr.codes().length - 1);
            final int enumId = bytesToInt(ArrayUtils.toPrimitive(sub.subList(0, 4).toArray(new Byte[0])));
            final long member = bytesToLong(ArrayUtils.toPrimitive(sub.subList(4, 12).toArray(new Byte[0])));
            return new ByteCodeEnumMember(enumId, member);
        }

    }; /* NOTE for enums, map all enums to a unique id which will be a replacement for an entire ass name,
               taking up less bytes. for names, save those somewhere, so that you can cast an enum ordinal to string to get the name back, instead of a number
       */

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

    public ByteCodeInstruction string(@NotNull final String literal) {
        throw new IllegalArgumentException("Unimplemented");
    }

    public String ofString(@NotNull final ByteCodeInstruction instr) {
        throw new IllegalArgumentException("Unimplemented");
    }

    public ByteCodeInstruction jump(final long to) {
        throw new IllegalArgumentException("Unimplemented");
    }

    public ByteCodeInstruction header() {
        throw new IllegalArgumentException("Unimplemented");
    }

    public ByteCodeInstruction enumMember(@NotNull final ByteCodeEnumMember member) {
        throw new IllegalArgumentException("Unimplemented");
    }

    public ByteCodeEnumMember ofEnumMember(@NotNull final ByteCodeInstruction instr) {
        throw new IllegalArgumentException("Unimplemented");
    }

    public ByteCodeInstruction integer(final long literal) {
        throw new IllegalArgumentException("Unimplemented");
    }

    public long ofInteger(@NotNull final ByteCodeInstruction instr) {
        throw new IllegalArgumentException("Unimplemented");
    }

    public ByteCodeInstruction decimal(final double literal) {
        throw new IllegalArgumentException("Unimplemented");
    }

    public double ofDecimal(@NotNull final ByteCodeInstruction instr) {
        throw new IllegalArgumentException("Unimplemented");
    }

    public ByteCodeInstruction defineVariable(@NotNull final ByteDatatype type) {
        throw new IllegalArgumentException("Unimplemented");
    }

    public ByteCodeInstruction declareVariable(@NotNull final ByteDatatype type) {
        throw new IllegalArgumentException("Unimplemented");
    }

    public ByteCodeInstruction function(final int id) {
        throw new IllegalArgumentException("Unimplemented");
    }

    public ByteCodeInstruction call(final int id) {
        throw new IllegalArgumentException("Unimplemented");
    }

    public ByteCodeInstruction push(@NotNull final Byte... value) {
        throw new IllegalArgumentException("Unimplemented");
    }

    public ByteCodeInstruction pop(final int amount) {
        throw new IllegalArgumentException("Unimplemented");
    }

    public ByteCodeInstruction cast(@NotNull final ByteDatatype type) {
        throw new IllegalArgumentException("Unimplemented");
    }

}
