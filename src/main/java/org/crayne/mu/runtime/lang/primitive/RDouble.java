package org.crayne.mu.runtime.lang.primitive;

import org.crayne.mu.lang.Datatype;
import org.crayne.mu.runtime.SyntaxTree;
import org.crayne.mu.runtime.lang.RDatatype;
import org.crayne.mu.runtime.lang.RValue;
import org.jetbrains.annotations.NotNull;

public class RDouble implements RPrimitiveType {

    private final SyntaxTree runtime;


    public SyntaxTree runtime() {
        return runtime;
    }

    public RDouble(@NotNull final SyntaxTree runtime) {
        this.runtime = runtime;
    }

    public RValue add(final RValue x, final RValue y) {
        return new RValue(RDatatype.of(Datatype.DOUBLE),
                (Double) x.getValue() + (Double) y.getValue()
        );
    }

    public RValue subtract(final RValue x, final RValue y) {
        return new RValue(RDatatype.of(Datatype.DOUBLE),
                (Double) x.getValue() - (Double) y.getValue()
        );
    }

    public RValue multiply(final RValue x, final RValue y) {
        return new RValue(RDatatype.of(Datatype.DOUBLE),
                (Double) x.getValue() * (Double) y.getValue()
        );
    }

    public RValue divide(final RValue x, final RValue y) {
        return new RValue(RDatatype.of(Datatype.DOUBLE),
                (Double) x.getValue() / (Double) y.getValue()
        );
    }

    public RValue modulus(final RValue x, final RValue y) {
        return new RValue(RDatatype.of(Datatype.DOUBLE),
                (Double) x.getValue() % (Double) y.getValue()
        );
    }

    public RValue equals(final RValue x, final RValue y) {
        return ((Double) x.getValue()).doubleValue() == ((Double) y.getValue()).doubleValue() ? RValue.TRUE : RValue.FALSE;
    }

    public RValue notEquals(final RValue x, final RValue y) {
        return ((Double) x.getValue()).doubleValue() != ((Double) y.getValue()).doubleValue() ? RValue.TRUE : RValue.FALSE;
    }

    public RValue lessThan(final RValue x, final RValue y) {
        return (Double) x.getValue() < (Double) y.getValue() ? RValue.TRUE : RValue.FALSE;
    }

    public RValue greaterThan(final RValue x, final RValue y) {
        return (Double) x.getValue() > (Double) y.getValue() ? RValue.TRUE : RValue.FALSE;
    }

    public RValue lessThanOrEquals(final RValue x, final RValue y) {
        return (Double) x.getValue() <= (Double) y.getValue() ? RValue.TRUE : RValue.FALSE;
    }

    public RValue greaterThanOrEquals(final RValue x, final RValue y) {
        return (Double) x.getValue() >= (Double) y.getValue() ? RValue.TRUE : RValue.FALSE;
    }

}
