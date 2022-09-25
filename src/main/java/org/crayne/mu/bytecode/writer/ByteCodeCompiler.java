package org.crayne.mu.bytecode.writer;

import org.crayne.mu.bytecode.common.*;
import org.crayne.mu.lang.EqualOperation;
import org.crayne.mu.parsing.ast.Node;
import org.crayne.mu.parsing.ast.NodeType;
import org.crayne.mu.parsing.lexer.Token;
import org.crayne.mu.runtime.SyntaxTreeCompilation;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import static org.crayne.mu.bytecode.common.ByteCode.*;

// TODO final todos to finish up the compiler: add if, else, while, do while, for & ternary operator
// after this, directly start work on interpreter
public class ByteCodeCompiler {
    private final List<ByteCodeInstruction> result;
    private final List<ByteCodeInstruction> globalVariables;
    private final List<ByteCodeInstruction> functionDefinitions;
    private final List<ByteCodeInstruction> enumDefinitions;
    private final SyntaxTreeCompilation tree;

    private final Map<String, Long> globalVariableStorage;
    private final LinkedHashMap<String, Long> localVariableStorage;
    private final Map<String, ByteCodeEnum> enumStorage;
    private final Set<ByteCodeFunction> functionStorage;

    private final List<String> currentModuleName = new ArrayList<>() {{this.add("!PARENT");}};

    private long absoluteAddress = 1;
    private long relativeAddress = -1;
    private long functionId = 0;
    private long label = 1;
    private int enumId = 0;
    private final List<Integer> localScopeVariables;
    private int scope = -1;

    public ByteCodeCompiler(@NotNull final SyntaxTreeCompilation tree) {
        this.tree = tree;
        globalVariableStorage = new HashMap<>();
        localScopeVariables = new ArrayList<>();
        localVariableStorage = new LinkedHashMap<>();
        enumStorage = new HashMap<>();
        functionStorage = new HashSet<>();
        enumDefinitions = new ArrayList<>();
        globalVariables = new ArrayList<>();
        functionDefinitions = new ArrayList<>();
        result = new ArrayList<>();
    }

    public int getStdlibFinishLine() {
        return tree.getStdlibFinishLine();
    }

    public String getLine(final int line) {
        return tree.getLine(line);
    }

    private void panic(@NotNull final String message) {
        tree.error(message);
    }

    private boolean compilingFunction() {
        return relativeAddress >= 0;
    }

    private String currentModuleName() {
        return String.join(".", currentModuleName);
    }

    public List<ByteCodeInstruction> compile() {
        final Node ast = tree.getAST();
        if (ast.type() == NodeType.PARENT) {
            compileParent(ast, result);
        } else {
            panic("Expected 'PARENT' node at the beginning of syntax tree");
            return new ArrayList<>();
        }
        result.addAll(0, functionDefinitions);
        result.addAll(0, enumDefinitions);
        result.addAll(0, globalVariables);
        result.add(0, header());
        return result;
    }

    public static void compileToFile(@NotNull final List<ByteCodeInstruction> bytecode, @NotNull final File file) throws IOException {
        if (bytecode.isEmpty()) return;
        Files.writeString(file.toPath(), String.join("", bytecode.stream().map(b -> b.write()).toList()));
    }

    private void compileParent(@NotNull final Node parent, @NotNull final List<ByteCodeInstruction> result) {
        for (final Node instruction : parent.children()) {
            tree.traceback(instruction.lineDebugging());
            switch (instruction.type()) {
                case VAR_DEF_AND_SET_VALUE -> compileVariableDefinition(instruction, result);
                case VAR_DEFINITION -> compileVariableDeclaration(instruction, result);
                case CREATE_MODULE -> {
                    currentModuleName.add(instruction.child(0).value().token());
                    compileParent(instruction.child(1), result);
                    currentModuleName.remove(currentModuleName.size() - 1);
                }
                case NATIVE_FUNCTION_DEFINITION -> compileNativeFunction(instruction);
                case FUNCTION_DEFINITION -> compileFunction(instruction, null, instruction.child(4));
                case FUNCTION_CALL -> compileFunctionCall(instruction, result);
                case CREATE_ENUM -> compileEnumDefinition(instruction);
                case VAR_SET_VALUE -> compileVariableMutation(instruction, false, result);
                case RETURN_VALUE -> compileReturnStatement(instruction, result);
                case NOOP -> {
                    if (!instruction.children().isEmpty()) {
                        compileParent(instruction, result);
                    }
                }
                case SCOPE -> {
                    scope++;
                    localScopeVariables.add(0);
                    compileParent(instruction, result);
                    final int vars = !localScopeVariables.isEmpty() ? localScopeVariables.get(this.scope) : 0;
                    if (vars > 0) rawInstruction(ByteCode.pop(vars), result);
                    //noinspection SuspiciousMethodCalls
                    Arrays.asList(localVariableStorage.keySet().toArray()).subList(0, vars).forEach(localVariableStorage.keySet()::remove);

                    localScopeVariables.remove(localScopeVariables.size() - 1);
                    scope--;
                }
                case IF_STATEMENT -> compileIfStatement(instruction, result);
                default -> System.out.println("ignored instruction: " + instruction);
            }
        }
    }

