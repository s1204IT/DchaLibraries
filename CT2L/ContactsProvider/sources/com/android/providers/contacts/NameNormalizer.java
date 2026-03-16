package com.android.providers.contacts;

import com.android.providers.contacts.util.Hex;
import java.text.CollationKey;
import java.text.Collator;
import java.text.RuleBasedCollator;
import java.util.Locale;

public class NameNormalizer {
    private static RuleBasedCollator sCachedComplexityCollator;
    private static RuleBasedCollator sCachedCompressingCollator;
    private static Locale sCollatorLocale;
    private static final Object sCollatorLock = new Object();

    private static void ensureCollators() {
        Locale locale = Locale.getDefault();
        if (!locale.equals(sCollatorLocale)) {
            sCollatorLocale = locale;
            sCachedCompressingCollator = (RuleBasedCollator) Collator.getInstance(locale);
            sCachedCompressingCollator.setStrength(0);
            sCachedCompressingCollator.setDecomposition(1);
            sCachedComplexityCollator = (RuleBasedCollator) Collator.getInstance(locale);
            sCachedComplexityCollator.setStrength(1);
        }
    }

    static RuleBasedCollator getCompressingCollator() {
        RuleBasedCollator ruleBasedCollator;
        synchronized (sCollatorLock) {
            ensureCollators();
            ruleBasedCollator = sCachedCompressingCollator;
        }
        return ruleBasedCollator;
    }

    static RuleBasedCollator getComplexityCollator() {
        RuleBasedCollator ruleBasedCollator;
        synchronized (sCollatorLock) {
            ensureCollators();
            ruleBasedCollator = sCachedComplexityCollator;
        }
        return ruleBasedCollator;
    }

    public static String normalize(String name) {
        CollationKey key = getCompressingCollator().getCollationKey(lettersAndDigitsOnly(name));
        return Hex.encodeHex(key.toByteArray(), true);
    }

    public static int compareComplexity(String name1, String name2) {
        String clean1 = lettersAndDigitsOnly(name1);
        String clean2 = lettersAndDigitsOnly(name2);
        int diff = getComplexityCollator().compare(clean1, clean2);
        if (diff != 0) {
            return diff;
        }
        int diff2 = -clean1.compareTo(clean2);
        return diff2 != 0 ? diff2 : name1.length() - name2.length();
    }

    private static String lettersAndDigitsOnly(String name) {
        if (name == null) {
            return "";
        }
        char[] letters = name.toCharArray();
        int length = 0;
        for (char c : letters) {
            if (Character.isLetterOrDigit(c)) {
                letters[length] = c;
                length++;
            }
        }
        if (length != letters.length) {
            return new String(letters, 0, length);
        }
        return name;
    }
}
