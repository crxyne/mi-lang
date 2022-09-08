package org.crayne.mu.runtime;

import org.crayne.mu.lang.Module;
import org.crayne.mu.log.LogHandler;
import org.crayne.mu.log.MessageHandler;
import org.crayne.mu.parsing.ast.Node;
import org.crayne.mu.runtime.lang.REvaluator;
import org.crayne.mu.runtime.lang.RVariable;
import org.crayne.mu.runtime.util.errorhandler.Traceback;
import org.crayne.mu.runtime.util.errorhandler.TracebackElement;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class SyntaxTree {

    private final Module parentModule;
    private final Node parentNode;
    private final MessageHandler out;
    private final REvaluator evaluator;
    private final List<String> code;
    private final Traceback traceback;
    private final int stdlibFinishLine;
    private boolean error;

    public SyntaxTree(@NotNull final Module parentModule, @NotNull final Node parentNode, @NotNull final MessageHandler out, @NotNull final String code, final int stdlibFinishLine) {
        this.parentModule = parentModule;
        this.parentNode = parentNode;
        this.out = out;
        this.code = Arrays.stream(code.split("\n")).toList();
        this.traceback = new Traceback();
        this.stdlibFinishLine = stdlibFinishLine;
        this.evaluator = new REvaluator(this);
    }

    public TracebackElement newTracebackElement(final int line) {
        return new TracebackElement(this, line);
    }

    public void traceback(final int line) {
        traceback.add(newTracebackElement(line));
    }

    public int getStdlibFinishLine() {
        return stdlibFinishLine;
    }

    public List<String> getCode() {
        return code;
    }

    public String getLine(final int line) {
        return line < code.size() && line >= 0 ? code.get(line) : null;
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
            if (error) return;
        }
    }

    public void runtimeError(@NotNull final String msg, @NotNull final String... quickFixes) {
        out
                .log("Unexpected runtime error: " + msg, LogHandler.Level.FATAL)
                .possibleSolutions(quickFixes)
                .print();
        error = true;
    }

    private void evalStatement(@NotNull final Node node) {
        try {
            switch (node.type()) {
                case VAR_DEFINITION, VAR_DEF_AND_SET_VALUE -> addVariable(RVariable.of(this, node));
            }
        } catch (final Exception e) {
            runtimeError("Caught unhandled error while executing mu program: " + e.getMessage());
            out.errorMsg(traceback.toString());
        }
    }

    private void addVariable(@NotNull final RVariable var) {

    }

}
