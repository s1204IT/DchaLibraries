package android.icu.text;

import android.icu.impl.number.Parse;
import android.icu.lang.UCharacter;
import android.icu.text.NumberFormat;
import android.icu.util.ULocale;
import java.text.AttributedCharacterIterator;
import java.util.Map;

public final class ScientificNumberFormatter {
    private static final Style SUPER_SCRIPT = new SuperscriptStyle();
    private final DecimalFormat fmt;
    private final String preExponent;
    private final Style style;

    public static ScientificNumberFormatter getSuperscriptInstance(ULocale uLocale) {
        return getInstanceForLocale(uLocale, SUPER_SCRIPT);
    }

    public static ScientificNumberFormatter getSuperscriptInstance(DecimalFormat decimalFormat) {
        return getInstance(decimalFormat, SUPER_SCRIPT);
    }

    public static ScientificNumberFormatter getMarkupInstance(ULocale uLocale, String str, String str2) {
        return getInstanceForLocale(uLocale, new MarkupStyle(str, str2));
    }

    public static ScientificNumberFormatter getMarkupInstance(DecimalFormat decimalFormat, String str, String str2) {
        return getInstance(decimalFormat, new MarkupStyle(str, str2));
    }

    public String format(Object obj) {
        String str;
        synchronized (this.fmt) {
            str = this.style.format(this.fmt.formatToCharacterIterator(obj), this.preExponent);
        }
        return str;
    }

    private static abstract class Style {
        abstract String format(AttributedCharacterIterator attributedCharacterIterator, String str);

        private Style() {
        }

        static void append(AttributedCharacterIterator attributedCharacterIterator, int i, int i2, StringBuilder sb) {
            int index = attributedCharacterIterator.getIndex();
            attributedCharacterIterator.setIndex(i);
            while (i < i2) {
                sb.append(attributedCharacterIterator.current());
                attributedCharacterIterator.next();
                i++;
            }
            attributedCharacterIterator.setIndex(index);
        }
    }

    private static class MarkupStyle extends Style {
        private final String beginMarkup;
        private final String endMarkup;

        MarkupStyle(String str, String str2) {
            super();
            this.beginMarkup = str;
            this.endMarkup = str2;
        }

        @Override
        String format(AttributedCharacterIterator attributedCharacterIterator, String str) {
            StringBuilder sb = new StringBuilder();
            attributedCharacterIterator.first();
            int runLimit = 0;
            while (attributedCharacterIterator.current() != 65535) {
                Map<AttributedCharacterIterator.Attribute, Object> attributes = attributedCharacterIterator.getAttributes();
                if (attributes.containsKey(NumberFormat.Field.EXPONENT_SYMBOL)) {
                    append(attributedCharacterIterator, runLimit, attributedCharacterIterator.getRunStart(NumberFormat.Field.EXPONENT_SYMBOL), sb);
                    runLimit = attributedCharacterIterator.getRunLimit(NumberFormat.Field.EXPONENT_SYMBOL);
                    attributedCharacterIterator.setIndex(runLimit);
                    sb.append(str);
                    sb.append(this.beginMarkup);
                } else if (attributes.containsKey(NumberFormat.Field.EXPONENT)) {
                    int runLimit2 = attributedCharacterIterator.getRunLimit(NumberFormat.Field.EXPONENT);
                    append(attributedCharacterIterator, runLimit, runLimit2, sb);
                    attributedCharacterIterator.setIndex(runLimit2);
                    sb.append(this.endMarkup);
                    runLimit = runLimit2;
                } else {
                    attributedCharacterIterator.next();
                }
            }
            append(attributedCharacterIterator, runLimit, attributedCharacterIterator.getEndIndex(), sb);
            return sb.toString();
        }
    }

    private static class SuperscriptStyle extends Style {
        private static final char[] SUPERSCRIPT_DIGITS = {8304, 185, 178, 179, 8308, 8309, 8310, 8311, 8312, 8313};
        private static final char SUPERSCRIPT_MINUS_SIGN = 8315;
        private static final char SUPERSCRIPT_PLUS_SIGN = 8314;

        private SuperscriptStyle() {
            super();
        }

