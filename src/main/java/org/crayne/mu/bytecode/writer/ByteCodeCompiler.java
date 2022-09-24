package org.crayne.mu.bytecode.writer;

import org.crayne.mu.bytecode.common.ByteCode;
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

import java.util.*;

import static org.crayne.mu.bytecode.common.ByteCode.*;

public class ByteCodeCompiler {

    private final MessageHandler messageHandler;
    private final List<ByteCodeInstruction> result;
    private final Set<ByteCodeInstruction> functionDefinitions;
    private final Set<ByteCodeInstruction> enumDefinitions;
    private final SyntaxTreeExecution tree;

    private final Map<String, Long> globalVariableStorage;
    private final Map<String, Long> localVariableStorage;
    private final Map<String, Long> functionStorage;

    private final List<String> currentModuleName = new ArrayList<>();

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
        functionStorage = new HashMap<>();
        enumDefinitions = new HashSet<>();
        functionDefinitions = new HashSet<>();
        result = new ArrayList<>() {{this.add(header());}};
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
            compileParent(ast);
        } else {
            panic("Expected 'PARENT' node at the beginning of syntax tree");
            return new ArrayList<>();
        }
        result.addAll(functionDefinitions);
        result.addAll(enumDefinitions);
        return result;
    }

    private void compileParent(@NotNull final Node parent) {
        for (final Node instruction : parent.children()) {
            switch (instruction.type()) {
                case VAR_DEF_AND_SET_VALUE -> compileVariableDefinition(instruction);
                case VAR_DEFINITION -> compileVariableDeclaration(instruction);
                case CREATE_MODULE -> {
                    currentModuleName.add(instruction.child(0).value().token());
                    compileParent(instruction.child(1));
                    currentModuleName.remove(currentModuleName.size() - 1);
                }
                case NATIVE_FUNCTION_DEFINITION -> {
                    final String name = instruction.child(0).value().token();
                    final String returnType = instruction.child(1).value().token();
                    final List<ByteDatatype> args = instruction
                            .child(3)
                            .children()
                            .stream()
                            .map(Node::children)
                            .map(n -> ByteDatatype.of(n.get(0).value().token()))
                            .toList();

                    final String javaClass = instruction.child(4).value().token();
                    defineFunction(name, returnType, args,
                            javaClass.substring(1, javaClass.length() - 1) +
                                    "." + name + "(" +
                                    String.join("|", args
                                            .stream()
                                            .map(d -> d.name().toLowerCase()).toList()
                                    )
                                    + ")"
                                    + ByteDatatype.of(returnType).code());
                }
                default -> System.out.println("ignored instruction: " + instruction);
            }
        }
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

    private void defineFunction(@NotNull final String name, @NotNull final String returnType, @NotNull final List<ByteDatatype> args, final String javaMethod) {
        functionStorage.put(currentModuleName() + "." + name + "@" + returnType + ":" + String.join("", args.stream().map(d -> String.valueOf(d.code())).toList()), functionId);
        functionDefinitions.add(javaMethod == null ? function(functionId) : nativeFunction(functionId, javaMethod));
        functionId++;
    }

    private void compileVariableDeclaration(@NotNull final Node definition) {
        final ByteDatatype type = variableDeclarationCommon(definition);
        rawInstruction(declareVariable(type));
    }

    private void compileVariableDefinition(@NotNull final Node definition) {
        compileExpression(definition.child(3));
        final ByteDatatype type = variableDeclarationCommon(definition);
        rawInstruction(defineVariable(type));
    }

    private void rawInstruction(@NotNull final ByteCodeInstruction instr) {
        result.add(instr);
    }

    private void compileExpression(final Node node) {
        if (node == null) return;
        traceback(node.lineDebugging());
        if (node.children().size() == 1 && node.type() == NodeType.VALUE && node.child(0).type().getAsDataType() != null) {
            ofLiteral(node.child(0));
            return;
        } else if (node.children().isEmpty() && node.type().getAsDataType() != null) {
            ofLiteral(node);
            return;
        }
        operator(node.type(), node.children(), node.value());
    }

    private void push(@NotNull final Byte... bytes) {
        rawInstruction(ByteCode.push(bytes));
    }

    private void push(@NotNull final ByteCodeInstruction instr) {
        push(instr.codes());
    }

    private void ofLiteral(@NotNull final Node node) {
        final String value = node.value().token();
        final String type = node.type().getAsDataType().getName();
        switch (ByteDatatype.of(type)) {
            case BOOL -> push(bool(value));
            case STRING -> push(string(value));
            case DOUBLE -> push(floating(value));
            case FLOAT -> push(doubleFloating(value));
            case LONG -> push(longInteger(value));
            case INT -> push(integer(value));
            case CHAR -> push(character(value));
            default -> panic("Unexpected datatype '" + type + "'");
        }
    }

    private void operator(@NotNull final NodeType op, @NotNull final List<Node> values, final Token nodeVal) {
        final Node x = values.size() > 0 ? values.get(0) : null;
        final Node y = values.size() > 1 ? values.get(1) : null;

        switch (op) {
            case DIVIDE -> operator(x, y, DIVIDE);
            case MULTIPLY -> operator(x, y, MULTIPLY);
            case ADD -> operator(x, y, PLUS);
            case SUBTRACT -> operator(x, y, MINUS);
            case MODULUS ->  operator(x, y, MODULO);
            case LOGICAL_AND ->  operator(x, y, LOGICAL_AND);
            case LOGICAL_OR ->  operator(x, y, LOGICAL_OR);
            case XOR ->  operator(x, y, BIT_XOR);
            case BIT_AND ->  operator(x, y, BIT_AND);
            case BIT_OR ->  operator(x, y, BIT_OR);
            case LSHIFT ->  operator(x, y, BITSHIFT_LEFT);
            case RSHIFT -> operator(x, y, BITSHIFT_RIGHT);
            case LESS_THAN -> operator(x, y, LESS_THAN);
            case LESS_THAN_EQ -> operator(x, y, LESS_THAN_OR_EQUAL);
            case GREATER_THAN -> operator(x, y, GREATER_THAN);
            case GREATER_THAN_EQ -> operator(x, y, GREATER_THAN_OR_EQUAL);
            case EQUALS -> operator(x, y, EQUALS);
            case NOTEQUALS -> {
                operator(x, y, EQUALS);
                rawInstruction(new ByteCodeInstruction(NOT.code()));
            }
            case NEGATE -> operator(Node.of(Token.of("0")), x, MINUS);
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
            case CAST_VALUE -> rawInstruction(cast(ByteDatatype.of(nodeVal.token())));
            /*case FUNCTION_CALL -> tree.functionCall(new Node(op, values));
            case VAR_SET_VALUE -> tree.variableSetValue(new Node(op, values));*/
            case VALUE -> operator(values.get(0).type(), values.get(0).children(), values.get(0).value());
            default -> panic("Could not parse expression (failed at " + op + ")");
        };
    }

    private void operator(final Node v1, final Node v2, final ByteCode op) {
        compileExpression(v1);
        compileExpression(v2);
        rawInstruction(new ByteCodeInstruction(op.code()));
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
