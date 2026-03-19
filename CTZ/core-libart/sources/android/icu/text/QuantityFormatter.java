package android.icu.text;

import android.icu.impl.SimpleFormatterImpl;
import android.icu.impl.StandardPlural;
import android.icu.text.PluralRules;
import java.text.FieldPosition;

class QuantityFormatter {
    static final boolean $assertionsDisabled = false;
    private final SimpleFormatter[] templates = new SimpleFormatter[StandardPlural.COUNT];

    public void addIfAbsent(CharSequence charSequence, String str) {
        int iIndexFromString = StandardPlural.indexFromString(charSequence);
        if (this.templates[iIndexFromString] != null) {
            return;
        }
        this.templates[iIndexFromString] = SimpleFormatter.compileMinMaxArguments(str, 0, 1);
    }

    public boolean isValid() {
        return this.templates[StandardPlural.OTHER_INDEX] != null;
    }

    public String format(double d, NumberFormat numberFormat, PluralRules pluralRules) {
        String str = numberFormat.format(d);
        SimpleFormatter simpleFormatter = this.templates[selectPlural(d, numberFormat, pluralRules).ordinal()];
        if (simpleFormatter == null) {
            simpleFormatter = this.templates[StandardPlural.OTHER_INDEX];
        }
        return simpleFormatter.format(str);
    }

    public SimpleFormatter getByVariant(CharSequence charSequence) {
        int iIndexOrOtherIndexFromString = StandardPlural.indexOrOtherIndexFromString(charSequence);
        SimpleFormatter simpleFormatter = this.templates[iIndexOrOtherIndexFromString];
        if (simpleFormatter != null || iIndexOrOtherIndexFromString == StandardPlural.OTHER_INDEX) {
            return simpleFormatter;
        }
        return this.templates[StandardPlural.OTHER_INDEX];
    }

    public static StandardPlural selectPlural(double d, NumberFormat numberFormat, PluralRules pluralRules) {
        String strSelect;
        if (numberFormat instanceof DecimalFormat) {
            strSelect = pluralRules.select(((DecimalFormat) numberFormat).getFixedDecimal(d));
        } else {
            strSelect = pluralRules.select(d);
        }
        return StandardPlural.orOtherFromString(strSelect);
    }

    public static StandardPlural selectPlural(Number number, NumberFormat numberFormat, PluralRules pluralRules, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        UFieldPosition uFieldPosition = new UFieldPosition(fieldPosition.getFieldAttribute(), fieldPosition.getField());
        numberFormat.format(number, stringBuffer, uFieldPosition);
        String strSelect = pluralRules.select(new PluralRules.FixedDecimal(number.doubleValue(), uFieldPosition.getCountVisibleFractionDigits(), uFieldPosition.getFractionDigits()));
        fieldPosition.setBeginIndex(uFieldPosition.getBeginIndex());
        fieldPosition.setEndIndex(uFieldPosition.getEndIndex());
        return StandardPlural.orOtherFromString(strSelect);
    }

    public static StringBuilder format(String str, CharSequence charSequence, StringBuilder sb, FieldPosition fieldPosition) {
        int[] iArr = new int[1];
        SimpleFormatterImpl.formatAndAppend(str, sb, iArr, charSequence);
        if (fieldPosition.getBeginIndex() != 0 || fieldPosition.getEndIndex() != 0) {
            if (iArr[0] >= 0) {
                fieldPosition.setBeginIndex(fieldPosition.getBeginIndex() + iArr[0]);
                fieldPosition.setEndIndex(fieldPosition.getEndIndex() + iArr[0]);
            } else {
                fieldPosition.setBeginIndex(0);
                fieldPosition.setEndIndex(0);
            }
        }
        return sb;
    }
}
