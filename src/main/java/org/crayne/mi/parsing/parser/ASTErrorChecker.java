package org.crayne.mi.parsing.parser;

import org.crayne.mi.lang.*;
import org.crayne.mi.parsing.ast.Node;
import org.crayne.mi.parsing.ast.NodeType;
import org.crayne.mi.parsing.lexer.Token;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.*;

public class ASTErrorChecker {

    private final Parser parser;
    private Node AST;
    private MiModule currentModule = new MiModule("!PARENT");
    private final MiModule rootModule = currentModule;
    private MiFunctionScope currentFunctionScope = null;

    public ASTErrorChecker(@NotNull final Parser parser) {
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
                case NATIVE_FUNCTION_DEFINITION -> defineNativeFunction(child);
                case DECLARE_VARIABLE -> defineGlobalVariable(child, false);
                case DEFINE_VARIABLE -> defineGlobalVariable(child, true);
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
    }

    private Optional<MiModule> findModuleByName(@NotNull final String dotted, final boolean goFromRoot, @NotNull final MiInternFunction usedAt, @NotNull final Token ident) {
        // calling a function with the . as the first character means explicitly going from
        // root and checking all submodules from there
        MiModule current = goFromRoot ? rootModule : usedAt.module();

        for (@NotNull final String subMod : dotted.split("\\.")) {
            if (subMod.isEmpty()) continue;
            if (current == null) break;
            final Optional<MiModule> submodule = current.findSubmoduleByName(subMod);
            current = submodule.orElse(null);
        }
        return current == null && !goFromRoot ? findModuleByName(dotted, true, usedAt, ident) : Optional.ofNullable(current);
    }

    private Optional<MiFunction> findFunctionByCall(@NotNull final Token ident, @NotNull final List<MiDatatype> callParams, @NotNull final MiInternFunction calledFrom) {
        final String name = ident.token();
        if (!name.contains(".")) {
            // calling a function without specifying the module means either using
            // a local function in the current scope, or a function in the root scope (standard library functions)
            return Optional.ofNullable(calledFrom.module().findFunction(name, callParams)
                    .orElse(rootModule.findFunction(name, callParams).orElse(null)));
        }
        final boolean searchAtRoot = name.startsWith(".");
        final String withoutFirstDotAndName = ASTGenerator.moduleOf(searchAtRoot ? name.substring(1) : name);
        final Optional<MiModule> callModule = findModuleByName(withoutFirstDotAndName, searchAtRoot, calledFrom, ident);
        if (callModule.isEmpty()) {
            parser.parserError("Cannot find " + (!searchAtRoot ? "sub" : "") + "module '" + withoutFirstDotAndName + "' here", ident,
                    "Make sure you spelled the module name correctly and have defined the module already.");
            return Optional.empty();
        }
        return callModule.get().findFunction(ASTGenerator.identOf(name), callParams);
    }

    private Optional<MiVariable> findGlobalVariableByAccess(@NotNull final Token ident, @NotNull final MiInternFunction accessedFrom) {
        final String name = ident.token();
        if (!name.contains(".")) {
            // using a variable without specifying the module means either using
            // a local variable, a global variable in the own module, or a variable in the root scope (standard library functionality)
            return Optional.ofNullable(accessedFrom.module().find(name) // TODO local variables here
                    .orElse(rootModule.find(name).orElse(null)));
        }

        final boolean searchAtRoot = name.startsWith(".");
        final String withoutFirstDotAndName = ASTGenerator.moduleOf(searchAtRoot ? name.substring(1) : name);
        final Optional<MiModule> accessModule = findModuleByName(withoutFirstDotAndName, searchAtRoot, accessedFrom, ident);
        if (accessModule.isEmpty()) {
            parser.parserError("Cannot find " + (!searchAtRoot ? "sub" : "") + "module '" + withoutFirstDotAndName + "' here", ident,
                    "Make sure you spelled the module name correctly and have defined the module already.");
            return Optional.empty();
        }

        return accessModule.get().find(ASTGenerator.identOf(name));
    }

    private void checkLocal(@NotNull final Node scope, @NotNull final MiInternFunction function) {
        for (@NotNull final Node child : scope.children()) {
            if (parser.encounteredError()) return;
            switch (child.type()) {
                case FUNCTION_CALL -> checkFunctionCall(child, function);
                case MUTATE_VARIABLE -> checkVariableMutation(child, function);
                case CREATE_MODULE -> parser.parserError("Unexpected module definition inside of a function", child.child(0).value(),
                        "Cannot create modules inside of functions, so move the module definition out of this scope");
                case FUNCTION_DEFINITION, NATIVE_FUNCTION_DEFINITION -> parser.parserError("Unexpected function definition inside of another function", child.child(0).value(),
                        "Cannot create nested functions, so move the function definition out of this scope");
            }
        }
    }

