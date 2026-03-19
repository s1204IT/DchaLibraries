package com.android.internal.widget;

import android.R;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.CaptioningManager;

public class SubtitleView extends View {
    private static final int COLOR_BEVEL_DARK = Integer.MIN_VALUE;
    private static final int COLOR_BEVEL_LIGHT = -2130706433;
    private static final float INNER_PADDING_RATIO = 0.125f;
    private Layout.Alignment mAlignment;
    private int mBackgroundColor;
    private final float mCornerRadius;
    private int mEdgeColor;
    private int mEdgeType;
    private int mForegroundColor;
    private boolean mHasMeasurements;
    private int mInnerPaddingX;
    private int mLastMeasuredWidth;
    private StaticLayout mLayout;
    private final RectF mLineBounds;
    private final float mOutlineWidth;
    private Paint mPaint;
    private final float mShadowOffsetX;
    private final float mShadowOffsetY;
    private final float mShadowRadius;
    private float mSpacingAdd;
    private float mSpacingMult;
    private final SpannableStringBuilder mText;
    private TextPaint mTextPaint;

    public SubtitleView(Context context) {
        this(context, null);
    }

    public SubtitleView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public SubtitleView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public SubtitleView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet);
        this.mLineBounds = new RectF();
        this.mText = new SpannableStringBuilder();
        this.mSpacingMult = 1.0f;
        this.mSpacingAdd = 0.0f;
        this.mInnerPaddingX = 0;
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.TextView, i, i2);
        CharSequence text = "";
        int indexCount = typedArrayObtainStyledAttributes.getIndexCount();
        int dimensionPixelSize = 15;
        for (int i3 = 0; i3 < indexCount; i3++) {
            int index = typedArrayObtainStyledAttributes.getIndex(i3);
            if (index != 0) {
                if (index == 18) {
                    text = typedArrayObtainStyledAttributes.getText(index);
                } else {
                    switch (index) {
                        case 53:
                            this.mSpacingAdd = typedArrayObtainStyledAttributes.getDimensionPixelSize(index, (int) this.mSpacingAdd);
                            break;
                        case 54:
                            this.mSpacingMult = typedArrayObtainStyledAttributes.getFloat(index, this.mSpacingMult);
                            break;
                    }
                }
            } else {
                dimensionPixelSize = typedArrayObtainStyledAttributes.getDimensionPixelSize(index, dimensionPixelSize);
            }
        }
        Resources resources = getContext().getResources();
        this.mCornerRadius = resources.getDimensionPixelSize(com.android.internal.R.dimen.subtitle_corner_radius);
        this.mOutlineWidth = resources.getDimensionPixelSize(com.android.internal.R.dimen.subtitle_outline_width);
        this.mShadowRadius = resources.getDimensionPixelSize(com.android.internal.R.dimen.subtitle_shadow_radius);
        this.mShadowOffsetX = resources.getDimensionPixelSize(com.android.internal.R.dimen.subtitle_shadow_offset);
        this.mShadowOffsetY = this.mShadowOffsetX;
        this.mTextPaint = new TextPaint();
        this.mTextPaint.setAntiAlias(true);
        this.mTextPaint.setSubpixelText(true);
        this.mPaint = new Paint();
        this.mPaint.setAntiAlias(true);
        setText(text);
        setTextSize(dimensionPixelSize);
    }

    public void setText(int i) {
        setText(getContext().getText(i));
    }

    public void setText(CharSequence charSequence) {
        this.mText.clear();
        this.mText.append(charSequence);
        this.mHasMeasurements = false;
        requestLayout();
        invalidate();
    }

    public void setForegroundColor(int i) {
        this.mForegroundColor = i;
        invalidate();
    }

    @Override
    public void setBackgroundColor(int i) {
        this.mBackgroundColor = i;
        invalidate();
    }

    public void setEdgeType(int i) {
        this.mEdgeType = i;
        invalidate();
    }

    public void setEdgeColor(int i) {
        this.mEdgeColor = i;
        invalidate();
    }

    public void setTextSize(float f) {
        if (this.mTextPaint.getTextSize() != f) {
            this.mTextPaint.setTextSize(f);
            this.mInnerPaddingX = (int) ((f * INNER_PADDING_RATIO) + 0.5f);
            this.mHasMeasurements = false;
            requestLayout();
            invalidate();
        }
    }

    public void setTypeface(Typeface typeface) {
        if (this.mTextPaint.getTypeface() != typeface) {
            this.mTextPaint.setTypeface(typeface);
            this.mHasMeasurements = false;
            requestLayout();
            invalidate();
        }
    }

    public void setAlignment(Layout.Alignment alignment) {
        if (this.mAlignment != alignment) {
            this.mAlignment = alignment;
            this.mHasMeasurements = false;
            requestLayout();
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int i, int i2) {
        if (computeMeasurements(View.MeasureSpec.getSize(i))) {
            StaticLayout staticLayout = this.mLayout;
            setMeasuredDimension(staticLayout.getWidth() + this.mPaddingLeft + this.mPaddingRight + (this.mInnerPaddingX * 2), staticLayout.getHeight() + this.mPaddingTop + this.mPaddingBottom);
            return;
        }
        setMeasuredDimension(16777216, 16777216);
    }

    @Override
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        computeMeasurements(i3 - i);
    }

    private boolean computeMeasurements(int i) {
        if (this.mHasMeasurements && i == this.mLastMeasuredWidth) {
            return true;
        }
        int i2 = i - ((this.mPaddingLeft + this.mPaddingRight) + (this.mInnerPaddingX * 2));
        if (i2 <= 0) {
            return false;
        }
        this.mHasMeasurements = true;
        this.mLastMeasuredWidth = i2;
        this.mLayout = StaticLayout.Builder.obtain(this.mText, 0, this.mText.length(), this.mTextPaint, i2).setAlignment(this.mAlignment).setLineSpacing(this.mSpacingAdd, this.mSpacingMult).setUseLineSpacingFromFallbacks(true).build();
        return true;
    }

    public void setStyle(int i) {
        CaptioningManager.CaptionStyle customStyle;
        ContentResolver contentResolver = this.mContext.getContentResolver();
        if (i == -1) {
            customStyle = CaptioningManager.CaptionStyle.getCustomStyle(contentResolver);
        } else {
            customStyle = CaptioningManager.CaptionStyle.PRESETS[i];
        }
        CaptioningManager.CaptionStyle captionStyle = CaptioningManager.CaptionStyle.DEFAULT;
        this.mForegroundColor = customStyle.hasForegroundColor() ? customStyle.foregroundColor : captionStyle.foregroundColor;
        this.mBackgroundColor = customStyle.hasBackgroundColor() ? customStyle.backgroundColor : captionStyle.backgroundColor;
        this.mEdgeType = customStyle.hasEdgeType() ? customStyle.edgeType : captionStyle.edgeType;
        this.mEdgeColor = customStyle.hasEdgeColor() ? customStyle.edgeColor : captionStyle.edgeColor;
        this.mHasMeasurements = false;
        setTypeface(customStyle.getTypeface());
        requestLayout();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int i;
        StaticLayout staticLayout = this.mLayout;
        if (staticLayout == null) {
            return;
        }
        int iSave = canvas.save();
        int i2 = this.mInnerPaddingX;
        canvas.translate(this.mPaddingLeft + i2, this.mPaddingTop);
        int lineCount = staticLayout.getLineCount();
        TextPaint textPaint = this.mTextPaint;
        Paint paint = this.mPaint;
        RectF rectF = this.mLineBounds;
        if (Color.alpha(this.mBackgroundColor) > 0) {
            float f = this.mCornerRadius;
            float lineTop = staticLayout.getLineTop(0);
            paint.setColor(this.mBackgroundColor);
            paint.setStyle(Paint.Style.FILL);
            float f2 = lineTop;
            for (int i3 = 0; i3 < lineCount; i3++) {
                float f3 = i2;
                rectF.left = staticLayout.getLineLeft(i3) - f3;
                rectF.right = staticLayout.getLineRight(i3) + f3;
                rectF.top = f2;
                rectF.bottom = staticLayout.getLineBottom(i3);
                f2 = rectF.bottom;
                canvas.drawRoundRect(rectF, f, f, paint);
            }
        }
        int i4 = this.mEdgeType;
        boolean z = true;
        if (i4 == 1) {
            textPaint.setStrokeJoin(Paint.Join.ROUND);
            textPaint.setStrokeWidth(this.mOutlineWidth);
            textPaint.setColor(this.mEdgeColor);
            textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            for (int i5 = 0; i5 < lineCount; i5++) {
                staticLayout.drawText(canvas, i5, i5);
            }
        } else if (i4 == 2) {
            textPaint.setShadowLayer(this.mShadowRadius, this.mShadowOffsetX, this.mShadowOffsetY, this.mEdgeColor);
        } else if (i4 == 3 || i4 == 4) {
            if (i4 != 3) {
                z = false;
            }
            if (!z) {
                i = this.mEdgeColor;
            } else {
                i = -1;
            }
            int i6 = z ? this.mEdgeColor : -1;
            float f4 = this.mShadowRadius / 2.0f;
            textPaint.setColor(this.mForegroundColor);
            textPaint.setStyle(Paint.Style.FILL);
            float f5 = -f4;
            textPaint.setShadowLayer(this.mShadowRadius, f5, f5, i);
            for (int i7 = 0; i7 < lineCount; i7++) {
                staticLayout.drawText(canvas, i7, i7);
            }
            textPaint.setShadowLayer(this.mShadowRadius, f4, f4, i6);
        }
        textPaint.setColor(this.mForegroundColor);
        textPaint.setStyle(Paint.Style.FILL);
        for (int i8 = 0; i8 < lineCount; i8++) {
            staticLayout.drawText(canvas, i8, i8);
        }
        textPaint.setShadowLayer(0.0f, 0.0f, 0.0f, 0);
        canvas.restoreToCount(iSave);
    }
}
