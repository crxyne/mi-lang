package org.crayne.mi.parsing.ast;

import org.crayne.mi.parsing.lexer.Token;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Node {

    private final List<Node> children;
    private final Node parent;
    private final NodeType type;
    private final Token value;
    private final int lineDebugging;

    public static Node of(@NotNull final NodeType type, final int lineDebugging) {
        return new Node(type, lineDebugging);
    }
    public static Node of(@NotNull final Token token) {
        return new Node(token);
    }

    public Node(@NotNull final NodeType type, final Token value, final int lineDebugging) {
        this.type = type;
        this.value = value;
        this.parent = null;
        this.lineDebugging = lineDebugging;
        children = new ArrayList<>();
    }

    public Node(@NotNull final NodeType type, final int lineDebugging) {
        this.type = type;
        this.value = null;
        this.parent = null;
        this.lineDebugging = lineDebugging;
        children = new ArrayList<>();
    }

    public Node(@NotNull final Token token) {
        this.type = NodeType.of(token);
        this.value = token;
        this.parent = null;
        this.lineDebugging = token.actualLine();
        children = new ArrayList<>();
    }

    public Node(@NotNull final NodeType type, final int lineDebugging, final Token value, @NotNull final Collection<Node> children) {
        this.type = type;
        this.value = value;
        this.parent = null;
        this.lineDebugging = lineDebugging;
        this.children = new ArrayList<>();
        this.children.addAll(children);
    }

    public Node(@NotNull final NodeType type, final int lineDebugging, @NotNull final Collection<Node> children) {
        this.type = type;
        this.value = null;
        this.parent = null;
        this.lineDebugging = lineDebugging;
        this.children = new ArrayList<>();
        this.children.addAll(children);
    }

    public Node(@NotNull final NodeType type, final Token value, final int lineDebugging, @NotNull final Node... children) {
        this.type = type;
        this.value = value;
        this.parent = null;
        this.lineDebugging = lineDebugging;
        this.children = new ArrayList<>();
        this.children.addAll(List.of(children));
    }

    public Node(@NotNull final NodeType type, final int lineDebugging, @NotNull final Node... children) {
        this.type = type;
        this.value = null;
        this.parent = null;
        this.lineDebugging = lineDebugging;
        this.children = new ArrayList<>();
        this.children.addAll(List.of(children));
    }

    public Node(@NotNull final Node parent, @NotNull final NodeType type, final int lineDebugging, @NotNull final Node... children) {
        this.type = type;
        this.value = null;
        this.parent = parent;
        this.lineDebugging = lineDebugging;
        this.children = new ArrayList<>();
        this.children.addAll(List.of(children));
    }

    public Node(@NotNull final Node parent, @NotNull final NodeType type, final int lineDebugging, @NotNull final Token value, @NotNull final Node... children) {
        this.type = type;
        this.value = value;
        this.parent = parent;
        this.lineDebugging = lineDebugging;
        this.children = new ArrayList<>();
        this.children.addAll(List.of(children));
    }

    public Node(@NotNull final Node parent, @NotNull final NodeType type, final int lineDebugging, @NotNull final Collection<Node> children) {
        this.type = type;
        this.value = null;
        this.parent = parent;
        this.lineDebugging = lineDebugging;
        this.children = new ArrayList<>();
        this.children.addAll(children);
    }

    public Node parent() {
        return parent;
    }

    public int lineDebugging() {
        return lineDebugging;
    }

    public void addChildren(@NotNull final Collection<Node> children) {
        this.children.addAll(children);
    }

    public void addChildren(@NotNull final Node... children) {
        this.children.addAll(List.of(children));
    }

    public Node child(final int index) {
        return children.get(index);
    }

    public Token value() {
        return value;
    }

    public NodeType type() {
        return type;
    }

    public List<Node> children() {
        return children;
    }

    public String toString() {
        final StringBuilder result = new StringBuilder(type.name());
        if (value != null) {
            result
                    .append(value.line() > 0 ? " [" + value.line() : "")
                    .append(value.column() > 0 ? ":" + value.column() + "" : "")
                    .append(value.line() > 0 ? "]" : "")
                    .append(" -> ").append(value.token());
        }
        if (!children.isEmpty()) {
            result.append(" [ \n");
            for (final Node child : children) {
                result.append(child.toString().indent(4));
            }
            result.append("]");
        } else if (value == null && !type.name().startsWith("LITERAL_")) {
            result.append(" []");
        }
        return result.toString();
    }

}
