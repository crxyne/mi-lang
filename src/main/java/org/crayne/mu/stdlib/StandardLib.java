package org.crayne.mu.stdlib;

import org.crayne.mu.lang.MuCallable;
import org.jetbrains.annotations.NotNull;

public class StandardLib {

    @MuCallable
    public static void println(@NotNull final String str) {
        System.out.println(str);
    }

    @MuCallable
    public static double cos(final double d) {
        return Math.cos(d);
    }

}
