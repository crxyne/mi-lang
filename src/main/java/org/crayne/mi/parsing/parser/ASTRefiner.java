package org.crayne.mi.parsing.parser;

import org.crayne.mi.lang.*;
import org.crayne.mi.parsing.ast.Node;
import org.crayne.mi.parsing.ast.NodeType;
import org.crayne.mi.parsing.lexer.Token;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.*;

public class ASTRefiner {

    private final Parser parser;
    private Node AST;
    private MiModule currentModule = new MiModule("!PARENT");
    private final MiModule rootModule = currentModule;

    public ASTRefiner(@NotNull final Parser parser) {
        this.parser = parser;
    }

    public Node checkAST(@NotNull final Node AST) {
        this.AST = AST;
        parser.reachedStdlibFinish(false);
        final Set<Map.Entry<Node, MiInternFunction>> functionScopes = defineAllGlobal(this.AST);
        parser.reachedStdlibFinish(false);
        checkAllFunctions(functionScopes);
        parser.reachedStdlibFinish(true);

        return parser.encounteredError() ? null : this.AST;
    }

    public Parser parser() {
        return parser;
    }

    private Set<Map.Entry<Node, MiInternFunction>> defineAllGlobal(@NotNull final Node node) {
        final Set<Map.Entry<Node, MiInternFunction>> functionScopes = new HashSet<>();
        for (@NotNull final Node child : node.children()) {
            if (parser.encounteredError()) return new HashSet<>();
            switch (child.type()) {
                case CREATE_MODULE -> functionScopes.addAll(defineModule(child));
                case STANDARDLIB_MI_FINISH_CODE -> parser.reachedStdlibFinish(true);
                case FUNCTION_DEFINITION -> {
                    final Map.Entry<Node, MiInternFunction> entry = defineInternFunction(child);
                    if (entry == null) return new HashSet<>();
                    functionScopes.add(entry);
                }
                case CREATE_ENUM -> defineEnum(child);
                case NOOP -> {}
                default -> {
                    final Token ident = child.child(0).value();
                    parser.parserError("Unexpected token '" + ident.token() + "'", ident);
                    return new HashSet<>();
                }
                case NATIVE_FUNCTION_DEFINITION -> defineNativeFunction(child);
                case DECLARE_VARIABLE -> defineVariable(child, currentModule, false, true);
                case DEFINE_VARIABLE -> defineVariable(child, currentModule, true, true);
            }
        }
        return functionScopes;
    }

    private void checkAllFunctions(@NotNull final Set<Map.Entry<Node, MiInternFunction>> functionScopes) {
        functionScopes.forEach(this::checkLocal);
    }

    private void checkLocal(@NotNull final Map.Entry<Node, MiInternFunction> functionScope) {
        final Node parentFunctionScope = functionScope.getKey();
        final MiInternFunction function = functionScope.getValue();

        parser.reachedStdlibFinish(parentFunctionScope.value().actualLine() > parser.stdlibFinishLine());
        checkLocal(parentFunctionScope, function, false);
        function.pop();
    }

    private Optional<MiModule> findModuleByName(@NotNull final String dotted, final boolean goFromRoot, @NotNull final MiModule usedAt) {
        // calling a function with the . as the first character means explicitly going from
        // root and checking all submodules from there
        MiModule current = goFromRoot ? rootModule : usedAt;

        for (@NotNull final String subMod : dotted.split("\\.")) {
            if (subMod.isEmpty()) continue;
            if (current == null) break;
            final Optional<MiModule> submodule = current.findSubmoduleByName(subMod);
            current = submodule.orElse(null);
        }
        return current == null && !goFromRoot ? findModuleByName(dotted, true, usedAt) : Optional.ofNullable(current);
    }

    protected Optional<MiEnum> findEnumByName(@NotNull final Token ident, @NotNull final MiModule accessedFrom) {
        final String name = ident.token();
        if (!name.contains(".")) return Optional.ofNullable(accessedFrom.findEnumByName(name)
                .orElse(rootModule.findEnumByName(name)
                        .orElse(null)));

        final boolean searchAtRoot = name.startsWith(".");
        final String withoutFirstDotAndName = ASTGenerator.moduleOf(searchAtRoot ? name.substring(1) : name);
        final Optional<MiModule> enumModule = findModuleByName(withoutFirstDotAndName, searchAtRoot, accessedFrom);
        if (enumModule.isEmpty()) {
            parser.parserError("Cannot find " + (!searchAtRoot ? "sub" : "") + "module '" + withoutFirstDotAndName + "' here", ident,
                    "Make sure you spelled the module name correctly and have defined the module already.");
            return Optional.empty();
        }
        return enumModule.get().findEnumByName(ASTGenerator.identOf(name));
    }

    protected Optional<MiFunction> findFunctionByCall(@NotNull final Token ident, @NotNull final List<MiDatatype> callParams, @NotNull final MiModule calledFrom) {
        final String name = ident.token();
        if (!name.contains(".")) {
            // calling a function without specifying the module means either using
            // a local function in the current scope, or a function in the root scope (standard library functions)
            return Optional.ofNullable(calledFrom.findFunction(name, callParams, false)
                    .orElse(rootModule.findFunction(name, callParams, false).orElse(null)));
        }
        final boolean searchAtRoot = name.startsWith(".");
        final String withoutFirstDotAndName = ASTGenerator.moduleOf(searchAtRoot ? name.substring(1) : name);
        final Optional<MiModule> callModule = findModuleByName(withoutFirstDotAndName, searchAtRoot, calledFrom);
        if (callModule.isEmpty()) {
            parser.parserError("Cannot find " + (!searchAtRoot ? "sub" : "") + "module '" + withoutFirstDotAndName + "' here", ident,
                    "Make sure you spelled the module name correctly and have defined the module already.");
            return Optional.empty();
        }
        return callModule.get().findFunction(ASTGenerator.identOf(name), callParams, false);
    }

