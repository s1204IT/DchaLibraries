package android.icu.text;

import android.icu.text.Normalizer;
import android.icu.text.UnicodeSet;
import android.icu.util.ICUUncheckedIOException;
import java.io.IOException;

public class FilteredNormalizer2 extends Normalizer2 {
    private Normalizer2 norm2;
    private UnicodeSet set;

    public FilteredNormalizer2(Normalizer2 normalizer2, UnicodeSet unicodeSet) {
        this.norm2 = normalizer2;
        this.set = unicodeSet;
    }

    @Override
    public StringBuilder normalize(CharSequence charSequence, StringBuilder sb) {
        if (sb == charSequence) {
            throw new IllegalArgumentException();
        }
        sb.setLength(0);
        normalize(charSequence, sb, UnicodeSet.SpanCondition.SIMPLE);
        return sb;
    }

    @Override
    public Appendable normalize(CharSequence charSequence, Appendable appendable) {
        if (appendable == charSequence) {
            throw new IllegalArgumentException();
        }
        return normalize(charSequence, appendable, UnicodeSet.SpanCondition.SIMPLE);
    }

    @Override
    public StringBuilder normalizeSecondAndAppend(StringBuilder sb, CharSequence charSequence) {
        return normalizeSecondAndAppend(sb, charSequence, true);
    }

    @Override
    public StringBuilder append(StringBuilder sb, CharSequence charSequence) {
        return normalizeSecondAndAppend(sb, charSequence, false);
    }

    @Override
    public String getDecomposition(int i) {
        if (this.set.contains(i)) {
            return this.norm2.getDecomposition(i);
        }
        return null;
    }

    @Override
    public String getRawDecomposition(int i) {
        if (this.set.contains(i)) {
            return this.norm2.getRawDecomposition(i);
        }
        return null;
    }

    @Override
    public int composePair(int i, int i2) {
        if (this.set.contains(i) && this.set.contains(i2)) {
            return this.norm2.composePair(i, i2);
        }
        return -1;
    }

    @Override
    public int getCombiningClass(int i) {
        if (this.set.contains(i)) {
            return this.norm2.getCombiningClass(i);
        }
        return 0;
    }

    @Override
    public boolean isNormalized(CharSequence charSequence) {
        UnicodeSet.SpanCondition spanCondition;
        UnicodeSet.SpanCondition spanCondition2 = UnicodeSet.SpanCondition.SIMPLE;
        int i = 0;
        while (i < charSequence.length()) {
            int iSpan = this.set.span(charSequence, i, spanCondition2);
            if (spanCondition2 == UnicodeSet.SpanCondition.NOT_CONTAINED) {
                spanCondition = UnicodeSet.SpanCondition.SIMPLE;
            } else {
                if (!this.norm2.isNormalized(charSequence.subSequence(i, iSpan))) {
                    return false;
                }
                spanCondition = UnicodeSet.SpanCondition.NOT_CONTAINED;
            }
            spanCondition2 = spanCondition;
            i = iSpan;
        }
        return true;
    }

    @Override
    public Normalizer.QuickCheckResult quickCheck(CharSequence charSequence) {
        Normalizer.QuickCheckResult quickCheckResult = Normalizer.YES;
        UnicodeSet.SpanCondition spanCondition = UnicodeSet.SpanCondition.SIMPLE;
        int i = 0;
        while (i < charSequence.length()) {
            int iSpan = this.set.span(charSequence, i, spanCondition);
            if (spanCondition == UnicodeSet.SpanCondition.NOT_CONTAINED) {
                spanCondition = UnicodeSet.SpanCondition.SIMPLE;
            } else {
                Normalizer.QuickCheckResult quickCheckResultQuickCheck = this.norm2.quickCheck(charSequence.subSequence(i, iSpan));
                if (quickCheckResultQuickCheck == Normalizer.NO) {
                    return quickCheckResultQuickCheck;
                }
                if (quickCheckResultQuickCheck == Normalizer.MAYBE) {
                    quickCheckResult = quickCheckResultQuickCheck;
                }
                spanCondition = UnicodeSet.SpanCondition.NOT_CONTAINED;
            }
            i = iSpan;
        }
        return quickCheckResult;
    }

