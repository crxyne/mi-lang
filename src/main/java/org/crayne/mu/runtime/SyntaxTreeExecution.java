package org.crayne.mu.runtime;

import org.crayne.mu.lang.Module;
import org.crayne.mu.log.LogHandler;
import org.crayne.mu.log.MessageHandler;
import org.crayne.mu.parsing.ast.Node;
import org.crayne.mu.runtime.lang.*;
import org.crayne.mu.runtime.util.MuUtil;
import org.crayne.mu.runtime.util.errorhandler.Traceback;
import org.crayne.mu.runtime.util.errorhandler.TracebackElement;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class SyntaxTreeExecution {

    private final RModule parentModule;
    private final Node parentNode;
    private final MessageHandler out;
    private final REvaluator evaluator;
    private final List<String> code;
    private final Traceback traceback;
    private final int stdlibFinishLine;
    private boolean error;

    private RModule currentModule;
    private RFunctionScope currentFunctionScope;
    private RFunction mainFunction;

    public SyntaxTreeExecution(@NotNull final Module parentModule, @NotNull final Node parentNode, @NotNull final MessageHandler out, @NotNull final String code, final int stdlibFinishLine) {
        this.parentModule = RModule.of(parentModule);
        this.currentModule = this.parentModule;
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

    public void traceback(final int... lines) {
        for (final int line : lines) traceback.add(newTracebackElement(line));
    }

    public int getStdlibFinishLine() {
        return stdlibFinishLine;
    }

    public boolean error() {
        return error;
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

    public RModule getParentModule() {
        return parentModule;
    }

    public Node getAST() {
        return parentNode;
    }

    public void execute(@NotNull final String mainFunction, @NotNull final Collection<Object> inputParams) throws Throwable {
        MuUtil.defineAllGlobalVariables(this, parentModule);
        final List<RValue> args = inputParams
                .stream()
                .map(p -> new RValue(RDatatype.of(p.getClass()), p)).toList();

        this.mainFunction = MuUtil
                .findFunction(this, mainFunction, args)
                .orElseThrow((Supplier<Throwable>) () -> new IllegalArgumentException("Could not find main function " + mainFunction + "' for input arguments " + args));
        if (this.mainFunction instanceof RNativeFunction) throw new IllegalArgumentException("May not use a native function as mu main function");

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
        out.errorMsg(traceback.toString());
        error = true;
    }

    public RFunctionScope getCurrentFunction() {
        return currentFunctionScope;
    }

    @SuppressWarnings("deprecation")
    private void evalStatement(@NotNull final Node node) {
        try {
            switch (node.type()) {
                case VAR_DEFINITION, VAR_DEF_AND_SET_VALUE -> addVariable(node);
                case FUNCTION_DEFINITION -> {
                    traceback(node.lineDebugging());
                    final String identifier = node.child(0).value().token();
                    final List<RDatatype> params = node
                            .child(3)
                            .children()
                            .stream()
                            .map(t -> RDatatype.of(t.child(0).value().token()))
                            .toList();


                    final RFunctionScope current = currentFunctionScope;
                    final Optional<RFunction> func = MuUtil.findFunctionByTypes(this, currentModule, identifier, params);
                    if (func.isEmpty()) {
                        runtimeError("Could not find function '" + identifier + "' in module '" + currentModule.getName() + "'");
                        return;
                    }
                    if (func.get() == mainFunction) {
                        currentFunctionScope = new RFunctionScope(func.get());
                        evalScope(node.child(4));
                    }
                    currentFunctionScope.deleteLocalVars();
                    currentFunctionScope = current;
                }
                case FUNCTION_CALL -> {
                    if (currentFunctionScope == null) {
                        runtimeError("Function call outside of function scope");
                        return;
                    }
                    traceback(node.lineDebugging());
                    final String identifier = node.child(0).value().token();
                    final List<RValue> params = node.child(1).children().stream().map(evaluator::evaluateExpression).toList();
                    final Optional<RFunction> func = MuUtil.findFunction(this, identifier, params);
                    if (func.isEmpty()) {
                        runtimeError("Could not find function '" + identifier + "' with parameters " + params.stream().map(RValue::getType).toList());
                        return;
                    }
                    final RFunctionScope current = currentFunctionScope;
                    currentFunctionScope = new RFunctionScope(func.get());
                    final Node scope = currentFunctionScope.getFunction().getScope();
                    if (scope != null) evalScope(scope);
                    else if (currentFunctionScope.getFunction() instanceof final RNativeFunction nativeFunction) {
                        final Method nativeMethod = nativeFunction.getNativeMethod();
                        final Class<?> callClass = nativeFunction.getNativeCallClass();
                        final Object[] args = params.stream().map(v -> evaluator.safecast(v.getType(), v)).toArray(Object[]::new);

                        nativeMethod.invoke(callClass.newInstance(), args);
                    } else {
                        runtimeError("Cannot execute null function");
                        return;
                    }
                    currentFunctionScope.deleteLocalVars();
                    currentFunctionScope = current;
                }
                case NOOP -> {
                    if (currentFunctionScope == null) {
                        runtimeError("Local scope outside of function scope");
                        return;
                    }
                    final RFunctionScope current = currentFunctionScope;
                    currentFunctionScope = new RFunctionScope(currentFunctionScope.getFunction());
                    evalScope(node.child(0));
                    currentFunctionScope.deleteLocalVars();
                    currentFunctionScope = current;
                }
                case CREATE_MODULE -> {
                    final String identifier = node.child(0).value().token();
                    final RModule current = currentModule;
                    currentModule = currentModule.getSubModules().stream().filter(m -> m.getName().equals(identifier)).findFirst().orElse(null);
                    if (currentModule == null) {
                        runtimeError("Could not find submodule '" + identifier + "' in module '" + current.getName() + "'");
                        return;
                    }
                    evalScope(node.child(1));
                    currentModule = current;
                }
                //default -> System.out.println("UNHANDLED: " + node.type());
            }
        } catch (final Exception e) {
            runtimeError("Caught unhandled error while executing mu program: " + e.getClass().getSimpleName() + " " + e.getMessage());
        }
    }

    private void evalScope(@NotNull final Node node) {
        for (final Node statement : node.children()) {
            evalStatement(statement);
        }
    }

    private void addVariable(@NotNull final Node node) {
        traceback(node.lineDebugging());
        if (currentFunctionScope != null) {
            final RVariable var = RVariable.of(this, node);
            currentFunctionScope.addLocalVar(var);
        }
    }

}
