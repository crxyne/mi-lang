package org.crayne.mi.parsing.parser;

import org.crayne.mi.log.MessageHandler;
import org.crayne.mi.parsing.ast.Node;
import org.crayne.mi.parsing.ast.NodeType;
import org.crayne.mi.parsing.lexer.Token;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Parser {

    private final ASTGenerator astGenerator;
    private boolean encounteredError;
    private final int stdlibFinishLine;
    private boolean stdlib = true;
    private final MessageHandler output;

    private Node currentNode = new Node(NodeType.PARENT, -1);

    public Parser(@NotNull final MessageHandler output, final int stdlibFinishLine) {
        this.astGenerator = new ASTGenerator(this);
        this.stdlibFinishLine = stdlibFinishLine;
        this.output = output;
    }

    public boolean encounteredError() {
        return encounteredError;
    }

    public int stdlibFinishLine() {
        return stdlibFinishLine;
    }

    public void parserError(@NotNull final String message, @NotNull final Token token, @NotNull final String... quickFixes) {
        parserError(message, token, false, quickFixes);
    }

    public void parserError(@NotNull final String message, @NotNull final Token token, final boolean skipToEndOfToken, @NotNull final String... quickFixes) {
        if (encounteredError) return;
        if (!stdlib) {
            output.astHelperError(message, token.line(), token.column() + (skipToEndOfToken ? token.token().length() : 0), stdlibFinishLine, false, quickFixes);
        } else {
            output.astHelperError(message, token.actualLine(), token.column() + (skipToEndOfToken ? token.token().length() : 0), 1, true, quickFixes);
        }
        encounteredError = true;
    }

    public void parserWarning(@NotNull final String message, @NotNull final Token token, @NotNull final String... quickFixes) {
        if (!stdlib) {
            output.astHelperWarning(message, token.line(), token.column(), stdlibFinishLine, false, quickFixes);
        } else {
            output.astHelperWarning(message, token.actualLine(), token.column(), 1, true, quickFixes);
        }
    }

    protected void reachedStdlibFinish(final boolean b) {
        stdlib = !b;
    }

    private List<List<Token>> extractStatements(@NotNull final List<Token> tokenList) {
        final List<List<Token>> statements = new ArrayList<>();
        List<Token> current = new ArrayList<>();
        int paren = 0;
        boolean parsingValue = false; // allow for lambdas, e.g. fn test = () -> {};

        for (@NotNull final Token tok : tokenList) {
            final NodeType tokType = NodeType.of(tok);
            if (tokType == NodeType.RBRACE) {
                if (paren == 0 && !parsingValue) {
                    if (!current.isEmpty()) statements.add(current);
                    statements.add(Collections.singletonList(tok));
                    current = new ArrayList<>();
                    continue;
                }
            }
            current.add(tok);
            switch (tokType) {
                case SEMI -> {
                    if (paren == 0) {
                        statements.add(current);
                        current = new ArrayList<>();
                    } else {
                        parserError("Expected ')'", tok);
                        return new ArrayList<>();
                    }
                    parsingValue = false;
                }
                case SET, SET_ADD, SET_AND, SET_OR, SET_DIV, SET_LSHIFT,
                        SET_MOD, SET_MULT, SET_RSHIFT, SET_SUB, SET_XOR -> {
                    if (NodeType.of(current.subList(ASTGenerator.modifiers(current).size(), current.size()).get(0)).isDatatype())
                        parsingValue = true;
                }
                case LPAREN -> paren++;
                case RPAREN -> {
                    if (paren == 0) {
                        parserError("Unexpected token ')'", tok);
                        return new ArrayList<>();
                    }
                    paren--;
                }
                case LBRACE -> {
                    if (!parsingValue) {
                        statements.add(current);
                        current = new ArrayList<>();
                    }
                }
            }
        }
        if (!current.isEmpty()) statements.add(current);
        return statements;
    }

    private Node lastScopedNode;

    public Node parse(@NotNull final List<Token> tokenList, @NotNull final String code) {
        int openedBrace = 0;
        output.setProgram(code);
        if (stdlibFinishLine == -1) {
            parserError("Cannot find a STANDARDLIB_MI_FINISH_CODE statement anywhere", tokenList.get(tokenList.size() - 1),
                    "Add a STANDARDLIB_MI_FINISH_CODE instruction after the standard library");
            return null;
        }
        final List<List<Token>> statements = extractStatements(tokenList).stream().filter(l -> !l.isEmpty()).toList();
        int i = 0;

        for (@NotNull final List<Token> statement : statements) {
            final Token lastToken = statement.get(statement.size() - 1);
            final NodeType last = NodeType.of(lastToken);
            if (encounteredError) return null;
            switch (last) {
                case LBRACE -> {
                    final Node sm = astGenerator.evalScoped(statement);
                    if (sm == null) {
                        parserError("Not a statement.", lastToken);
                        return null;
                    }
                    final Node scope = new Node(currentNode, NodeType.SCOPE, lastToken.actualLine(), lastToken);

                    if (lastScopedNode != null && sm.type() == NodeType.ELSE_STATEMENT) {
                        if (lastScopedNode.type() != NodeType.IF_STATEMENT) {
                            parserError("Not a statement.", lastToken);
                            return null;
                        }
                        sm.child(0).addChildren(scope);
                        lastScopedNode.addChildren(sm);
                        lastScopedNode = sm.child(0);
                    } else {
                        sm.addChildren(scope);
                        currentNode.addChildren(sm);
                        lastScopedNode = sm;
                    }

                    currentNode = scope;
                    openedBrace++;
                }
                case SEMI -> {
                    final Node sm = astGenerator.evalUnscoped(statement);
                    if (sm == null) {
                        parserError("Not a statement.", lastToken);
                        return null;
                    }
                    // put the unscoped while statement into the actual do statement if there is one
                    if (lastScopedNode != null) {
                        switch (sm.type()) {
                            case WHILE_STATEMENT_UNSCOPED -> {
                                if (lastScopedNode.type() != NodeType.DO_STATEMENT) {
                                    parserError("Not a statement.", lastToken);
                                    return null;
                                }
                                lastScopedNode.addChildren(sm);
                                continue;
                            }
                            case ELSE_STATEMENT -> {
                                if (lastScopedNode.type() != NodeType.IF_STATEMENT) {
                                    parserError("Not a statement.", lastToken);
                                    return null;
                                }
                                lastScopedNode.addChildren(sm);
                                continue;
                            }
                        }
                    }
                    currentNode.addChildren(sm);
                }
                case RBRACE -> {
                    final List<Token> previousStatement = i <= 0 ? new ArrayList<>() : statements.get(i - 1);
                    final NodeType lastTokenPrevStatement = previousStatement.isEmpty() ? null : NodeType.of(previousStatement.get(previousStatement.size() - 1));
                    if (!previousStatement.isEmpty() && lastTokenPrevStatement != NodeType.SEMI && lastTokenPrevStatement != NodeType.LBRACE && lastTokenPrevStatement != NodeType.RBRACE) {
                        final Node sm = astGenerator.evalEnumMembers(previousStatement);
                        if (sm == null) {
                            parserError("Not a statement.", lastToken);
                            return null;
                        }
                        currentNode.addChildren(sm);
                    }
                    currentNode = currentNode.parent();
                    openedBrace--;
                }
            }
            i++;
        }
        if (openedBrace != 0) {
            final List<Token> lastStatement = statements.get(statements.size() - 1);
            final Token lastToken = lastStatement.get(lastStatement.size() - 1);
            parserError("Missing '}'", lastToken, "Add the missing '}' where it belongs. Every scope {} must be complete in order to compile the program.");
        }
        final ASTRefiner checkErrs = new ASTRefiner(this);
        return checkErrs.checkAST(currentNode);
    }

    public Node currentNode() {
        return currentNode;
    }

    public Token getAndExpect(@NotNull final Collection<Token> given, final int at, @NotNull final NodeType... toBe) {
        if (at < 0 || at >= given.size()) return null;
        final Token tok = given.stream().toList().get(at);
        return Arrays.stream(toBe).toList().contains(NodeType.of(tok)) ? tok : null;
    }

    public Token getAny(@NotNull final Collection<Token> given, final int at) {
        if (at < 0 || at >= given.size()) return null;
        return given.stream().toList().get(at);
    }

    @SafeVarargs
    public static <T> boolean anyNull(final T... objs) {
        return Arrays.stream(objs).anyMatch(Objects::isNull);
    }


}
