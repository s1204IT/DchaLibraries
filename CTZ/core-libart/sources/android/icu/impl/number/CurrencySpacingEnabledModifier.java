package android.icu.impl.number;

import android.icu.text.DecimalFormatSymbols;
import android.icu.text.NumberFormat;
import android.icu.text.UnicodeSet;

public class CurrencySpacingEnabledModifier extends ConstantMultiFieldModifier {
    static final short IN_CURRENCY = 0;
    static final short IN_NUMBER = 1;
    static final byte PREFIX = 0;
    static final byte SUFFIX = 1;
    private static final UnicodeSet UNISET_DIGIT = new UnicodeSet("[:digit:]").freeze();
    private static final UnicodeSet UNISET_NOTS = new UnicodeSet("[:^S:]").freeze();
    private final String afterPrefixInsert;
    private final UnicodeSet afterPrefixUnicodeSet;
    private final String beforeSuffixInsert;
    private final UnicodeSet beforeSuffixUnicodeSet;

    public CurrencySpacingEnabledModifier(NumberStringBuilder numberStringBuilder, NumberStringBuilder numberStringBuilder2, boolean z, DecimalFormatSymbols decimalFormatSymbols) {
        super(numberStringBuilder, numberStringBuilder2, z);
        if (numberStringBuilder.length() > 0 && numberStringBuilder.fieldAt(numberStringBuilder.length() - 1) == NumberFormat.Field.CURRENCY) {
            if (getUnicodeSet(decimalFormatSymbols, (short) 0, (byte) 0).contains(numberStringBuilder.getLastCodePoint())) {
                this.afterPrefixUnicodeSet = getUnicodeSet(decimalFormatSymbols, (short) 1, (byte) 0);
                this.afterPrefixUnicodeSet.freeze();
                this.afterPrefixInsert = getInsertString(decimalFormatSymbols, (byte) 0);
            } else {
                this.afterPrefixUnicodeSet = null;
                this.afterPrefixInsert = null;
            }
        } else {
            this.afterPrefixUnicodeSet = null;
            this.afterPrefixInsert = null;
        }
        if (numberStringBuilder2.length() > 0 && numberStringBuilder2.fieldAt(0) == NumberFormat.Field.CURRENCY) {
            if (getUnicodeSet(decimalFormatSymbols, (short) 0, (byte) 1).contains(numberStringBuilder2.getLastCodePoint())) {
                this.beforeSuffixUnicodeSet = getUnicodeSet(decimalFormatSymbols, (short) 1, (byte) 1);
                this.beforeSuffixUnicodeSet.freeze();
                this.beforeSuffixInsert = getInsertString(decimalFormatSymbols, (byte) 1);
                return;
            } else {
                this.beforeSuffixUnicodeSet = null;
                this.beforeSuffixInsert = null;
                return;
            }
        }
        this.beforeSuffixUnicodeSet = null;
        this.beforeSuffixInsert = null;
    }

    @Override
    public int apply(NumberStringBuilder numberStringBuilder, int i, int i2) {
        int i3 = i2 - i;
        int iInsert = 0;
        if (i3 > 0 && this.afterPrefixUnicodeSet != null && this.afterPrefixUnicodeSet.contains(numberStringBuilder.codePointAt(i))) {
            iInsert = 0 + numberStringBuilder.insert(i, this.afterPrefixInsert, (NumberFormat.Field) null);
        }
        if (i3 > 0 && this.beforeSuffixUnicodeSet != null && this.beforeSuffixUnicodeSet.contains(numberStringBuilder.codePointBefore(i2))) {
            iInsert += numberStringBuilder.insert(i2 + iInsert, this.beforeSuffixInsert, (NumberFormat.Field) null);
        }
        return iInsert + super.apply(numberStringBuilder, i, i2 + iInsert);
    }

    public static int applyCurrencySpacing(NumberStringBuilder numberStringBuilder, int i, int i2, int i3, int i4, DecimalFormatSymbols decimalFormatSymbols) {
        int iApplyCurrencySpacingAffix = 0;
        boolean z = i2 > 0;
        boolean z2 = i4 > 0;
        boolean z3 = (i3 - i) - i2 > 0;
        if (z && z3) {
            iApplyCurrencySpacingAffix = 0 + applyCurrencySpacingAffix(numberStringBuilder, i + i2, (byte) 0, decimalFormatSymbols);
        }
        if (z2 && z3) {
            return iApplyCurrencySpacingAffix + applyCurrencySpacingAffix(numberStringBuilder, i3 + iApplyCurrencySpacingAffix, (byte) 1, decimalFormatSymbols);
        }
        return iApplyCurrencySpacingAffix;
    }

    private static int applyCurrencySpacingAffix(NumberStringBuilder numberStringBuilder, int i, byte b, DecimalFormatSymbols decimalFormatSymbols) {
        if ((b == 0 ? numberStringBuilder.fieldAt(i - 1) : numberStringBuilder.fieldAt(i)) != NumberFormat.Field.CURRENCY) {
            return 0;
        }
        if (!getUnicodeSet(decimalFormatSymbols, (short) 0, b).contains(b == 0 ? numberStringBuilder.codePointBefore(i) : numberStringBuilder.codePointAt(i))) {
            return 0;
        }
        if (getUnicodeSet(decimalFormatSymbols, (short) 1, b).contains(b == 0 ? numberStringBuilder.codePointAt(i) : numberStringBuilder.codePointBefore(i))) {
            return numberStringBuilder.insert(i, getInsertString(decimalFormatSymbols, b), (NumberFormat.Field) null);
        }
        return 0;
    }

    private static UnicodeSet getUnicodeSet(DecimalFormatSymbols decimalFormatSymbols, short s, byte b) {
        String patternForCurrencySpacing = decimalFormatSymbols.getPatternForCurrencySpacing(s == 0 ? 0 : 1, b == 1);
        if (patternForCurrencySpacing.equals("[:digit:]")) {
            return UNISET_DIGIT;
        }
        if (patternForCurrencySpacing.equals("[:^S:]")) {
            return UNISET_NOTS;
        }
        return new UnicodeSet(patternForCurrencySpacing);
    }

    private static String getInsertString(DecimalFormatSymbols decimalFormatSymbols, byte b) {
        return decimalFormatSymbols.getPatternForCurrencySpacing(2, b == 1);
    }
}
