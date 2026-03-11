package com.android.launcher3;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
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
import android.util.SparseArray;
import android.view.animation.DecelerateInterpolator;
import com.android.launcher3.compat.PackageInstallerCompat;

public class FastBitmapDrawable extends Drawable {

    private static final int[] f2comandroidlauncher3FastBitmapDrawable$StateSwitchesValues = null;
    public static final TimeInterpolator CLICK_FEEDBACK_INTERPOLATOR = new TimeInterpolator() {
        @Override
        public float getInterpolation(float input) {
            if (input < 0.05f) {
                return input / 0.05f;
            }
            if (input < 0.3f) {
                return 1.0f;
            }
            return (1.0f - input) / 0.7f;
        }
    };
    private static final SparseArray<ColorFilter> sCachedFilter = new SparseArray<>();
    private static final ColorMatrix sTempBrightnessMatrix = new ColorMatrix();
    private static final ColorMatrix sTempFilterMatrix = new ColorMatrix();
    private final Bitmap mBitmap;
    private AnimatorSet mPropertyAnimator;
    private final Paint mPaint = new Paint(2);
    private State mState = State.NORMAL;
    private int mDesaturation = 0;
    private int mBrightness = 0;
    private int mAlpha = 255;
    private int mPrevUpdateKey = Integer.MAX_VALUE;

    private static int[] m99getcomandroidlauncher3FastBitmapDrawable$StateSwitchesValues() {
        if (f2comandroidlauncher3FastBitmapDrawable$StateSwitchesValues != null) {
            return f2comandroidlauncher3FastBitmapDrawable$StateSwitchesValues;
        }
        int[] iArr = new int[State.valuesCustom().length];
        try {
            iArr[State.DISABLED.ordinal()] = 5;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[State.FAST_SCROLL_HIGHLIGHTED.ordinal()] = 1;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[State.FAST_SCROLL_UNHIGHLIGHTED.ordinal()] = 2;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[State.NORMAL.ordinal()] = 3;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[State.PRESSED.ordinal()] = 4;
        } catch (NoSuchFieldError e5) {
        }
        f2comandroidlauncher3FastBitmapDrawable$StateSwitchesValues = iArr;
        return iArr;
    }

    public enum State {
        NORMAL(0.0f, 0.0f, 1.0f, new DecelerateInterpolator()),
        PRESSED(0.0f, 0.39215687f, 1.0f, FastBitmapDrawable.CLICK_FEEDBACK_INTERPOLATOR),
        FAST_SCROLL_HIGHLIGHTED(0.0f, 0.0f, 1.15f, new DecelerateInterpolator()),
        FAST_SCROLL_UNHIGHLIGHTED(0.0f, 0.0f, 1.0f, new DecelerateInterpolator()),
        DISABLED(1.0f, 0.5f, 1.0f, new DecelerateInterpolator());

        public final float brightness;
        public final float desaturation;
        public final TimeInterpolator interpolator;
        public final float viewScale;

        public static State[] valuesCustom() {
            return values();
        }

        State(float desaturation, float brightness, float viewScale, TimeInterpolator interpolator) {
            this.desaturation = desaturation;
            this.brightness = brightness;
            this.viewScale = viewScale;
            this.interpolator = interpolator;
        }
    }

    public FastBitmapDrawable(Bitmap b) {
        this.mBitmap = b;
        setBounds(0, 0, b.getWidth(), b.getHeight());
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawBitmap(this.mBitmap, (Rect) null, getBounds(), this.mPaint);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }

    @Override
    public int getOpacity() {
        return -3;
    }

    @Override
    public void setAlpha(int alpha) {
        this.mAlpha = alpha;
        this.mPaint.setAlpha(alpha);
    }

    @Override
    public void setFilterBitmap(boolean filterBitmap) {
        this.mPaint.setFilterBitmap(filterBitmap);
        this.mPaint.setAntiAlias(filterBitmap);
    }

    @Override
    public int getAlpha() {
        return this.mAlpha;
    }

    @Override
    public int getIntrinsicWidth() {
        return this.mBitmap.getWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return this.mBitmap.getHeight();
    }

    @Override
    public int getMinimumWidth() {
        return getBounds().width();
    }

    @Override
    public int getMinimumHeight() {
        return getBounds().height();
    }

    public Bitmap getBitmap() {
        return this.mBitmap;
    }

