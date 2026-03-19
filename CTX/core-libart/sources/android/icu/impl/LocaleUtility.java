package android.icu.impl;

import java.util.Locale;

public class LocaleUtility {
    public static Locale getLocaleFromName(String str) {
        String strSubstring;
        String strSubstring2;
        String strSubstring3;
        String strSubstring4 = "";
        int iIndexOf = str.indexOf(95);
        if (iIndexOf < 0) {
            strSubstring2 = "";
            strSubstring = str;
        } else {
            strSubstring = str.substring(0, iIndexOf);
            int i = iIndexOf + 1;
            int iIndexOf2 = str.indexOf(95, i);
            if (iIndexOf2 < 0) {
                strSubstring3 = str.substring(i);
                return new Locale(strSubstring, strSubstring3, strSubstring4);
            }
            strSubstring2 = str.substring(i, iIndexOf2);
            strSubstring4 = str.substring(iIndexOf2 + 1);
        }
        strSubstring3 = strSubstring2;
        return new Locale(strSubstring, strSubstring3, strSubstring4);
    }

    public static boolean isFallbackOf(String str, String str2) {
        if (!str2.startsWith(str)) {
            return false;
        }
        int length = str.length();
        return length == str2.length() || str2.charAt(length) == '_';
    }

    public static boolean isFallbackOf(Locale locale, Locale locale2) {
        return isFallbackOf(locale.toString(), locale2.toString());
    }

    public static Locale fallback(Locale locale) {
        String[] strArr = new String[3];
        strArr[0] = locale.getLanguage();
        strArr[1] = locale.getCountry();
        strArr[2] = locale.getVariant();
        int i = 2;
        while (true) {
            if (i < 0) {
                break;
            }
            if (strArr[i].length() == 0) {
                i--;
            } else {
                strArr[i] = "";
                break;
            }
        }
        if (i < 0) {
            return null;
        }
        return new Locale(strArr[0], strArr[1], strArr[2]);
    }
}