    @Override
    public int spanQuickCheckYes(CharSequence charSequence) {
        UnicodeSet.SpanCondition spanCondition = UnicodeSet.SpanCondition.SIMPLE;
        int i = 0;
        while (i < charSequence.length()) {
            int iSpan = this.set.span(charSequence, i, spanCondition);
            if (spanCondition == UnicodeSet.SpanCondition.NOT_CONTAINED) {
                spanCondition = UnicodeSet.SpanCondition.SIMPLE;
            } else {
                int iSpanQuickCheckYes = i + this.norm2.spanQuickCheckYes(charSequence.subSequence(i, iSpan));
                if (iSpanQuickCheckYes < iSpan) {
                    return iSpanQuickCheckYes;
                }
                spanCondition = UnicodeSet.SpanCondition.NOT_CONTAINED;
            }
            i = iSpan;
        }
        return charSequence.length();
    }

    @Override
    public boolean hasBoundaryBefore(int i) {
        return !this.set.contains(i) || this.norm2.hasBoundaryBefore(i);
    }

    @Override
    public boolean hasBoundaryAfter(int i) {
        return !this.set.contains(i) || this.norm2.hasBoundaryAfter(i);
    }

    @Override
    public boolean isInert(int i) {
        return !this.set.contains(i) || this.norm2.isInert(i);
    }

    private Appendable normalize(CharSequence charSequence, Appendable appendable, UnicodeSet.SpanCondition spanCondition) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < charSequence.length()) {
            try {
                int iSpan = this.set.span(charSequence, i, spanCondition);
                int i2 = iSpan - i;
                if (spanCondition == UnicodeSet.SpanCondition.NOT_CONTAINED) {
                    if (i2 != 0) {
                        appendable.append(charSequence, i, iSpan);
                    }
                    spanCondition = UnicodeSet.SpanCondition.SIMPLE;
                } else {
                    if (i2 != 0) {
                        appendable.append(this.norm2.normalize(charSequence.subSequence(i, iSpan), sb));
                    }
                    spanCondition = UnicodeSet.SpanCondition.NOT_CONTAINED;
                }
                i = iSpan;
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }
        return appendable;
    }

    private StringBuilder normalizeSecondAndAppend(StringBuilder sb, CharSequence charSequence, boolean z) {
        if (sb == charSequence) {
            throw new IllegalArgumentException();
        }
        if (sb.length() == 0) {
            if (z) {
                return normalize(charSequence, sb);
            }
            sb.append(charSequence);
            return sb;
        }
        int iSpan = this.set.span(charSequence, 0, UnicodeSet.SpanCondition.SIMPLE);
        if (iSpan != 0) {
            CharSequence charSequenceSubSequence = charSequence.subSequence(0, iSpan);
            int iSpanBack = this.set.spanBack(sb, Integer.MAX_VALUE, UnicodeSet.SpanCondition.SIMPLE);
            if (iSpanBack == 0) {
                if (z) {
                    this.norm2.normalizeSecondAndAppend(sb, charSequenceSubSequence);
                } else {
                    this.norm2.append(sb, charSequenceSubSequence);
                }
            } else {
                StringBuilder sb2 = new StringBuilder(sb.subSequence(iSpanBack, sb.length()));
                if (z) {
                    this.norm2.normalizeSecondAndAppend(sb2, charSequenceSubSequence);
                } else {
                    this.norm2.append(sb2, charSequenceSubSequence);
                }
                sb.delete(iSpanBack, Integer.MAX_VALUE).append((CharSequence) sb2);
            }
        }
        if (iSpan < charSequence.length()) {
            CharSequence charSequenceSubSequence2 = charSequence.subSequence(iSpan, charSequence.length());
            if (z) {
                normalize(charSequenceSubSequence2, sb, UnicodeSet.SpanCondition.NOT_CONTAINED);
            } else {
                sb.append(charSequenceSubSequence2);
            }
        }
        return sb;
    }
}
