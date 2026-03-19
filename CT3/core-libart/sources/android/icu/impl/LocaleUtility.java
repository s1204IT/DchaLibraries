package android.icu.impl;

import java.util.Locale;

public class LocaleUtility {
    public static Locale getLocaleFromName(String name) {
        String language;
        String country = "";
        String variant = "";
        int i1 = name.indexOf(95);
        if (i1 < 0) {
            language = name;
        } else {
            language = name.substring(0, i1);
            int i12 = i1 + 1;
            int i2 = name.indexOf(95, i12);
            if (i2 < 0) {
                country = name.substring(i12);
            } else {
                country = name.substring(i12, i2);
                variant = name.substring(i2 + 1);
            }
        }
        return new Locale(language, country, variant);
    }

    public static boolean isFallbackOf(String parent, String child) {
        if (!child.startsWith(parent)) {
            return false;
        }
        int i = parent.length();
        return i == child.length() || child.charAt(i) == '_';
    }

    public static boolean isFallbackOf(Locale parent, Locale child) {
        return isFallbackOf(parent.toString(), child.toString());
    }

    public static Locale fallback(Locale loc) {
        String[] parts = new String[3];
        parts[0] = loc.getLanguage();
        parts[1] = loc.getCountry();
        parts[2] = loc.getVariant();
        int i = 2;
        while (true) {
            if (i < 0) {
                break;
            }
            if (parts[i].length() == 0) {
                i--;
            } else {
                parts[i] = "";
                break;
            }
        }
        if (i < 0) {
            return null;
        }
        return new Locale(parts[0], parts[1], parts[2]);
    }
}
