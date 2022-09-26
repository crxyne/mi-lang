package org.crayne.mi.lang;

import org.crayne.mi.parsing.ast.Node;
import org.crayne.mi.parsing.lexer.Token;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class Variable {

    private final List<Modifier> modifiers;
    private final Module module;
    private final Node node;
    private final Datatype type;
    private final String name;
    private boolean initialized = false;

    public Variable(@NotNull final String name, @NotNull final Datatype type, @NotNull final List<Modifier> modifiers, final Module module, final Node node) {
        this.name = name;
        this.type = type;
        this.modifiers = modifiers;
        this.module = module;
        this.node = node;
    }

    public Variable(@NotNull final String name, @NotNull final Datatype type, @NotNull final List<Modifier> modifiers, final Module module, final boolean initialized, final Node node) {
        this.name = name;
        this.type = type;
        this.modifiers = modifiers;
        this.module = module;
        this.initialized = initialized;
        this.node = node;
    }

    public Node node() {
        return node;
    }

    public static Optional<Variable> findVariableByName(@NotNull final List<Variable> globalModuleVariables, @NotNull final String name) {
        Variable result = null;
        for (final Variable var : globalModuleVariables) if (var.name().equals(name)) result = var;
        return Optional.ofNullable(result);
    }

    public static Optional<LocalVariable> findLocalVariableByName(@NotNull final List<LocalVariable> localVariables, @NotNull final String name) {
        LocalVariable result = null;
        for (final LocalVariable var : localVariables) if (var.name().equals(name)) result = var;
        return Optional.ofNullable(result);
    }

    public void initialize() {
        initialized = true;
    }

    public boolean initialized() {
        return initialized;
    }

    public Module module() {
        return module;
    }

    public String name() {
        return name;
    }

    public Datatype type() {
        return type;
    }

    public List<Modifier> modifiers() {
        return modifiers;
    }

    public boolean isConstant() {
        return isConstant(modifiers);
    }

    public static boolean isConstant(@NotNull final Collection<Modifier> modifiers) {
        return modifiers.contains(Modifier.CONSTANT) || !modifiers.contains(Modifier.MUTABLE);
    }

    public Token asIdentifierToken(@NotNull final Token identifierTok) {
        return new Token(
                module != null ? (module().fullName() + "." + name()) : name(),
                identifierTok.actualLine(),
                identifierTok.line(),
                identifierTok.column()
        );
    }

    @Override
    public String toString() {
        return "Variable{" +
                "modifiers=" + modifiers +
                ", type=" + type +
                ", name='" + name + '\'' +
                '}';
    }

}