    protected Optional<MiVariable> findGlobalVariableByAccess(@NotNull final Token ident, @NotNull final MiModule accessedFrom) {
        final String name = ident.token();
        if (!name.contains(".")) {
            // using a variable without specifying the module means either using
            // a local variable, a global variable in the own module, or a variable in the root scope (standard library functionality)
            return Optional.ofNullable(accessedFrom.find(name)
                    .orElse(rootModule.find(name).orElse(null)));
        }

        final boolean searchAtRoot = name.startsWith(".");
        final String withoutFirstDotAndName = ASTGenerator.moduleOf(searchAtRoot ? name.substring(1) : name);
        final Optional<MiModule> accessModule = findModuleByName(withoutFirstDotAndName, searchAtRoot, accessedFrom);
        if (accessModule.isEmpty()) {
            parser.parserError("Cannot find " + (!searchAtRoot ? "sub" : "") + "module '" + withoutFirstDotAndName + "' here", ident,
                    "Make sure you spelled the module name correctly and have defined the module already.");
            return Optional.empty();
        }

        return accessModule.get().find(ASTGenerator.identOf(name));
    }

    private void checkLocal(@NotNull final Node scope, @NotNull final MiFunctionScope functionScope, final boolean ignoreMissingReturns) {
        final MiInternFunction function = functionScope.function();

        for (@NotNull final Node child : scope.children()) {
            if (parser.encounteredError()) return;
            final Token first = child.value() == null ? child.child(0).value() : child.value();
            if (functionScope.hasReachedScopeEnd() || functionScope.hasReachedLoopEnd()) {
                parser.parserError("Unreachable statement", first,
                        "Delete the unreachable statement or move it above a return statement.");
            }
            switch (child.type()) {
                case FUNCTION_CALL -> checkFunctionCall(child, functionScope);
                case NOOP -> checkInnerScope(child, function, functionScope, false, false);
                case RETURN_STATEMENT -> checkReturnStatement(child, functionScope);
                case IF_STATEMENT -> checkConditionalStatement(child, function, functionScope, MiScopeType.IF);
                case WHILE_STATEMENT -> checkConditionalStatement(child, function, functionScope, MiScopeType.WHILE);
                case DO_STATEMENT -> checkDoWhileStatement(child, function, functionScope);
                case FOR_FAKE_SCOPE -> checkForStatement(child, function, functionScope);
                case BREAK_STATEMENT, CONTINUE_STATEMENT -> checkLoopStop(child, functionScope);
                case DECLARE_VARIABLE -> defineVariable(child, functionScope, false, false);
                case DEFINE_VARIABLE -> defineVariable(child, functionScope, true, false);
                case MUTATE_VARIABLE -> checkVariableMutation(child, functionScope, function.module());
                default -> {
                    parser.parserError("Not a statement.", first);
                    return;
                }
            }
        }
        if (functionScope.parent().isEmpty() && !functionScope.hasReachedScopeEnd() && !function.returnType().equals(MiDatatype.VOID) && !ignoreMissingReturns) {
            parser.parserError("Missing return statement", scope.value(),
                    "Add a return statement at the end of the function definition scope");
        }
    }

    private boolean expectBooleanCondition(@NotNull final ASTExpressionParser.TypedNode condition, @NotNull final String cond, @NotNull final Token condToken) {
        if (!condition.type().equals(MiDatatype.BOOL)) { // expect nonnull bool, nullable wont work here
            if (condition.type().name().equals(MiDatatype.BOOL.name()) && condition.type().nullable()) {
                parser.parserError("Expected a nonnull bool condition for " + cond + " statement, but got a " + condition.type() + ".", condToken,
                        "Put a different condition for the " + cond + " statement or cast the current condition to bool.",
                        "The condition appears to already be a boolean, but it is nullable. Use std.to_nonnull() to safely convert the condition to a nonnull bool."
                );
                return true;
            }
            parser.parserError("Expected a nonnull bool condition for " + cond + " statement, but got a " + condition.type() + ".", condToken,
                    "Put a different condition for the " + cond + " statement or cast the current condition to bool.");
            return true;
        }
        return false;
    }

    private void checkLoopStop(@NotNull final Node child, @NotNull final MiFunctionScope scope) {
        final Token statement = child.value();
        if (!scope.looping()) {
            parser.parserError("Unexpected " + statement.token() + "; Expected it inside of a loop scope", statement,
                    "Remove the " + statement.token() + " statement.");
            return;
        }
        scope.reachedLoopEnd();
    }

    private void checkConditionalStatement(@NotNull final Node child, @NotNull final MiInternFunction function, @NotNull final MiFunctionScope scope, @NotNull final MiScopeType conditional) {
        final Token condToken = child.value();
        final String cond = conditional.name().toLowerCase();
        final ASTExpressionParser.TypedNode condition = parseExpression(child.child(0).child(0), condToken, scope);
        if (expectBooleanCondition(condition, cond, condToken)) return;

        final Node conditionalScopeNode = child.child(1);
        final Node elseStatement = child.children().size() > 2 && conditional == MiScopeType.IF ? child.child(2) : null;

        final MiFunctionScope conditionalScope = new MiFunctionScope(conditional, function, scope);
        scope.childScope(conditionalScope);
        checkLocal(conditionalScopeNode, conditionalScope, false);
        conditionalScope.pop();

        if (elseStatement != null) checkInnerScope(elseStatement, function, scope, true, conditionalScope.rawReachedEnd()); // else is designed to look exactly like an inner scope
    }

