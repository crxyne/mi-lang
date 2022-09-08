package org.crayne.mu.parsing.parser;

import org.crayne.mu.lang.Enum;
import org.crayne.mu.lang.*;
import org.crayne.mu.parsing.ast.Node;
import org.crayne.mu.parsing.ast.NodeType;
import org.crayne.mu.parsing.lexer.Token;
import org.crayne.mu.parsing.parser.scope.FunctionScope;
import org.crayne.mu.parsing.parser.scope.Scope;
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
            parserParent.parserError("Unexpected token '" + expr.get(parsingPosition).token() + "', couldn't parse expression", val);
        }
        return x;
    }

    private TypedNode evalTernaryOperator(final TypedNode x, final TypedNode y) {
        if (!eat(":")) {
            final Token val = y.node.value();
            parserParent.parserError("Expected ':' after ternary expression", val);
            return new TypedNode(null, new Node(NodeType.VALUE));
        }
        final TypedNode z = parseExpression();
        final boolean areEqual = z.type.equals(y.type);
        if (!areEqual) {
            final Token val = z.node.value();
            parserParent.parserError("'if' part of ternary operator should (atleast implicitly) have the same type as the 'else' part of the ternary operator", val,
                    "'if' part is of type " + y.type.getName() + ", while 'else' part is " + z.type.getName());
            return new TypedNode(null, new Node(NodeType.VALUE));
        }
        if (x.type.notPrimitive() || !x.type.getPrimitive().equals(PrimitiveDatatype.BOOL)) {
            parserParent.parserError("Ternary operator condition should be of type 'bool' but is instead '" + x.type.getName() + "'", x.node.value());
            return new TypedNode(null, new Node(NodeType.VALUE));
        }
        return new TypedNode(y.type, new Node(NodeType.TERNARY_OPERATOR,
                new Node(NodeType.CONDITION, x.node),
                new Node(NodeType.TERNARY_OPERATOR_IF, y.node),
                new Node(NodeType.TERNARY_OPERATOR_ELSE, z.node)
        ));
    }

    private TypedNode evalExpression(final TypedNode x, final TypedNode y, final Token op) {
        if (parserParent.encounteredError) return null;
        if (op.token().equals("?")) return evalTernaryOperator(x, y);
        boolean operatorDefined = false;
        try {
            operatorDefined = x.type.operatorDefined(NodeType.of(op.token()), y.type);
        } catch (final Exception ignored) {}
        if (!operatorDefined) {
            if (x.type == y.type) {
                parserParent.parserError("Operator '" + op.token() + "' is not defined for operand type " + x.type.getName(), op);
                return new TypedNode(null, new Node(NodeType.VALUE));
            }
            parserParent.parserError("Operator '" + op.token() + "' is not defined for left operand " + x.type.getName() + " and right operand " + y.type.getName(), op);
            return new TypedNode(null, new Node(NodeType.VALUE));
        }
        return new TypedNode(PrimitiveDatatype.isComparator(op.token()) ? Datatype.BOOL : x.type.heavier(y.type), new Node(NodeType.of(op.token()), x.node, y.node));
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
            parserParent.parserError("Unexpected parsing error");
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
        // ****prefixed**** ++ and -- dont de/in-crement
        final Token prev = currentToken;
        if (eat("+") || eat("--") || eat("++")) {
            final TypedNode fact = parseFactor();
            if (parserParent.encounteredError) return new TypedNode(null, new Node(NodeType.VALUE));
            if (!fact.type.operatorDefined(NodeType.ADD, Datatype.DOUBLE)) {
                parserParent.parserError("Cannot use '" + prev.token() + "' operator on type '" + fact.type + "'", prev);
                return new TypedNode(null, new Node(NodeType.VALUE));
            }
            return fact;
        }
        if (eat("-")) {
            final TypedNode fact = parseFactor();
            if (parserParent.encounteredError) return new TypedNode(null, new Node(NodeType.VALUE));
            if (!fact.type.operatorDefined(NodeType.ADD, Datatype.DOUBLE)) {
                parserParent.parserError("Cannot use '" + prev.token() + "' operator on type '" + fact.type + "'", prev);
                return new TypedNode(null, new Node(NodeType.VALUE));
            }
            return new TypedNode(fact.type, new Node(NodeType.NEGATE, fact.node));
        }
        if (eat("!")) {
            final TypedNode fact = parseFactor();
            if (parserParent.encounteredError) return new TypedNode(null, new Node(NodeType.VALUE));
            if (fact.type != Datatype.BOOL) {
                parserParent.parserError("Cannot use '" + prev.token() + "' operator on type '" + fact.type + "'", prev);
                return new TypedNode(null, new Node(NodeType.VALUE));
            }
            return new TypedNode(fact.type, new Node(NodeType.BOOL_NOT, fact.node));
        }
        if (currentToken != null && NodeType.of(currentToken).isDatatype()) {
            final String datatype = currentToken.token();
            if (parsingPosition + 1 >= expr.size()) {
                parserParent.parserError("Expected value after '" + datatype + "' to cast", currentToken, true);
                return new TypedNode(null, new Node(NodeType.VALUE));
            }
            nextPart();
            return castValue(parseFactor(), Token.of(datatype));
        }
        return null;
    }

    private TypedNode parseFactor() {
        final TypedNode prefixed = handleFactorPrefixes();
        if (prefixed != null) return prefixed;

        if (currentToken == null) {
            parserParent.parserError("Unexpected parsing error");
            return new TypedNode(null, new Node(NodeType.VALUE));
        }
        final Token nextPart = parsingPosition + 1 < expr.size() ? expr.get(parsingPosition + 1) : null;

        if (NodeType.of(currentToken) == NodeType.IDENTIFIER && nextPart != null && nextPart.token().equals("::") && parsingPosition + 2 < expr.size()) {
            final Token enumMember = expr.get(parsingPosition + 2);
            if (NodeType.of(enumMember) == NodeType.IDENTIFIER) {
                final Token enumName = currentToken;
                final String enumMemberStr = enumMember.token();
                final String enumNameStr = enumName.token();

                final Optional<Scope> scope = parserParent.scope();
                final FunctionScope functionScope = scope.isPresent() && scope.get() instanceof FunctionScope ? (FunctionScope) scope.get() : null;

                final Enum foundEnum = Datatype.findEnumByIdentifier(parserParent, functionScope != null ? functionScope.using() : Collections.emptyList(), enumName, true);

                if (foundEnum == null) return new TypedNode(null, new Node(NodeType.VALUE));
                if (!foundEnum.members().contains(enumMemberStr)) {
                    parserParent.parserError("Enum '" + enumNameStr + "' does not have member '" + enumMemberStr + "'", enumMember);
                    return new TypedNode(null, new Node(NodeType.VALUE));
                }
                nextPart();
                nextPart();
                nextPart();
                final Datatype datatype = new Datatype(parserParent, enumName);

                return new TypedNode(datatype, new Node(NodeType.GET_ENUM_MEMBER,
                        new Node(NodeType.IDENTIFIER, foundEnum.asIdentifierToken(enumName)),
                        new Node(NodeType.MEMBER, enumMember)
                ));
            } else {
                parserParent.parserError("Unexpected token '::'", nextPart);
                return new TypedNode(null, new Node(NodeType.VALUE));
            }
        }
        if (NodeType.of(currentToken.token()) == NodeType.IDENTIFIER && (nextPart == null || !nextPart.token().equals("("))) {
            final Optional<Variable> findVar = parserParent.evaluator().findVariable(currentToken, true);
            if (findVar.isEmpty()) return new TypedNode(null, new Node(NodeType.VALUE));

            final Token identifier = currentToken;
            if (nextPart != null) {
                final EqualOperation eq = EqualOperation.of(nextPart.token());
                if (eq != null) {
                    nextPart();
                    nextPart();

                    final TypedNode val = nextPart.token().equals("++") || nextPart.token().equals("--")
                            ? new TypedNode(Datatype.INT, new Node(NodeType.INTEGER_NUM_LITERAL, Token.of("1")))
                            : parseExpression();

                    return new TypedNode(findVar.get().type(), parserParent.evaluator().evalVariableChange(identifier, val, nextPart, findVar.get()));
                }
            }

            if (!findVar.get().initialized()) {
                parserParent.parserError("Variable '" + identifier.token() + "' might not have been initialized yet", identifier, "Set the value of the variable upon declaration");
                return new TypedNode(null, new Node(NodeType.VALUE));
            }
            final TypedNode result = new TypedNode(findVar.get().type(), new Node(NodeType.IDENTIFIER, identifier));
            nextPart();
            return result;
        }
        if (eat("(")) {
            final TypedNode result = parseExpression();
            if (!eat(")")) parserParent.parserError("Expected ')' after expression in parenthesis", currentToken);
            return result;
        }
        if (nextPart != null && nextPart.token().equals("(")) {
            if (currentToken != null) {
                final Token identifier = currentToken;
                final List<TypedNode> parsedArgs = parseArgs();
                if (parsedArgs == null || parserParent.encounteredError) return new TypedNode(null, new Node(NodeType.VALUE));

                final FunctionDefinition def = parserParent.evaluator().checkValidFunctionCall(identifier, parsedArgs, true, true);
                if (def == null) return new TypedNode(null, new Node(NodeType.VALUE));

                final Datatype retType = def.returnType();
                if (retType == Datatype.VOID) {
                    parserParent.parserError("Usage of void function as a value in expression", identifier);
                    return new TypedNode(null, new Node(NodeType.VALUE));
                }

                return new TypedNode(retType, new Node(NodeType.FUNCTION_CALL,
                        new Node(NodeType.IDENTIFIER, def.asIdentifierToken(identifier)),
                        new Node(NodeType.PARAMETERS, parsedArgs.stream().map(TypedNode::node).toList())
                ));
            }
        }
        final Token result = currentToken;
        final NodeType nodeType = result == null ? null : NodeType.of(result.token());
        final Datatype datatype = nodeType == null ? null : NodeType.getAsDataType(parserParent, new Node(nodeType, result));
        if (datatype == null) return new TypedNode(null, new Node(NodeType.VALUE));
        if (datatype == Datatype.NULL && !parserParent.stdlib && parserParent.skimming) {
            parserParent.parserError("Unexpected token 'null'", "Only the standard library may use 'null' as a raw value, use Optional<T> to use nullable types");
            return new TypedNode(null, new Node(NodeType.VALUE));
        }
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
        return new TypedNode(Datatype.of(parserParent, castType), new Node(NodeType.CAST_VALUE, castType, value.node));
    }

}
