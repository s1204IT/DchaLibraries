package android.icu.text;

import android.icu.text.UnicodeSet;
import android.icu.util.OutputInt;

public class UnicodeSetSpanner {
    private final UnicodeSet unicodeSet;

    public enum CountMethod {
        WHOLE_SPAN,
        MIN_ELEMENTS
    }

    public enum TrimOption {
        LEADING,
        BOTH,
        TRAILING
    }

    public UnicodeSetSpanner(UnicodeSet unicodeSet) {
        this.unicodeSet = unicodeSet;
    }

    public UnicodeSet getUnicodeSet() {
        return this.unicodeSet;
    }

    public boolean equals(Object obj) {
        return (obj instanceof UnicodeSetSpanner) && this.unicodeSet.equals(((UnicodeSetSpanner) obj).unicodeSet);
    }

    public int hashCode() {
        return this.unicodeSet.hashCode();
    }

    public int countIn(CharSequence charSequence) {
        return countIn(charSequence, CountMethod.MIN_ELEMENTS, UnicodeSet.SpanCondition.SIMPLE);
    }

    public int countIn(CharSequence charSequence, CountMethod countMethod) {
        return countIn(charSequence, countMethod, UnicodeSet.SpanCondition.SIMPLE);
    }

    public int countIn(CharSequence charSequence, CountMethod countMethod, UnicodeSet.SpanCondition spanCondition) {
        UnicodeSet.SpanCondition spanCondition2 = spanCondition == UnicodeSet.SpanCondition.NOT_CONTAINED ? UnicodeSet.SpanCondition.SIMPLE : UnicodeSet.SpanCondition.NOT_CONTAINED;
        int length = charSequence.length();
        int iSpan = 0;
        OutputInt outputInt = null;
        int i = 0;
        while (iSpan != length) {
            int iSpan2 = this.unicodeSet.span(charSequence, iSpan, spanCondition2);
            if (iSpan2 == length) {
                break;
            }
            if (countMethod == CountMethod.WHOLE_SPAN) {
                iSpan = this.unicodeSet.span(charSequence, iSpan2, spanCondition);
                i++;
            } else {
                if (outputInt == null) {
                    outputInt = new OutputInt();
                }
                iSpan = this.unicodeSet.spanAndCount(charSequence, iSpan2, spanCondition, outputInt);
                i += outputInt.value;
            }
        }
        return i;
    }

    public String deleteFrom(CharSequence charSequence) {
        return replaceFrom(charSequence, "", CountMethod.WHOLE_SPAN, UnicodeSet.SpanCondition.SIMPLE);
    }

    public String deleteFrom(CharSequence charSequence, UnicodeSet.SpanCondition spanCondition) {
        return replaceFrom(charSequence, "", CountMethod.WHOLE_SPAN, spanCondition);
    }

    public String replaceFrom(CharSequence charSequence, CharSequence charSequence2) {
        return replaceFrom(charSequence, charSequence2, CountMethod.MIN_ELEMENTS, UnicodeSet.SpanCondition.SIMPLE);
    }

    public String replaceFrom(CharSequence charSequence, CharSequence charSequence2, CountMethod countMethod) {
        return replaceFrom(charSequence, charSequence2, countMethod, UnicodeSet.SpanCondition.SIMPLE);
    }

    public String replaceFrom(CharSequence charSequence, CharSequence charSequence2, CountMethod countMethod, UnicodeSet.SpanCondition spanCondition) {
        int iSpanAndCount;
        UnicodeSet.SpanCondition spanCondition2 = spanCondition == UnicodeSet.SpanCondition.NOT_CONTAINED ? UnicodeSet.SpanCondition.SIMPLE : UnicodeSet.SpanCondition.NOT_CONTAINED;
        int i = 0;
        boolean z = charSequence2.length() == 0;
        StringBuilder sb = new StringBuilder();
        int length = charSequence.length();
        OutputInt outputInt = null;
        while (i != length) {
            if (countMethod == CountMethod.WHOLE_SPAN) {
                iSpanAndCount = this.unicodeSet.span(charSequence, i, spanCondition);
            } else {
                if (outputInt == null) {
                    outputInt = new OutputInt();
                }
                iSpanAndCount = this.unicodeSet.spanAndCount(charSequence, i, spanCondition, outputInt);
            }
            if (!z && iSpanAndCount != 0) {
                if (countMethod == CountMethod.WHOLE_SPAN) {
                    sb.append(charSequence2);
                } else {
                    for (int i2 = outputInt.value; i2 > 0; i2--) {
                        sb.append(charSequence2);
                    }
                }
            }
            if (iSpanAndCount == length) {
                break;
            }
            int iSpan = this.unicodeSet.span(charSequence, iSpanAndCount, spanCondition2);
            sb.append(charSequence.subSequence(iSpanAndCount, iSpan));
            i = iSpan;
        }
        return sb.toString();
    }

    public CharSequence trim(CharSequence charSequence) {
        return trim(charSequence, TrimOption.BOTH, UnicodeSet.SpanCondition.SIMPLE);
    }

    public CharSequence trim(CharSequence charSequence, TrimOption trimOption) {
        return trim(charSequence, trimOption, UnicodeSet.SpanCondition.SIMPLE);
    }

    public CharSequence trim(CharSequence charSequence, TrimOption trimOption, UnicodeSet.SpanCondition spanCondition) {
        int iSpan;
        int iSpanBack;
        int length = charSequence.length();
        if (trimOption != TrimOption.TRAILING) {
            iSpan = this.unicodeSet.span(charSequence, spanCondition);
            if (iSpan == length) {
                return "";
            }
        } else {
            iSpan = 0;
        }
        if (trimOption != TrimOption.LEADING) {
            iSpanBack = this.unicodeSet.spanBack(charSequence, spanCondition);
        } else {
            iSpanBack = length;
        }
        return (iSpan == 0 && iSpanBack == length) ? charSequence : charSequence.subSequence(iSpan, iSpanBack);
    }
}
