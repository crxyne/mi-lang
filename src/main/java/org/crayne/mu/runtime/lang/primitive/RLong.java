package org.crayne.mu.runtime.lang.primitive;

import org.crayne.mu.lang.Datatype;
import org.crayne.mu.runtime.SyntaxTree;
import org.crayne.mu.runtime.lang.RDatatype;
import org.crayne.mu.runtime.lang.RValue;
import org.jetbrains.annotations.NotNull;

public class RLong implements ROperand {

    private final SyntaxTree runtime;


    public SyntaxTree runtime() {
        return runtime;
    }

    public RLong(@NotNull final SyntaxTree runtime) {
        this.runtime = runtime;
    }


    public RValue add(final RValue x, final RValue y) {
        return new RValue(RDatatype.of(Datatype.LONG),
                (Long) x.getValue() + (Long) y.getValue()
        );
    }

    public RValue subtract(final RValue x, final RValue y) {
        return new RValue(RDatatype.of(Datatype.LONG),
                (Long) x.getValue() - (Long) y.getValue()
        );
    }

    public RValue multiply(final RValue x, final RValue y) {
        return new RValue(RDatatype.of(Datatype.LONG),
                (Long) x.getValue() * (Long) y.getValue()
        );
    }

    public RValue divide(final RValue x, final RValue y) {
        return new RValue(RDatatype.of(Datatype.LONG),
                (Long) x.getValue() / (Long) y.getValue()
        );
    }

    public RValue modulus(final RValue x, final RValue y) {
        return new RValue(RDatatype.of(Datatype.LONG),
                (Long) x.getValue() % (Long) y.getValue()
        );
    }

    public RValue equals(final RValue x, final RValue y) {
        return ((Long) x.getValue()).longValue() == ((Long) y.getValue()).longValue() ? RValue.TRUE : RValue.FALSE;
    }

    public RValue notEquals(final RValue x, final RValue y) {
        return ((Long) x.getValue()).longValue() != ((Long) y.getValue()).longValue() ? RValue.TRUE : RValue.FALSE;
    }

    public RValue bitXor(final RValue x, final RValue y) {
        return new RValue(RDatatype.of(Datatype.LONG),
                (Long) x.getValue() ^ (Long) y.getValue()
        );
    }

    public RValue bitAnd(final RValue x, final RValue y) {
        return new RValue(RDatatype.of(Datatype.LONG),
                (Long) x.getValue() & (Long) y.getValue()
        );
    }

    public RValue bitOr(final RValue x, final RValue y) {
        return new RValue(RDatatype.of(Datatype.LONG),
                (Long) x.getValue() | (Long) y.getValue()
        );
    }

    public RValue bitShiftLeft(final RValue x, final RValue y) {
        return new RValue(RDatatype.of(Datatype.LONG),
                (Long) x.getValue() << (Long) y.getValue()
        );
    }

    public RValue bitShiftRight(final RValue x, final RValue y) {
        return new RValue(RDatatype.of(Datatype.LONG),
                (Long) x.getValue() >> (Long) y.getValue()
        );
    }

    public RValue lessThan(final RValue x, final RValue y) {
        return (Long) x.getValue() < (Long) y.getValue() ? RValue.TRUE : RValue.FALSE;
    }

    public RValue greaterThan(final RValue x, final RValue y) {
        return (Long) x.getValue() > (Long) y.getValue() ? RValue.TRUE : RValue.FALSE;
    }

    public RValue lessThanOrEquals(final RValue x, final RValue y) {
        return (Long) x.getValue() <= (Long) y.getValue() ? RValue.TRUE : RValue.FALSE;
    }

    public RValue greaterThanOrEquals(final RValue x, final RValue y) {
        return (Long) x.getValue() >= (Long) y.getValue() ? RValue.TRUE : RValue.FALSE;
    }

}
