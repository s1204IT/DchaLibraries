package com.android.gallery3d.filtershow.category;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import com.android.gallery3d.R;

public class IconView extends View {
    private int mBackgroundColor;
    private Bitmap mBitmap;
    private Rect mBitmapBounds;
    private int mMargin;
    private int mOrientation;
    private Paint mPaint;
    private String mText;
    private Rect mTextBounds;
    private int mTextColor;
    private int mTextSize;
    private boolean mUseOnlyDrawable;

    public IconView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mPaint = new Paint();
        this.mMargin = 16;
        this.mOrientation = 1;
        this.mTextSize = 32;
        this.mTextBounds = new Rect();
        this.mUseOnlyDrawable = false;
        setup(context);
        setBitmap(BitmapFactory.decodeStream(context.getResources().openRawResource(attributeSet.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "src", 0))));
        setUseOnlyDrawable(true);
    }

    public IconView(Context context) {
        super(context);
        this.mPaint = new Paint();
        this.mMargin = 16;
        this.mOrientation = 1;
        this.mTextSize = 32;
        this.mTextBounds = new Rect();
        this.mUseOnlyDrawable = false;
        setup(context);
    }

    private void setup(Context context) {
        Resources resources = getResources();
        this.mTextColor = resources.getColor(R.color.filtershow_categoryview_text);
        this.mBackgroundColor = resources.getColor(R.color.filtershow_categoryview_background);
        this.mMargin = resources.getDimensionPixelOffset(R.dimen.category_panel_margin);
        this.mTextSize = resources.getDimensionPixelSize(R.dimen.category_panel_text_size);
    }

    protected void computeTextPosition(String str) {
        if (str == null) {
            return;
        }
        this.mPaint.setTextSize(this.mTextSize);
        if (getOrientation() == 0) {
            str = str.toUpperCase();
            this.mPaint.setTypeface(Typeface.DEFAULT_BOLD);
        }
        this.mPaint.getTextBounds(str, 0, str.length(), this.mTextBounds);
    }

    public boolean needsCenterText() {
        return this.mOrientation == 1;
    }

    protected void drawText(Canvas canvas, String str) {
        if (str == null) {
            return;
        }
        float fMeasureText = this.mPaint.measureText(str);
        int width = (int) ((canvas.getWidth() - fMeasureText) - (this.mMargin * 2));
        if (needsCenterText()) {
            width = (int) ((canvas.getWidth() - fMeasureText) / 2.0f);
        }
        if (width < 0) {
            width = this.mMargin;
        }
        canvas.drawText(str, width, canvas.getHeight() - (2 * this.mMargin), this.mPaint);
    }

    protected void drawOutlinedText(Canvas canvas, String str) {
        this.mPaint.setColor(getBackgroundColor());
        this.mPaint.setStyle(Paint.Style.STROKE);
        this.mPaint.setStrokeWidth(3.0f);
        drawText(canvas, str);
        this.mPaint.setColor(getTextColor());
        this.mPaint.setStyle(Paint.Style.FILL);
        this.mPaint.setStrokeWidth(1.0f);
        drawText(canvas, str);
    }

    public int getOrientation() {
        return this.mOrientation;
    }

    public void setOrientation(int i) {
        this.mOrientation = i;
    }

    public int getTextColor() {
        return this.mTextColor;
    }

    public int getBackgroundColor() {
        return this.mBackgroundColor;
    }

    public void setText(String str) {
        this.mText = str;
    }

    public String getText() {
        return this.mText;
    }

    public void setBitmap(Bitmap bitmap) {
        this.mBitmap = bitmap;
    }

    public void setUseOnlyDrawable(boolean z) {
        this.mUseOnlyDrawable = z;
    }

    @Override
    public CharSequence getContentDescription() {
        return this.mText;
    }

    public boolean isHalfImage() {
        return false;
    }

    public void computeBitmapBounds() {
        if (this.mUseOnlyDrawable) {
            this.mBitmapBounds = new Rect(this.mMargin / 2, this.mMargin, getWidth() - (this.mMargin / 2), (getHeight() - this.mTextSize) - (2 * this.mMargin));
        } else if (getOrientation() == 0 && isHalfImage()) {
            this.mBitmapBounds = new Rect(this.mMargin / 2, this.mMargin, getWidth() / 2, getHeight());
        } else {
            this.mBitmapBounds = new Rect(this.mMargin / 2, this.mMargin, getWidth() - (this.mMargin / 2), getHeight());
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        this.mPaint.reset();
        this.mPaint.setAntiAlias(true);
        this.mPaint.setFilterBitmap(true);
        canvas.drawColor(this.mBackgroundColor);
        computeBitmapBounds();
        computeTextPosition(getText());
        float width = 0.0f;
        if (this.mBitmap != null) {
            canvas.save();
            canvas.clipRect(this.mBitmapBounds);
            Matrix matrix = new Matrix();
            if (this.mUseOnlyDrawable) {
                this.mPaint.setFilterBitmap(true);
                matrix.setRectToRect(new RectF(0.0f, 0.0f, this.mBitmap.getWidth(), this.mBitmap.getHeight()), new RectF(this.mBitmapBounds), Matrix.ScaleToFit.CENTER);
            } else {
                float fMax = Math.max(this.mBitmapBounds.width() / this.mBitmap.getWidth(), this.mBitmapBounds.height() / this.mBitmap.getHeight());
                matrix.postScale(fMax, fMax);
                matrix.postTranslate(((this.mBitmapBounds.width() - (this.mBitmap.getWidth() * fMax)) / 2.0f) + this.mBitmapBounds.left, ((this.mBitmapBounds.height() - (this.mBitmap.getHeight() * fMax)) / 2.0f) + this.mBitmapBounds.top);
            }
            canvas.drawBitmap(this.mBitmap, matrix, this.mPaint);
            canvas.restore();
        }
        if (!this.mUseOnlyDrawable) {
            int iArgb = Color.argb(0, 0, 0, 0);
            int iArgb2 = Color.argb(200, 0, 0, 0);
            float height = (getHeight() - (this.mMargin * 2)) - (this.mTextSize * 2);
            float height2 = getHeight();
            this.mPaint.setShader(new LinearGradient(0.0f, height, 0.0f, height2, iArgb, iArgb2, Shader.TileMode.CLAMP));
            if (getOrientation() == 0 && isHalfImage()) {
                width = getWidth() / 2;
            }
            canvas.drawRect(new RectF(width, height, getWidth(), height2), this.mPaint);
            this.mPaint.setShader(null);
        }
        drawOutlinedText(canvas, getText());
    }
}
