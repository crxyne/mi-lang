package org.crayne.mu3.tests;

import org.crayne.mu.runtime.Runtime;
import org.jetbrains.annotations.NotNull;

public class Tests {

    public static void main(@NotNull final String... args) {
        final String code = """
                pub? true = 1b;
                pub? false = 0b;
                pub? test = 1.0f / 0.0f;
                
                module std {
                
                    pub nat fn println~ (string s) -> "org.crayne.mu.lang.stdlib.StandardLib";
                    
                }
                
                STANDARDLIB_MU_FINISH_CODE;
                
                module helloworld {
                
                    pub fn main {
                        
                    }
                    
                }
                """;

        final Runtime runtime = new Runtime(System.out, true);
        runtime.execute(code);
    }

}
