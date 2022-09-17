package org.crayne.mu.lang;

import org.crayne.mu.parsing.ast.Node;
import org.crayne.mu.parsing.lexer.Token;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.List;

public class FunctionDefinition {

    private final String name;
    private final Datatype returnType;
    private final List<FunctionParameter> parameters;
    private final List<Modifier> modifiers;
    private Node scope;
    private Method nativeMethod;
    private Class<?> nativeCallClass;
    private final Module module;
    private final boolean simulated;

    public FunctionDefinition(@NotNull final String name, @NotNull final Datatype returnType, @NotNull final List<FunctionParameter> parameters,
                              @NotNull final List<Modifier> modifiers, @NotNull final Module module, final boolean simulated) {
        this.name = name;
        this.returnType = returnType;
        this.parameters = parameters;
        this.modifiers = modifiers;
        this.module = module;
        this.simulated = simulated;
    }

    public FunctionDefinition(@NotNull final String name, @NotNull final Datatype returnType, @NotNull final List<FunctionParameter> parameters,
                              @NotNull final List<Modifier> modifiers, @NotNull final Module module, @NotNull final Method nativeMethod, @NotNull final Class<?> nativeCallClass) {
        this.name = name;
        this.returnType = returnType;
        this.parameters = parameters;
        this.modifiers = modifiers;
        this.module = module;
        this.nativeMethod = nativeMethod;
        this.nativeCallClass = nativeCallClass;
        this.simulated = false;
    }


    public FunctionDefinition(@NotNull final String name, @NotNull final Datatype returnType, @NotNull final List<FunctionParameter> parameters,
                              @NotNull final List<Modifier> modifiers, @NotNull final Module module, @NotNull final Node scope) {
        this.name = name;
        this.returnType = returnType;
        this.parameters = parameters;
        this.modifiers = modifiers;
        this.module = module;
        this.scope = scope;
        this.simulated = false;
    }

    public void scope(@NotNull final Node scope) {
        this.scope = scope;
    }

    public boolean simulated() {
        return simulated;
    }

    public Method nativeMethod() {
        return nativeMethod;
    }

    public Class<?> nativeCallClass() {
        return nativeCallClass;
    }

    public String name() {
        return name;
    }

    public Datatype returnType() {
        return returnType;
    }

    public List<FunctionParameter> parameters() {
        return parameters;
    }

    public List<Modifier> modifiers() {
        return modifiers;
    }

    public Module module() {
        return module;
    }

    public Node scope() {
        return scope;
    }
    public Token asIdentifierToken(@NotNull final Token identifierTok) {
        return new Token(
                module().fullName() + "." + name(),
                identifierTok.actualLine(),
                identifierTok.line(),
                identifierTok.column()
        );
    }

    public static Token asIdentifierToken(@NotNull final Module module, @NotNull final Token identifierTok) {
        return new Token(
                module.fullName() + "." + identifierTok.token(),
                identifierTok.actualLine(),
                identifierTok.line(),
                identifierTok.column()
        );
    }

    @Override
    public String toString() {
        return "FunctionDefinition{" +
                "name='" + name + '\'' +
                ", returnType=" + returnType +
                ", parameters=" + parameters +
                ", modifiers=" + modifiers +
                ", scope=" + scope +
                ", module=" + module.fullName() +
                ", nativeMethod=" + (nativeMethod != null ? (nativeMethod.getDeclaringClass().getName() + "." + nativeMethod.getName()) : null) +
                '}';
    }
}
