package org.crayne.mu;

import org.apache.commons.lang3.StringUtils;
import org.crayne.mu.runtime.Runtime;
import org.crayne.mu.stdlib.StandardLib;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

public class MuLang {

    public static void main(@NotNull final String... args) {
        final Runtime runtime = new Runtime(System.out, true);

        final List<String> argsList = List.of(args);
        final Optional<String> firstInvalid = argsList.stream().filter(s -> !s.startsWith("--main=") && !s.startsWith("--file=")).findFirst();
        if (firstInvalid.isPresent()) {
            runtime.messageHandler().errorMsg("Invalid input argument '" + firstInvalid.get() + "'");
            return;
        }
        String inputFile = null;
        String mainFunc = null;

        for (@NotNull final String arg : argsList) {
            final String value = StringUtils.substringAfter(arg, "=").trim();
            final String key = StringUtils.substringBefore(arg, "=").trim();
            switch (key) {
                case "--main" -> mainFunc = value;
                case "--file" -> inputFile = value;
            }
        }
        if (inputFile == null) {
            runtime.messageHandler().errorMsg("No input file specified");
            return;
        }
        if (mainFunc == null) {
            runtime.messageHandler().errorMsg("No main function specified");
            return;
        }
        final File input = new File(inputFile);
        if (!input.isFile()) {
            runtime.messageHandler().errorMsg("Cannot find input file '" + inputFile + "'");
            return;
        }
        final String code;
        try {
            code = Files.readString(input.toPath());
        } catch (IOException e) {
            runtime.messageHandler().errorMsg("Could not read input file: " + e.getClass().getSimpleName() + " " + e.getMessage());
            return;
        }

        try {
            runtime.execute(StandardLib.standardLib() + code + "\n", true, mainFunc);
        } catch (Throwable e) {
            runtime.messageHandler().errorMsg(e.getMessage());
        }
    }

}
