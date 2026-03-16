package java.text;

import java.util.Comparator;
import java.util.Locale;
import libcore.icu.ICU;
import libcore.icu.RuleBasedCollatorICU;

public abstract class Collator implements Comparator<Object>, Cloneable {
    public static final int CANONICAL_DECOMPOSITION = 1;
    public static final int FULL_DECOMPOSITION = 2;
    public static final int IDENTICAL = 3;
    public static final int NO_DECOMPOSITION = 0;
    public static final int PRIMARY = 0;
    public static final int SECONDARY = 1;
    public static final int TERTIARY = 2;
    RuleBasedCollatorICU icuColl;

    public abstract int compare(String str, String str2);

    public abstract CollationKey getCollationKey(String str);

    public abstract int hashCode();

    Collator(RuleBasedCollatorICU icuColl) {
        this.icuColl = icuColl;
    }

    protected Collator() {
        this.icuColl = new RuleBasedCollatorICU(Locale.getDefault());
    }

    public Object clone() {
        try {
            Collator clone = (Collator) super.clone();
            clone.icuColl = (RuleBasedCollatorICU) this.icuColl.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public int compare(Object object1, Object object2) {
        return compare((String) object1, (String) object2);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Collator)) {
            return false;
        }
        Collator collator = (Collator) object;
        return this.icuColl == null ? collator.icuColl == null : this.icuColl.equals(collator.icuColl);
    }

    public boolean equals(String string1, String string2) {
        return compare(string1, string2) == 0;
    }

    public static Locale[] getAvailableLocales() {
        return ICU.getAvailableCollatorLocales();
    }

    public int getDecomposition() {
        return decompositionMode_ICU_Java(this.icuColl.getDecomposition());
    }

    public static Collator getInstance() {
        return getInstance(Locale.getDefault());
    }

    public static Collator getInstance(Locale locale) {
        if (locale == null) {
            throw new NullPointerException("locale == null");
        }
        return new RuleBasedCollator(new RuleBasedCollatorICU(locale));
    }

    public int getStrength() {
        return strength_ICU_Java(this.icuColl.getStrength());
    }

    public void setDecomposition(int value) {
        this.icuColl.setDecomposition(decompositionMode_Java_ICU(value));
    }

    public void setStrength(int value) {
        this.icuColl.setStrength(strength_Java_ICU(value));
    }

    private int decompositionMode_Java_ICU(int mode) {
        switch (mode) {
            case 0:
                return 16;
            case 1:
                return 17;
            default:
                throw new IllegalArgumentException("Bad mode: " + mode);
        }
    }

    private int decompositionMode_ICU_Java(int mode) {
        switch (mode) {
            case 16:
                return 0;
            case 17:
                return 1;
            default:
                return mode;
        }
    }

    private int strength_Java_ICU(int value) {
        switch (value) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 15;
            default:
                throw new IllegalArgumentException("Bad strength: " + value);
        }
    }

    private int strength_ICU_Java(int value) {
        switch (value) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 15:
                return 3;
            default:
                return value;
        }
    }
}
