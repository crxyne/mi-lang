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

import java.lang.reflect.InvocationTargetException;
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
    private final List<Integer> localAddrOffset = new ArrayList<>();
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
        return ByteCode.bytesToString(primitiveByteArray(arr, subBegin, subEnd));
    }

    public void run() {
        preRead();
        if (mainFunc != -1) read();
    }

    private void preRead() {
        for (label = 0; label < program.size(); label++) {
            final ByteCodeInstruction instr = program.get(label);
            evalPre(instr);
        }
    }

    private void read() {
        final ByteCodeRuntimeFunction mainFunction = functionDefinitions.get(mainFunc);
        if (!(mainFunction instanceof final ByteCodeInternFunction mainInternFunc)) throw new ByteCodeException("The main function should be an intern function");

        localAddrOffset.add(0);
        for (label = mainInternFunc.label() + 1; label < program.size(); label++) {
            final ByteCodeInstruction instr = program.get(label);
            if (eval(instr)) return; // eval() returns true if the function should end
        }
    }

    private void push(@NotNull final ByteDatatype type, @NotNull final Byte[] values) {
        final ByteCodeValue value = new ByteCodeValue(type, values);
        pushStack.add(value);
    }

    private void push(@NotNull final ByteCodeValue value) {
        pushStack.add(value);
    }

    private Optional<ByteCodeValue> pushTop(final int offset) {
        return pushStack.size() - offset - 1 >= pushStack.size() ? Optional.empty() : Optional.of(pushStack.get(pushStack.size() - offset - 1));
    }

    private Optional<ByteCodeValue> varTop(final int offset) {
        return variableStack.size() - offset - 1 >= variableStack.size() ? Optional.empty() : Optional.of(variableStack.get(variableStack.size() - offset - 1));
    }

    private Optional<ByteCodeValue> pushTop() {
        return pushTop(0);
    }

    private void defineVar() {
        final ByteCodeValue val = pushTop().orElseThrow(() -> new ByteCodeException("Cannot define variable without any value on the push stack"));
        variableStack.add(val);
        if (!localAddrOffset.isEmpty()) incLocalAddrOffset();
        popPushStack();
    }

    private void declareVar(@NotNull final ByteDatatype type) {
        variableStack.add(new ByteCodeValue(type, new Byte[0]));
        if (!localAddrOffset.isEmpty()) incLocalAddrOffset();
    }

    private int localAddrOffsetIndex() {
        return localAddrOffset.size() - 1;
    }

    private int localAddrOffset(final int offset) {
        return localAddrOffset.get(offset);
    }

    private void incLocalAddrOffset() {
        localAddrOffset.set(localAddrOffsetIndex(), localAddrOffset(localAddrOffsetIndex()) + 1);
    }

    private void decLocalAddrOffset() {
        localAddrOffset.set(localAddrOffsetIndex(), localAddrOffset(localAddrOffsetIndex()) - 1);
    }

    private void evalPre(@NotNull final ByteCodeInstruction instr) {
        if (localAddrOffset.isEmpty()) switch (instr.type().orElseThrow(() -> new ByteCodeException("Cannot read bytecode instruction " + instr))) {
            case PUSH -> evalPush(instr);
            case DEFINE_VARIABLE -> defineVar();
            case DECLARE_VARIABLE -> evalVarDeclare(instr);
            case NATIVE_FUNCTION_DEFINITION_BEGIN -> evalNatFunc(instr);
            case FUNCTION_DEFINITION_BEGIN -> evalInternFunc();
            case VALUE_AT_ADDRESS -> evalValAtAddr();
            case MAIN_FUNCTION -> evalMainFunc(instr);
            case CAST -> evalCast(instr);
            //case MUTATE_VARIABLE -> popPushStack(2);
            case PLUS, MINUS, MULTIPLY, DIVIDE, MODULO, BIT_AND, BIT_OR, BIT_XOR, BITSHIFT_LEFT, BITSHIFT_RIGHT, LOGICAL_AND, LOGICAL_OR,
                    EQUALS, LESS_THAN, LESS_THAN_OR_EQUAL, GREATER_THAN, GREATER_THAN_OR_EQUAL -> popPushStack(1);
        } else if (instr.type().orElse(null) == ByteCode.FUNCTION_DEFINITION_END) evalFuncEnd();
    }

    private boolean eval(@NotNull final ByteCodeInstruction instr) {
        switch (instr.type().orElseThrow(() -> new ByteCodeException("Cannot read bytecode instruction " + instr))) {
            case PUSH -> evalPush(instr);
            case POP -> evalPop(instr);
            case DEFINE_VARIABLE -> defineVar();
            case DECLARE_VARIABLE -> evalVarDeclare(instr);
            case NATIVE_FUNCTION_DEFINITION_BEGIN -> evalNatFunc(instr);
            case FUNCTION_DEFINITION_BEGIN -> evalInternFunc();
            case FUNCTION_DEFINITION_END, RETURN_STATEMENT -> {
                evalFuncEnd();
                if (returnStack.isEmpty()) return true;
                label = returnStack.get(returnStack.size() - 1) - 1;
                returnStack.remove(returnStack.size() - 1);
            }
            case VALUE_AT_RELATIVE_ADDRESS -> evalValAtRelAddr();
            case VALUE_AT_ADDRESS -> evalValAtAddr();
            case FUNCTION_CALL -> evalFuncCall(instr);
            case JUMP -> evalJump(instr);
            case JUMP_IF -> evalJumpIf(instr);
            case CAST -> evalCast(instr);
            //case MUTATE_VARIABLE -> popPushStack(2); // TODO
            case NOT, PLUS, MINUS, MULTIPLY, DIVIDE, MODULO, BIT_AND, BIT_OR, BIT_XOR, BIT_NOT, BITSHIFT_LEFT, BITSHIFT_RIGHT, LOGICAL_AND, LOGICAL_OR,
                    EQUALS, LESS_THAN, LESS_THAN_OR_EQUAL, GREATER_THAN, GREATER_THAN_OR_EQUAL -> evalOperator(instr);
           // default -> System.out.println("ignored instr " + instr);
        }
        return false;
    }

    private ByteCodeValue popPushStack() {
        if (pushStack.isEmpty()) throw new ByteCodeException("Cannot perform pop, push stack is empty");
        return pushStack.remove(pushStack.size() - 1);
    }

    private void popPushStack(final int amount) {
        for (int i = 0; i < amount; i++) popPushStack();
    }

    private ByteCodeValue popVarStack() {
        if (variableStack.isEmpty()) throw new ByteCodeException("Cannot perform pop, variable stack is empty");
        if (!localAddrOffset.isEmpty()) decLocalAddrOffset();
        return variableStack.remove(variableStack.size() - 1);
    }

    private void popVarStack(final int amount) {
        for (int i = 0; i < amount; i++) popVarStack();
    }

    private void evalFuncEnd() {
        if (!localAddrOffset.isEmpty()) localAddrOffset.remove(localAddrOffsetIndex());
    }

    private void evalPush(@NotNull final ByteCodeInstruction instr) {
        final Byte[] values = instr.codes();
        final ByteCode valueType = ByteCode.of(values[1]).orElseThrow(() -> new ByteCodeException("Cannot find bytecode corresponding to " + ByteCodeReader.byteToHexString(values[1])));
        final Byte[] pushValue = List.of(instr.codes()).subList(2, instr.codes().length - 1).toArray(new Byte[0]);

        switch (valueType) {
            case STRING_VALUE -> push(ByteDatatype.STRING, pushValue);
            case INTEGER_VALUE -> push(ByteDatatype.INT, pushValue);
            case CHARACTER_VALUE -> push(ByteDatatype.CHAR, pushValue);
            case FLOAT_VALUE -> push(ByteDatatype.FLOAT, pushValue);
            case DOUBLE_VALUE -> push(ByteDatatype.DOUBLE, pushValue);
            case LONG_INTEGER_VALUE -> push(ByteDatatype.LONG, pushValue);
            case BOOL_VALUE -> push(ByteDatatype.BOOL, pushValue);
            case ENUM_VALUE -> push(ByteDatatype.ENUM, pushValue);
            case NULL_VALUE -> push(ByteDatatype.NULL, new Byte[0]);
        }
    }

    private void evalCast(@NotNull final ByteCodeInstruction instr) {
        final Byte[] values = instr.codes();
        final ByteDatatype type = ByteDatatype.ofId(values[1]);
        final ByteCodeValue top = pushTop().orElseThrow(() -> new ByteCodeException("Cannot cast value; no value on push stack"));
        final ByteCodeValue cast = top.cast(type);
        popPushStack();
        push(cast);
    }

    private void evalOperator(@NotNull final ByteCodeInstruction instr) {
        final ByteCode type = instr.type().orElseThrow(() -> new ByteCodeException("Cannot find opcode type of " + instr));
        final ByteCodeValue newValue;

        final ByteCodeValue y = popPushStack();
        switch (type) {
            case NOT -> {
                push(y.not());
                return;
            }
            case BIT_NOT -> {
                push(y.bit_not());
                return;
            }
        }
        final ByteCodeValue x = popPushStack();
        newValue = switch (type) {
            case EQUALS -> x.equal(y);
            case PLUS -> x.plus(y);
            case MINUS -> x.minus(y);
            case MULTIPLY -> x.multiply(y);
            case DIVIDE -> x.divide(y);
            case MODULO -> x.modulo(y);
            case BIT_AND -> x.bit_and(y);
            case BIT_OR -> x.bit_or(y);
            case BIT_XOR -> x.bit_xor(y);
            case BITSHIFT_LEFT -> x.bit_shift_left(y);
            case BITSHIFT_RIGHT -> x.bit_shift_right(y);
            case LOGICAL_AND -> x.logical_and(y);
            case LOGICAL_OR -> x.logical_or(y);
            case LESS_THAN -> x.less_than(y);
            case LESS_THAN_OR_EQUAL -> x.less_than_or_equal(y);
            case GREATER_THAN -> x.greater_than(y);
            case GREATER_THAN_OR_EQUAL -> x.greater_than_or_equal(y);
            default -> null;
        };
        if (newValue == null) return;
        push(newValue);
    }

    private void evalFuncCall(@NotNull final ByteCodeInstruction instr) {
        final Byte[] values = instr.codes();
        final long functionId = readLong(values, 1, values.length - 1);
        final ByteCodeRuntimeFunction func = functionDefinitions.get(functionId);

        if (func instanceof final ByteCodeInternFunction internFunc) {
            localAddrOffset.add(0);
            returnStack.add(label + 1);
            label = internFunc.label();
        } else if (func instanceof final ByteCodeNativeFunction nativeFunc) {
            invokeNativeFuncCall(nativeFunc);
        }
    }

    private void invokeNativeFuncCall(@NotNull final ByteCodeNativeFunction nativeFunc) {
        final Method method = nativeFunc.method();
        final List<ByteCodeValue> args = new ArrayList<>();
        for (int i = 0; i < method.getParameterCount(); i++) {
            args.add(popPushStack());
        }
        Collections.reverse(args);

        final List<Object> params = new ArrayList<>(args.stream().map(ByteCodeValue::asObject).toList());
        try {
            final Object res = method.invoke(null, params.toArray(new Object[0]));
            if (res != null) {
                final ByteDatatype retType = ByteDatatype.of(argClassToArgString(res.getClass()));
                final Byte[] bytes = switch (retType.id()) {
                    case 0x00 -> ArrayUtils.toObject(ByteCode.intToBytes(((boolean) res) ? 1 : 0));
                    case 0x01, 0x02 -> ArrayUtils.toObject(ByteCode.intToBytes((int) res));
                    case 0x03 -> ArrayUtils.toObject(ByteCode.longToBytes((long) res));
                    case 0x04 -> ArrayUtils.toObject(ByteCode.floatToBytes((float) res));
                    case 0x05 -> ArrayUtils.toObject(ByteCode.doubleToBytes((double) res));
                    case 0x06 -> ByteCode.stringToBytes(String.valueOf(res));
                    default -> throw new ByteCodeException("Cannot use " + retType + " as a native function return datatype");
                };
                push(retType, bytes);
            }
        } catch (final IllegalAccessException | InvocationTargetException e) {
            throw new ByteCodeException("Cannot invoke native function method: " + e.getMessage());
        }
    }

    private void evalJump(@NotNull final ByteCodeInstruction instr) {
        final Byte[] values = instr.codes();
        final int jumpTo = readInt(values, 1, values.length - 1);
        label = jumpTo - 2;
    }

    private void evalJumpIf(@NotNull final ByteCodeInstruction instr) {
        final Byte[] values = instr.codes();
        final int jumpTo = readInt(values, 1, values.length - 1);
        final ByteCodeValue condition = pushTop().orElseThrow(() -> new ByteCodeException("No condition at top of stack for jump-if to work"));
        if (condition.type().id() != ByteDatatype.BOOL.id()) throw new ByteCodeException("Expected boolean value as condition for jump-if opcode");

        final int condInt = Ints.fromByteArray(ArrayUtils.toPrimitive(condition.value()));
        if (condInt != 0) label = jumpTo - 2;
        popPushStack(); // pop condition since we dont need it anymore
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

    private void evalValAtRelAddr() {
        final ByteCodeValue addrBytes = pushTop().orElseThrow(() -> new ByteCodeException("No address specified for relative addr to absolut addr opcode"));
        if (localAddrOffset.isEmpty()) throw new ByteCodeException("Relative address evaluation outside of function");
        final int addr = readInt(addrBytes.value());
        final ByteCodeValue val = atRelativeAddress(addr);

        popPushStack();
        push(val);
    }

    private ByteCodeValue atRelativeAddress(final int addr) {
        final int currentRelAddr = localAddrOffset(localAddrOffsetIndex());
        if (currentRelAddr == 0) throw new ByteCodeException("Cannot get variable at relative address, no variables have been defined in this function");
        final int currentAbs = variableStack.size();
        int abs = currentAbs - currentRelAddr + addr;
        return variableStack.get(abs);
    }

    private void evalInternFunc() {
        localAddrOffset.add(0);
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
            default -> throw new ByteCodeException("Cannot use '" + argType + "' as a native function variable type");
        };
    }

    private static String argClassToArgString(@NotNull final Class<?> argType) {
        return switch (argType.getName()) {
            case "java.lang.Integer" -> "int";
            case "java.lang.Character" -> "char";
            case "java.lang.Boolean" -> "bool";
            case "java.lang.Long" -> "long";
            case "java.lang.Float" -> "float";
            case "java.lang.Double" -> "double";
            case "java.lang.String" -> "string";
            default -> throw new ByteCodeException("Cannot use '" + argType + "' as a native function variable type");
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