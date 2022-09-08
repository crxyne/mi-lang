package org.crayne.mu.runtime.lang;

import org.crayne.mu.lang.Enum;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class REnum {

    private final List<String> members;
    private final String name;

    public REnum(@NotNull final String name, @NotNull final List<String> members) {
        this.name = name;
        this.members = members;
    }

    public String getName() {
        return name;
    }

    public List<String> getMembers() {
        return members;
    }

    public static REnum of(@NotNull final Enum e) {
        return new REnum(e.name(), e.members());
    }

    @Override
    public String toString() {
        return "REnum{" +
                "members=" + members +
                ", name='" + name + '\'' +
                '}';
    }

}
