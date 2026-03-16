package android.text;

public class SpannableString extends SpannableStringInternal implements CharSequence, GetChars, Spannable {
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

    public SpannableString(CharSequence source) {
        super(source, 0, source.length());
    }

    private SpannableString(CharSequence source, int start, int end) {
        super(source, start, end);
    }

    public static SpannableString valueOf(CharSequence source) {
        return source instanceof SpannableString ? (SpannableString) source : new SpannableString(source);
    }

    @Override
    public void setSpan(Object what, int start, int end, int flags) {
        super.setSpan(what, start, end, flags);
    }

    @Override
    public void removeSpan(Object what) {
        super.removeSpan(what);
    }

    @Override
    public final CharSequence subSequence(int start, int end) {
        return new SpannableString(this, start, end);
    }
}
