package org.crayne.mi;

import org.crayne.mi.bytecode.common.ByteCodeInstruction;
import org.crayne.mi.bytecode.writer.ByteCodeCompiler;
import org.crayne.mi.log.MessageHandler;
import org.crayne.mi.parsing.ast.Node;
import org.crayne.mi.parsing.lexer.Token;
import org.crayne.mi.parsing.lexer.Tokenizer;
import org.crayne.mi.parsing.parser.Parser;
import org.crayne.mi.util.SyntaxTree;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Mi {

    private static final List<String> multiTokens = Arrays.asList("<<", ">>", "->", "&&", "||", "==", "!=", "::", "<=", ">=", "++", "--", "+=", "*=", "/=", "-=", "%=", "<<=", ">>=", "&=", "|=");
    private final MessageHandler out;

    public Mi(@NotNull final PrintStream out, final boolean enableColor) {
        this.out = new MessageHandler(out, enableColor);
    }

    public MessageHandler messageHandler() {
        return out;
    }

    private Optional<SyntaxTree> parse(@NotNull final String stdlib, @NotNull final String code, final File inputFile) {
        final String actualCode = stdlib + code + "\n";
        this.out.setProgram(actualCode);

        final Tokenizer tokenizer = new Tokenizer(out, multiTokens);
        final List<Token> tokenList = tokenizer.tokenize(actualCode);
        if (tokenizer.encounteredError()) return Optional.empty();

        final Parser parser = new Parser(out, tokenizer.stdlibFinishLine());
        final Node node = parser.parse(tokenList, actualCode);
        return node == null ? Optional.empty() : Optional.of(new SyntaxTree(node, out, actualCode, tokenizer.stdlibFinishLine(), inputFile));
    }

    public List<ByteCodeInstruction> compile(@NotNull final String stdlib, @NotNull final String code) {
        final Optional<SyntaxTree> tree = parse(stdlib, code, null);
        if (tree.isEmpty()) return new ArrayList<>();
        final ByteCodeCompiler compiler = new ByteCodeCompiler(tree.get());
        final List<ByteCodeInstruction> result = compiler.compile();
        //System.out.println(compiler);
        return result;
    }

    public void compile(@NotNull final String stdlib, @NotNull final String code, @NotNull final File file, @NotNull final File inputFile) {
        final Optional<SyntaxTree> tree = parse(stdlib, code, inputFile);
        if (tree.isPresent()) {
            try {
                tree.get().compile(file);
            } catch (Throwable e) {
                tree.get().error("Error encountered when trying to compile: " + e.getClass().getSimpleName() + " " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

}
