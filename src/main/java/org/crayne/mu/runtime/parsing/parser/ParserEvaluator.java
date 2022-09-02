package org.crayne.mu.runtime.parsing.parser;

import org.apache.commons.lang3.StringUtils;
import org.crayne.mu.lang.Module;
import org.crayne.mu.lang.*;
import org.crayne.mu.runtime.parsing.ast.Node;
import org.crayne.mu.runtime.parsing.ast.NodeType;
import org.crayne.mu.runtime.parsing.lexer.Token;
import org.crayne.mu.runtime.parsing.parser.scope.FunctionScope;
import org.crayne.mu.runtime.parsing.parser.scope.Scope;
import org.crayne.mu.runtime.parsing.parser.scope.ScopeType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ParserEvaluator {

    final Parser parser;

    public ParserEvaluator(@NotNull final Parser parser) {
        this.parser = parser;
    }

    protected void addGlobalVarFromResult(@NotNull final Node result) {
        final Module module = parser.lastModule();
        final List<Modifier> modifiers = result.child(0).children().stream().map(n -> Modifier.of(n.type())).toList();

        final Scope current = parser.scope();
        if (!parser.stdlib && (current == null || current.type() != ScopeType.MODULE)) {
            parser.parserError("Cannot define global variables at root level",
                    "Move your variable into a module or create a new module for it");
            return;
        }

        if (result.children().size() == 4) {
            module.addGlobalVariable(new Variable(
                    result.child(1).value().token(),
                    NodeType.of(result.child(2).value()).getAsDataType(),
                    modifiers,
                    result.child(3)
            ));
            return;
        }
        module.addGlobalVariable(new Variable(
                result.child(1).value().token(),
                NodeType.of(result.child(2).value()).getAsDataType(),
                modifiers,
                null
        ));
    }

    public void addLocalVarFromResult(@NotNull final Node result) {
        final List<Modifier> modifiers = result.child(0).children().stream().map(n -> Modifier.of(n.type())).toList();
        final Scope current = parser.scope();
        if (!(current instanceof final FunctionScope functionScope)) {
            parser.parserError("Unexpected parsing error, expected statement to be inside of a function");
            return;
        }
        if (result.children().size() == 4) {
            functionScope.addLocalVariable(parser, new Variable(
                    result.child(1).value().token(),
                    NodeType.of(result.child(2).value()).getAsDataType(),
                    modifiers,
                    result.child(3)
            ));
            return;
        }
        functionScope.addLocalVariable(parser, new Variable(
                result.child(1).value().token(),
                NodeType.of(result.child(2).value()).getAsDataType(),
                modifiers,
                null
        ));
    }

    protected void addFunctionFromResult(@NotNull final Node result) {
        final Module module = parser.lastModule();
        try {
            final List<Modifier> modifiers = result.child(2).children().stream().map(n -> Modifier.of(n.type())).toList();

            final List<Node> paramNodes = result.child(3).children().stream().toList();
            final List<FunctionParameter> params = paramNodes.stream().map(n -> new FunctionParameter(
                    Datatype.valueOf(n.child(0).value().token().toUpperCase()),
                    n.child(1).value().token(),
                    n.child(2).children().stream().map(n2 -> Modifier.of(n2.type())).toList()
            )).toList();

            if (result.children().size() == 5) {
                module.addFunction(new FunctionDefinition(
                        result.child(0).value().token(),
                        Datatype.valueOf(result.child(1).value().token().toUpperCase()),
                        params,
                        modifiers,
                        result.child(4)
                ));
                return;
            }
            module.addFunction(new FunctionDefinition(
                    result.child(0).value().token(),
                    Datatype.valueOf(result.child(1).value().token().toUpperCase()),
                    params,
                    modifiers
            ));
        } catch (final NullPointerException e) {
            e.printStackTrace();
            parser.output.errorMsg("Could not parse function definition");
        }
    }

    public Node evalFirstIdentifier(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        if (tokens.size() <= 1) return null;
        final Token secondToken = Parser.tryAndGet(tokens, 1);
        if (secondToken == null) return null;

        final NodeType second = NodeType.of(secondToken);
        if (second == NodeType.LPAREN) return evalFunctionCall(tokens, modifiers);
        else if (EqualOperation.of(second.getAsString()) != null) return evalVariableChange(tokens, modifiers);
        return null;
    }

    public Node evalVariableChange(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        if (unexpectedModifiers(modifiers)) return null;
        final Token identifier = Parser.tryAndGet(tokens, 0);
        if (parser.expect(identifier, identifier, NodeType.IDENTIFIER) || identifier == null) return null;
        final Token equal = Parser.tryAndGet(tokens, 1);
        if (parser.expect(equal, equal, NodeType.SET, NodeType.SET_ADD, NodeType.SET_AND, NodeType.SET_DIV, NodeType.SET_LSHIFT,
                NodeType.SET_MOD, NodeType.SET_MULT, NodeType.SET_OR, NodeType.SET_RSHIFT, NodeType.SET_SUB, NodeType.SET_XOR) || equal == null) return null;

        final ValueParser.TypedNode value = parseExpression(tokens.subList(2, tokens.size() - 1));

        if (parser.skimming) {
            final Scope currentScope = parser.scope();
            if (!(currentScope instanceof final FunctionScope functionScope)) {
                parser.parserError("Unexpected parsing error, expected statement to be inside of a function", "Enclose your statement inside of a function");
                return null;
            }
            final EqualOperation eq = EqualOperation.of(equal.token());
            if (eq == null) {
                parser.parserError("Unexpected parsing error, invalid equals operation '" + equal.token() + "'");
                return null;
            }
            final Variable foundVariable = findVariable(identifier);
            if (foundVariable == null) return null;

            final boolean success = functionScope.localVariableValue(parser, identifier, value, eq);
            if (!success && parser.encounteredError) return null;
        }
        return new Node(NodeType.VAR_SET_VALUE,
                new Node(NodeType.IDENTIFIER, identifier),
                new Node(NodeType.OPERATOR, equal),
                new Node(NodeType.VALUE, value.node())
        );
    }

    protected Variable findVariable(@NotNull final Token identifierTok) {
        final String identifier = identifierTok.token();
        final Scope currentScope = parser.scope();
        if (!(currentScope instanceof final FunctionScope functionScope)) {
            parser.parserError("Unexpected parsing error, expected statement to be inside of a function", "Enclose your statement inside of a function");
            return null;
        }
        final Variable var = functionScope.localVariable(parser, identifierTok);
        if (var == null) {
            if (parser.encounteredError) return null; // localVariable() returns null if the needed variable is global but does not actually print an error into the logs

            final Module globalMod = parser.findModuleFromIdentifier(identifier, identifierTok, false);
            if (globalMod == null) {
                parser.parserError("Unexpected parsing error, module of global variable is null without any previous parsing error");
                return null;
            }
            final Variable globalVar = globalMod.findVariableByName(ParserEvaluator.identOf(identifier));
            if (globalVar == null) {
                parser.parserError("Unexpected parsing error, global variable is null without any previous parsing error");
                return null;
            }
            parser.checkAccessValidity(globalMod, IdentifierType.VARIABLE, identifier, globalVar.modifiers());
            return globalVar;
        }
        return var;
    }

    public static String moduleOf(@NotNull final String identifier) {
        return StringUtils.substringBeforeLast(identifier, ".");
    }

    public static String identOf(@NotNull final String identifier) {
        return identifier.contains(".") ? StringUtils.substringAfterLast(identifier, ".") : identifier;
    }

    public Node evalFunctionCall(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        if (unexpectedModifiers(modifiers)) return null;

        final Token identifierTok = Parser.tryAndGet(tokens, 0);
        if (parser.expect(identifierTok, identifierTok, NodeType.IDENTIFIER)) return null;
        if (identifierTok == null) return null;
        final List<ValueParser.TypedNode> params = parseParametersCallFunction(tokens.subList(2, tokens.size() - 2));

        if (parser.skimming) {
            final String identifier = identifierTok.token();
            final String moduleAsString = moduleOf(identifier);
            final Module functionModule = parser.findModuleFromIdentifier(identifier, identifierTok, true);
            if (functionModule == null) return null;
            final String function = identOf(identifier);
            final FunctionConcept funcConcept = functionModule.findFunctionConceptByName(function);

            if (funcConcept == null) {
                parser.parserError("Cannot find any function called '" + function + "' in module '" + (moduleAsString.isEmpty() ? parser.lastModule().name() : moduleAsString) + "'", identifierTok.line(), identifierTok.column() + moduleAsString.length() + 1);
                return null;
            }
            final FunctionDefinition def = funcConcept.definitionByCallParameters(params);

            if (def == null) {
                if (params.isEmpty()) {
                    parser.parserError("Cannot find any implementation for function '" + function + "' with no arguments", identifierTok, true);
                    return null;
                }
                parser.parserError("Cannot find any implementation for function '" + function + "' with argument types " + callArgsToString(params), identifierTok, true);
                return null;
            }
            parser.checkAccessValidity(functionModule, IdentifierType.FUNCTION, identifier, def.modifiers());
        }
        return new Node(NodeType.FUNCTION_CALL,
                new Node(NodeType.IDENTIFIER, identifierTok),
                new Node(NodeType.PARAMETERS, params.stream().map(n -> new Node(NodeType.VALUE, n.node())).toList())
        );
    }

    private String callArgsToString(@NotNull final List<ValueParser.TypedNode> params) {
        return String.join(", ", params.stream().map(n -> n.type().getName().toLowerCase()).toList());
    }

    public List<ValueParser.TypedNode> parseParametersCallFunction(@NotNull final List<Token> tokens) {
        return ValueParser.parseParametersCallFunction(tokens, parser);
    }

    public Node evalVariableDefinition(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        final Token identifier = Parser.tryAndGet(tokens, 1);
        if (parser.expect(identifier, identifier, NodeType.IDENTIFIER)) return null;
        final Token equalsOrSemi = Parser.tryAndGet(tokens, 2);
        if (parser.expect(equalsOrSemi, equalsOrSemi, NodeType.SET, NodeType.SEMI) || equalsOrSemi == null) return null;
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
        final Node finalType = indefinite ? new Node(NodeType.TYPE, Token.of(NodeType.of(value.type()).getAsString())) : new Node(NodeType.TYPE, datatype);

        if (!indefinite && !ValueParser.validVarset(value.type(), NodeType.of(datatype).getAsDataType())) {
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
            if (type.isDatatype()) {
                if (parsedIdentifier) {
                    parser.parserError("Unexpected token '" + token.token() + "' while parsing function parameters, expected datatype before identifier");
                    return new ArrayList<>();
                }
                parsedDatatype = true;
                currentNode.addChildren(asNode);
                continue;
            }
            if (type == NodeType.IDENTIFIER) {
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
        final Token fnToken = Parser.tryAndGet(tokens, 0);
        if (parser.expect(fnToken, fnToken, NodeType.LITERAL_FN)) return null;
        final Token identifier = Parser.tryAndGet(tokens, 1);
        if (parser.expect(identifier, identifier, NodeType.IDENTIFIER)) return null;
        final Token retDef = Parser.tryAndGet(tokens, 2);
        if (parser.expect(retDef, retDef, NodeType.TILDE, NodeType.DOUBLE_COLON, NodeType.LBRACE) || retDef == null) return null;

        final Optional<Node> firstMutabilityModif = modifiers.stream().filter(m -> m.type().isMutabilityModifier()).findFirst();
        if (firstMutabilityModif.isPresent()) {
            parser.parserError("Cannot declare functions as own, const or mut, they are automatically constant because they cannot be redefined",
                    firstMutabilityModif.get().value());
            return null;
        }
        final NodeType ret = NodeType.of(retDef);
        final Scope current = parser.scope();
        if (parser.skimming) {
            if (current == null) {
                parser.parserError("Unexpected parsing error", "Could not create function at root level");
                return null;
            }
            if (!parser.stdlib ? current.type() != ScopeType.MODULE : current.type() != ScopeType.PARENT && current.type() != ScopeType.MODULE) {
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
            parser.scope(ScopeType.FUNCTION);
        }

        if (ret == NodeType.LBRACE) {
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
            final Token returnToken = Parser.tryAndGet(tokens, 3);
            if (parser.expect(returnToken, returnToken,
                    Arrays.stream(NodeType.values())
                    .filter(NodeType::isDatatype)
                    .toList()
                    .toArray(new NodeType[0])
            ) || returnToken == null) return null;
            returnType = NodeType.of(returnToken).getAsDataType();
        }

        final Token parenOpen = Parser.tryAndGet(tokens, 3 + extraIndex);
        if (parser.expect(parenOpen, parenOpen, NodeType.LPAREN)) return null;
        final Token parenClose = Parser.tryAndGet(tokens, tokens.size() - 2);
        if (parser.expect(parenClose, parenClose, true, NodeType.RPAREN)) return null;
        final List<Node> params = parseParametersDefineFunction(tokens.subList(4 + extraIndex, tokens.size() - 2));

        return new Node(NodeType.FUNCTION_DEFINITION,
                new Node(NodeType.IDENTIFIER, identifier),
                new Node(NodeType.TYPE, Token.of(returnType.name().toLowerCase())),
                new Node(NodeType.MODIFIERS, modifiers),
                new Node(NodeType.PARAMETERS, params)
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

    public Node evalModuleDefinition(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        if (unexpectedModifiers(modifiers)) return null;

        final Token identifier = Parser.tryAndGet(tokens, 1);
        if (parser.expect(identifier, identifier, NodeType.IDENTIFIER)) return null;
        final Token lbrace = Parser.tryAndGet(tokens, 2);
        if (parser.expect(lbrace, lbrace, NodeType.LBRACE)) return null;

        final Scope current = parser.scope();
        if (parser.skimming) {
            if (current == null) {
                parser.parserError("Unexpected parsing error, could not create module at root level");
                return null;
            }
            if (current.type() != ScopeType.PARENT && current.type() != ScopeType.MODULE) {
                parser.parserError("Expected module definition to be at root level or inside of another module");
                return null;
            }
            parser.scope(ScopeType.MODULE);
        }

        return new Node(NodeType.CREATE_MODULE,
                new Node(NodeType.IDENTIFIER, identifier)
        );
    }

    public Node evalStdLibFinish(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        if (unexpectedModifiers(modifiers)) return null;

        final Token stdlibFinish = Parser.tryAndGet(tokens, 0);
        if (parser.expect(stdlibFinish, stdlibFinish, NodeType.STANDARDLIB_MU_FINISH_CODE)) return null;
        final Token semi = Parser.tryAndGet(tokens, 1);
        if (parser.expect(semi, semi, NodeType.SEMI)) return null;

        parser.stdlib = false;

        return new Node(NodeType.STANDARDLIB_MU_FINISH_CODE);
    }

}
