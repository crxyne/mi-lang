package org.crayne.mu.runtime.lang;

import org.crayne.mu.lang.Variable;
import org.crayne.mu.parsing.ast.Node;
import org.crayne.mu.runtime.SyntaxTreeExecution;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RVariable {

    private final String name;
    private final RDatatype type;
    private RValue value;
    private final Node nodeValue;

    public RVariable(@NotNull final String name, @NotNull final RDatatype datatype, final Node nodeValue) {
        this.name = name;
        this.type = datatype;
        this.nodeValue = nodeValue;
    }

    public RVariable(@NotNull final String name, @NotNull final RDatatype datatype, final RValue value, final Node nodeValue) {
        this.name = name;
        this.type = datatype;
        this.value = value;
        this.nodeValue = nodeValue;
    }

    public RDatatype getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public RValue getValue() {
        return value;
    }

    public void setValue(final RValue value) {
        this.value = value;
    }

    public static RVariable of(@NotNull final Variable v) {
        return new RVariable(v.name(), new RDatatype(v.type().getName()), v.node());
    }

    public static RVariable of(@NotNull final SyntaxTreeExecution tree, @NotNull final Node varDefinition) {
        final List<Node> values = varDefinition.children();
        tree.traceback(values.get(1).lineDebugging(), values.get(2).lineDebugging());

        final String name = values.get(1).value().token();
        final RDatatype type = new RDatatype(values.get(2).value().token());

        if (values.size() == 3) return new RVariable(name, type, null); // no value specified in AST

        final RValue value = tree.getEvaluator().evaluateExpression(values.get(3));
        return new RVariable(name, type, value, values.get(3));
    }

    public Node getNodeValue() {
        return nodeValue;
    }

    @Override
    public String toString() {
        return type.getName() + " " + name + " = " + value;
    }
}
