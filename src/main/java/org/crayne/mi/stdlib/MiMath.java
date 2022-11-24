package org.crayne.mi.stdlib;

import org.crayne.mi.lang.MiCallable;
import org.jetbrains.annotations.NotNull;

public class MiMath {

    @MiCallable
    @NotNull
    public static Double sin(@NotNull final Double d) {
        return Math.sin(d);
    }

    @MiCallable
    @NotNull
    public static Double cos(@NotNull final Double d) {
        return Math.cos(d);
    }

    @MiCallable
    @NotNull
    public static Double tan(@NotNull final Double d) {
        return Math.tan(d);
    }

    @MiCallable
    @NotNull
    public static Double arcsin(@NotNull final Double d) {
        return Math.asin(d);
    }

    @MiCallable
    @NotNull
    public static Double arccos(@NotNull final Double d) {
        return Math.acos(d);
    }

    @MiCallable
    @NotNull
    public static Double arctan(@NotNull final Double d) {
        return Math.atan(d);
    }

    @MiCallable
    @NotNull
    public static Double sinh(@NotNull final Double d) {
        return Math.sinh(d);
    }

    @MiCallable
    @NotNull
    public static Double cosh(@NotNull final Double d) {
        return Math.cosh(d);
    }

    @MiCallable
    @NotNull
    public static Double tanh(@NotNull final Double d) {
        return Math.tanh(d);
    }

    @MiCallable
    @NotNull
    public static Double to_rad(@NotNull final Double d) {
        return Math.toRadians(d);
    }

    @MiCallable
    @NotNull
    public static Double to_deg(@NotNull final Double d) {
        return Math.toDegrees(d);
    }

    @MiCallable
    @NotNull
    public static Double ln(@NotNull final Double d) {
        return Math.log(d);
    }

    @MiCallable
    @NotNull
    public static Double log(@NotNull final Double d) {
        return Math.log10(d);
    }

    @MiCallable
    @NotNull
    public static Double sqrt(@NotNull final Double d) {
        return Math.sqrt(d);
    }

    @MiCallable
    @NotNull
    public static Double cbrt(@NotNull final Double d) {
        return Math.cbrt(d);
    }

    @MiCallable
    @NotNull
    public static Double ceil(@NotNull final Double d) {
        return Math.ceil(d);
    }

    @MiCallable
    @NotNull
    public static Double floor(@NotNull final Double d) {
        return Math.floor(d);
    }

    @MiCallable
    @NotNull
    public static Long round(@NotNull final Double d) {
        return Math.round(d);
    }

    @MiCallable
    @NotNull
    public static Double pow(@NotNull final Double d, @NotNull final Double e) {
        return Math.pow(d, e);
    }

    @MiCallable
    @NotNull
    public static Double max(@NotNull final Double d, @NotNull final Double e) {
        return Math.max(d, e);
    }

    @MiCallable
    @NotNull
    public static Double min(@NotNull final Double d, @NotNull final Double e) {
        return Math.min(d, e);
    }

    @MiCallable
    @NotNull
    public static Double random() {
        return Math.random();
    }

    @MiCallable
    @NotNull
    public static Double abs(@NotNull final Double d) {
        return Math.abs(d);
    }

}
