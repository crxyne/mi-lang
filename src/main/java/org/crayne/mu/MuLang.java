package org.crayne.mu;

import org.crayne.mu.log.MessageHandler;
import org.crayne.mu.runtime.Runtime;
import org.crayne.mu.stdlib.MuStandardLib;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Predicate;

public class MuLang {

    public record Argument(String key, String value) {}

    public static List<String> sanitizeArgs(@NotNull final String[] args) {
        return Arrays.stream(String
                        .join(" ", args)
                        .trim()
                        .replace("=", "= ")
                        .split("\\s+(?=(?:(?<=[a-zA-Z\\d])'(?=[A-Za-z\\d])|'[^']*'|[^'])*$)"))
                .map(s -> s.startsWith("'") && s.endsWith("'") ? s.substring(1, s.length() - 1) : s)
                .toList();
    }

    public static Optional<List<Argument>> parseArguments(@NotNull final MessageHandler messageHandler, @NotNull final String[] args) {
        final List<String> argsSanitized = sanitizeArgs(args);
        final List<Argument> result = new ArrayList<>();
        String currentKey = null;

        for (final String arg : argsSanitized) {
            if (!arg.contains("=")) {
                if (currentKey == null) {
                    messageHandler.errorMsg("Expected key before value (key=value)");
                    return Optional.empty();
                }
                result.add(new Argument(currentKey, arg));
                currentKey = null;
                continue;
            }
            currentKey = arg.substring(0, arg.length() - 1);
            if (currentKey.isBlank()) {
                messageHandler.errorMsg("Expected key name, but got empty string before '='");
                return Optional.empty();
            }
        }
        if (currentKey != null) {
            messageHandler.errorMsg("Expected value after key '" + currentKey + "'");
            return Optional.empty();
        }
        final Optional<String> firstInvalid = result.stream().map(Argument::key).filter(invalidArgument).findFirst();
        if (firstInvalid.isPresent()) {
            messageHandler.errorMsg("Unrecognized argument '" + firstInvalid.get() + "'");
            return Optional.empty();
        }
        return Optional.of(result);
    }

    private static final Set<String> validArgs = new HashSet<>(Arrays.asList("main", "pass", "file"));

    public static Predicate<String> invalidArgument = arg -> !validArgs.contains(arg);

    public static Optional<String> findKeyvalueOrElse(@NotNull final String key, @NotNull final MessageHandler messageHandler, @NotNull final String errMsg, @NotNull final List<Argument> args) {
        final Optional<Argument> firstKey = args.stream().filter(a -> a.key.equals(key)).findFirst();
        if (firstKey.isEmpty()) {
            messageHandler.errorMsg(errMsg);
            return Optional.empty();
        }
        return Optional.ofNullable(firstKey.get().value);
    }

    public static Optional<String> readCode(@NotNull final String inputFile, @NotNull final MessageHandler messageHandler) {
        final File input = new File(inputFile);
        if (!input.isFile()) {
            messageHandler.errorMsg("Cannot find input file '" + inputFile + "'");
            return Optional.empty();
        }
        final String code;
        try {
            code = Files.readString(input.toPath());
            return Optional.ofNullable(code);
        } catch (IOException e) {
            messageHandler.errorMsg("Could not read input file: " + e.getClass().getSimpleName() + " " + e.getMessage());
            return Optional.empty();
        }
    }

    public static void main(@NotNull final String... args) {
        final Runtime runtime = new Runtime(System.out, true);
        final MessageHandler messageHandler = runtime.messageHandler();

        final Optional<List<Argument>> oparams = parseArguments(messageHandler, args);
        if (oparams.isEmpty()) return;
        final List<Argument> params = oparams.get();

        final Optional<String> inputFile = findKeyvalueOrElse("file", messageHandler,
                "No input file specified (specify using the file='some file.mu' argument)", params);
        final Optional<String> mainFunc = findKeyvalueOrElse("main", messageHandler,
                "No main function specified (specify using the main=testing.main argument, format: 'main=module.function_name')", params);

        if (inputFile.isEmpty() || mainFunc.isEmpty()) return;

        final Object[] passInParams = params.stream().filter(a -> a.key.equals("pass")).map(Argument::value).toArray();

        final Optional<String> code = readCode(inputFile.get(), messageHandler);
        if (code.isEmpty()) return;

        try {
            runtime.execute(MuStandardLib.standardLib(), code.get(), true, mainFunc.get(), passInParams);
        } catch (Throwable e) {
            messageHandler.errorMsg(e.getMessage());
        }
    }

}
