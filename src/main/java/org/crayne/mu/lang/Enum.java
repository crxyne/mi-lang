package org.crayne.mu.lang;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Enum {

    private final List<String> members;

    public Enum(@NotNull final List<String> members) {
        this.members = new ArrayList<>(members);
    }

    public Enum() {
        this.members = new ArrayList<>();
    }

    public void add(@NotNull final String member) {
        members.add(member);
    }

    public List<String> members() {
        return members;
    }
}
