package org.crayne.mi.parsing.lexer;

import org.apache.commons.text.StringEscapeUtils;
import org.crayne.mi.log.MessageHandler;
import org.crayne.mi.parsing.ast.NodeType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

@SuppressWarnings("unused")
public class Tokenizer {

    private final MessageHandler output;
    private final List<String> multiSpecial;
    private final List<Token> result = new ArrayList<>();
    private char currentQuotes = 0;
    private int beganStringLine = 0;
    private int beganStringColumn = 0;
    private String previous = null;
    private boolean singleLineCommented = false;
    private boolean multilineCommented = false;
    private StringBuilder currentToken = new StringBuilder();
    private int line = 1;
    private int actualLine = 0;
    private int stdlibFinishLine = -1;

    public int stdlibFinishLine() {
        return stdlibFinishLine;
    }

    private int column = 0;
    private char atPos = 0;
    private boolean countIndices = false;
    private boolean encounteredError = false;
    private boolean unfinishedTextLiteral = false;

    public Tokenizer(@NotNull final MessageHandler output) {
        this.output = output;
        this.multiSpecial = new ArrayList<>();
    }

    public Tokenizer(@NotNull final MessageHandler output, @NotNull final Collection<String> multiSpecial) {
        this.output = output;
        this.multiSpecial = new ArrayList<>();
        for (final String special : multiSpecial)
            if (!isMultiToken(special)) throw new IllegalArgumentException("Not a multi special token: '" + special + "'");

        this.multiSpecial.addAll(multiSpecial);
    }

    private static final String specials = ",;:-#+&!%/()[]{}=?^<>|*$\\'\"@~";
    private static final String stringEscapeRegex = splitKeepDelim("\\\\u[\\dA-Fa-f]{4}|\\\\\\d|\\\\[\"\\\\'tnbfr]").substring(1);

    public static boolean isSpecialToken(@NotNull final String s) {
        return specials.contains(s);
    }

    private static <T> T isAnyType(@NotNull final Callable<T> typeCheck) {
        try {
            return typeCheck.call();
        } catch (final Exception e) {
            return null;
        }
    }

    public static Boolean isBool(@NotNull final String s) {
        final String lower = s.toLowerCase();
        return isAnyType(() -> lower.equals("1b") ? Boolean.TRUE : lower.equals("0b") ? Boolean.FALSE : null);
    }

    public static Double isDouble(@NotNull final String s) {
        final String lower = s.toLowerCase();
        if (lower.endsWith("f")) return null;
        return isAnyType(() -> Double.parseDouble(lower.endsWith("d") ? s.substring(0, s.length() - 1) : s));
    }

    public static Integer isInt(@NotNull final String s) {
        return isAnyType(() -> Integer.decode(s));
    }

    public static Long isLong(@NotNull final String s) {
        return isAnyType(() -> Long.parseLong(s.toLowerCase().endsWith("l") ? s.substring(0, s.length() - 1) : s));
    }

    public static Float isFloat(@NotNull final String s) {
        return isAnyType(() -> Float.parseFloat(s));
    }

    public static Character isChar(@NotNull final String s) {
        return isAnyType(() -> s.startsWith("'") && s.endsWith("'") && s.length() == 3 ? s.charAt(1) : null);
    }

    public static String isString(@NotNull final String s) {
        return isAnyType(() -> s.startsWith("\"") && s.endsWith("\"") ? s : null);
    }

    private void lexerError(@NotNull final String message, @NotNull final String... quickFixes) {
        output.astHelperError(message, line, column, stdlibFinishLine, countIndices, quickFixes);
        encounteredError = true;
    }

    private void lexerError(@NotNull final String message, final int line, final int column, @NotNull final String... quickFixes) {
        output.astHelperError(message, line, column, stdlibFinishLine, countIndices, quickFixes);
        encounteredError = true;
    }

    public static String removeStringLiterals(@NotNull final String string) {
        if ((!string.startsWith("\"") || !string.endsWith("\"")) && (!string.startsWith("'") || !string.endsWith("'"))) return string;
        return string.substring(1, string.length() - 1);
    }

    public static String addStringLiterals(@NotNull final String string) {
        return "\"" + string + "\"";
    }

    private String fixEscapeCodes(@NotNull final String string) {
        final boolean isLiteralString = isString(string) != null;
        final String strippedQuotes = removeStringLiterals(string);
        final String escapedCodes = strippedQuotes
                .replace("\n", "\\n")
                .replace("\"", "\\\"");
        return isLiteralString ? addStringLiterals(escapedCodes) : escapedCodes;
    }