    private void checkDoWhileStatement(@NotNull final Node child, @NotNull final MiInternFunction function, @NotNull final MiFunctionScope scope) {
        final Token doToken = child.value();
        final Node doScopeNode = child.child(0);
        final Node whileStatement = child.children().size() != 2 || child.child(1).type() != NodeType.WHILE_STATEMENT_UNSCOPED ? null : child.child(1);
        if (whileStatement == null) {
            parser.parserError("Expected 'while' without a scope after 'do' scope", doToken,
                    "Add a condition after the do statement scope to resolve the issue.");
            return;
        }
        final ASTExpressionParser.TypedNode condition = parseExpression(whileStatement.child(0).child(0), whileStatement.value(), scope);
        if (expectBooleanCondition(condition, "do", whileStatement.value())) return;

        final MiFunctionScope conditionalScope = new MiFunctionScope(MiScopeType.DO, function, scope);
        scope.childScope(conditionalScope);
        checkLocal(doScopeNode, conditionalScope, false);
        conditionalScope.pop();
    }

    private void checkForStatement(@NotNull final Node child, @NotNull final MiInternFunction function, @NotNull final MiFunctionScope scope) {
        final Node statement = child.child(0);
        final Node varDef = statement.child(0);
        final MiFunctionScope fakeScope = new MiFunctionScope(MiScopeType.FUNCTION_LOCAL, function, scope);
        // create a fake scope, embracing the for statement to allow for the index variable name to be reused (we simply delete the index variable after the for statement scope ends)
        scope.childScope(fakeScope);

        defineVariable(varDef, fakeScope, true, false);

        final Node conditionNode = statement.child(1).child(0);
        final ASTExpressionParser.TypedNode condition = parseExpression(conditionNode, conditionNode.value(), fakeScope);
        if (condition == null || condition.type() == null || expectBooleanCondition(condition, "for", statement.value())) return;

        final Node forInstruct = statement.child(2);
        checkLocal(forInstruct, fakeScope, true);

        final Node forScopeNode = child.child(1);

        final MiFunctionScope forScope = new MiFunctionScope(MiScopeType.FOR, function, fakeScope);
        fakeScope.childScope(forScope);
        checkLocal(forScopeNode, forScope, false);
        forScope.pop();

        fakeScope.pop();
    }

    private void checkInnerScope(@NotNull final Node child, @NotNull final MiInternFunction function,
                                 @NotNull final MiFunctionScope scope, final boolean elseScope, final boolean ifReachedEnd) {
        if (child.children().isEmpty()) return; // normal noop, no scope here

        final MiFunctionScope localScope = new MiFunctionScope(elseScope ? MiScopeType.ELSE : MiScopeType.FUNCTION_LOCAL, function, scope);
        if (ifReachedEnd) localScope.ifReachedEnd();
        scope.childScope(localScope);
        checkLocal(elseScope ? child : child.child(0), localScope, false);
        localScope.pop();
    }

    private void checkReturnStatement(@NotNull final Node child, @NotNull final MiFunctionScope scope) {
        final Node valueNode = child.children().isEmpty() ? null : child.child(0);
        final ASTExpressionParser.TypedNode value = valueNode == null ? null : parseExpression(valueNode, valueNode.value(), scope);
        if (value != null && value.type() == null) return;

        final MiDatatype returnDatatype = value == null ? MiDatatype.VOID : value.type();
        final MiDatatype functionReturnType = scope.function().returnType();

        if (!MiDatatype.match(returnDatatype, functionReturnType)) {
            final String err = "Return statement returns value of type " + returnDatatype + " while function return type is " + functionReturnType;
            final Token at = child.value() == null && valueNode != null ? valueNode.value() : child.value();

            if (returnDatatype.equals(MiDatatype.VOID)) {
                parser.parserError(err, at, "Return an 'empty' " + functionReturnType + " value or change the function return type to void.");
                return;
            }
            if (functionReturnType.equals(MiDatatype.VOID)) {
                parser.parserError(err, at, "Don't return any value or change the function return type to " + returnDatatype + ".");
                return;
            }

            parser.parserError(err, at, "Cast the return value to " + functionReturnType + " or change the function return type to fix this problem.");
            return;
        }
        if (scope.type() == MiScopeType.ELSE) {
            if (scope.ifHasReachedEnd()) scope.reachedScopeEnd();
            return;
        }
        scope.reachedScopeEnd();
    }

