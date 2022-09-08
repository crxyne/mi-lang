package org.crayne.mu.stdlib;

import org.crayne.mu.lang.MuCallable;
import org.jetbrains.annotations.NotNull;

public class StandardLib {

    public static String standardLib() {
        return """
pub? true = 1b;
pub? false = 0b;

module std {

	pub nat fn println~ (string s) -> "$stdclass";
    pub nat fn print~ (string s) -> "$stdclass";
    pub nat fn println~ (int i) -> "$stdclass";
    pub nat fn print~ (int i) -> "$stdclass";
    pub nat fn println~ (double d) -> "$stdclass";
    pub nat fn print~ (double d) -> "$stdclass";
    pub nat fn println~ (float f) -> "$stdclass";
    pub nat fn print~ (float f) -> "$stdclass";
    pub nat fn println~ (long l) -> "$stdclass";
    pub nat fn print~ (long l) -> "$stdclass";
    pub nat fn println~ (bool b) -> "$stdclass";
    pub nat fn print~ (bool b) -> "$stdclass";
    pub nat fn println~ (char c) -> "$stdclass";
    pub nat fn print~ (char c) -> "$stdclass";

}

module termion {

	pub nat fn color_fg :: string (int r, int g, int b) -> "$stdtermion";
	pub nat fn color_bg :: string (int r, int g, int b) -> "$stdtermion";
	
	pub nat fn color_fg :: string (string hex) -> "$stdtermion";
	pub nat fn color_bg :: string (string hex) -> "$stdtermion";
	
}

STANDARDLIB_MU_FINISH_CODE;
"""
                .replace("$stdclass", StandardLib.class.getName())
                .replace("$stdtermion", StdTermion.class.getName());
    }

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
