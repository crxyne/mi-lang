package org.crayne.mu.stdlib;

import org.crayne.mu.lang.MuCallable;
import org.jetbrains.annotations.NotNull;

public class StandardLib {

    @MuCallable
    public static void println(@NotNull final String str) {
        System.out.println(str);
    }

    @MuCallable
    public static void print(@NotNull final String str) {
        System.out.println(str);
    }

    @MuCallable
    public static void println(final int i) {
        System.out.println(i);
    }

    @MuCallable
    public static void print(final int i) {
        System.out.println(i);
    }

    @MuCallable
    public static void println(final double d) {
        System.out.println(d);
    }

    @MuCallable
    public static void print(final double d) {
        System.out.println(d);
    }

    @MuCallable
    public static void println(final float f) {
        System.out.println(f);
    }

    @MuCallable
    public static void print(final float f) {
        System.out.println(f);
    }

    @MuCallable
    public static void println(final long l) {
        System.out.println(l);
    }

    @MuCallable
    public static void print(final long l) {
        System.out.println(l);
    }

    @MuCallable
    public static void println(final boolean b) {
        System.out.println(b);
    }

    @MuCallable
    public static void print(final boolean b) {
        System.out.println(b);
    }

    @MuCallable
    public static void println(final char c) {
        System.out.println(c);
    }

    @MuCallable
    public static void print(final char c) {
        System.out.println(c);
    }

}
