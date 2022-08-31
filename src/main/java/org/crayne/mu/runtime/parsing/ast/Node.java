package org.crayne.mu.runtime.parsing.ast;

import org.crayne.mu.runtime.parsing.lexer.Token;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Node {

    private final List<Node> children;
    private final NodeType type;
    private final Token value;

    public static Node of(@NotNull final NodeType type) {
        return new Node(type);
    }
    public static Node of(@NotNull final Token token) {
        return new Node(token);
    }

    public Node(@NotNull final NodeType type, final Token value) {
        this.type = type;
        this.value = value;
        children = new ArrayList<>();
    }

    public Node(@NotNull final NodeType type) {
        this.type = type;
        this.value = null;
        children = new ArrayList<>();
    }

    public Node(@NotNull final Token token) {
        this.type = NodeType.of(token);
        this.value = token;
        children = new ArrayList<>();
    }

    public Node(@NotNull final NodeType type, final Token value, @NotNull final Collection<Node> children) {
        this.type = type;
        this.value = value;
        this.children = new ArrayList<>();
        this.children.addAll(children);
    }

    public Node(@NotNull final NodeType type, @NotNull final Collection<Node> children) {
        this.type = type;
        this.value = null;
        this.children = new ArrayList<>();
        this.children.addAll(children);
    }

    public Node(@NotNull final NodeType type, final Token value, @NotNull final Node... children) {
        this.type = type;
        this.value = value;
        this.children = new ArrayList<>();
        this.children.addAll(List.of(children));
    }

    public Node(@NotNull final NodeType type, @NotNull final Node... children) {
        this.type = type;
        this.value = null;
        this.children = new ArrayList<>();
        this.children.addAll(List.of(children));
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
        int indent = 0;
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
                result.append(child.toString().indent(indent + 4));
            }
            result.append("]");
        } else if (value == null && !type.name().startsWith("LITERAL_")) {
            result.append(" []");
        }
        return result.toString();
    }

}
