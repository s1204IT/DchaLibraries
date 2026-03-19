package android.icu.impl.number;

import android.icu.text.NumberFormat;

public class ConstantAffixModifier implements Modifier {
    public static final ConstantAffixModifier EMPTY = new ConstantAffixModifier();
    private final NumberFormat.Field field;
    private final String prefix;
    private final boolean strong;
    private final String suffix;

    public ConstantAffixModifier(String str, String str2, NumberFormat.Field field, boolean z) {
        this.prefix = str == null ? "" : str;
        this.suffix = str2 == null ? "" : str2;
        this.field = field;
        this.strong = z;
    }

    public ConstantAffixModifier() {
        this.prefix = "";
        this.suffix = "";
        this.field = null;
        this.strong = false;
    }

    @Override
    public int apply(NumberStringBuilder numberStringBuilder, int i, int i2) {
        return numberStringBuilder.insert(i2, this.suffix, this.field) + numberStringBuilder.insert(i, this.prefix, this.field);
    }

    @Override
    public int getPrefixLength() {
        return this.prefix.length();
    }

    @Override
    public int getCodePointCount() {
        return this.prefix.codePointCount(0, this.prefix.length()) + this.suffix.codePointCount(0, this.suffix.length());
    }

    @Override
    public boolean isStrong() {
        return this.strong;
    }

    public String toString() {
        return String.format("<ConstantAffixModifier prefix:'%s' suffix:'%s'>", this.prefix, this.suffix);
    }
}
