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
                case ENUM_VALUES -> {
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

        checkLocal(parentFunctionScope, function);
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
            return Optional.ofNullable(calledFrom.findFunction(name, callParams)
                    .orElse(rootModule.findFunction(name, callParams).orElse(null)));
        }
        final boolean searchAtRoot = name.startsWith(".");
        final String withoutFirstDotAndName = ASTGenerator.moduleOf(searchAtRoot ? name.substring(1) : name);
        final Optional<MiModule> callModule = findModuleByName(withoutFirstDotAndName, searchAtRoot, calledFrom);
        if (callModule.isEmpty()) {
            parser.parserError("Cannot find " + (!searchAtRoot ? "sub" : "") + "module '" + withoutFirstDotAndName + "' here", ident,
                    "Make sure you spelled the module name correctly and have defined the module already.");
            return Optional.empty();
        }
        return callModule.get().findFunction(ASTGenerator.identOf(name), callParams);
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

    private void checkLocal(@NotNull final Node scope, @NotNull final MiFunctionScope functionScope) {
        final MiInternFunction function = functionScope.function();

        for (@NotNull final Node child : scope.children()) {
            if (parser.encounteredError()) return;
            switch (child.type()) {
                case FUNCTION_CALL -> checkFunctionCall(child, function.module());
                case SCOPE -> {
                    final MiFunctionScope localScope = new MiFunctionScope(function, functionScope);
                    checkLocal(child, localScope);
                    localScope.pop();
                }
                case ENUM_VALUES -> {
                    final Token ident = child.child(0).value();
                    parser.parserError("Unexpected token '" + ident.token() + "'", ident);
                    return;
                }
                case DECLARE_VARIABLE -> defineVariable(child, functionScope, false, false);
                case DEFINE_VARIABLE -> defineVariable(child, functionScope, true, false);
                case MUTATE_VARIABLE -> checkVariableMutation(child, functionScope, function.module());
                case CREATE_ENUM -> parser.parserError("Unexpected enum definition inside of a function", child.child(0).value(),
                        "Cannot create enums inside of functions, so move the enum definition out of this scope");
                case CREATE_MODULE -> parser.parserError("Unexpected module definition inside of a function", child.child(0).value(),
                        "Cannot create modules inside of functions, so move the module definition out of this scope");
                case FUNCTION_DEFINITION, NATIVE_FUNCTION_DEFINITION -> parser.parserError("Unexpected function definition inside of another function", child.child(0).value(),
                        "Cannot create nested functions, so move the function definition out of this scope");
            }
        }
    }

    protected void checkVariableMutation(@NotNull final Node child, final MiFunctionScope scope, @NotNull final MiModule module) {
        if (checkLocalVariableMutation(child, scope)) return;
        final Token ident = child.child(0).value();

        final Optional<MiVariable> globalVariable = findGlobalVariableByAccess(ident, module);

        if (globalVariable.isEmpty()) {
            parser.parserError("Cannot find any variable called '" + ident.token() + "' here", ident,
                    "Are you sure you spelled the variable name correctly?" + (ident.token().contains(".") ? " Are you using the right module?" : ""));
            return;
        }
        checkInvalidGlobalVariableAccess(globalVariable.get(), module, ident);
        checkInvalidGlobalVariableMutation(globalVariable.get(), module, ident);
        globalVariable.get().initialize();
    }

    protected Optional<MiVariable> findVariable(@NotNull final Token ident, @NotNull final MiModule module, final MiInternFunction function) {
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
        final Token operator = child.child(1).value();
        final ASTExpressionParser.TypedNode value = NodeType.of(operator).incrementDecrement() ? null : parseExpression(child.child(2), operator, function);
        if (value == null) return false;

        final Set<MiModifier> modifiers = variable.get().modifiers();
        final MiModifier mmodifier = MiModifier.effectiveMutabilityModifier(modifiers);

        if (MiModifier.invalidLocalMutation(variable.get())) {
            parser.parserError("Invalid variable mutation; Cannot change " + mmodifier.getName() + " local variable here", ident,
                    "Constants can only be initialized once, changing a constants value is not allowed.");
        }
        final MiDatatype valueType = value.type();
        final MiDatatype varType = variable.get().type();
        if (valueType == null) return false;

        if (!MiDatatype.match(valueType, varType)) {
            parser.parserError("Invalid value type; Cannot assign " + valueType.name() + " values to " + varType.name() + " variables", operator,
                    "Change the variable type to " + valueType.name() + " or cast the value to " + varType.name() + ".");
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
        if (result != null && result.node() != null) {
            value.children().clear();
            value.addChildren(result.node());
        }
        return result;
    }

    private void checkFunctionCall(@NotNull final Node child, @NotNull final MiModule calledFrom) {
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

        final Optional<MiFunction> callFunction = findFunctionByCall(ident, callParams, calledFrom);
        if (callFunction.isEmpty()) {
            parser.parserError("Cannot find any function called '" + ident.token() + "' with the specified arguments " + callParams + " here", ident,
                    "Are you sure you spelled the function name correctly? Are you using the right module?");
            return;
        }
        final Set<MiModifier> modifiers = callFunction.get().modifiers();
        final MiModifier vmodifier = MiModifier.effectiveVisibilityModifier(modifiers);
        if (MiModifier.invalidAccess(modifiers, callFunction.get().module(), calledFrom)) {
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

    private void defineVariable(@NotNull final Node node, @NotNull final MiContainer container, final boolean initialized, final boolean global) {
        final Token ident = node.child(1).value();
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
        final Node valueNode = node.child(3);

        final ASTExpressionParser.TypedNode value = parseExpression(valueNode, valueNode.value(), container); if (value == null) return;
        final MiDatatype valueType = value.type(); if (valueType == null) return;

        final MiVariable variable = new MiVariable(container, name, valueType, modifiers, !(container instanceof MiFunctionScope) || initialized);

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
            parser.parserError("Cannot assign " + valueType.name() + " values to " + type.name() + " variables.", node.child(3).child(0).value(),
                    "Cast the value to " + type.name() + " or change the variable datatype to " + valueType.name());
        }
        // initialized when this is global, so using this is possible, but when not initialized it is null.
        // when the variable is a local one, just use the initialized boolean for this
        container.add(variable);
    }

    private void defineEnum(@NotNull final Node node) {
        final Token ident = node.child(0).value();
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
        final List<MiModifier> modifiers = functionModifiers(node); if (modifiers == null) return null;
        final List<MiVariable> params = functionParameters(node); if (params == null) return null;
        final MiDatatype type = functionReturnType(node, ident, modifiers);

        final MiInternFunction function = new MiInternFunction(modifiers, ident.token(), type, currentModule, params);
        tryAddFunction(function, ident);
        return parser.encounteredError() ? null : Map.entry(node.child(4), function);
    }

    private void defineNativeFunction(@NotNull final Node node) {
        final Token ident = functionName(node);
        final List<MiModifier> modifiers = functionModifiers(node); if (modifiers == null) return;
        final List<MiVariable> params = functionParameters(node); if (params == null) return;
        final MiDatatype type = functionReturnType(node, ident, modifiers);

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

    private MiDatatype functionReturnType(@NotNull final Node node, @NotNull final Token ident, @NotNull final List<MiModifier> modifiers) {
        final Token datatypeToken = node.child(1).value();
        if (NodeType.of(datatypeToken) == NodeType.LITERAL_VOID) {
            final Optional<MiModifier> firstNullability = modifiers.stream().filter(m -> m == MiModifier.NULLABLE || m == MiModifier.NONNULL).findFirst();
            if (firstNullability.isPresent()) {
                parser.parserError("Cannot mark void functions as nullable, nor as nonnull; No value is returned, marking no value as nullable or nonnull does not make sense", ident,
                        "Remove the nonnull / nullable modifier to fix this issue.");
            }
        }
        return MiDatatype.of(datatypeToken.token(), ASTGenerator.nullableModifiers(modifiers));
    }

    private List<MiVariable> functionParameters(@NotNull final Node node) {
        final List<Node> paramNodes = node.child(3).children();
        if (checkInvalidParameterModifiers(paramNodes)) return null;
        return parameters(paramNodes);
    }

    private boolean checkFunctionAlreadyExists(@NotNull final MiFunction function, @NotNull final Token ident) {
        if (currentModule.findFunction(function.name(), function.parameterTypes()).isPresent()) {
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

    private List<MiVariable> parameters(@NotNull final List<Node> paramNodes) {
        return paramNodes.stream().map(n -> {
            final List<Node> modifiers = n.child(2).children();

            final List<Optional<MiModifier>> modifs = ASTGenerator.modifiersOfNodes(modifiers);
            final List<MiModifier> modifsDefinite = modifs.stream().map(o -> o.orElseThrow(RuntimeException::new)).toList();

            return new MiVariable(n.child(1).value().token(), MiDatatype.of(n.child(0).value().token(), ASTGenerator.nullableOptModifiers(modifs)), modifsDefinite);
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
