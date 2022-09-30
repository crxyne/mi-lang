package org.crayne.mi.bytecode.reader;

import com.google.common.primitives.Ints;
import org.apache.commons.lang3.ArrayUtils;
import org.crayne.mi.bytecode.common.ByteCodeException;
import org.crayne.mi.bytecode.common.ByteDatatype;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public record ByteCodeValue(ByteDatatype type, Byte[] value) {

    private static ByteCodeValue boolValue(final boolean cond) {
        return new ByteCodeValue(ByteDatatype.BOOL, ArrayUtils.toObject(Ints.toByteArray(cond ? 1 : 0)));
    }

    public ByteCodeValue not() {
        if (type.id() != ByteDatatype.BOOL.id()) throw new ByteCodeException("Expected boolean value for 'not' operator");
        final int val = Ints.fromByteArray(ArrayUtils.toPrimitive(value));
        return boolValue(val == 0);
    }

    public ByteCodeValue equal(@NotNull final ByteCodeValue other) {
        return boolValue(type.id() == other.type.id() && Arrays.equals(value, other.value));
    }

    public String toString() {
        return "ByteCodeValue{" +
                "type=" + type +
                ", value=" + String.join(", ", Arrays.stream(value).map(ByteCodeReader::byteToHexString).toList()) +
                '}';
    }
}