    private int findEnumId(@NotNull final String fullName) {
        final ByteCodeEnum enumDef = findEnum(fullName);
        return enumDef == null ? -1 : enumDef.id();
    }

    private ByteCodeEnum findEnum(@NotNull final String fullName) {
        return enumStorage.get(fullName);
    }

    private void compileEnumDefinition(@NotNull final Node instr) {
        final String name = instr.child(0).value().token();
        final List<String> members = instr.child(2).child(0).children().stream().map(n -> n.value().token()).toList();
        final ByteCodeEnum enumDef = new ByteCodeEnum(name, enumId, members);
        enumId++;
        final List<ByteCodeInstruction> bytes = defineEnum(enumDef);
        enumDefinitions.addAll(bytes);
        label += bytes.size();
        enumStorage.put(currentModuleName() + "." + name, enumDef);
    }

    private void compileIfStatement(@NotNull final Node instr, @NotNull final List<ByteCodeInstruction> result) {
        final Node condition = instr.child(0).child(0);
        final Node ifScope = instr.child(1);
        final boolean hasElse = instr.children().size() > 2;

        compileExpression(condition, result);
        rawInstruction(new ByteCodeInstruction(NOT.code()), result); // push the condition and invert it
        // the plan is to jump to the else scope if the condition was false, otherwise just keep going (execute if scope)
        // then, after the if scope, jump to whenever the else scope ends

        final int elseJumpIndex = result.size(); // save the index in the result, to add the label later
        compileParent(ifScope, result); // normally parse the if scope
        final int afterElseJumpIndex = result.size() + 1; // save the index in the result, to add the other label later
        final long elseJumpLabel = label + 2; // then get the label of where the else scope starts (+ 2, because 2 jumps have to be added)
        if (hasElse) { // check if there even is an else scope
            final Node elseScope = instr.child(2).child(0).child(0).child(0);
            compileParent(elseScope, result); // parse the else scope like normal here
        }
        result.add(elseJumpIndex, jumpIf(elseJumpLabel)); // add the jump statements using the saved labels, at their right positions
        label++;
        if (hasElse) { // to avoid unnecessary jumping, just check if theres an else at all
            result.add(afterElseJumpIndex, jump(label + 1));
            label++;
        }
    }

    private void compileReturnStatement(@NotNull final Node instr, @NotNull final Collection<ByteCodeInstruction> result) {
        if (instr.children().isEmpty()) {
            rawInstruction(new ByteCodeInstruction(RETURN_STATEMENT.code()), result);
            return;
        }
        compileExpression(instr.child(0), result);
        rawInstruction(new ByteCodeInstruction(RETURN_STATEMENT.code()), result);
    }

    private void singleInstruction(@NotNull final ByteCode byteCode, @NotNull final Collection<ByteCodeInstruction> result) {
        rawInstruction(new ByteCodeInstruction(byteCode.code()), result);
    }

