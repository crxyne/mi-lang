package org.crayne.mu.bytecode.writer;

import org.crayne.mu.bytecode.common.ByteCode;
import org.crayne.mu.bytecode.common.ByteCodeFunction;
import org.crayne.mu.bytecode.common.ByteCodeInstruction;
import org.crayne.mu.bytecode.common.ByteDatatype;
import org.crayne.mu.bytecode.common.errorhandler.Traceback;
import org.crayne.mu.bytecode.common.errorhandler.TracebackElement;
import org.crayne.mu.log.MessageHandler;
import org.crayne.mu.parsing.ast.Node;
import org.crayne.mu.parsing.ast.NodeType;
import org.crayne.mu.parsing.lexer.Token;
import org.crayne.mu.runtime.SyntaxTreeExecution;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static org.crayne.mu.bytecode.common.ByteCode.*;

public class ByteCodeCompiler {

    private final MessageHandler messageHandler;
    private final List<ByteCodeInstruction> result;
    private final List<ByteCodeInstruction> functionDefinitions;
    private final Set<ByteCodeInstruction> enumDefinitions;
    private final SyntaxTreeExecution tree;

    private final Map<String, Long> globalVariableStorage;
    private final Map<String, Long> localVariableStorage;
    private final Set<ByteCodeFunction> functionStorage;

    private final List<String> currentModuleName = new ArrayList<>() {{this.add("!PARENT");}};

    private long absoluteAddress = 1;
    private long relativeAddress = -1;
    private long functionId = 0;

    private final Traceback traceback;
    public TracebackElement newTracebackElement(final int line) {
        return new TracebackElement(this, line);
    }

    public void traceback(final int... lines) {
        for (final int line : lines) traceback.add(newTracebackElement(line));
    }

    public ByteCodeCompiler(@NotNull final MessageHandler messageHandler, @NotNull final SyntaxTreeExecution tree) {
        this.messageHandler = messageHandler;
        this.traceback = new Traceback();
        this.tree = tree;
        globalVariableStorage = new HashMap<>();
        localVariableStorage = new HashMap<>();
        functionStorage = new HashSet<>();
        enumDefinitions = new HashSet<>();
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
        messageHandler.errorMsg("Encountered fatal compiler error: " + message);
    }

    private boolean compilingFunction() {
        return relativeAddress < 0;
    }

    private String currentModuleName() {
        return String.join(".", currentModuleName);
    }

    public List<ByteCodeInstruction> compile() {
        final Node ast = tree.getAST();
        traceback(ast.lineDebugging());
        if (ast.type() == NodeType.PARENT) {
            compileParent(ast, result);
        } else {
            panic("Expected 'PARENT' node at the beginning of syntax tree");
            return new ArrayList<>();
        }
        result.addAll(0, enumDefinitions);
        result.addAll(0, functionDefinitions);
        result.add(0, header());
        return result;
    }

    public static void compileToFile(@NotNull final List<ByteCodeInstruction> bytecode, @NotNull final File file) throws IOException {
        Files.writeString(file.toPath(), String.join("", bytecode.stream().map(b -> b.write()).toList()));
    }

    private void compileParent(@NotNull final Node parent, @NotNull final List<ByteCodeInstruction> result) {
        for (final Node instruction : parent.children()) {
            switch (instruction.type()) {
                case VAR_DEF_AND_SET_VALUE -> compileVariableDefinition(instruction, result);
                case VAR_DEFINITION -> compileVariableDeclaration(instruction, result);
                case CREATE_MODULE -> {
                    currentModuleName.add(instruction.child(0).value().token());
                    compileParent(instruction.child(1), result);
                    currentModuleName.remove(currentModuleName.size() - 1);
                }
                case NATIVE_FUNCTION_DEFINITION -> compileNativeFunction(instruction);
                case FUNCTION_DEFINITION -> compileFunction(instruction, null, instruction.child(4), result);
                case FUNCTION_CALL -> compileFunctionCall(instruction, result);
                default -> System.out.println("ignored instruction: " + instruction);
            }
        }
    }

    private void compileFunctionCall(@NotNull final Node instr, @NotNull final List<ByteCodeInstruction> result) {
        final long id = findFunctionId(instr.child(0).value().token(), instr.child(1).children());
        instr.child(1).children().stream().map(n -> n.child(0)).toList().forEach(n -> compileExpression(n, result));
        rawInstruction(ByteCode.call(id), result);
    }

    private long findFunctionId(@NotNull final String fullName, @NotNull final List<Node> inputArgs) {
        final List<ByteDatatype> args = inputArgs
                .stream()
                .map(n -> ByteDatatype.of(n.child(1).value().token()))
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
        final List<ByteDatatype> args = functionDefinitionParams(instr);

        final String javaClass = instr.child(4).value().token();
        final String javaMethod = javaClass.substring(1, javaClass.length() - 1) +
                "." + name + "(" +
                String.join("|", args
                        .stream()
                        .map(d -> d.name().toLowerCase()).toList()
                )
                + ")"
                + ByteDatatype.of(returnType).name().toLowerCase();

        compileFunction(instr, javaMethod, null, new ArrayList<>());
    }

