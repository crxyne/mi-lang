package org.crayne.mu.log;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.PrintStream;

public class LogHandler {

    public enum Level {
        INFO(Color.LIGHT_GRAY),
        FIX(Color.decode("#30ACFF")),
        WARN(Color.YELLOW),
        HINT(Color.CYAN),
        SUCCESS(Color.GREEN),
        ERROR(Color.RED);
        private final Color color;
        Level(@NotNull final Color color) {
            this.color = color;
        }

        public Color color() {
            return color;
        }
    }

    private final boolean enableColor;

    public LogHandler(final boolean enableColor) {
        this.enableColor = enableColor;
    }

    public String prefix(@NotNull final Level level) {
        return (enableColor ? TerminalColor.foreground(level.color()) : "") + "[mu] [" + level + "]: ";
    }

    public void log(@NotNull final String msg, final Level level, @NotNull final PrintStream out) {
        out.println((level != null ? prefix(level) : "") + msg + TerminalColor.reset());
    }

    public void info(@NotNull final String msg, @NotNull final PrintStream out) {
        log(msg, Level.INFO, out);
    }

    public void warn(@NotNull final String msg, @NotNull final PrintStream out) {
        log(msg, Level.WARN, out);
    }

    public void error(@NotNull final String msg, @NotNull final PrintStream out) {
        log(msg, Level.ERROR, out);
    }

}
