package jp.co.omronsoft.iwnnime.ml;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

public class ComposingExtraView extends View {
    private Drawable mBackGroundDrawable;
    private String mDisplayText;
    private Paint mDrawPaint;
    private Paint.FontMetricsInt mFontMetricsInt;
    private int mSideMargin;
    private int mViewPaddingBottom;
    private int mViewPaddingLeft;
    private int mViewPaddingRight;
    private int mViewPaddingTop;

    public ComposingExtraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mDrawPaint = null;
        this.mBackGroundDrawable = null;
        this.mFontMetricsInt = null;
        this.mSideMargin = 0;
        this.mViewPaddingTop = getPaddingTop();
        this.mViewPaddingBottom = getPaddingBottom();
        this.mViewPaddingLeft = getPaddingLeft();
        this.mViewPaddingRight = getPaddingRight();
        this.mDisplayText = null;
        Resources res = context.getResources();
        this.mBackGroundDrawable = res.getDrawable(R.drawable.composing_extra_view_bg);
        this.mSideMargin = res.getInteger(R.integer.composing_extra_view_side_margin_size);
        this.mDrawPaint = new Paint();
        this.mDrawPaint.setColor(res.getColor(R.color.composing_extra_view_text_color));
        this.mDrawPaint.setAntiAlias(true);
        this.mDrawPaint.setTextSize(res.getDimensionPixelSize(R.dimen.composing_extra_view_text_size));
        this.mFontMetricsInt = this.mDrawPaint.getFontMetricsInt();
    }

    public void setComposingExtraText(String text) {
        this.mDisplayText = text;
        measure(-2, -2);
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float measureWidth;
        int measureHeight = (this.mFontMetricsInt.bottom - this.mFontMetricsInt.top) + this.mViewPaddingTop + this.mViewPaddingBottom;
        if (this.mDisplayText == null || this.mDisplayText.length() == 0) {
            measureWidth = 0.0f;
        } else {
            float measureWidth2 = this.mViewPaddingLeft + this.mViewPaddingRight + (this.mSideMargin * 2);
            measureWidth = measureWidth2 + this.mDrawPaint.measureText(this.mDisplayText, 0, this.mDisplayText.length());
        }
        setMeasuredDimension((int) (0.5f + measureWidth), measureHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (this.mDisplayText != null) {
            float pointX = this.mViewPaddingLeft + this.mSideMargin;
            float pointY = (-this.mFontMetricsInt.top) + this.mViewPaddingTop;
            this.mBackGroundDrawable.setBounds(this.mViewPaddingLeft, this.mViewPaddingTop, getWidth() - this.mViewPaddingRight, getHeight() - this.mViewPaddingBottom);
            this.mBackGroundDrawable.draw(canvas);
            canvas.drawText(this.mDisplayText, 0, this.mDisplayText.length(), pointX, pointY, this.mDrawPaint);
        }
    }
}
