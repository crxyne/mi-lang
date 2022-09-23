package org.crayne.mu;

import org.crayne.mu.bytecode.common.ByteCode;
import org.crayne.mu.bytecode.writer.ByteCodeCompiler;
import org.crayne.mu.runtime.MuProgram;
import org.crayne.mu.runtime.SyntaxTreeExecution;
import org.crayne.mu.stdlib.MuStandardLib;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class Tests {

    public static void main(@NotNull final String[] args) throws Exception {
        final Optional<Byte> duplicateByteCode = findFirstDuplicateBytecode();
        if (duplicateByteCode.isPresent()) {
            throw new Exception("Found duplicate bytecode: " + Integer.toHexString((int) duplicateByteCode.get()));
        }
        final String code = """
                
                """;

        final MuProgram muProgram = new MuProgram(System.out, true);
        final Optional<SyntaxTreeExecution> AST = muProgram.parse(MuStandardLib.standardLib(), code);
        if (AST.isEmpty()) return;

        final ByteCodeCompiler compiler = new ByteCodeCompiler(muProgram.messageHandler(), AST.get());
        compiler.compile();

    }

    private static Optional<Byte> findFirstDuplicateBytecode() {
        return firstDuplicate(Arrays.stream(ByteCode.values()).map(ByteCode::code).toList().toArray(new Byte[0]));
    }

    private static Optional<Byte> firstDuplicate(@NotNull final Byte[] array) {
        final Set<Byte> result = new HashSet<>();
        for (final Byte i : array) if (!result.add(i)) return Optional.of(i);
        return Optional.empty();
    }

}
