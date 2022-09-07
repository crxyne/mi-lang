package org.crayne.mu.runtime;

import org.crayne.mu.lang.Module;
import org.crayne.mu.log.LogHandler;
import org.crayne.mu.log.MessageHandler;
import org.crayne.mu.parsing.ast.Node;
import org.crayne.mu.runtime.lang.REvaluator;
import org.crayne.mu.runtime.lang.RVariable;
import org.jetbrains.annotations.NotNull;

public class SyntaxTree {

    private final Module parentModule;
    private final Node parentNode;
    private final MessageHandler out;
    private final REvaluator evaluator;

    public SyntaxTree(@NotNull final Module parentModule, @NotNull final Node parentNode, @NotNull final MessageHandler out) {
        this.parentModule = parentModule;
        this.parentNode = parentNode;
        this.out = out;
        this.evaluator = new REvaluator(this);
    }

    public REvaluator getEvaluator() {
        return evaluator;
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

    public void runtimeError(@NotNull final String msg, @NotNull final String... quickFixes) {
        out
                .log("Unexpected runtime error: " + msg, LogHandler.Level.FATAL)
                .possibleSolutions(quickFixes)
                .print();
    }

    private void evalStatement(@NotNull final Node node) {
        switch (node.type()) {
            case VAR_DEFINITION, VAR_DEF_AND_SET_VALUE -> addVariable(RVariable.of(this, node));
        }
    }

    private void addVariable(@NotNull final RVariable var) {

    }

}
