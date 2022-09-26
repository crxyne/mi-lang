package org.crayne.mi.bytecode.writer;

import org.crayne.mi.parsing.ast.Node;

public record ByteLoopBound(long beginLabel, long beforeJumpIfLabel, Node forloopInstr) { }
