package org.crayne.mu.runtime.lang;

import org.jetbrains.annotations.NotNull;

public class RValue {

    private final RDatatype type;
    private final Object value;

    public RValue(@NotNull final RDatatype type, @NotNull final Object value) {
        this.type = type;
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public RDatatype getType() {
        return type;
    }

    @Override
    public String toString() {
        return "RValue{" +
                "type=" + type +
                ", value=" + value +
                '}';
    }

}
