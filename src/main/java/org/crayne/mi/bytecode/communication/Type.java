package org.crayne.mi.bytecode.communication;

import org.apache.commons.lang3.StringUtils;
import org.crayne.mi.bytecode.common.ByteDatatype;
import org.jetbrains.annotations.NotNull;

public class Type {

    private final ByteDatatype type;

    protected Type(@NotNull final ByteDatatype type) {
        this.type = type;
    }

    public static Type of(@NotNull final String typename) {
        final ByteDatatype type = ByteDatatype.of(typename);
        if (type.id() == ByteDatatype.NULL.id()
                || type.id() == ByteDatatype.VOID.id()
                || type.id() == ByteDatatype.UNKNOWN.id()) throw new MiExecutionException("Expected definite datatype (void and null are not allowed)");

        return type.id() == 7 ? new Type(ByteDatatype.of("!PARENT." + typename)) : new Type(type);
    }

    public static Type of(@NotNull final Class<?> clazz) {
        return Type.of(miType(clazz.getName()));
    }

    public ByteDatatype byteDatatype() {
        return type;
    }

    public String typename() {
        return type.name();
    }

    protected static String miType(@NotNull final String javaType) {
        return switch (javaType) {
            case "java.lang.Integer" -> "int";
            case "java.lang.Long" -> "long";
            case "java.lang.Double" -> "double";
            case "java.lang.Float" -> "float";
            case "java.lang.Character" -> "char";
            case "java.lang.Boolean" -> "bool";
            case "java.lang.String" -> "string";
            default -> throw new MiExecutionException("No Mi type matching for java class " + javaType);
        };
    }

    protected String javaType() {
        return switch (type.name()) {
            case "double", "float", "long" -> "java.lang." + StringUtils.capitalize(type.name());
            case "char" -> "java.lang.Character";
            case "int" -> "java.lang.Integer";
            case "bool" -> "java.lang.Boolean";
            case "string" -> "java.lang.String";
            default -> throw new MiExecutionException("No Java type matching for mi type " + type.name());
        };
    }

    public String toString() {
        return typename();
    }
}
