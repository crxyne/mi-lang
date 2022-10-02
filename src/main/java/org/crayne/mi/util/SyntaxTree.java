package org.crayne.mi.util;

import org.crayne.mi.bytecode.common.ByteCodeInstruction;
import org.crayne.mi.util.errorhandler.Traceback;
import org.crayne.mi.util.errorhandler.TracebackElement;
import org.crayne.mi.bytecode.writer.ByteCodeCompiler;
import org.crayne.mi.log.LogHandler;
import org.crayne.mi.log.MessageHandler;
import org.crayne.mi.parsing.ast.Node;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class SyntaxTree {
    private final Node parentNode;
    private final File inputFile;
    private final MessageHandler out;
    private final List<String> code;
    private final Traceback traceback;
    private final int stdlibFinishLine;
    private boolean error;

    public SyntaxTree(@NotNull final Node parentNode, @NotNull final MessageHandler out, @NotNull final String code, final int stdlibFinishLine, final File inputFile) {
        this.parentNode = parentNode;
        this.out = out;
        this.code = Arrays.stream(code.split("\n")).toList();
        this.traceback = new Traceback();
        this.stdlibFinishLine = stdlibFinishLine;
        this.inputFile = inputFile;
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

    public void compile(@NotNull final File file, @NotNull final String mainFunctionModule, @NotNull final String mainFunction) throws IOException {
        final ByteCodeCompiler compiler = new ByteCodeCompiler(this, mainFunctionModule, mainFunction);
        out.infoMsg("Compiling " + inputFile.getName() + "...");
        final List<ByteCodeInstruction> compiled = compiler.compile();
        ByteCodeCompiler.compileToFile(compiled, file);
        if (compiled.isEmpty()) {
            out.errorMsg("Could not compile " + inputFile.getName() + ". See error output above.");
            return;
        }
        out.infoMsg("Completed. See output file here: " + file.getAbsolutePath());
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
