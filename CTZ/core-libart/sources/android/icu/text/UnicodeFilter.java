package android.icu.text;

public abstract class UnicodeFilter implements UnicodeMatcher {
    public abstract boolean contains(int i);

    @Override
    public int matches(Replaceable replaceable, int[] iArr, int i, boolean z) {
        if (iArr[0] < i) {
            int iChar32At = replaceable.char32At(iArr[0]);
            if (contains(iChar32At)) {
                iArr[0] = iArr[0] + UTF16.getCharCount(iChar32At);
                return 2;
            }
        }
        if (iArr[0] <= i || !contains(replaceable.char32At(iArr[0]))) {
            return (z && iArr[0] == i) ? 1 : 0;
        }
        iArr[0] = iArr[0] - 1;
        if (iArr[0] >= 0) {
            iArr[0] = iArr[0] - (UTF16.getCharCount(replaceable.char32At(iArr[0])) - 1);
        }
        return 2;
    }

    @Deprecated
    protected UnicodeFilter() {
    }
}
