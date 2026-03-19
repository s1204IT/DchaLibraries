package android.text;

import android.graphics.Rect;
import android.text.style.MetricAffectingSpan;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.Objects;

public class PrecomputedText implements Spannable {
    private static final char LINE_FEED = '\n';
    private final int mEnd;
    private final ParagraphInfo[] mParagraphInfo;
    private final Params mParams;
    private final int mStart;
    private final SpannableString mText;

    public static final class Params {
        private final int mBreakStrategy;
        private final int mHyphenationFrequency;
        private final TextPaint mPaint;
        private final TextDirectionHeuristic mTextDir;

        public static class Builder {
            private final TextPaint mPaint;
            private TextDirectionHeuristic mTextDir = TextDirectionHeuristics.FIRSTSTRONG_LTR;
            private int mBreakStrategy = 1;
            private int mHyphenationFrequency = 1;

            public Builder(TextPaint textPaint) {
                this.mPaint = textPaint;
            }

            public Builder setBreakStrategy(int i) {
                this.mBreakStrategy = i;
                return this;
            }

            public Builder setHyphenationFrequency(int i) {
                this.mHyphenationFrequency = i;
                return this;
            }

            public Builder setTextDirection(TextDirectionHeuristic textDirectionHeuristic) {
                this.mTextDir = textDirectionHeuristic;
                return this;
            }

            public Params build() {
                return new Params(this.mPaint, this.mTextDir, this.mBreakStrategy, this.mHyphenationFrequency);
            }
        }

        public Params(TextPaint textPaint, TextDirectionHeuristic textDirectionHeuristic, int i, int i2) {
            this.mPaint = textPaint;
            this.mTextDir = textDirectionHeuristic;
            this.mBreakStrategy = i;
            this.mHyphenationFrequency = i2;
        }

        public TextPaint getTextPaint() {
            return this.mPaint;
        }

        public TextDirectionHeuristic getTextDirection() {
            return this.mTextDir;
        }

        public int getBreakStrategy() {
            return this.mBreakStrategy;
        }

        public int getHyphenationFrequency() {
            return this.mHyphenationFrequency;
        }

        public boolean isSameTextMetricsInternal(TextPaint textPaint, TextDirectionHeuristic textDirectionHeuristic, int i, int i2) {
            return this.mTextDir == textDirectionHeuristic && this.mBreakStrategy == i && this.mHyphenationFrequency == i2 && this.mPaint.equalsForTextMeasurement(textPaint);
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || !(obj instanceof Params)) {
                return false;
            }
            Params params = (Params) obj;
            return isSameTextMetricsInternal(params.mPaint, params.mTextDir, params.mBreakStrategy, params.mHyphenationFrequency);
        }

        public int hashCode() {
            return Objects.hash(Float.valueOf(this.mPaint.getTextSize()), Float.valueOf(this.mPaint.getTextScaleX()), Float.valueOf(this.mPaint.getTextSkewX()), Float.valueOf(this.mPaint.getLetterSpacing()), Float.valueOf(this.mPaint.getWordSpacing()), Integer.valueOf(this.mPaint.getFlags()), this.mPaint.getTextLocales(), this.mPaint.getTypeface(), this.mPaint.getFontVariationSettings(), Boolean.valueOf(this.mPaint.isElegantTextHeight()), this.mTextDir, Integer.valueOf(this.mBreakStrategy), Integer.valueOf(this.mHyphenationFrequency));
        }

