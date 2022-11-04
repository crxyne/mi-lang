package org.crayne.mi.bytecode.communication;

import org.apache.commons.lang3.StringUtils;
import org.crayne.mi.bytecode.reader.ByteCodeInterpreter;
import org.crayne.mi.bytecode.reader.ByteCodeValue;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;

public class MiCommunicator {

    private final ByteCodeInterpreter runtime;

    public MiCommunicator(@NotNull final ByteCodeInterpreter runtime) {
        this.runtime = runtime;
        this.runtime.prepare();
    }

    public static MiCommunicator of(@NotNull final ByteCodeInterpreter runtime) {
        return new MiCommunicator(runtime);
    }

    public Value value(@NotNull final Type type, @NotNull final Object obj) {
        return Value.of(type, obj, runtime);
    }

    public Value value(@NotNull final Object obj) {
        return Value.of(obj, runtime);
    }

    public Type type(@NotNull final String typename) {
        return Type.of(typename);
    }

    public Optional<Value> invoke(@NotNull final String module, @NotNull final String func, @NotNull final Value... params) {
        final Optional<ByteCodeValue> res = runtime.execute(module, func, Arrays.stream(params).map(Value::byteCodeValue).toList());
        return res.isEmpty() ? Optional.empty() : Optional.of(new Value(res.get()));
    }

    public Optional<Value> invoke(@NotNull final String fullFuncName, @NotNull final Value... params) {
        return invoke(moduleOf(fullFuncName), identOf(fullFuncName), params);
    }

    public Optional<Value> invoke(@NotNull final String fullFuncName, @NotNull final Object... params) {
        return invoke(fullFuncName, Arrays.stream(params).map(this::value).toList().toArray(new Value[0]));
    }

    private static String moduleOf(@NotNull final String identifier) {
        return identifier.contains(".") ? StringUtils.substringBeforeLast(identifier, ".") : "";
    }

    private static String identOf(@NotNull final String identifier) {
        return identifier.contains(".") ? StringUtils.substringAfterLast(identifier, ".") : identifier;
    }



}
