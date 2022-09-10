package org.crayne.mu.runtime;

import org.crayne.mu.log.MessageHandler;
import org.crayne.mu.parsing.lexer.Token;
import org.crayne.mu.parsing.lexer.Tokenizer;
import org.crayne.mu.parsing.parser.Parser;
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

    public Optional<SyntaxTreeExecution> parse(@NotNull final String stdlib, @NotNull final String code) {
        return parse(stdlib, code, true);
    }

    private Optional<SyntaxTreeExecution> parse(@NotNull final String stdlib, @NotNull final String code, final boolean printStackTraces) {
        final String actualCode = stdlib + code + "\n";
        this.out.setProgram(actualCode);

        final Tokenizer tokenizer = new Tokenizer(out, multiTokens);
        final List<Token> tokenList = tokenizer.tokenize(actualCode);
        if (tokenizer.encounteredError()) return Optional.empty();

        final Parser parser = new Parser(out, tokenList, tokenizer.stdlibFinishLine(), actualCode, printStackTraces);
        return Optional.ofNullable(parser.parse());
    }

    public void execute(@NotNull final String stdlib, @NotNull final String code, final boolean printStackTraces, @NotNull final String mainFunc, @NotNull final Object... args) throws Throwable {
        final Optional<SyntaxTreeExecution> tree = parse(stdlib, code, printStackTraces);
        if (tree.isPresent()) tree.get().execute(mainFunc, List.of(args));
    }

}
