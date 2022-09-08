package org.crayne.mu3.tests;

import org.crayne.mu.runtime.Runtime;
import org.jetbrains.annotations.NotNull;

public class Tests {

    public static void main(@NotNull final String... args) {
        final String code = """
                pub? true = 1b;
                pub? false = 0b;
                
                module std {
                
                    module math {
                        pub? Pi = 3.14159265358979;
                    }
                    
                    pub nat fn println~ (string s) -> "org.crayne.mu.lang.stdlib.StandardLib";
                }
                
                STANDARDLIB_MU_FINISH_CODE;
                
                module helloworld {
                
                    pub fn main {
                        use std;
                        ?hi = true;
                        if hi {
                            println("hello world!");
                        } else {
                            println("goodbye world.");
                        }
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
