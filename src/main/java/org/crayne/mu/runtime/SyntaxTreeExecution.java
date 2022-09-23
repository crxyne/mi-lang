package org.crayne.mu.runtime;

import org.crayne.mu.lang.Module;
import org.crayne.mu.lang.*;
import org.crayne.mu.log.LogHandler;
import org.crayne.mu.log.MessageHandler;
import org.crayne.mu.parsing.ast.Node;
import org.crayne.mu.parsing.ast.NodeType;
import org.crayne.mu.runtime.lang.*;
import org.crayne.mu.runtime.util.MuUtil;
import org.crayne.mu.bytecode.common.errorhandler.Traceback;
import org.crayne.mu.bytecode.common.errorhandler.TracebackElement;
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
    private final boolean printStackTraces;
    private final int stdlibFinishLine;
    private boolean error;

    private RModule currentModule;
    private RFunctionScope currentFunctionScope;
    private RFunction mainFunction;
    private List<RValue> mainFunctionArgs;

    public SyntaxTreeExecution(@NotNull final Module parentModule, @NotNull final Node parentNode, @NotNull final MessageHandler out, @NotNull final String code, final int stdlibFinishLine, final boolean printStackTraces) {
        this.parentModule = RModule.of(parentModule);
        this.currentModule = this.parentModule;
        this.parentNode = parentNode;
        this.out = out;
        this.code = Arrays.stream(code.split("\n")).toList();
        this.traceback = new Traceback();
        this.stdlibFinishLine = stdlibFinishLine;
        this.evaluator = new REvaluator(this);
        this.printStackTraces = printStackTraces;
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
        mainFunctionArgs = inputParams
                .stream()
                .map(p -> new RValue(RDatatype.of(p.getClass()), p)).toList();

        this.mainFunction = MuUtil
                .findFunction(this, mainFunction, mainFunctionArgs)
                .orElseThrow((Supplier<Throwable>) () -> new IllegalArgumentException("Could not find main function '" + mainFunction + "' for input arguments " + mainFunctionArgs));
        if (this.mainFunction instanceof RNativeFunction) throw new IllegalArgumentException("May not use a native function as mu main function");

        scope(parentNode);
    }

    private boolean statement(@NotNull final Node node) {
        traceback(node.lineDebugging());
        try {
            return switch (node.type()) {
                case VAR_DEFINITION, VAR_DEF_AND_SET_VALUE -> variableDefinition(node);
                case FUNCTION_DEFINITION -> functionDefinition(node);
                case FUNCTION_CALL -> {
                    functionCall(node);
                    yield false;
                }
                case DO_STATEMENT -> doWhileStatement(node);
                case IF_STATEMENT -> ifStatement(node);
                case WHILE_STATEMENT -> whileStatement(node);
                case VAR_SET_VALUE -> {
                    variableSetValue(node);
                    yield false;
                }
                case FOR_FAKE_SCOPE -> forStatement(node);
                case SCOPE -> scope(node);
                case NOOP -> createLocalScope(node.child(0));
                case RETURN_VALUE -> {
                    if (currentFunctionScope != null) {
                        currentFunctionScope.deleteLocalVars();
                        currentFunctionScope = currentFunctionScope.getParent();
                    }
                    yield  true;
                }
                case CREATE_MODULE -> createModule(node);
                default -> false;
            };
        } catch (final Exception e) {
            runtimeError("Caught unhandled error while executing mu program: " + e.getClass().getSimpleName() + " " + e.getMessage());
            if (printStackTraces) e.printStackTrace(out.outStream());
        }
        return false;
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

    private boolean forStatement(@NotNull final Node node) {
        final Node forStatement = node.child(0);
        if (currentFunctionScope == null) {
            runtimeError("Local scope outside of function scope");
            return true;
        }
        final RFunctionScope current = currentFunctionScope;
        currentFunctionScope = new RFunctionScope(currentFunctionScope.getFunction(), current);
        if (statement(forStatement.child(0))) return true;
        final Node condition = forStatement.child(1).child(0);
        final Node forInstruct = forStatement.child(2).child(0);

        Boolean executeFor = condition(condition);
        if (executeFor == null) return true;

        while (executeFor) {
            createLocalScope(node.child(1));
            if (statement(forInstruct)) return true;
            executeFor = condition(condition);
            if (executeFor == null) return true;
            if (!executeFor) break;
        }

        currentFunctionScope.deleteLocalVars();
        currentFunctionScope = current;
        return false;
    }

    private boolean whileStatement(@NotNull final Node node) {
        if (currentFunctionScope == null) {
            runtimeError("Local scope outside of function scope");
            return false;
        }
        final Node condition = node.child(0).child(0);

        Boolean executeWhile = condition(condition);
        if (executeWhile == null) return true;

        while (executeWhile) {
            if (createLocalScope(node.child(1))) return true;
            executeWhile = condition(condition);
            if (executeWhile == null) return true;
            if (!executeWhile) break;
        }
        return false;
    }

    private boolean doWhileStatement(@NotNull final Node node) {
        if (currentFunctionScope == null) {
            runtimeError("Local scope outside of function scope");
            return false;
        }
        final Node condition = node.child(1).child(0);

        Boolean executeWhile = condition(condition);
        if (executeWhile == null) return true;

        do {
            if (createLocalScope(node.child(0))) return true;
            executeWhile = condition(condition);
            if (executeWhile == null) return true;
        } while (executeWhile);
        return false;
    }

    public RValue variableSetValue(@NotNull final Node node) {
        final String identifier = node.child(0).value().token();
        final Optional<RVariable> ovar = MuUtil.findVariable(this, identifier);
        if (ovar.isEmpty()) {
            runtimeError("Cannot find variable '" + identifier + "'");
            return null;
        }
        final RVariable var = ovar.get();
        final String operatorStr = node.child(1).value().token();
        final EqualOperation operator = EqualOperation.of(operatorStr);
        if (operator == null) {
            runtimeError("Unknown equal operator '" + operatorStr + "'");
            return null;
        }
        final RValue value = evaluator.evaluateExpression(node.child(2));
        if (value == null) return null;

        return switch (operator) {
            case ADD -> var.setValue(var.getValue().add(evaluator, value));
            case SUB -> var.setValue(var.getValue().subtract(evaluator, value));
            case MULT -> var.setValue(var.getValue().multiply(evaluator, value));
            case XOR -> var.setValue(var.getValue().bitXor(evaluator, value));
            case SHIFTR -> var.setValue(var.getValue().bitShiftRight(evaluator, value));
            case SHIFTL -> var.setValue(var.getValue().bitShiftLeft(evaluator, value));
            case MOD -> var.setValue(var.getValue().modulus(evaluator, value));
            case DIV -> var.setValue(var.getValue().divide(evaluator, value));
            case AND -> var.setValue(var.getValue().bitAnd(evaluator, value));
            case OR -> var.setValue(var.getValue().bitOr(evaluator, value));
            case EQUAL -> var.setValue(value);
        };
    }

    private boolean ifStatement(@NotNull final Node node) {
        final Boolean executeIf = condition(node.child(0).child(0));
        if (executeIf == null) return true;
        if (executeIf) return createLocalScope(node.child(1));

        if (node.children().size() > 2) return createLocalScope(node.child(2).child(0));
        return false;
    }

    private Boolean condition(@NotNull final Node node) {
        final RValue condition = evaluator.evaluateExpression(node);
        if (condition == null) return null;
        final Object conditionValue = condition.getValue();
        if (!condition.getType().primitive() || condition.getType().getPrimitive() != PrimitiveDatatype.BOOL || !(conditionValue instanceof Boolean)) {
            runtimeError("Non-Boolean condition at if statement");
            return null;
        }
        return (Boolean) conditionValue;
    }

    public RValue functionCall(@NotNull final Node node) {
        if (currentFunctionScope == null) {
            runtimeError("Function call outside of function scope");
            return null;
        }
        final String identifier = node.child(0).value().token();
        final List<RValue> params = node.child(1).children().stream().map(evaluator::evaluateExpression).toList();
        final Optional<RFunction> func = MuUtil.findFunction(this, identifier, params);
        if (func.isEmpty()) {
            runtimeError("Could not find function '" + identifier + "' with parameters " + params.stream().map(RValue::getType).toList());
            return null;
        }
        final RFunctionScope current = currentFunctionScope;
        currentFunctionScope = new RFunctionScope(func.get(), current);
        final Node scope = currentFunctionScope.getFunction().getScope();

        RValue retVal = null;
        final RDatatype retType = RDatatype.of(func.get().getReturnType());

        if (scope != null) {
            for (int i = 0; i < params.size(); i++) {
                final RValue value = params.get(i);
                final FunctionParameter defParam = func.get().getDefinedParams().get(i);
                final String identParam = defParam.name();
                final Datatype typeParam = defParam.type();
                final RVariable var = new RVariable(identParam, RDatatype.of(typeParam), value, null);
                currentFunctionScope.addLocalVar(var);
            }
            for (final Node statement : scope.children()) {
                if (statement.type() == NodeType.RETURN_VALUE) {
                    if (statement.children().isEmpty()) break;
                    retVal = evaluator.evaluateExpression(statement.child(0));
                    break;
                }
                if (statement(statement)) break;
            }
        } else if (currentFunctionScope.getFunction() instanceof final RNativeFunction nativeFunction) {
            final Method nativeMethod = nativeFunction.getNativeMethod();
            final Class<?> callClass = nativeFunction.getNativeCallClass();
            final Object[] args = params.stream().map(v -> evaluator.safecast(v.getType(), v)).toArray(Object[]::new);

            try {
                final Object ret = nativeMethod.invoke(callClass.newInstance(), args);

                retVal = ret == null ? null : new RValue(retType, ret);
            } catch (final Exception e) {
                runtimeError("Could not call native method " + nativeMethod.getName() + ": " + e.getClass().getSimpleName() + " " + e.getMessage());
            }
        } else {
            runtimeError("Cannot execute null function");
            return null;
        }
        currentFunctionScope.deleteLocalVars();
        currentFunctionScope = current;
        if (retVal == null) return null;
        return new RValue(retType, evaluator.safecast(retType, retVal));
    }

    private boolean createModule(@NotNull final Node node) {
        final String identifier = node.child(0).value().token();
        final RModule current = currentModule;
        currentModule = currentModule.getSubModules().stream().filter(m -> m.getName().equals(identifier)).findFirst().orElse(null);
        if (currentModule == null) {
            runtimeError("Could not find submodule '" + identifier + "' in module '" + current.getName() + "'");
            return false;
        }
        if (scope(node.child(1))) return true;
        currentModule = current;
        return false;
    }

    private boolean createLocalScope(@NotNull final Node node) {
        if (currentFunctionScope == null) {
            runtimeError("Local scope outside of function scope");
            return false;
        }
        final RFunctionScope current = currentFunctionScope;
        currentFunctionScope = new RFunctionScope(currentFunctionScope.getFunction(), current);
        if (scope(node)) return true;
        currentFunctionScope.deleteLocalVars();
        currentFunctionScope = current;
        return false;
    }

    private boolean scope(@NotNull final Node node) {
        for (final Node statement : node.children()) {
            if (statement(statement)) return true;
        }
        return false;
    }

    private boolean functionDefinition(@NotNull final Node node) {
        final String identifier = node.child(0).value().token();
        final List<RDatatype> params = node
                .child(3)
                .children()
                .stream()
                .map(t -> RDatatype.of(t.child(0).value().token()))
                .toList();


        final Optional<RFunction> func = MuUtil.findFunctionByTypes(this, currentModule, identifier, params);
        if (func.isEmpty()) {
            runtimeError("Could not find function '" + identifier + "' in module '" + currentModule.getName() + "'");
            return true;
        }
        if (func.get() == mainFunction) {
            currentFunctionScope = new RFunctionScope(func.get(), null);

            for (int i = 0; i < mainFunctionArgs.size(); i++) {
                final RValue value = mainFunctionArgs.get(i);
                final FunctionParameter defParam = this.mainFunction.getDefinedParams().get(i);
                final String identParam = defParam.name();
                final Datatype typeParam = defParam.type();
                final RVariable var = new RVariable(identParam, RDatatype.of(typeParam), value, null);
                currentFunctionScope.addLocalVar(var);
            }
            if (scope(node.child(4))) return true;
        }
        currentFunctionScope = null;
        return false;
    }

    private boolean variableDefinition(@NotNull final Node node) {
        traceback(node.lineDebugging());
        if (currentFunctionScope != null) {
            final RVariable var = RVariable.of(this, node);
            currentFunctionScope.addLocalVar(var);
        }
        return false;
    }

}
