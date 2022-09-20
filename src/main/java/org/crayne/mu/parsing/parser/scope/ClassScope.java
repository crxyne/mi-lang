package org.crayne.mu.parsing.parser.scope;

import org.crayne.mu.lang.*;
import org.crayne.mu.lang.Module;
import org.crayne.mu.parsing.lexer.Token;
import org.crayne.mu.parsing.parser.Parser;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public class ClassScope extends Scope {

    private String name;
    private Module module;
    private final List<Variable> members;
    private final HashSet<FunctionConcept> methodConcepts;
    private final FunctionConcept constructor;

    public ClassScope(@NotNull final ScopeType type, final int scopeIndent, final int actualIndent, @NotNull final String name, @NotNull final Module module) {
        super(type, scopeIndent, actualIndent);
        this.name = name;
        this.module = module;
        this.members = new ArrayList<>();
        methodConcepts = new HashSet<>();
        constructor = new FunctionConcept("new", Datatype.VOID);
    }

    public ClassScope(@NotNull final ScopeType type, final int scopeIndent, final int actualIndent) {
        super(type, scopeIndent, actualIndent);
        this.members = new ArrayList<>();
        methodConcepts = new HashSet<>();
        constructor = new FunctionConcept("new", Datatype.VOID);
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
        if (!methodConcepts.isEmpty()) {
            parser.parserWarning("Expected ordinal variables to be at the top of the class definition, before any methods", identToken);
        }
        members.add(var);
    }

    public void constructor(@NotNull final Parser parser, @NotNull final FunctionDefinition def, @NotNull final Token at) {
        final Optional<FunctionDefinition> alreadyExistingDef = constructor.definitionByParameters(def.parameters());
        if (alreadyExistingDef.isPresent()) {
            parser.parserError("A constructor with the same parameters already exists", at);
            return;
        }
        constructor.addDefinition(def);
    }

    public void addMethod(@NotNull final Parser parser, @NotNull final Token at, @NotNull final FunctionDefinition def) {
        final FunctionConcept concept = new FunctionConcept(def.name(), def.returnType());
        for (@NotNull final FunctionConcept methodConcept : methodConcepts) {
            if (methodConcept.equals(concept)) {
                final Optional<FunctionDefinition> alreadyExistingDef = methodConcept.definitionByParameters(def.parameters());
                if (alreadyExistingDef.isPresent()) {
                    parser.parserError("A method with the same parameters already exists", at, "Change either of the method names");
                    return;
                }
                methodConcept.addDefinition(def);
                return;
            }
        }
        concept.addDefinition(def);
        methodConcepts.add(concept);
    }

    public MClass createClass() {
        return new MClass(name, module, members, methodConcepts, constructor);
    }

}
