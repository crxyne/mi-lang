package org.crayne.mi.stdlib;

import org.crayne.mi.lang.MiCallable;
import org.crayne.mi.log.TerminalColor;
import org.jetbrains.annotations.NotNull;

public class StdTermion {

    @MiCallable
    public static String color_fg(final Integer r, final Integer g, final Integer b) {
        return TerminalColor.foreground(r, g, b);
    }

    @MiCallable
    public static String color_bg(final Integer r, final Integer g, final Integer b) {
        return TerminalColor.background(r, g, b);
    }

    @MiCallable
    public static String color_fg(@NotNull final String hex) {
        return TerminalColor.foreground(hex);
    }

    @MiCallable
    public static String color_bg(@NotNull final String hex) {
        return TerminalColor.background(hex);
    }

    @MiCallable
    public static String color_reset() {
        return TerminalColor.reset();
    }

}
