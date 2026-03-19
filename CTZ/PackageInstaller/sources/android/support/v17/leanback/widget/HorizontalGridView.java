package android.support.v17.leanback.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.support.v17.leanback.R;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

public class HorizontalGridView extends BaseGridView {
    private boolean mFadingHighEdge;
    private boolean mFadingLowEdge;
    private LinearGradient mHighFadeShader;
    private int mHighFadeShaderLength;
    private int mHighFadeShaderOffset;
    private LinearGradient mLowFadeShader;
    private int mLowFadeShaderLength;
    private int mLowFadeShaderOffset;
    private Bitmap mTempBitmapHigh;
    private Bitmap mTempBitmapLow;
    private Paint mTempPaint;
    private Rect mTempRect;

    public HorizontalGridView(Context context) {
        this(context, null);
    }

    public HorizontalGridView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HorizontalGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mTempPaint = new Paint();
        this.mTempRect = new Rect();
        this.mLayoutManager.setOrientation(0);
        initAttributes(context, attrs);
    }

    protected void initAttributes(Context context, AttributeSet attrs) {
        initBaseGridViewAttributes(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.lbHorizontalGridView);
        setRowHeight(a);
        setNumRows(a.getInt(R.styleable.lbHorizontalGridView_numberOfRows, 1));
        a.recycle();
        updateLayerType();
        this.mTempPaint = new Paint();
        this.mTempPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
    }

    void setRowHeight(TypedArray array) {
        TypedValue typedValue = array.peekValue(R.styleable.lbHorizontalGridView_rowHeight);
        if (typedValue != null) {
            int size = array.getLayoutDimension(R.styleable.lbHorizontalGridView_rowHeight, 0);
            setRowHeight(size);
        }
    }

    public void setNumRows(int numRows) {
        this.mLayoutManager.setNumRows(numRows);
        requestLayout();
    }

    public void setRowHeight(int height) {
        this.mLayoutManager.setRowHeight(height);
        requestLayout();
    }

    private boolean needsFadingLowEdge() {
        if (!this.mFadingLowEdge) {
            return false;
        }
        int c = getChildCount();
        for (int i = 0; i < c; i++) {
            View view = getChildAt(i);
            if (this.mLayoutManager.getOpticalLeft(view) < getPaddingLeft() - this.mLowFadeShaderOffset) {
                return true;
            }
        }
        return false;
    }

    private boolean needsFadingHighEdge() {
        if (!this.mFadingHighEdge) {
            return false;
        }
        int c = getChildCount();
        for (int i = c - 1; i >= 0; i--) {
            View view = getChildAt(i);
            if (this.mLayoutManager.getOpticalRight(view) > (getWidth() - getPaddingRight()) + this.mHighFadeShaderOffset) {
                return true;
            }
        }
        return false;
    }

    private Bitmap getTempBitmapLow() {
        if (this.mTempBitmapLow == null || this.mTempBitmapLow.getWidth() != this.mLowFadeShaderLength || this.mTempBitmapLow.getHeight() != getHeight()) {
            this.mTempBitmapLow = Bitmap.createBitmap(this.mLowFadeShaderLength, getHeight(), Bitmap.Config.ARGB_8888);
        }
        return this.mTempBitmapLow;
    }

    private Bitmap getTempBitmapHigh() {
        if (this.mTempBitmapHigh == null || this.mTempBitmapHigh.getWidth() != this.mHighFadeShaderLength || this.mTempBitmapHigh.getHeight() != getHeight()) {
            this.mTempBitmapHigh = Bitmap.createBitmap(this.mHighFadeShaderLength, getHeight(), Bitmap.Config.ARGB_8888);
        }
        return this.mTempBitmapHigh;
    }

    @Override
    public void draw(Canvas canvas) {
        boolean needsFadingLow = needsFadingLowEdge();
        boolean needsFadingHigh = needsFadingHighEdge();
        if (!needsFadingLow) {
            this.mTempBitmapLow = null;
        }
        if (!needsFadingHigh) {
            this.mTempBitmapHigh = null;
        }
        if (!needsFadingLow && !needsFadingHigh) {
            super.draw(canvas);
            return;
        }
        int lowEdge = this.mFadingLowEdge ? (getPaddingLeft() - this.mLowFadeShaderOffset) - this.mLowFadeShaderLength : 0;
        int highEdge = this.mFadingHighEdge ? (getWidth() - getPaddingRight()) + this.mHighFadeShaderOffset + this.mHighFadeShaderLength : getWidth();
        int save = canvas.save();
        canvas.clipRect((this.mFadingLowEdge ? this.mLowFadeShaderLength : 0) + lowEdge, 0, highEdge - (this.mFadingHighEdge ? this.mHighFadeShaderLength : 0), getHeight());
        super.draw(canvas);
        canvas.restoreToCount(save);
        Canvas tmpCanvas = new Canvas();
        this.mTempRect.top = 0;
        this.mTempRect.bottom = getHeight();
        float f = 0.0f;
        if (needsFadingLow && this.mLowFadeShaderLength > 0) {
            Bitmap tempBitmap = getTempBitmapLow();
            tempBitmap.eraseColor(0);
            tmpCanvas.setBitmap(tempBitmap);
            int tmpSave = tmpCanvas.save();
            tmpCanvas.clipRect(0, 0, this.mLowFadeShaderLength, getHeight());
            tmpCanvas.translate(-lowEdge, 0.0f);
            super.draw(tmpCanvas);
            tmpCanvas.restoreToCount(tmpSave);
            this.mTempPaint.setShader(this.mLowFadeShader);
            tmpCanvas.drawRect(0.0f, 0.0f, this.mLowFadeShaderLength, getHeight(), this.mTempPaint);
            this.mTempRect.left = 0;
            this.mTempRect.right = this.mLowFadeShaderLength;
            f = 0.0f;
            canvas.translate(lowEdge, 0.0f);
            canvas.drawBitmap(tempBitmap, this.mTempRect, this.mTempRect, (Paint) null);
            canvas.translate(-lowEdge, 0.0f);
        }
        if (needsFadingHigh && this.mHighFadeShaderLength > 0) {
            Bitmap tempBitmap2 = getTempBitmapHigh();
            tempBitmap2.eraseColor(0);
            tmpCanvas.setBitmap(tempBitmap2);
            int tmpSave2 = tmpCanvas.save();
            tmpCanvas.clipRect(0, 0, this.mHighFadeShaderLength, getHeight());
            tmpCanvas.translate(-(highEdge - this.mHighFadeShaderLength), f);
            super.draw(tmpCanvas);
            tmpCanvas.restoreToCount(tmpSave2);
            this.mTempPaint.setShader(this.mHighFadeShader);
            tmpCanvas.drawRect(0.0f, 0.0f, this.mHighFadeShaderLength, getHeight(), this.mTempPaint);
            this.mTempRect.left = 0;
            this.mTempRect.right = this.mHighFadeShaderLength;
            canvas.translate(highEdge - this.mHighFadeShaderLength, 0.0f);
            canvas.drawBitmap(tempBitmap2, this.mTempRect, this.mTempRect, (Paint) null);
            canvas.translate(-(highEdge - this.mHighFadeShaderLength), 0.0f);
        }
    }

    private void updateLayerType() {
        if (this.mFadingLowEdge || this.mFadingHighEdge) {
            setLayerType(2, null);
            setWillNotDraw(false);
        } else {
            setLayerType(0, null);
            setWillNotDraw(true);
        }
    }
}
