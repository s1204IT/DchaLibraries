package com.android.internal.widget;

import android.content.Context;
import android.text.BoringLayout;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextUtils;
import android.text.method.TransformationMethod;
import android.util.AttributeSet;
import android.view.RemotableViewMethod;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TextView;

@RemoteViews.RemoteView
public class ImageFloatingTextView extends TextView {
    private int mImageEndMargin;
    private int mIndentLines;
    private int mLayoutMaxLines;
    private int mMaxLinesForHeight;
    private int mResolvedDirection;

    public ImageFloatingTextView(Context context) {
        this(context, null);
    }

    public ImageFloatingTextView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ImageFloatingTextView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public ImageFloatingTextView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mResolvedDirection = -1;
        this.mMaxLinesForHeight = -1;
        this.mLayoutMaxLines = -1;
    }

    @Override
    protected Layout makeSingleLayout(int i, BoringLayout.Metrics metrics, int i2, Layout.Alignment alignment, boolean z, TextUtils.TruncateAt truncateAt, boolean z2) {
        int maxLines;
        int[] iArr;
        TransformationMethod transformationMethod = getTransformationMethod();
        CharSequence text = getText();
        if (transformationMethod != null) {
            text = transformationMethod.getTransformation(text, this);
        }
        if (text == null) {
            text = "";
        }
        StaticLayout.Builder hyphenationFrequency = StaticLayout.Builder.obtain(text, 0, text.length(), getPaint(), i).setAlignment(alignment).setTextDirection(getTextDirectionHeuristic()).setLineSpacing(getLineSpacingExtra(), getLineSpacingMultiplier()).setIncludePad(getIncludeFontPadding()).setUseLineSpacingFromFallbacks(true).setBreakStrategy(1).setHyphenationFrequency(2);
        if (this.mMaxLinesForHeight > 0) {
            maxLines = this.mMaxLinesForHeight;
        } else {
            maxLines = getMaxLines() >= 0 ? getMaxLines() : Integer.MAX_VALUE;
        }
        hyphenationFrequency.setMaxLines(maxLines);
        this.mLayoutMaxLines = maxLines;
        if (z) {
            hyphenationFrequency.setEllipsize(truncateAt).setEllipsizedWidth(i2);
        }
        if (this.mIndentLines > 0) {
            iArr = new int[this.mIndentLines + 1];
            for (int i3 = 0; i3 < this.mIndentLines; i3++) {
                iArr[i3] = this.mImageEndMargin;
            }
        } else {
            iArr = null;
        }
        if (this.mResolvedDirection == 1) {
            hyphenationFrequency.setIndents(iArr, null);
        } else {
            hyphenationFrequency.setIndents(null, iArr);
        }
        return hyphenationFrequency.build();
    }

    @RemotableViewMethod
    public void setImageEndMargin(int i) {
        this.mImageEndMargin = i;
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int size = (View.MeasureSpec.getSize(i2) - this.mPaddingTop) - this.mPaddingBottom;
        if (getLayout() != null && getLayout().getHeight() != size) {
            this.mMaxLinesForHeight = -1;
            nullLayouts();
        }
        super.onMeasure(i, i2);
        Layout layout = getLayout();
        if (layout.getHeight() > size) {
            int lineCount = layout.getLineCount() - 1;
            while (lineCount > 1 && layout.getLineBottom(lineCount - 1) > size) {
                lineCount--;
            }
            if (getMaxLines() > 0) {
                lineCount = Math.min(getMaxLines(), lineCount);
            }
            if (lineCount != this.mLayoutMaxLines) {
                this.mMaxLinesForHeight = lineCount;
                nullLayouts();
                super.onMeasure(i, i2);
            }
        }
    }

    @Override
    public void onRtlPropertiesChanged(int i) {
        super.onRtlPropertiesChanged(i);
        if (i != this.mResolvedDirection && isLayoutDirectionResolved()) {
            this.mResolvedDirection = i;
            if (this.mIndentLines > 0) {
                nullLayouts();
                requestLayout();
            }
        }
    }

    @RemotableViewMethod
    public void setHasImage(boolean z) {
        setNumIndentLines(z ? 2 : 0);
    }

    public boolean setNumIndentLines(int i) {
        if (this.mIndentLines != i) {
            this.mIndentLines = i;
            nullLayouts();
            requestLayout();
            return true;
        }
        return false;
    }
}
