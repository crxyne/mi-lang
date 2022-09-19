package org.crayne.mu.bytecode.common;

public enum ByteDatatype {

    BOOL((byte) 0x00),
    CHAR((byte) 0x01),
    INT((byte) 0x02),
    LONG((byte) 0x03),
    FLOAT((byte) 0x04),
    DOUBLE((byte) 0x05),
    STRING((byte) 0x06),
    ENUM((byte) 0x07);

    private final byte code;

    ByteDatatype(final byte code) {
        this.code = code;
    }

    public byte code() {
        return code;
    }


}
