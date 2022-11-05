package org.crayne.mi.parsing.parser;

import org.crayne.mi.lang.*;
import org.crayne.mi.parsing.ast.Node;
import org.crayne.mi.parsing.ast.NodeType;
import org.crayne.mi.parsing.lexer.Token;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ASTExpressionParser {

    private int parsingPosition = -1;
    private Token currentToken;
    private final Token equalsToken;
    private final List<Token> expr;
    private final ASTRefiner refiner;
    private final MiModule module;
    private final MiFunctionScope function;

    public record TypedNode(MiDatatype type, Node node) {
        public static TypedNode empty() {
            return new TypedNode(null, new Node(NodeType.VALUE, -1));
        }
        public int lineDebugging() {
            return node == null ? -1 : node.lineDebugging();
        }
    }

    public ASTExpressionParser(@NotNull final List<Token> expr, @NotNull final Token equalsToken, @NotNull final ASTRefiner refiner,
                               @NotNull final MiModule module) {
        this.expr = expr;
        this.equalsToken = equalsToken;
        this.refiner = refiner;
        this.module = module;
        this.function = null;
    }

    public ASTExpressionParser(@NotNull final List<Token> expr, @NotNull final Token equalsToken, @NotNull final ASTRefiner refiner,
                               @NotNull final MiInternFunction internFunction) {
        this.expr = expr;
        this.equalsToken = equalsToken;
        this.refiner = refiner;
        this.module = internFunction.module();
        this.function = internFunction;
    }

    public ASTExpressionParser(@NotNull final List<Token> expr, @NotNull final Token equalsToken, @NotNull final ASTRefiner refiner,
                               @NotNull final MiContainer container) {
        this.expr = expr;
        this.equalsToken = equalsToken;
        this.refiner = refiner;

        if (container instanceof final MiInternFunction internFunction) {
            this.module = internFunction.module();
            this.function = internFunction;
            return;
        }
        if (container instanceof final MiFunctionScope functionScope) {
            this.module = functionScope.function().module();
            this.function = functionScope;
            return;
        }
        if (container instanceof final MiModule miModule) {
            this.module = miModule;
            this.function = null;
            return;
        }
        this.module = null;
        this.function = null;
    }

    private void nextPart() {
        ++parsingPosition;
        currentToken = parsingPosition >= 0 && parsingPosition < expr.size() ? expr.get(parsingPosition) : null;
    }

    public TypedNode parse() {
        nextPart();
        final TypedNode x = parseExpression();
        if (parsingPosition < expr.size()) {
            final Token val = expr.get(parsingPosition);
            if (val != null) refiner.parser().parserError("Unexpected token '" + val.token() + "', couldn't parse expression", val);
        }
        return x;
    }

    private boolean ternaryExpectColon(@NotNull final TypedNode y) {
        if (currentToken == null) {
            refiner.parser().parserError("Expected else-value after ternary 'if'", y.node.value());
            return true;
        }

        if (NodeType.of(currentToken) != NodeType.COLON) {
            final boolean expectValue = y.node.value() != null && NodeType.of(y.node.value()) == NodeType.COLON;
            refiner.parser().parserError(
                    expectValue ? "Expected if-value after ternary condition" : "Expected ':' after ternary 'if'",
                    y.node.value());
            return true;
        }
        return false;
    }

    private boolean ternaryIfElseNotEqual(@NotNull final TypedNode z, @NotNull final TypedNode y) {
        if (z.type == null) {
            refiner.parser().parserError("Expected value after ':'", y.node.value());
            return true;
        }
        if (!z.type.equals(y.type)) {
            refiner.parser().parserError("'if' part of ternary operator should have the same type as the 'else' part of the ternary operator", equalsToken,
                    "'if' part is of type " + y.type + ", while 'else' part is " + z.type + ".",
                    "Cast either of the two values to one shared datatype to fix the issue.",
                    "Use std.to_nonnull() to explicitely convert nullable types to nonnull types."
            );
            return true;
        }
        return false;
    }

    private boolean ternaryConditionNotBoolean(@NotNull final TypedNode x) {
        if (!MiDatatype.match(x.type, MiDatatype.BOOL)) {
            refiner.parser().parserError("Ternary operator condition should be of type 'nonnull bool' but is instead '" + x.type + "'", x.node.value());
            return true;
        }
        return false;
    }

    private TypedNode evalTernaryOperator(final TypedNode x, final TypedNode y) {
        if (ternaryExpectColon(y)) return TypedNode.empty();
        nextPart();

        final TypedNode z = parseExpression();

        if (ternaryIfElseNotEqual(z, y)) return TypedNode.empty();
        if (ternaryConditionNotBoolean(x)) return TypedNode.empty();

        return new TypedNode(y.type, new Node(NodeType.TERNARY_OPERATOR, x.node.value(), x.lineDebugging(),
                new Node(NodeType.CONDITION, x.node.value(), x.lineDebugging(), x.node),
                new Node(NodeType.TERNARY_OPERATOR_IF, y.lineDebugging(), y.node),
                new Node(NodeType.TERNARY_OPERATOR_ELSE, z.lineDebugging(), z.node)
        ));
    }

    private static boolean isComparator(@NotNull final String token) {
        return switch (NodeType.of(token)) {
            case EQUALS, NOTEQUALS, GREATER_THAN, GREATER_THAN_EQ, LESS_THAN, LESS_THAN_EQ -> true;
            default -> false;
        };
    }

    private TypedNode evalExpression(final TypedNode x, final TypedNode y, final Token op) {
        if (NodeType.of(op) == NodeType.QUESTION_MARK) return evalTernaryOperator(x, y);
        if (x.type == null || y.type == null) return TypedNode.empty();

        return new TypedNode(
                isComparator(op.token()) ? MiDatatype.BOOL : MiDatatype.heavier(x.type, y.type),
                new Node(NodeType.of(op.token()), -1, x.node, y.node));
    }

    private static final List<List<NodeType>> operatorPrecedence = Arrays.asList(
            List.of(NodeType.MULTIPLY, NodeType.DIVIDE, NodeType.MODULUS),
            List.of(NodeType.ADD, NodeType.SUBTRACT),
            List.of(NodeType.RSHIFT, NodeType.LSHIFT),
            List.of(NodeType.LESS_THAN, NodeType.GREATER_THAN, NodeType.LESS_THAN_EQ, NodeType.GREATER_THAN_EQ),
            List.of(NodeType.EQUALS, NodeType.NOTEQUALS),
            List.of(NodeType.BIT_AND),
            List.of(NodeType.XOR),
            List.of(NodeType.BIT_OR),
            List.of(NodeType.LOGICAL_AND),
            List.of(NodeType.LOGICAL_OR),
            List.of(NodeType.QUESTION_MARK)
    );

    private TypedNode parseExpression() {
        return parseExpression(operatorPrecedence.size() - 1);
    }

    private TypedNode parseExpression(final int precendece) {
        TypedNode nodeX = precendece > 0 ? parseExpression(precendece - 1) : parseFactor();
        if (nodeX == null) {
            refiner.parser().parserError("Unexpected parsing error", currentToken);
            return TypedNode.empty();
        }
        for (; ; ) {
            try {
                if (currentToken != null && precendece >= 0 && (operatorPrecedence.get(precendece).contains(NodeType.of(currentToken.token())))) {
                    final Token op = currentToken;
                    nextPart();
                    final TypedNode nodeY = parseExpression(precendece - 1);

                    if (nodeX.type == null || cannotUseOperator(nodeX, op)) return TypedNode.empty();
                    if (nodeY.type == null || (NodeType.of(op) != NodeType.QUESTION_MARK && cannotUseOperator(nodeY, op))) return TypedNode.empty();
                    if (!nodeX.type.primitive() && !nodeY.type.primitive() && !nodeX.type.name().equals(nodeY.type.name())) {
                        refiner.parser().parserError("Enums are not the same; Expected the same enum types for left and right operands (" + nodeX.type + " vs " + nodeY.type + ")", op);
                        return TypedNode.empty();
                    }

                    nodeX = evalExpression(nodeX, nodeY, op);
                } else {
                    return nodeX;
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void cannotUseOperatorError(@NotNull final TypedNode fact, @NotNull final Token prev) {
        refiner.parser().parserError("Cannot use operator '" + prev.token() + "' for " + fact.type + " values", equalsToken);
    }

    private boolean cannotUseOperator(@NotNull final TypedNode fact, @NotNull final Token prev) {
        if (MiDatatype.operatorUndefined(prev.token(), fact.type.name())) {
            cannotUseOperatorError(fact, prev);
            return true;
        }
        return false;
    }

    private static TypedNode embraceFactor(@NotNull final TypedNode factor, @NotNull final NodeType nodeType) {
        return new TypedNode(factor.type,
                new Node(
                        nodeType,
                        -1,
                        factor.node
                )
        );
    }

    private TypedNode parseNumberFactor(@NotNull final Token prev) {
        if (expectValue(prev)) return TypedNode.empty();
        nextPart();

        final TypedNode fact = parseFactor();
        return cannotUseOperator(fact, prev) ? TypedNode.empty() : fact;
    }

    private TypedNode negateNumberFactor(@NotNull final Token prev) {
        if (expectValue(prev)) return TypedNode.empty();
        nextPart();

        final TypedNode fact = parseFactor();
        final TypedNode negated = embraceFactor(fact, NodeType.NEGATE);
        return cannotUseOperator(fact, prev) ? TypedNode.empty() : negated;
    }

    private TypedNode invertBooleanFactor(@NotNull final Token prev) {
        if (expectValue(prev)) return TypedNode.empty();
        nextPart();

        final TypedNode fact = parseFactor();
        final TypedNode inverted = embraceFactor(fact, NodeType.BOOL_NOT);
        return cannotUseOperator(fact, prev) ? TypedNode.empty() : inverted;
    }

    private TypedNode bitwiseInvertIntegerFactor(@NotNull final Token prev) {
        if (expectValue(prev)) return TypedNode.empty();
        nextPart();

        final TypedNode fact = parseFactor();
        final TypedNode inverted = embraceFactor(fact, NodeType.BIT_NOT);
        return cannotUseOperator(fact, prev) ? TypedNode.empty() : inverted;
    }

    private TypedNode castFactor(@NotNull final Token prev) {
        final String datatype = prev.token();
        if (expectValue(prev)) return TypedNode.empty();
        nextPart();
        final TypedNode factor = parseFactor();
        if (factor.type == null) return TypedNode.empty();
        return castValue(factor, Token.of(datatype), factor.type.nullable() || factor.type.name().equals("null"));
    }

    private boolean expectValue(@NotNull final Token prev) {
        if (parsingPosition + 1 >= expr.size()) {
            refiner.parser().parserError("Expected value after '" + prev.token() + "'", prev);
            return true;
        }
        return false;
    }

    private Optional<TypedNode> handleFactorPrefixes() {
        final Token prev = currentToken;
        if (currentToken == null) {
            refiner.parser().parserError("Expected value", expr.isEmpty() ? equalsToken : expr.get(expr.size() - 1));
            return Optional.of(TypedNode.empty());
        }

        final NodeType tokenType = NodeType.of(prev);
        if (tokenType.isDatatype() && tokenType != NodeType.QUESTION_MARK) return Optional.of(castFactor(prev));

        return Optional.ofNullable(switch (tokenType) {
            case ADD, INCREMENT_LITERAL, DECREMENT_LITERAL -> parseNumberFactor(prev); // +, ++ and -- as PREFIXES do not change a number, but they only make sense when used on any number datatype
            case SUBTRACT -> negateNumberFactor(prev);
            case EXCLAMATION_MARK -> invertBooleanFactor(prev);
            case TILDE -> bitwiseInvertIntegerFactor(prev);
            default -> null;
        });
    }

    private TypedNode evalEnumMember(@NotNull final Token nextPart) {
        final Token enumMember = expr.get(parsingPosition + 2);
        if (NodeType.of(enumMember) == NodeType.IDENTIFIER) {
            final Token enumName = currentToken;
            nextPart();
            nextPart();
            nextPart();
            final Optional<MiEnum> foundEnum = refiner.findEnumByName(enumName, module);
            if (foundEnum.isEmpty()) {
                refiner.parser().parserError("Cannot find an enum called '" + enumName.token() + "' here", enumName,
                        "Did you spell the enum name correctly? Are you sure you are using the right module?");
                return TypedNode.empty();
            }
            final Set<MiModifier> enumModifiers = foundEnum.get().modifiers();
            final MiModifier vmodifier = MiModifier.effectiveVisibilityModifier(enumModifiers);
            if (MiModifier.invalidAccess(enumModifiers, foundEnum.get().module(), module)) {
                refiner.parser().parserError("Cannot access " + vmodifier.getName() + " enum '" + enumName.token() + "' here", enumName,
                        "Are you sure you are using the right module? Are you using the correct enum?");
                return TypedNode.empty();
            }
            if (!foundEnum.get().members().contains(enumMember.token())) {
                refiner.parser().parserError("Cannot find an enum member with the name '" + enumMember.token() + "' in the specified enum '" + enumName.token() + "'", enumMember,
                        "Did you spell the enum member name correctly? Are you sure you are using the right enum?");
                return TypedNode.empty();
            }

            final MiDatatype miDatatype = new MiDatatype(foundEnum.get().identifier().token(), false);

            return new TypedNode(miDatatype, new Node(NodeType.GET_ENUM_MEMBER, enumName.actualLine(),
                    new Node(NodeType.IDENTIFIER, foundEnum.get().identifier(), enumName.actualLine()),
                    new Node(NodeType.MEMBER, enumMember, enumMember.actualLine())
            ));
        } else {
            refiner.parser().parserError("Unexpected token '::'", nextPart);
            return TypedNode.empty();
        }
    }

    private TypedNode evalFunctionCall() {
        final Token identifier = currentToken;
        final List<TypedNode> parsedArgs = parseArgs();
        if (parsedArgs == null) {
            refiner.parser().parserError("Could not parse function call parameters", identifier);
            return TypedNode.empty();
        }
        final List<MiDatatype> callParams = parsedArgs.stream().map(TypedNode::type).toList();

        final Optional<MiFunction> foundFunction = refiner.findFunctionByCall(identifier, callParams, module, function != null ? function.using() : Collections.emptyList());
        if (foundFunction.isEmpty()) {
            refiner.parser().parserError("Cannot find any function called '" + identifier.token() + "' with the specified arguments " + callParams + " here", identifier,
                    "Are you sure you spelled the function name correctly? Are you using the right module?");
            return TypedNode.empty();
        }
        final MiDatatype returnType = foundFunction.get().returnType();
        if (returnType.equals(MiDatatype.VOID, true)) {
            refiner.parser().parserError("Cannot use void functions as values in expressions", identifier,
                    "Extract the function call into a seperate statement");
            return TypedNode.empty();
        }

        return new TypedNode(returnType, new Node(NodeType.FUNCTION_CALL, identifier.actualLine(),
                new Node(NodeType.IDENTIFIER, foundFunction.get().identifier(), identifier.actualLine()),
                new Node(NodeType.PARAMETERS, identifier.actualLine(), parsedArgs.stream().map(n ->
                        new Node(NodeType.PARAMETER, n.lineDebugging(),
                                new Node(NodeType.VALUE, n.lineDebugging(), n.node()),
                                new Node(NodeType.TYPE, Token.of(n.type().name()), n.lineDebugging())
                        )
                ).toList())
        ));
    }

    private TypedNode evalVariable(final Token nextPart) {
        final Token identifier = currentToken;
        final Optional<MiVariable> variable = refiner.findVariable(identifier, module, function);
        if (variable.isEmpty()) {
            refiner.parser().parserError("Cannot find variable '" + identifier.token() + "'", identifier,
                    "Did you spell the variable name correctly?");
            return TypedNode.empty();
        }

        if (nextPart != null) {
            final Optional<MiEqualOperator> eq = MiEqualOperator.of(nextPart.token());
            if (eq.isPresent()) {
                nextPart();
                nextPart();

                final TypedNode val = NodeType.of(nextPart).incrementDecrement() ? null : parseExpression();
                final Node varMutation = new Node(NodeType.MUTATE_VARIABLE, nextPart.actualLine(),
                        ASTGenerator.variableChange(variable.get(), identifier, val == null ? null : new Node(NodeType.VALUE, val.lineDebugging(), val.node), nextPart));

                if (MiDatatype.operatorUndefined(nextPart.token(), variable.get().type().name())) {
                    refiner.parser().parserError("Cannot use operator '" + nextPart.token() + "' for " + variable.get().type() + " values.", nextPart);
                    return TypedNode.empty();
                }

                refiner.checkVariableMutation(varMutation, function, module);
                if (refiner.parser().encounteredError()) return TypedNode.empty();

                return new TypedNode(variable.get().type(), varMutation);
            }
        }
        if (variable.get().uninitialized()) {
            refiner.parser().parserError("Variable '" + identifier.token() + "' might have not been initialized yet", identifier,
                    "Give the variable an explicit value by using the normal set (=) operator");
            return TypedNode.empty();
        }
        final TypedNode result = new TypedNode(variable.get().type(), new Node(NodeType.IDENTIFIER, variable.get().identifier(), identifier.actualLine()));
        nextPart();
        return result;
    }

    private TypedNode evalStructCreation(@NotNull final Token nextPart) {
        final MiDatatype miDatatype = new MiDatatype(nextPart.token(), false);
        return new TypedNode(miDatatype, new Node(NodeType.STRUCT_CONSTRUCT, nextPart.actualLine(),
                new Node(NodeType.IDENTIFIER, nextPart, nextPart.actualLine())
        ));
    }

    private TypedNode parseFactor() {
        final Optional<TypedNode> prefixed = handleFactorPrefixes();
        if (prefixed.isPresent()) return prefixed.get();

        if (currentToken == null) {
            refiner.parser().parserError("Unexpected parsing error", expr.get(0));
            return TypedNode.empty();
        }
        final Token nextPart = parsingPosition + 1 < expr.size() ? expr.get(parsingPosition + 1) : null;
        final NodeType currentType = NodeType.of(currentToken);
        final NodeType nextType = nextPart != null ? NodeType.of(nextPart) : null;

        switch (currentType) {
            case IDENTIFIER -> {
                if (nextType != null) switch (nextType) {
                    case DOUBLE_COLON -> {
                        if (parsingPosition + 2 >= expr.size()) {
                            refiner.parser().parserError("Expected enum ordinal identifier after '::'", currentToken);
                            return TypedNode.empty();
                        }
                        return evalEnumMember(nextPart);
                    }
                    case LPAREN -> {
                        return evalFunctionCall();
                    }
                }
                return evalVariable(nextPart);
            }
            /*case LITERAL_NEW -> {
                if (nextType != NodeType.IDENTIFIER) {
                    parserParent.parserError("Expected identifier after 'new'", currentToken);
                    return TypedNode.empty();
                }
                nextPart();
                nextPart();
                return evalStructCreation(nextPart);
            }*/
            case LPAREN -> {
                nextPart();
                final TypedNode result = parseExpression();
                if (!currentToken.token().equals(")")) refiner.parser().parserError("Expected ')' after expression in parenthesis", expr.get(expr.size() - 1));
                nextPart();
                return result;
            }
            default -> {
                final Token result = currentToken;
                final NodeType nodeType = result == null ? null : NodeType.of(result.token());
                final MiDatatype miDatatype = nodeType == null ? null : NodeType.getAsDataType(new Node(nodeType, result, result.actualLine()));
                if (miDatatype == null) return TypedNode.empty();
                nextPart();
                return new TypedNode(miDatatype, new Node(nodeType, result, result.actualLine()));
            }
        }
    }

    private List<TypedNode> parseArgs() {
        int foundEndingParen = -1;
        final int start = parsingPosition + 1;
        int paren = 0;
        while (parsingPosition < expr.size()) {
            if (currentToken.token().equals("(")) paren++;
            if (currentToken.token().equals(")")) {
                paren--;
                if (paren <= 0) {
                    foundEndingParen = parsingPosition;
                    break;
                }
            }
            nextPart();
        }
        if (foundEndingParen == -1) {
            refiner.parser().parserError("Expected ')' after arguments of function call", currentToken);
            return null;
        }
        nextPart();
        return parseParametersCallFunction(expr.subList(start + 1, foundEndingParen), function == null ? module : function);
    }

    public static List<TypedNode> parseParametersCallFunction(@NotNull final List<Token> tokens, @NotNull final ASTRefiner refiner,
                                                              @NotNull final MiContainer container) {
        final List<TypedNode> result = new ArrayList<>();

        if (tokens.isEmpty()) return result;

        final List<Token> currentArg = new ArrayList<>();
        int paren = 0;
        boolean addedNode = false;
        for (@NotNull final Token token : tokens) {
            final NodeType type = NodeType.of(token.token());

            if (type == NodeType.LPAREN) paren++;
            if (type == NodeType.RPAREN) paren--;
            if (type == NodeType.COMMA && paren == 0) {
                if (currentArg.isEmpty()) {
                    refiner.parser().parserError("Expected a parameter before ','", token);
                    return new ArrayList<>();
                }

                result.add(new ASTExpressionParser(currentArg, token, refiner, container).parse());
                currentArg.clear();
                addedNode = true;
                continue;
            }
            addedNode = false;
            currentArg.add(token);
        }
        if (!addedNode) result.add(new ASTExpressionParser(currentArg, currentArg.get(0), refiner, container).parse());
        return result;
    }

    private List<TypedNode> parseParametersCallFunction(@NotNull final List<Token> tokens, @NotNull final MiContainer container) {
        return parseParametersCallFunction(tokens, refiner, container);
    }

    private TypedNode castValue(@NotNull final TypedNode value, @NotNull final Token castType, final boolean nullable) {
        return new TypedNode(MiDatatype.of(castType.token(), nullable), new Node(NodeType.CAST_VALUE, castType, castType.actualLine(), value.node));
    }

}