    protected void checkVariableMutation(@NotNull final Node child, final MiFunctionScope scope, @NotNull final MiModule module) {
        if (checkLocalVariableMutation(child, scope)) return;
        final Token ident = child.child(0).value();
        final Token operator = child.child(1).value();
        final Optional<MiEqualOperator> equalOperator = MiEqualOperator.of(operator.token());
        if (equalOperator.isEmpty()) {
            parser.parserError("Fatal parsing error; Equal operation is invalid '" + operator.token() + "'", operator);
            return;
        }

        final Optional<MiVariable> globalVariable = findGlobalVariableByAccess(ident, module);

        if (globalVariable.isEmpty()) {
            parser.parserError("Cannot find any variable called '" + ident.token() + "' here", ident,
                    "Are you sure you spelled the variable name correctly?" + (ident.token().contains(".") ? " Are you using the right module?" : ""));
            return;
        }
        child.child(0).value(globalVariable.get().identifier());
        if (equalOperator.get() != MiEqualOperator.SET && globalVariable.get().uninitialized()) {
            parser.parserError("Variable '" + ident.token() + "' might have not been initialized yet", ident,
                    "Give the variable an explicit value by using the normal set (=) operator");
            return;
        }
        if (MiDatatype.operatorUndefined(operator.token(), globalVariable.get().type().name())) {
            parser.parserError("Cannot use operator '" + operator.token() + "' for " + globalVariable.get().type() + " values.", operator);
            return;
        }
        checkInvalidGlobalVariableAccess(globalVariable.get(), module, ident);
        checkInvalidGlobalVariableMutation(globalVariable.get(), module, ident);
        globalVariable.get().initialize();
    }

    protected Optional<MiVariable> findVariable(@NotNull final Token ident, @NotNull final MiModule module, final MiFunctionScope function) {
        if (function == null) return findGlobalVariableByAccess(ident, module);
        final Optional<MiVariable> localVariable = function.find(ident.token());
        if (localVariable.isPresent()) return localVariable;

        return findGlobalVariableByAccess(ident, module);
    }

    private boolean checkLocalVariableMutation(@NotNull final Node child, final MiFunctionScope function) {
        if (function == null) return false;
        final Token ident = child.child(0).value();
        final Optional<MiVariable> variable = function.find(ident.token());
        if (variable.isEmpty()) return false;

        child.child(0).value(variable.get().identifier());
        final Token operator = child.child(1).value();
        final Optional<MiEqualOperator> equalOperator = MiEqualOperator.of(operator.token());
        if (equalOperator.isEmpty()) {
            parser.parserError("Fatal parsing error; Equal operation is invalid '" + operator.token() + "'", operator);
            return true;
        }
        if (equalOperator.get() != MiEqualOperator.SET && variable.get().uninitialized()) {
            parser.parserError("Variable '" + ident.token() + "' might have not been initialized yet", ident,
                    "Give the variable an explicit value by using the normal set (=) operator");
            return true;
        }
        if (MiDatatype.operatorUndefined(operator.token(), variable.get().type().name())) {
            parser.parserError("Cannot use operator '" + operator.token() + "' for " + variable.get().type() + " values.", operator);
            return true;
        }

        final boolean incDec = NodeType.of(operator).incrementDecrement();
        final ASTExpressionParser.TypedNode value = incDec ? null : parseExpression(child.child(2), operator, function);
        if (value == null && !incDec) return false;

        final Set<MiModifier> modifiers = variable.get().modifiers();
        final MiModifier mmodifier = MiModifier.effectiveMutabilityModifier(modifiers);

        if (MiModifier.invalidLocalMutation(variable.get())) {
            parser.parserError("Invalid variable mutation; Cannot change " + mmodifier.getName() + " local variable here", ident,
                    "Constants can only be initialized once, changing a constants value is not allowed.");
        }
        final MiDatatype valueType = incDec ? variable.get().type() : value.type();
        final MiDatatype varType = variable.get().type();
        if (valueType == null) return false;

        if (!MiDatatype.match(valueType, varType)) {
            parser.parserError("Invalid value type; Cannot assign " + valueType + " values to " + varType + " variables", operator,
                    (valueType != MiDatatype.NULL
                            ? "Cast the value to " + varType.name() + " or change the variable datatype to " + valueType + "."
                            : "Mark your variable as nullable or use std.to_nonnull() to safely convert a null-value to a nonnull-value."));
        }
        variable.get().initialize();
        return true;
    }

    private void checkInvalidGlobalVariableMutation(@NotNull final MiVariable globalVariable, @NotNull final MiModule ownModule, @NotNull final Token ident) {
        final Set<MiModifier> modifiers = globalVariable.modifiers();
        final MiModifier mmodifier = MiModifier.effectiveMutabilityModifier(modifiers);

        if (MiModifier.invalidGlobalMutation(globalVariable, ownModule)) {
            parser.parserError("Invalid variable mutation; Cannot change " + mmodifier.getName() + " global variable here", ident,
                    (mmodifier == MiModifier.CONST ?
                            "Constants can only be initialized once, changing a constants value is not allowed." :
                            "Variables marked as 'own' can only be modified inside of their own module, similar to the 'prot' modifier."));
        }
    }

    private void checkInvalidGlobalVariableAccess(@NotNull final MiVariable globalVariable, @NotNull final MiModule ownModule, @NotNull final Token ident) {
        final Set<MiModifier> modifiers = globalVariable.modifiers();
        final MiModifier vmodifier = MiModifier.effectiveVisibilityModifier(modifiers);
        final MiContainer variableContainer = globalVariable.container();
        if (!(variableContainer instanceof final MiModule variableModule)) throw new RuntimeException("Unexpected error; global variable container is not a module");

        if (MiModifier.invalidAccess(modifiers, variableModule, ownModule)) {
            parser.parserError("Invalid access; Cannot access " + vmodifier.getName() + " global variable from here", ident,
                    (vmodifier == MiModifier.PRIV ?
                            "Private variables can only be accessed when the accessing module and the variable module are the same."
                            : "Protected variables can only be accessed within their own module scope."));
        }
    }

