package org.crayne.mu.runtime.util;

import org.apache.commons.lang3.StringUtils;
import org.crayne.mu.runtime.SyntaxTreeExecution;
import org.crayne.mu.runtime.lang.RModule;
import org.crayne.mu.runtime.lang.RVariable;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MuUtil {

    public static <T> Set<T> unmodifiableSet(@NotNull final Stream<T> stream) {
        return stream.collect(Collectors.toUnmodifiableSet());
    }

    public static Optional<RModule> findSubmoduleByIdentifier(@NotNull final SyntaxTreeExecution tree, @NotNull final String ident) {
        RModule mod = tree.getParentModule();
        final String[] splitDot = ident.split("\\.");
        for (int i = 0; i < splitDot.length - 1; i++) {
            final String sub = splitDot[i];
            mod = mod.getSubModules().stream().filter(m -> m.getName().equals(sub)).findFirst().orElse(null);
            if (mod == null) break;
        }
        return Optional.ofNullable(mod);
    }

    public static Optional<RVariable> findVariable(@NotNull final SyntaxTreeExecution tree, @NotNull final String identifier) {
        return findGlobalVariable(tree, identifier); // TODO try to find local variables first (only if the identifier does not contain '.')
    }

    public static Optional<RVariable> findGlobalVariable(@NotNull final SyntaxTreeExecution tree, @NotNull final String identifier) {
        final Optional<RModule> module = findSubmoduleByIdentifier(tree, identifier);
        return module.flatMap(m -> m.getGlobalModuleVariables().stream().filter(v -> foundIdentifier(identifier, v.getName())).findFirst());
    }

    public static boolean foundIdentifier(@NotNull final String find, @NotNull final String actual) {
        return identOf(find).equals(identOf(actual));
    }

    public static String identOf(@NotNull final String s) {
        return s.contains(".") ? StringUtils.substringAfterLast(s, ".") : s;
    }

    public static void defineAllGlobalVariables(@NotNull final SyntaxTreeExecution tree, @NotNull final RModule module) {
        for (final RVariable variable : module.getGlobalModuleVariables()) {
            if (variable.getValue() != null) continue;
            variable.setValue(tree.getEvaluator().evaluateExpression(variable.getNodeValue()));
        }
        for (final RModule sub : module.getSubModules()) {
            defineAllGlobalVariables(tree, sub);
        }
    }


}
