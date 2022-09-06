package org.crayne.mu.runtime.parsing.parser.scope;

import org.crayne.mu.lang.Enum;
import org.crayne.mu.lang.Modifier;
import org.crayne.mu.lang.Module;
import org.crayne.mu.runtime.parsing.parser.Parser;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EnumScope extends Scope {

    private final List<String> members;
    private List<Modifier> modifiers;
    private String name;

    public EnumScope(@NotNull final ScopeType type, final int scopeIndent, final int actualIndent) {
        super(type, scopeIndent, actualIndent);
        members = new ArrayList<>();
    }

    public void name(@NotNull final String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public void modifiers(@NotNull final List<Modifier> modifiers) {
        this.modifiers = new ArrayList<>(modifiers);
    }

    public List<Modifier> modifiers() {
        return new ArrayList<>(modifiers);
    }

    public void addMembers(@NotNull final Collection<String> members) {
        this.members.addAll(members);
    }

    public boolean hasMember(@NotNull final String ident) {
        return members.contains(ident);
    }

    public boolean hasMembers() {
        return !members.isEmpty();
    }

    public List<String> members() {
        return new ArrayList<>(members);
    }

    public void scopeEnd(@NotNull final Parser parser) {
        if (!hasMembers()) {
            parser.parserError("Cannot create empty enum, expected members");
            return;
        }
        final Module module = parser.lastModule();
        module.addEnum(Enum.of(this));
    }

}
