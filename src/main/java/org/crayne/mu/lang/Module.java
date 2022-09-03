package org.crayne.mu.lang;

import org.crayne.mu.runtime.parsing.parser.Parser;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Module {

    private final String name;
    private final int scopeIndent;
    private final List<Variable> globalModuleVariables;
    private final HashSet<Module> subModules;
    private final HashSet<FunctionConcept> functionConcepts;
    private final Module parent;

    public Module(@NotNull final String name, final int scopeIndent, final Module parent) {
        this.name = name;
        this.scopeIndent = scopeIndent;
        this.parent = parent;
        this.globalModuleVariables = new ArrayList<>();
        this.subModules = new HashSet<>();
        this.functionConcepts = new HashSet<>();
    }

    public int scopeIndent() {
        return scopeIndent;
    }

    public String name() {
        return name;
    }

    public Module parent() {
        return parent;
    }

    public void addFunction(@NotNull final FunctionDefinition def) {
        final FunctionConcept concept = new FunctionConcept(def.name(), def.returnType());
        for (@NotNull final FunctionConcept functionConcept : functionConcepts) {
            if (functionConcept.equals(concept)) {
                functionConcept.addDefinition(def.parameters(), def.modifiers(), def.scope());
                return;
            }
        }
        concept.addDefinition(def.parameters(), def.modifiers(), def.scope());
        functionConcepts.add(concept);
    }

    public void addSubmodule(@NotNull final Module mod) {
        subModules.add(mod);
    }

    public boolean findSubmoduleByName(@NotNull final String mod) {
        return foundModuleByName(subModules, mod);
    }

    public void addGlobalVariable(@NotNull final Parser parser, @NotNull final Variable var) {
        final String name = var.name();
        final Variable alreadyExisting = findVariableByName(name);
        if (alreadyExisting != null) {
            parser.parserError("A global variable with the name '" + name + "' already exists in this module.", parser.currentToken(),
                    "Rename either the existing variable or the new one.");
            return;
        }
        globalModuleVariables.add(var);
    }

    public Variable findVariableByName(@NotNull final String name) {
        return Variable.findVariableByName(globalModuleVariables, name);
    }

    public static boolean foundModuleByName(@NotNull final Set<Module> modules, @NotNull final String mod) {
        return modules.stream().map(Module::name).anyMatch(s -> s.equals(mod));
    }

    public static Module findModuleByName(@NotNull final Set<Module> modules, @NotNull final String mod) {
        return modules.stream().filter(m -> m.name.equals(mod)).findFirst().orElse(null);
    }

    public FunctionConcept findFunctionConceptByName(@NotNull final String name) {
        return functionConcepts.stream().filter(f -> f.name().equals(name)).findFirst().orElse(null);
    }

    public HashSet<Module> subModules() {
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
        return "Module {" +
                "name='" + name + '\'' +
                ", globalModuleVariables=" + globalModuleVariables +
                ", functionConcepts=" + functionConcepts +
                ", subModules=" + subModules +
                '}';
    }
}
