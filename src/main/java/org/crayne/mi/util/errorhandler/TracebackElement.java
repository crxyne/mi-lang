package org.crayne.mi.util.errorhandler;

import org.crayne.mi.bytecode.reader.ByteCodeInterpreter;
import org.crayne.mi.bytecode.writer.ByteCodeCompiler;
import org.crayne.mi.util.SyntaxTree;
import org.jetbrains.annotations.NotNull;

public class TracebackElement {

    final String lineStr;
    final int line;
    final int lineNoStandardLib;

    public TracebackElement(@NotNull final SyntaxTree tree, final int line) {
        this.line = line;
        this.lineNoStandardLib = line - tree.getStdlibFinishLine();
        final String getline = tree.getLine(line);
        this.lineStr = getline == null ? null : "\"" + getline.trim() + "\"";
    }

    public TracebackElement(@NotNull final ByteCodeCompiler compiler, final int line) {
        this.line = line;
        this.lineNoStandardLib = line - compiler.getStdlibFinishLine();
        final String getline = compiler.getLine(line);
        this.lineStr = getline == null ? null : "\"" + getline.trim() + "\"";
    }

    public TracebackElement(@NotNull final ByteCodeInterpreter runtime, final int line) {
        this.line = line;
        this.lineNoStandardLib = line - runtime.getStdlibFinishLine();
        this.lineStr = null;
    }

    public String toString() {
        if (lineStr == null) return lineNoStandardLib <= 0 ? "standard library line " + line : "line " + lineNoStandardLib;
        if (lineNoStandardLib <= 0) return lineStr.replace("STANDARDLIB_MI_FINISH_CODE;", "") + "  :   @ line " + line;
        return lineStr + "  :   @ line " + lineNoStandardLib;
    }

}
