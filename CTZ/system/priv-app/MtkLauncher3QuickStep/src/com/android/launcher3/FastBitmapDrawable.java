package com.android.launcher3;

import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Property;
import android.util.SparseArray;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.graphics.BitmapInfo;

/* loaded from: classes.dex */
public class FastBitmapDrawable extends Drawable {
    public static final int CLICK_FEEDBACK_DURATION = 200;
    private static final float DISABLED_BRIGHTNESS = 0.5f;
    private static final float DISABLED_DESATURATION = 1.0f;
    private static final float PRESSED_SCALE = 1.1f;
    private static final int REDUCED_FILTER_VALUE_SPACE = 48;
    private int mAlpha;
    protected Bitmap mBitmap;
    private int mBrightness;
    private int mDesaturation;
    protected final int mIconColor;
    private boolean mIsDisabled;
    private boolean mIsPressed;
    protected final Paint mPaint;
    private int mPrevUpdateKey;
    private float mScale;
    private ObjectAnimator mScaleAnimation;
    private static final SparseArray<ColorFilter> sCachedFilter = new SparseArray<>();
    private static final ColorMatrix sTempBrightnessMatrix = new ColorMatrix();
    private static final ColorMatrix sTempFilterMatrix = new ColorMatrix();
    private static final Property<FastBitmapDrawable, Float> SCALE = new Property<FastBitmapDrawable, Float>(Float.TYPE, "scale") { // from class: com.android.launcher3.FastBitmapDrawable.1
        /* JADX DEBUG: Method merged with bridge method: get(Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.util.Property
        public Float get(FastBitmapDrawable fastBitmapDrawable) {
            return Float.valueOf(fastBitmapDrawable.mScale);
        }

        /* JADX DEBUG: Method merged with bridge method: set(Ljava/lang/Object;Ljava/lang/Object;)V */
        @Override // android.util.Property
        public void set(FastBitmapDrawable fastBitmapDrawable, Float f) {
            fastBitmapDrawable.mScale = f.floatValue();
            fastBitmapDrawable.invalidateSelf();
        }
    };

    public FastBitmapDrawable(Bitmap bitmap) {
        this(bitmap, 0);
    }

    public FastBitmapDrawable(BitmapInfo bitmapInfo) {
        this(bitmapInfo.icon, bitmapInfo.color);
    }

    public FastBitmapDrawable(ItemInfoWithIcon itemInfoWithIcon) {
        this(itemInfoWithIcon.iconBitmap, itemInfoWithIcon.iconColor);
    }

    protected FastBitmapDrawable(Bitmap bitmap, int i) {
        this.mPaint = new Paint(3);
        this.mScale = 1.0f;
        this.mDesaturation = 0;
        this.mBrightness = 0;
        this.mAlpha = 255;
        this.mPrevUpdateKey = Integer.MAX_VALUE;
        this.mBitmap = bitmap;
        this.mIconColor = i;
        setFilterBitmap(true);
    }

    @Override // android.graphics.drawable.Drawable
    public final void draw(Canvas canvas) {
        if (this.mScaleAnimation != null) {
            int iSave = canvas.save();
            Rect bounds = getBounds();
            canvas.scale(this.mScale, this.mScale, bounds.exactCenterX(), bounds.exactCenterY());
            drawInternal(canvas, bounds);
            canvas.restoreToCount(iSave);
            return;
        }
        drawInternal(canvas, getBounds());
    }

    protected void drawInternal(Canvas canvas, Rect rect) {
        canvas.drawBitmap(this.mBitmap, (Rect) null, rect, this.mPaint);
    }

    @Override // android.graphics.drawable.Drawable
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override // android.graphics.drawable.Drawable
    public int getOpacity() {
        return -3;
    }

    @Override // android.graphics.drawable.Drawable
    public void setAlpha(int i) {
        this.mAlpha = i;
        this.mPaint.setAlpha(i);
    }

    @Override // android.graphics.drawable.Drawable
    public void setFilterBitmap(boolean z) {
        this.mPaint.setFilterBitmap(z);
        this.mPaint.setAntiAlias(z);
    }

    @Override // android.graphics.drawable.Drawable
    public int getAlpha() {
        return this.mAlpha;
    }

    public float getAnimatedScale() {
        if (this.mScaleAnimation == null) {
            return 1.0f;
        }
        return this.mScale;
    }

    @Override // android.graphics.drawable.Drawable
    public int getIntrinsicWidth() {
        return this.mBitmap.getWidth();
    }

    @Override // android.graphics.drawable.Drawable
    public int getIntrinsicHeight() {
        return this.mBitmap.getHeight();
    }

    @Override // android.graphics.drawable.Drawable
    public int getMinimumWidth() {
        return getBounds().width();
    }

    @Override // android.graphics.drawable.Drawable
    public int getMinimumHeight() {
        return getBounds().height();
    }

