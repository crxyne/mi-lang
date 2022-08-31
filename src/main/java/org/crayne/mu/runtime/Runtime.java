package org.crayne.mu.runtime;

import org.crayne.mu.log.MessageHandler;
import org.crayne.mu.runtime.parsing.ast.Node;
import org.crayne.mu.runtime.parsing.lexer.Token;
import org.crayne.mu.runtime.parsing.lexer.Tokenizer;
import org.crayne.mu.runtime.parsing.parser.Parser;
import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

public class Runtime {

    private static final List<String> multiTokens = Arrays.asList("<<", ">>", "->", "&&", "||", "==", "!=", "::", "<=", ">=", "++", "--", "+=", "*=", "/=", "-=", "<<=", ">>=");
    private final MessageHandler out;

    public Runtime(@NotNull final PrintStream out, final boolean enableColor) {
        this.out = new MessageHandler(out, enableColor);
    }

    public MessageHandler messageHandler() {
        return out;
    }

    public Node parse(@NotNull final String code) {
        this.out.setProgram(code);

        final Tokenizer tokenizer = new Tokenizer(out, multiTokens);
        final List<Token> tokenList = tokenizer.tokenize(code);
        if (tokenizer.encounteredError()) return null;

        final Parser parser = new Parser(out, tokenList, tokenizer.stdlibFinishLine());
        return parser.parse();
    }

}