        @Override
        String format(AttributedCharacterIterator attributedCharacterIterator, String str) {
            int runLimit;
            StringBuilder sb = new StringBuilder();
            attributedCharacterIterator.first();
            int runLimit2 = 0;
            while (attributedCharacterIterator.current() != 65535) {
                Map<AttributedCharacterIterator.Attribute, Object> attributes = attributedCharacterIterator.getAttributes();
                if (attributes.containsKey(NumberFormat.Field.EXPONENT_SYMBOL)) {
                    append(attributedCharacterIterator, runLimit2, attributedCharacterIterator.getRunStart(NumberFormat.Field.EXPONENT_SYMBOL), sb);
                    runLimit2 = attributedCharacterIterator.getRunLimit(NumberFormat.Field.EXPONENT_SYMBOL);
                    attributedCharacterIterator.setIndex(runLimit2);
                    sb.append(str);
                } else {
                    if (attributes.containsKey(NumberFormat.Field.EXPONENT_SIGN)) {
                        int runStart = attributedCharacterIterator.getRunStart(NumberFormat.Field.EXPONENT_SIGN);
                        runLimit = attributedCharacterIterator.getRunLimit(NumberFormat.Field.EXPONENT_SIGN);
                        int iChar32AtAndAdvance = char32AtAndAdvance(attributedCharacterIterator);
                        if (Parse.UNISET_MINUS.contains(iChar32AtAndAdvance)) {
                            append(attributedCharacterIterator, runLimit2, runStart, sb);
                            sb.append(SUPERSCRIPT_MINUS_SIGN);
                        } else if (Parse.UNISET_PLUS.contains(iChar32AtAndAdvance)) {
                            append(attributedCharacterIterator, runLimit2, runStart, sb);
                            sb.append(SUPERSCRIPT_PLUS_SIGN);
                        } else {
                            throw new IllegalArgumentException();
                        }
                        attributedCharacterIterator.setIndex(runLimit);
                    } else if (attributes.containsKey(NumberFormat.Field.EXPONENT)) {
                        int runStart2 = attributedCharacterIterator.getRunStart(NumberFormat.Field.EXPONENT);
                        runLimit = attributedCharacterIterator.getRunLimit(NumberFormat.Field.EXPONENT);
                        append(attributedCharacterIterator, runLimit2, runStart2, sb);
                        copyAsSuperscript(attributedCharacterIterator, runStart2, runLimit, sb);
                        attributedCharacterIterator.setIndex(runLimit);
                    } else {
                        attributedCharacterIterator.next();
                    }
                    runLimit2 = runLimit;
                }
            }
            append(attributedCharacterIterator, runLimit2, attributedCharacterIterator.getEndIndex(), sb);
            return sb.toString();
        }

        private static void copyAsSuperscript(AttributedCharacterIterator attributedCharacterIterator, int i, int i2, StringBuilder sb) {
            int index = attributedCharacterIterator.getIndex();
            attributedCharacterIterator.setIndex(i);
            while (attributedCharacterIterator.getIndex() < i2) {
                int iDigit = UCharacter.digit(char32AtAndAdvance(attributedCharacterIterator));
                if (iDigit < 0) {
                    throw new IllegalArgumentException();
                }
                sb.append(SUPERSCRIPT_DIGITS[iDigit]);
            }
            attributedCharacterIterator.setIndex(index);
        }

        private static int char32AtAndAdvance(AttributedCharacterIterator attributedCharacterIterator) {
            char cCurrent = attributedCharacterIterator.current();
            char next = attributedCharacterIterator.next();
            if (UCharacter.isHighSurrogate(cCurrent) && UCharacter.isLowSurrogate(next)) {
                attributedCharacterIterator.next();
                return UCharacter.toCodePoint(cCurrent, next);
            }
            return cCurrent;
        }
    }

    private static String getPreExponent(DecimalFormatSymbols decimalFormatSymbols) {
        StringBuilder sb = new StringBuilder();
        sb.append(decimalFormatSymbols.getExponentMultiplicationSign());
        char[] digits = decimalFormatSymbols.getDigits();
        sb.append(digits[1]);
        sb.append(digits[0]);
        return sb.toString();
    }

    private static ScientificNumberFormatter getInstance(DecimalFormat decimalFormat, Style style) {
        return new ScientificNumberFormatter((DecimalFormat) decimalFormat.clone(), getPreExponent(decimalFormat.getDecimalFormatSymbols()), style);
    }

    private static ScientificNumberFormatter getInstanceForLocale(ULocale uLocale, Style style) {
        DecimalFormat decimalFormat = (DecimalFormat) DecimalFormat.getScientificInstance(uLocale);
        return new ScientificNumberFormatter(decimalFormat, getPreExponent(decimalFormat.getDecimalFormatSymbols()), style);
    }

    private ScientificNumberFormatter(DecimalFormat decimalFormat, String str, Style style) {
        this.fmt = decimalFormat;
        this.preExponent = str;
        this.style = style;
    }
}
