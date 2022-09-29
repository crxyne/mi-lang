package org.crayne.mi.bytecode.common;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ByteCodeFunctionDefinition {

    private final String fullName;
    private final long id;
    private final ByteDatatype returnType;
    private final List<ByteDatatype> args;

    public ByteCodeFunctionDefinition(@NotNull final String fullName, final ByteDatatype returnType, @NotNull final Collection<ByteDatatype> args, final long id) {
        this.fullName = fullName;
        this.returnType = returnType;
        this.id = id;
        this.args = new ArrayList<>() {{this.addAll(args);}};
    }

    public ByteDatatype returnType() {
        return returnType;
    }

    public List<ByteDatatype> args() {
        return args;
    }

    public String name() {
        return fullName;
    }

    public long id() {
        return id;
    }

    public boolean equals(@NotNull final ByteCodeFunctionDefinition other) {
        boolean equal = fullName.equals(other.fullName) && args.size() == other.args.size();
        if (!equal) return false;
        for (int i = 0; i < args.size(); i++) {
            final ByteDatatype selfType = args.get(i);
            final ByteDatatype otherType = other.args.get(i);
            if (!selfType.equals(otherType)) {
                equal = false;
                break;
            }
        }
        return equal;
    }

    @Override
    public String toString() {
        return "ByteCodeFunction{" +
                "fullName='" + fullName + '\'' +
                ", returnType=" + returnType +
                ", args=" + args +
                '}';
    }

}
