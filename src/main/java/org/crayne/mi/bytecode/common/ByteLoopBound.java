package org.crayne.mi.bytecode.common;

import org.crayne.mi.parsing.ast.Node;

public record ByteLoopBound(long beginLabel, long beforeJumpIfLabel, Node forloopInstr) { }
