package org.crayne.mu3.tests;

import org.crayne.mu.runtime.Runtime;
import org.crayne.mu.runtime.parsing.ast.Node;
import org.jetbrains.annotations.NotNull;

public class Tests {

    public static void main(@NotNull final String... args) {
        final String code = """
                pub? true = 1b;
                pub? false = 0b;
                
                STANDARDLIB_MU_FINISH_CODE;
               
                
                module std {
                    
                    module math {
                        pub fn test~ () {
                        
                        }
                    }
                    
                }
               
                module testing {
                
                    pub fn main {
                        ? a = 0xBEEF;
                        std.math.test();
                    }
                    
                    
                    
                }
                """;

        final Runtime runtime = new Runtime(System.out, true);
        final Node AST = runtime.parse(code);
        if (AST != null) System.out.println(AST);
    }

}
