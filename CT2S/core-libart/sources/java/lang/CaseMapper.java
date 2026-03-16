package java.lang;

import dalvik.bytecode.Opcodes;
import java.util.Locale;
import libcore.icu.ICU;
import libcore.icu.Transliterator;

class CaseMapper {
    private static final char GREEK_CAPITAL_SIGMA = 931;
    private static final char GREEK_SMALL_FINAL_SIGMA = 962;
    private static final char LATIN_CAPITAL_I_WITH_DOT = 304;
    private static final char[] upperValues = "SS\u0000ʼN\u0000J̌\u0000Ϊ́Ϋ́ԵՒ\u0000H̱\u0000T̈\u0000W̊\u0000Y̊\u0000Aʾ\u0000Υ̓\u0000Υ̓̀Υ̓́Υ̓͂ἈΙ\u0000ἉΙ\u0000ἊΙ\u0000ἋΙ\u0000ἌΙ\u0000ἍΙ\u0000ἎΙ\u0000ἏΙ\u0000ἈΙ\u0000ἉΙ\u0000ἊΙ\u0000ἋΙ\u0000ἌΙ\u0000ἍΙ\u0000ἎΙ\u0000ἏΙ\u0000ἨΙ\u0000ἩΙ\u0000ἪΙ\u0000ἫΙ\u0000ἬΙ\u0000ἭΙ\u0000ἮΙ\u0000ἯΙ\u0000ἨΙ\u0000ἩΙ\u0000ἪΙ\u0000ἫΙ\u0000ἬΙ\u0000ἭΙ\u0000ἮΙ\u0000ἯΙ\u0000ὨΙ\u0000ὩΙ\u0000ὪΙ\u0000ὫΙ\u0000ὬΙ\u0000ὭΙ\u0000ὮΙ\u0000ὯΙ\u0000ὨΙ\u0000ὩΙ\u0000ὪΙ\u0000ὫΙ\u0000ὬΙ\u0000ὭΙ\u0000ὮΙ\u0000ὯΙ\u0000ᾺΙ\u0000ΑΙ\u0000ΆΙ\u0000Α͂\u0000Α͂ΙΑΙ\u0000ῊΙ\u0000ΗΙ\u0000ΉΙ\u0000Η͂\u0000Η͂ΙΗΙ\u0000Ϊ̀Ϊ́Ι͂\u0000Ϊ͂Ϋ̀Ϋ́Ρ̓\u0000Υ͂\u0000Ϋ͂ῺΙ\u0000ΩΙ\u0000ΏΙ\u0000Ω͂\u0000Ω͂ΙΩΙ\u0000FF\u0000FI\u0000FL\u0000FFIFFLST\u0000ST\u0000ՄՆ\u0000ՄԵ\u0000ՄԻ\u0000ՎՆ\u0000ՄԽ\u0000".toCharArray();
    private static final char[] upperValues2 = "\u000b\u0000\f\u0000\r\u0000\u000e\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u000f\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019\u001a\u001b\u001c\u001d\u001e\u001f !\"#$%&'()*+,-./0123456789:;<=>\u0000\u0000?@A\u0000BC\u0000\u0000\u0000\u0000D\u0000\u0000\u0000\u0000\u0000EFG\u0000HI\u0000\u0000\u0000\u0000J\u0000\u0000\u0000\u0000\u0000KL\u0000\u0000MN\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000OPQ\u0000RS\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000TUV\u0000WX\u0000\u0000\u0000\u0000Y".toCharArray();
    private static final ThreadLocal<Transliterator> EL_UPPER = new ThreadLocal<Transliterator>() {
        @Override
        protected Transliterator initialValue() {
            return new Transliterator("el-Upper");
        }
    };

    private CaseMapper() {
    }

    public static String toLowerCase(Locale locale, String s, char[] value, int offset, int count) {
        char newCh;
        String languageCode = locale.getLanguage();
        if (languageCode.equals("tr") || languageCode.equals("az") || languageCode.equals("lt")) {
            return ICU.toLowerCase(s, locale);
        }
        char[] newValue = null;
        int newCount = 0;
        int end = offset + count;
        for (int i = offset; i < end; i++) {
            char ch = value[i];
            if (ch == 304 || Character.isHighSurrogate(ch)) {
                return ICU.toLowerCase(s, locale);
            }
            if (ch == 931 && isFinalSigma(value, offset, count, i)) {
                newCh = GREEK_SMALL_FINAL_SIGMA;
            } else {
                newCh = Character.toLowerCase(ch);
            }
            if (newValue == null && ch != newCh) {
                newValue = new char[count];
                newCount = i - offset;
                System.arraycopy(value, offset, newValue, 0, newCount);
            }
            int newCount2 = newCount;
            if (newValue != null) {
                newCount = newCount2 + 1;
                newValue[newCount2] = newCh;
            } else {
                newCount = newCount2;
            }
        }
        return newValue != null ? new String(0, newCount, newValue) : s;
    }