    private void compileVariableMutation(@NotNull final Node instr, final boolean pushMutated, @NotNull final Collection<ByteCodeInstruction> result) {
        final String identifier = instr.child(0).value().token();
        final String operator = instr.child(1).value().token();
        final Node value = instr.child(2);

        compileExpression(value, result);
        final EqualOperation equalOperation = Objects.requireNonNull(EqualOperation.of(operator));
        if (equalOperation != EqualOperation.EQUAL) compileExpression(instr.child(0), result);
        switch (equalOperation) {
            case ADD -> singleInstruction(PLUS, result);
            case SUB -> singleInstruction(MINUS, result);
            case MULT -> singleInstruction(MULTIPLY, result);
            case DIV -> singleInstruction(DIVIDE, result);
            case OR -> singleInstruction(BIT_OR, result);
            case AND -> singleInstruction(BIT_AND, result);
            case XOR -> singleInstruction(BIT_XOR, result);
            case MOD -> singleInstruction(MODULO, result);
            case SHIFTL -> singleInstruction(BITSHIFT_LEFT, result);
            case SHIFTR -> singleInstruction(BITSHIFT_RIGHT, result);
        }
        if (identifier.startsWith("!PARENT.")) {
            final long absoluteAddress = globalVariableStorage.get(identifier);
            push(result, ByteCode.integer(absoluteAddress));
        } else {
            final long relativeAddress = localVariableStorage.get(identifier);
            push(result, ByteCode.integer(relativeAddress));
            rawInstruction(new ByteCodeInstruction(RELATIVE_TO_ABSOLUTE_ADDRESS.code()), result);
        }
        if (pushMutated) {
            rawInstruction(new ByteCodeInstruction(PUSH.code(), MUTATE_VARIABLE.code()), result);
            return;
        }
        rawInstruction(new ByteCodeInstruction(MUTATE_VARIABLE.code()), result);
    }

    private void compileFunctionCall(@NotNull final Node instr, @NotNull final Collection<ByteCodeInstruction> result) {
        compileFunctionCall(instr.child(0).value().token(), instr.child(1).children(), result);
    }

    private void compileFunctionCall(@NotNull final String identifier, @NotNull final List<Node> inputArgs, @NotNull final Collection<ByteCodeInstruction> result) {
        final long id = findFunctionId(identifier, inputArgs);
        inputArgs.stream().map(n -> n.child(0)).toList().forEach(n -> compileExpression(n, result));
        rawInstruction(ByteCode.call(id), result);
    }

    private long findFunctionId(@NotNull final String fullName, @NotNull final List<Node> inputArgs) {
        final List<ByteDatatype> args = inputArgs
                .stream()
                .map(n -> {
                    final String type = n.child(1).value().token();
                    return ByteDatatype.of(type, findEnumId(type));
                })
                .toList();

        final ByteCodeFunction functionCall = new ByteCodeFunction(fullName, null, args, -1);
        final Optional<ByteCodeFunction> found = functionStorage.stream().filter(f -> f.equals(functionCall)).findFirst();
        if (found.isEmpty()) {
            panic("Cannot find function '" + fullName + "'");
            return 0;
        }
        return found.get().id();
    }

    private void compileNativeFunction(@NotNull final Node instr) {
        final String name = instr.child(0).value().token();
        final String returnType = instr.child(1).value().token();
        final Map<String, ByteDatatype> args = functionDefinitionParams(instr);

        final String javaClass = instr.child(4).value().token();
        final String javaMethod = javaClass.substring(1, javaClass.length() - 1) +
                "." + name + "(" +
                String.join("|", args.values()
                        .stream()
                        .map(d -> d.name().toLowerCase()).toList()
                )
                + ")"
                + ByteDatatype.of(returnType, findEnumId(returnType)).name().toLowerCase();

        compileFunction(instr, javaMethod, null);
    }

