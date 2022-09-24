package org.crayne.mu.bytecode.common;

public record ByteCodeEnumMember(int enumId, int ordinal) {

    public int enumId() {
        return enumId;
    }

    public int ordinal() {
        return ordinal;
    }

    public String toString() {
        return "enum(" + enumId + "::" + ordinal + ")";
    }
}
