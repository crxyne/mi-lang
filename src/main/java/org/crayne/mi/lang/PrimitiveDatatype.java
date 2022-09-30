package org.crayne.mi.lang;

import org.crayne.mi.parsing.ast.NodeType;
import org.crayne.mi.parsing.lexer.Token;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public enum PrimitiveDatatype {

    INT("Integer") {
        public boolean addDefined(final PrimitiveDatatype y) {return y.isNumber() || y == STRING;}
        public boolean subtractDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean multiplyDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean divideDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean modDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean equalDefined(final PrimitiveDatatype y) {return y.isNumber() || y == NULL;}
        public boolean notequalsDefined(final PrimitiveDatatype y) {return y.isNumber() || y == NULL;}
        public boolean bitxorDefined(final PrimitiveDatatype y) {return y == INT || y == LONG;}
        public boolean bitandDefined(final PrimitiveDatatype y) {return y == INT || y == LONG;}
        public boolean bitorDefined(final PrimitiveDatatype y) {return y == INT || y == LONG;}
        public boolean bitshiftlDefined(final PrimitiveDatatype y) {return y == INT || y == LONG;}
        public boolean bitshiftrDefined(final PrimitiveDatatype y) {return y == INT || y == LONG;}
        public boolean lessthanDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean greaterthanDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean lessequalsDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean greaterequalsDefined(final PrimitiveDatatype y) {return y.isNumber();}
    },
    LONG("Long") {
        public boolean addDefined(final PrimitiveDatatype y) {return y.isNumber() || y == STRING;}
        public boolean subtractDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean multiplyDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean divideDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean modDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean equalDefined(final PrimitiveDatatype y) {return y.isNumber() || y == NULL;}
        public boolean notequalsDefined(final PrimitiveDatatype y) {return y.isNumber() || y == NULL;}
        public boolean bitxorDefined(final PrimitiveDatatype y) {return y == INT || y == LONG;}
        public boolean bitandDefined(final PrimitiveDatatype y) {return y == INT || y == LONG;}
        public boolean bitorDefined(final PrimitiveDatatype y) {return y == INT || y == LONG;}
        public boolean bitshiftlDefined(final PrimitiveDatatype y) {return y == INT || y == LONG;}
        public boolean bitshiftrDefined(final PrimitiveDatatype y) {return y == INT || y == LONG;}
        public boolean lessthanDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean greaterthanDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean lessequalsDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean greaterequalsDefined(final PrimitiveDatatype y) {return y.isNumber();}
    },
    DOUBLE("Double") {
        public boolean addDefined(final PrimitiveDatatype y) {return y.isNumber() || y == STRING;}
        public boolean subtractDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean multiplyDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean divideDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean modDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean equalDefined(final PrimitiveDatatype y) {return y.isNumber() || y == NULL;}
        public boolean notequalsDefined(final PrimitiveDatatype y) {return y.isNumber() || y == NULL;}
        public boolean lessthanDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean greaterthanDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean lessequalsDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean greaterequalsDefined(final PrimitiveDatatype y) {return y.isNumber();}
    },
    FLOAT("Float") {
        public boolean addDefined(final PrimitiveDatatype y) {return y.isNumber() || y == STRING;}
        public boolean subtractDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean multiplyDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean divideDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean modDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean equalDefined(final PrimitiveDatatype y) {return y.isNumber() || y == NULL;}
        public boolean notequalsDefined(final PrimitiveDatatype y) {return y.isNumber() || y == NULL;}
        public boolean lessthanDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean greaterthanDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean lessequalsDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean greaterequalsDefined(final PrimitiveDatatype y) {return y.isNumber();}
    },
    BOOL("Boolean") {
        public boolean equalDefined(final PrimitiveDatatype y) {return y == BOOL || y == NULL;}
        public boolean notequalsDefined(final PrimitiveDatatype y) {return y == BOOL || y == NULL;}
        public boolean andDefined(final PrimitiveDatatype y) {return y == BOOL;}
        public boolean orDefined(final PrimitiveDatatype y) {return y == BOOL;}
        public boolean bitxorDefined(final PrimitiveDatatype y) {return y == BOOL;}
        public boolean bitandDefined(final PrimitiveDatatype y) {return y == BOOL;}
        public boolean bitorDefined(final PrimitiveDatatype y) {return y == BOOL;}
    },
    STRING("String") {
        public boolean addDefined(final PrimitiveDatatype y) {return true;}
        public boolean equalDefined(final PrimitiveDatatype y) {return y == STRING || y == NULL;}
        public boolean notequalsDefined(final PrimitiveDatatype y) {return y == STRING || y == NULL;}
        public boolean lessthanDefined(final PrimitiveDatatype y) {return y == STRING;}
        public boolean greaterthanDefined(final PrimitiveDatatype y) {return y == STRING;}
        public boolean lessequalsDefined(final PrimitiveDatatype y) {return y == STRING;}
        public boolean greaterequalsDefined(final PrimitiveDatatype y) {return y == STRING;}
    },
    CHAR("Character") {
        public boolean addDefined(final PrimitiveDatatype y) {return y.isNumber() || y == STRING;}
        public boolean subtractDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean multiplyDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean divideDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean modDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean equalDefined(final PrimitiveDatatype y) {return y.isNumber() || y == NULL;}
        public boolean notequalsDefined(final PrimitiveDatatype y) {return y.isNumber() || y == NULL;}
        public boolean bitxorDefined(final PrimitiveDatatype y) {return y == INT || y == LONG;}
        public boolean bitandDefined(final PrimitiveDatatype y) {return y == INT || y == LONG;}
        public boolean bitorDefined(final PrimitiveDatatype y) {return y == INT || y == LONG;}
        public boolean bitshiftlDefined(final PrimitiveDatatype y) {return y == INT || y == LONG;}
        public boolean bitshiftrDefined(final PrimitiveDatatype y) {return y == INT || y == LONG;}
        public boolean lessthanDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean greaterthanDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean lessequalsDefined(final PrimitiveDatatype y) {return y.isNumber();}
        public boolean greaterequalsDefined(final PrimitiveDatatype y) {return y.isNumber();}
    },
    VOID("Void"),
    NULL("Null") {
        public boolean equalDefined(final PrimitiveDatatype y) {return true;}
        public boolean notequalsDefined(final PrimitiveDatatype y) {return true;}
    };

    private static final List<String> comparatorOperators = Arrays.asList(
            "<", "<=", ">", ">=", "!=", "=="
    );

    public static boolean isComparator(@NotNull final String op) {
        return comparatorOperators.contains(op);
    }

    private final String name;

    PrimitiveDatatype(@NotNull final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static PrimitiveDatatype of(@NotNull final Token token) {
        for (@NotNull final PrimitiveDatatype type : values()) if (type.name().equals(token.token().toUpperCase())) return type;
        return null;
    }

    public boolean isNumber() {
        return switch (this) {
            case INT, DOUBLE, FLOAT, LONG -> true;
            default -> false;
        };
    }

    public boolean operatorDefined(final NodeType op, final PrimitiveDatatype y) {
        if (y == null) return false;
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
        try {
            return operatorMap.get(op).call();
        } catch (final Exception e) {
            return false;
        }
    }

    public boolean addDefined(final PrimitiveDatatype y) {
        return false;
    }

    public boolean subtractDefined(final PrimitiveDatatype y) {
        return false;
    }

    public boolean multiplyDefined(final PrimitiveDatatype y) {
        return false;
    }

    public boolean divideDefined(final PrimitiveDatatype y) {
        return false;
    }

    public boolean modDefined(final PrimitiveDatatype y) {
        return false;
    }

    public boolean equalDefined(final PrimitiveDatatype y) {
        return false;
    }

    public boolean notequalsDefined(final PrimitiveDatatype y) {
        return false;
    }

    public boolean andDefined(final PrimitiveDatatype y) {
        return false;
    }

    public boolean orDefined(final PrimitiveDatatype y) {
        return false;
    }

    public boolean bitxorDefined(final PrimitiveDatatype y) {
        return false;
    }

    public boolean bitandDefined(final PrimitiveDatatype y) {
        return false;
    }

    public boolean bitorDefined(final PrimitiveDatatype y) {
        return false;
    }

    public boolean bitshiftlDefined(final PrimitiveDatatype y) {
        return false;
    }

    public boolean bitshiftrDefined(final PrimitiveDatatype y) {
        return false;
    }

    public boolean lessthanDefined(final PrimitiveDatatype y) {
        return false;
    }

    public boolean greaterthanDefined(final PrimitiveDatatype y) {
        return false;
    }

    public boolean lessequalsDefined(final PrimitiveDatatype y) {
        return false;
    }

    public boolean greaterequalsDefined(final PrimitiveDatatype y) {
        return false;
    }

}
