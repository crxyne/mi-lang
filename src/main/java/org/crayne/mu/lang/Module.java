package org.crayne.mu.lang;

import org.crayne.mu.parsing.lexer.Token;
import org.crayne.mu.parsing.parser.Parser;
import org.crayne.mu.parsing.parser.ParserEvaluator;
import org.crayne.mu.parsing.parser.scope.FunctionScope;
import org.crayne.mu.parsing.parser.scope.Scope;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Module {

    private final String name;
    private final int scopeIndent;
    private final List<Variable> globalModuleVariables;
    private final List<MClass> classes;
    private final List<Module> subModules; // these HAVE TO be Lists, or else there could be runtime errors with illegal forward referencing
    private final HashSet<Enum> enums;
    private final HashSet<FunctionConcept> functionConcepts; // here the order would not matter
    private final Module parent;

    public Module(@NotNull final String name, final int scopeIndent, final Module parent) {
        this.name = name;
        this.scopeIndent = scopeIndent;
        this.parent = parent;
        this.globalModuleVariables = new ArrayList<>();
        this.subModules = new ArrayList<>();
        this.functionConcepts = new HashSet<>();
        this.enums = new HashSet<>();
        this.classes = new ArrayList<>();
    }

    public int scopeIndent() {
        return scopeIndent;
    }

    public String name() {
        return name;
    }
    public String fullName() {
        final StringBuilder result = new StringBuilder(name);
        Module current = this.parent;
        while (current != null) {
            result.insert(0, current.name + ".");
            current = current.parent;
        }
        return result.toString();
    }

    public void addClass(@NotNull final Parser parser, @NotNull final MClass mClass, @NotNull final Token at) {
        final Optional<MClass> alreadyExisting = classes.stream().filter(c -> c.name().equals(mClass.name())).findFirst();
        if (alreadyExisting.isPresent()) {
            parser.parserError("Class '" + mClass.name() + "' already exists in this module", "Rename your class or move either of the two to another module");
            return;
        }
        classes.add(mClass);
    }

    public Module parent() {
        return parent;
    }

    public void addFunction(@NotNull final Parser parser, @NotNull final Token at, @NotNull final FunctionDefinition def) {
        final FunctionConcept concept = new FunctionConcept(def.name(), def.returnType());
        for (@NotNull final FunctionConcept functionConcept : functionConcepts) {
            if (functionConcept.equals(concept)) {
                final Optional<FunctionDefinition> alreadyExistingDef = functionConcept.definitionByParameters(def.parameters());
                if (alreadyExistingDef.isPresent()) {
                    if (alreadyExistingDef.get().simulated()) {
                        functionConcept.removeDefinition(alreadyExistingDef.get());
                    } else {
                        parser.parserError("A function with the same parameters already exists", at, "Change either of the function names");
                        return;
                    }
                }
                functionConcept.addDefinition(def);
                return;
            }
        }
        concept.addDefinition(def);
        functionConcepts.add(concept);
    }

    public void addSubmodule(@NotNull final Module mod) {
        subModules.add(mod);
    }
    public void addEnum(@NotNull final Enum _enum) {
        enums.add(_enum);
    }

    public Optional<Enum> findEnumByName(@NotNull final String en) {
        return Enum.findEnumByName(enums, en);
    }

    public boolean findSubmoduleByName(@NotNull final String mod) {
        return foundModuleByName(subModules, mod);
    }

    public HashSet<Enum> enums() {
        return enums;
    }

    public void addGlobalVariable(@NotNull final Parser parser, @NotNull final Variable var) {
        final String name = var.name();
        final Optional<Variable> alreadyExisting = findVariableByName(name);
        if (alreadyExisting.isPresent()) {
            parser.parserError("A global variable with the name '" + name + "' already exists in this module.", parser.currentToken(),
                    "Rename either the existing variable or the new one.");
            return;
        }
        globalModuleVariables.add(var);
    }

    public Optional<Variable> findVariableByName(@NotNull final String name) {
        return Variable.findVariableByName(globalModuleVariables, name);
    }

    public static boolean foundModuleByName(@NotNull final Collection<Module> modules, @NotNull final String mod) {
        return modules.stream().map(Module::name).anyMatch(s -> s.equals(mod));
    }

    public static Module findModuleByName(@NotNull final Collection<Module> modules, @NotNull final String mod) {
        return modules.stream().filter(m -> m.name.equals(mod)).findFirst().orElse(null);
    }

    public Optional<FunctionConcept> findRawFunctionConcept(@NotNull final String name) {
        return functionConcepts.stream().filter(f -> f.name().equals(name)).findFirst();
    }

    public Optional<FunctionConcept> findFunctionConceptByName(@NotNull final Parser parser, @NotNull final Token name) {
        final String nameStr = ParserEvaluator.identOf(name.token());
        final Optional<FunctionConcept> found = functionConcepts.stream().filter(f -> f.name().equals(nameStr)).findFirst();
        if (found.isEmpty()) {
            final Optional<Scope> currentScope = parser.scope();
            if (currentScope.isPresent() && currentScope.get() instanceof final FunctionScope functionScope
                    && functionScope.definition().name().equals(name.token()) && findRawFunctionConcept(name.token()).isEmpty()) {
                addFunction(parser, name, functionScope.definition());
                return functionConcepts.stream().filter(f -> f.name().equals(nameStr)).findFirst();
            }
        }
        return found;
    }

    public List<Module> subModules() {
        return subModules;
    }

    public HashSet<FunctionConcept> functionConcepts() {
        return functionConcepts;
    }

    public List<Variable> moduleVariables() {
        return globalModuleVariables;
    }

    @Override
    public String toString() {
        return "Module{" +
                "name='" + name + '\'' +
                ", scopeIndent=" + scopeIndent +
                ", globalModuleVariables=" + globalModuleVariables +
                ", classes=" + classes +
                ", subModules=" + subModules +
                ", enums=" + enums +
                ", functionConcepts=" + functionConcepts +
                '}';
    }

}