    private Map<String, ByteDatatype> functionDefinitionParams(@NotNull final Node instr) {
        return instr
                .child(3)
                .children()
                .stream()
                .map(Node::children)
                .map(n -> {
                    final String type = n.get(0).value().token();
                    final String name = n.get(1).value().token();
                    return Map.entry(name, ByteDatatype.of(type, findEnumId(type)));
                })
                .collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue,
                        (v1,v2)->v1,
                        LinkedHashMap::new));
    }

    private void compileFunction(@NotNull final Node instr, final String javaMethod, final Node scope) {
        final String name = instr.child(0).value().token();
        final String returnType = instr.child(1).value().token();
        final Map<String, ByteDatatype> args = functionDefinitionParams(instr);

        defineFunction(name, returnType, args, javaMethod, scope);
    }

    private ByteDatatype variableDeclarationCommon(@NotNull final Node var) {
        final Token name = var.child(1).value();
        final String typeStr = var.child(2).value().token();
        final ByteDatatype type = ByteDatatype.of(typeStr, findEnumId(typeStr));

        if (compilingFunction()) {
            addLocalVariableToStorage(name.token());
        } else {
            globalVariableStorage.put(currentModuleName() + "." + name.token(), absoluteAddress);
        }
        absoluteAddress++;
        return type;
    }

    private void addLocalVariableToStorage(@NotNull final String name) {
        localVariableStorage.put(name, relativeAddress);
        localScopeVariables.set(this.scope, localScopeVariables.get(this.scope) + 1);
        relativeAddress++;
    }

    private void defineFunction(@NotNull final String name, @NotNull final String returnType, @NotNull final Map<String, ByteDatatype> args, final String javaMethod, final Node scope) {
        functionStorage.add(new ByteCodeFunction(currentModuleName() + "." + name, ByteDatatype.of(returnType, findEnumId(returnType)), args.values(), functionId));
        if (javaMethod == null) {
            relativeAddress = 0;
            this.scope = 0;
            localScopeVariables.add(0);

            args.forEach((s, d) -> addLocalVariableToStorage(s));
            functionDefinitions.add(function(functionId));
            label++;
            if (scope == null) {
                panic("The function scope of '" + name + "' is null");
                return;
            }
            compileParent(scope, functionDefinitions);
            final int vars = !localScopeVariables.isEmpty() ? localScopeVariables.get(this.scope) : 0;
            if (vars > 0) rawInstruction(ByteCode.pop(vars), functionDefinitions);
            localVariableStorage.clear();
            localScopeVariables.clear();
            relativeAddress = -1;
            rawInstruction(new ByteCodeInstruction(FUNCTION_DEFINITION_END.code()), functionDefinitions);
        } else {
            functionDefinitions.add(nativeFunction(functionId, javaMethod));
            label++;
        }
        functionId++;
    }

    private void compileVariableDeclaration(@NotNull final Node definition, @NotNull final Collection<ByteCodeInstruction> result) {
        final ByteDatatype type = variableDeclarationCommon(definition);
        rawInstruction(declareVariable(type), !compilingFunction() ? globalVariables : result);
    }

    private void compileVariableDefinition(@NotNull final Node definition, @NotNull final Collection<ByteCodeInstruction> result) {
        compileExpression(definition.child(3), !compilingFunction() ? globalVariables : result);
        final ByteDatatype type = variableDeclarationCommon(definition);
        rawInstruction(defineVariable(type), !compilingFunction() ? globalVariables : result);
    }

    private void rawInstruction(@NotNull final ByteCodeInstruction instr, @NotNull final Collection<ByteCodeInstruction> result) {
        result.add(instr);
        label++;
    }

    private void compileExpression(final Node node, @NotNull final Collection<ByteCodeInstruction> result) {
        if (node == null) return;
        tree.traceback(node.lineDebugging());
        if (node.children().size() == 1 && node.type() == NodeType.VALUE && node.child(0).type().getAsDataType() != null) {
            ofLiteral(node.child(0), result);
            return;
        } else if (node.children().isEmpty() && node.type().getAsDataType() != null) {
            ofLiteral(node, result);
            return;
        }
        operator(node.type(), node.children(), node.value(), result);
    }

    private void push(@NotNull final Collection<ByteCodeInstruction> result, @NotNull final Byte... bytes) {
        rawInstruction(ByteCode.push(bytes), result);
    }

    private void push(@NotNull final Collection<ByteCodeInstruction> result, @NotNull final ByteCodeInstruction instr) {
        push(result, instr.codes());
    }

    private void ofLiteral(@NotNull final Node node, @NotNull final Collection<ByteCodeInstruction> result) {
        final String value = node.value().token();
        final String type = node.type().getAsDataType().getName();
        switch (ByteDatatype.of(type, findEnumId(type)).name()) {
            case "bool" -> push(result, bool(value));
            case "string" -> push(result, string(value.substring(1, value.length() - 1)));
            case "double" -> push(result, floating(value));
            case "float" -> push(result, doubleFloating(value));
            case "long" -> push(result, longInteger(value));
            case "int" -> push(result, integer(value));
            case "char" -> push(result, character(value));
            default -> panic("Unexpected datatype '" + type + "'");
        }
    }

    private void operator(@NotNull final NodeType op, @NotNull final List<Node> values, final Token nodeVal, @NotNull final Collection<ByteCodeInstruction> result) {
        final Node x = values.size() > 0 ? values.get(0) : null;
        final Node y = values.size() > 1 ? values.get(1) : null;

        switch (op) {
            case DIVIDE -> operator(x, y, DIVIDE, result);
            case MULTIPLY -> operator(x, y, MULTIPLY, result);
            case ADD -> operator(x, y, PLUS, result);
            case SUBTRACT -> operator(x, y, MINUS, result);
            case MODULUS -> operator(x, y, MODULO, result);
            case LOGICAL_AND -> operator(x, y, LOGICAL_AND, result);
            case LOGICAL_OR -> operator(x, y, LOGICAL_OR, result);
            case XOR ->  operator(x, y, BIT_XOR, result);
            case BIT_AND -> operator(x, y, BIT_AND, result);
            case BIT_OR -> operator(x, y, BIT_OR, result);
            case LSHIFT -> operator(x, y, BITSHIFT_LEFT, result);
            case RSHIFT -> operator(x, y, BITSHIFT_RIGHT, result);
            case LESS_THAN -> operator(x, y, LESS_THAN, result);
            case LESS_THAN_EQ -> operator(x, y, LESS_THAN_OR_EQUAL, result);
            case GREATER_THAN -> operator(x, y, GREATER_THAN, result);
            case GREATER_THAN_EQ -> operator(x, y, GREATER_THAN_OR_EQUAL, result);
            case EQUALS -> operator(x, y, EQUALS, result);
            case NOTEQUALS -> {
                operator(x, y, EQUALS, result);
                rawInstruction(new ByteCodeInstruction(NOT.code()), result);
            }
            case BOOL_NOT -> {
                compileExpression(new Node(NodeType.VALUE, values), result);
                rawInstruction(new ByteCodeInstruction(NOT.code()), result);
            }
            case NEGATE -> operator(Node.of(Token.of("0")), x, MINUS, result);
            case GET_ENUM_MEMBER -> {
                final String identifier = values.get(0).value().token();
                final String member = values.get(1).value().token();
                final ByteCodeEnum enumDef = findEnum(identifier);
                if (enumDef == null) {
                    panic("Cannot find enum '" + identifier + "'");
                    return;
                }
                rawInstruction(ByteCode.enumMember(new ByteCodeEnumMember(enumDef.id(), enumDef.ordinalMember(member))), result);
            }/*
            case TERNARY_OPERATOR -> {
                final boolean ternaryCondition = isTrue(compileExpression(values.get(0).child(0)).getValue());
                if (ternaryCondition) yield compileExpression(values.get(1).child(0));
                yield compileExpression(values.get(2).child(0));
            }*/
            case IDENTIFIER -> {
                final String identifier = nodeVal.token();
                if (identifier.startsWith("!PARENT.")) {
                    final long absoluteAddress = globalVariableStorage.get(identifier);
                    push(result, ByteCode.integer(absoluteAddress));
                    rawInstruction(new ByteCodeInstruction(VALUE_AT_ADDRESS.code()), result);
                    return;
                }
                final long relativeAddress = localVariableStorage.get(identifier);
                push(result, ByteCode.integer(relativeAddress));
                rawInstruction(new ByteCodeInstruction(RELATIVE_TO_ABSOLUTE_ADDRESS.code()), result);
                rawInstruction(new ByteCodeInstruction(VALUE_AT_ADDRESS.code()), result);
            }
            case CAST_VALUE -> {
                compileExpression(new Node(NodeType.VALUE, values), result);
                rawInstruction(cast(ByteDatatype.of(nodeVal.token(), findEnumId(nodeVal.token()))), result);
            }
            case FUNCTION_CALL -> {
                final String name = values.get(0).value().token();
                final List<Node> args = values.get(1).children();
                compileFunctionCall(name, args, result);
            }
            case VAR_SET_VALUE -> compileVariableMutation(new Node(op, values), true, result);
            case VALUE -> operator(values.get(0).type(), values.get(0).children(), values.get(0).value(), result);
            default -> panic("Could not parse expression (failed at " + op + ")");
        }
    }

    private void operator(final Node v1, final Node v2, final ByteCode op, @NotNull final Collection<ByteCodeInstruction> result) {
        compileExpression(v1, result);
        compileExpression(v2, result);
        rawInstruction(new ByteCodeInstruction(op.code()), result);
    }

    private static String sanitize(@NotNull final String instr) {
        return instr.replace("\r", "[CR]");
    }

    public String toString() {
        return String.join("\n", result.stream().map(ByteCodeInstruction::toString).toList())
                + "\n----------------------------------\n"
                + String.join("", result.stream().map(b -> sanitize(b.write())).toList());
    }
}
