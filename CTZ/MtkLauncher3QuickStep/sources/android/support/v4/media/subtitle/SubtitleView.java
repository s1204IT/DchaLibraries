package android.support.v4.media.subtitle;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.support.annotation.RequiresApi;
import android.support.mediacompat.R;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

@RequiresApi(28)
class SubtitleView extends View {
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

    SubtitleView(Context context) {
        this(context, null);
    }

    SubtitleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    SubtitleView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    SubtitleView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs);
        this.mLineBounds = new RectF();
        this.mText = new SpannableStringBuilder();
        this.mSpacingMult = 1.0f;
        this.mSpacingAdd = 0.0f;
        this.mInnerPaddingX = 0;
        Resources res = getContext().getResources();
        this.mCornerRadius = res.getDimensionPixelSize(R.dimen.subtitle_corner_radius);
        this.mOutlineWidth = res.getDimensionPixelSize(R.dimen.subtitle_outline_width);
        this.mShadowRadius = res.getDimensionPixelSize(R.dimen.subtitle_shadow_radius);
        this.mShadowOffsetX = res.getDimensionPixelSize(R.dimen.subtitle_shadow_offset);
        this.mShadowOffsetY = this.mShadowOffsetX;
        this.mTextPaint = new TextPaint();
        this.mTextPaint.setAntiAlias(true);
        this.mTextPaint.setSubpixelText(true);
        this.mPaint = new Paint();
        this.mPaint.setAntiAlias(true);
    }

    public void setText(int resId) {
        CharSequence text = getContext().getText(resId);
        setText(text);
    }

    public void setText(CharSequence text) {
        this.mText.clear();
        this.mText.append(text);
        this.mHasMeasurements = false;
        requestLayout();
        invalidate();
    }

    public void setForegroundColor(int color) {
        this.mForegroundColor = color;
        invalidate();
    }

    @Override
    public void setBackgroundColor(int color) {
        this.mBackgroundColor = color;
        invalidate();
    }

    public void setEdgeType(int edgeType) {
        this.mEdgeType = edgeType;
        invalidate();
    }

    public void setEdgeColor(int color) {
        this.mEdgeColor = color;
        invalidate();
    }

    public void setTextSize(float size) {
        if (this.mTextPaint.getTextSize() != size) {
            this.mTextPaint.setTextSize(size);
            this.mInnerPaddingX = (int) ((INNER_PADDING_RATIO * size) + 0.5f);
            this.mHasMeasurements = false;
            requestLayout();
            invalidate();
        }
    }

    public void setTypeface(Typeface typeface) {
        if (typeface != null && !typeface.equals(this.mTextPaint.getTypeface())) {
            this.mTextPaint.setTypeface(typeface);
            this.mHasMeasurements = false;
            requestLayout();
            invalidate();
        }
    }

    public void setAlignment(Layout.Alignment textAlignment) {
        if (this.mAlignment != textAlignment) {
            this.mAlignment = textAlignment;
            this.mHasMeasurements = false;
            requestLayout();
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpec = View.MeasureSpec.getSize(widthMeasureSpec);
        if (computeMeasurements(widthSpec)) {
            StaticLayout layout = this.mLayout;
            int paddingX = getPaddingLeft() + getPaddingRight() + (this.mInnerPaddingX * 2);
            int width = layout.getWidth() + paddingX;
            int height = layout.getHeight() + getPaddingTop() + getPaddingBottom();
            setMeasuredDimension(width, height);
            return;
        }
        setMeasuredDimension(16777216, 16777216);
    }

    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = r - l;
        computeMeasurements(width);
    }

    private boolean computeMeasurements(int maxWidth) {
        if (this.mHasMeasurements && maxWidth == this.mLastMeasuredWidth) {
            return true;
        }
        int paddingX = getPaddingLeft() + getPaddingRight() + (this.mInnerPaddingX * 2);
        int maxWidth2 = maxWidth - paddingX;
        if (maxWidth2 <= 0) {
            return false;
        }
        this.mHasMeasurements = true;
        this.mLastMeasuredWidth = maxWidth2;
        this.mLayout = StaticLayout.Builder.obtain(this.mText, 0, this.mText.length(), this.mTextPaint, maxWidth2).setAlignment(this.mAlignment).setLineSpacing(this.mSpacingAdd, this.mSpacingMult).setUseLineSpacingFromFallbacks(true).build();
        return true;
    }

    @Override
    protected void onDraw(Canvas c) {
        StaticLayout layout = this.mLayout;
        if (layout == null) {
            return;
        }
        int saveCount = c.save();
        int innerPaddingX = this.mInnerPaddingX;
        c.translate(getPaddingLeft() + innerPaddingX, getPaddingTop());
        int lineCount = layout.getLineCount();
        Paint textPaint = this.mTextPaint;
        Paint paint = this.mPaint;
        RectF bounds = this.mLineBounds;
        if (Color.alpha(this.mBackgroundColor) > 0) {
            float cornerRadius = this.mCornerRadius;
            float previousBottom = layout.getLineTop(0);
            paint.setColor(this.mBackgroundColor);
            paint.setStyle(Paint.Style.FILL);
            float previousBottom2 = previousBottom;
            for (int i = 0; i < lineCount; i++) {
                bounds.left = layout.getLineLeft(i) - innerPaddingX;
                bounds.right = layout.getLineRight(i) + innerPaddingX;
                bounds.top = previousBottom2;
                bounds.bottom = layout.getLineBottom(i);
                previousBottom2 = bounds.bottom;
                c.drawRoundRect(bounds, cornerRadius, cornerRadius, paint);
            }
        }
        int edgeType = this.mEdgeType;
        if (edgeType == 1) {
            textPaint.setStrokeJoin(Paint.Join.ROUND);
            textPaint.setStrokeWidth(this.mOutlineWidth);
            textPaint.setColor(this.mEdgeColor);
            textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            layout.draw(c);
        } else if (edgeType == 2) {
            textPaint.setShadowLayer(this.mShadowRadius, this.mShadowOffsetX, this.mShadowOffsetY, this.mEdgeColor);
        } else {
            if (edgeType == 3 || edgeType == 4) {
                boolean raised = edgeType == 3;
                int colorUp = raised ? -1 : this.mEdgeColor;
                int colorDown = raised ? this.mEdgeColor : -1;
                float offset = this.mShadowRadius / 2.0f;
                textPaint.setColor(this.mForegroundColor);
                textPaint.setStyle(Paint.Style.FILL);
                textPaint.setShadowLayer(this.mShadowRadius, -offset, -offset, colorUp);
                layout.draw(c);
                textPaint.setShadowLayer(this.mShadowRadius, offset, offset, colorDown);
            }
            textPaint.setColor(this.mForegroundColor);
            textPaint.setStyle(Paint.Style.FILL);
            layout.draw(c);
            textPaint.setShadowLayer(0.0f, 0.0f, 0.0f, 0);
            c.restoreToCount(saveCount);
        }
        textPaint.setColor(this.mForegroundColor);
        textPaint.setStyle(Paint.Style.FILL);
        layout.draw(c);
        textPaint.setShadowLayer(0.0f, 0.0f, 0.0f, 0);
        c.restoreToCount(saveCount);
    }
}
