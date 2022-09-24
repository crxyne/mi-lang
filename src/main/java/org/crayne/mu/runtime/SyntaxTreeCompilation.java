package org.crayne.mu.runtime;

import org.crayne.mu.bytecode.common.ByteCodeInstruction;
import org.crayne.mu.bytecode.common.errorhandler.Traceback;
import org.crayne.mu.bytecode.common.errorhandler.TracebackElement;
import org.crayne.mu.bytecode.writer.ByteCodeCompiler;
import org.crayne.mu.log.LogHandler;
import org.crayne.mu.log.MessageHandler;
import org.crayne.mu.parsing.ast.Node;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class SyntaxTreeCompilation {
    private final Node parentNode;
    private final MessageHandler out;
    private final List<String> code;
    private final Traceback traceback;
    private final int stdlibFinishLine;
    private boolean error;

    public SyntaxTreeCompilation(@NotNull final Node parentNode, @NotNull final MessageHandler out, @NotNull final String code, final int stdlibFinishLine) {
        this.parentNode = parentNode;
        this.out = out;
        this.code = Arrays.stream(code.split("\n")).toList();
        this.traceback = new Traceback();
        this.stdlibFinishLine = stdlibFinishLine;
    }

    public TracebackElement newTracebackElement(final int line) {
        return new TracebackElement(this, line);
    }

    public void traceback(final int... lines) {
        for (final int line : lines) traceback.add(newTracebackElement(line));
    }

    public int getStdlibFinishLine() {
        return stdlibFinishLine;
    }

    public boolean error() {
        return error;
    }

    public List<String> getCode() {
        return code;
    }

    public String getLine(final int line) {
        return line < code.size() && line >= 0 ? code.get(line) : null;
    }

    public Node getAST() {
        return parentNode;
    }

    public void compile(@NotNull final File file) throws IOException {
        final ByteCodeCompiler compiler = new ByteCodeCompiler(out, this);
        final List<ByteCodeInstruction> compiled = compiler.compile();
        ByteCodeCompiler.compileToFile(compiled, file);
    }

    public void error(@NotNull final String msg, @NotNull final String... quickFixes) {
        out
                .log("Unexpected compiler error: " + msg, LogHandler.Level.FATAL)
                .possibleSolutions(quickFixes)
                .print();
        out.errorMsg(traceback.toString());
        error = true;
    }

}
