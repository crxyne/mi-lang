package org.crayne.mi.bytecode.reader;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.crayne.mi.bytecode.common.ByteCode;
import org.crayne.mi.bytecode.common.ByteCodeException;
import org.crayne.mi.bytecode.common.ByteCodeInstruction;
import org.crayne.mi.bytecode.common.ByteDatatype;
import org.crayne.mi.bytecode.reader.function.ByteCodeInternFunction;
import org.crayne.mi.bytecode.reader.function.ByteCodeNativeFunction;
import org.crayne.mi.bytecode.reader.function.ByteCodeRuntimeFunction;
import org.crayne.mi.log.MessageHandler;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ByteCodeInterpreter {

    private final List<ByteCodeInstruction> program;
    private final MessageHandler messageHandler;
    private long mainFunc = -1;
    private int label;

    private final Map<Long, ByteCodeRuntimeFunction> functionDefinitions = new ConcurrentHashMap<>();
    private long currentFunctionId = 0;
    private int localAddrOffset = -1;
    private final List<ByteCodeValue> variableStack = new ArrayList<>();
    private final List<ByteCodeValue> pushStack = new ArrayList<>();
    private final List<Integer> returnStack = new ArrayList<>();

    public ByteCodeInterpreter(@NotNull final List<ByteCodeInstruction> program, @NotNull final MessageHandler messageHandler) {
        this.program = new ArrayList<>(program);
        this.messageHandler = messageHandler;
    }

    protected static byte[] primitiveByteArray(@NotNull final Byte[] arr, final int begin, final int end) {
        return ArrayUtils.toPrimitive(List.of(arr).subList(begin, end).toArray(new Byte[0]));
    }

    protected static int readInt(@NotNull final Byte[] arr) {
        return readInt(arr, 0, arr.length);
    }

    protected static long readLong(@NotNull final Byte[] arr) {
        return readLong(arr, 0, arr.length);
    }

    protected static String readString(@NotNull final Byte[] arr) {
        return readString(arr, 0, arr.length);
    }

    protected static int readInt(@NotNull final Byte[] arr, final int subBegin, final int subEnd) {
        return Ints.fromByteArray(primitiveByteArray(arr, subBegin, subEnd));
    }

    protected static long readLong(@NotNull final Byte[] arr, final int subBegin, final int subEnd) {
        return Longs.fromByteArray(primitiveByteArray(arr, subBegin, subEnd));
    }

    protected static String readString(@NotNull final Byte[] arr, final int subBegin, final int subEnd) {
        return ByteCode.ofString(primitiveByteArray(arr, subBegin, subEnd));
    }

    public void run() {
        preRead();
        //read();
    }

    private void preRead() {
        for (label = 0; label < program.size(); label++) {
            final ByteCodeInstruction instr = program.get(label);
            evalPre(instr);
        }
    }

    private void read() {
        for (label = 0; label < program.size(); label++) {
            final ByteCodeInstruction instr = program.get(label);
            eval(instr);
        }
    }

    private void push(@NotNull final ByteDatatype type, @NotNull final Byte[] value) {
        pushStack.add(new ByteCodeValue(type, value));
    }

    private void push(@NotNull final ByteCodeValue value) {
        pushStack.add(value);
    }

    private Optional<ByteCodeValue> pushTop(final int offset) {
        return pushStack.size() - offset - 1 >= pushStack.size() ? Optional.empty() : Optional.of(pushStack.get(pushStack.size() - offset - 1));
    }

    private Optional<ByteCodeValue> pushTop() {
        return pushTop(0);
    }

    private void defineVar() {
        variableStack.add(pushTop().orElseThrow(() -> new ByteCodeException("Cannot define variable without any value on the push stack")));
        if (localAddrOffset != -1) localAddrOffset++;
        popPushStack();
    }

    private void declareVar(@NotNull final ByteDatatype type) {
        variableStack.add(new ByteCodeValue(type, new Byte[0]));
        if (localAddrOffset != -1) localAddrOffset++;
    }

    private void evalPre(@NotNull final ByteCodeInstruction instr) {
        switch (instr.type().orElseThrow(() -> new ByteCodeException("Cannot read bytecode instruction " + instr))) {
            case PUSH -> evalPush(instr);
            case DEFINE_VARIABLE -> defineVar();
            case DECLARE_VARIABLE -> evalVarDeclare(instr);
            case NATIVE_FUNCTION_DEFINITION_BEGIN -> evalNatFunc(instr);
            case FUNCTION_DEFINITION_BEGIN -> evalInternFunc();
            case FUNCTION_DEFINITION_END -> evalFuncEnd();
            case VALUE_AT_ADDRESS -> evalValAtAddr();
            case MAIN_FUNCTION -> evalMainFunc(instr);
        }
    }

    private boolean eval(@NotNull final ByteCodeInstruction instr) {
        switch (instr.type().orElseThrow(() -> new ByteCodeException("Cannot read bytecode instruction " + instr))) {
            case PUSH -> evalPush(instr);
            case POP -> evalPop(instr);
            case DEFINE_VARIABLE -> defineVar();
            case DECLARE_VARIABLE -> evalVarDeclare(instr);
            case NATIVE_FUNCTION_DEFINITION_BEGIN -> evalNatFunc(instr);
            case FUNCTION_DEFINITION_BEGIN -> evalInternFunc();
            case FUNCTION_DEFINITION_END -> {
                evalFuncEnd();
                if (returnStack.isEmpty()) return true;
            }
            case RETURN_STATEMENT -> {
                if (returnStack.isEmpty()) return true;
            }
            case VALUE_AT_RELATIVE_ADDRESS -> evalRelToAbsAddr();
            case VALUE_AT_ADDRESS -> evalValAtAddr();
        }
        return false;
    }

    private ByteCodeValue popPushStack() {
        if (pushStack.isEmpty()) throw new ByteCodeException("Cannot perform pop, push stack is empty");
        final ByteCodeValue val = pushStack.get(pushStack.size() - 1);
        pushStack.remove(pushStack.size() - 1);
        return val;
    }

    private void popPushStack(final int amount) {
        for (int i = 0; i < amount; i++) popPushStack();
    }

    private void popVarStack() {
        if (variableStack.isEmpty()) throw new ByteCodeException("Cannot perform pop, variable stack is empty");
        variableStack.remove(variableStack.size() - 1);
    }

    private void popVarStack(final int amount) {
        for (int i = 0; i < amount; i++) popVarStack();
    }

    private void evalFuncEnd() {
        localAddrOffset = -1;
    }

    private void evalPush(@NotNull final ByteCodeInstruction instr) {
        final Byte[] values = instr.codes();
        final ByteCode valueType = ByteCode.of(values[1]).orElseThrow(() -> new ByteCodeException("Cannot find bytecode corresponding to " + ByteCodeReader.byteToHexString(values[1])));
        final Byte[] pushValue = List.of(instr.codes()).subList(2, instr.codes().length - 1).toArray(new Byte[0]);

        switch (valueType) {
            case STRING_VALUE -> push(ByteDatatype.STRING, pushValue);
            case INTEGER_VALUE -> push(ByteDatatype.INT, pushValue);
            case FLOAT_VALUE -> push(ByteDatatype.FLOAT, pushValue);
            case DOUBLE_VALUE -> push(ByteDatatype.DOUBLE, pushValue);
            case LONG_INTEGER_VALUE -> push(ByteDatatype.LONG, pushValue);
            case BOOL_VALUE -> push(ByteDatatype.BOOL, pushValue);
            case ENUM_VALUE -> push(ByteDatatype.ENUM, pushValue);
        }
    }

    private void evalMainFunc(@NotNull final ByteCodeInstruction instr) {
        final Byte[] values = instr.codes();
        final long funcId = readLong(values, 1, values.length - 1);
        if (mainFunc != -1) throw new ByteCodeException("Redefinition of main function (was id " + mainFunc + " already, trying to set it to " + funcId + ")");
        mainFunc = funcId;
    }

    private void evalPop(@NotNull final ByteCodeInstruction instr) {
        final Byte[] values = instr.codes();
        final int amount = readInt(values, 1, values.length - 1);
        popVarStack(amount);
    }

    private void evalVarDeclare(@NotNull final ByteCodeInstruction instr) {
        final Byte[] values = instr.codes();
        final ByteDatatype type = ByteDatatype.ofId(values[1]);
        declareVar(type);
    }

    private void evalValAtAddr() {
        final ByteCodeValue addrBytes = pushTop().orElseThrow(() -> new ByteCodeException("No address specified for value at address opcode"));
        final int addr = readInt(addrBytes.value());
        final ByteCodeValue value = variableStack.get(addr - 1);
        popPushStack();
        push(value);
    }

    private void evalRelToAbsAddr() {
        final ByteCodeValue addrBytes = pushTop().orElseThrow(() -> new ByteCodeException("No address specified for relative addr to absolut addr opcode"));
        final int addr = readInt(addrBytes.value());
        final int currentAbs = variableStack.size() - 1;
        final int abs = currentAbs - localAddrOffset + addr;
        popPushStack();
        push(ByteDatatype.INT, ArrayUtils.toObject(Ints.toByteArray(abs)));
    }

    private void evalInternFunc() {
        localAddrOffset = 0;
        functionDefinitions.put(currentFunctionId, new ByteCodeInternFunction(label));
        currentFunctionId++;
    }

    private static Class<?> argStringToArgClass(@NotNull final String argType) {
        return switch (argType) {
            case "int" -> Integer.class;
            case "char" -> Character.class;
            case "bool" -> Boolean.class;
            case "long" -> Long.class;
            case "float" -> Float.class;
            case "double" -> Double.class;
            case "string" -> String.class;
            default -> throw new ByteCodeException("Cannot use '" + argType + "' as a native function parameter type");
        };
    }

    private static Class<?>[] argStringToArgClasses(@NotNull final String[] argTypes) {
        if (argTypes.length == 0 || argTypes[0].isEmpty()) return new Class<?>[0];
        return Arrays.stream(argTypes).map(ByteCodeInterpreter::argStringToArgClass).toList().toArray(new Class<?>[0]);
    }

    private void evalNatFunc(@NotNull final ByteCodeInstruction instr) {
        final Byte[] values = instr.codes();
        final String signature = readString(values, 6, values.length - 3);
        final String clazzWithMethod = StringUtils.substringBefore(signature, "(");
        final String returnType = StringUtils.substringAfterLast(signature, ")");
        final String[] argTypes = signature.substring(clazzWithMethod.length() + 1, signature.length() - returnType.length() - 1).split("\\|");

        final String clazzStr = StringUtils.substringBeforeLast(clazzWithMethod, ".");
        final String methodStr = StringUtils.substringAfterLast(clazzWithMethod, ".");

        try {
            final Class<?> clazz = Class.forName(clazzStr);
            final Method method = clazz.getMethod(methodStr, argStringToArgClasses(argTypes));
            functionDefinitions.put(currentFunctionId, new ByteCodeNativeFunction(method));
            currentFunctionId++;
        } catch (final ClassNotFoundException e) {
            throw new ByteCodeException("Cannot find class '" + clazzStr + "'");
        } catch (NoSuchMethodException e) {
            throw new ByteCodeException("Cannot find method '" + methodStr + "' in class '" + clazzStr + "'");
        }
    }

}
