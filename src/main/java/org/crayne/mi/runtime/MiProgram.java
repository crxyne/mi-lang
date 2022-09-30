package org.crayne.mi.runtime;

import org.crayne.mi.log.MessageHandler;
import org.crayne.mi.parsing.lexer.Token;
import org.crayne.mi.parsing.lexer.Tokenizer;
import org.crayne.mi.parsing.parser.Parser;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class MiProgram {

    private static final List<String> multiTokens = Arrays.asList("<<", ">>", "->", "&&", "||", "==", "!=", "::", "<=", ">=", "++", "--", "+=", "*=", "/=", "-=", "%=", "<<=", ">>=", "&=", "|=");
    private final MessageHandler out;

    public MiProgram(@NotNull final PrintStream out, final boolean enableColor) {
        this.out = new MessageHandler(out, enableColor);
    }

    public MessageHandler messageHandler() {
        return out;
    }

    private Optional<SyntaxTreeCompilation> parse(@NotNull final String stdlib, @NotNull final String code, @NotNull final File inputFile) {
        final String actualCode = stdlib + code + "\n";
        this.out.setProgram(actualCode);

        final Tokenizer tokenizer = new Tokenizer(out, multiTokens);
        final List<Token> tokenList = tokenizer.tokenize(actualCode);
        if (tokenizer.encounteredError()) return Optional.empty();

        final Parser parser = new Parser(out, tokenList, tokenizer.stdlibFinishLine(), actualCode, inputFile);
        return Optional.ofNullable(parser.parse());
    }

    public void compile(@NotNull final String stdlib, @NotNull final String code, @NotNull final File file, @NotNull final File inputFile, @NotNull final String mainFunctionModule, @NotNull final String mainFunction) {
        final Optional<SyntaxTreeCompilation> tree = parse(stdlib, code, inputFile);
        if (tree.isPresent()) {
            try {
                tree.get().compile(file, mainFunctionModule, mainFunction);
            } catch (Throwable e) {
                tree.get().error("Error encountered when trying to compile: " + e.getClass().getSimpleName() + " " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

}
