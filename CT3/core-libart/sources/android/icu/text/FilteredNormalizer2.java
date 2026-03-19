package android.icu.text;

import android.icu.text.Normalizer;
import android.icu.text.UnicodeSet;
import android.icu.util.ICUUncheckedIOException;
import java.io.IOException;

public class FilteredNormalizer2 extends Normalizer2 {
    private Normalizer2 norm2;
    private UnicodeSet set;

    public FilteredNormalizer2(Normalizer2 n2, UnicodeSet filterSet) {
        this.norm2 = n2;
        this.set = filterSet;
    }

    @Override
    public StringBuilder normalize(CharSequence src, StringBuilder dest) {
        if (dest == src) {
            throw new IllegalArgumentException();
        }
        dest.setLength(0);
        normalize(src, dest, UnicodeSet.SpanCondition.SIMPLE);
        return dest;
    }

    @Override
    public Appendable normalize(CharSequence src, Appendable dest) {
        if (dest == src) {
            throw new IllegalArgumentException();
        }
        return normalize(src, dest, UnicodeSet.SpanCondition.SIMPLE);
    }

    @Override
    public StringBuilder normalizeSecondAndAppend(StringBuilder first, CharSequence second) {
        return normalizeSecondAndAppend(first, second, true);
    }

    @Override
    public StringBuilder append(StringBuilder first, CharSequence second) {
        return normalizeSecondAndAppend(first, second, false);
    }

    @Override
    public String getDecomposition(int c) {
        if (this.set.contains(c)) {
            return this.norm2.getDecomposition(c);
        }
        return null;
    }

    @Override
    public String getRawDecomposition(int c) {
        if (this.set.contains(c)) {
            return this.norm2.getRawDecomposition(c);
        }
        return null;
    }

    @Override
    public int composePair(int a, int b) {
        if (this.set.contains(a) && this.set.contains(b)) {
            return this.norm2.composePair(a, b);
        }
        return -1;
    }

    @Override
    public int getCombiningClass(int c) {
        if (this.set.contains(c)) {
            return this.norm2.getCombiningClass(c);
        }
        return 0;
    }

    @Override
    public boolean isNormalized(CharSequence s) {
        UnicodeSet.SpanCondition spanCondition = UnicodeSet.SpanCondition.SIMPLE;
        int prevSpanLimit = 0;
        while (prevSpanLimit < s.length()) {
            int spanLimit = this.set.span(s, prevSpanLimit, spanCondition);
            if (spanCondition == UnicodeSet.SpanCondition.NOT_CONTAINED) {
                spanCondition = UnicodeSet.SpanCondition.SIMPLE;
            } else {
                if (!this.norm2.isNormalized(s.subSequence(prevSpanLimit, spanLimit))) {
                    return false;
                }
                spanCondition = UnicodeSet.SpanCondition.NOT_CONTAINED;
            }
            prevSpanLimit = spanLimit;
        }
        return true;
    }

    @Override
    public Normalizer.QuickCheckResult quickCheck(CharSequence s) {
        Normalizer.QuickCheckResult result = Normalizer.YES;
        UnicodeSet.SpanCondition spanCondition = UnicodeSet.SpanCondition.SIMPLE;
        int prevSpanLimit = 0;
        while (prevSpanLimit < s.length()) {
            int spanLimit = this.set.span(s, prevSpanLimit, spanCondition);
            if (spanCondition == UnicodeSet.SpanCondition.NOT_CONTAINED) {
                spanCondition = UnicodeSet.SpanCondition.SIMPLE;
            } else {
                Normalizer.QuickCheckResult qcResult = this.norm2.quickCheck(s.subSequence(prevSpanLimit, spanLimit));
                if (qcResult == Normalizer.NO) {
                    return qcResult;
                }
                if (qcResult == Normalizer.MAYBE) {
                    result = qcResult;
                }
                spanCondition = UnicodeSet.SpanCondition.NOT_CONTAINED;
            }
            prevSpanLimit = spanLimit;
        }
        return result;
    }