    private ASTExpressionParser.TypedNode parseExpression(@NotNull final Node value, @NotNull final Token equalsToken, @NotNull final MiContainer container) {
        if (value.type() != NodeType.VALUE) throw new RuntimeException("Expected value node for expression");

        final ASTExpressionParser.TypedNode result = new ASTExpressionParser(value.children().stream().map(Node::value).toList(), equalsToken, this, container).parse();
        if (result != null && result.node() != null && result.type() != null) {
            value.children().clear();
            value.addChildren(new Node(NodeType.VALUE, -1, result.node()));
            value.addChildren(new Node(NodeType.TYPE, Token.of(result.type().name()), -1));
        }
        return result;
    }

    private void checkFunctionCall(@NotNull final Node child, @NotNull final MiFunctionScope calledFrom) {
        final Token ident = child.child(0).value();
        final List<ASTExpressionParser.TypedNode> callParamNodes = child
                .child(1)
                .children()
                .stream()
                .map(Node::children)
                .flatMap(Collection::stream)
                .map(n -> parseExpression(n, ident, calledFrom))
                .toList();

        final List<MiDatatype> callParams = callParamNodes
                .stream()
                .map(ASTExpressionParser.TypedNode::type)
                .toList();

        final Optional<MiFunction> callFunction = findFunctionByCall(ident, callParams, calledFrom.function().module());
        if (callFunction.isEmpty()) {
            parser.parserError("Cannot find any function called '" + ident.token() + "' with the specified arguments " + callParams + " here", ident,
                    "Are you sure you spelled the function name correctly? Are you using the right module?");
            return;
        }
        child.child(0).value(callFunction.get().identifier());
        final Set<MiModifier> modifiers = callFunction.get().modifiers();
        final MiModifier vmodifier = MiModifier.effectiveVisibilityModifier(modifiers);
        if (MiModifier.invalidAccess(modifiers, callFunction.get().module(), calledFrom.function().module())) {
            parser.parserError("Invalid access error; Cannot access " + vmodifier.getName() + " function from here", ident,
                    (vmodifier == MiModifier.PRIV ? "Private functions can only be accessed when the accessing module and the function module are the same."
                            : "Protected functions can only be accessed within their own module scope"));
        }
    }

    private static Class<?> primitiveToJavaType(@NotNull final MiDatatype type) {
        return switch (NodeType.of(type.name())) {
            case LITERAL_INT -> Integer.class;
            case LITERAL_LONG -> Long.class;
            case LITERAL_DOUBLE -> Double.class;
            case LITERAL_FLOAT -> Float.class;
            case LITERAL_BOOL -> Boolean.class;
            case LITERAL_STRING -> String.class;
            case LITERAL_CHAR -> Character.class;
            case LITERAL_VOID -> void.class;
            case LITERAL_NULL -> Object.class;
            default -> null;
        };
    }

    protected boolean verifyEnum(@NotNull final MiDatatype type, @NotNull final Node enumIdentNode, @NotNull final MiModule accessedAt) {
        final Token tok = enumIdentNode.value();
        final Optional<MiEnum> foundEnum = findEnumByName(tok, accessedAt);
        if (!type.primitive()) {
            if (foundEnum.isEmpty()) {
                parser.parserError("Cannot find an enum called '" + tok.token() + "' here", tok,
                        "Did you spell the enum name correctly? Are you sure you are using the right module?");
                return true;
            }
            final Set<MiModifier> enumModifiers = foundEnum.get().modifiers();
            final MiModifier vmodifier = MiModifier.effectiveVisibilityModifier(enumModifiers);
            if (MiModifier.invalidAccess(enumModifiers, foundEnum.get().module(), accessedAt)) {
                parser.parserError("Cannot access " + vmodifier.getName() + " enum '" + tok.token() + "' here", tok,
                        "Are you sure you are using the right module? Are you using the correct enum?");
                return true;
            }
            enumIdentNode.value(foundEnum.get().identifier());
        }
        return false;
    }

    private void defineVariable(@NotNull final Node node, @NotNull final MiContainer container, final boolean initialized, final boolean global) {
        final Token ident = node.child(1).value();
        if (ident.token().contains(".")) {
            parser.parserError("Variable identifiers may not contain dot characters", ident, "Remove any '.' characters in the variable name");
            return;
        }

        final List<MiModifier> modifiers = variableModifiers(node, initialized, ident, global); if (modifiers == null) return;

        final String name = ident.token();
        if (container.find(name).isPresent()) {
            if (container instanceof MiFunctionScope) {
                parser.parserError("A local variable with the same name '" + name + "' already exists in this scope", ident, "Rename your variable, or move it to its own local scope.");
                return;
            }
            parser.parserError("A global variable with the same name '" + name + "' already exists in this module", ident, "Rename your variable to fix this issue.");
            return;
        }
        final Token originalType = node.child(2).value();
        final MiDatatype type = MiDatatype.of(originalType.token(), modifiers.contains(MiModifier.NULLABLE));
        if (verifyEnum(type, node.child(2), container instanceof final MiFunctionScope scope ? scope.function().module() : ((MiModule) container))) return;

        if (!initialized) {
            final MiVariable variable = new MiVariable(container, name, type, modifiers, !(container instanceof MiFunctionScope));
            node.child(1).value(variable.identifier());
            container.add(variable);
            return;
        }
        final Node valueNode = node.child(3);

        final ASTExpressionParser.TypedNode value = parseExpression(valueNode, valueNode.value(), container); if (value == null) return;
        final MiDatatype valueType = value.type(); if (valueType == null) return;

        final MiVariable variable = new MiVariable(container, name, valueType.name().equals("null") ? type : valueType, modifiers, true);
        node.child(1).value(variable.identifier());

        if (type.equals(MiDatatype.AUTO, true)) {
            node.child(2).value(Token.of(valueType.name())); // set datatype for ? at compile time

            if (valueType.equals(MiDatatype.NULL, true)) {
                parser.parserError("Cannot find datatype for null-value at compile time", originalType,
                        "Change the datatype from ? to a definite type, like int for example. Make sure it's exactly the one you need.");
                return;
            }
            container.add(variable);
            return;
        }
        if (!MiDatatype.match(valueType, type)) {
            parser.parserError("Cannot assign " + valueType + " values to " + type + " variables.", node.value(),
                    (valueType != MiDatatype.NULL
                            ? "Cast the value to " + type + " or change the variable datatype to " + valueType + "."
                            : "Mark your variable as nullable or use std.to_nonnull() to safely convert a null-value to a nonnull-value."));
        }
        // initialized when this is global, so using this is possible, but when not initialized it is null.
        // when the variable is a local one, just use the initialized boolean for this
        container.add(variable);
    }