    public boolean animateState(State newState) {
        State prevState = this.mState;
        if (this.mState == newState) {
            return false;
        }
        this.mState = newState;
        this.mPropertyAnimator = cancelAnimator(this.mPropertyAnimator);
        this.mPropertyAnimator = new AnimatorSet();
        this.mPropertyAnimator.playTogether(ObjectAnimator.ofFloat(this, "desaturation", newState.desaturation), ObjectAnimator.ofFloat(this, "brightness", newState.brightness));
        this.mPropertyAnimator.setInterpolator(newState.interpolator);
        this.mPropertyAnimator.setDuration(getDurationForStateChange(prevState, newState));
        this.mPropertyAnimator.setStartDelay(getStartDelayForStateChange(prevState, newState));
        this.mPropertyAnimator.start();
        return true;
    }

    public boolean setState(State newState) {
        if (this.mState != newState) {
            this.mState = newState;
            this.mPropertyAnimator = cancelAnimator(this.mPropertyAnimator);
            setDesaturation(newState.desaturation);
            setBrightness(newState.brightness);
            return true;
        }
        return false;
    }

    public State getCurrentState() {
        return this.mState;
    }

    public static int getDurationForStateChange(com.android.launcher3.FastBitmapDrawable.State r4, com.android.launcher3.FastBitmapDrawable.State r5) {
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.FastBitmapDrawable.getDurationForStateChange(com.android.launcher3.FastBitmapDrawable$State, com.android.launcher3.FastBitmapDrawable$State):int");
    }

    public static int getStartDelayForStateChange(State fromState, State toState) {
        switch (m99getcomandroidlauncher3FastBitmapDrawable$StateSwitchesValues()[toState.ordinal()]) {
            case PackageInstallerCompat.STATUS_FAILED:
                switch (m99getcomandroidlauncher3FastBitmapDrawable$StateSwitchesValues()[fromState.ordinal()]) {
                    case 3:
                        return 37;
                    default:
                        return 0;
                }
            default:
                return 0;
        }
    }

    public void setDesaturation(float desaturation) {
        int newDesaturation = (int) Math.floor(48.0f * desaturation);
        if (this.mDesaturation == newDesaturation) {
            return;
        }
        this.mDesaturation = newDesaturation;
        updateFilter();
    }

    public float getDesaturation() {
        return this.mDesaturation / 48.0f;
    }

    public void setBrightness(float brightness) {
        int newBrightness = (int) Math.floor(48.0f * brightness);
        if (this.mBrightness == newBrightness) {
            return;
        }
        this.mBrightness = newBrightness;
        updateFilter();
    }

    public float getBrightness() {
        return this.mBrightness / 48.0f;
    }

    private void updateFilter() {
        boolean usePorterDuffFilter = false;
        int key = -1;
        if (this.mDesaturation > 0) {
            key = (this.mDesaturation << 16) | this.mBrightness;
        } else if (this.mBrightness > 0) {
            key = 65536 | this.mBrightness;
            usePorterDuffFilter = true;
        }
        if (key == this.mPrevUpdateKey) {
            return;
        }
        this.mPrevUpdateKey = key;
        if (key != -1) {
            ColorFilter filter = sCachedFilter.get(key);
            if (filter == null) {
                float brightnessF = getBrightness();
                int brightnessI = (int) (255.0f * brightnessF);
                if (usePorterDuffFilter) {
                    filter = new PorterDuffColorFilter(Color.argb(brightnessI, 255, 255, 255), PorterDuff.Mode.SRC_ATOP);
                } else {
                    float saturationF = 1.0f - getDesaturation();
                    sTempFilterMatrix.setSaturation(saturationF);
                    if (this.mBrightness > 0) {
                        float scale = 1.0f - brightnessF;
                        float[] mat = sTempBrightnessMatrix.getArray();
                        mat[0] = scale;
                        mat[6] = scale;
                        mat[12] = scale;
                        mat[4] = brightnessI;
                        mat[9] = brightnessI;
                        mat[14] = brightnessI;
                        sTempFilterMatrix.preConcat(sTempBrightnessMatrix);
                    }
                    filter = new ColorMatrixColorFilter(sTempFilterMatrix);
                }
                sCachedFilter.append(key, filter);
            }
            this.mPaint.setColorFilter(filter);
        } else {
            this.mPaint.setColorFilter(null);
        }
        invalidateSelf();
    }

    private AnimatorSet cancelAnimator(AnimatorSet animator) {
        if (animator != null) {
            animator.removeAllListeners();
            animator.cancel();
        }
        return null;
    }
}
