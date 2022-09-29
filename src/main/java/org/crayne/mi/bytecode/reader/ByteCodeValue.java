package org.crayne.mi.bytecode.reader;

import org.crayne.mi.bytecode.common.ByteDatatype;

import java.util.Arrays;

public record ByteCodeValue(ByteDatatype type, Byte[] value) {

    public String toString() {
        return "ByteCodeValue{" +
                "type=" + type +
                ", value=" + String.join(", ", Arrays.stream(value).map(ByteCodeReader::byteToHexString).toList()) +
                '}';
    }
}