    private void defineEnum(@NotNull final Node node) {
        final Token ident = node.child(0).value();
        if (ident.token().contains(".")) {
            parser.parserError("Enum identifiers may not contain dot characters", ident, "Remove any '.' characters in the enum name");
            return;
        }

        if (currentModule.findEnumByName(ident.token()).isPresent()) {
            parser.parserError("An enum with the name '" + ident.token() + "' already exists in this module", ident,
                    "Rename the enum or move it to another module.");
            return;
        }
        final List<MiModifier> modifiers = enumModifiers(node, ident); if (modifiers == null) return;

        final Node scope = node.child(2);
        if (scope.children().size() != 1 && !scope.children().isEmpty()) {
            parser.parserError("Enum has more than one member definition list", scope.child(1).child(0).value(),
                    "Remove the extra enum definitions and only keep one.");
            return;
        }
        final List<String> members;
        if (scope.children().isEmpty()) {
            members = new ArrayList<>();
        } else {
            final Set<String> temp = new HashSet<>();
            final Optional<Token> firstDup = scope.child(0).children().stream().map(Node::value).filter(n -> !temp.add(n.token())).findFirst();
            if (firstDup.isPresent()) {
                parser.parserError("A duplicate enum member was found '" + firstDup.get().token() + "'", firstDup.get(),
                        "Remove the duplicate enum member or rename it.");
                return;
            }

            members = scope.child(0).children().stream().map(n -> n.value().token()).toList();
        }

        final MiEnum miEnum = new MiEnum(ident.token(), currentModule, modifiers, members);
        currentModule.enums().add(miEnum);
    }

    private Set<Map.Entry<Node, MiInternFunction>> defineModule(@NotNull final Node node) {
        final Token ident = node.child(0).value();
        if (ident.token().contains(".")) {
            parser.parserError("Module identifiers may not contain dot characters", ident, "Remove any '.' characters in the module name");
            return new HashSet<>();
        }

        final String name = ident.token();
        final MiModule sub = new MiModule(name, currentModule);
        if (currentModule.findSubmoduleByName(name).isPresent()) {
            parser.parserError("A " + (currentModule.parent().isPresent() ? "sub" : "")
                    + "module called '" + name + "' already exists here", ident, "Rename the module or move it somewhere else.");
            return new HashSet<>();
        }
        currentModule.submodules().add(sub);

        final MiModule parent = currentModule;
        currentModule = sub;
        final Set<Map.Entry<Node, MiInternFunction>> result = defineAllGlobal(node.child(1)); // define everything inside of the submodule
        currentModule = parent;
        return result;
    }

    private Map.Entry<Node, MiInternFunction> defineInternFunction(@NotNull final Node node) {
        final Token ident = functionName(node);
        if (ident.token().contains(".")) {
            parser.parserError("Function identifiers may not contain dot characters", ident, "Remove any '.' characters in the function name");
            return null;
        }

        final List<MiModifier> modifiers = functionModifiers(node); if (modifiers == null) return null;
        final List<MiVariable> params = functionParameters(node, currentModule); if (params == null) return null;
        final MiDatatype type = functionReturnType(node, ident, modifiers, currentModule);

        final MiInternFunction function = new MiInternFunction(modifiers, ident.token(), type, currentModule, params);
        tryAddFunction(function, ident);
        return parser.encounteredError() ? null : Map.entry(node.child(4), function);
    }

    private void defineNativeFunction(@NotNull final Node node) {
        final Token ident = functionName(node);
        if (ident.token().contains(".")) {
            parser.parserError("Function identifiers may not contain dot characters", ident, "Remove any '.' characters in the function name");
            return;
        }

        final List<MiModifier> modifiers = functionModifiers(node); if (modifiers == null) return;
        final List<MiVariable> params = functionParameters(node, currentModule); if (params == null) return;
        final MiDatatype type = functionReturnType(node, ident, modifiers, currentModule);

        final Method nativeMethod = functionNativeMethod(node, params, ident); if (nativeMethod == null) return;

        final MiNativeFunction function = new MiNativeFunction(nativeMethod, modifiers, ident.token(), type, currentModule, params);
        tryAddFunction(function, ident);
    }

