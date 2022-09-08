package org.crayne.mu.runtime.lang;

import org.crayne.mu.lang.Datatype;
import org.crayne.mu.lang.FunctionDefinition;
import org.crayne.mu.lang.FunctionParameter;
import org.crayne.mu.parsing.ast.Node;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RFunction {

    private final String name;
    private final Datatype returnType;
    private final List<FunctionParameter> definedParams;
    private final Node scope;

    public RFunction(@NotNull final String name, @NotNull final Datatype returnType, @NotNull final List<FunctionParameter> definedParams, final Node scope) {
        this.name = name;
        this.returnType = returnType;
        this.definedParams = definedParams;
        this.scope = scope;
    }

    public String getName() {
        return name;
    }

    public Datatype getReturnType() {
        return returnType;
    }

    public Node getScope() {
        return scope;
    }

    public List<FunctionParameter> getDefinedParams() {
        return definedParams;
    }

    public static RFunction of(@NotNull final FunctionDefinition f) {
        if (f.scope() == null) return new RNativeFunction(f.name(), f.returnType(), f.parameters(), f.nativeMethod());
        return new RFunction(f.name(), f.returnType(), f.parameters(), f.scope());
    }

    @Override
    public String toString() {
        return "RFunction{\n" +
                ("name='" + name + '\'' +
                ",\nreturnType=" + returnType +
                ",\ndefinedParams=" + definedParams +
                ",\nscope=\n" + scope.toString().indent(4)).indent(4) +
                "}";
    }

}
