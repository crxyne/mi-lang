package org.crayne.mi.stdlib;

import org.crayne.mi.lang.MuCallable;
import org.crayne.mi.log.TerminalColor;
import org.jetbrains.annotations.NotNull;

public class StdTermion {

    @MuCallable
    public static String color_fg(final Integer r, final Integer g, final Integer b) {
        return TerminalColor.foreground(r, g, b);
    }

    @MuCallable
    public static String color_bg(final Integer r, final Integer g, final Integer b) {
        return TerminalColor.background(r, g, b);
    }

    @MuCallable
    public static String color_fg(@NotNull final String hex) {
        return TerminalColor.foreground(hex);
    }

    @MuCallable
    public static String color_bg(@NotNull final String hex) {
        return TerminalColor.background(hex);
    }

}
