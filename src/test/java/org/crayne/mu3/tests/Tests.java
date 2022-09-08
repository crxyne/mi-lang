package org.crayne.mu3.tests;

import org.crayne.mu.runtime.Runtime;
import org.jetbrains.annotations.NotNull;

public class Tests {

    public static void main(@NotNull final String... args) {
        final String code = """
                pub? true = 1b;
                pub? false = 0b;
                
                module std {
                    
                    pub nat fn println~ (string s) -> "org.crayne.mu.stdlib.StandardLib";
                    pub nat fn print~ (string s) -> "org.crayne.mu.stdlib.StandardLib";
                    
                    pub nat fn println~ (int i) -> "org.crayne.mu.stdlib.StandardLib";
                    pub nat fn print~ (int i) -> "org.crayne.mu.stdlib.StandardLib";
                    
                    pub nat fn println~ (double d) -> "org.crayne.mu.stdlib.StandardLib";
                    pub nat fn print~ (double d) -> "org.crayne.mu.stdlib.StandardLib";
                    
                    pub nat fn println~ (float f) -> "org.crayne.mu.stdlib.StandardLib";
                    pub nat fn print~ (float f) -> "org.crayne.mu.stdlib.StandardLib";
                    
                    pub nat fn println~ (long l) -> "org.crayne.mu.stdlib.StandardLib";
                    pub nat fn print~ (long l) -> "org.crayne.mu.stdlib.StandardLib";
                    
                    pub nat fn println~ (bool b) -> "org.crayne.mu.stdlib.StandardLib";
                    pub nat fn print~ (bool b) -> "org.crayne.mu.stdlib.StandardLib";
                    
                    pub nat fn println~ (char c) -> "org.crayne.mu.stdlib.StandardLib";
                    pub nat fn print~ (char c) -> "org.crayne.mu.stdlib.StandardLib";
                    
                }
                
                STANDARDLIB_MU_FINISH_CODE;
                
                module helloworld {
                
                    pub fn main {
                        std.print("12L");
                    }
                    
                }
                """;

        final Runtime runtime = new Runtime(System.out, true);
        try {
            runtime.execute(code, "helloworld.main");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

}
