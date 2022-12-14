package org.crayne.mi.parsing.parser;

import org.apache.commons.lang3.StringUtils;
import org.crayne.mi.lang.MiDatatype;
import org.crayne.mi.lang.MiModifier;
import org.crayne.mi.lang.MiVariable;
import org.crayne.mi.parsing.ast.Node;
import org.crayne.mi.parsing.ast.NodeType;
import org.crayne.mi.parsing.lexer.Token;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class ASTGenerator {

    final Parser parser;

    public ASTGenerator(@NotNull final Parser parser) {
        this.parser = parser;
    }

    private static boolean restrictedName(@NotNull final String name) {
        return name.contains(".");
    }

    public Node evalWithIdentifier(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        if (tokens.isEmpty()) return null;
        final Token second = tokens.size() == 1 ? null : tokens.get(1);
        return second == null ? evalEnumMembers(tokens, modifiers) : switch (NodeType.of(second)) {
            case LPAREN -> evalFunctionCall(tokens, modifiers);
            case SET, SET_ADD, SET_AND, SET_OR, SET_DIV, SET_LSHIFT, SET_MOD, SET_MULT, SET_RSHIFT, SET_SUB, SET_XOR, INCREMENT_LITERAL, DECREMENT_LITERAL -> evalVariableChange(tokens, modifiers);
            case COMMA, SEMI -> evalEnumMembers(tokens, modifiers);
            case IDENTIFIER -> evalVariableDefinition(tokens, modifiers);
            default -> null;
        };
    }

    public Node evalReturnStatement(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        if (unexpectedModifiers(modifiers)) return null;

        final Token ret = parser.getAndExpect(tokens, 0, NodeType.LITERAL_RETURN, NodeType.DOUBLE_COLON);
        if (ret == null) return null;

        if (tokens.size() == 2) { // ret ; are two tokens
            return new Node(parser.currentNode(), NodeType.RETURN_STATEMENT, ret.actualLine(), ret);
        }
        final Node retVal = parseExpression(ret, tokens.subList(1, tokens.size() - 1));

        return new Node(parser.currentNode(), NodeType.RETURN_STATEMENT, ret.actualLine(), retVal);
    }

    public Node evalEnumMembers(@NotNull final List<Token> tokens) {
        final List<Node> modifiers = modifiers(tokens);
        return evalEnumMembers(tokens.subList(modifiers.size(), tokens.size()), modifiers);
    }

    public Node evalEnumMembers(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        if (unexpectedModifiers(modifiers)) return null;

        final List<Token> children = extractIdentifiers(tokens.get(tokens.size() - 1).token().equals(";") ? tokens.subList(0, tokens.size() - 1) : tokens);
        if (children == null) return null;

        return new Node(parser.currentNode(), NodeType.ENUM_VALUES, tokens.get(0).actualLine(),
                children.stream().map(s -> new Node(NodeType.IDENTIFIER, s, s.actualLine())).collect(Collectors.toList())
        );
    }

    private List<Token> extractIdentifiers(@NotNull final List<Token> tokens) {
        final List<Token> result = new ArrayList<>();
        Token current = null;

        for (@NotNull final Token token : tokens) {
            final NodeType tokenType = NodeType.of(token);
            final NodeType currentType = current == null ? null : NodeType.of(current);

            if (tokenType == NodeType.COMMA) {
                if (currentType == null) {
                    parser.parserError("Unexpected token ','.", token);
                    return null;
                }
                result.add(current);
                current = null;
                continue;
            }
            if (current != null) {
                parser.parserError("Expected ','.", token);
                return null;
            }
            if (tokenType != NodeType.IDENTIFIER) {
                parser.parserError("Expected ';'", token);
                return null;
            }
            current = token;
        }
        if (current != null) result.add(current);
        return result;
    }

    public Node evalVariableChange(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        if (unexpectedModifiers(modifiers)) return null;
        final Token identifier = parser.getAndExpect(tokens, 0, NodeType.IDENTIFIER);
        final Token equal = parser.getAndExpect(tokens, 1, NodeType.SET, NodeType.SET_ADD, NodeType.SET_AND, NodeType.SET_DIV, NodeType.SET_LSHIFT,
                NodeType.SET_MOD, NodeType.SET_MULT, NodeType.SET_OR, NodeType.SET_RSHIFT, NodeType.SET_SUB, NodeType.SET_XOR,
                NodeType.INCREMENT_LITERAL, NodeType.DECREMENT_LITERAL, NodeType.MULTIPLY);

        if (Parser.anyNull(identifier, equal)) return null;

        final Node value = NodeType.of(equal).incrementDecrement() ? null : parseExpression(equal, tokens.subList(2, tokens.size() - 1));
        return evalVariableChange(identifier, value, equal);
    }

    public Node evalVariableChange(@NotNull final Token identifier, final Node value, @NotNull final Token equal) {
        if (value == null) { // inc / decrement variable
            return new Node(parser.currentNode(), NodeType.MUTATE_VARIABLE, equal.actualLine(),
                    new Node(NodeType.IDENTIFIER, identifier, identifier.actualLine()),
                    new Node(NodeType.OPERATOR, equal, equal.actualLine())
            );
        }
        return new Node(parser.currentNode(), NodeType.MUTATE_VARIABLE, equal.actualLine(), variableChange(null, identifier, value, equal));
    }

    public static List<Node> variableChange(final MiVariable variable, @NotNull final Token identifier, final Node value, @NotNull final Token equal) {
        if (value == null) { // inc / decrement variable
            return List.of(
                    new Node(NodeType.IDENTIFIER, variable == null ? identifier : variable.identifier(), identifier.actualLine()),
                    new Node(NodeType.OPERATOR, equal, equal.actualLine()));
        }
        return List.of(
                new Node(NodeType.IDENTIFIER, variable == null ? identifier : variable.identifier(), identifier.actualLine()),
                new Node(NodeType.OPERATOR, equal, equal.actualLine()),
                value
        );
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

        final List<Node> params = parseParametersCallFunction(tokens.subList(2, tokens.size() - 2));

        return new Node(parser.currentNode(), NodeType.FUNCTION_CALL, identifierTok.actualLine(),
                new Node(NodeType.IDENTIFIER, identifierTok, identifierTok.actualLine()),
                new Node(NodeType.PARAMETERS, tokens.get(1).actualLine(), params.stream().map(n ->
                        new Node(NodeType.PARAMETER, n.lineDebugging(), n)
                ).toList())
        );
    }

    public List<Node> parseParametersCallFunction(@NotNull final List<Token> tokens) {
        final List<Node> result = new ArrayList<>();

        if (tokens.isEmpty()) return result;

        final List<Token> currentArg = new ArrayList<>();
        int paren = 0;
        boolean addedNode = false;
        for (@NotNull final Token token : tokens) {
            final NodeType type = NodeType.of(token.token());

            if (type == NodeType.LPAREN) paren++;
            if (type == NodeType.RPAREN) paren--;
            if (type == NodeType.COMMA && paren == 0) {
                result.add(parseExpression(tokens.get(0), currentArg));
                currentArg.clear();
                addedNode = true;
                continue;
            }
            addedNode = false;
            currentArg.add(token);
        }
        if (!addedNode) result.add(parseExpression(tokens.get(0), currentArg));
        return result;
    }

    protected static List<Node> modifiers(@NotNull final Collection<Token> tokens) {
        final List<Node> result = new ArrayList<>();
        for (@NotNull final Token token : tokens) {
            if (!NodeType.of(token).isModifier()) break;
            result.add(Node.of(token));
        }
        return result;
    }

    public Node evalUnscoped(@NotNull final Collection<Token> tokens) {
        final List<Node> modifiers = modifiers(tokens);
        final List<Token> withoutModifiers = tokens.stream().toList().subList(modifiers.size(), tokens.size());
        final NodeType first = withoutModifiers.isEmpty() ? null : NodeType.of(withoutModifiers.get(0));

        return first == null ? null : switch (first) {
            case STANDARDLIB_MI_FINISH_CODE -> evalStdLibFinish(withoutModifiers, modifiers);
            case LITERAL_BREAK -> evalBreak(withoutModifiers, modifiers);
            case LITERAL_CONTINUE -> evalContinue(withoutModifiers, modifiers);
            case LITERAL_ELSE -> evalElseStatement(withoutModifiers, modifiers, false);
            case LITERAL_RETURN -> evalReturnStatement(withoutModifiers, modifiers);
            case LITERAL_INT, LITERAL_DOUBLE, LITERAL_LONG, LITERAL_FLOAT,
                    LITERAL_CHAR, LITERAL_STRING, LITERAL_BOOL, QUESTION_MARK ->
                    evalVariableDefinition(withoutModifiers, modifiers);
            case LITERAL_FN -> evalFunctionDefinition(withoutModifiers, modifiers);
            case LITERAL_WHILE -> evalWhileStatement(withoutModifiers, modifiers, true);
            case LITERAL_USE -> evalUseStatement(withoutModifiers, modifiers);
            case IDENTIFIER -> evalWithIdentifier(withoutModifiers, modifiers);
            case SEMI -> evalNoop(withoutModifiers, modifiers);
            default -> null;
        };
    }

    public Node evalScoped(@NotNull final Collection<Token> tokens) {
        final List<Node> modifiers = modifiers(tokens);
        final List<Token> withoutModifiers = tokens.stream().toList().subList(modifiers.size(), tokens.size());
        final NodeType first = withoutModifiers.isEmpty() ? null : NodeType.of(withoutModifiers.get(0));

        return first == null ? null : switch (first) {
            case LITERAL_FN -> evalFunctionDefinition(withoutModifiers, modifiers);
            case LITERAL_IF -> evalIfStatement(withoutModifiers, modifiers);
            case LITERAL_ELSE -> evalElseStatement(withoutModifiers, modifiers, true);
            case LITERAL_WHILE -> evalWhileStatement(withoutModifiers, modifiers, false);
            case LITERAL_FOR -> evalForStatement(withoutModifiers, modifiers);
            case LITERAL_ENUM -> evalEnumDefinition(withoutModifiers, modifiers);
            case LITERAL_MODULE -> evalModuleDefinition(withoutModifiers, modifiers);
            case LITERAL_DO -> evalDoStatement(withoutModifiers, modifiers);
            case LBRACE -> evalLocalScope(withoutModifiers, modifiers);
            default -> null;
        };
    }

    public Node evalNoop(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        if (unexpectedModifiers(modifiers) || tokens.size() != 1) return null;
        return new Node(NodeType.NOOP, tokens.get(0).actualLine(), tokens.get(0), Collections.emptyList());
    }

    public Node evalLocalScope(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        if (unexpectedModifiers(modifiers) || tokens.size() != 1) return null;
        return new Node(NodeType.NOOP, tokens.get(0).actualLine(), tokens.get(0), Collections.emptyList());
    }

    public static boolean nullable(@NotNull final Collection<Node> modifiers) {
        return nullableOptModifiers(modifiers
                .stream()
                .map(n -> MiModifier.of(n.type()))
                .toList());
    }

    public static boolean nullableNodeTypes(@NotNull final Collection<NodeType> modifiers) {
        return nullableOptModifiers(modifiers
                .stream()
                .map(MiModifier::of)
                .toList());
    }

    public static boolean nullableOptModifiers(@NotNull final Collection<Optional<MiModifier>> modifiers) {
        return modifiers
                .stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList()
                .contains(MiModifier.NULLABLE);
    }

    public static boolean nullableModifiers(@NotNull final Collection<MiModifier> modifiers) {
        return modifiers
                .stream()
                .toList()
                .contains(MiModifier.NULLABLE);
    }

    public static List<Optional<MiModifier>> modifiersOfNodes(@NotNull final Collection<Node> modifiers) {
        return modifiers
                .stream()
                .map(n -> MiModifier.of(n.type()))
                .toList();
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
                        "The '?' cannot be used as a datatype when there is no value specified upon declaration, so change the datatype to a definite.");
                return null;
            }

            return new Node(parser.currentNode(), NodeType.DECLARE_VARIABLE, identifier.actualLine(), datatype,
                    new Node(NodeType.MODIFIERS, modifiers.isEmpty() ? -1 : modifiers.get(0).lineDebugging(), modifiers),
                    new Node(NodeType.IDENTIFIER, identifier, identifier.actualLine()),
                    new Node(NodeType.TYPE, datatype, datatype.actualLine())
            );
        }
        final Node value = parseExpression(equalsOrSemi, tokens.subList(3, tokens.size() - 1));

        final Node finalType = new Node(NodeType.TYPE, datatype, datatype.actualLine());

        return new Node(parser.currentNode(), NodeType.DEFINE_VARIABLE, identifier.actualLine(), datatype,
                new Node(NodeType.MODIFIERS, modifiers.isEmpty() ? -1 : modifiers.get(0).lineDebugging(), modifiers),
                new Node(NodeType.IDENTIFIER, identifier, identifier.actualLine()),
                finalType,
                value
        );
    }

    protected static Node parseExpression(@NotNull final Token at, @NotNull final List<Token> tokens) {
        return new Node(NodeType.VALUE, at.actualLine(), at, tokens.stream().map(Node::of).toList());
    }

    public Node evalBreak(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        if (unexpectedModifiers(modifiers)) return null;
        final Token breakToken = parser.getAndExpect(tokens, 0, NodeType.LITERAL_BREAK);
        final Token semi = parser.getAndExpect(tokens, 1, NodeType.SEMI);
        if (Parser.anyNull(breakToken, semi)) return null;

        return new Node(parser.currentNode(), NodeType.BREAK_STATEMENT, breakToken.actualLine(), breakToken);
    }

    public Node evalContinue(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        if (unexpectedModifiers(modifiers)) return null;
        final Token continueToken = parser.getAndExpect(tokens, 0, NodeType.LITERAL_CONTINUE);
        final Token semi = parser.getAndExpect(tokens, 1, NodeType.SEMI);
        if (Parser.anyNull(continueToken, semi)) return null;

        return new Node(parser.currentNode(), NodeType.CONTINUE_STATEMENT, continueToken.actualLine(), continueToken);
    }

    public List<Node> parseParametersDefineFunction(@NotNull final List<Token> tokens) {
        final List<Node> result = new ArrayList<>();
        if (tokens.isEmpty()) return result;
        Node currentNode = new Node(NodeType.DECLARE_VARIABLE, -1);
        final List<Node> currentNodeModifiers = new ArrayList<>();
        boolean addedNode = false;
        boolean parsedDatatype = false;
        boolean parsedIdentifier = false;

        for (@NotNull final Token token : tokens) {
            final Node asNode = new Node(NodeType.of(token), token, token.actualLine());
            final NodeType type = asNode.type();
            final MiDatatype asMiDatatype = parsedDatatype ? null : MiDatatype.of(token.token(), nullable(currentNodeModifiers));

            if (type == NodeType.COMMA) {
                currentNode.addChildren(new Node(NodeType.MODIFIERS, -1, currentNodeModifiers));
                result.add(currentNode);
                currentNode = new Node(NodeType.DECLARE_VARIABLE, -1);
                currentNodeModifiers.clear();
                parsedDatatype = false;
                parsedIdentifier = false;
                addedNode = true;
                continue;
            }
            addedNode = false;
            if (type.isModifier()) {
                if (parsedIdentifier || parsedDatatype) {
                    parser.parserError("Unexpected token '" + token.token() + "' while parsing function parameters, expected modifiers before datatype before identifier", token);
                    return new ArrayList<>();
                }
                currentNodeModifiers.add(asNode);
                continue;
            }
            if (asMiDatatype != null) {
                parsedDatatype = true;
                currentNode.addChildren(asNode);
                continue;
            }
            parsedIdentifier = true;
            currentNode.addChildren(asNode);
        }
        if (!addedNode) {
            currentNode.addChildren(new Node(NodeType.MODIFIERS, -1, currentNodeModifiers));
            result.add(currentNode);
            result.forEach(n -> {
                if (n.child(1).value() == null) {
                    parser.parserError("Expected identifier after datatype", n.child(0).value());
                }
            });

            final Set<String> duplicates = findFirstDuplicate(result.stream().map(n -> n.child(1).value().token()).toList());
            if (!duplicates.isEmpty()) {
                final String duplicate = duplicates.stream().toList().get(0);
                parser.parserError("Redefinition of function argument '" + duplicate + "'", tokens.get(0));
                return new ArrayList<>();
            }
        }
        return result;
    }

    public static <T> Set<T> findFirstDuplicate(@NotNull final List<T> list) {
        final Set<T> items = new HashSet<>();
        return list.stream()
                .filter(n -> !items.add(n))
                .collect(Collectors.toSet());

    }

    public Node evalFunctionDefinition(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        final Token fnToken = parser.getAndExpect(tokens, 0, NodeType.LITERAL_FN);
        final Token identifier = parser.getAndExpect(tokens, 1, NodeType.IDENTIFIER);
        final Token retDef = parser.getAndExpect(tokens, 2, NodeType.LPAREN, NodeType.DOUBLE_COLON, NodeType.LBRACE);
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
        final List<Optional<MiModifier>> modifs = modifiers.stream().map(n -> MiModifier.of(n.type())).toList();
        if (ret == NodeType.LBRACE) {
            if (nativeFunc) {
                parser.parserError("Expected ';' after native function definition", last);
                return null;
            }
            return new Node(parser.currentNode(), NodeType.FUNCTION_DEFINITION, identifier.actualLine(),
                    new Node(NodeType.IDENTIFIER, identifier, identifier.actualLine()),
                    new Node(NodeType.TYPE, Token.of("void"), -1),
                    new Node(NodeType.MODIFIERS, modifiers.isEmpty() ? -1 : modifiers.get(0).lineDebugging(), modifiers),
                    new Node(NodeType.PARAMETERS, -1, Collections.emptyList())
            );
        }
        final int extraIndex = ret == NodeType.LPAREN ? -1 : 1;
        final MiDatatype returnType;
        if (extraIndex == -1) returnType = MiDatatype.VOID;
        else {
            final Token returnToken = parser.getAndExpect(tokens, 3,
                    Arrays.stream(NodeType.values())
                            .filter(t -> t.isDatatype() || t == NodeType.LITERAL_VOID || t == NodeType.IDENTIFIER)
                            .toList()
                            .toArray(new NodeType[0]));

            if (returnToken == null) return null;

            returnType = MiDatatype.of(returnToken.token(), nullableOptModifiers(modifs));
        }
        if (nativeFunc) return evalNativeFunction(tokens, modifiers, extraIndex, last, identifier, returnType, retDef);

        final Token parenOpen = parser.getAndExpect(tokens, 3 + extraIndex, NodeType.LPAREN);
        final Token parenClose = parser.getAndExpect(tokens, tokens.size() - 2, NodeType.RPAREN);
        if (Parser.anyNull(parenOpen, parenClose)) return null;

        final List<Node> params = parseParametersDefineFunction(tokens.subList(4 + extraIndex, tokens.size() - 2));

        return new Node(parser.currentNode(), NodeType.FUNCTION_DEFINITION, identifier.actualLine(),
                new Node(NodeType.IDENTIFIER, identifier, identifier.actualLine()),
                new Node(NodeType.TYPE, Token.of(returnType.name()), retDef.actualLine()),
                new Node(NodeType.MODIFIERS, modifiers.isEmpty() ? -1 : modifiers.get(0).lineDebugging(), modifiers),
                new Node(NodeType.PARAMETERS, tokens.get(3 + extraIndex).actualLine(), params)
        );
    }

    public Node evalNativeFunction(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers, final int extraIndex,
                                   @NotNull final Token last, @NotNull final Token identifier, @NotNull final MiDatatype returnType, @NotNull final Token retDef) {
        final Token beforeLastArrow = parser.getAny(tokens, tokens.size() - 3);
        if (beforeLastArrow == null || NodeType.of(beforeLastArrow) != NodeType.ARROW) {
            parser.parserError("Expected '-> <constant-string-literal>' after ')' in native function definition", last,
                    "The scheme for native functions is: <modifiers> <identifier> <return-definition> ( <args> ) -> <constant-string-literal>");
            return null;
        }
        final Token stringLiteral = parser.getAndExpect(tokens, tokens.size() - 2, NodeType.STRING_LITERAL);
        if (stringLiteral == null) return null;

        final Token parenOpen = parser.getAndExpect(tokens, 3 + extraIndex, NodeType.LPAREN);
        final Token parenClose = parser.getAndExpect(tokens, tokens.size() - 4, NodeType.RPAREN);
        if (parenOpen == null || parenClose == null) return null;

        final List<Node> params = parseParametersDefineFunction(tokens.subList(4 + extraIndex, tokens.size() - 4));

        return new Node(parser.currentNode(), NodeType.NATIVE_FUNCTION_DEFINITION, identifier.actualLine(),
                new Node(NodeType.IDENTIFIER, identifier, identifier.actualLine()),
                new Node(NodeType.TYPE, Token.of(returnType.name()), retDef.actualLine()),
                new Node(NodeType.MODIFIERS, modifiers.isEmpty() ? -1 : modifiers.get(0).lineDebugging(), modifiers),
                new Node(NodeType.PARAMETERS, tokens.get(3 + extraIndex).actualLine(), params),
                new Node(NodeType.NATIVE_JAVA_FUNCTION_STR, stringLiteral, stringLiteral.actualLine())
        );
    }

    public Node evalIfStatement(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        return evalConditional(tokens, modifiers, NodeType.LITERAL_IF, false);
    }

    public Node evalElseStatement(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers, final boolean scope) {
        if (unexpectedModifiers(modifiers)) return null;
        final Token elseToken = parser.getAndExpect(tokens, 0, NodeType.LITERAL_ELSE);
        final Token op = parser.getAndExpect(tokens, tokens.size() - 1, NodeType.LBRACE, NodeType.SEMI);
        if (Parser.anyNull(elseToken, op)) return null;

        final Node instr = tokens.size() == 2
                ? new Node(NodeType.NOOP, op, op.actualLine())
                : (scope ? evalScoped(tokens.subList(1, tokens.size())) : evalUnscoped(tokens.subList(1, tokens.size())));

        return new Node(NodeType.ELSE_STATEMENT, elseToken, elseToken.actualLine(), instr);
    }

    public Node evalWhileStatement(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers, final boolean unscoped) {
        return evalConditional(tokens, modifiers, NodeType.LITERAL_WHILE, unscoped);
    }

    public Node evalDoStatement(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        if (unexpectedModifiers(modifiers)) return null;
        final Token doToken = parser.getAndExpect(tokens, 0, NodeType.LITERAL_DO);
        final Token scopeToken = parser.getAndExpect(tokens, 1, NodeType.LBRACE);
        if (Parser.anyNull(doToken, scopeToken)) return null;

        return new Node(parser.currentNode(), NodeType.DO_STATEMENT, doToken.actualLine(), doToken);
    }

    public Node evalForStatement(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        if (unexpectedModifiers(modifiers)) return null;
        final List<List<Token>> exprs = splitByComma(tokens.subList(1, tokens.size() - 1));
        if (exprs.isEmpty()) {
            parser.parserError("Expected expression after 'for' statement", tokens.get(0));
            return null;
        }
        return switch (exprs.size()) {
            case 2 -> evalTransitionalForStatement(tokens.get(0), exprs);
            case 3 -> evalTraditionalForStatement(tokens.get(0), exprs);
            default -> {
                final int atIndex = exprs.subList(0, exprs.size() - 1).stream().map(l -> l.size() + 1).reduce(0, Integer::sum) - 1;
                final Token at = tokens.get(atIndex);
                parser.parserError("Unexpected token '" + at.token() + "'", at, "Expected 2 or 3 expressions, so remove any trailing ones, or add a second expression if you only have 1.");
                yield null;
            }
        };
    }

    // for mut? i = 0, i < 10, i++
    public Node evalTraditionalForStatement(@NotNull final Token forToken, @NotNull final List<List<Token>> exprs) {
        final Node createVariable = evalUnscoped(exprs.get(0));
        if (createVariable == null) return null;
        if (createVariable.type() != NodeType.DEFINE_VARIABLE) {
            parser.parserError("Expected variable definition", forToken);
            return null;
        }
        final Node condition = parseExpression(exprs.get(1).get(0), exprs.get(1).subList(0, exprs.get(1).size() - 1));
        final Node loopStatement = evalUnscoped(exprs.get(2));

        if (loopStatement == null) return null;
        if (loopStatement.type() != NodeType.MUTATE_VARIABLE && loopStatement.type() != NodeType.FUNCTION_CALL) {
            parser.parserError("Expected variable set or function call as for loop instruct", exprs.get(0).get(0));
            return null;
        }
        return new Node(parser.currentNode(), NodeType.FOR_FAKE_SCOPE, exprs.get(0).get(0).actualLine(),
                new Node(NodeType.FOR_STATEMENT, forToken, exprs.get(0).get(0).actualLine(),
                        createVariable,
                        new Node(NodeType.CONDITION, exprs.get(1).get(0).actualLine(), condition),
                        new Node(NodeType.FOR_INSTRUCT, exprs.get(2).get(0).actualLine(), loopStatement)
                )
        );
    }

    // for mut? i = 0, i -> 10 // i goes in a transition to 10, exactly the same as above example but written differently
    public Node evalTransitionalForStatement(@NotNull final Token forToken, @NotNull final List<List<Token>> exprs) {
        final List<Token> transition = exprs.get(1);
        final Token identifier = parser.getAndExpect(transition, 0, NodeType.IDENTIFIER);
        final Token arrow = parser.getAndExpect(transition, 1, NodeType.DOUBLE_DOT);
        if (Parser.anyNull(arrow, identifier)) return null;

        final List<Token> val = transition.subList(2, transition.size());

        final List<Token> condition = new ArrayList<>(Arrays.asList(identifier, Token.of("<"), Token.of("(")));
        condition.addAll(val);
        condition.addAll(Arrays.asList(Token.of(")"), Token.of(";"))); // semicolon needed as splitByComma() automatically puts those & evalTraditionalForStatement thinks there always is a semicolon

        return evalTraditionalForStatement(forToken, Arrays.asList(
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
        if (tokens.size() == 2) {
            parser.parserError("Expected condition after '" + condType.getAsString() + "'.", first, "Add a boolean condition to resolve the issue.");
            return null;
        }

        final Node expr = parseExpression(first, tokens.subList(1, tokens.size() - 1));

        return new Node(parser.currentNode(), NodeType.valueOf(condType.getAsString().toUpperCase() + "_STATEMENT" + (unscoped ? "_UNSCOPED" : "")), first.actualLine(), first,
                new Node(NodeType.CONDITION, first.actualLine(), expr)
        );
    }

    private boolean unexpectedModifiers(@NotNull final List<Node> modifiers) {
        if (!modifiers.isEmpty()) {
            parser.parserError("Unexpected modifiers.", modifiers.get(0).value(), "Remove any modifiers here to fix this issue.");
            return true;
        }
        return false;
    }

    public Node evalUseStatement(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        if (unexpectedModifiers(modifiers)) return null;

        final Token use = parser.getAndExpect(tokens, 0, NodeType.LITERAL_USE);
        final Token identifier = parser.getAndExpect(tokens, 1, NodeType.IDENTIFIER);
        final Token semi = parser.getAndExpect(tokens, 2, NodeType.SEMI);
        if (Parser.anyNull(use, identifier, semi)) return null;

        return new Node(parser.currentNode(), NodeType.USE_STATEMENT, identifier.actualLine(), new Node(NodeType.IDENTIFIER, identifier, identifier.actualLine()));
    }

    public Node evalModuleDefinition(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        if (unexpectedModifiers(modifiers)) return null;

        final Token identifier = parser.getAndExpect(tokens, 1, NodeType.IDENTIFIER);
        final Token lbrace = parser.getAndExpect(tokens, 2, NodeType.LBRACE);
        if (Parser.anyNull(lbrace, identifier)) return null;

        return new Node(parser.currentNode(), NodeType.CREATE_MODULE, identifier.actualLine(),
                new Node(NodeType.IDENTIFIER, identifier, identifier.actualLine())
        );
    }

    public Node evalEnumDefinition(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        final Token identifier = parser.getAndExpect(tokens, 1, NodeType.IDENTIFIER);
        final Token lbrace = parser.getAndExpect(tokens, 2, NodeType.LBRACE);
        if (Parser.anyNull(lbrace, identifier)) return null;

        final Optional<Node> firstMutabilityModif = modifiers.stream().filter(m -> m.type().isMutabilityModifier()).findFirst();
        if (firstMutabilityModif.isPresent()) {
            parser.parserError("Cannot declare enums as own, const or mut, they are automatically constant because they cannot be redefined",
                    firstMutabilityModif.get().value());
            return null;
        }
        return new Node(parser.currentNode(), NodeType.CREATE_ENUM, identifier.actualLine(),
                new Node(NodeType.IDENTIFIER, identifier, identifier.actualLine()),
                new Node(NodeType.MODIFIERS, modifiers.isEmpty() ? -1 : modifiers.get(0).lineDebugging(), modifiers)
        );
    }

    public Node evalStdLibFinish(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        if (unexpectedModifiers(modifiers)) return null;

        final Token stdlibFinish = parser.getAndExpect(tokens, 0, NodeType.STANDARDLIB_MI_FINISH_CODE);
        final Token semi = parser.getAndExpect(tokens, 1, NodeType.SEMI);
        if (stdlibFinish == null || semi == null) return null;

        parser.reachedStdlibFinish(true);
        return new Node(parser.currentNode(), NodeType.STANDARDLIB_MI_FINISH_CODE, stdlibFinish.actualLine());
    }

    public Node evalStructDefinition(@NotNull final List<Token> tokens, @NotNull final List<Node> modifiers) {
        final Token identifier = parser.getAndExpect(tokens, 1, NodeType.IDENTIFIER);
        final Token lbrace = parser.getAndExpect(tokens, 2, NodeType.LBRACE);
        if (Parser.anyNull(lbrace, identifier)) return null;

        final Optional<Node> firstMutabilityModif = modifiers.stream().filter(m -> m.type().isMutabilityModifier()).findFirst();
        if (firstMutabilityModif.isPresent()) {
            parser.parserError("Cannot declare structs as own, const or mut, they are automatically constant because they cannot be redefined",
                    firstMutabilityModif.get().value());
            return null;
        }

        return new Node(parser.currentNode(), NodeType.CREATE_STRUCT, identifier.actualLine(),
                new Node(NodeType.IDENTIFIER, identifier, identifier.actualLine()),
                new Node(NodeType.MODIFIERS, modifiers.isEmpty() ? -1 : modifiers.get(0).lineDebugging(), modifiers)
        );
    }

}
