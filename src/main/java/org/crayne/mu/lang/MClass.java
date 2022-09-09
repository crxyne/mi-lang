package org.crayne.mu.lang;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MClass {

    private final List<Variable> members;
    private final HashSet<FunctionConcept> methodConcepts;
    private final FunctionConcept constructor;

    public MClass() {
        members = new ArrayList<>();
        methodConcepts = new HashSet<>();
        constructor = new FunctionConcept("new", Datatype.VOID);
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
