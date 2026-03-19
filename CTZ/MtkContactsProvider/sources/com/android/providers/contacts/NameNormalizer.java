package com.android.providers.contacts;

import com.android.providers.contacts.util.Hex;
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
        if (locale.equals(sCollatorLocale)) {
            return;
        }
        sCollatorLocale = locale;
        sCachedCompressingCollator = (RuleBasedCollator) Collator.getInstance(locale);
        sCachedCompressingCollator.setStrength(0);
        sCachedCompressingCollator.setDecomposition(1);
        sCachedComplexityCollator = (RuleBasedCollator) Collator.getInstance(locale);
        sCachedComplexityCollator.setStrength(1);
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

    public static String normalize(String str) {
        return Hex.encodeHex(getCompressingCollator().getCollationKey(lettersAndDigitsOnly(str)).toByteArray(), true);
    }

    public static int compareComplexity(String str, String str2) {
        String strLettersAndDigitsOnly = lettersAndDigitsOnly(str);
        String strLettersAndDigitsOnly2 = lettersAndDigitsOnly(str2);
        int iCompare = getComplexityCollator().compare(strLettersAndDigitsOnly, strLettersAndDigitsOnly2);
        if (iCompare != 0) {
            return iCompare;
        }
        int i = -strLettersAndDigitsOnly.compareTo(strLettersAndDigitsOnly2);
        if (i != 0) {
            return i;
        }
        return str.length() - str2.length();
    }

    private static String lettersAndDigitsOnly(String str) {
        if (str == null) {
            return "";
        }
        char[] charArray = str.toCharArray();
        int i = 0;
        for (char c : charArray) {
            if (Character.isLetterOrDigit(c)) {
                charArray[i] = c;
                i++;
            }
        }
        if (i != charArray.length) {
            return new String(charArray, 0, i);
        }
        return str;
    }
}
