package org.crayne.mi.parsing.lexer;

import org.jetbrains.annotations.NotNull;

public class Token {

    private final String token;
    private final int line;
    private final int actualLine;
    private final int column;

    public Token(@NotNull final String token, final int actualLine, final int line, final int column) {
        this.token = token;
        this.line = line;
        this.column = column;
        this.actualLine = actualLine;
    }

    public Token(@NotNull final String token, final int line, final int column) {
        this.token = token;
        this.line = line;
        this.actualLine = line;
        this.column = column;
    }

    public Token(@NotNull final String token) {
        this.token = token;
        this.line = -1;
        this.actualLine = -1;
        this.column = -1;
    }

    public static Token of(@NotNull final String token, final int line, final int column) {
        return new Token(token, line, column);
    }

    public static Token of(@NotNull final String token) {
        return new Token(token);
    }

    public String token() {
        return token;
    }

    public int line() {
        return line;
    }

    public int actualLine() {
        return actualLine;
    }

    public int column() {
        return column;
    }

    public boolean equals(@NotNull final Token other) {
        return token.equals(other.token);
    }

    @Override
    public String toString() {
        return "Token{" +
                "token='" + token + '\'' +
                ", line=" + line +
                ", column=" + column +
                '}';
    }
}