    private static String splitKeepDelim(@NotNull final String delimRegex) {
        return "|((?=" + delimRegex + ")|(?<=" + delimRegex + "))";
    }

    public static boolean validEscapeSeq(@NotNull final String seq) {
        return seq.matches("\\\\u[\\dA-Fa-f]{4}|\\\\\\d|\\\\[\"\\\\'tnbfr]");
    }

    public boolean validEscapeSequences(@NotNull final String seq) {
        for (final String escapeSequence : seq.split(stringEscapeRegex)) {
            if (!validEscapeSeq(escapeSequence) && escapeSequence.startsWith("\\")) {
                lexerError("Invalid escape sequences found: " + seq,
                        "Only allowed escape sequences are \\n, \\f, \\r, \\\", \\', \\b, \\t, \\\\, any unicode escapes like \\uXXXX and any ascii escapes like \\X");
                return false;
            }
        }
        return true;
    }

    private static <T> T getLast(@NotNull final List<T> list) {
        return getLast(list, 1);
    }

    private static <T> T getLast(@NotNull final List<T> list, final int backSeek) {
        return list.get(list.size() - backSeek);
    }

    private static <T> void pop(@NotNull final List<T> list) {
        list.remove(list.size() - 1);
    }

    private static <T> void pop(@NotNull final List<T> list, final int amount) {
        for (int i = 0; i < amount; i++) pop(list);
    }

    private Token tokenOf(@NotNull final String token) {
        return new Token(token, actualLine, line, Math.max(column - token.length(), 0));
    }

    private Token currentToken() {
        return tokenOf(currentToken.toString());
    }

    private boolean notInComment() {
        return !singleLineCommented && !multilineCommented;
    }

    private boolean appendToCurrentString() {
        if (currentQuotes != 0) {
            currentToken.append(atPos);
            previous = "" + atPos;
            return true;
        }
        return false;
    }

    private boolean isPreviousEscape() {
        return previous != null && previous.equals("\\");
    }

    private void beginString() {
        addCurrent();
        setCurrent(atPos + "");
        currentQuotes = atPos;
        beganStringColumn = column;
        beganStringLine = line;
    }

    private boolean invalidChar(@NotNull final String s) {
        if (currentQuotes == '\'' && s.length() != 3) {
            lexerError("Invalid character literal " + s + ", should contain exactly one character", line, column + 1 - s.length(),
                    "The \" and ' text literals are different, double quotes are used for",
                    "strings of characters, while single quotes can only be used to store one character.",
                    "If you meant to put a whole string of characters here, change the quote type to \".",
                    "If not, remove any extra characters inside the character literals.");
        }
        return false;
    }

    private boolean endString() {
        if (currentQuotes == atPos) {
            currentToken.append(atPos);
            if (!validEscapeSequences(currentToken.toString())) return true;
            final String str = StringEscapeUtils.unescapeJava(currentToken.toString());
            if (invalidChar(str)) return true;

            setCurrent(str);
            addCurrent();
            clearCurrent();
            currentQuotes = 0;
            return true;
        }
        return false;
    }

    private boolean beginOrEndString() {
        switch (currentQuotes) {
            case 0 -> {
                beginString();
                return true;
            }
            case '\'', '"' -> {
                if (endString()) return true;
                return appendToCurrentString();
            }
        }
        return false;
    }

    private boolean handleQuoted() {
        if (notInComment()) {
            if (currentQuotes != 0) handleNewlines();
            if (atPos == '\'' || atPos == '"') {
                if (isPreviousEscape() && appendToCurrentString()) return true;
                if (beginOrEndString()) return true;
            }
            return appendToCurrentString();
        }
        return false;
    }

    private char nextChar = 0;
    private char nextNextChar = 0;

    private char lastCharCurrent() {
        return currentToken.charAt(currentToken.toString().length() - 1);
    }

    private char nextCharCurrent() {
        return nextChar;
    }

    private char nextNextCharCurrent() {
        return nextNextChar;
    }

    private void handleNewlines() {
        if (atPos != '\n') return;

        if (currentQuotes != 0 && !unfinishedTextLiteral) {
            lexerError("Expected text literal to end at the same line", beganStringLine, beganStringColumn,
                    "The text literal begin is at line " + beganStringLine + " and column " + beganStringColumn + ", so try to end it there with a " + currentQuotes + "."
            );
            unfinishedTextLiteral = true;
        }
        actualLine++;
        column = 0;
        if (countIndices) {
            line++;
        }
        singleLineCommented = false;
    }

