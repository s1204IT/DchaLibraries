package android.text;

public class AlteredCharSequence implements CharSequence, GetChars {
    private char[] mChars;
    private int mEnd;
    private CharSequence mSource;
    private int mStart;

    public static AlteredCharSequence make(CharSequence charSequence, char[] cArr, int i, int i2) {
        if (charSequence instanceof Spanned) {
            return new AlteredSpanned(charSequence, cArr, i, i2);
        }
        return new AlteredCharSequence(charSequence, cArr, i, i2);
    }

    private AlteredCharSequence(CharSequence charSequence, char[] cArr, int i, int i2) {
        this.mSource = charSequence;
        this.mChars = cArr;
        this.mStart = i;
        this.mEnd = i2;
    }

    void update(char[] cArr, int i, int i2) {
        this.mChars = cArr;
        this.mStart = i;
        this.mEnd = i2;
    }

    private static class AlteredSpanned extends AlteredCharSequence implements Spanned {
        private Spanned mSpanned;

        private AlteredSpanned(CharSequence charSequence, char[] cArr, int i, int i2) {
            super(charSequence, cArr, i, i2);
            this.mSpanned = (Spanned) charSequence;
        }

        @Override
        public <T> T[] getSpans(int i, int i2, Class<T> cls) {
            return (T[]) this.mSpanned.getSpans(i, i2, cls);
        }

        @Override
        public int getSpanStart(Object obj) {
            return this.mSpanned.getSpanStart(obj);
        }

        @Override
        public int getSpanEnd(Object obj) {
            return this.mSpanned.getSpanEnd(obj);
        }

        @Override
        public int getSpanFlags(Object obj) {
            return this.mSpanned.getSpanFlags(obj);
        }

        @Override
        public int nextSpanTransition(int i, int i2, Class cls) {
            return this.mSpanned.nextSpanTransition(i, i2, cls);
        }
    }

    @Override
    public char charAt(int i) {
        if (i >= this.mStart && i < this.mEnd) {
            return this.mChars[i - this.mStart];
        }
        return this.mSource.charAt(i);
    }

    @Override
    public int length() {
        return this.mSource.length();
    }

    @Override
    public CharSequence subSequence(int i, int i2) {
        return make(this.mSource.subSequence(i, i2), this.mChars, this.mStart - i, this.mEnd - i);
    }

    @Override
    public void getChars(int i, int i2, char[] cArr, int i3) {
        TextUtils.getChars(this.mSource, i, i2, cArr, i3);
        int iMax = Math.max(this.mStart, i);
        int iMin = Math.min(this.mEnd, i2);
        if (iMax > iMin) {
            System.arraycopy(this.mChars, iMax - this.mStart, cArr, i3, iMin - iMax);
        }
    }

    @Override
    public String toString() {
        int length = length();
        char[] cArr = new char[length];
        getChars(0, length, cArr, 0);
        return String.valueOf(cArr);
    }
}
