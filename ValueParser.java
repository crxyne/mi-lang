package org.crayne.mu.parsing.parser;

import org.crayne.mu.lang.*;
import org.crayne.mu.lang.Enum;
import org.crayne.mu.parsing.ast.Node;
import org.crayne.mu.parsing.ast.NodeType;
import org.crayne.mu.parsing.lexer.Token;
import org.crayne.mu.parsing.parser.scope.FunctionScope;
import org.crayne.mu.parsing.parser.scope.Scope;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Stream;

public class ValueParser {

    private int parsingPosition = -1;
    private Token currentToken;
    private final List<Token> expr;
    private final Parser parserParent;

    public record TypedNode(Datatype type, Node node) {
        public static TypedNode empty() {
            return new TypedNode(null, new Node(NodeType.VALUE));
        }
    }

    public ValueParser(@NotNull final List<Token> expr, @NotNull final Parser parserParent) {
        this.expr = expr;
        this.parserParent = parserParent;
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
            parserParent.parserError("Unexpected token '" + expr.get(parsingPosition).token() + "', couldn't parse expression", val);
        }
        return x;
    }

    private boolean ternaryExpectColon(@NotNull final TypedNode y) {
        if (NodeType.of(currentToken) != NodeType.COLON) {
            parserParent.parserError("Expected ':' after ternary 'if'", y.node.value(), true);
            return true;
        }
        return false;
    }

    private boolean ternaryIfElseNotEqual(@NotNull final TypedNode z, @NotNull final TypedNode y) {
        if (!z.type.equals(y.type)) {
            parserParent.parserError("'if' part of ternary operator should (atleast implicitly) have the same type as the 'else' part of the ternary operator", z.node.value(),
                    "'if' part is of type " + y.type.getName() + ", while 'else' part is " + z.type.getName());
            return true;
        }
        return false;
    }

    private boolean ternaryConditionNotBoolean(@NotNull final TypedNode x) {
        if (x.type.notPrimitive() || !x.type.getPrimitive().equals(PrimitiveDatatype.BOOL)) {
            parserParent.parserError("Ternary operator condition should be of type 'bool' but is instead '" + x.type.getName() + "'", x.node.value());
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

        final int line = x.node.value().actualLine();

        return new TypedNode(y.type, new Node(NodeType.TERNARY_OPERATOR, line,
                new Node(NodeType.CONDITION, line, x.node),
                new Node(NodeType.TERNARY_OPERATOR_IF, line, y.node),
                new Node(NodeType.TERNARY_OPERATOR_ELSE, line, z.node)
        ));
    }

    private TypedNode evalExpression(final TypedNode x, final TypedNode y, final Token op) {
        if (parserParent.encounteredError) return TypedNode.empty();
        if (NodeType.of(op) == NodeType.QUESTION_MARK) return evalTernaryOperator(x, y);
        boolean operatorDefined = false;

        try {
            operatorDefined = x.type.operatorDefined(NodeType.of(op.token()), y.type);
        } catch (final Exception ignored) {}

        if (!operatorDefined) {
            if (x.type.equals(y.type)) {
                parserParent.parserError("Operator '" + op.token() + "' is not defined for operand type " + x.type.getName(), op);
                return TypedNode.empty();
            }
            parserParent.parserError("Operator '" + op.token() + "' is not defined for left operand " + x.type.getName() + " and right operand " + y.type.getName(), op);
            return TypedNode.empty();
        }
        return new TypedNode(
                PrimitiveDatatype.isComparator(op.token()) ? Datatype.BOOL : x.type.heavier(y.type),
                new Node(NodeType.of(op.token()), -1, x.node, y.node));
    }

    private static final List<Set<NodeType>> operatorPrecedence = Arrays.asList(
            Set.of(NodeType.MULTIPLY, NodeType.DIVIDE, NodeType.MODULUS),
            Set.of(NodeType.ADD, NodeType.SUBTRACT),
            Set.of(NodeType.RSHIFT, NodeType.LSHIFT),
            Set.of(NodeType.LESS_THAN, NodeType.GREATER_THAN, NodeType.LESS_THAN_EQ, NodeType.GREATER_THAN_EQ),
            Set.of(NodeType.EQUALS, NodeType.NOTEQUALS),
            Set.of(NodeType.BIT_AND),
            Set.of(NodeType.XOR),
            Set.of(NodeType.BIT_OR),
            Set.of(NodeType.LOGICAL_AND),
            Set.of(NodeType.LOGICAL_OR),
            Set.of(NodeType.QUESTION_MARK)
    );

    private TypedNode parseExpression() {
        return parseExpression(operatorPrecedence.size() - 1);
    }

    private TypedNode parseExpression(final int precendece) {
        TypedNode nodeX = precendece > 0 ? parseExpression(precendece - 1) : parseFactor();
        if (nodeX == null) {
            parserParent.parserError("Unexpected parsing error");
            return TypedNode.empty();
        }
        for (; ; ) {
            try {
                if (currentToken != null && precendece >= 0 && (operatorPrecedence.get(precendece).contains(NodeType.of(currentToken.token())))) {
                    final Token op = currentToken;
                    nextPart();
                    final TypedNode nodeY = parseExpression(precendece - 1);
                    nodeX = evalExpression(nodeX, nodeY, op);
                } else {
                    return nodeX;
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean noTypeMatches(@NotNull final TypedNode factor, @NotNull final Datatype... types) {
        return Stream.of(types).noneMatch(factor.type::equals);
    }

    private void cannotUseOperatorError(@NotNull final TypedNode fact, @NotNull final Token prev) {
        parserParent.parserError("Cannot use '" + prev.token() + "' operator on type '" + fact.type + "'", prev);
    }

    private boolean cannotUseOperator(@NotNull final TypedNode fact, @NotNull final Token prev, @NotNull final Datatype... types) {
        if (noTypeMatches(fact, types)) {
            cannotUseOperatorError(fact, prev);
            return true;
        }
        return false;
    }

    private boolean cannotUseNumberOperator(@NotNull final TypedNode fact, @NotNull final Token prev) {
        return cannotUseOperator(fact, prev, Datatype.INT, Datatype.LONG, Datatype.DOUBLE, Datatype.FLOAT);
    }

    private boolean cannotUseBooleanOperator(@NotNull final TypedNode fact, @NotNull final Token prev) {
        return cannotUseOperator(fact, prev, Datatype.BOOL);
    }

    @NotNull
    private static TypedNode embraceFactor(@NotNull final TypedNode factor, @NotNull final NodeType nodeType) {
        return new TypedNode(factor.type,
                new Node(
                        nodeType,
                        -1,
                        factor.node
                )
        );
    }

    @NotNull
    private TypedNode parseNumberFactor(@NotNull final Token prev) {
        if (expectValue(prev)) return TypedNode.empty();
        nextPart();

        final TypedNode fact = parseFactor();
        return cannotUseNumberOperator(fact, prev) ? TypedNode.empty() : fact;
    }

    @NotNull
    private TypedNode negateNumberFactor(@NotNull final Token prev) {
        if (expectValue(prev)) return TypedNode.empty();
        nextPart();

        final TypedNode fact = parseFactor();
        final TypedNode negated = embraceFactor(fact, NodeType.NEGATE);
        return cannotUseNumberOperator(fact, prev) ? TypedNode.empty() : negated;
    }

    @NotNull
    private TypedNode invertBooleanFactor(@NotNull final Token prev) {
        if (expectValue(prev)) return TypedNode.empty();
        nextPart();

        final TypedNode fact = parseFactor();
        final TypedNode inverted = embraceFactor(fact, NodeType.BOOL_NOT);
        return cannotUseBooleanOperator(fact, prev) ? TypedNode.empty() : inverted;
    }

    @NotNull
    private TypedNode castFactor(@NotNull final Token prev) {
        final String datatype = prev.token();
        if (expectValue(prev)) return TypedNode.empty();
        nextPart();

        return castValue(parseFactor(), Token.of(datatype));
    }

    private boolean expectValue(@NotNull final Token prev) {
        if (parsingPosition + 1 >= expr.size()) {
            parserParent.parserError("Expected value after '" + prev.token() + "'", prev, true);
            return true;
        }
        return false;
    }

    @NotNull
    private Optional<TypedNode> handleFactorPrefixes() {
        final Token prev = currentToken;
        if (parserParent.encounteredError || prev == null)
            return Optional.empty();

        final NodeType tokenType = NodeType.of(prev);
        if (NodeType.of(prev).isDatatype()) return Optional.of(castFactor(prev));

        return Optional.ofNullable(switch (tokenType) {
            case ADD, INCREMENT_LITERAL, DECREMENT_LITERAL -> parseNumberFactor(prev); // +, ++ and -- as PREFIXES do not change a number, but they only make sense when used on any number datatype
            case SUBTRACT -> negateNumberFactor(prev);
            case EXCLAMATION_MARK -> invertBooleanFactor(prev);
            default -> null;
        });
    }

    private TypedNode parseFunctionCall(@NotNull final Token prev) {
        final List<TypedNode> parsedArgs = parseArgs();
        if (parsedArgs == null || parserParent.encounteredError) return TypedNode.empty();

        final FunctionDefinition def = parserParent.evaluator().checkValidFunctionCall(prev, parsedArgs, true, true);
        if (def == null) return new TypedNode(null, new Node(NodeType.VALUE));

        final Datatype retType = def.returnType();
        if (retType == Datatype.VOID) {
            parserParent.parserError("Usage of void function as a value in expression", prev);
            return new TypedNode(null, new Node(NodeType.VALUE));
        }
        return new TypedNode(retType, new Node(NodeType.FUNCTION_CALL, prev.actualLine(),
                new Node(NodeType.IDENTIFIER, prev),
                new Node(NodeType.PARAMETERS, parsedArgs.stream().map(TypedNode::node).toList())
        ));
    }

    private TypedNode parseEnumMember(@NotNull final Token prev) {
        nextPart();
        if (currentToken == null) {
            parserParent.parserError("Expected identifier after '::'");
            return TypedNode.empty();
        }
        final Optional<Scope> scope = parserParent.scope();
        final FunctionScope functionScope = scope.isPresent() && scope.get() instanceof FunctionScope ? (FunctionScope) scope.get() : null;

        final Enum foundEnum = Datatype.findEnumByIdentifier(parserParent, functionScope != null ? functionScope.using() : Collections.emptyList(), prev, true);
        if (foundEnum == null) return new TypedNode(null, new Node(NodeType.VALUE));

        if (!foundEnum.members().contains(currentToken.token())) {
            parserParent.parserError("Enum '" + prev.token() + "' does not have ordinal '" + currentToken.token() + "'", currentToken);
            return new TypedNode(null, new Node(NodeType.VALUE));
        }
        final Datatype datatype = new Datatype(parserParent, prev);

        return new TypedNode(datatype, new Node(NodeType.GET_ENUM_MEMBER, prev.actualLine(),
                new Node(NodeType.IDENTIFIER, prev),
                new Node(NodeType.MEMBER, currentToken)
        ));
    }

    private TypedNode parseVariableSet(@NotNull final Token prev) {
        final NodeType currentTokenType = NodeType.of(currentToken);
        nextPart();

        final Optional<Variable> findVar = parserParent.evaluator().findVariable(currentToken, true);
        if (findVar.isEmpty()) return new TypedNode(null, new Node(NodeType.VALUE));

        final TypedNode val = currentTokenType == NodeType.INCREMENT_LITERAL || currentTokenType == NodeType.DECREMENT_LITERAL
                ? new TypedNode(Datatype.INT, new Node(NodeType.INTEGER_NUM_LITERAL, Token.of("1")))
                : parseExpression();

        return new TypedNode(findVar.get().type(), parserParent.evaluator().evalVariableChange(prev, val, currentToken, findVar.get()));
    }

    private Optional<TypedNode> parseIdentifierSuffix(@NotNull final Token prev) {
        if (currentToken == null) return Optional.empty();
        final NodeType currentTokenType = NodeType.of(currentToken);

        return Optional.ofNullable(switch (currentTokenType) {
            case LPAREN -> parseFunctionCall(prev);
            case DOUBLE_COLON -> parseEnumMember(prev);
            case SET, SET_ADD, SET_AND,
                    SET_DIV, SET_LSHIFT, SET_OR,
                    SET_MOD, SET_MULT, SET_RSHIFT,
                    SET_SUB, SET_XOR, INCREMENT_LITERAL,
                    DECREMENT_LITERAL -> parseVariableSet(prev);
            default -> null;
        });
    }

    private TypedNode parseIdentifier(@NotNull final Token prev) {
        final Optional<TypedNode> withSuffix = parseIdentifierSuffix(prev);
        if (withSuffix.isPresent()) return withSuffix.get();

        final Optional<Variable> findVar = parserParent.evaluator().findVariable(prev, true);
        if (findVar.isEmpty()) return TypedNode.empty();

        if (!findVar.get().initialized()) {
            parserParent.parserError("Variable '" + prev.token() + "' might not have been initialized yet", prev, "Set the value of the variable upon declaration");
            return TypedNode.empty();
        }
        return new TypedNode(findVar.get().type(), new Node(NodeType.IDENTIFIER, prev)); // there was no ::, ( or x= after the variable, so just get the variable value here
    }

    private TypedNode parseParenthesis() {
        final TypedNode parenthesisExpr = parseExpression();
        if (currentToken == null || NodeType.of(currentToken) != NodeType.RPAREN) {
            parserParent.parserError("Expected ')' after expression in parenthesis", expr.get(parsingPosition - 1));
            return TypedNode.empty();
        }
        nextPart();
        return parenthesisExpr;
    }

    private TypedNode parseLiteralFactor(final Token prev) {
        final NodeType nodeType = prev == null ? null : NodeType.of(prev.token());
        final Datatype datatype = nodeType == null ? null : NodeType.getAsDataType(parserParent, new Node(nodeType, prev));
        if (datatype == null) return TypedNode.empty();
        return new TypedNode(datatype, new Node(nodeType, prev));
    }

    private TypedNode parseFactor() {
        final Optional<TypedNode> prefixed = handleFactorPrefixes();
        if (prefixed.isPresent()) return prefixed.get();

        if (parserParent.encounteredError) return TypedNode.empty(); // if the prefix was there but threw an error, do not continue parsing like normal

        if (currentToken == null) {
            parserParent.parserError("Unexpected parsing error, value expected");
            return TypedNode.empty();
        }
        final Token prev = currentToken;
        nextPart();

        final NodeType currentTokenType = NodeType.of(prev);

        return switch (currentTokenType) {
            case IDENTIFIER -> parseIdentifier(prev);
            case LPAREN -> parseParenthesis();
            default -> parseLiteralFactor(prev);
        };
    }

    private List<TypedNode> parseArgs() {
        int foundEndingParen = -1;
        final int start = parsingPosition;
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
            parserParent.parserError("Expected ')' after arguments of function call");
            return null;
        }
        nextPart();
        return parseParametersCallFunction(expr.subList(start + 1, foundEndingParen));
    }

    public static List<TypedNode> parseParametersCallFunction(@NotNull final List<Token> tokens, @NotNull final Parser parserParent) {
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
                result.add(new ValueParser(currentArg, parserParent).parse());
                currentArg.clear();
                addedNode = true;
                continue;
            }
            addedNode = false;
            currentArg.add(token);
        }
        if (!addedNode) result.add(new ValueParser(currentArg, parserParent).parse());
        return result;
    }

    private List<TypedNode> parseParametersCallFunction(@NotNull final List<Token> tokens) {
        return parseParametersCallFunction(tokens, parserParent);
    }

    private TypedNode castValue(final TypedNode value, final Token castType) {
        return new TypedNode(Datatype.of(parserParent, castType), new Node(NodeType.CAST_VALUE, castType, castType.actualLine(), value.node));
    }

}
