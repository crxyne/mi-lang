package org.crayne.mu.runtime.parsing.parser;

import org.crayne.mu.lang.Datatype;
import org.crayne.mu.lang.Variable;
import org.crayne.mu.runtime.parsing.ast.Node;
import org.crayne.mu.runtime.parsing.ast.NodeType;
import org.crayne.mu.runtime.parsing.lexer.Token;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ValueParser {

    private int parsingPosition = -1;
    private Token currentToken;
    private final List<Token> expr;
    private final Parser parserParent;

    public record TypedNode(Datatype type, Node node) {}

    public ValueParser(@NotNull final List<Token> expr, @NotNull final Parser parserParent) {
        this.expr = expr;
        this.parserParent = parserParent;
    }

    private void nextPart() {
        ++parsingPosition;
        currentToken = parsingPosition >= 0 && parsingPosition < expr.size() ? expr.get(parsingPosition) : null;
    }

    private boolean eat(final String toEat) {
        if (currentToken != null && currentToken.token().equals(toEat)) {
            nextPart();
            return true;
        }
        return false;
    }

    public TypedNode parse() {
        nextPart();
        final TypedNode x = parseExpression();
        if (parsingPosition < expr.size()) {
            final Token val = expr.get(parsingPosition);
            parserError("Unexpected token '" + expr.get(parsingPosition).token() + "', couldn't parse expression", val);
        }
        return x;
    }

    protected void parserError(@NotNull final String message, @NotNull final String... quickFixes) {
        if (currentToken == null) parserError(message, parserParent.currentToken(), quickFixes);
        else parserError(message, currentToken, quickFixes);
    }

    private void parserError(@NotNull final String message, @NotNull final Token token, @NotNull final String... quickFixes) {
        parserParent.output.astHelperError(message, token.line(), token.column(), parserParent.stdlibFinishLine, parserParent.stdlib, quickFixes);
        parserParent.encounteredError = true;
    }

    private void parserError(@NotNull final String message, final int line, final int column, @NotNull final String... quickFixes) {
        parserParent.output.astHelperError(message, line, column, parserParent.stdlibFinishLine, parserParent.stdlib, quickFixes);
        parserParent.encounteredError = true;
    }

    private TypedNode evalTernaryOperator(final TypedNode x, final TypedNode y) {
        if (!eat(":")) {
            final Token val = y.node.value();
            parserError("Expected ':' after ternary expression", val.line(), val.column());
            return null;
        }
        final TypedNode z = parseExpression();
        final boolean areEqual = validVarset(z.type, y.type);
        if (!areEqual) {
            final Token val = z.node.value();
            parserError("'if' part of ternary operator should (atleast implicitly) have the same type as the 'else' part of the ternary operator",
                    val.line(), val.column(),
                    "'if' part is of type " + y.type.getName() + ", while 'else' part is " + z.type.name());
            return null;
        }
        return new TypedNode(y.type, new Node(NodeType.TERNARY_OPERATOR,
                new Node(NodeType.CONDITION, x.node),
                new Node(NodeType.TERNARY_OPERATOR_IF, y.node),
                new Node(NodeType.TERNARY_OPERATOR_ELSE, z.node)
        ));
    }

    private TypedNode evalExpression(final TypedNode x, final TypedNode y, final Token op) {
        if (op.token().equals("?")) return evalTernaryOperator(x, y);
        boolean operatorDefined = false;
        try {
            operatorDefined = x.type.operatorDefined(NodeType.of(op.token()), y.type);
        } catch (final Exception ignored) {}
        if (!operatorDefined) {
            if (x.type == y.type) {
                parserError("Operator '" + op.token() + "' is not defined for operand type " + x.type.getName(), op.line(), op.column());
                return null;
            }
            parserError("Operator '" + op.token() + "' is not defined for left operand " + x.type.getName() + " and right operand " + y.type.getName(), op.line(), op.column());
            return null;
        }
        return new TypedNode(Datatype.isComparator(op.token()) ? Datatype.BOOL : getHeavierType(x.type, y.type), new Node(NodeType.of(op.token()), x.node, y.node));
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

    private static final Map<Datatype, Integer> datatypeRanking = new HashMap<>() {{
        this.put(Datatype.ENUM, 1);
        this.put(Datatype.BOOL, 1);
        this.put(Datatype.STRING, 2);
        this.put(Datatype.DOUBLE, 3);
        this.put(Datatype.FLOAT, 4);
        this.put(Datatype.LONG, 5);
        this.put(Datatype.INT, 6);
        this.put(Datatype.CHAR, 6);
    }};

    public static Datatype getHeavierType(@NotNull final Datatype d1, @NotNull final Datatype d2) {
        final Integer r1 = datatypeRanking.get(d1);
        final Integer r2 = datatypeRanking.get(d2);
        if (r1 == null || r2 == null) return null;
        return r1 < r2 ? d1 : d2;
    }

    public static boolean validVarset(@NotNull final Datatype newType, @NotNull final Datatype oldType) {
        if (newType == oldType) return true;
        final Integer newRank = datatypeRanking.get(newType);
        final Integer oldRank = datatypeRanking.get(oldType);
        if (newRank == null || oldRank == null) return false;
        return newRank.equals(oldRank) || oldRank < newRank;
    }

    private TypedNode parseExpression() {
        return parseExpression(operatorPrecedence.size() - 1);
    }

    private TypedNode parseExpression(final int precendece) {
        TypedNode nodeX = precendece > 0 ? parseExpression(precendece - 1) : parseFactor();
        if (nodeX == null) {
            parserError("Unexpected parsing error");
            return new TypedNode(null, new Node(NodeType.VALUE));
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

    private TypedNode handleFactorPrefixes() {
        if (eat("+")) return parseFactor();
        if (eat("-")) {
            final TypedNode fact = parseFactor();
            return new TypedNode(fact.type, new Node(NodeType.NEGATE, fact.node));
        }
        if (eat("!")) {
            final TypedNode fact = parseFactor();
            return new TypedNode(fact.type, new Node(NodeType.BOOL_NOT, fact.node));
        }
        if (currentToken != null && NodeType.of(currentToken).isDatatype()) {
            final String datatype = currentToken.token();
            nextPart();
            return castValue(parseFactor(), Token.of(datatype));
        }
        return null;
    }

    private TypedNode parseFactor() {
        final TypedNode prefixed = handleFactorPrefixes();
        if (prefixed != null) return prefixed;

        if (currentToken == null) {
            parserError("Unexpected parsing error");
            return new TypedNode(null, new Node(NodeType.VALUE));
        }
        if (NodeType.of(currentToken.token()) == NodeType.IDENTIFIER) {
            final Variable findVar = parserParent.evaluator().findVariable(currentToken);
            if (findVar == null) return new TypedNode(null, new Node(NodeType.VALUE));
            if (!findVar.initialized()) {
                parserError("Variable '" + currentToken.token() + "' might not have been initialized yet", currentToken, "Set the value of the variable upon declaration");
                return new TypedNode(null, new Node(NodeType.VALUE));
            }

            final TypedNode result = new TypedNode(findVar.type(), new Node(NodeType.IDENTIFIER, currentToken));
            nextPart();
            return result;
        }
        final Token nextPart = parsingPosition + 1 < expr.size() ? expr.get(parsingPosition + 1) : null;
        if (nextPart != null && nextPart.token().equals("::") && parsingPosition + 2 < expr.size()) {
            final Token enumMember = expr.get(parsingPosition + 2);
            final Token enumName = currentToken;
            nextPart();
            nextPart();
            nextPart();
            return new TypedNode(Datatype.ENUM, new Node(NodeType.GET_ENUM_MEMBER,
                    new Node(NodeType.IDENTIFIER, enumName),
                    new Node(NodeType.MEMBER, enumMember)
            ));
        }
        if (eat("(")) {
            final TypedNode result = parseExpression();
            if (!eat(")")) parserError("Expected ')' after expression in parenthesis");
            return result;
        }
        if (parsingPosition + 1 < expr.size() && expr.get(parsingPosition + 1).token().equals("(")) {
            if (currentToken != null) {
                final String identifier = currentToken.token();
                final List<TypedNode> parsedArgs = parseArgs();
                if (parsedArgs == null || parserParent.encounteredError) return new TypedNode(null, new Node(NodeType.VALUE));

                return new TypedNode(null, new Node(NodeType.FUNCTION_CALL,
                        new Node(NodeType.IDENTIFIER, Token.of(identifier)),
                        new Node(NodeType.PARAMETERS, parsedArgs.stream().map(TypedNode::node).toList())
                ));
            }
        }
        final Token result = currentToken;
        final NodeType nodeType = result == null ? null : NodeType.of(result.token());
        final Datatype datatype = nodeType == null ? null : nodeType.getAsDataType();
        if (datatype == null) return new TypedNode(null, new Node(NodeType.VALUE));
        nextPart();
        return new TypedNode(datatype, new Node(nodeType, result));
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
            parserError("Expected ')' after arguments of function call");
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
        return new TypedNode(Datatype.of(castType), new Node(NodeType.CAST_VALUE, castType, value.node));
    }

}