    @Override
    public int spanQuickCheckYes(CharSequence s) {
        UnicodeSet.SpanCondition spanCondition = UnicodeSet.SpanCondition.SIMPLE;
        int prevSpanLimit = 0;
        while (prevSpanLimit < s.length()) {
            int spanLimit = this.set.span(s, prevSpanLimit, spanCondition);
            if (spanCondition == UnicodeSet.SpanCondition.NOT_CONTAINED) {
                spanCondition = UnicodeSet.SpanCondition.SIMPLE;
            } else {
                int yesLimit = prevSpanLimit + this.norm2.spanQuickCheckYes(s.subSequence(prevSpanLimit, spanLimit));
                if (yesLimit < spanLimit) {
                    return yesLimit;
                }
                spanCondition = UnicodeSet.SpanCondition.NOT_CONTAINED;
            }
            prevSpanLimit = spanLimit;
        }
        return s.length();
    }

    @Override
    public boolean hasBoundaryBefore(int c) {
        if (this.set.contains(c)) {
            return this.norm2.hasBoundaryBefore(c);
        }
        return true;
    }

    @Override
    public boolean hasBoundaryAfter(int c) {
        if (this.set.contains(c)) {
            return this.norm2.hasBoundaryAfter(c);
        }
        return true;
    }

    @Override
    public boolean isInert(int c) {
        if (this.set.contains(c)) {
            return this.norm2.isInert(c);
        }
        return true;
    }

    private Appendable normalize(CharSequence src, Appendable dest, UnicodeSet.SpanCondition spanCondition) {
        StringBuilder tempDest = new StringBuilder();
        int prevSpanLimit = 0;
        while (prevSpanLimit < src.length()) {
            try {
                int spanLimit = this.set.span(src, prevSpanLimit, spanCondition);
                int spanLength = spanLimit - prevSpanLimit;
                if (spanCondition == UnicodeSet.SpanCondition.NOT_CONTAINED) {
                    if (spanLength != 0) {
                        dest.append(src, prevSpanLimit, spanLimit);
                    }
                    spanCondition = UnicodeSet.SpanCondition.SIMPLE;
                } else {
                    if (spanLength != 0) {
                        dest.append(this.norm2.normalize(src.subSequence(prevSpanLimit, spanLimit), tempDest));
                    }
                    spanCondition = UnicodeSet.SpanCondition.NOT_CONTAINED;
                }
                prevSpanLimit = spanLimit;
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }
        return dest;
    }

    private StringBuilder normalizeSecondAndAppend(StringBuilder first, CharSequence second, boolean doNormalize) {
        if (first == second) {
            throw new IllegalArgumentException();
        }
        if (first.length() == 0) {
            if (doNormalize) {
                return normalize(second, first);
            }
            return first.append(second);
        }
        int prefixLimit = this.set.span(second, 0, UnicodeSet.SpanCondition.SIMPLE);
        if (prefixLimit != 0) {
            CharSequence prefix = second.subSequence(0, prefixLimit);
            int suffixStart = this.set.spanBack(first, Integer.MAX_VALUE, UnicodeSet.SpanCondition.SIMPLE);
            if (suffixStart == 0) {
                if (doNormalize) {
                    this.norm2.normalizeSecondAndAppend(first, prefix);
                } else {
                    this.norm2.append(first, prefix);
                }
            } else {
                StringBuilder middle = new StringBuilder(first.subSequence(suffixStart, first.length()));
                if (doNormalize) {
                    this.norm2.normalizeSecondAndAppend(middle, prefix);
                } else {
                    this.norm2.append(middle, prefix);
                }
                first.delete(suffixStart, Integer.MAX_VALUE).append((CharSequence) middle);
            }
        }
        if (prefixLimit < second.length()) {
            CharSequence rest = second.subSequence(prefixLimit, second.length());
            if (doNormalize) {
                normalize(rest, first, UnicodeSet.SpanCondition.NOT_CONTAINED);
            } else {
                first.append(rest);
            }
        }
        return first;
    }
}
