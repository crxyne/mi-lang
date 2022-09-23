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
import org.crayne.mu.parsing.lexer.Tokenizer;
import org.crayne.mu.runtime.SyntaxTreeExecution;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ByteCodeCompiler {

    private final MessageHandler messageHandler;
    private final List<ByteCodeInstruction> result;
    private final SyntaxTreeExecution tree;

    private final Map<String, Long> globalVariableStorage;
    private final Map<String, Long> localVariableStorage;

    private long absoluteAddress = 1;
    private long relativeAddress = -1;

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
        result = new ArrayList<>() {{this.add(ByteCode.PROGRAM_HEADER.header());}};
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

    public void compile() {
        final Node ast = tree.getAST();
        traceback(ast.lineDebugging());
        if (ast.type() == NodeType.PARENT) {
            compileParent(ast);
        } else {
            panic("Expected 'PARENT' node at the beginning of syntax tree");
            return;
        }
        System.out.println(this);
    }

    private void compileParent(@NotNull final Node parent) {
        for (final Node instruction : parent.children()) {
            switch (instruction.type()) {
                case VAR_DEF_AND_SET_VALUE -> compileVariableDefinition(instruction);
                case VAR_DEFINITION -> compileVariableDeclaration(instruction);
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

    private void compileVariableDeclaration(@NotNull final Node definition) {
        final ByteDatatype type = variableDeclarationCommon(definition);
        rawInstruction(ByteCode.DECLARE_VARIABLE.declareVariable(type));
    }

    private void compileVariableDefinition(@NotNull final Node definition) {
        compileExpression(definition.child(3));
        final ByteDatatype type = variableDeclarationCommon(definition);
        rawInstruction(ByteCode.DEFINE_VARIABLE.defineVariable(type));
    }

    private void rawInstruction(@NotNull final ByteCodeInstruction instr) {
        result.add(instr);
    }

    private void compileExpression(final Node node) {
        if (node == null) return;
        tree.traceback(node.lineDebugging());
        if (node.children().size() == 1 && node.type() == NodeType.VALUE && node.child(0).type().getAsDataType() != null) {
            ofLiteral(node.child(0));
            return;
        } else if (node.children().isEmpty() && node.type().getAsDataType() != null) {
            ofLiteral(node);
            return;
        }
        operator(node.type(), node.children(), node.value());
    }

    private void ofLiteral(@NotNull final Node node) {
        final String value = node.value().token();
        final String type = node.type().getAsDataType().getName();
        switch (ByteDatatype.of(type)) {
            case BOOL -> rawInstruction(ByteCode.PUSH.push(ByteCode.INTEGER_VALUE.integer(value.equals("1b") ? 1L : 0L).codes()));
            case STRING -> rawInstruction(ByteCode.PUSH.push(ByteCode.STRING_VALUE.string(value).codes()));
            case DOUBLE -> rawInstruction(ByteCode.PUSH.push(ByteCode.FLOAT_VALUE.decimal(Tokenizer.isDouble(value) != null ? Double.parseDouble(value) : 0d).codes()));
            case FLOAT -> rawInstruction(ByteCode.PUSH.push(ByteCode.FLOAT_VALUE.decimal(Tokenizer.isFloat(value) != null ? Float.parseFloat(value) : 0f).codes()));
            case LONG -> rawInstruction(ByteCode.PUSH.push(ByteCode.INTEGER_VALUE.integer(Tokenizer.isLong(value) != null ? Long.parseLong(value) : 0L).codes()));
            case INT -> rawInstruction(ByteCode.PUSH.push(ByteCode.INTEGER_VALUE.integer(Tokenizer.isInt(value) != null ? Integer.parseInt(value) : 0).codes()));
            case CHAR -> rawInstruction(ByteCode.PUSH.push(ByteCode.INTEGER_VALUE.integer((value.startsWith("'") ? value.charAt(1) : Integer.parseInt(value))).codes()));
            default -> panic("Unexpected datatype '" + type + "'");
        }
    }

    private void operator(@NotNull final NodeType op, @NotNull final List<Node> values, final Token nodeVal) {
        final Node x = values.size() > 0 ? values.get(0) : null;
        final Node y = values.size() > 1 ? values.get(1) : null;
        switch (op) {
            case DIVIDE -> divide(x, y);
            case MULTIPLY -> multiply(x, y);
            case ADD -> add(x, y);
            case SUBTRACT -> subtract(x, y);
            case MODULUS -> modulus(x, y);
            case LOGICAL_AND -> logicalAnd(x, y);
            case LOGICAL_OR -> logicalOr(x, y);
            case XOR -> bitXor(x, y);
            case BIT_AND -> bitAnd(x, y);
            case BIT_OR -> bitOr(x, y);
            case LSHIFT -> bitShiftLeft(x, y);
            case RSHIFT -> bitShiftRight(x, y);
            case LESS_THAN -> lessThan(x, y);
            case LESS_THAN_EQ -> lessThanOrEquals(x, y);
            case GREATER_THAN -> greaterThan(x, y);
            case GREATER_THAN_EQ -> greaterThanOrEquals(x, y);
            case EQUALS -> equals(x, y);
            case NOTEQUALS -> notEquals(x, y);
            case NEGATE -> subtract(Node.of(Token.of("0")), x);
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
            case CAST_VALUE -> rawInstruction(ByteCode.CAST.cast(ByteDatatype.of(nodeVal.token())));
            /*case FUNCTION_CALL -> tree.functionCall(new Node(op, values));
            case VAR_SET_VALUE -> tree.variableSetValue(new Node(op, values));*/
            case VALUE -> operator(values.get(0).type(), values.get(0).children(), values.get(0).value());
            default -> panic("Could not parse expression (failed at " + op + ")");
        };
    }

    public void multiply(final Node v1, final Node v2) {
        compileExpression(v1);
        compileExpression(v2);
        rawInstruction(new ByteCodeInstruction(ByteCode.MULTIPLY.code()));
    }

    public void divide(final Node v1, final Node v2) {
        compileExpression(v1);
        compileExpression(v2);
        rawInstruction(new ByteCodeInstruction(ByteCode.DIVIDE.code()));
    }

    private void subtract(final Node v1, final Node v2) {
        compileExpression(v1);
        compileExpression(v2);
        rawInstruction(new ByteCodeInstruction(ByteCode.MINUS.code()));
    }

    private void add(final Node v1, final Node v2) {
        compileExpression(v1);
        compileExpression(v2);
        rawInstruction(new ByteCodeInstruction(ByteCode.PLUS.code()));
    }

    private void modulus(final Node v1, final Node v2) {
        compileExpression(v1);
        compileExpression(v2);
        rawInstruction(new ByteCodeInstruction(ByteCode.MODULO.code()));
    }

    private void equals(final Node v1, final Node v2) {
        compileExpression(v1);
        compileExpression(v2);
        rawInstruction(new ByteCodeInstruction(ByteCode.EQUALS.code()));
    }

    private void notEquals(final Node v1, final Node v2) {
        equals(v1, v2);
        rawInstruction(new ByteCodeInstruction(ByteCode.NOT.code()));
    }

    private void lessThan(final Node v1, final Node v2) {
        compileExpression(v1);
        compileExpression(v2);
        rawInstruction(new ByteCodeInstruction(ByteCode.LESS_THAN.code()));
    }

    private void greaterThan(final Node v1, final Node v2) {
        compileExpression(v1);
        compileExpression(v2);
        rawInstruction(new ByteCodeInstruction(ByteCode.GREATER_THAN.code()));
    }

    private void lessThanOrEquals(final Node v1, final Node v2) {
        compileExpression(v1);
        compileExpression(v2);
        rawInstruction(new ByteCodeInstruction(ByteCode.LESS_THAN_OR_EQUAL.code()));
    }

    private void greaterThanOrEquals(final Node v1, final Node v2) {
        compileExpression(v1);
        compileExpression(v2);
        rawInstruction(new ByteCodeInstruction(ByteCode.GREATER_THAN_OR_EQUAL.code()));
    }

    private void logicalAnd(final Node v1, final Node v2) {
        compileExpression(v1);
        compileExpression(v2);
        rawInstruction(new ByteCodeInstruction(ByteCode.LOGICAL_AND.code()));
    }

    private void logicalOr(final Node v1, final Node v2) {
        compileExpression(v1);
        compileExpression(v2);
        rawInstruction(new ByteCodeInstruction(ByteCode.LOGICAL_OR.code()));
    }

    private void bitXor(final Node v1, final Node v2) {
        compileExpression(v1);
        compileExpression(v2);
        rawInstruction(new ByteCodeInstruction(ByteCode.BIT_XOR.code()));
    }

    private void bitAnd(final Node v1, final Node v2) {
        compileExpression(v1);
        compileExpression(v2);
        rawInstruction(new ByteCodeInstruction(ByteCode.BIT_AND.code()));
    }

    private void bitOr(final Node v1, final Node v2) {
        compileExpression(v1);
        compileExpression(v2);
        rawInstruction(new ByteCodeInstruction(ByteCode.BIT_OR.code()));
    }

    private void bitShiftLeft(final Node v1, final Node v2) {
        compileExpression(v1);
        compileExpression(v2);
        rawInstruction(new ByteCodeInstruction(ByteCode.BITSHIFT_LEFT.code()));
    }

    private void bitShiftRight(final Node v1, final Node v2) {
        compileExpression(v1);
        compileExpression(v2);
        rawInstruction(new ByteCodeInstruction(ByteCode.BITSHIFT_RIGHT.code()));
    }

    public String toString() {
        return String.join("\n", result.stream().map(ByteCodeInstruction::toString).toList())
                + "\n----------------------------------\n"
                + String.join("", result.stream().map(b -> b.write()).toList());
    }
}
