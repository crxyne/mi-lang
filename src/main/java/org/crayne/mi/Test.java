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
                mod testing {
                                
                	mut? r = 255;
                	mut? g = 0;
                	mut? b = 0;
                	
                	pub? moveCursorHome = "\\u001b[H";
                	
                	pub fn rainbow {
                		if (r == 255 && g < 255 && b == 0) {
                			g += 5;
                		} else if (r > 0 && g == 255 && b == 0) {
                			r -= 5;
                		} else if (r == 0 && g == 255 && b < 255) {
                			b += 5;
                		} else if (r == 0 && g > 0 && b == 255) {
                			g -= 5;
                		} else if (r < 255 && g == 0 && b == 255) {
                			r += 5;
                		} else if (r == 255 && g == 0 && b > 0) {
                			b -= 5;
                		}
                	}
                	
                	pub fn setCursorVisible(bool v) {
                		if v {
                			std.print("\\u001b[?25h");
                			return;
                		}
                		std.print("\\u001b[?25l");
                	}
                	
                	pub fn executeRainbow(long wait_long, string text) {
                		use std;
                		use termion;
                				
                		setCursorVisible(false);
                		
                		while true {
                			print(moveCursorHome);
                			print(color_fg(r, g, b) + text);
                			rainbow();
                			sleep(wait_long);
                		}
                	}
                	
                	pub fn main {
                		?wait = 10L;
                		executeRainbow(wait, "µ");
                	}
                	
                }
                """);

        final ByteCodeInterpreter run = new ByteCodeInterpreter(compiled, mi.messageHandler());
        final MiCommunicator c = run.newCommunicator();
        c.invoke("testing.main");
    }

}
