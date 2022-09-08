package org.crayne.mu.runtime.lang;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RFunctionScope {

    private final List<RVariable> localVars;
    private final RFunction function;

    public RFunctionScope(@NotNull final RFunction function) {
        localVars = new ArrayList<>();
        this.function = function;
    }

    public List<RVariable> getLocalVars() {
        return localVars;
    }

    public RFunction getFunction() {
        return function;
    }

    public void deleteLocalVars() {
        localVars.clear();
    }

    public void addLocalVar(@NotNull final RVariable var) {
        localVars.add(var);
    }
}