    private void checkVariableMutation(@NotNull final Node child, @NotNull final MiInternFunction function) {
        final Token ident = child.child(0).value();
        final Optional<MiVariable> globalVariable = findGlobalVariableByAccess(ident, function);
        if (globalVariable.isEmpty()) {
            parser.parserError("Cannot find any variable called '" + ident.token() + "' here", ident,
                    "Are you sure you spelled the variable name correctly? Are you using the right module?");
            return;
        }
        final MiModule ownModule = function.module();
        checkInvalidGlobalVariableAccess(globalVariable.get(), ownModule, ident);
        checkInvalidGlobalVariableMutation(globalVariable.get(), ownModule, ident);
    }

    private void checkInvalidGlobalVariableMutation(@NotNull final MiVariable globalVariable, @NotNull final MiModule ownModule, @NotNull final Token ident) {
        final Set<MiModifier> modifiers = globalVariable.modifiers();
        final MiModifier mmodifier = MiModifier.effectiveMutabilityModifier(modifiers);

        if (MiModifier.invalidGlobalMutation(globalVariable, ownModule)) {
            parser.parserError("Invalid variable mutation; Cannot change " + mmodifier.getName() + " global variable from here", ident,
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

    private void checkFunctionCall(@NotNull final Node child, @NotNull final MiInternFunction calledFrom) {
        final Token ident = child.child(0).value();
        final List<MiDatatype> callParams = child
                .child(1)
                .children()
                .stream()
                .map(n -> MiDatatype.of(n.child(1).value().token()))
                .toList();

        final Optional<MiFunction> callFunction = findFunctionByCall(ident, callParams, calledFrom);
        if (callFunction.isEmpty()) {
            parser.parserError("Cannot find any function called '" + ident.token() + "' with the specified arguments " + callParams + " here", ident,
                    "Are you sure you spelled the function name correctly? Are you using the right module?");
            return;
        }
        final Set<MiModifier> modifiers = callFunction.get().modifiers();
        final MiModifier vmodifier = MiModifier.effectiveVisibilityModifier(modifiers);
        if (MiModifier.invalidAccess(modifiers, callFunction.get().module(), calledFrom.module())) {
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

    private void defineGlobalVariable(@NotNull final Node node, final boolean initialized) {
        final Token ident = node.child(1).value();
        final List<MiModifier> modifiers = variableModifiers(node, initialized, ident); if (modifiers == null) return;

        final String name = ident.token();
        if (currentModule.find(name).isPresent()) {
            parser.parserError("A global variable with the same name '" + name + "' already exists in this module", ident, "Rename your variable to fix this issue.");
            return;
        }
        final MiDatatype type = MiDatatype.of(node.child(2).value().token(), modifiers.contains(MiModifier.NULLABLE));
        final MiVariable variable = new MiVariable(currentModule, name, type, modifiers, true); // initialized because this is global, so using this is possible, but when not initialized it is null.

        currentModule.add(variable);
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
        final MiDatatype type = functionReturnType(node, modifiers);

        final MiInternFunction function = new MiInternFunction(modifiers, ident.token(), type, currentModule, params);
        tryAddFunction(function, ident);
        return parser.encounteredError() ? null : Map.entry(node.child(4), function);
    }

    private void defineNativeFunction(@NotNull final Node node) {
        final Token ident = functionName(node);
        final List<MiModifier> modifiers = functionModifiers(node); if (modifiers == null) return;
        final List<MiVariable> params = functionParameters(node); if (params == null) return;
        final MiDatatype type = functionReturnType(node, modifiers);

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
                    .map(ASTErrorChecker::primitiveToJavaType)
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

    private MiDatatype functionReturnType(@NotNull final Node node, @NotNull final List<MiModifier> modifiers) {
        return MiDatatype.of(node.child(1).value().token(), ASTGenerator.nullableModifiers(modifiers));
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
        firstDuplicate.ifPresent(token -> parser.parserError("A duplicate function modifier was found", token,
                "Duplicate modifiers don't make sense to have, so remove any duplicate to resolve this issue."));
        final Optional<Token> firstConflicting = MiModifier.firstConflicting(modifiers);
        firstConflicting.ifPresent(token -> parser.parserError("A conflicting function modifier was found", token,
                "Some modifiers conflict with others and don't really make any sense when combined, so remove the conflicting modifier and keep the one you really need."));

        return parser.encounteredError();
    }

    private List<MiModifier> variableModifiers(@NotNull final Node node, final boolean initialized, @NotNull final Token ident) {
        final List<Node> modifiers = node.child(0).children();
        if (checkInvalidModifiers(modifiers) || checkInvalidGlobalVariableModifiers(modifiers, initialized, ident)) return null;
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
            if (modifier.get() == MiModifier.MUT || modifier.get() == MiModifier.OWN) constVar = false;
            i++;
        }
        if (constVar && !initialized) {
            parser.parserError("Invalid global constant; Cannot have an immutable global constant without an initial value", ident,
                    "Make your global constant a variable by adding the 'mut' modifier, or give it an initial value.");
            return true;
        }
        return false;
    }

    public Node AST() {
        return AST;
    }
}
