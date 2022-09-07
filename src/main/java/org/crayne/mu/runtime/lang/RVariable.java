package org.crayne.mu.runtime.lang;

import org.crayne.mu.lang.Variable;
import org.crayne.mu.parsing.ast.Node;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RVariable {

    private final String name;
    private final RDatatype type;
    private RValue value;

    public RVariable(@NotNull final String name, @NotNull final RDatatype datatype) {
        this.name = name;
        this.type = datatype;
    }

    public RVariable(@NotNull final String name, @NotNull final RDatatype datatype, final RValue value) {
        this.name = name;
        this.type = datatype;
        this.value = value;
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

    public void setValue(@NotNull final RValue value) {
        this.value = value;
    }

    public static RVariable of(@NotNull final Variable v) {
        return new RVariable(v.name(), new RDatatype(v.type().getName()));
    }

    public static RVariable of(@NotNull final Node varDefinition) {
        final List<Node> values = varDefinition.children();
        if (values.size() == 4) {
            return new RVariable(
                    values.get(1).value().token(),
                    new RDatatype(values.get(2).value().token())
            );
        }
        return new RVariable(
                values.get(1).value().token(),
                new RDatatype(values.get(2).value().token()),
                REvaluator.evaluateExpression(values.get(3))
        );
    }

}