        public String toString() {
            return "{textSize=" + this.mPaint.getTextSize() + ", textScaleX=" + this.mPaint.getTextScaleX() + ", textSkewX=" + this.mPaint.getTextSkewX() + ", letterSpacing=" + this.mPaint.getLetterSpacing() + ", textLocale=" + this.mPaint.getTextLocales() + ", typeface=" + this.mPaint.getTypeface() + ", variationSettings=" + this.mPaint.getFontVariationSettings() + ", elegantTextHeight=" + this.mPaint.isElegantTextHeight() + ", textDir=" + this.mTextDir + ", breakStrategy=" + this.mBreakStrategy + ", hyphenationFrequency=" + this.mHyphenationFrequency + "}";
        }
    }

    public static class ParagraphInfo {
        public final MeasuredParagraph measured;
        public final int paragraphEnd;

        public ParagraphInfo(int i, MeasuredParagraph measuredParagraph) {
            this.paragraphEnd = i;
            this.measured = measuredParagraph;
        }
    }

    public static PrecomputedText create(CharSequence charSequence, Params params) {
        return new PrecomputedText(charSequence, 0, charSequence.length(), params, createMeasuredParagraphs(charSequence, params, 0, charSequence.length(), true));
    }

    public static ParagraphInfo[] createMeasuredParagraphs(CharSequence charSequence, Params params, int i, int i2, boolean z) {
        boolean z2;
        ArrayList arrayList = new ArrayList();
        Preconditions.checkNotNull(charSequence);
        Preconditions.checkNotNull(params);
        if (params.getBreakStrategy() == 0 || params.getHyphenationFrequency() == 0) {
            z2 = false;
        } else {
            z2 = true;
        }
        while (true) {
            int i3 = i;
            if (i3 < i2) {
                int iIndexOf = TextUtils.indexOf(charSequence, LINE_FEED, i3, i2);
                if (iIndexOf >= 0) {
                    i = iIndexOf + 1;
                } else {
                    i = i2;
                }
                arrayList.add(new ParagraphInfo(i, MeasuredParagraph.buildForStaticLayout(params.getTextPaint(), charSequence, i3, i, params.getTextDirection(), z2, z, null)));
            } else {
                return (ParagraphInfo[]) arrayList.toArray(new ParagraphInfo[arrayList.size()]);
            }
        }
    }

    private PrecomputedText(CharSequence charSequence, int i, int i2, Params params, ParagraphInfo[] paragraphInfoArr) {
        this.mText = new SpannableString(charSequence, true);
        this.mStart = i;
        this.mEnd = i2;
        this.mParams = params;
        this.mParagraphInfo = paragraphInfoArr;
    }

    public CharSequence getText() {
        return this.mText;
    }

    public int getStart() {
        return this.mStart;
    }

    public int getEnd() {
        return this.mEnd;
    }

    public Params getParams() {
        return this.mParams;
    }

    public int getParagraphCount() {
        return this.mParagraphInfo.length;
    }

    public int getParagraphStart(int i) {
        Preconditions.checkArgumentInRange(i, 0, getParagraphCount(), "paraIndex");
        return i == 0 ? this.mStart : getParagraphEnd(i - 1);
    }

    public int getParagraphEnd(int i) {
        Preconditions.checkArgumentInRange(i, 0, getParagraphCount(), "paraIndex");
        return this.mParagraphInfo[i].paragraphEnd;
    }

    public MeasuredParagraph getMeasuredParagraph(int i) {
        return this.mParagraphInfo[i].measured;
    }

    public ParagraphInfo[] getParagraphInfo() {
        return this.mParagraphInfo;
    }

    public boolean canUseMeasuredResult(int i, int i2, TextDirectionHeuristic textDirectionHeuristic, TextPaint textPaint, int i3, int i4) {
        this.mParams.getTextPaint();
        return this.mStart == i && this.mEnd == i2 && this.mParams.isSameTextMetricsInternal(textPaint, textDirectionHeuristic, i3, i4);
    }

    public int findParaIndex(int i) {
        for (int i2 = 0; i2 < this.mParagraphInfo.length; i2++) {
            if (i < this.mParagraphInfo[i2].paragraphEnd) {
                return i2;
            }
        }
        throw new IndexOutOfBoundsException("pos must be less than " + this.mParagraphInfo[this.mParagraphInfo.length - 1].paragraphEnd + ", gave " + i);
    }

    public float getWidth(int i, int i2) {
        Preconditions.checkArgument(i >= 0 && i <= this.mText.length(), "invalid start offset");
        Preconditions.checkArgument(i2 >= 0 && i2 <= this.mText.length(), "invalid end offset");
        Preconditions.checkArgument(i <= i2, "start offset can not be larger than end offset");
        if (i == i2) {
            return 0.0f;
        }
        int iFindParaIndex = findParaIndex(i);
        int paragraphStart = getParagraphStart(iFindParaIndex);
        int paragraphEnd = getParagraphEnd(iFindParaIndex);
        if (i < paragraphStart || paragraphEnd < i2) {
            throw new IllegalArgumentException("Cannot measured across the paragraph:para: (" + paragraphStart + ", " + paragraphEnd + "), request: (" + i + ", " + i2 + ")");
        }
        return getMeasuredParagraph(iFindParaIndex).getWidth(i - paragraphStart, i2 - paragraphStart);
    }

    public void getBounds(int i, int i2, Rect rect) {
        Preconditions.checkArgument(i >= 0 && i <= this.mText.length(), "invalid start offset");
        Preconditions.checkArgument(i2 >= 0 && i2 <= this.mText.length(), "invalid end offset");
        Preconditions.checkArgument(i <= i2, "start offset can not be larger than end offset");
        Preconditions.checkNotNull(rect);
        if (i == i2) {
            rect.set(0, 0, 0, 0);
            return;
        }
        int iFindParaIndex = findParaIndex(i);
        int paragraphStart = getParagraphStart(iFindParaIndex);
        int paragraphEnd = getParagraphEnd(iFindParaIndex);
        if (i < paragraphStart || paragraphEnd < i2) {
            throw new IllegalArgumentException("Cannot measured across the paragraph:para: (" + paragraphStart + ", " + paragraphEnd + "), request: (" + i + ", " + i2 + ")");
        }
        getMeasuredParagraph(iFindParaIndex).getBounds(i - paragraphStart, i2 - paragraphStart, rect);
    }

    public int getMemoryUsage() {
        int memoryUsage = 0;
        for (int i = 0; i < getParagraphCount(); i++) {
            memoryUsage += getMeasuredParagraph(i).getMemoryUsage();
        }
        return memoryUsage;
    }

    @Override
    public void setSpan(Object obj, int i, int i2, int i3) {
        if (obj instanceof MetricAffectingSpan) {
            throw new IllegalArgumentException("MetricAffectingSpan can not be set to PrecomputedText.");
        }
        this.mText.setSpan(obj, i, i2, i3);
    }

    @Override
    public void removeSpan(Object obj) {
        if (obj instanceof MetricAffectingSpan) {
            throw new IllegalArgumentException("MetricAffectingSpan can not be removed from PrecomputedText.");
        }
        this.mText.removeSpan(obj);
    }

    @Override
    public <T> T[] getSpans(int i, int i2, Class<T> cls) {
        return (T[]) this.mText.getSpans(i, i2, cls);
    }

    @Override
    public int getSpanStart(Object obj) {
        return this.mText.getSpanStart(obj);
    }

    @Override
    public int getSpanEnd(Object obj) {
        return this.mText.getSpanEnd(obj);
    }

    @Override
    public int getSpanFlags(Object obj) {
        return this.mText.getSpanFlags(obj);
    }

    @Override
    public int nextSpanTransition(int i, int i2, Class cls) {
        return this.mText.nextSpanTransition(i, i2, cls);
    }

    @Override
    public int length() {
        return this.mText.length();
    }

    @Override
    public char charAt(int i) {
        return this.mText.charAt(i);
    }

    @Override
    public CharSequence subSequence(int i, int i2) {
        return create(this.mText.subSequence(i, i2), this.mParams);
    }

    @Override
    public String toString() {
        return this.mText.toString();
    }
}