    private List<ByteDatatype> functionDefinitionParams(@NotNull final Node instr) {
        return instr
                .child(3)
                .children()
                .stream()
                .map(Node::children)
                .map(n -> ByteDatatype.of(n.get(0).value().token()))
                .toList();
    }

    private void compileFunction(@NotNull final Node instr, final String javaMethod, final Node scope, @NotNull final List<ByteCodeInstruction> result) {
        final String name = instr.child(0).value().token();
        final String returnType = instr.child(1).value().token();
        final List<ByteDatatype> args = functionDefinitionParams(instr);

        defineFunction(name, returnType, args, javaMethod, scope, result);
    }

    private ByteDatatype variableDeclarationCommon(@NotNull final Node var) {
        final Token name = var.child(1).value();
        final ByteDatatype type = ByteDatatype.of(var.child(2).value().token());

        if (compilingFunction()) {
            localVariableStorage.put(name.token(), relativeAddress);
            relativeAddress++;
        } else {
            globalVariableStorage.put(name.token(), absoluteAddress);
        }
        absoluteAddress++;
        return type;
    }

    private void defineFunction(@NotNull final String name, @NotNull final String returnType, @NotNull final List<ByteDatatype> args, final String javaMethod, final Node scope, @NotNull final List<ByteCodeInstruction> result) {
        functionStorage.add(new ByteCodeFunction(currentModuleName() + "." + name, ByteDatatype.of(returnType), args, functionId));
        if (javaMethod == null) {
            functionDefinitions.add(function(functionId));
            if (scope == null) {
                panic("The function scope of '" + name + "' is null");
                return;
            }
            compileParent(scope, functionDefinitions);
            rawInstruction(new ByteCodeInstruction(FUNCTION_DEFINITION_END.code()), functionDefinitions);
        } else {
            functionDefinitions.add(nativeFunction(functionId, javaMethod));
        }
        functionId++;
    }

    private void compileVariableDeclaration(@NotNull final Node definition, @NotNull final List<ByteCodeInstruction> result) {
        final ByteDatatype type = variableDeclarationCommon(definition);
        rawInstruction(declareVariable(type), result);
    }

    private void compileVariableDefinition(@NotNull final Node definition, @NotNull final List<ByteCodeInstruction> result) {
        compileExpression(definition.child(3), result);
        final ByteDatatype type = variableDeclarationCommon(definition);
        rawInstruction(defineVariable(type), result);
    }

    private void rawInstruction(@NotNull final ByteCodeInstruction instr, @NotNull final List<ByteCodeInstruction> result) {
        result.add(instr);
    }

    private void compileExpression(final Node node, @NotNull final List<ByteCodeInstruction> result) {
        if (node == null) return;
        traceback(node.lineDebugging());
        if (node.children().size() == 1 && node.type() == NodeType.VALUE && node.child(0).type().getAsDataType() != null) {
            ofLiteral(node.child(0), result);
            return;
        } else if (node.children().isEmpty() && node.type().getAsDataType() != null) {
            ofLiteral(node, result);
            return;
        }
        operator(node.type(), node.children(), node.value(), result);
    }

    private void push(@NotNull final List<ByteCodeInstruction> result, @NotNull final Byte... bytes) {
        rawInstruction(ByteCode.push(bytes), result);
    }

    private void push(@NotNull final List<ByteCodeInstruction> result, @NotNull final ByteCodeInstruction instr) {
        push(result, instr.codes());
    }

    private void ofLiteral(@NotNull final Node node, @NotNull final List<ByteCodeInstruction> result) {
        final String value = node.value().token();
        final String type = node.type().getAsDataType().getName();
        switch (ByteDatatype.of(type).name()) {
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

    private void operator(@NotNull final NodeType op, @NotNull final List<Node> values, final Token nodeVal, @NotNull final List<ByteCodeInstruction> result) {
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
            case NEGATE -> operator(Node.of(Token.of("0")), x, MINUS, result);
            /*case GET_ENUM_MEMBER -> {
                final String identifier = values.get(0).value().token();
                final String member = values.get(1).value().token();
                yield new RValue(RDatatype.of(identifier), member);
            }
            case TERNARY_OPERATOR -> {
                final boolean ternaryCondition = isTrue(compileExpression(values.get(0).child(0)).getValue());
                if (ternaryCondition) yield compileExpression(values.get(1).child(0));
                yield compileExpression(values.get(2).child(0));
            }
            case IDENTIFIER -> {
                final Optional<RVariable> find = MuUtil.findVariable(tree, nodeVal.token());
                if (find.isEmpty()) {
                    tree.runtimeError("Cannot find variable '" + nodeVal.token() + "'");
                    yield null;
                }
                yield new RValue(find.get().getType(), find.get().getValue().getValue());
            }*/
            case CAST_VALUE -> rawInstruction(cast(ByteDatatype.of(nodeVal.token())), result);
            /*case FUNCTION_CALL -> tree.functionCall(new Node(op, values));
            case VAR_SET_VALUE -> tree.variableSetValue(new Node(op, values));*/
            case VALUE -> operator(values.get(0).type(), values.get(0).children(), values.get(0).value(), result);
            default -> panic("Could not parse expression (failed at " + op + ")");
        };
    }

    private void operator(final Node v1, final Node v2, final ByteCode op, @NotNull final List<ByteCodeInstruction> result) {
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