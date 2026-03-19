package android.icu.text;

import android.icu.impl.ICUCache;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.SimpleCache;
import android.icu.lang.UCharacter;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import android.icu.util.UResourceBundleIterator;
import java.util.ArrayList;
import java.util.Locale;
import java.util.MissingResourceException;

public class NumberingSystem {
    private static ICUCache<String, NumberingSystem> cachedLocaleData = new SimpleCache();
    private static ICUCache<String, NumberingSystem> cachedStringData = new SimpleCache();
    private int radix = 10;
    private boolean algorithmic = false;
    private String desc = "0123456789";
    private String name = "latn";

    public static NumberingSystem getInstance(int radix_in, boolean isAlgorithmic_in, String desc_in) {
        return getInstance(null, radix_in, isAlgorithmic_in, desc_in);
    }

    private static NumberingSystem getInstance(String name_in, int radix_in, boolean isAlgorithmic_in, String desc_in) {
        if (radix_in < 2) {
            throw new IllegalArgumentException("Invalid radix for numbering system");
        }
        if (!isAlgorithmic_in && (desc_in.length() != radix_in || !isValidDigitString(desc_in))) {
            throw new IllegalArgumentException("Invalid digit string for numbering system");
        }
        NumberingSystem ns = new NumberingSystem();
        ns.radix = radix_in;
        ns.algorithmic = isAlgorithmic_in;
        ns.desc = desc_in;
        ns.name = name_in;
        return ns;
    }

    public static NumberingSystem getInstance(Locale inLocale) {
        return getInstance(ULocale.forLocale(inLocale));
    }

    public static NumberingSystem getInstance(ULocale locale) {
        String[] OTHER_NS_KEYWORDS = {"native", "traditional", "finance"};
        Boolean nsResolved = true;
        String numbersKeyword = locale.getKeywordValue("numbers");
        if (numbersKeyword != null) {
            int length = OTHER_NS_KEYWORDS.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                String keyword = OTHER_NS_KEYWORDS[i];
                if (!numbersKeyword.equals(keyword)) {
                    i++;
                } else {
                    nsResolved = false;
                    break;
                }
            }
        } else {
            numbersKeyword = "default";
            nsResolved = false;
        }
        if (nsResolved.booleanValue()) {
            NumberingSystem ns = getInstanceByName(numbersKeyword);
            if (ns != null) {
                return ns;
            }
            numbersKeyword = "default";
            nsResolved = false;
        }
        String baseName = locale.getBaseName();
        NumberingSystem ns2 = cachedLocaleData.get(baseName + "@numbers=" + numbersKeyword);
        if (ns2 != null) {
            return ns2;
        }
        String originalNumbersKeyword = numbersKeyword;
        String resolvedNumberingSystem = null;
        while (!nsResolved.booleanValue()) {
            try {
                ICUResourceBundle rb = (ICUResourceBundle) UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", locale);
                resolvedNumberingSystem = rb.getWithFallback("NumberElements").getStringWithFallback(numbersKeyword);
                nsResolved = true;
            } catch (MissingResourceException e) {
                if (numbersKeyword.equals("native") || numbersKeyword.equals("finance")) {
                    numbersKeyword = "default";
                } else if (numbersKeyword.equals("traditional")) {
                    numbersKeyword = "native";
                } else {
                    nsResolved = true;
                }
            }
        }
        if (resolvedNumberingSystem != null) {
            ns2 = getInstanceByName(resolvedNumberingSystem);
        }
        if (ns2 == null) {
            ns2 = new NumberingSystem();
        }
        cachedLocaleData.put(baseName + "@numbers=" + originalNumbersKeyword, ns2);
        return ns2;
    }

    public static NumberingSystem getInstance() {
        return getInstance(ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public static NumberingSystem getInstanceByName(String name) {
        NumberingSystem ns = cachedStringData.get(name);
        if (ns != null) {
            return ns;
        }
        try {
            UResourceBundle numberingSystemsInfo = UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", "numberingSystems");
            UResourceBundle nsCurrent = numberingSystemsInfo.get("numberingSystems");
            UResourceBundle nsTop = nsCurrent.get(name);
            String description = nsTop.getString("desc");
            UResourceBundle nsRadixBundle = nsTop.get("radix");
            UResourceBundle nsAlgBundle = nsTop.get("algorithmic");
            int radix = nsRadixBundle.getInt();
            int algorithmic = nsAlgBundle.getInt();
            boolean isAlgorithmic = algorithmic == 1;
            NumberingSystem ns2 = getInstance(name, radix, isAlgorithmic, description);
            cachedStringData.put(name, ns2);
            return ns2;
        } catch (MissingResourceException e) {
            return null;
        }
    }

    public static String[] getAvailableNames() {
        UResourceBundle numberingSystemsInfo = UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", "numberingSystems");
        UResourceBundle nsCurrent = numberingSystemsInfo.get("numberingSystems");
        ArrayList<String> output = new ArrayList<>();
        UResourceBundleIterator it = nsCurrent.getIterator();
        while (it.hasNext()) {
            UResourceBundle temp = it.next();
            String nsName = temp.getKey();
            output.add(nsName);
        }
        return (String[]) output.toArray(new String[output.size()]);
    }

    public static boolean isValidDigitString(String str) {
        int i = 0;
        UCharacterIterator it = UCharacterIterator.getInstance(str);
        it.setToStart();
        while (true) {
            int c = it.nextCodePoint();
            if (c == -1) {
                return i == 10;
            }
            if (UCharacter.isSupplementary(c)) {
                return false;
            }
            i++;
        }
    }

    public int getRadix() {
        return this.radix;
    }

    public String getDescription() {
        return this.desc;
    }

    public String getName() {
        return this.name;
    }

    public boolean isAlgorithmic() {
        return this.algorithmic;
    }
}
