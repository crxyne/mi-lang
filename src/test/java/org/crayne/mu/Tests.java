package org.crayne.mu;

import org.crayne.mu.bytecode.common.ByteCode;
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