    private boolean handleWhitespaces() {
        if (Character.isWhitespace(atPos)) {
            if (!currentToken.isEmpty() && ((lastCharCurrent() != '.' && nextCharCurrent() != '.')
                    || (nextCharCurrent() == '.' && nextNextCharCurrent() == '.')
                    || NodeType.of(currentToken()) == NodeType.DOUBLE_DOT
                    || NodeType.of(currentToken()).isKeyword())) {
                addCurrent();
                clearCurrent();
            }
            handleNewlines();
            return true;
        }
        return false;
    }

    private boolean handleComments(@NotNull final String multiTok) {
        switch (multiTok) {
            case "//" -> {
                singleLineCommented = true;
                clearCurrent();
                return true;
            }
            case "/*" -> {
                multilineCommented = true;
                clearCurrent();
                return true;
            }
        }
        if ((previous + atPos).equals("*/") && multilineCommented) {
            multilineCommented = false;
            clearCurrent();
            return true;
        }
        return false;
    }

    private boolean isCurrentMultiToken() {
        return isMultiToken(currentToken.toString());
    }

    private boolean isMultiToken(@NotNull final String multiTok) {
        return Arrays.stream(multiTok.split("")).allMatch(Tokenizer::isSpecialToken);
    }

    private boolean isCurrentNotBlank() {
        return !currentToken.toString().isBlank();
    }

    private void setCurrent(@NotNull final String s) {
        currentToken = new StringBuilder(s);
    }

    private void clearCurrent() {
        setCurrent("");
    }

    private void addCurrent() {
        if (currentToken.isEmpty()) return;
        if (currentToken.toString().equals("STANDARDLIB_MI_FINISH_CODE")) {
            if (countIndices) {
                lexerError("Duplicate standardlib code finish token", line, column - currentToken.toString().length(),
                        "Only the standard library can use this, so it is not useful anywhere else.",
                        "To fix this error, simply remove the second occurrence of this token.");
            }
            stdlibFinishLine = actualLine;
            countIndices = true;
        }
        if (!encounteredError) result.add(currentToken()); // so that the lexer does not unnecessarily add more tokens to the result if an empty list will be returned anyway
                                                           // it still tries to tokenize the rest of the program though, to directly show any other errors
                                                           // (in the lexer it most definetly won't infinitely cascade to more nonexistent errors, so here it is actually useful to display ALL errors)
        previous = currentToken.toString();
    }

    private boolean addCurrentMultiToken() {
        if (notInComment() && isCurrentNotBlank() && isCurrentMultiToken()) {
            addCurrent();
            setCurrent("" + atPos);
            return true;
        }
        return false;
    }

    private boolean doesMultiTokenExist(@NotNull final String multiTok) {
        return multiSpecial.stream().anyMatch(s -> s.startsWith(multiTok));
    }

    private boolean handleSpecialTokens() {
        if (Tokenizer.isSpecialToken(atPos + "")) {
            final String multiTok = currentToken.toString() + atPos;
            if (handleComments(multiTok)) return true;

            if (notInComment()) {
                if (isCurrentMultiToken() && doesMultiTokenExist(multiTok) && !NodeType.of(multiTok).isKeyword()) {
                    currentToken.append(atPos);
                    return true;
                }
                if (isCurrentNotBlank()) addCurrent();
                setCurrent("" + atPos);
                return true;
            }
            previous = "" + atPos;
            return true;
        }
        return addCurrentMultiToken();
    }

    public List<Token> tokenize(@NotNull final String code) {
        for (int i = 0; i < code.length(); i++) {
            this.atPos = code.charAt(i);

            for (int j = i + 1; j < code.length() && i + 1 < code.length(); j++) {
                nextChar = code.charAt(j);
                if (!Character.isWhitespace(nextChar)) {
                    for (int k = j + 1; k < code.length() && j + 1 < code.length(); k++) {
                        nextNextChar = code.charAt(k);
                        if (!Character.isWhitespace(nextNextChar)) break;
                    }
                    break;
                }
            }
            column++;

            if (handleQuoted() || handleWhitespaces() || handleSpecialTokens()) continue;
            if (notInComment()) currentToken.append(atPos);
        }
        if (encounteredError) return new ArrayList<>();
        return result;
    }

    public boolean encounteredError() {
        return encounteredError;
    }

}
