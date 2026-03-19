package java.net;

import android.icu.text.IDNA;
import android.icu.text.StringPrepParseException;

public final class IDN {
    public static final int ALLOW_UNASSIGNED = 1;
    public static final int USE_STD3_ASCII_RULES = 2;

    public static String toASCII(String str, int i) {
        try {
            return IDNA.convertIDNToASCII(str, i).toString();
        } catch (StringPrepParseException e) {
            throw new IllegalArgumentException("Invalid input to toASCII: " + str, e);
        }
    }

    public static String toASCII(String str) {
        return toASCII(str, 0);
    }

    public static String toUnicode(String str, int i) {
        try {
            return convertFullStop(IDNA.convertIDNToUnicode(str, i)).toString();
        } catch (StringPrepParseException e) {
            return str;
        }
    }

    private static boolean isLabelSeperator(char c) {
        return c == 12290 || c == 65294 || c == 65377;
    }

    private static StringBuffer convertFullStop(StringBuffer stringBuffer) {
        for (int i = 0; i < stringBuffer.length(); i++) {
            if (isLabelSeperator(stringBuffer.charAt(i))) {
                stringBuffer.setCharAt(i, '.');
            }
        }
        return stringBuffer;
    }

    public static String toUnicode(String str) {
        return toUnicode(str, 0);
    }

    private IDN() {
    }
}
