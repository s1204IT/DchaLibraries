package android.text;

public class AlteredCharSequence implements CharSequence, GetChars {
    private char[] mChars;
    private int mEnd;
    private CharSequence mSource;
    private int mStart;

    public static AlteredCharSequence make(CharSequence source, char[] sub, int substart, int subend) {
        return source instanceof Spanned ? new AlteredSpanned(source, sub, substart, subend) : new AlteredCharSequence(source, sub, substart, subend);
    }

    private AlteredCharSequence(CharSequence source, char[] sub, int substart, int subend) {
        this.mSource = source;
        this.mChars = sub;
        this.mStart = substart;
        this.mEnd = subend;
    }

    void update(char[] sub, int substart, int subend) {
        this.mChars = sub;
        this.mStart = substart;
        this.mEnd = subend;
    }

    private static class AlteredSpanned extends AlteredCharSequence implements Spanned {
        private Spanned mSpanned;

        private AlteredSpanned(CharSequence source, char[] sub, int substart, int subend) {
            super(source, sub, substart, subend);
            this.mSpanned = (Spanned) source;
        }

        @Override
        public <T> T[] getSpans(int i, int i2, Class<T> cls) {
            return (T[]) this.mSpanned.getSpans(i, i2, cls);
        }

        @Override
        public int getSpanStart(Object span) {
            return this.mSpanned.getSpanStart(span);
        }

        @Override
        public int getSpanEnd(Object span) {
            return this.mSpanned.getSpanEnd(span);
        }

        @Override
        public int getSpanFlags(Object span) {
            return this.mSpanned.getSpanFlags(span);
        }

        @Override
        public int nextSpanTransition(int start, int end, Class kind) {
            return this.mSpanned.nextSpanTransition(start, end, kind);
        }
    }

    @Override
    public char charAt(int off) {
        return (off < this.mStart || off >= this.mEnd) ? this.mSource.charAt(off) : this.mChars[off - this.mStart];
    }

    @Override
    public int length() {
        return this.mSource.length();
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return make(this.mSource.subSequence(start, end), this.mChars, this.mStart - start, this.mEnd - start);
    }

    @Override
    public void getChars(int start, int end, char[] dest, int off) {
        TextUtils.getChars(this.mSource, start, end, dest, off);
        int start2 = Math.max(this.mStart, start);
        int end2 = Math.min(this.mEnd, end);
        if (start2 > end2) {
            System.arraycopy(this.mChars, start2 - this.mStart, dest, off, end2 - start2);
        }
    }

    @Override
    public String toString() {
        int len = length();
        char[] ret = new char[len];
        getChars(0, len, ret, 0);
        return String.valueOf(ret);
    }
}
