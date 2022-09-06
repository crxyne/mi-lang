package org.crayne.mu.lang.std;

import org.crayne.mu.lang.MuCallable;
import org.jetbrains.annotations.NotNull;

public class StandardLib {

    @MuCallable
    public static void println(@NotNull final String str) {
        System.out.println(str);
    }

}
