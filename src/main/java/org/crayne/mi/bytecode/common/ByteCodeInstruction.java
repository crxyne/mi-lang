package org.crayne.mi.bytecode.common;

import org.apache.commons.lang3.ArrayUtils;
import org.crayne.mi.bytecode.reader.ByteCodeReader;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class ByteCodeInstruction {

    private final Byte[] codes;

    public ByteCodeInstruction(@NotNull final Collection<Byte> codes) {
        if (codes.isEmpty()) throw new IllegalArgumentException("Expected bytecode values for instruction, received empty array");

        final List<Byte> codeList = new ArrayList<>(codes);
        codeList.add(ByteCode.INSTRUCT_FINISH.code());
        this.codes = codeList.toArray(new Byte[0]);
    }

    public ByteCodeInstruction(@NotNull final Byte... codes) {
        if (codes.length == 0) throw new IllegalArgumentException("Expected bytecode values for instruction, received empty array");

        final List<Byte> codeList = new ArrayList<>(List.of(codes.clone()));
        codeList.add(ByteCode.INSTRUCT_FINISH.code());
        this.codes = codeList.toArray(new Byte[0]);
    }

    public Optional<ByteCode> type() {
        return ByteCode.of(codes[0]);
    }

    public Byte[] codes() {
        return codes;
    }

    public static String write(@NotNull final ByteCodeInstruction instr) {
        return new String(ArrayUtils.toPrimitive(instr.codes), StandardCharsets.ISO_8859_1);
    }

    public String write() {
        return ByteCodeInstruction.write(this);
    }

    public static ByteCodeInstruction read(@NotNull final String instr) {
        return new ByteCodeInstruction(
                Arrays.stream(ArrayUtils.toObject(instr.getBytes(StandardCharsets.ISO_8859_1)))
                        .toList()
                        .subList(0, instr.length() - 1)
                        .toArray(new Byte[0])
        );
    }

    public String toString() {
        final Optional<ByteCode> type = type();
        if (type.isEmpty()) return ";";
        if (codes.length == 2) return type.get().name().toLowerCase() + " ;";

        return type.get().name().toLowerCase() + " " + String.join(" ",
                Arrays.stream(codes)
                        .toList()
                        .subList(1, codes.length - 1)
                        .stream()
                        .map(ByteCodeReader::byteToHexString)
                        //.map(b -> "" + (int) b)
                        .toList()
        ).trim() + " ;";
    }

}
