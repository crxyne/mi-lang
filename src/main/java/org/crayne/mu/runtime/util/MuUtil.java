package org.crayne.mu.runtime.util;

import org.crayne.mu.runtime.SyntaxTreeExecution;
import org.crayne.mu.runtime.lang.RModule;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MuUtil {

    public static <T> Set<T> unmodifiableSet(@NotNull final Stream<T> stream) {
        return stream.collect(Collectors.toUnmodifiableSet());
    }

    public static RModule findSubmoduleByIdentifier(@NotNull final SyntaxTreeExecution tree, @NotNull final String ident) {
        RModule mod = tree.getParentModule();
        final String[] splitDot = ident.split("\\.");
        for (int i = 0; i < splitDot.length - 1; i++) {
            final String sub = splitDot[i];
            mod = mod.getSubModules().stream().filter(m -> m.getName().equals(sub)).findFirst().orElse(null);
            if (mod == null) break;
        }
        return mod;
    }


}
