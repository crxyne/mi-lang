package org.crayne.mu3.tests;

import org.crayne.mu.runtime.Runtime;
import org.jetbrains.annotations.NotNull;

public class Tests {

    public static void main(@NotNull final String... args) {
        final String code = """
                pub enum TestEnum {
                    TEST
                }
                
                pub? true = 1b;
                pub? false = 0b;
                
                
                module std {
                
                    pub enum TestEnum {
                        TEST, OTHER
                    }
                    
                    pub nat fn println~ (string s) -> "org.crayne.mu.lang.stdlib.StandardLib";
                }
                
                pub? test = 1 / 0;
                
                STANDARDLIB_MU_FINISH_CODE;
                
                
                
                module helloworld {
                
                    pub fn main {
                        use std;
                        println("hi");
                    }
                    
                }
                """;

        final Runtime runtime = new Runtime(System.out, true);
        runtime.execute(code);
    }

}
