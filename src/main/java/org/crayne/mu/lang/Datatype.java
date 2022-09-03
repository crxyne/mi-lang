package org.crayne.mu.lang;

import org.crayne.mu.runtime.parsing.ast.NodeType;
import org.crayne.mu.runtime.parsing.lexer.Token;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public enum Datatype {

    INT("Integer") {
        public boolean addDefined(final Datatype y) {return y.isNumber() || y == STRING;}
        public boolean subtractDefined(final Datatype y) {return y.isNumber();}
        public boolean multiplyDefined(final Datatype y) {return y.isNumber();}
        public boolean divideDefined(final Datatype y) {return y.isNumber();}
        public boolean modDefined(final Datatype y) {return y.isNumber();}
        public boolean equalDefined(final Datatype y) {return y.isNumber();}
        public boolean notequalsDefined(final Datatype y) {return y.isNumber();}
        public boolean bitxorDefined(final Datatype y) {return y == INT || y == LONG;}
        public boolean bitandDefined(final Datatype y) {return y == INT || y == LONG;}
        public boolean bitorDefined(final Datatype y) {return y == INT || y == LONG;}
        public boolean bitshiftlDefined(final Datatype y) {return y == INT || y == LONG;}
        public boolean bitshiftrDefined(final Datatype y) {return y == INT || y == LONG;}
        public boolean lessthanDefined(final Datatype y) {return y.isNumber();}
        public boolean greaterthanDefined(final Datatype y) {return y.isNumber();}
        public boolean lessequalsDefined(final Datatype y) {return y.isNumber();}
        public boolean greaterequalsDefined(final Datatype y) {return y.isNumber();}
    },
    LONG("Long") {
        public boolean addDefined(final Datatype y) {return y.isNumber() || y == STRING;}
        public boolean subtractDefined(final Datatype y) {return y.isNumber();}
        public boolean multiplyDefined(final Datatype y) {return y.isNumber();}
        public boolean divideDefined(final Datatype y) {return y.isNumber();}
        public boolean modDefined(final Datatype y) {return y.isNumber();}
        public boolean equalDefined(final Datatype y) {return y.isNumber();}
        public boolean notequalsDefined(final Datatype y) {return y.isNumber();}
        public boolean bitxorDefined(final Datatype y) {return y == INT || y == LONG;}
        public boolean bitandDefined(final Datatype y) {return y == INT || y == LONG;}
        public boolean bitorDefined(final Datatype y) {return y == INT || y == LONG;}
        public boolean bitshiftlDefined(final Datatype y) {return y == INT || y == LONG;}
        public boolean bitshiftrDefined(final Datatype y) {return y == INT || y == LONG;}
        public boolean lessthanDefined(final Datatype y) {return y.isNumber();}
        public boolean greaterthanDefined(final Datatype y) {return y.isNumber();}
        public boolean lessequalsDefined(final Datatype y) {return y.isNumber();}
        public boolean greaterequalsDefined(final Datatype y) {return y.isNumber();}
    },
    DOUBLE("Double") {
        public boolean addDefined(final Datatype y) {return y.isNumber() || y == STRING;}
        public boolean subtractDefined(final Datatype y) {return y.isNumber();}
        public boolean multiplyDefined(final Datatype y) {return y.isNumber();}
        public boolean divideDefined(final Datatype y) {return y.isNumber();}
        public boolean modDefined(final Datatype y) {return y.isNumber();}
        public boolean equalDefined(final Datatype y) {return y.isNumber();}
        public boolean notequalsDefined(final Datatype y) {return y.isNumber();}
        public boolean lessthanDefined(final Datatype y) {return y.isNumber();}
        public boolean greaterthanDefined(final Datatype y) {return y.isNumber();}
        public boolean lessequalsDefined(final Datatype y) {return y.isNumber();}
        public boolean greaterequalsDefined(final Datatype y) {return y.isNumber();}
    },
    FLOAT("Float") {
        public boolean addDefined(final Datatype y) {return y.isNumber() || y == STRING;}
        public boolean subtractDefined(final Datatype y) {return y.isNumber();}
        public boolean multiplyDefined(final Datatype y) {return y.isNumber();}
        public boolean divideDefined(final Datatype y) {return y.isNumber();}
        public boolean modDefined(final Datatype y) {return y.isNumber();}
        public boolean equalDefined(final Datatype y) {return y.isNumber();}
        public boolean notequalsDefined(final Datatype y) {return y.isNumber();}
        public boolean lessthanDefined(final Datatype y) {return y.isNumber();}
        public boolean greaterthanDefined(final Datatype y) {return y.isNumber();}
        public boolean lessequalsDefined(final Datatype y) {return y.isNumber();}
        public boolean greaterequalsDefined(final Datatype y) {return y.isNumber();}
    },
    BOOL("Boolean") {
        public boolean equalDefined(final Datatype y) {return y == BOOL;}
        public boolean notequalsDefined(final Datatype y) {return y == BOOL;}
        public boolean andDefined(final Datatype y) {return y == BOOL;}
        public boolean orDefined(final Datatype y) {return y == BOOL;}
        public boolean bitxorDefined(final Datatype y) {return y == BOOL;}
        public boolean bitandDefined(final Datatype y) {return y == BOOL;}
        public boolean bitorDefined(final Datatype y) {return y == BOOL;}
    },
    STRING("String") {
        public boolean addDefined(final Datatype y) {return true;}
    },
    CHAR("Character") {
        public boolean addDefined(final Datatype y) {return y.isNumber() || y == STRING;}
        public boolean subtractDefined(final Datatype y) {return y.isNumber();}
        public boolean multiplyDefined(final Datatype y) {return y.isNumber();}
        public boolean divideDefined(final Datatype y) {return y.isNumber();}
        public boolean modDefined(final Datatype y) {return y.isNumber();}
        public boolean equalDefined(final Datatype y) {return y.isNumber();}
        public boolean notequalsDefined(final Datatype y) {return y.isNumber();}
        public boolean bitxorDefined(final Datatype y) {return y == INT || y == LONG;}
        public boolean bitandDefined(final Datatype y) {return y == INT || y == LONG;}
        public boolean bitorDefined(final Datatype y) {return y == INT || y == LONG;}
        public boolean bitshiftlDefined(final Datatype y) {return y == INT || y == LONG;}
        public boolean bitshiftrDefined(final Datatype y) {return y == INT || y == LONG;}
        public boolean lessthanDefined(final Datatype y) {return y.isNumber();}
        public boolean greaterthanDefined(final Datatype y) {return y.isNumber();}
        public boolean lessequalsDefined(final Datatype y) {return y.isNumber();}
        public boolean greaterequalsDefined(final Datatype y) {return y.isNumber();}
    },
    ENUM("Enumeration") {
        public boolean addDefined(final Datatype y) {return y == STRING;}
        public boolean equalDefined(final Datatype y) {return y == ENUM;}
        public boolean notequalsDefined(final Datatype y) {return y == ENUM;}
    },
    VOID("Void");

    private static final List<String> comparatorOperators = Arrays.asList(
            "<", "<=", ">", ">=", "!=", "=="
    );

    public static boolean isComparator(@NotNull final String op) {
        return comparatorOperators.contains(op);
    }

    private final String name;

    Datatype(@NotNull final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static Datatype of(@NotNull final Token token) {
        for (@NotNull final Datatype type : values()) if (type.name().equals(token.token().toUpperCase())) return type;
        return null;
    }

    public boolean isNumber() {
        return switch (this) {
            case INT, DOUBLE, FLOAT, LONG -> true;
            default -> false;
        };
    }

    public boolean operatorDefined(final NodeType op, final Datatype y) throws Exception {
        final Map<NodeType, Callable<Boolean>> operatorMap = new HashMap<>() {{
            this.put(NodeType.ADD, () -> addDefined(y));
            this.put(NodeType.SUBTRACT, () -> subtractDefined(y));
            this.put(NodeType.MULTIPLY, () -> multiplyDefined(y));
            this.put(NodeType.DIVIDE, () -> divideDefined(y));
            this.put(NodeType.MODULUS, () -> modDefined(y));
            this.put(NodeType.EQUALS, () -> equalDefined(y));
            this.put(NodeType.NOTEQUALS, () -> notequalsDefined(y));
            this.put(NodeType.LOGICAL_AND, () -> andDefined(y));
            this.put(NodeType.LOGICAL_OR, () -> orDefined(y));
            this.put(NodeType.BIT_AND, () -> bitandDefined(y));
            this.put(NodeType.BIT_OR, () -> bitorDefined(y));
            this.put(NodeType.XOR, () -> bitxorDefined(y));
            this.put(NodeType.LSHIFT, () -> bitshiftlDefined(y));
            this.put(NodeType.RSHIFT, () -> bitshiftrDefined(y));
            this.put(NodeType.LESS_THAN, () -> lessthanDefined(y));
            this.put(NodeType.GREATER_THAN, () -> greaterthanDefined(y));
            this.put(NodeType.LESS_THAN_EQ, () -> lessequalsDefined(y));
            this.put(NodeType.GREATER_THAN_EQ, () -> greaterequalsDefined(y));
        }};
        return operatorMap.get(op).call();
    }

    public boolean addDefined(final Datatype y) {
        return false;
    }

    public boolean subtractDefined(final Datatype y) {
        return false;
    }

    public boolean multiplyDefined(final Datatype y) {
        return false;
    }

    public boolean divideDefined(final Datatype y) {
        return false;
    }

    public boolean modDefined(final Datatype y) {
        return false;
    }

    public boolean equalDefined(final Datatype y) {
        return false;
    }

    public boolean notequalsDefined(final Datatype y) {
        return false;
    }

    public boolean andDefined(final Datatype y) {
        return false;
    }

    public boolean orDefined(final Datatype y) {
        return false;
    }

    public boolean bitxorDefined(final Datatype y) {
        return false;
    }

    public boolean bitandDefined(final Datatype y) {
        return false;
    }

    public boolean bitorDefined(final Datatype y) {
        return false;
    }

    public boolean bitshiftlDefined(final Datatype y) {
        return false;
    }

    public boolean bitshiftrDefined(final Datatype y) {
        return false;
    }

    public boolean lessthanDefined(final Datatype y) {
        return false;
    }

    public boolean greaterthanDefined(final Datatype y) {
        return false;
    }

    public boolean lessequalsDefined(final Datatype y) {
        return false;
    }

    public boolean greaterequalsDefined(final Datatype y) {
        return false;
    }

}
