package org.crayne.mi;

import org.crayne.mi.bytecode.common.ByteCodeInstruction;
import org.crayne.mi.bytecode.communication.MiCommunicator;
import org.crayne.mi.bytecode.reader.ByteCodeInterpreter;
import org.crayne.mi.stdlib.MiStandardLib;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Test {

    public static void main(@NotNull final String... args) {
        final Mi mi = new Mi(System.out, true);
        final List<ByteCodeInstruction> compiled = mi.compile(MiStandardLib.standardLib(), """
                mod main {
                    
                    pub fn main :: double() {
                        return test(2, 1.0d);
                    }
                    
                    pub fn test :: double (int i, double i2) {
                        std.println(string i + string(int i2));
                        return i + i2;
                    }
                    
                }
                """);

        final ByteCodeInterpreter run = new ByteCodeInterpreter(compiled, mi.messageHandler());
        final MiCommunicator c = run.newCommunicator();
        System.out.println(c.invoke("main.test", 6, 9.0));
        System.out.println(c.invoke("main.main"));
    }

}
