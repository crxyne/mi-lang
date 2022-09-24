package org.crayne.mu;

import org.crayne.mu.bytecode.common.ByteCode;
import org.crayne.mu.runtime.MuProgram;
import org.crayne.mu.stdlib.MuStandardLib;
import org.jetbrains.annotations.NotNull;

import java.io.File;
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
                module hi {
                    pub enum Result {
                        Ok, Err
                    }
                
                    pub fn main {
                        mut? i = 0;
                        ?i2 = i++;
                    }
                }
                """;

        final MuProgram muProgram = new MuProgram(System.out, true);
        final File writeTo = new File("mu-testing.mub");
        muProgram.compile(MuStandardLib.standardLib(), code, writeTo);
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
