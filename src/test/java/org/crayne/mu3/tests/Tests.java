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
                    
                    pub mut? a = 5;
                
                    pub fn println~ (string s) {
                        // java communication (native functions) not implemented yet
                    }
                    
                }
                                
                STANDARDLIB_MU_FINISH_CODE;
                
                module helloworld {
                
                    pub fn main ~ () {
                        int i;
                        
                        if 4 == 5 {
                            i = 3;
                        } else {
                            if 2 == 3 {
                                i = 2;
                            } else {
                                i = 6
                            }
                        }
                        std.println("" + i);
                    }
                                
                }
                """;

        final Runtime runtime = new Runtime(System.out, true);
        final Node AST = runtime.parse(code);
        if (AST != null) System.out.println(AST);
    }

}