    @Override // android.graphics.drawable.Drawable
    public boolean isStateful() {
        return true;
    }

    @Override // android.graphics.drawable.Drawable
    public ColorFilter getColorFilter() {
        return this.mPaint.getColorFilter();
    }

    @Override // android.graphics.drawable.Drawable
    protected boolean onStateChange(int[] iArr) {
        boolean z;
        int length = iArr.length;
        int i = 0;
        while (true) {
            if (i < length) {
                if (iArr[i] != 16842919) {
                    i++;
                } else {
                    z = true;
                    break;
                }
            } else {
                z = false;
                break;
            }
        }
        if (this.mIsPressed == z) {
            return false;
        }
        this.mIsPressed = z;
        if (this.mScaleAnimation != null) {
            this.mScaleAnimation.cancel();
            this.mScaleAnimation = null;
        }
        if (this.mIsPressed) {
            this.mScaleAnimation = ObjectAnimator.ofFloat(this, SCALE, PRESSED_SCALE);
            this.mScaleAnimation.setDuration(200L);
            this.mScaleAnimation.setInterpolator(Interpolators.ACCEL);
            this.mScaleAnimation.start();
        } else {
            this.mScale = 1.0f;
            invalidateSelf();
        }
        return true;
    }

    private void invalidateDesaturationAndBrightness() {
        setDesaturation(this.mIsDisabled ? 1.0f : 0.0f);
        setBrightness(this.mIsDisabled ? 0.5f : 0.0f);
    }

    public void setIsDisabled(boolean z) {
        if (this.mIsDisabled != z) {
            this.mIsDisabled = z;
            invalidateDesaturationAndBrightness();
        }
    }

    private void setDesaturation(float f) {
        int iFloor = (int) Math.floor(f * 48.0f);
        if (this.mDesaturation != iFloor) {
            this.mDesaturation = iFloor;
            updateFilter();
        }
    }

    public float getDesaturation() {
        return this.mDesaturation / 48.0f;
    }

    private void setBrightness(float f) {
        int iFloor = (int) Math.floor(f * 48.0f);
        if (this.mBrightness != iFloor) {
            this.mBrightness = iFloor;
            updateFilter();
        }
    }

    private float getBrightness() {
        return this.mBrightness / 48.0f;
    }

    /* JADX WARN: Removed duplicated region for block: B:12:0x0022 A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:13:0x0023  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void updateFilter() {
        int i;
        boolean z;
        if (this.mDesaturation > 0) {
            i = (this.mDesaturation << 16) | this.mBrightness;
        } else {
            if (this.mBrightness > 0) {
                i = 65536 | this.mBrightness;
                z = true;
                if (i != this.mPrevUpdateKey) {
                    return;
                }
                this.mPrevUpdateKey = i;
                if (i != -1) {
                    ColorFilter colorMatrixColorFilter = sCachedFilter.get(i);
                    if (colorMatrixColorFilter == null) {
                        float brightness = getBrightness();
                        int i2 = (int) (255.0f * brightness);
                        if (z) {
                            colorMatrixColorFilter = new PorterDuffColorFilter(Color.argb(i2, 255, 255, 255), PorterDuff.Mode.SRC_ATOP);
                        } else {
                            sTempFilterMatrix.setSaturation(1.0f - getDesaturation());
                            if (this.mBrightness > 0) {
                                float f = 1.0f - brightness;
                                float[] array = sTempBrightnessMatrix.getArray();
                                array[0] = f;
                                array[6] = f;
                                array[12] = f;
                                float f2 = i2;
                                array[4] = f2;
                                array[9] = f2;
                                array[14] = f2;
                                sTempFilterMatrix.preConcat(sTempBrightnessMatrix);
                            }
                            colorMatrixColorFilter = new ColorMatrixColorFilter(sTempFilterMatrix);
                        }
                        sCachedFilter.append(i, colorMatrixColorFilter);
                    }
                    this.mPaint.setColorFilter(colorMatrixColorFilter);
                } else {
                    this.mPaint.setColorFilter(null);
                }
                invalidateSelf();
                return;
            }
            i = -1;
        }
        z = false;
        if (i != this.mPrevUpdateKey) {
        }
    }

    @Override // android.graphics.drawable.Drawable
    public Drawable.ConstantState getConstantState() {
        return new MyConstantState(this.mBitmap, this.mIconColor);
    }

    protected static class MyConstantState extends Drawable.ConstantState {
        protected final Bitmap mBitmap;
        protected final int mIconColor;

        public MyConstantState(Bitmap bitmap, int i) {
            this.mBitmap = bitmap;
            this.mIconColor = i;
        }

        @Override // android.graphics.drawable.Drawable.ConstantState
        public Drawable newDrawable() {
            return new FastBitmapDrawable(this.mBitmap, this.mIconColor);
        }

        @Override // android.graphics.drawable.Drawable.ConstantState
        public int getChangingConfigurations() {
            return 0;
        }
    }
}
