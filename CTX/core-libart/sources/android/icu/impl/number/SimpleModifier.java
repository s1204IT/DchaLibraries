package android.icu.impl.number;

import android.icu.text.NumberFormat;

public class SimpleModifier implements Modifier {
    static final boolean $assertionsDisabled = false;
    private static final int ARG_NUM_LIMIT = 256;
    private final String compiledPattern;
    private final NumberFormat.Field field;
    private final int prefixLength;
    private final boolean strong;
    private final int suffixLength;
    private final int suffixOffset;

    public SimpleModifier(String str, NumberFormat.Field field, boolean z) {
        this.compiledPattern = str;
        this.field = field;
        this.strong = z;
        if (str.charAt(1) != 0) {
            this.prefixLength = str.charAt(1) - 256;
            this.suffixOffset = this.prefixLength + 3;
        } else {
            this.prefixLength = 0;
            this.suffixOffset = 2;
        }
        if (3 + this.prefixLength < str.length()) {
            this.suffixLength = str.charAt(this.suffixOffset) - 256;
        } else {
            this.suffixLength = 0;
        }
    }

    @Override
    public int apply(NumberStringBuilder numberStringBuilder, int i, int i2) {
        return formatAsPrefixSuffix(numberStringBuilder, i, i2, this.field);
    }

    @Override
    public int getPrefixLength() {
        return this.prefixLength;
    }

    @Override
    public int getCodePointCount() {
        int iCodePointCount = this.prefixLength > 0 ? 0 + Character.codePointCount(this.compiledPattern, 2, this.prefixLength + 2) : 0;
        if (this.suffixLength > 0) {
            return iCodePointCount + Character.codePointCount(this.compiledPattern, this.suffixOffset + 1, 1 + this.suffixOffset + this.suffixLength);
        }
        return iCodePointCount;
    }

    @Override
    public boolean isStrong() {
        return this.strong;
    }

    public int formatAsPrefixSuffix(NumberStringBuilder numberStringBuilder, int i, int i2, NumberFormat.Field field) {
        if (this.prefixLength > 0) {
            numberStringBuilder.insert(i, this.compiledPattern, 2, 2 + this.prefixLength, field);
        }
        if (this.suffixLength > 0) {
            numberStringBuilder.insert(i2 + this.prefixLength, this.compiledPattern, 1 + this.suffixOffset, 1 + this.suffixOffset + this.suffixLength, field);
        }
        return this.prefixLength + this.suffixLength;
    }
}
