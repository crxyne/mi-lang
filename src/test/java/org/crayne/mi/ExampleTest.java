package org.crayne.mi;

import org.junit.jupiter.api.Test;

class ExampleTest {
    @Test
    void test() {
        MiLang.main("compile", "file='examples/rainbow.mi'");
        MiLang.main("run", "file='examples/rainbow.mib'", "main='testing.main'");
    }
}
