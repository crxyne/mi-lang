package org.crayne.mu3.tests;

import org.crayne.mu.runtime.Runtime;
import org.crayne.mu.runtime.parsing.ast.Node;
import org.jetbrains.annotations.NotNull;

public class Tests {

    public static void main(@NotNull final String... args) {
        final String code = """
                pub? true = 1b;
                pub? false = 0b;
                
                module std {
                
                    module math {
                        
                        pub fn cos :: double (double d) {
                            ret 0.5;
                        }
                        
                    }
                
                    pub fn println~ (string s) {
                        // java communication (native functions) not implemented yet
                    }
                    
                }
                                
                STANDARDLIB_MU_FINISH_CODE;
                
                module helloworld {
                
                    pub enum Mat {
                        Dirt, Stone, Grass
                    }
                
                    pub fn main {
                        
                    }
                    
                }
                """;

        final Runtime runtime = new Runtime(System.out, true);
        final Node AST = runtime.parse(code);
        if (AST != null) System.out.println(AST);
    }

}
