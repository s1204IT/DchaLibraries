package java.text;

import java.util.Comparator;
import java.util.Locale;
import libcore.icu.ICU;

public abstract class Collator implements Comparator<Object>, Cloneable {
    public static final int CANONICAL_DECOMPOSITION = 1;
    public static final int FULL_DECOMPOSITION = 2;
    public static final int IDENTICAL = 3;
    public static final int NO_DECOMPOSITION = 0;
    public static final int PRIMARY = 0;
    public static final int SECONDARY = 1;
    public static final int TERTIARY = 2;
    android.icu.text.Collator icuColl;

    public abstract int compare(String str, String str2);

    public abstract CollationKey getCollationKey(String str);

    public abstract int hashCode();

    public static synchronized Collator getInstance() {
        return getInstance(Locale.getDefault());
    }

    public static synchronized Collator getInstance(Locale locale) {
        if (locale == null) {
            throw new NullPointerException("locale == null");
        }
        return new RuleBasedCollator((android.icu.text.RuleBasedCollator) android.icu.text.Collator.getInstance(locale));
    }

    @Override
    public int compare(Object obj, Object obj2) {
        return compare((String) obj, (String) obj2);
    }

    public boolean equals(String str, String str2) {
        return compare(str, str2) == 0;
    }

    public synchronized int getStrength() {
        int strength;
        strength = this.icuColl.getStrength();
        if (strength == 15) {
            strength = 3;
        }
        return strength;
    }

    public synchronized void setStrength(int i) {
        if (i == 3) {
            i = 15;
        }
        this.icuColl.setStrength(i);
    }

    public synchronized int getDecomposition() {
        return decompositionMode_ICU_Java(this.icuColl.getDecomposition());
    }

    public synchronized void setDecomposition(int i) {
        this.icuColl.setDecomposition(decompositionMode_Java_ICU(i));
    }

    public static synchronized Locale[] getAvailableLocales() {
        return ICU.getAvailableCollatorLocales();
    }

    private int decompositionMode_Java_ICU(int i) {
        switch (i) {
            case 0:
                return 16;
            case 1:
                return 17;
            default:
                throw new IllegalArgumentException("Bad mode: " + i);
        }
    }

    private int decompositionMode_ICU_Java(int i) {
        switch (i) {
            case 16:
                return 0;
            case 17:
                return 1;
            default:
                return i;
        }
    }

    public Object clone() {
        try {
            Collator collator = (Collator) super.clone();
            collator.icuColl = (android.icu.text.Collator) this.icuColl.clone();
            return collator;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Collator collator = (Collator) obj;
        if (this.icuColl != null) {
            return this.icuColl.equals(collator.icuColl);
        }
        if (collator.icuColl == null) {
            return true;
        }
        return false;
    }

    protected Collator() {
        this.icuColl = android.icu.text.RuleBasedCollator.getInstance(Locale.getDefault());
    }

    Collator(android.icu.text.Collator collator) {
        this.icuColl = collator;
    }
}
