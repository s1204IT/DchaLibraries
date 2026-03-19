package android.icu.impl;

import android.icu.text.PluralRules;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum StandardPlural {
    ZERO(PluralRules.KEYWORD_ZERO),
    ONE(PluralRules.KEYWORD_ONE),
    TWO(PluralRules.KEYWORD_TWO),
    FEW(PluralRules.KEYWORD_FEW),
    MANY(PluralRules.KEYWORD_MANY),
    OTHER(PluralRules.KEYWORD_OTHER);

    private final String keyword;
    public static final int OTHER_INDEX = OTHER.ordinal();
    public static final List<StandardPlural> VALUES = Collections.unmodifiableList(Arrays.asList(values()));
    public static final int COUNT = VALUES.size();

    StandardPlural(String str) {
        this.keyword = str;
    }

    public final String getKeyword() {
        return this.keyword;
    }

    public static final StandardPlural orNullFromString(CharSequence charSequence) {
        switch (charSequence.length()) {
            case 3:
                if (PluralRules.KEYWORD_ONE.contentEquals(charSequence)) {
                    return ONE;
                }
                if (PluralRules.KEYWORD_TWO.contentEquals(charSequence)) {
                    return TWO;
                }
                if (PluralRules.KEYWORD_FEW.contentEquals(charSequence)) {
                    return FEW;
                }
                return null;
            case 4:
                if (PluralRules.KEYWORD_MANY.contentEquals(charSequence)) {
                    return MANY;
                }
                if (PluralRules.KEYWORD_ZERO.contentEquals(charSequence)) {
                    return ZERO;
                }
                return null;
            case 5:
                if (PluralRules.KEYWORD_OTHER.contentEquals(charSequence)) {
                    return OTHER;
                }
                return null;
            default:
                return null;
        }
    }

    public static final StandardPlural orOtherFromString(CharSequence charSequence) {
        StandardPlural standardPluralOrNullFromString = orNullFromString(charSequence);
        return standardPluralOrNullFromString != null ? standardPluralOrNullFromString : OTHER;
    }

    public static final StandardPlural fromString(CharSequence charSequence) {
        StandardPlural standardPluralOrNullFromString = orNullFromString(charSequence);
        if (standardPluralOrNullFromString != null) {
            return standardPluralOrNullFromString;
        }
        throw new IllegalArgumentException(charSequence.toString());
    }

    public static final int indexOrNegativeFromString(CharSequence charSequence) {
        StandardPlural standardPluralOrNullFromString = orNullFromString(charSequence);
        if (standardPluralOrNullFromString != null) {
            return standardPluralOrNullFromString.ordinal();
        }
        return -1;
    }

    public static final int indexOrOtherIndexFromString(CharSequence charSequence) {
        StandardPlural standardPluralOrNullFromString = orNullFromString(charSequence);
        if (standardPluralOrNullFromString == null) {
            standardPluralOrNullFromString = OTHER;
        }
        return standardPluralOrNullFromString.ordinal();
    }

    public static final int indexFromString(CharSequence charSequence) {
        StandardPlural standardPluralOrNullFromString = orNullFromString(charSequence);
        if (standardPluralOrNullFromString != null) {
            return standardPluralOrNullFromString.ordinal();
        }
        throw new IllegalArgumentException(charSequence.toString());
    }
}
