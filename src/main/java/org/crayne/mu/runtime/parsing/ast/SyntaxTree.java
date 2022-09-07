package org.crayne.mu.runtime.parsing.ast;

import org.crayne.mu.lang.Module;
import org.jetbrains.annotations.NotNull;

public class SyntaxTree {

    private final Module parentModule;
    private final Node parentNode;

    public SyntaxTree(@NotNull final Module parentModule, @NotNull final Node parentNode) {
        this.parentModule = parentModule;
        this.parentNode = parentNode;
    }

    public Module getParentModule() {
        return parentModule;
    }

    public Node getAST() {
        return parentNode;
    }

    public void execute() {

    }

}
