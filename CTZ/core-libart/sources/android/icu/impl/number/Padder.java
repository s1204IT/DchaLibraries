package android.icu.impl.number;

import android.icu.text.NumberFormat;

public class Padder {
    static final boolean $assertionsDisabled = false;
    public static final String FALLBACK_PADDING_STRING = " ";
    public static final Padder NONE = new Padder(null, -1, null);
    String paddingString;
    PadPosition position;
    int targetWidth;

    public enum PadPosition {
        BEFORE_PREFIX,
        AFTER_PREFIX,
        BEFORE_SUFFIX,
        AFTER_SUFFIX;

        public static PadPosition fromOld(int i) {
            switch (i) {
                case 0:
                    return BEFORE_PREFIX;
                case 1:
                    return AFTER_PREFIX;
                case 2:
                    return BEFORE_SUFFIX;
                case 3:
                    return AFTER_SUFFIX;
                default:
                    throw new IllegalArgumentException("Don't know how to map " + i);
            }
        }

        public int toOld() {
            switch (this) {
                case BEFORE_PREFIX:
                    return 0;
                case AFTER_PREFIX:
                    return 1;
                case BEFORE_SUFFIX:
                    return 2;
                case AFTER_SUFFIX:
                    return 3;
                default:
                    return -1;
            }
        }
    }

    public Padder(String str, int i, PadPosition padPosition) {
        this.paddingString = str == null ? FALLBACK_PADDING_STRING : str;
        this.targetWidth = i;
        this.position = padPosition == null ? PadPosition.BEFORE_PREFIX : padPosition;
    }

    public static Padder none() {
        return NONE;
    }

    public static Padder codePoints(int i, int i2, PadPosition padPosition) {
        if (i2 >= 0) {
            return new Padder(String.valueOf(Character.toChars(i)), i2, padPosition);
        }
        throw new IllegalArgumentException("Padding width must not be negative");
    }

    public boolean isValid() {
        return this.targetWidth > 0;
    }

    public int padAndApply(Modifier modifier, Modifier modifier2, NumberStringBuilder numberStringBuilder, int i, int i2) {
        int codePointCount = (this.targetWidth - (modifier.getCodePointCount() + modifier2.getCodePointCount())) - numberStringBuilder.codePointCount();
        int iAddPaddingHelper = 0;
        if (codePointCount <= 0) {
            int iApply = 0 + modifier.apply(numberStringBuilder, i, i2);
            return iApply + modifier2.apply(numberStringBuilder, i, i2 + iApply);
        }
        if (this.position == PadPosition.AFTER_PREFIX) {
            iAddPaddingHelper = 0 + addPaddingHelper(this.paddingString, codePointCount, numberStringBuilder, i);
        } else if (this.position == PadPosition.BEFORE_SUFFIX) {
            iAddPaddingHelper = 0 + addPaddingHelper(this.paddingString, codePointCount, numberStringBuilder, i2 + 0);
        }
        int iApply2 = iAddPaddingHelper + modifier.apply(numberStringBuilder, i, i2 + iAddPaddingHelper);
        int iApply3 = iApply2 + modifier2.apply(numberStringBuilder, i, i2 + iApply2);
        if (this.position == PadPosition.BEFORE_PREFIX) {
            return iApply3 + addPaddingHelper(this.paddingString, codePointCount, numberStringBuilder, i);
        }
        if (this.position == PadPosition.AFTER_SUFFIX) {
            return iApply3 + addPaddingHelper(this.paddingString, codePointCount, numberStringBuilder, i2 + iApply3);
        }
        return iApply3;
    }

    private static int addPaddingHelper(String str, int i, NumberStringBuilder numberStringBuilder, int i2) {
        for (int i3 = 0; i3 < i; i3++) {
            numberStringBuilder.insert(i2, str, (NumberFormat.Field) null);
        }
        return str.length() * i;
    }
}
