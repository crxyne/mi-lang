package org.crayne.mi.bytecode.writer;

import org.crayne.mi.parsing.ast.Node;

public record ByteLoopBound(int beginLabel, int beforeJumpIfLabel, Node forloopInstr) { }
