package org.crayne.mi;

import org.crayne.mi.log.MessageHandler;
import org.crayne.mi.parsing.lexer.Token;
import org.crayne.mi.parsing.lexer.Tokenizer;
import org.crayne.mi.parsing.parser.Parser;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class Test {

    public static void main(@NotNull final String... args) {
        final String code =
"""
mod std {

    pub? Pi = 3.14159265358979323;
    pub? E = 2.7182818284;
    
    pub nat fn println(string s) -> "org.crayne.mi.stdlib.MiStandardLib";
    
}
STANDARDLIB_MI_FINISH_CODE;
mod main {

    fn main {
        std.println("hello, world!");
    }
    
}
""";
        final MessageHandler out = new MessageHandler(System.out, true);
        final Tokenizer tokenizer = new Tokenizer(out, Arrays.asList("<<", ">>", "->", "&&", "||", "==", "!=", "::", "<=", ">=", "++", "--", "+=", "*=", "/=", "-=", "%=", "<<=", ">>=", "&=", "|="));
        final List<Token> tokenList = tokenizer.tokenize(code);
        final Parser parser = new Parser(out, tokenizer.stdlibFinishLine());
        System.out.println(parser.parse(tokenList, code));
    }

}
