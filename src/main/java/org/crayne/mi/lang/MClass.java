package org.crayne.mi.lang;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MClass {

    private final String name;
    private final Module module;
    private final List<Variable> members;
    private final HashSet<FunctionConcept> methodConcepts;
    private final FunctionConcept constructor;

    public MClass(@NotNull final String name, @NotNull final Module module) {
        this.name = name;
        this.module = module;
        members = new ArrayList<>();
        methodConcepts = new HashSet<>();
        constructor = new FunctionConcept("new", Datatype.VOID);
    }

    public MClass(@NotNull final String name, @NotNull final Module module, @NotNull final List<Variable> members,
                  @NotNull final HashSet<FunctionConcept> methodConcepts, @NotNull final FunctionConcept constructor) {
        this.name = name;
        this.module = module;
        this.members = new ArrayList<>(members);
        this.methodConcepts = new HashSet<>(methodConcepts);
        this.constructor = constructor;
    }

    public String name() {
        return name;
    }

    public Module module() {
        return module;
    }

    public List<Variable> members() {
        return members;
    }

    public FunctionConcept constructor() {
        return constructor;
    }

    public HashSet<FunctionConcept> methodConcepts() {
        return methodConcepts;
    }

}
