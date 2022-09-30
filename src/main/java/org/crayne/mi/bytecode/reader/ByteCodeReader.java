package org.crayne.mi.bytecode.reader;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.ArrayUtils;
import org.crayne.mi.bytecode.common.ByteCode;
import org.crayne.mi.bytecode.common.ByteCodeException;
import org.crayne.mi.bytecode.common.ByteCodeInstruction;
import org.crayne.mi.log.MessageHandler;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ByteCodeReader {

    private int currentBytePos = -1;
    private byte currentByte = 0;
    private final List<Byte> bytecodeProgram;
    private final List<ByteCodeInstruction> instructionSet;
    private final MessageHandler messageHandler;

    private static List<Byte> bytes(@NotNull final String s) {
        return List.of(ArrayUtils.toObject(s.getBytes(StandardCharsets.ISO_8859_1)));
    }

    public ByteCodeReader(@NotNull final String bytecodeProgram, @NotNull final MessageHandler messageHandler) {
        this.bytecodeProgram = bytes(bytecodeProgram);
        instructionSet = new ArrayList<>();
        this.messageHandler = messageHandler;
    }

    public ByteCodeReader(@NotNull final File bytecodeFile, @NotNull final MessageHandler messageHandler) throws IOException {
        this.bytecodeProgram = bytes(Files.readString(bytecodeFile.toPath(), StandardCharsets.ISO_8859_1));
        instructionSet = new ArrayList<>();
        this.messageHandler = messageHandler;
    }

    public List<ByteCodeInstruction> read() {
        try {
            next();
            readHeader();
            while (currentBytePos < bytecodeProgram.size()) {
                final ByteCode code = byteCodeOfByte(currentByte);
                next();
                switch (code) {
                    case PUSH -> readPushInstruction(code);
                    case DEFINE_VARIABLE, DECLARE_VARIABLE, CAST -> readVariablar(code);
                    case JUMP, JUMP_IF, POP, FUNCTION_CALL, MAIN_FUNCTION -> readWithInteger(code);
                    case NATIVE_FUNCTION_DEFINITION_BEGIN -> readNativeFunctionBegin(code);
                    case ENUM_DEFINITION_BEGIN -> readEnumDefinitionBegin(code);
                    case ENUM_MEMBER_DEFINITION -> readEnumMemberDefinition(code);
                    case FUNCTION_DEFINITION_BEGIN, FUNCTION_DEFINITION_END, VALUE_AT_ADDRESS, EQUALS, NOT, PLUS, MINUS, MULTIPLY, DIVIDE, MODULO,
                            BIT_AND, BIT_OR, BIT_XOR, BIT_NOT, LOGICAL_AND, LOGICAL_OR, LESS_THAN, LESS_THAN_OR_EQUAL, GREATER_THAN, GREATER_THAN_OR_EQUAL,
                            MUTATE_VARIABLE, MUTATE_VARIABLE_AND_PUSH, BITSHIFT_LEFT, BITSHIFT_RIGHT, VALUE_AT_RELATIVE_ADDRESS, RETURN_STATEMENT, ENUM_DEFINITION_END // any of the instructions that dont pass any arguments in should just be added to instruction set
                            -> instruction(code, (l) -> {});
                    default -> throw new ByteCodeException("Unhandled bytecode instruction " + code);
                }
                expect(ByteCode.INSTRUCT_FINISH);
            }
        } catch (final Throwable e) {
            messageHandler.errorMsg("Caught an error while parsing mi bytecode: " + e.getMessage());
            return new ArrayList<>();
        }
        return instructionSet;
    }

    private void instruction(@NotNull final ByteCode code, final Consumer<ArrayList<Byte>> consumer) {
        instructionSet.add(new ByteCodeInstruction(new ArrayList<>() {{
            this.add(code.code());
            consumer.accept(this);
        }}));
    }

    private void readPushInstruction(@NotNull final ByteCode code) throws Throwable {
        final byte datatype = currentByte;
        final Byte[] val = readValue();
        instruction(code, (l) -> {
            l.add(datatype);
            l.addAll(listOfByteArray(val));
        });
    }

    private void readVariablar(@NotNull final ByteCode code) {
        final byte datatype = currentByte;
        next();
        instruction(code, (l) -> l.add(datatype));
    }

    private void readWithInteger(@NotNull final ByteCode code) {
        final Byte[] num = code == ByteCode.POP || code == ByteCode.JUMP || code == ByteCode.JUMP_IF ? readIntegerValue() : readLongIntegerValue();
        instruction(code, (l) -> l.addAll(listOfByteArray(num)));
    }

    private void readNativeFunctionBegin(@NotNull final ByteCode code) {
        expect(ByteCode.STRING_VALUE);
        final Byte[] nativeFunction = readStringValue();

        instruction(code, (l) -> {
            l.add(ByteCode.STRING_VALUE.code());
            l.addAll(listOfByteArray(nativeFunction));
            expect(ByteCode.INSTRUCT_FINISH, ByteCode.FUNCTION_DEFINITION_END);
            l.add(ByteCode.INSTRUCT_FINISH.code());
            l.add(ByteCode.FUNCTION_DEFINITION_END.code());
        });
    }

    private void readEnumDefinitionBegin(@NotNull final ByteCode code) {
        final Byte[] enumId = readIntegerValue();
        instruction(code, (l) -> l.addAll(listOfByteArray(enumId)));
    }

    private void readEnumMemberDefinition(@NotNull final ByteCode code) {
        final Byte[] memberOrdinal = readIntegerValue();
        expect(ByteCode.STRING_VALUE);
        final Byte[] memberName = readStringValue();
        instruction(code, (l) -> {
            l.addAll(listOfByteArray(memberOrdinal));
            l.add(ByteCode.STRING_VALUE.code());
            l.addAll(listOfByteArray(memberName));
        });
    }

    private static ByteCode byteCodeOfByte(final byte b) throws Throwable {
        return Arrays.stream(ByteCode.values()).filter(c -> c.code() == b).findFirst().orElseThrow((Supplier<Throwable>) () ->
                new ByteCodeException("Unrecognized bytecode instruction " + byteToHexString(b)));
    }

    private void readHeader() {
        final byte[] expectedHeader = new byte[] {ByteCode.PROGRAM_HEADER.code(), (byte) 0x00, (byte) 0x6D, (byte) 0x00, (byte) 0x75};
        expect(expectedHeader);
        final byte bytecodeVersion = currentByte;
        if (bytecodeVersion > ByteCode.BYTECODE_VERSION) throw new ByteCodeException("Unsupported bytecode version: " + bytecodeVersion);
        next();
        expect(ByteCode.INSTRUCT_FINISH);
        instructionSet.add(new ByteCodeInstruction(ByteCode.PROGRAM_HEADER.code(), (byte) 0x00, (byte) 0x6D, (byte) 0x00, (byte) 0x75, bytecodeVersion));
    }

    public static List<Byte> listOfByteArray(final byte... arr) {
        return listOfByteArray(ArrayUtils.toObject(arr));
    }

    public static List<Byte> listOfByteArray(final Byte... arr) {
        return List.of(arr);
    }

    private Byte[] readValue() throws Throwable {
        final ByteCode valueType = byteCodeOfByte(currentByte);
        expectAny(valueType, ByteCode.ENUM_VALUE, ByteCode.FLOAT_VALUE, ByteCode.INTEGER_VALUE, ByteCode.LONG_INTEGER_VALUE, ByteCode.STRING_VALUE, ByteCode.DOUBLE_VALUE, ByteCode.BOOL_VALUE);

        return switch (valueType) {
            case BOOL_VALUE, INTEGER_VALUE -> readIntegerValue();
            case LONG_INTEGER_VALUE -> readLongIntegerValue();
            case ENUM_VALUE -> readEnumValue();
            case FLOAT_VALUE -> readFloatValue();
            case DOUBLE_VALUE -> readDoubleValue();
            case STRING_VALUE -> readStringValue();
            default -> new Byte[0];
        };
    }

    private Byte[] readLongIntegerValue() {
        return readBytes(8);
    }

    private Byte[] readIntegerValue() {
        return readBytes(4);
    }

    private Byte[] readEnumValue() {
        return readBytes(8);
    }

    private Byte[] readDoubleValue() {
        return readBytes(8);
    }

    private Byte[] readFloatValue() {
        return readBytes(4);
    }

    private Byte[] readByteValue() {
        return readBytes(1);
    }

    private Byte[] readStringValue() {
        final List<Byte> result = new ArrayList<>();
        final Byte[] lengthBytes = readBytes(4);
        final int length = ByteCode.bytesToInt(ArrayUtils.toPrimitive(lengthBytes));
        final Byte[] stringBytes = readBytes(length);

        result.addAll(List.of(lengthBytes));
        result.addAll(List.of(stringBytes));
        return result.toArray(new Byte[0]);
    }

    private Byte[] readBytes(final int amount) {
        final List<Byte> result = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            result.add(currentByte);
            next();
        }
        return result.toArray(new Byte[0]);
    }

    private void expectAny(@NotNull final ByteCode code, @NotNull final ByteCode... possible) {
        expectAny(code.code(), ArrayUtils.toPrimitive(Arrays.stream(possible).map(ByteCode::code).toList().toArray(new Byte[0])));
    }

    private void expectAny(final byte b, @NotNull final ByteCode... possible) {
        expectAny(b, ArrayUtils.toPrimitive(Arrays.stream(possible).map(ByteCode::code).toList().toArray(new Byte[0])));
    }

    private void expectAny(final byte b, final byte... possible) {
        if (!List.of(ArrayUtils.toObject(possible)).contains(b))
            throw new ByteCodeException("Expected any of the possible bytes " +
                    Arrays.stream(ArrayUtils.toObject(possible)).map(ByteCodeReader::byteToHexString).toList()
                    + ", but got " + byteToHexString(b) + " instead");
        next();
    }

    private void expect(@NotNull final ByteCode... nextBytes) {
        expect(ArrayUtils.toPrimitive(Arrays.stream(nextBytes).map(ByteCode::code).toList().toArray(new Byte[0])));
    }

    private void expect(final byte... nextBytes) {
        for (final byte next : nextBytes) {
            if (currentByte != next)
                throw new ByteCodeException("Expected byte " + byteToHexString(next) + " at position " + currentBytePos + ", got " + byteToHexString(currentByte) + " instead");
            next();
        }
    }

    private void next() {
        if (currentBytePos >= bytecodeProgram.size()) throw new ByteCodeException("Reached end of file");
        currentBytePos++;
        currentByte = currentBytePos >= bytecodeProgram.size() ? 0 : bytecodeProgram.get(currentBytePos);
    }

    public static List<ByteCodeInstruction> read(@NotNull final String bytecode, @NotNull final MessageHandler messageHandler) {
        return new ByteCodeReader(bytecode, messageHandler).read();
    }

    public static List<ByteCodeInstruction> read(@NotNull final File bytecodeFile, @NotNull final MessageHandler messageHandler) throws Throwable {
        return new ByteCodeReader(bytecodeFile, messageHandler).read();
    }

    public static String byteToHexString(final byte b) {
        return String.format("%02x", b);
    }

    public String toString() {
        return "|-" + "-".repeat(32 * 3) + "|\n" + Lists.partition(bytecodeProgram
                .stream()
                .map(ByteCodeReader::byteToHexString)
                .toList(), 32)
                .stream()
                .map(l -> {
                    final String row = String.join(" ", l);
                    return "| " + row + " ".repeat(32 * 3 - row.length() - 1) + " |\n";
                })
                .collect(Collectors.joining()) + "|-" + "-".repeat(32 * 3) + "|";
    }
}
