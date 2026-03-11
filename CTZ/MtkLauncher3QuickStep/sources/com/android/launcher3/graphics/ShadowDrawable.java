package com.android.launcher3.graphics;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

@TargetApi(26)
public class ShadowDrawable extends Drawable {
    private final Paint mPaint;
    private final ShadowDrawableState mState;

    public ShadowDrawable() {
        this(new ShadowDrawableState());
    }

    private ShadowDrawable(ShadowDrawableState shadowDrawableState) {
        this.mPaint = new Paint(3);
        this.mState = shadowDrawableState;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty()) {
            return;
        }
        if (this.mState.mLastDrawnBitmap == null) {
            regenerateBitmapCache();
        }
        canvas.drawBitmap(this.mState.mLastDrawnBitmap, (Rect) null, bounds, this.mPaint);
    }

    @Override
    public void setAlpha(int i) {
        this.mPaint.setAlpha(i);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        this.mPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public Drawable.ConstantState getConstantState() {
        return this.mState;
    }

    @Override
    public int getOpacity() {
        return -3;
    }

    @Override
    public int getIntrinsicHeight() {
        return this.mState.mIntrinsicHeight;
    }

    @Override
    public int getIntrinsicWidth() {
        return this.mState.mIntrinsicWidth;
    }

    @Override
    public boolean canApplyTheme() {
        return this.mState.canApplyTheme();
    }

    @Override
    public void applyTheme(Resources.Theme theme) {
        TypedArray typedArrayObtainStyledAttributes = theme.obtainStyledAttributes(new int[]{R.attr.isWorkspaceDarkText});
        boolean z = typedArrayObtainStyledAttributes.getBoolean(0, false);
        typedArrayObtainStyledAttributes.recycle();
        if (this.mState.mIsDark != z) {
            this.mState.mIsDark = z;
            this.mState.mLastDrawnBitmap = null;
            invalidateSelf();
        }
    }

    private void regenerateBitmapCache() {
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(this.mState.mIntrinsicWidth, this.mState.mIntrinsicHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        Drawable drawableMutate = this.mState.mChildState.newDrawable().mutate();
        drawableMutate.setBounds(this.mState.mShadowSize, this.mState.mShadowSize, this.mState.mIntrinsicWidth - this.mState.mShadowSize, this.mState.mIntrinsicHeight - this.mState.mShadowSize);
        drawableMutate.setTint(this.mState.mIsDark ? this.mState.mDarkTintColor : -1);
        drawableMutate.draw(canvas);
        if (!this.mState.mIsDark) {
            Paint paint = new Paint(3);
            paint.setMaskFilter(new BlurMaskFilter(this.mState.mShadowSize, BlurMaskFilter.Blur.NORMAL));
            Bitmap bitmapExtractAlpha = bitmapCreateBitmap.extractAlpha(paint, new int[2]);
            paint.setMaskFilter(null);
            paint.setColor(this.mState.mShadowColor);
            bitmapCreateBitmap.eraseColor(0);
            canvas.drawBitmap(bitmapExtractAlpha, r5[0], r5[1], paint);
            drawableMutate.draw(canvas);
        }
        if (Utilities.ATLEAST_OREO) {
            bitmapCreateBitmap = bitmapCreateBitmap.copy(Bitmap.Config.HARDWARE, false);
        }
        this.mState.mLastDrawnBitmap = bitmapCreateBitmap;
    }

    @Override
    public void inflate(Resources resources, XmlPullParser xmlPullParser, AttributeSet attributeSet, Resources.Theme theme) throws XmlPullParserException, IOException {
        TypedArray typedArrayObtainStyledAttributes;
        super.inflate(resources, xmlPullParser, attributeSet, theme);
        if (theme != null) {
            typedArrayObtainStyledAttributes = theme.obtainStyledAttributes(attributeSet, R.styleable.ShadowDrawable, 0, 0);
        } else {
            typedArrayObtainStyledAttributes = resources.obtainAttributes(attributeSet, R.styleable.ShadowDrawable);
        }
        try {
            Drawable drawable = typedArrayObtainStyledAttributes.getDrawable(0);
            if (drawable == null) {
                throw new XmlPullParserException("missing src attribute");
            }
            this.mState.mShadowColor = typedArrayObtainStyledAttributes.getColor(1, ViewCompat.MEASURED_STATE_MASK);
            this.mState.mShadowSize = typedArrayObtainStyledAttributes.getDimensionPixelSize(2, 0);
            this.mState.mDarkTintColor = typedArrayObtainStyledAttributes.getColor(3, ViewCompat.MEASURED_STATE_MASK);
            this.mState.mIntrinsicHeight = drawable.getIntrinsicHeight() + (this.mState.mShadowSize * 2);
            this.mState.mIntrinsicWidth = drawable.getIntrinsicWidth() + (2 * this.mState.mShadowSize);
            this.mState.mChangingConfigurations = drawable.getChangingConfigurations();
            this.mState.mChildState = drawable.getConstantState();
        } finally {
            typedArrayObtainStyledAttributes.recycle();
        }
    }

    private static class ShadowDrawableState extends Drawable.ConstantState {
        int mChangingConfigurations;
        Drawable.ConstantState mChildState;
        int mDarkTintColor;
        int mIntrinsicHeight;
        int mIntrinsicWidth;
        boolean mIsDark;
        Bitmap mLastDrawnBitmap;
        int mShadowColor;
        int mShadowSize;

        private ShadowDrawableState() {
        }

        @Override
        public Drawable newDrawable() {
            return new ShadowDrawable(this);
        }

        @Override
        public int getChangingConfigurations() {
            return this.mChangingConfigurations;
        }

        @Override
        public boolean canApplyTheme() {
            return true;
        }
    }
}
