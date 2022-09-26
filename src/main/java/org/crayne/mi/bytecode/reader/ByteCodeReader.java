package org.crayne.mi.bytecode.reader;

import org.crayne.mi.bytecode.common.ByteCodeInstruction;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ByteCodeReader {

    private long currentByte = -1;
    private final String bytecode;
    private final List<ByteCodeInstruction> instructionSet;

    public ByteCodeReader(@NotNull final String bytecode) {
        this.bytecode = bytecode;
        instructionSet = new ArrayList<>();
    }

    public ByteCodeReader(@NotNull final File bytecodeFile) throws IOException {
        this.bytecode = Files.readString(bytecodeFile.toPath());
        instructionSet = new ArrayList<>();
    }

    public List<ByteCodeInstruction> read() {
        // TODO
        return instructionSet;
    }

    public static List<ByteCodeInstruction> read(@NotNull final String bytecode) {
        return new ByteCodeReader(bytecode).read();
    }

    public static List<ByteCodeInstruction> read(@NotNull final File bytecodeFile) throws IOException {
        return new ByteCodeReader(bytecodeFile).read();
    }



}
