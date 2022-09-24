package org.crayne.mu.runtime.util;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MuUtil {

    public static <T> Set<T> unmodifiableSet(@NotNull final Stream<T> stream) {
        return stream.collect(Collectors.toUnmodifiableSet());
    }

    public static String withoutExplicitParent(@NotNull final String str) {
        return str.startsWith("!PARENT.") ? str.substring("!PARENT.".length()) : str;
    }

    public static String identOf(@NotNull final String s) {
        return s.contains(".") ? StringUtils.substringAfterLast(s, ".") : s;
    }

}
