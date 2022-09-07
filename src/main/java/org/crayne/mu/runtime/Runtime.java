package org.crayne.mu.runtime;

import org.crayne.mu.log.MessageHandler;
import org.crayne.mu.runtime.parsing.ast.SyntaxTree;
import org.crayne.mu.runtime.parsing.lexer.Token;
import org.crayne.mu.runtime.parsing.lexer.Tokenizer;
import org.crayne.mu.runtime.parsing.parser.Parser;
import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Runtime {

    private static final List<String> multiTokens = Arrays.asList("<<", ">>", "->", "&&", "||", "==", "!=", "::", "<=", ">=", "++", "--", "+=", "*=", "/=", "-=", "%=", "<<=", ">>=", "&=", "|=");
    private final MessageHandler out;

    public Runtime(@NotNull final PrintStream out, final boolean enableColor) {
        this.out = new MessageHandler(out, enableColor);
    }

    public MessageHandler messageHandler() {
        return out;
    }

    public Optional<SyntaxTree> parse(@NotNull final String code) {
        this.out.setProgram(code);

        final Tokenizer tokenizer = new Tokenizer(out, multiTokens);
        final List<Token> tokenList = tokenizer.tokenize(code);
        if (tokenizer.encounteredError()) return Optional.empty();

        final Parser parser = new Parser(out, tokenList, tokenizer.stdlibFinishLine());
        return Optional.ofNullable(parser.parse());
    }

    public void execute(@NotNull final String code) {
        parse(code).ifPresent(SyntaxTree::execute);
    }

}
