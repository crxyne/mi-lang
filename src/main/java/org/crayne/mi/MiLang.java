package org.crayne.mi;

import org.apache.commons.lang3.StringUtils;
import org.crayne.mi.bytecode.common.ByteCodeInstruction;
import org.crayne.mi.bytecode.communication.MiCommunicator;
import org.crayne.mi.bytecode.reader.ByteCodeInterpreter;
import org.crayne.mi.bytecode.reader.ByteCodeReader;
import org.crayne.mi.log.MessageHandler;
import org.crayne.mi.stdlib.MiStandardLib;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Predicate;

public class MiLang {

    // TODO big big issue, cant access enum values from other modules, as it just cant "find the variable"

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

    private static final Set<String> validArgs = new HashSet<>(Arrays.asList("file", "main"));

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
            return Optional.of(code.replace("\t", "    "));
        } catch (IOException e) {
            messageHandler.errorMsg("Could not read input file: " + e.getClass().getSimpleName() + " " + e.getMessage());
            return Optional.empty();
        }
    }

    public static void main(@NotNull final String... args) {
        final Mi mi = new Mi(System.out, true);
        final MessageHandler messageHandler = mi.messageHandler();

        if (args.length == 0) {
            messageHandler.errorMsg("Expected either 'compile' or 'run' as first argument in args: " + Arrays.toString(args));
            return;
        }
        final String first = args[0];
        if (!first.equals("compile") && !first.equals("run")) {
            messageHandler.errorMsg("Expected either 'compile' or 'run' as first argument in args: " + Arrays.toString(args));
            return;
        }
        final boolean compile = first.equals("compile");

        final Optional<List<Argument>> oparams = parseArguments(messageHandler, List.of(args).subList(1, args.length).toArray(new String[0]));
        if (oparams.isEmpty()) return;
        final List<Argument> params = oparams.get();

        final Optional<String> inputFile = findKeyvalueOrElse("file", messageHandler,
                "No input file specified (specify using the file='some file.mi' argument)", params);

        if (inputFile.isEmpty()) return;

        if (compile) {
            final Optional<String> code = readCode(inputFile.get(), messageHandler);
            if (code.isEmpty()) return;

            final File outputFile = new File(StringUtils.substringBeforeLast(inputFile.get(), ".") + ".mib");

            mi.compile(MiStandardLib.standardLib(), code.get(), outputFile, new File(inputFile.get()));
            return;
        }
        final Optional<String> omainFunc = findKeyvalueOrElse("main", messageHandler,
                "No main function specified (specify using the main=some.module.name.main argument)", params);
        if (omainFunc.isEmpty()) return;

        final String mainFunc = omainFunc.get();
        try {
            final List<ByteCodeInstruction> instrs = ByteCodeReader.read(new File(inputFile.get()), messageHandler);
            final ByteCodeInterpreter interpreter = new ByteCodeInterpreter(instrs, messageHandler);
            final MiCommunicator c = interpreter.newCommunicator();
            c.invoke(mainFunc);
        } catch (final Throwable e) {
            e.printStackTrace();
        }
    }

}
