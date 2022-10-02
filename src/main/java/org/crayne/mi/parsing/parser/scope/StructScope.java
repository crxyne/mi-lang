package org.crayne.mi.parsing.parser.scope;

import org.crayne.mi.lang.*;
import org.crayne.mi.lang.Module;
import org.crayne.mi.parsing.lexer.Token;
import org.crayne.mi.parsing.parser.Parser;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StructScope extends Scope {

    private String name;
    private Module module;
    private final List<Variable> members;
    private List<Modifier> modifiers;

    public StructScope(@NotNull final ScopeType type, final int scopeIndent, final int actualIndent, @NotNull final String name, @NotNull final Module module) {
        super(type, scopeIndent, actualIndent);
        this.name = name;
        this.module = module;
        this.members = new ArrayList<>();
    }

    public StructScope(@NotNull final ScopeType type, final int scopeIndent, final int actualIndent) {
        super(type, scopeIndent, actualIndent);
        this.members = new ArrayList<>();
    }

    public void name(final String name) {
        this.name = name;
    }

    public void module(final Module module) {
        this.module = module;
    }

    public void addVar(@NotNull final Parser parser, @NotNull final Variable var, @NotNull final Token identToken) {
        final Optional<Variable> alreadyExisting = members.stream().filter(v -> v.name().equals(var.name())).findFirst();
        if (alreadyExisting.isPresent()) {
            parser.parserError("Redefinition of ordinal variable '" + var.name() + "' in class " + name, identToken);
            return;
        }
        members.add(var);
    }

    public void modifiers(@NotNull final List<Modifier> modifiers) {
        this.modifiers = new ArrayList<>(modifiers);
    }

    public Struct createStruct() {
        return new Struct(name, module, members, modifiers);
    }

}
