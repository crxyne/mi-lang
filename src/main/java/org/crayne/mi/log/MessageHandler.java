package org.crayne.mi.log;

import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;
import java.util.List;

public class MessageHandler {

    private final PrintStream out;
    private final LogHandler log;
    private List<String> program;

    public MessageHandler(@NotNull final PrintStream out, final boolean enableColor) {
        this.out = out;
        this.log = new LogHandler(enableColor);
    }

    public PrintStream outStream() {
        return out;
    }

    public Message log(@NotNull final String msg, @NotNull final LogHandler.Level level) {
        return new Message(msg, this, level);
    }

    public void infoMsg(@NotNull final String msg) {
        log.info(msg, out);
    }

    public void errorMsg(@NotNull final String msg) {
        log.error(msg, out);
    }

    public void warnMsg(@NotNull final String msg) {
        log.warn(msg, out);
    }

    public void astHelperError(@NotNull final String msg, final int line, final int column, final int stdlibFinishLine, final boolean stdlib, @NotNull final String... quickFixes) {
        if (program == null) throw new RuntimeException("No program has been fed into MessageHandler instance " + this);
        if (line > program.size() || line <= 0) throw new RuntimeException("Line is out of bounds of the program " + this);

        final String atLine = program.get(line - 1 + stdlibFinishLine);
        if (column > atLine.length() || column <= 0) throw new RuntimeException("Column (" + column + ") is out of bounds of line " + line + " (" + atLine + ")" + this);
        final String helperArrow = " ".repeat(column - 1) + "^";

        log("Encountered error while parsing mi (µ) program\n>>> at line " + (line + (stdlib ? 1 : 0)) + ", column " + column, LogHandler.Level.ERROR)
                .extraInfo(msg)
                .hints(atLine, helperArrow)
                .possibleSolutions(quickFixes)
                .print();
    }

    public void astHelperWarning(@NotNull final String msg, final int line, final int column, final int stdlibFinishLine, final boolean stdlib, @NotNull final String... quickFixes) {
        if (program == null) throw new RuntimeException("No program has been fed into MessageHandler instance " + this);
        if (line > program.size() || line <= (stdlib ? -1 : 0)) throw new RuntimeException("Line " + line + " is out of bounds of the program " + this);

        final String atLine = program.get(line - 1 + stdlibFinishLine);
        if (column > atLine.length() || column <= 0) throw new RuntimeException("Column (" + column + ") is out of bounds of line " + line + " (" + atLine + ")" + this);
        final String helperArrow = " ".repeat(column - 1) + "^";

        log("Encountered warning while parsing mi (µ) program\n>>> at line " + (line + (stdlib ? 1 : 0)) + ", column " + column, LogHandler.Level.WARN)
                .extraInfo(msg)
                .hints(atLine, helperArrow)
                .possibleSolutions(quickFixes)
                .print();
    }

    public void setProgram(@NotNull final String code) {
        this.program = List.of(code.split("\n"));
    }

    public LogHandler logger() {
        return log;
    }

    protected <T> void println(@NotNull final T obj) {
        out.println(obj);
    }

}
