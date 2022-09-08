package org.crayne.mu.runtime.lang;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RFunctionScope {

    private final List<RVariable> localVars;
    private final RFunctionScope parent;
    private final RFunction function;

    public RFunctionScope(@NotNull final RFunction function, final RFunctionScope parent) {
        localVars = new ArrayList<>();
        this.function = function;
        this.parent = parent;
    }

    public List<RVariable> getLocalVars() {
        return localVars;
    }

    public RFunction getFunction() {
        return function;
    }

    public RFunctionScope getParent() {
        return parent;
    }

    public void deleteLocalVars() {
        localVars.clear();
    }

    public void addLocalVar(@NotNull final RVariable var) {
        localVars.add(var);
    }
}
