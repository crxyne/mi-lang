package org.crayne.mu.runtime.util;

import org.apache.commons.lang3.StringUtils;
import org.crayne.mu.runtime.SyntaxTreeExecution;
import org.crayne.mu.runtime.lang.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;
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
        final String[] splitDot = withoutExplicitParent(ident).split("\\.");
        for (int i = 0; i < splitDot.length - 1; i++) {
            final String sub = splitDot[i];
            mod = mod.getSubModules().stream().filter(m -> m.getName().equals(sub)).findFirst().orElse(null);
            if (mod == null) break;
        }
        return Optional.ofNullable(mod);
    }

    public static String withoutExplicitParent(@NotNull final String str) {
        return str.startsWith("!PARENT.") ? str.substring("!PARENT.".length()) : str;
    }

    public static Optional<RVariable> findLocalVariable(@NotNull final SyntaxTreeExecution tree, @NotNull final String identifier) {
        final RFunctionScope currentFunc = tree.getCurrentFunction();
        if (currentFunc == null) return Optional.empty();
        return Optional.ofNullable(currentFunc.getLocalVars().stream().filter(v -> v.getName().equals(identifier)).findFirst().orElseGet(() -> {
            RFunctionScope searchParent = currentFunc.getParent();
            while (searchParent != null) {
                final Optional<RVariable> find = searchParent.getLocalVars().stream().filter(v -> v.getName().equals(identifier)).findFirst();
                if (find.isPresent()) return find.get();
                searchParent = searchParent.getParent();
            }
            return null;
        }));
    }

    public static Optional<RVariable> findVariable(@NotNull final SyntaxTreeExecution tree, @NotNull final String identifier) {
        if (!identifier.contains(".")) {
            Optional<RVariable> local = findLocalVariable(tree, identifier);
            if (local.isPresent()) return local;
        }
        return findGlobalVariable(tree, identifier);
    }

    public static Optional<RVariable> findGlobalVariable(@NotNull final SyntaxTreeExecution tree, @NotNull final String identifier) {
        final Optional<RModule> module = findSubmoduleByIdentifier(tree, identifier);
        return module.flatMap(m -> m.getGlobalModuleVariables().stream().filter(v -> foundIdentifier(identifier, v.getName())).findFirst());
    }

    public static Optional<REnum> findEnum(@NotNull final SyntaxTreeExecution tree, @NotNull final String identifier) {
        final Optional<RModule> module = findSubmoduleByIdentifier(tree, identifier);
        return module.flatMap(m -> m.getEnums().stream().filter(v -> foundIdentifier(identifier, v.getName())).findFirst());
    }

    public static Optional<RFunction> findFunction(@NotNull final SyntaxTreeExecution tree, @NotNull final String identifier) {
        final Optional<RModule> module = findSubmoduleByIdentifier(tree, identifier);
        return module.flatMap(m -> m.getFunctions().stream().filter(f -> foundIdentifier(identifier, f.getName())).findFirst());
    }

    public static Optional<RFunction> findFunction(@NotNull final SyntaxTreeExecution tree, @NotNull final RModule module, @NotNull final String identifier) {
        return module.getFunctions().stream().filter(f -> f.getName().equals(identifier)).findFirst();
    }

    public static Optional<RFunction> findFunction(@NotNull final SyntaxTreeExecution tree, @NotNull final String identifier, @NotNull final List<RValue> params) {
        final Optional<RModule> module = findSubmoduleByIdentifier(tree, identifier);
        return module.flatMap(m -> m.getFunctions().stream().filter(f -> foundIdentifier(identifier, f.getName()) && RFunction.paramsMatch(params, f.getDefinedParams())).findFirst());
    }

    public static Optional<RFunction> findFunction(@NotNull final SyntaxTreeExecution tree, @NotNull final RModule module, @NotNull final String identifier, @NotNull final List<RValue> params) {
        return module.getFunctions().stream().filter(f -> f.getName().equals(identifier) && RFunction.paramsMatch(params, f.getDefinedParams())).findFirst();
    }

    public static Optional<RFunction> findFunctionByTypes(@NotNull final SyntaxTreeExecution tree, @NotNull final String identifier, @NotNull final List<RDatatype> types) {
        final Optional<RModule> module = findSubmoduleByIdentifier(tree, identifier);
        return module.flatMap(m -> m.getFunctions().stream().filter(f -> foundIdentifier(identifier, f.getName()) && RFunction.paramsMatchDatatypes(types, f.getDefinedParams())).findFirst());
    }

    public static Optional<RFunction> findFunctionByTypes(@NotNull final SyntaxTreeExecution tree, @NotNull final RModule module, @NotNull final String identifier, @NotNull final List<RDatatype> types) {
        return module.getFunctions().stream().filter(f -> f.getName().equals(identifier) && RFunction.paramsMatchDatatypes(types, f.getDefinedParams())).findFirst();
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
