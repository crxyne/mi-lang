package org.crayne.mi;

import org.crayne.mi.bytecode.common.ByteCodeInstruction;
import org.crayne.mi.bytecode.reader.ByteCodeInterpreter;
import org.crayne.mi.stdlib.MiStandardLib;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Test {

    public static void main(@NotNull final String... args) {
        final Mi mi = new Mi(System.out, true);
        final List<ByteCodeInstruction> compiled = mi.compile(MiStandardLib.standardLib(), """
                mod main {
                    pub fn main {
                        for mut? i = 0, i .. 10 {
                            std.println("hello " + i);
                        }
                    }
                }
                """, "main", "main");

        final ByteCodeInterpreter run = new ByteCodeInterpreter(compiled, mi.messageHandler());
        run.run();
    }

}