    private void tryAddFunction(@NotNull final MiFunction function, @NotNull final Token ident) {
        if (checkFunctionAlreadyExists(function, ident)) return;
        currentModule.functions().add(function);
    }

    private Method functionNativeMethod(@NotNull final Node node, @NotNull final List<MiVariable> params, @NotNull final Token ident) {
        final Token nativeClassToken = node.child(4).value();
        final String nativeClassStr = nativeClassToken.token();
        final String nativeClassFullName = nativeClassStr.substring(1, nativeClassStr.length() - 1);
        try {
            final Class<?> nativeMethodClass = Class.forName(nativeClassFullName);
            final List<? extends Class<?>> paramTypesClasses = params
                    .stream()
                    .map(MiVariable::type)
                    .map(ASTRefiner::primitiveToJavaType)
                    .toList();

            return nativeMethodClass.getMethod(ident.token(), paramTypesClasses.toArray(new Class<?>[0]));
        } catch (final ClassNotFoundException e) {
            parser.parserError("Cannot find native java class '" + nativeClassFullName + "'", nativeClassToken);
            return null;
        } catch (final NoSuchMethodException e) {
            parser.parserError("Cannot find native java method '" + ident.token() + params.stream().map(MiVariable::type).toList() + "' in class '" + nativeClassFullName + "'", ident);
            return null;
        }
    }

    private MiDatatype functionReturnType(@NotNull final Node node, @NotNull final Token ident, @NotNull final List<MiModifier> modifiers, @NotNull final MiModule accessedAt) {
        final Token datatypeToken = node.child(1).value();
        if (NodeType.of(datatypeToken) == NodeType.LITERAL_VOID) {
            final Optional<MiModifier> firstNullability = modifiers.stream().filter(m -> m == MiModifier.NULLABLE || m == MiModifier.NONNULL).findFirst();
            if (firstNullability.isPresent()) {
                parser.parserError("Cannot mark void functions as nullable, nor as nonnull; No value is returned, marking no value as nullable or nonnull does not make sense", ident,
                        "Remove the nonnull / nullable modifier to fix this issue.");
            }
        }
        final boolean nullable = ASTGenerator.nullableModifiers(modifiers);
        final MiDatatype type = MiDatatype.of(datatypeToken.token(), nullable);
        verifyEnum(type, node.child(1), accessedAt);
        return MiDatatype.of(node.child(1).value().token(), nullable);
    }

    private List<MiVariable> functionParameters(@NotNull final Node node, @NotNull final MiModule accessedAt) {
        final List<Node> paramNodes = node.child(3).children();
        if (checkInvalidParameterModifiers(paramNodes)) return null;
        return parameters(paramNodes, accessedAt);
    }

    private boolean checkFunctionAlreadyExists(@NotNull final MiFunction function, @NotNull final Token ident) {
        if (currentModule.findFunction(function.name(), function.parameterTypes(), true).isPresent()) {
            final String args = function.parameterTypes().toString();
            parser.parserError("This function already exists in this module: " + function.name() + "(" + args.substring(1, args.length() - 1) + ")", ident,
                    "Rename your function, change up the parameters or move the function to another module to fix this problem.");
            return true;
        }
        return false;
    }

    private boolean checkInvalidModifiers(@NotNull final List<Node> modifiers) {
        final Optional<Token> firstDuplicate = MiModifier.firstDuplicate(modifiers);
        firstDuplicate.ifPresent(token -> parser.parserError("A duplicate modifier was found", token,
                "Duplicate modifiers don't make sense to have, so remove any duplicate to resolve this issue."));
        final Optional<Token> firstConflicting = MiModifier.firstConflicting(modifiers);
        firstConflicting.ifPresent(token -> parser.parserError("A conflicting modifier was found", token,
                "Some modifiers conflict with others and don't really make any sense when combined, so remove the conflicting modifier and keep the one you really need."));

        return parser.encounteredError();
    }

    private List<MiModifier> enumModifiers(@NotNull final Node node, @NotNull final Token ident) {
        final List<Node> modifiers = node.child(1).children();
        if (checkInvalidModifiers(modifiers) || checkInvalidEnumModifiers(modifiers, ident)) return null;
        final List<Optional<MiModifier>> modifs = ASTGenerator.modifiersOfNodes(modifiers);
        return definiteModifiers(modifs);
    }

    private List<MiModifier> variableModifiers(@NotNull final Node node, final boolean initialized, @NotNull final Token ident, final boolean global) {
        final List<Node> modifiers = node.child(0).children();
        if (checkInvalidModifiers(modifiers) || (global ? checkInvalidGlobalVariableModifiers(modifiers, initialized, ident) : checkInvalidLocalVariableModifiers(modifiers))) return null;
        final List<Optional<MiModifier>> modifs = ASTGenerator.modifiersOfNodes(modifiers);
        return definiteModifiers(modifs);
    }

    private List<MiModifier> functionModifiers(@NotNull final Node node) {
        final List<Node> modifiers = node.child(2).children();
        if (checkInvalidModifiers(modifiers) || checkInvalidFunctionModifiers(modifiers)) return null;
        final List<Optional<MiModifier>> modifs = ASTGenerator.modifiersOfNodes(modifiers);
        return definiteModifiers(modifs);
    }

    public static List<MiModifier> definiteModifiers(@NotNull final List<Optional<MiModifier>> modifiers) {
        return modifiers.stream().map(o -> o.orElseThrow(RuntimeException::new)).toList();
    }

