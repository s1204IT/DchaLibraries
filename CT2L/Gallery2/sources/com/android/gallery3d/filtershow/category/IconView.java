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

    public IconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mPaint = new Paint();
        this.mMargin = 16;
        this.mOrientation = 1;
        this.mTextSize = 32;
        this.mTextBounds = new Rect();
        this.mUseOnlyDrawable = false;
        setup(context);
        int bitmapRsc = attrs.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "src", 0);
        Resources res = context.getResources();
        Bitmap bitmap = BitmapFactory.decodeStream(res.openRawResource(bitmapRsc));
        setBitmap(bitmap);
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
        Resources res = getResources();
        this.mTextColor = res.getColor(R.color.filtershow_categoryview_text);
        this.mBackgroundColor = res.getColor(R.color.filtershow_categoryview_background);
        this.mMargin = res.getDimensionPixelOffset(R.dimen.category_panel_margin);
        this.mTextSize = res.getDimensionPixelSize(R.dimen.category_panel_text_size);
    }

    protected void computeTextPosition(String text) {
        if (text != null) {
            this.mPaint.setTextSize(this.mTextSize);
            if (getOrientation() == 0) {
                text = text.toUpperCase();
                this.mPaint.setTypeface(Typeface.DEFAULT_BOLD);
            }
            this.mPaint.getTextBounds(text, 0, text.length(), this.mTextBounds);
        }
    }

    public boolean needsCenterText() {
        return this.mOrientation == 1;
    }

    protected void drawText(Canvas canvas, String text) {
        if (text != null) {
            float textWidth = this.mPaint.measureText(text);
            int x = (int) ((canvas.getWidth() - textWidth) - (this.mMargin * 2));
            if (needsCenterText()) {
                x = (int) ((canvas.getWidth() - textWidth) / 2.0f);
            }
            if (x < 0) {
                x = this.mMargin;
            }
            int y = canvas.getHeight() - (this.mMargin * 2);
            canvas.drawText(text, x, y, this.mPaint);
        }
    }

    protected void drawOutlinedText(Canvas canvas, String text) {
        this.mPaint.setColor(getBackgroundColor());
        this.mPaint.setStyle(Paint.Style.STROKE);
        this.mPaint.setStrokeWidth(3.0f);
        drawText(canvas, text);
        this.mPaint.setColor(getTextColor());
        this.mPaint.setStyle(Paint.Style.FILL);
        this.mPaint.setStrokeWidth(1.0f);
        drawText(canvas, text);
    }

    public int getOrientation() {
        return this.mOrientation;
    }

    public void setOrientation(int orientation) {
        this.mOrientation = orientation;
    }

    public int getTextColor() {
        return this.mTextColor;
    }

    public int getBackgroundColor() {
        return this.mBackgroundColor;
    }

    public void setText(String text) {
        this.mText = text;
    }

    public String getText() {
        return this.mText;
    }

    public void setBitmap(Bitmap bitmap) {
        this.mBitmap = bitmap;
    }

    public void setUseOnlyDrawable(boolean value) {
        this.mUseOnlyDrawable = value;
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
            this.mBitmapBounds = new Rect(this.mMargin / 2, this.mMargin, getWidth() - (this.mMargin / 2), (getHeight() - this.mTextSize) - (this.mMargin * 2));
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
        if (this.mBitmap != null) {
            canvas.save();
            canvas.clipRect(this.mBitmapBounds);
            Matrix m = new Matrix();
            if (this.mUseOnlyDrawable) {
                this.mPaint.setFilterBitmap(true);
                m.setRectToRect(new RectF(0.0f, 0.0f, this.mBitmap.getWidth(), this.mBitmap.getHeight()), new RectF(this.mBitmapBounds), Matrix.ScaleToFit.CENTER);
            } else {
                float scaleWidth = this.mBitmapBounds.width() / this.mBitmap.getWidth();
                float scaleHeight = this.mBitmapBounds.height() / this.mBitmap.getHeight();
                float scale = Math.max(scaleWidth, scaleHeight);
                float dx = (this.mBitmapBounds.width() - (this.mBitmap.getWidth() * scale)) / 2.0f;
                float dy = (this.mBitmapBounds.height() - (this.mBitmap.getHeight() * scale)) / 2.0f;
                m.postScale(scale, scale);
                m.postTranslate(dx + this.mBitmapBounds.left, dy + this.mBitmapBounds.top);
            }
            canvas.drawBitmap(this.mBitmap, m, this.mPaint);
            canvas.restore();
        }
        if (!this.mUseOnlyDrawable) {
            int startColor = Color.argb(0, 0, 0, 0);
            int endColor = Color.argb(200, 0, 0, 0);
            float start = (getHeight() - (this.mMargin * 2)) - (this.mTextSize * 2);
            float end = getHeight();
            Shader shader = new LinearGradient(0.0f, start, 0.0f, end, startColor, endColor, Shader.TileMode.CLAMP);
            this.mPaint.setShader(shader);
            float startGradient = 0.0f;
            if (getOrientation() == 0 && isHalfImage()) {
                startGradient = getWidth() / 2;
            }
            canvas.drawRect(new RectF(startGradient, start, getWidth(), end), this.mPaint);
            this.mPaint.setShader(null);
        }
        drawOutlinedText(canvas, getText());
    }
}
