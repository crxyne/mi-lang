package org.crayne.mu.runtime.util.errorhandler;

import org.crayne.mu.runtime.SyntaxTree;
import org.jetbrains.annotations.NotNull;

public class TracebackElement {

    final String lineStr;
    final int line;
    final int lineNoStandardLib;

    public TracebackElement(@NotNull final SyntaxTree tree, final int line) {
        this.line = line;
        this.lineNoStandardLib = line - tree.getStdlibFinishLine();
        final String getline = tree.getLine(line);
        this.lineStr = getline == null ? null : "\"" + getline + "\"";
    }

    public String toString() {
        if (lineStr == null) return null;
        if (lineNoStandardLib <= 0) return lineStr + "  :   " + line;
        return lineStr + "  :   " + lineNoStandardLib;
    }

}
