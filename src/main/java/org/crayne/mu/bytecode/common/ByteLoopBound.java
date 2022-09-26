package org.crayne.mu.bytecode.common;

import org.crayne.mu.parsing.ast.Node;

public record ByteLoopBound(long beginLabel, long beforeJumpIfLabel, Node forloopInstr) { }
