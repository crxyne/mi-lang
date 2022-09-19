package org.crayne.mu.bytecode.common;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

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
        return Arrays.stream(instr.codes).map(b -> String.valueOf((char) b.byteValue())).collect(Collectors.joining(""));
    }

    public static ByteCodeInstruction read(@NotNull final String instr) {
        return new ByteCodeInstruction(
                instr
                .chars()
                .mapToObj(
                        c -> ByteCode.of((char) c)
                        .orElseThrow(IllegalArgumentException::new)
                                .code()
                )
                .toList()
                .toArray(new Byte[0])
        );
    }

}
