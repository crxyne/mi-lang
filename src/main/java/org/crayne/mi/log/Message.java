package org.crayne.mi.log;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Message {

    private final String message;
    private final List<String> extraInfo;
    private final List<String> possibleSolutions;
    private final List<String> hints;
    private final MessageHandler handler;
    private final LogHandler.Level level;

    public Message(@NotNull final String message, @NotNull final MessageHandler handler, @NotNull final LogHandler.Level level) {
        this.message = message;
        this.extraInfo = new ArrayList<>();
        this.possibleSolutions = new ArrayList<>();
        this.hints = new ArrayList<>();
        this.handler = handler;
        this.level = level;
    }

    public List<String> extraInfo() {
        return extraInfo;
    }

    public List<String> hints() {
        return hints;
    }

    public List<String> possibleSolutions() {
        return possibleSolutions;
    }

    public Message possibleSolutions(@NotNull final String... sol) {
        addToList(possibleSolutions, sol);
        return this;
    }

    public Message hints(@NotNull final String... hint) {
        addToList(hints, hint);
        return this;
    }

    public Message extraInfo(@NotNull final String... info) {
        addToList(extraInfo, info);
        return this;
    }

    public String message() {
        return message;
    }

    @SafeVarargs
    private static <T> void addToList(@NotNull final List<T> list, @NotNull final T... args) {
        list.addAll(List.of(args));
    }

    private static final String newline = "\n";
    private void message(@NotNull final LogHandler.Level level, @NotNull final String message, @NotNull final StringBuilder builder) {
        builder.append(handler.logger().prefix(level)).append(message).append(TerminalColor.reset()).append(newline);
    }

    public void print() {
        final StringBuilder result = new StringBuilder();
        message(level, message, result);

        for (@NotNull final String info : extraInfo) message(level, info, result);
        for (@NotNull final String sol : possibleSolutions) message(LogHandler.Level.FIX, sol, result);
        for (@NotNull final String hint : hints) message(LogHandler.Level.HINT, hint, result);

        handler.println(result.substring(0, result.toString().length() - 1));
    }

}
