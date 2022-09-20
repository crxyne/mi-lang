package org.crayne.mu;

import org.crayne.mu.bytecode.common.ByteCode;
import org.crayne.mu.bytecode.common.ByteCodeEnumMember;
import org.crayne.mu.bytecode.common.ByteCodeInstruction;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class Tests {

    public static void main(@NotNull final String[] args) {
        final ByteCodeEnumMember enumMember = new ByteCodeEnumMember(69, 69);
        System.out.println(enumMember);
        final ByteCodeInstruction instr = ByteCode.ENUM_VALUE.enumMember(enumMember);
        System.out.println(Arrays.toString(instr.codes()));
        System.out.println(instr.write());
        final ByteCodeEnumMember ofInstr = ByteCode.ENUM_VALUE.ofEnumMember(instr);
        System.out.println(ofInstr);
    }

}
