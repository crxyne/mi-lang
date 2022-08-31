package org.crayne.mu.lang;

import org.crayne.mu.runtime.parsing.ast.Node;
import org.crayne.mu.runtime.parsing.parser.ValueParser;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;

public class FunctionConcept {

    private final String name;
    private final Datatype returnType;
    private final HashSet<FunctionDefinition> definitions;

    public FunctionConcept(@NotNull final String name, @NotNull final Datatype returnType) {
        this.name = name;
        this.returnType = returnType;
        this.definitions = new HashSet<>();
    }

    public final void addDefinition(@NotNull final List<FunctionParameter> defs, @NotNull final List<Modifier> modifiers, final Node scope) {
        if (scope == null) definitions.add(new FunctionDefinition(name, returnType, defs, modifiers));
        else definitions.add(new FunctionDefinition(name, returnType, defs, modifiers, scope));
    }

    public String name() {
        return name;
    }

    public FunctionDefinition definitionByParameters(@NotNull final List<FunctionParameter> parameters) {
        for (@NotNull final FunctionDefinition def : definitions) if (def.parameters().equals(parameters)) return def;
        return null;
    }

    public FunctionDefinition definitionByCallParameters(@NotNull final List<ValueParser.TypedNode> parameters) {
        for (@NotNull final FunctionDefinition def : definitions) {
            final List<FunctionParameter> defParams = def.parameters();
            if (defParams.size() != parameters.size()) continue;

            boolean equalParams = true;
            for (int i = 0; i < defParams.size(); i++) {
                final FunctionParameter defParam = defParams.get(i);
                final ValueParser.TypedNode callParam = parameters.get(i);

                if (defParam.type() != callParam.type()) {
                    equalParams = false;
                    break;
                }
            }
            if (equalParams) return def;
        }
        return null;
    }

    public boolean isDefined(@NotNull final List<FunctionParameter> withParams) {
        return definitionByParameters(withParams) != null;
    }

    @Override
    public String toString() {
        return "FunctionConcept{" +
                "name='" + name + '\'' +
                ", returnType=" + returnType +
                ", definitions=" + definitions +
                '}';
    }
}
