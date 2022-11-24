package org.crayne.mi;

import org.crayne.mi.bytecode.communication.MiCommunicator;
import org.crayne.mi.bytecode.reader.ByteCodeInterpreter;
import org.crayne.mi.stdlib.MiStandardLib;
import org.jetbrains.annotations.NotNull;

public class Test {

    public static void main(@NotNull final String... args) {
        final String code = """

                                """;

        final Mi mi = new Mi(System.out, true);
        final var instrs = mi.compile(MiStandardLib.standardLib(), code);
        final MiCommunicator c = new ByteCodeInterpreter(instrs, mi.messageHandler()).newCommunicator();
        c.invoke("testing.main");
    }

}
