package org.crayne.mu.runtime;

import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MuUtil {

    public static <T> Set<T> unmodifiableSet(@NotNull final Stream<T> stream) {
        return stream.collect(Collectors.toUnmodifiableSet());
    }


}