    private static Token functionName(@NotNull final Node node) {
        return node.child(0).value();
    }

    private List<MiVariable> parameters(@NotNull final List<Node> paramNodes, @NotNull final MiModule accessedAt) {
        return paramNodes.stream().map(n -> {
            final List<Node> modifiers = n.child(2).children();

            final List<Optional<MiModifier>> modifs = ASTGenerator.modifiersOfNodes(modifiers);
            final List<MiModifier> modifsDefinite = modifs.stream().map(o -> o.orElseThrow(RuntimeException::new)).toList();
            final boolean nullable = ASTGenerator.nullableOptModifiers(modifs);
            final MiDatatype type = MiDatatype.of(n.child(0).value().token(), nullable);
            verifyEnum(type, n.child(0), accessedAt);

            return new MiVariable(n.child(1).value().token(), MiDatatype.of(n.child(0).value().token(), nullable), modifsDefinite);
        }).toList();
    }

    private boolean checkInvalidParameterModifiers(@NotNull final List<Node> paramNodes) {
        paramNodes.forEach(n -> {
            final List<Node> modifNodes = n.child(2).children();
            if (checkInvalidModifiers(modifNodes)) return;

            final List<Optional<MiModifier>> modifs = ASTGenerator.modifiersOfNodes(modifNodes);
            int i = 0;
            for (@NotNull final Optional<MiModifier> modifier : modifs) {
                if (modifier.isEmpty()) {
                    parser.parserError("Invalid parameter modifier found", modifNodes.get(i).value());
                    return;
                }
                if (modifier.get().visibilityModifier()) {
                    parser.parserError("Invalid parameter modifier found", modifNodes.get(i).value(),
                            "Cannot use visibility modifiers like pub, priv, prot and own on parameter variables. They would have no effect anyway, so removing them is no issue.");
                    return;
                }
                i++;
            }
        });
        return parser.encounteredError();
    }

    private boolean checkInvalidFunctionModifiers(@NotNull final List<Node> modifNodes) {
        final List<Optional<MiModifier>> modifs = ASTGenerator.modifiersOfNodes(modifNodes);
        int i = 0;
        for (@NotNull final Optional<MiModifier> modifier : modifs) {
            if (modifier.isEmpty()) {
                parser.parserError("Invalid function modifier found", modifNodes.get(i).value());
                return true;
            }
            if (modifier.get().mutabilityModifier()) {
                parser.parserError("Invalid function modifier found", modifNodes.get(i).value(),
                        "Cannot use mutability modifiers like mut, const and own on functions. They would have no effect anyway, so removing them is no issue.");
                return true;
            }
            i++;
        }
        return false;
    }

    private boolean checkInvalidGlobalVariableModifiers(@NotNull final List<Node> modifNodes, final boolean initialized, @NotNull final Token ident) {
        final List<Optional<MiModifier>> modifs = ASTGenerator.modifiersOfNodes(modifNodes);
        boolean constVar = true;
        int i = 0;
        for (@NotNull final Optional<MiModifier> modifier : modifs) {
            if (modifier.isEmpty()) {
                parser.parserError("Invalid variable modifier found", modifNodes.get(i).value());
                return true;
            }
            switch (modifier.get()) {
                case MUT, OWN -> constVar = false;
                case NAT, INTERN -> {
                    parser.parserError("Invalid variable modifier; only allowed modifiers for global variables are mut, const, own, nullable, nonnull, pub, priv and prot", modifNodes.get(i).value());
                    return true;
                }
            }
            i++;
        }
        if (constVar && !initialized) {
            parser.parserError("Invalid global constant; Cannot have an immutable global constant without an initial value", ident,
                    "Make your global constant a variable by adding the 'mut' modifier, or give it an initial value.");
            return true;
        }
        return false;
    }

    private boolean checkInvalidLocalVariableModifiers(@NotNull final List<Node> modifNodes) {
        final List<Optional<MiModifier>> modifs = ASTGenerator.modifiersOfNodes(modifNodes);
        int i = 0;
        for (@NotNull final Optional<MiModifier> modifier : modifs) {
            if (modifier.isEmpty()) {
                parser.parserError("Invalid variable modifier found", modifNodes.get(i).value());
                return true;
            }
            switch (modifier.get()) {
                case NAT, INTERN, PUB, PRIV, PROT, OWN -> {
                    parser.parserError("Invalid variable modifier; only allowed modifiers for local variables are mut, const, nullable and nonnull", modifNodes.get(i).value());
                    return true;
                }
            }
            i++;
        }
        return false;
    }

    private boolean checkInvalidEnumModifiers(@NotNull final List<Node> modifNodes, @NotNull final Token ident) {
        final List<Optional<MiModifier>> modifs = ASTGenerator.modifiersOfNodes(modifNodes);
        int i = 0;
        for (@NotNull final Optional<MiModifier> modifier : modifs) {
            if (modifier.isEmpty()) {
                parser.parserError("Invalid enum modifier found", modifNodes.get(i).value());
                return true;
            }
            switch (modifier.get()) {
                case MUT, NAT, OWN, CONST, INTERN, NULLABLE, NONNULL -> {
                    parser.parserError("Invalid enum modifier; only allowed modifiers for enums are pub, priv and prot", modifNodes.get(i).value(),
                            "Remove the invalid modifier(s)");
                    return true;
                }
            }
            i++;
        }
        return false;
    }

    public Node AST() {
        return AST;
    }
}
