package org.crayne.mi.log;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Collection;

public class TerminalColor {

    public static final String colorForm = "\033[%sm";
    public static final byte rgbFG = 38;
    public static final byte rgbBG = 48;

    public static String colorcode(@NotNull final Collection<Integer> ansiCodes) {
        return colorcode(ansiCodes.stream().mapToInt(Integer::intValue).toArray());
    }

    public static String colorcode(final int... ansiCodes) {
        boolean prependSemi = false;
        final StringBuilder colorResult = new StringBuilder();
        for (final int code : ansiCodes) {
            colorResult.append(prependSemi ? ";" : "").append(code);
            prependSemi = true;
        }
        return colorForm.formatted(colorResult);
    }

    public static String reset() {
        return colorcode(0);
    }

    private static int[] rgbCodesOf(final int rFG, final int gFG, final int bFG, final int rBG, final int gBG, final int bBG) {
        return new int[] {rgbFG, 2, rFG, gFG, bFG, rgbBG, 2, rBG, gBG, bBG};
    }

    private static int[] rgbCodesOf(final int r, final int g, final int b, final int prepend) {
        return new int[] {prepend, 2, r, g, b};
    }

    private static int[] rgbCodesOf(@NotNull final Color color, final int prepend) {
        return rgbCodesOf(color.getRed(), color.getGreen(), color.getBlue(), prepend);
    }

    private static int[] rgbCodesOf(@NotNull final Color fg, @NotNull final Color bg) {
        return rgbCodesOf(fg.getRed(), fg.getGreen(), fg.getBlue(), bg.getRed(), bg.getGreen(), bg.getBlue());
    }

    public static String foreground(@NotNull final Color color) {
        return colorcode(rgbCodesOf(color, rgbFG));
    }

    public static String background(@NotNull final Color color) {
        return colorcode(rgbCodesOf(color, rgbBG));
    }

    public static String color(@NotNull final Color fg, @NotNull final Color bg) {
        return colorcode(rgbCodesOf(fg, bg));
    }

    public static String foreground(final int r, final int g, final int b) {
        return colorcode(rgbCodesOf(r, g, b, rgbFG));
    }

    public static String background(final int r, final int g, final int b) {
        return colorcode(rgbCodesOf(r, g, b, rgbBG));
    }

    public static String color(final int rFG, final int gFG, final int bFG, final int rBG, final int gBG, final int bBG) {
        return colorcode(rgbCodesOf(rFG, gFG, bFG, rBG, gBG, bBG));
    }

    public static Color ofHex(@NotNull final String hex) {
        try {
            return Color.decode(hex);
        } catch (final Exception e) {
            return Color.WHITE;
        }
    }

    public static String foreground(@NotNull final String hex) {
        return colorcode(rgbCodesOf(ofHex(hex), rgbFG));
    }

    public static String background(@NotNull final String hex) {
        return colorcode(rgbCodesOf(ofHex(hex), rgbBG));
    }

    public static String color(@NotNull final String fgHex, @NotNull final String bgHex) {
        return colorcode(rgbCodesOf(ofHex(fgHex), ofHex(bgHex)));
    }


}
