package com.android.deskclock.widget;

import android.text.Layout;
import android.text.TextPaint;
import android.view.View;
import android.widget.TextView;

public final class TextSizeHelper {
    private boolean mIgnoreRequestLayout;
    private float mMaxTextSize;
    private final TextView mTextView;
    private final TextPaint mMeasurePaint = new TextPaint();
    private int mWidthConstraint = Integer.MAX_VALUE;
    private int mHeightConstraint = Integer.MAX_VALUE;

    public TextSizeHelper(TextView textView) {
        this.mTextView = textView;
        this.mMaxTextSize = textView.getTextSize();
    }

    public void onMeasure(int i, int i2) {
        int size;
        if (View.MeasureSpec.getMode(i) != 0) {
            size = (View.MeasureSpec.getSize(i) - this.mTextView.getCompoundPaddingLeft()) - this.mTextView.getCompoundPaddingRight();
        } else {
            size = Integer.MAX_VALUE;
        }
        int size2 = View.MeasureSpec.getMode(i2) != 0 ? (View.MeasureSpec.getSize(i2) - this.mTextView.getCompoundPaddingTop()) - this.mTextView.getCompoundPaddingBottom() : Integer.MAX_VALUE;
        if (this.mTextView.isLayoutRequested() || this.mWidthConstraint != size || this.mHeightConstraint != size2) {
            this.mWidthConstraint = size;
            this.mHeightConstraint = size2;
            adjustTextSize();
        }
    }

    public void onTextChanged(int i, int i2) {
        if (i != i2) {
            this.mTextView.requestLayout();
        }
    }

    public boolean shouldIgnoreRequestLayout() {
        return this.mIgnoreRequestLayout;
    }

    private void adjustTextSize() {
        CharSequence text = this.mTextView.getText();
        float f = this.mMaxTextSize;
        if (text.length() > 0 && (this.mWidthConstraint < Integer.MAX_VALUE || this.mHeightConstraint < Integer.MAX_VALUE)) {
            this.mMeasurePaint.set(this.mTextView.getPaint());
            float f2 = this.mMaxTextSize;
            float f3 = f;
            float f4 = 1.0f;
            while (f2 >= f4) {
                float fRound = Math.round((f2 + f4) / 2.0f);
                this.mMeasurePaint.setTextSize(fRound);
                float desiredWidth = Layout.getDesiredWidth(text, this.mMeasurePaint);
                float fontMetricsInt = this.mMeasurePaint.getFontMetricsInt(null);
                if (desiredWidth > this.mWidthConstraint || fontMetricsInt > this.mHeightConstraint) {
                    f2 = fRound - 1.0f;
                } else {
                    f4 = fRound + 1.0f;
                    f3 = fRound;
                }
            }
            f = f3;
        }
        if (this.mTextView.getTextSize() != f) {
            this.mIgnoreRequestLayout = true;
            this.mTextView.setTextSize(0, f);
            this.mIgnoreRequestLayout = false;
        }
    }
}
