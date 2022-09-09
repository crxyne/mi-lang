package org.crayne.mu;

import org.crayne.mu.runtime.Runtime;
import org.crayne.mu.runtime.SyntaxTreeExecution;
import org.crayne.mu.stdlib.StandardLib;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class Tests {

    public static void main(@NotNull final String[] args) {
        final Runtime runtime = new Runtime(System.out, true);
        final String code = """
                module testing {
                
                    pub class Test {
                        
                        pub int ID;
                        
                        pub new (int ID) {
                        
                        }
                        
                        pub fn doSomething {
                        
                        }
                        
                    }
                
                }
                """;
        final Optional<SyntaxTreeExecution> tree = runtime.parse(StandardLib.standardLib() + code + "\n");
        tree.ifPresent(ast -> {
           // System.out.println(ast.getAST());
           // System.out.println(ast.getParentModule());
        });
    }

}
