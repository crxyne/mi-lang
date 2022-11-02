package org.crayne.mi.lang;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MiEnum {

    private final String name;
    private final Set<MiModifier> modifiers;
    private final List<String> members;
    private final MiModule module;

    public MiEnum(@NotNull final String name, @NotNull final MiModule module, @NotNull final Collection<MiModifier> modifiers, @NotNull final List<String> members) {
        this.name = name;
        this.module = module;
        this.modifiers = new HashSet<>(modifiers);
        this.members = new ArrayList<>(members);
    }

    public MiEnum(@NotNull final String name, @NotNull final MiModule module, @NotNull final Collection<MiModifier> modifiers) {
        this.name = name;
        this.module = module;
        this.modifiers = new HashSet<>(modifiers);
        this.members = new ArrayList<>();
    }

    public MiModule module() {
        return module;
    }

    public String name() {
        return name;
    }

    public Set<MiModifier> modifiers() {
        return modifiers;
    }

    public List<String> members() {
        return members;
    }

    public void members(@NotNull final List<String> members) {
        this.members.clear();
        this.members.addAll(members);
    }

}
