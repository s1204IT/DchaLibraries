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

    public SpannedString(CharSequence source) {
        super(source, 0, source.length());
    }

    private SpannedString(CharSequence source, int start, int end) {
        super(source, start, end);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new SpannedString(this, start, end);
    }

    public static SpannedString valueOf(CharSequence source) {
        return source instanceof SpannedString ? (SpannedString) source : new SpannedString(source);
    }
}
