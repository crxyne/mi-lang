package org.crayne.mu.runtime.lang;

import org.crayne.mu.lang.Enum;
import org.crayne.mu.lang.FunctionConcept;
import org.crayne.mu.lang.Module;
import org.crayne.mu.lang.Variable;
import org.crayne.mu.runtime.MuUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class RModule {

    private final String name;
    private final HashSet<RVariable> globalModuleVariables;
    private final HashSet<RModule> subModules;
    private final HashSet<REnum> enums;
    private final HashSet<RFunction> functions;
    private final RModule parent;

    public RModule(@NotNull final String name, final RModule parent) {
        this.name = name;
        this.parent = parent;
        this.globalModuleVariables = new HashSet<>();
        this.subModules = new HashSet<>();
        this.enums = new HashSet<>();
        this.functions = new HashSet<>();
    }

    public RModule(@NotNull final String name, final RModule parent, @NotNull final Collection<RVariable> globalVariables,
                   @NotNull final Collection<RModule> subModules, @NotNull final Collection<REnum> enums, Collection<RFunction> functions) {
        this.name = name;
        this.parent = parent;
        this.globalModuleVariables = new HashSet<>();
        this.globalModuleVariables.addAll(globalVariables);
        this.subModules = new HashSet<>();
        this.subModules.addAll(subModules);
        this.enums = new HashSet<>();
        this.enums.addAll(enums);
        this.functions = new HashSet<>();
        this.functions.addAll(functions);
    }

    public String getName() {
        return name;
    }

    public HashSet<RModule> getSubModules() {
        return subModules;
    }

    public Optional<RModule> getParent() {
        return Optional.ofNullable(parent);
    }

    public HashSet<REnum> getEnums() {
        return enums;
    }

    public HashSet<RFunction> getFunctions() {
        return functions;
    }

    public HashSet<RVariable> getGlobalModuleVariables() {
        return globalModuleVariables;
    }

    public static RModule of(@NotNull final Module m) {
        return new RModule(
                m.name(),
                m.parent() != null ? RModule.of(m.parent()) : null,
                globalVarsOf(m.moduleVariables()),
                submodulesOf(m.subModules()),
                enumsOf(m.enums()),
                functionsOf(m.functionConcepts())
        );
    }

    public static Set<RVariable> globalVarsOf(@NotNull final List<Variable> globalVars) {
        return MuUtil.unmodifiableSet(
                globalVars
                        .stream()
                        .map(RVariable::of)
        );
    }

    public static Set<RModule> submodulesOf(@NotNull final HashSet<Module> submodules) {
        return MuUtil.unmodifiableSet(
                submodules
                        .stream()
                        .map(RModule::of)
        );
    }

    public static Set<REnum> enumsOf(@NotNull final HashSet<Enum> enums) {
        return MuUtil.unmodifiableSet(
                enums
                        .stream()
                        .map(REnum::of)
        );
    }

    public static Set<RFunction> functionsOf(@NotNull final HashSet<FunctionConcept> concepts) {
        return MuUtil.unmodifiableSet(
                concepts
                        .stream()
                        .map(FunctionConcept::definitions)
                        .flatMap(HashSet::stream)
                        .map(RFunction::of)
        );
    }

    @Override
    public String toString() {
        return "RModule{" +
                "name='" + name + '\'' +
                ", globalModuleVariables=" + globalModuleVariables +
                ", subModules=" + subModules +
                ", enums=" + enums +
                ", functions=" + functions +
                ", parent=" + parent +
                '}';
    }

}