    private static boolean isFinalSigma(char[] value, int offset, int count, int index) {
        if (index <= offset) {
            return false;
        }
        char previous = value[index - 1];
        if (!Character.isLowerCase(previous) && !Character.isUpperCase(previous) && !Character.isTitleCase(previous)) {
            return false;
        }
        if (index + 1 >= offset + count) {
            return true;
        }
        char next = value[index + 1];
        return (Character.isLowerCase(next) || Character.isUpperCase(next) || Character.isTitleCase(next)) ? false : true;
    }

    private static int upperIndex(int i) {
        char c = -1;
        c = -1;
        c = -1;
        c = -1;
        c = -1;
        c = -1;
        if (i >= 223) {
            if (i <= 1415) {
                switch (i) {
                    case Opcodes.OP_XOR_INT_LIT8:
                        return 0;
                    case 329:
                        return 1;
                    case 496:
                        return 2;
                    case 912:
                        return 3;
                    case 944:
                        return 4;
                    case 1415:
                        return 5;
                }
            }
            if (i >= 7830) {
                if (i <= 7834) {
                    c = (i + 6) - 7830;
                } else if (i >= 8016 && i <= 8188) {
                    c = upperValues2[i - 8016];
                    if (c == 0) {
                        c = -1;
                    }
                } else if (i >= 64256) {
                    if (i <= 64262) {
                        c = (i + 90) - 64256;
                    } else if (i >= 64275 && i <= 64279) {
                        c = (i + 97) - 64275;
                    }
                }
            }
        }
        return c == true ? 1 : 0;
    }

    public static String toUpperCase(Locale locale, String s, char[] value, int offset, int count) {
        int i;
        int i2;
        int i3;
        String languageCode = locale.getLanguage();
        if (languageCode.equals("tr") || languageCode.equals("az") || languageCode.equals("lt")) {
            return ICU.toUpperCase(s, locale);
        }
        if (languageCode.equals("el")) {
            return EL_UPPER.get().transliterate(s);
        }
        char[] output = null;
        int o = offset;
        int end = offset + count;
        int i4 = 0;
        while (o < end) {
            char ch = value[o];
            if (Character.isHighSurrogate(ch)) {
                return ICU.toUpperCase(s, locale);
            }
            int index = upperIndex(ch);
            if (index == -1) {
                if (output != null && i4 >= output.length) {
                    char[] newoutput = new char[output.length + (count / 6) + 2];
                    System.arraycopy(output, 0, newoutput, 0, output.length);
                    output = newoutput;
                }
                char upch = Character.toUpperCase(ch);
                if (ch != upch) {
                    if (output == null) {
                        output = new char[count];
                        i3 = o - offset;
                        System.arraycopy(value, offset, output, 0, i3);
                    } else {
                        i3 = i4;
                    }
                    output[i3] = upch;
                    i2 = i3 + 1;
                } else if (output != null) {
                    i2 = i4 + 1;
                    output[i4] = ch;
                } else {
                    i2 = i4;
                }
            } else {
                int target = index * 3;
                char val3 = upperValues[target + 2];
                if (output == null) {
                    output = new char[(count / 6) + count + 2];
                    i = o - offset;
                    System.arraycopy(value, offset, output, 0, i);
                } else {
                    if ((val3 == 0 ? 1 : 2) + i4 >= output.length) {
                        char[] newoutput2 = new char[output.length + (count / 6) + 3];
                        System.arraycopy(output, 0, newoutput2, 0, output.length);
                        output = newoutput2;
                        i = i4;
                    } else {
                        i = i4;
                    }
                }
                char val = upperValues[target];
                int i5 = i + 1;
                output[i] = val;
                char val2 = upperValues[target + 1];
                i2 = i5 + 1;
                output[i5] = val2;
                if (val3 != 0) {
                    i4 = i2 + 1;
                    output[i2] = val3;
                    i2 = i4;
                }
            }
            o++;
            i4 = i2;
        }
        if (output != null) {
            return (output.length == i4 || output.length - i4 < 8) ? new String(0, i4, output) : new String(output, 0, i4);
        }
        return s;
    }
}
