package android.icu.impl.coll;

import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.util.ICUUncheckedIOException;
import android.icu.util.Output;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.io.IOException;
import java.util.MissingResourceException;

public final class CollationLoader {
    private static volatile String rootRules = null;

    private CollationLoader() {
    }

    private static void loadRootRules() {
        if (rootRules != null) {
            return;
        }
        synchronized (CollationLoader.class) {
            if (rootRules == null) {
                rootRules = UResourceBundle.getBundleInstance(ICUData.ICU_COLLATION_BASE_NAME, ULocale.ROOT).getString("UCARules");
            }
        }
    }

    public static String getRootRules() {
        loadRootRules();
        return rootRules;
    }

    private static final class ASCII {
        private ASCII() {
        }

        static String toLowerCase(String str) {
            int i = 0;
            while (i < str.length()) {
                char cCharAt = str.charAt(i);
                if ('A' > cCharAt || cCharAt > 'Z') {
                    i++;
                } else {
                    StringBuilder sb = new StringBuilder(str.length());
                    sb.append((CharSequence) str, 0, i);
                    sb.append((char) (cCharAt + ' '));
                    while (true) {
                        i++;
                        if (i < str.length()) {
                            char cCharAt2 = str.charAt(i);
                            if ('A' <= cCharAt2 && cCharAt2 <= 'Z') {
                                cCharAt2 = (char) (cCharAt2 + ' ');
                            }
                            sb.append(cCharAt2);
                        } else {
                            return sb.toString();
                        }
                    }
                }
            }
            return str;
        }
    }

    static String loadRules(ULocale uLocale, String str) {
        return ((ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_COLLATION_BASE_NAME, uLocale)).getWithFallback("collations/" + ASCII.toLowerCase(str)).getString("Sequence");
    }

    private static final UResourceBundle findWithFallback(UResourceBundle uResourceBundle, String str) {
        return ((ICUResourceBundle) uResourceBundle).findWithFallback(str);
    }

    public static CollationTailoring loadTailoring(ULocale uLocale, Output<ULocale> output) {
        ?? r2;
        String lowerCase;
        ULocale uLocale2;
        String strFindStringWithFallback;
        CollationTailoring root = CollationRoot.getRoot();
        String name = uLocale.getName();
        if (name.length() == 0 || name.equals("root")) {
            output.value = ULocale.ROOT;
            return root;
        }
        try {
            ICUResourceBundle bundleInstance = ICUResourceBundle.getBundleInstance(ICUData.ICU_COLLATION_BASE_NAME, uLocale, ICUResourceBundle.OpenType.LOCALE_ROOT);
            ULocale uLocale3 = bundleInstance.getULocale();
            String name2 = uLocale3.getName();
            if (name2.length() != 0) {
                r2 = uLocale3;
                if (name2.equals("root")) {
                    r2 = ULocale.ROOT;
                }
            }
            output.value = r2;
            try {
                UResourceBundle uResourceBundle = bundleInstance.get("collations");
                if (uResourceBundle == null) {
                    return root;
                }
                String keywordValue = uLocale.getKeywordValue("collation");
                String str = "standard";
                String strFindStringWithFallback2 = ((ICUResourceBundle) uResourceBundle).findStringWithFallback("default");
                if (strFindStringWithFallback2 != null) {
                    str = strFindStringWithFallback2;
                }
                if (keywordValue != null && !keywordValue.equals("default")) {
                    lowerCase = ASCII.toLowerCase(keywordValue);
                } else {
                    lowerCase = str;
                }
                UResourceBundle uResourceBundleFindWithFallback = findWithFallback(uResourceBundle, lowerCase);
                if (uResourceBundleFindWithFallback == null && lowerCase.length() > 6 && lowerCase.startsWith("search")) {
                    lowerCase = "search";
                    uResourceBundleFindWithFallback = findWithFallback(uResourceBundle, "search");
                }
                if (uResourceBundleFindWithFallback == null && !lowerCase.equals(str)) {
                    uResourceBundleFindWithFallback = findWithFallback(uResourceBundle, str);
                    lowerCase = str;
                }
                if (uResourceBundleFindWithFallback == null && !lowerCase.equals("standard")) {
                    lowerCase = "standard";
                    uResourceBundleFindWithFallback = findWithFallback(uResourceBundle, "standard");
                }
                if (uResourceBundleFindWithFallback == null) {
                    return root;
                }
                ULocale uLocale4 = uResourceBundleFindWithFallback.getULocale();
                String name3 = uLocale4.getName();
                if (name3.length() != 0) {
                    uLocale2 = uLocale4;
                    if (name3.equals("root")) {
                        uLocale2 = ULocale.ROOT;
                        if (lowerCase.equals("standard")) {
                            return root;
                        }
                    }
                }
                CollationTailoring collationTailoring = new CollationTailoring(root.settings);
                collationTailoring.actualLocale = uLocale2;
                try {
                    CollationDataReader.read(root, uResourceBundleFindWithFallback.get("%%CollationBin").getBinary(), collationTailoring);
                    try {
                        collationTailoring.setRulesResource(uResourceBundleFindWithFallback.get("Sequence"));
                    } catch (MissingResourceException e) {
                    }
                    if (!lowerCase.equals(str)) {
                        output.value = r2.setKeywordValue("collation", lowerCase);
                    }
                    if (uLocale2.equals(r2) || (strFindStringWithFallback = ((ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_COLLATION_BASE_NAME, uLocale2)).findStringWithFallback("collations/default")) == null) {
                        strFindStringWithFallback = str;
                    }
                    if (!lowerCase.equals(strFindStringWithFallback)) {
                        collationTailoring.actualLocale = collationTailoring.actualLocale.setKeywordValue("collation", lowerCase);
                    }
                    return collationTailoring;
                } catch (IOException e2) {
                    throw new ICUUncheckedIOException("Failed to load collation tailoring data for locale:" + uLocale2 + " type:" + lowerCase, e2);
                }
            } catch (MissingResourceException e3) {
                return root;
            }
        } catch (MissingResourceException e4) {
            output.value = ULocale.ROOT;
            return root;
        }
    }
}
