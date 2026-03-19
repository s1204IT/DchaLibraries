package android.text;

public final class SpannedString extends SpannableStringInternal implements CharSequence, GetChars, Spanned {
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int getSpanEnd(Object obj) {
        return super.getSpanEnd(obj);
    }

    @Override
    public int getSpanFlags(Object obj) {
        return super.getSpanFlags(obj);
    }

    @Override
    public int getSpanStart(Object obj) {
        return super.getSpanStart(obj);
    }

    @Override
    public Object[] getSpans(int i, int i2, Class cls) {
        return super.getSpans(i, i2, cls);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public int nextSpanTransition(int i, int i2, Class cls) {
        return super.nextSpanTransition(i, i2, cls);
    }

    @Override
    public void removeSpan(Object obj, int i) {
        super.removeSpan(obj, i);
    }

    public SpannedString(CharSequence charSequence, boolean z) {
        super(charSequence, 0, charSequence.length(), z);
    }

    public SpannedString(CharSequence charSequence) {
        this(charSequence, false);
    }

    private SpannedString(CharSequence charSequence, int i, int i2) {
        super(charSequence, i, i2, false);
    }

    @Override
    public CharSequence subSequence(int i, int i2) {
        return new SpannedString(this, i, i2);
    }

    public static SpannedString valueOf(CharSequence charSequence) {
        if (charSequence instanceof SpannedString) {
            return (SpannedString) charSequence;
        }
        return new SpannedString(charSequence);
    }
}
