package org.crayne.mu.runtime.parsing.parser;

import org.apache.commons.lang3.StringUtils;
import org.crayne.mu.lang.Module;
import org.crayne.mu.lang.*;
import org.crayne.mu.runtime.parsing.ast.Node;
import org.crayne.mu.runtime.parsing.ast.NodeType;
import org.crayne.mu.runtime.parsing.lexer.Token;
import org.crayne.mu.runtime.parsing.parser.scope.EnumScope;
import org.crayne.mu.runtime.parsing.parser.scope.FunctionScope;
import org.crayne.mu.runtime.parsing.parser.scope.Scope;
import org.crayne.mu.runtime.parsing.parser.scope.ScopeType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class ParserEvaluator {

    final Parser parser;

    public ParserEvaluator(@NotNull final Parser parser) {
        this.parser = parser;
    }

    private static boolean restrictedName(@NotNull final String name, @NotNull final IdentifierType identifierType) {
        return (identifierType != IdentifierType.MODULE && name.contains(".")) || name.endsWith(".") || name.startsWith(".");
    }

    private boolean handleRestrictedName(@NotNull final Token name, @NotNull final IdentifierType identifierType, final boolean constVar) {
        final String tok = name.token();
        if (!restrictedName(tok, identifierType)) {
            warnUnconventional(name, identifierType, constVar);
            return false;
        }
        parser.parserError("Cannot use restricted name '" + tok + "'", name,
                "'.' characters are only allowed to be inserted into module identifiers",
                "No identifier may start / end with a '.' character");
        return true;
    }

    private static boolean isMixedCaseCapitalized(@NotNull final String ident) {
        return !ident.isEmpty() && StringUtils.isMixedCase(ident) && Character.isUpperCase(ident.charAt(0));
    }

    private void warnUnconventional(@NotNull final Token name, @NotNull final IdentifierType identifierType, final boolean constVar) {
        if (!parser.skimming) return;
        final String tok = name.token();
        final String nounder = tok.replace("_", "");
        final String identName = identifierType.name().toLowerCase();

        if (!nounder.isEmpty() && identifierType == IdentifierType.ENUM_MEMBER && !Character.isUpperCase(nounder.charAt(0))) {
            parser.parserWarning(
                    "Name '" + tok + "' does not follow Mu conventions; Enum members should be capitalized, but encountered an uncapitalized enum member name", name);
        }
        if (identifierType != IdentifierType.ENUM && identifierType != IdentifierType.ENUM_MEMBER && isMixedCaseCapitalized(nounder)) {
            parser.parserWarning(
                    "Name '" + tok + "' does not follow Mu conventions; Only enum names and members should be capitalized, but encountered a capitalized " + identName + " name",
                    name, "Variable, module and function names should be uncapitalized");
        }
        if (identifierType == IdentifierType.ENUM && !isMixedCaseCapitalized(nounder)) {
            parser.parserWarning(
                    "Name '" + tok + "' does not follow Mu conventions; Enum names should be capitalized, but encountered an uncapitalized enum name", name);
        }

        if (!constVar && StringUtils.isAllUpperCase(nounder)) {
            parser.parserWarning("Name '" + tok + "' does not follow Mu conventions; Only constant variables should be uppercase, but encountered an uppercase " + identName + " name",
                    name, "Anything that is not constant should not be capitalized");
        }
    }

    protected void addGlobalVarFromResult(@NotNull final Node result) {
        final Module module = parser.lastModule();
        final List<Modifier> modifiers = result.child(0).children().stream().map(n -> Modifier.of(n.type())).toList();

        final Optional<Scope> current = parser.scope();
        if (!parser.stdlib && (current.isEmpty() || current.get().type() != ScopeType.MODULE)) {
            parser.parserError("Cannot define global variables at root level",
                    "Move your variable into a module or create a new module for it");
            return;
        }
        final Datatype datatype = Datatype.of(parser, Collections.emptyList(), result.child(2).value());
        if (datatype == null) return;

        final Token ident = result.child(1).value();
        final Variable var = new Variable(
                ident.token(),
                datatype,
                modifiers,
                result.children().size() == 4
        );

        if (handleRestrictedName(ident, IdentifierType.VARIABLE, var.isConstant())) return;

        module.addGlobalVariable(parser, var);
    }

    public void addLocalVarFromResult(@NotNull final Node result) {
        final List<Modifier> modifiers = result.child(0).children().stream().map(n -> Modifier.of(n.type())).toList();

        final Token ident = result.child(1).value();
        final FunctionScope functionScope = expectFunctionScope(ident);
        final Datatype datatype = Datatype.of(parser, result.child(2).value());
        if (functionScope == null || datatype == null) return;

        final Variable var = new Variable(
                ident.token(),
                datatype,
                modifiers,
                result.children().size() == 4
        );

        if (handleRestrictedName(ident, IdentifierType.VARIABLE, var.isConstant())) return;

        functionScope.addLocalVariable(parser, var);
    }

    protected void addFunctionFromResult(@NotNull final Node result) {
        final Module module = parser.lastModule();
        try {
            final List<Modifier> modifiers = result.child(2).children().stream().map(n -> Modifier.of(n.type())).toList();

            final List<Node> paramNodes = result.child(3).children().stream().toList();

            final List<FunctionParameter> params = paramNodes.stream().map(n -> {
                final Datatype datatype = Datatype.of(parser, n.child(0).value());
                if (datatype == null) throw new NullPointerException();
                return new FunctionParameter(
                        datatype,
                        n.child(1).value().token(),
                        n.child(2).children().stream().map(n2 -> Modifier.of(n2.type())).toList()
                );
            }).toList();

            final Token nameToken = result.child(0).value();
            if (handleRestrictedName(nameToken, IdentifierType.FUNCTION, false)) return;

            final Datatype datatype = Datatype.of(parser, result.child(1).value());
            if (datatype == null) throw new NullPointerException();

            if (result.children().size() == 5) {
                module.addFunction(parser, nameToken, new FunctionDefinition(
                        nameToken.token(),
                        datatype,
                        params,
                        modifiers,
                        result.child(4)
                ));
                return;
            }
            module.addFunction(parser, nameToken, new FunctionDefinition(
                    nameToken.token(),
                    datatype,
                    params,
                    modifiers
            ));
        } catch (final NullPointerException e) {
            e.printStackTrace();
            parser.output.errorMsg("Could not parse function definition");
        }
    }

    protected void addNativeFunctionFromResult(@NotNull final Node result, final Method nativeMethod) {
        final Module module = parser.lastModule();
        try {
            final List<Modifier> modifiers = result.child(2).children().stream().map(n -> Modifier.of(n.type())).toList();

            final List<Node> paramNodes = result.child(3).children().stream().toList();

            final List<FunctionParameter> params = paramNodes.stream().map(n -> {
                final Datatype datatype = Datatype.of(parser, n.child(0).value());
                if (datatype == null) throw new NullPointerException();
                return new FunctionParameter(
                        datatype,
                        n.child(1).value().token(),
                        n.child(2).children().stream().map(n2 -> Modifier.of(n2.type())).toList()
                );
            }).toList();

            final Token nameToken = result.child(0).value();
            if (handleRestrictedName(nameToken, IdentifierType.FUNCTION, false)) return;

            final Datatype datatype = Datatype.of(parser, result.child(1).value());
            if (datatype == null) throw new NullPointerException();

            if (result.children().size() == 5) {
                if (nativeMethod != null) {
                    module.addFunction(parser, nameToken, new FunctionDefinition(
                            nameToken.token(),
                            datatype,
                            params,
                            modifiers,
                            nativeMethod
                    ));
                    return;
                }
                module.addFunction(parser, nameToken, new FunctionDefinition(
                        nameToken.token(),
                        datatype,
                        params,
                        modifiers,
                        result.child(4)
                ));
            }
        } catch (final NullPointerException e) {
            e.printStackTrace();
            parser.output.errorMsg("Could not parse function definition");
        }
    }

    public Node evalReturnStatement(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        if (unexpectedModifiers(modifiers)) return null;

        final Token ret = parser.getAndExpect(tokens, 0, NodeType.LITERAL_RET, NodeType.DOUBLE_COLON);
        if (ret == null) return null;

        final FunctionScope functionScope = expectFunctionScope(tokens.get(0));
        if (functionScope == null) return null;

        final Datatype expectedType = parser.currentFuncReturnType;
        if (expectedType == null) { // should never happen but just in case i guess?
            parser.parserError("Unexpected parsing error, the datatype of the current function is unknown");
            return null;
        }
        if (tokens.size() == 2) { // ret ; are two tokens
            if (expectedType != Datatype.VOID) {
                parser.parserError("Expected datatype of return value to be " + expectedType.getName() + ", but got Void instead", tokens.get(1));
                return null;
            }
            functionScope.reachedEnd();
            return new Node(NodeType.RETURN_VALUE);
        }

        final ValueParser.TypedNode retVal = parseExpression(tokens.subList(1, tokens.size() - 1));
        if (retVal == null || retVal.type() == null || retVal.node() == null) return null;

        if (!retVal.type().equals(expectedType)) {
            parser.parserError("Expected datatype of return value to be " + expectedType.getName() + ", but got " + retVal.type().getName() + " instead", tokens.get(1));
            return null;
        }
        functionScope.reachedEnd();

        return new Node(NodeType.RETURN_VALUE, new Node(NodeType.VALUE, retVal.node()));
    }

    public Node evalFirstIdentifier(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        final Token secondToken = parser.getAny(tokens, 1);
        if (Parser.anyNull(secondToken)) return null;

        final NodeType second = NodeType.of(secondToken);
        if (second == NodeType.LPAREN) return evalFunctionCall(tokens, modifiers);
        else if (second == NodeType.COMMA || second == NodeType.SEMI) return evalEnumMembers(tokens, modifiers);
        else if (second == NodeType.IDENTIFIER) return evalVariableDefinition(tokens, modifiers);
        else if (second.getAsString() != null && EqualOperation.of(second.getAsString()) != null) return evalVariableChange(tokens, modifiers);
        return null;
    }

    private Node evalEnumMembers(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        if (unexpectedModifiers(modifiers)) return null;
        final EnumScope enumScope = expectEnumScope(tokens.get(0));
        if (enumScope == null) return null;

        final List<String> children = extractIdentifiers(tokens.subList(0, tokens.size() - 1));
        if (children == null) return null;
        if (enumScope.hasMembers()) {
            parser.parserError("Redefinition of enum members", tokens.get(0), "Delete redefinition");
            return null;
        }
        enumScope.addMembers(children);

        return new Node(NodeType.ENUM_VALUES,
                children.stream().map(s -> new Node(NodeType.IDENTIFIER, Token.of(s))).collect(Collectors.toList())
        );
    }

    private List<String> extractIdentifiers(@NotNull final List<Token> tokens) {
        final List<String> result = new ArrayList<>();
        String current = null;

        for (@NotNull final Token token : tokens) {
            if (NodeType.of(token) == NodeType.COMMA) {
                if (current == null) {
                    parser.parserError("Unexpected token ','", token);
                    return null;
                }
                if (NodeType.of(current) != NodeType.IDENTIFIER) {
                    parser.parserError("Expected identifier", token);
                    return null;
                }
                if (result.contains(current)) {
                    parser.parserError("Redefinition of identifier '" + current + "'", token);
                    return null;
                }
                result.add(current);
                current = null;
                continue;
            }
            if (current != null) {
                parser.parserError("Expected ','");
                return null;
            }
            warnUnconventional(token, IdentifierType.ENUM_MEMBER, true);
            current = token.token();
        }
        if (current != null) result.add(current);
        return result;
    }

    public Node evalVariableChange(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        if (unexpectedModifiers(modifiers)) return null;
        final Token identifier = parser.getAndExpect(tokens, 0, NodeType.IDENTIFIER);
        final Token equal = parser.getAndExpect(tokens, 1, NodeType.SET, NodeType.SET_ADD, NodeType.SET_AND, NodeType.SET_DIV, NodeType.SET_LSHIFT,
                NodeType.SET_MOD, NodeType.SET_MULT, NodeType.SET_OR, NodeType.SET_RSHIFT, NodeType.SET_SUB, NodeType.SET_XOR,
                NodeType.INCREMENT_LITERAL, NodeType.DECREMENT_LITERAL);

        if (Parser.anyNull(identifier, equal)) return null;

        final ValueParser.TypedNode value = NodeType.of(equal) == NodeType.INCREMENT_LITERAL || NodeType.of(equal) == NodeType.DECREMENT_LITERAL
                ? new ValueParser.TypedNode(Datatype.INT,
                        new Node(NodeType.INTEGER_NUM_LITERAL, Token.of("1"))
                )
                : parseExpression(tokens.subList(2, tokens.size() - 1));


        final Optional<Variable> foundVariable = findVariable(identifier, true);
        if (foundVariable.isEmpty()) return null;

        return evalVariableChange(identifier, value, equal);
    }

    public Node evalVariableChange(@NotNull final Token identifier, @NotNull final ValueParser.TypedNode value, @NotNull final Token equal) {
        final FunctionScope functionScope = expectFunctionScope(identifier);
        if (functionScope == null) return null;

        final EqualOperation eq = EqualOperation.of(equal.token());
        if (eq == null) {
            parser.parserError("Unexpected parsing error, invalid equals operation '" + equal.token() + "'", equal);
            return null;
        }

        final boolean success = functionScope.localVariableValue(parser, identifier, value, eq);
        if (!success && parser.encounteredError) return null;

        final NodeType eqType = NodeType.of(equal);
        final Token finalEq = switch (eqType) {
            case INCREMENT_LITERAL -> Token.of("+=");
            case DECREMENT_LITERAL -> Token.of("-=");
            default -> equal;
        };

        return new Node(NodeType.VAR_SET_VALUE,
                new Node(NodeType.IDENTIFIER, identifier),
                new Node(NodeType.OPERATOR, finalEq),
                new Node(NodeType.VALUE, value.node())
        );
    }

    protected Optional<Variable> findVariable(@NotNull final Token identifierTok, final boolean panic) {
        final FunctionScope functionScope = expectFunctionScope(identifierTok);
        if (functionScope == null) return Optional.empty();


        return Optional.ofNullable(functionScope.localVariable(parser, identifierTok).orElseGet(() -> {
            if (parser.encounteredError) return null; // localVariable() returns null if the needed variable is global but does not actually print an error into the logs

            return findGlobalVariable(identifierTok, functionScope.using(), panic);
        }));
    }

    protected Variable findGlobalVariable(@NotNull final Token identifierTok, final List<String> usingMods, final boolean panic) {
        final String identifier = identifierTok.token();
        final Optional<Module> oglobalMod = parser.findModuleFromIdentifier(identifier, identifierTok, panic);
        if (oglobalMod.isEmpty()) {
            if (panic) parser.parserError("Unexpected parsing error, module of global variable is null without any previous parsing error", identifierTok);
            return null;
        }
        final Module globalMod = oglobalMod.get();
        Optional<Variable> globalVar = globalMod.findVariableByName(ParserEvaluator.identOf(identifier));

        if (globalVar.isEmpty())
            for (final String using : usingMods) {
                final Variable findUsing = findGlobalVariable(new Token(using + "." + identifier, identifierTok.actualLine(), identifierTok.line(), identifierTok.column()),
                        Collections.emptyList(), false);

                if (findUsing != null) globalVar = Optional.of(findUsing);
            }

        if (globalVar.isEmpty()) {
            if (panic) parser.parserError("Unexpected parsing error, global variable is null without any previous parsing error", identifierTok);
            return null;
        }
        parser.checkAccessValidity(globalMod, IdentifierType.VARIABLE, identifierTok, globalVar.get().modifiers());
        return globalVar.get();
    }

    public static String moduleOf(@NotNull final String identifier) {
        return identifier.contains(".") ? StringUtils.substringBeforeLast(identifier, ".") : "";
    }

    public static String identOf(@NotNull final String identifier) {
        return identifier.contains(".") ? StringUtils.substringAfterLast(identifier, ".") : identifier;
    }

    public Node evalFunctionCall(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        if (unexpectedModifiers(modifiers)) return null;

        final Token identifierTok = parser.getAndExpect(tokens, 0, NodeType.IDENTIFIER);
        if (Parser.anyNull(identifierTok)) return null;

        final List<ValueParser.TypedNode> params = parseParametersCallFunction(tokens.subList(2, tokens.size() - 2));

        if (parser.skimming) {
            if (checkValidFunctionCall(identifierTok, params, true, true) == null) return null;
        }
        return new Node(NodeType.FUNCTION_CALL,
                new Node(NodeType.IDENTIFIER, identifierTok),
                new Node(NodeType.PARAMETERS, params.stream().map(n -> new Node(NodeType.VALUE, n.node())).toList())
        );
    }

    protected FunctionDefinition checkValidFunctionCall(@NotNull final Token identifierTok, @NotNull final List<ValueParser.TypedNode> params, final boolean checkUsing, final boolean panic) {
        final String identifier = identifierTok.token();
        final String moduleAsString = moduleOf(identifier);
        final Optional<Module> ofunctionModule = parser.findModuleFromIdentifier(identifier, identifierTok, panic);
        if (ofunctionModule.isEmpty()) return null;

        final FunctionScope functionScope = expectFunctionScope(identifierTok);
        if (functionScope == null) return null;

        final Module functionModule = ofunctionModule.get();
        final String function = identOf(identifier);
        final Optional<FunctionConcept> funcConcept = functionModule.findFunctionConceptByName(function);

        if (funcConcept.isEmpty()) {
            if (checkUsing) {
                for (final String using : functionScope.using()) {
                    final FunctionDefinition findUsing = checkValidFunctionCall(
                            new Token(using + "." + identifier, identifierTok.actualLine(), identifierTok.line(), identifierTok.column()),
                            params, false, false
                    );
                    if (findUsing != null) return findUsing;
                }
            }
            if (panic) parser.parserError("Cannot find any function called '" + function + "' in module '" +
                    (moduleAsString.isEmpty() ? parser.lastModule().name() : moduleAsString) + "'", identifierTok.line(), identifierTok.column() + moduleAsString.length());
            return null;
        }
        final Optional<FunctionDefinition> def = funcConcept.get().definitionByCallParameters(params);
        if (parser.encounteredError) return null;

        if (def.isEmpty()) {
            if (params.isEmpty()) {
                if (panic) parser.parserError("Cannot find any implementation for function '" + function + "' with no arguments", identifierTok, true);
                return null;
            }
            if (panic) parser.parserError("Cannot find any implementation for function '" + function + "' with argument types " + callArgsToString(params), identifierTok, true);
            return null;
        }
        if (panic) parser.checkAccessValidity(functionModule, IdentifierType.FUNCTION, identifierTok, def.get().modifiers());
        if (parser.encounteredError) return null;
        return def.get();
    }

    private String callArgsToString(@NotNull final List<ValueParser.TypedNode> params) {
        return String.join(", ", params.stream().map(n -> n.type().getName().toLowerCase()).toList());
    }

    public List<ValueParser.TypedNode> parseParametersCallFunction(@NotNull final List<Token> tokens) {
        return ValueParser.parseParametersCallFunction(tokens, parser);
    }

    public Node evalVariableDefinition(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        final Token identifier = parser.getAndExpect(tokens, 1, NodeType.IDENTIFIER);
        final Token equalsOrSemi = parser.getAndExpect(tokens, 2, NodeType.SET, NodeType.SEMI);
        if (Parser.anyNull(identifier, equalsOrSemi)) return null;

        final Token datatype = tokens.get(0);
        final boolean indefinite = NodeType.of(datatype) == NodeType.QUESTION_MARK;

        if (NodeType.of(equalsOrSemi) == NodeType.SEMI) {
            if (indefinite) {
                parser.parserError("Unexpected token '?', expected a definite datatype", datatype,
                        "The '?' cannot be used as a datatype when there is no value directly specified, so change the datatype to a definite.");
                return null;
            }

            return new Node(NodeType.VAR_DEFINITION,
                    new Node(NodeType.MODIFIERS, modifiers),
                    new Node(NodeType.IDENTIFIER, identifier),
                    new Node(NodeType.TYPE, datatype)
            );
        }
        final ValueParser.TypedNode value = parseExpression(tokens.subList(3, tokens.size() - 1));
        if (value.type() == null || value.node() == null) return null;

        final Node finalType = indefinite
                ? new Node(NodeType.TYPE, Token.of(value.type().getName()))
                : new Node(NodeType.TYPE, datatype);

        if (!indefinite && !value.type().equals(Objects.requireNonNull(Datatype.of(parser, finalType.value())))) {
            parser.parserError("Datatypes are not equal on both sides, trying to assign " + value.type().getName() + " to a " + datatype.token() + " variable.", datatype,
                    "Change the datatype to the correct one, try casting values inside the expression to the needed datatype or set the variable type to '?'.");
            return null;
        }

        return new Node(NodeType.VAR_DEF_AND_SET_VALUE,
                new Node(NodeType.MODIFIERS, modifiers),
                new Node(NodeType.IDENTIFIER, identifier),
                finalType,
                new Node(NodeType.VALUE, value.node())
        );
    }

    public ValueParser.TypedNode parseExpression(@NotNull final List<Token> tokens) {
        return new ValueParser(tokens, parser).parse();
    }

    public List<Node> parseParametersDefineFunction(@NotNull final List<Token> tokens) {
        final List<Node> result = new ArrayList<>();
        if (tokens.isEmpty()) return result;
        Node currentNode = new Node(NodeType.VAR_DEFINITION);
        final List<NodeType> currentNodeModifiers = new ArrayList<>();
        boolean addedNode = false;
        boolean parsedDatatype = false;
        boolean parsedIdentifier = false;

        for (@NotNull final Token token : tokens) {
            final Node asNode = new Node(NodeType.of(token), token);
            final NodeType type = asNode.type();
            final Datatype asDatatype = parsedDatatype ? null : Datatype.of(parser, Collections.emptyList(), token);

            if (type == NodeType.COMMA) {
                currentNode.addChildren(new Node(NodeType.MODIFIERS, currentNodeModifiers.stream().map(Node::of).toList()));
                result.add(currentNode);
                currentNode = new Node(NodeType.VAR_DEFINITION);
                currentNodeModifiers.clear();
                parsedDatatype = false;
                parsedIdentifier = false;
                addedNode = true;
                continue;
            }
            addedNode = false;
            if (type.isModifier()) {
                if (parsedIdentifier || parsedDatatype) {
                    parser.parserError("Unexpected token '" + token.token() + "' while parsing function parameters, expected modifiers before datatype before identifier");
                    return new ArrayList<>();
                }
                if (type.isVisibilityModifier()) {
                    parser.parserError("Unexpected token '" + token.token() + "', cannot use visibility modifiers (pub, priv, own) for function parameters");
                }
                if (parser.findConflictingModifiers(currentNodeModifiers, type, token)) return new ArrayList<>();
                currentNodeModifiers.add(type);
                continue;
            }
            if (asDatatype != null && asDatatype.valid()) {
                parsedDatatype = true;
                currentNode.addChildren(asNode);
                continue;
            }
            if (asDatatype == null || !asDatatype.valid()) {
                if (!parsedDatatype) {
                    parser.parserError("Unexpected token '" + token.token() + "' while parsing function parameters, expected datatype before identifier");
                    return new ArrayList<>();
                }
                parsedIdentifier = true;
                currentNode.addChildren(asNode);
                continue;
            }
            parser.parserError("Could not parse function argument, unexpected token '" + token.token() + "'");
            return new ArrayList<>();
        }
        if (!addedNode) {
            currentNode.addChildren(new Node(NodeType.MODIFIERS, currentNodeModifiers.stream().map(Node::of).toList()));
            result.add(currentNode);
        }
        return result;
    }

    public Node evalFunctionDefinition(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {

        final Token fnToken = parser.getAndExpect(tokens, 0, NodeType.LITERAL_FN);
        final Token identifier = parser.getAndExpect(tokens, 1, NodeType.IDENTIFIER);
        final Token retDef = parser.getAndExpect(tokens, 2, NodeType.TILDE, NodeType.DOUBLE_COLON, NodeType.LBRACE);
        final Token last = parser.getAndExpect(tokens, tokens.size() - 1, NodeType.SEMI, NodeType.LBRACE);
        if (Parser.anyNull(fnToken, identifier, retDef, last)) return null;

        final Optional<Node> firstMutabilityModif = modifiers.stream().filter(m -> m.type().isMutabilityModifier()).findFirst();
        if (firstMutabilityModif.isPresent()) {
            parser.parserError("Cannot declare functions as own, const or mut, they are automatically constant because they cannot be redefined",
                    firstMutabilityModif.get().value());
            return null;
        }
        final NodeType lastType = NodeType.of(last);
        final Optional<Node> firstNat = modifiers.stream().filter(m -> m.type() == NodeType.LITERAL_NAT).findFirst();
        final boolean nativeFunc = firstNat.isPresent();
        if (nativeFunc) {
            if (lastType == NodeType.LBRACE) {
                parser.parserError("Expected ';' after native function definition", last);
                return null;
            }
        } else if (lastType == NodeType.SEMI) {
            parser.parserError("Expected '{' after intern function definition", last);
            return null;
        }

        final NodeType ret = NodeType.of(retDef);
        if (parser.skimming) {
            final Optional<Scope> current = parser.scope();
            if (current.isEmpty()) {
                parser.parserError("Unexpected parsing error", "Could not create function at root level");
                return null;
            }
            final ScopeType currentType = current.get().type();
            if (!parser.stdlib ? currentType != ScopeType.MODULE : currentType != ScopeType.PARENT && currentType != ScopeType.MODULE) {
                if (parser.stdlib) {
                    parser.parserError("Expected function definition to be inside of a module or at root level",
                            "Cannot define functions inside of other functions");
                    return null;
                }
                parser.parserError("Expected function definition to be inside of a module",
                        "Cannot define functions at root level, create a module for your function or move it to an existing module",
                        "Cannot define functions inside of other functions either");
                return null;
            }
        }

        if (ret == NodeType.LBRACE) {
            if (nativeFunc) {
                parser.parserError("Expected ';' after native function definition", last);
                return null;
            }
            parser.scope(ScopeType.FUNCTION);
            parser.currentFuncReturnType = Datatype.VOID;
            return new Node(NodeType.FUNCTION_DEFINITION,
                    new Node(NodeType.IDENTIFIER, identifier),
                    new Node(NodeType.TYPE, Token.of("void")),
                    new Node(NodeType.MODIFIERS, modifiers),
                    new Node(NodeType.PARAMETERS, Collections.emptyList())
            );
        }
        final int extraIndex = ret == NodeType.TILDE ? 0 : 1;
        final Datatype returnType;
        if (extraIndex == 0) returnType = Datatype.VOID;
        else {
            final Token returnToken = parser.getAndExpect(tokens, 3,
                    Arrays.stream(NodeType.values())
                            .filter(NodeType::isDatatype)
                            .toList()
                            .toArray(new NodeType[0]));
            if (returnToken == null) return null;

            returnType = Datatype.of(parser, returnToken);
        }
        if (returnType == null) return null;

        if (nativeFunc) return evalNativeFunction(tokens, modifiers, extraIndex, last, identifier, returnType);

        final Token parenOpen = parser.getAndExpect(tokens, 3 + extraIndex, NodeType.LPAREN);
        final Token parenClose = parser.getAndExpect(tokens, tokens.size() - 2, true, NodeType.RPAREN);
        if (parenOpen == null || parenClose == null) return null;

        final List<Node> params = parseParametersDefineFunction(tokens.subList(4 + extraIndex, tokens.size() - 2));

        parser.scope(ScopeType.FUNCTION);
        parser.currentFuncReturnType = returnType;

        return new Node(NodeType.FUNCTION_DEFINITION,
                new Node(NodeType.IDENTIFIER, identifier),
                new Node(NodeType.TYPE, Token.of(returnType.getName())),
                new Node(NodeType.MODIFIERS, modifiers),
                new Node(NodeType.PARAMETERS, params)
        );
    }

    public Node evalNativeFunction(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers, final int extraIndex,
                                   @NotNull final Token last, @NotNull final Token identifier, @NotNull final Datatype returnType) {
        final Token beforeLastArrow = parser.getAny(tokens, tokens.size() - 3);
        if (beforeLastArrow == null || NodeType.of(beforeLastArrow) != NodeType.BECOMES) {
            parser.parserError("Expected '-> <constant-string-literal>' after ')' in native function definition", last,
                    "The scheme for native functions is: <modifiers> <identifier> <return-definition> ( <args> ) -> <constant-string-literal>");
            return null;
        }
        final Token stringLiteral = parser.getAndExpect(tokens, tokens.size() - 2, NodeType.STRING_LITERAL);
        if (stringLiteral == null) return null;

        final Token parenOpen = parser.getAndExpect(tokens, 3 + extraIndex, NodeType.LPAREN);
        final Token parenClose = parser.getAndExpect(tokens, tokens.size() - 4, true, NodeType.RPAREN);
        if (parenOpen == null || parenClose == null) return null;

        final List<Node> params = parseParametersDefineFunction(tokens.subList(4 + extraIndex, tokens.size() - 4));

        final Node result = new Node(NodeType.NATIVE_FUNCTION_DEFINITION,
                new Node(NodeType.IDENTIFIER, identifier),
                new Node(NodeType.TYPE, Token.of(returnType.getName())),
                new Node(NodeType.MODIFIERS, modifiers),
                new Node(NodeType.PARAMETERS, params),
                new Node(NodeType.NATIVE_JAVA_FUNCTION_STR, stringLiteral)
        );

        final List<FunctionParameter> parameters = params.stream().map(n -> {
            final Datatype datatype = Datatype.of(parser, n.child(0).value());
            if (datatype == null) throw new NullPointerException();
            return new FunctionParameter(
                    datatype,
                    n.child(1).value().token(),
                    n.child(2).children().stream().map(n2 -> Modifier.of(n2.type())).toList()
            );
        }).toList();

        final Method nativeMethod = checkNativeFunctionValidity(last, stringLiteral.token().substring(1, stringLiteral.token().length() - 1), identifier.token(), parameters, returnType);
        if (nativeMethod == null) return null;
        addNativeFunctionFromResult(result, nativeMethod);
        return result;
    }

    private static Class<?> primitiveToJavaType(@NotNull final PrimitiveDatatype primitiveDatatype) {
        return switch (primitiveDatatype) {
            case INT -> int.class;
            case LONG -> long.class;
            case DOUBLE -> double.class;
            case FLOAT -> float.class;
            case BOOL -> boolean.class;
            case STRING -> String.class;
            case CHAR -> char.class;
            case VOID -> void.class;
        };
    }

    private Method checkNativeFunctionValidity(@NotNull final Token at,
                                                @NotNull final String className, @NotNull final String functionName, @NotNull final List<FunctionParameter> params, @NotNull final Datatype returnType) {
        try {
            final Class<?> jcallClass = Class.forName(className);
            final ArrayList<Class<?>> paramTypes = new ArrayList<>();

            for (final FunctionParameter arg : params) {
                final Datatype type = arg.type();
                if (type.notPrimitive()) {
                    parser.parserError("Only primitive datatypes (int, long, double, float, bool, string, char) may be used as native function arguments", at);
                    return null;
                }
                paramTypes.add(primitiveToJavaType(type.getPrimitive()));
            }
            if (returnType.notPrimitive()) {
                parser.parserError("Only primitive datatypes (int, long, double, float, bool, string, char) may be used as a native function return type", at);
                return null;
            }
            final Method invokeMethod = jcallClass.getMethod(functionName, paramTypes.toArray(new Class<?>[0]));
            final Class<?> methodType = invokeMethod.getReturnType();
            if (methodType != primitiveToJavaType(returnType.getPrimitive())) {
                parser.parserWarning("Return type of native function does not match return type of native java method", at);
            }
            final MuCallable annotationTest = invokeMethod.getAnnotation(MuCallable.class);
            if (annotationTest == null) {
                parser.parserError("May only use java methods annotated with org.crayne.mu.lang.MuCallable as native functions", at, "Annotate the java method with org.crayne.mu.lang.MuCallable");
                return null;
            }
            return invokeMethod;
        } catch (final Exception e) {
            parser.parserError("Unknown error when evaluating native java function: " + e.getClass().getName(), at);
            return null;
        }
    }

    public Node evalIfStatement(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        final FunctionScope functionScope = expectFunctionScope(tokens.get(0));
        if (functionScope == null) return null;
        parser.scope(ScopeType.IF);
        return evalConditional(tokens, modifiers, NodeType.LITERAL_IF, false);
    }

    public Node evalWhileStatement(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers, final boolean unscoped) {
        final FunctionScope functionScope = expectFunctionScope(tokens.get(0));
        if (functionScope == null) return null;
        if (!unscoped) parser.scope(ScopeType.WHILE);
        return evalConditional(tokens, modifiers, NodeType.LITERAL_WHILE, unscoped);
    }

    public Node evalDoStatement(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        final FunctionScope functionScope = expectFunctionScope(tokens.get(0));
        if (functionScope == null) return null;
        if (unexpectedModifiers(modifiers)) return null;
        final Token doToken = parser.getAndExpect(tokens, 0, NodeType.LITERAL_DO);
        final Token scopeToken = parser.getAndExpect(tokens, 1, NodeType.LBRACE);
        if (Parser.anyNull(doToken, scopeToken)) return null;

        parser.scope(ScopeType.DO);
        return new Node(NodeType.DO_STATEMENT);
    }

    public Node evalForStatement(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        final FunctionScope functionScope = expectFunctionScope(tokens.get(0));
        if (functionScope == null) return null;
        if (unexpectedModifiers(modifiers)) return null;
        final List<List<Token>> exprs = splitByComma(tokens.subList(1, tokens.size() - 1));
        if (exprs.isEmpty()) {
            parser.parserError("Expected expression after 'for' statement");
            return null;
        }
        return switch (exprs.size()) {
            case 2 -> evalTransitionalForStatement(exprs);
            case 3 -> evalTraditionalForStatement(exprs);
            default -> {
                final int atIndex = exprs.subList(0, exprs.size() - 1).stream().map(l -> l.size() + 1).reduce(0, Integer::sum) - 1;
                final Token at = tokens.get(atIndex);
                parser.parserError("Unexpected token '" + at.token() + "'", at, "Expected 2 or 3 expressions, so remove any trailing ones, or add a second expression if you only have 1.");
                yield null;
            }
        };
    }

    // for mut? i = 0, i < 10, i++
    public Node evalTraditionalForStatement(@NotNull final List<List<Token>> exprs) {
        final FunctionScope functionScope = expectFunctionScope(exprs.get(0).get(0));
        if (functionScope == null) return null;
        parser.scope(ScopeType.FAKE);
        parser.scopeIndent++;
        parser.actualIndent++;
        final Node createVariable = parser.evalUnscoped(exprs.get(0), NodeType.of(exprs.get(0).get(0)), Collections.emptyList());

        if (createVariable == null) return null;
        if (createVariable.type() != NodeType.VAR_DEF_AND_SET_VALUE) {
            parser.parserError("Expected variable definition", exprs.get(0).get(0));
            return null;
        }
        addLocalVarFromResult(createVariable);

        final ValueParser.TypedNode condition = parseExpression(exprs.get(1).subList(0, exprs.get(1).size() - 1));

        if (condition == null || condition.type() == null) return null;
        if (condition.type() != Datatype.BOOL) {
            parser.parserError("Expected boolean condition", "Cast condition to 'bool' or change the condition to be a bool on its own");
            return null;
        }

        final Node loopStatement = parser.evalUnscoped(exprs.get(2), NodeType.of(exprs.get(2).get(0)), Collections.emptyList());

        if (loopStatement == null) return null;
        if (loopStatement.type() != NodeType.VAR_SET_VALUE && loopStatement.type() != NodeType.FUNCTION_CALL) {
            parser.parserError("Expected variable set or function call as for loop instruct", exprs.get(0).get(0));
            return null;
        }
        parser.scope(ScopeType.FOR);
        return new Node(NodeType.FOR_FAKE_SCOPE,
                new Node(NodeType.FOR_STATEMENT,
                        createVariable,
                        new Node(NodeType.CONDITION, condition.node()),
                        new Node(NodeType.FOR_INSTRUCT, loopStatement)
                )
        );
    }

    // for mut? i = 0, i -> 10 // i goes in a transition to 10, exactly the same as above example but written differently
    public Node evalTransitionalForStatement(@NotNull final List<List<Token>> exprs) {
        final List<Token> transition = exprs.get(1);
        final Token identifier = parser.getAndExpect(transition, 0, NodeType.IDENTIFIER);
        final Token arrow = parser.getAndExpect(transition, 1, NodeType.BECOMES);
        if (Parser.anyNull(arrow, identifier)) return null;

        final List<Token> val = transition.subList(2, transition.size());

        final List<Token> condition = new ArrayList<>(Arrays.asList(identifier, Token.of("<"), Token.of("(")));
        condition.addAll(val);
        condition.addAll(Arrays.asList(Token.of(")"), Token.of(";"))); // semicolon needed as splitByComma() automatically puts those & evalTraditionalForStatement thinks there always is a semicolon

        return evalTraditionalForStatement(Arrays.asList(
                exprs.get(0),
                condition,
                Arrays.asList(identifier, Token.of("++"))
        ));
    }

    private List<List<Token>> splitByComma(@NotNull final List<Token> tokens) {
        final List<List<Token>> result = new ArrayList<>();
        List<Token> current = new ArrayList<>();

        for (@NotNull final Token token : tokens) {
            if (NodeType.of(token) == NodeType.COMMA) {
                current.add(Token.of(";"));
                result.add(current);
                current = new ArrayList<>();
                continue;
            }
            current.add(token);
        }
        if (!current.isEmpty()) result.add(current);
        return result;
    }

    private Node evalConditional(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers, @NotNull final NodeType condType, final boolean unscoped) {
        if (unexpectedModifiers(modifiers)) return null;
        final Token first = parser.getAndExpect(tokens, 0, condType);
        if (Parser.anyNull(first)) return null;

        final ValueParser.TypedNode expr = parseExpression(tokens.subList(1, tokens.size() - 1));
        if (expr == null) return null;
        if (expr.type() != Datatype.BOOL) {
            parser.parserError("Expected boolean condition after '" + condType.getAsString() + "'", "Cast condition to 'bool' or change the expression to be a bool on its own");
            return null;
        }
        return new Node(NodeType.valueOf(condType.getAsString().toUpperCase() + "_STATEMENT" + (unscoped ? "_UNSCOPED" : "")),
                new Node(NodeType.CONDITION,
                        new Node(NodeType.VALUE, expr.node())
                )
        );
    }

    private boolean unexpectedModifiers(@NotNull final List<Node> modifiers) {
        if (!modifiers.isEmpty()) {
            final Token firstModif = modifiers.stream().map(Node::value).findFirst().orElse(null);
            parser.unexpected(firstModif);
            return true;
        }
        return false;
    }

    public Node evalUseStatement(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        if (unexpectedModifiers(modifiers)) return null;
        final FunctionScope functionScope = expectFunctionScope(tokens.get(0));
        if (functionScope == null) return null;

        final Token use = parser.getAndExpect(tokens, 0, NodeType.LITERAL_USE);
        final Token identifier = parser.getAndExpect(tokens, 1, NodeType.IDENTIFIER);
        final Token semi = parser.getAndExpect(tokens, 2, NodeType.SEMI);
        if (Parser.anyNull(use, identifier, semi)) return null;

        final String moduleName = identifier.token();

        final Optional<Module> module = parser.findModuleFromIdentifier(moduleName, identifier, true);
        if (module.isEmpty()) return null;

        functionScope.using(moduleName);
        return new Node(NodeType.USE_STATEMENT, new Node(NodeType.IDENTIFIER, identifier));
    }

    public Node evalModuleDefinition(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        if (unexpectedModifiers(modifiers)) return null;

        final Token identifier = parser.getAndExpect(tokens, 1, NodeType.IDENTIFIER);
        final Token lbrace = parser.getAndExpect(tokens, 2, NodeType.LBRACE);
        if (Parser.anyNull(lbrace, identifier)) return null;

        if (expectModuleOrRoot(IdentifierType.MODULE)) return null;
        parser.scope(ScopeType.MODULE);
        if (handleRestrictedName(identifier, IdentifierType.MODULE, false)) return null;

        return new Node(NodeType.CREATE_MODULE,
                new Node(NodeType.IDENTIFIER, identifier)
        );
    }

    public Node evalEnumDefinition(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        final Token identifier = parser.getAndExpect(tokens, 1, NodeType.IDENTIFIER);
        final Token lbrace = parser.getAndExpect(tokens, 2, NodeType.LBRACE);
        if (Parser.anyNull(lbrace, identifier)) return null;

        if (expectModuleOrRoot(IdentifierType.ENUM)) return null;
        final Optional<Node> firstMutabilityModif = modifiers.stream().filter(m -> m.type().isMutabilityModifier()).findFirst();
        if (firstMutabilityModif.isPresent()) {
            parser.parserError("Cannot declare enums as own, const or mut, they are automatically constant because they cannot be redefined",
                    firstMutabilityModif.get().value());
            return null;
        }
        parser.scope(ScopeType.ENUM);
        final Optional<Scope> newScope = parser.scope();
        if (newScope.isEmpty()) {
            parser.parserError("Unexpected parsing error, null scope after adding an enum", identifier);
            return null;
        }
        final EnumScope enumScope = (EnumScope) newScope.get();
        enumScope.modifiers(modifiers.stream().map(n -> Modifier.of(n.type())).collect(Collectors.toList()));
        enumScope.name(identifier.token());
        enumScope.module(parser.lastModule());
        warnUnconventional(identifier, IdentifierType.ENUM, false);

        return new Node(NodeType.CREATE_ENUM,
                new Node(NodeType.IDENTIFIER, identifier)
        );
    }

    private boolean expectModuleOrRoot(@NotNull final IdentifierType type) {
        final Optional<Scope> current = parser.scope();
        if (parser.skimming) {
            if (current.isEmpty()) {
                parser.parserError("Unexpected parsing error, could not create " + type.name().toLowerCase() + " at root level");
                return true;
            }
            final ScopeType currentType = current.get().type();
            if (currentType != ScopeType.PARENT && currentType != ScopeType.MODULE) {
                parser.parserError("Expected " + type.name().toLowerCase() + " definition to be at root level or inside of another module");
                return true;
            }
        }
        return false;
    }

    public Node evalStdLibFinish(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        if (unexpectedModifiers(modifiers)) return null;

        final Token stdlibFinish = parser.getAndExpect(tokens, 0, NodeType.STANDARDLIB_MU_FINISH_CODE);
        final Token semi = parser.getAndExpect(tokens, 1, NodeType.SEMI);
        if (stdlibFinish == null || semi == null) return null;

        parser.stdlib = false;

        return new Node(NodeType.STANDARDLIB_MU_FINISH_CODE);
    }

    public FunctionScope expectFunctionScope(@NotNull final Token at) {
        final Optional<Scope> currentScope = parser.scope();
        if (currentScope.isEmpty() || !(currentScope.get() instanceof final FunctionScope functionScope)) {
            parser.parserError("Unexpected parsing error, expected statement to be inside of a function", at, "Enclose your statement inside of a function");
            return null;
        }
        return functionScope;
    }

    private EnumScope expectEnumScope(@NotNull final Token at) {
        final Optional<Scope> currentScope = parser.scope();
        if (currentScope.isEmpty() || !(currentScope.get() instanceof final EnumScope enumScope)) {
            parser.parserError("Unexpected parsing error, expected statement to be inside of an enum", at, "Enclose your statement inside of an enum");
            return null;
        }
        return enumScope;
    }

}
