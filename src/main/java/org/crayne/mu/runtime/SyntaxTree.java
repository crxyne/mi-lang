package org.crayne.mu.runtime;

import org.crayne.mu.lang.Module;
import org.crayne.mu.log.MessageHandler;
import org.crayne.mu.parsing.ast.Node;
import org.crayne.mu.runtime.lang.RVariable;
import org.jetbrains.annotations.NotNull;

public class SyntaxTree {

    private final Module parentModule;
    private final Node parentNode;
    private final MessageHandler out;

    public SyntaxTree(@NotNull final Module parentModule, @NotNull final Node parentNode, @NotNull final MessageHandler out) {
        this.parentModule = parentModule;
        this.parentNode = parentNode;
        this.out = out;
    }

    public Module getParentModule() {
        return parentModule;
    }

    public Node getAST() {
        return parentNode;
    }

    public void execute() {
        for (final Node statement : parentNode.children()) {
            evalStatement(statement);
        }
    }

    private void evalStatement(@NotNull final Node node) {
        switch (node.type()) {
            case VAR_DEFINITION, VAR_DEF_AND_SET_VALUE -> addVariable(RVariable.of(node));
        }
    }

    private void addVariable(@NotNull final RVariable var) {

    }

}
