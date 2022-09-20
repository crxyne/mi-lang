package org.crayne.mu.bytecode.common;

public record ByteCodeEnumMember(int enumId, long ordinal) {

    public int enumId() {
        return enumId;
    }

    public long ordinal() {
        return ordinal;
    }

    public String toString() {
        return "enum(" + enumId + "::" + ordinal + ")";
    }
}
