package org.crayne.mu.runtime.lang;

import org.jetbrains.annotations.NotNull;

public class RDatatype {

    private final String name;

    public RDatatype(@NotNull final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "RDatatype{" +
                "name='" + name + '\'' +
                '}';
    }

}
