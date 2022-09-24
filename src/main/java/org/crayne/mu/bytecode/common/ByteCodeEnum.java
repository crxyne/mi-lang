package org.crayne.mu.bytecode.common;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ByteCodeEnum {

    private final int id;
    private final String name;
    private final List<String> members;

    public ByteCodeEnum(@NotNull final String name, final int id, @NotNull final String... members) {
        this.name = name;
        this.id = id;
        this.members = new ArrayList<>(List.of(members));
    }

    public ByteCodeEnum(@NotNull final String name, final int id, @NotNull final Collection<String> members) {
        this.name = name;
        this.id = id;
        this.members = new ArrayList<>(members);
    }

    public String name() {
        return name;
    }

    public List<String> members() {
        return members;
    }

    public int ordinalMember(@NotNull final String member) {
        return members.indexOf(member);
    }

    public int id() {
        return id;
    }

    public String toString() {
        return "ByteCodeEnum{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", members=" + members +
                '}';
    }

}
