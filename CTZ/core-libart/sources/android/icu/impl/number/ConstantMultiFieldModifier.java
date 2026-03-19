package android.icu.impl.number;

import android.icu.text.NumberFormat;

public class ConstantMultiFieldModifier implements Modifier {
    protected final char[] prefixChars;
    protected final NumberFormat.Field[] prefixFields;
    private final boolean strong;
    protected final char[] suffixChars;
    protected final NumberFormat.Field[] suffixFields;

    public ConstantMultiFieldModifier(NumberStringBuilder numberStringBuilder, NumberStringBuilder numberStringBuilder2, boolean z) {
        this.prefixChars = numberStringBuilder.toCharArray();
        this.suffixChars = numberStringBuilder2.toCharArray();
        this.prefixFields = numberStringBuilder.toFieldArray();
        this.suffixFields = numberStringBuilder2.toFieldArray();
        this.strong = z;
    }

    @Override
    public int apply(NumberStringBuilder numberStringBuilder, int i, int i2) {
        return numberStringBuilder.insert(i2, this.suffixChars, this.suffixFields) + numberStringBuilder.insert(i, this.prefixChars, this.prefixFields);
    }

    @Override
    public int getPrefixLength() {
        return this.prefixChars.length;
    }

    @Override
    public int getCodePointCount() {
        return Character.codePointCount(this.prefixChars, 0, this.prefixChars.length) + Character.codePointCount(this.suffixChars, 0, this.suffixChars.length);
    }

    @Override
    public boolean isStrong() {
        return this.strong;
    }

    public String toString() {
        NumberStringBuilder numberStringBuilder = new NumberStringBuilder();
        apply(numberStringBuilder, 0, 0);
        int prefixLength = getPrefixLength();
        return String.format("<ConstantMultiFieldModifier prefix:'%s' suffix:'%s'>", numberStringBuilder.subSequence(0, prefixLength), numberStringBuilder.subSequence(prefixLength, numberStringBuilder.length()));
    }
}
