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

    public void parserError(@NotNull final String message, @NotNull final Token token, @NotNull final String... quickFixes) {
        parserError(message, token, false, quickFixes);
    }

    public void parserError(@NotNull final String message, @NotNull final Token token, final boolean skipToEndOfToken, @NotNull final String... quickFixes) {
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

        for (@NotNull final Token tok : tokenList) {
            final NodeType tokType = NodeType.of(tok);
            if (tokType == NodeType.RBRACE) {
                if (paren == 0) {
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
                    }
                }
                case LPAREN -> paren++;
                case RPAREN -> paren--;
                case LBRACE -> {
                    statements.add(current);
                    current = new ArrayList<>();
                }
            }
        }
        if (!current.isEmpty()) statements.add(current);
        return statements;
    }

    public Node parse(@NotNull final List<Token> tokenList, @NotNull final String code) {
        output.setProgram(code);
        if (stdlibFinishLine == -1) {
            parserError("Cannot find a STANDARDLIB_MI_FINISH_CODE statement anywhere", tokenList.get(tokenList.size() - 1),
                    "Add a STANDARDLIB_MI_FINISH_CODE instruction after the standard library");
            return null;
        }
        final List<List<Token>> statements = extractStatements(tokenList).stream().filter(l -> !l.isEmpty()).toList();

        for (@NotNull final List<Token> statement : statements) {
            final Token lastToken = statement.get(statement.size() - 1);
            final NodeType last = NodeType.of(lastToken);
            if (encounteredError) return null;
            switch (last) {
                case LBRACE -> {
                    final Node sm = astGenerator.evalScoped(statement);
                    if (sm == null) {
                        System.out.println(lastToken);
                        parserError("Not a statement.", lastToken);
                        return null;
                    }

                    final Node scope = new Node(currentNode, NodeType.SCOPE, lastToken.actualLine());
                    sm.addChildren(scope);
                    currentNode.addChildren(sm);
                    currentNode = scope;
                }
                case SEMI -> {
                    final Node sm = astGenerator.evalUnscoped(statement);
                    if (sm == null) {
                        parserError("Not a statement.", lastToken);
                        return null;
                    }

                    currentNode.addChildren(sm);
                }
                case RBRACE -> currentNode = currentNode.parent();
            }
        }
        final ASTErrorChecker checkErrs = new ASTErrorChecker(this);
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
