package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import com.android.internal.R;

public class EdgeEffect {
    private static final float EPSILON = 0.001f;
    private static final float GLOW_ALPHA_START = 0.09f;
    private static final float MAX_ALPHA = 0.15f;
    private static final float MAX_GLOW_SCALE = 2.0f;
    private static final int MAX_VELOCITY = 10000;
    private static final int MIN_VELOCITY = 100;
    private static final int PULL_DECAY_TIME = 2000;
    private static final float PULL_DISTANCE_ALPHA_GLOW_FACTOR = 0.8f;
    private static final float PULL_GLOW_BEGIN = 0.0f;
    private static final int PULL_TIME = 167;
    private static final float RADIUS_FACTOR = 0.6f;
    private static final int RECEDE_TIME = 600;
    private static final int STATE_ABSORB = 2;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PULL = 1;
    private static final int STATE_PULL_DECAY = 4;
    private static final int STATE_RECEDE = 3;
    private static final String TAG = "EdgeEffect";
    private static final int VELOCITY_GLOW_FACTOR = 6;
    private float mBaseGlowScale;
    private float mDuration;
    private float mGlowAlpha;
    private float mGlowAlphaFinish;
    private float mGlowAlphaStart;
    private float mGlowScaleY;
    private float mGlowScaleYFinish;
    private float mGlowScaleYStart;
    private final Interpolator mInterpolator;
    private float mPullDistance;
    private float mRadius;
    private long mStartTime;
    private static final double ANGLE = 0.5235987755982988d;
    private static final float SIN = (float) Math.sin(ANGLE);
    private static final float COS = (float) Math.cos(ANGLE);
    private int mState = 0;
    private final Rect mBounds = new Rect();
    private final Paint mPaint = new Paint();
    private float mDisplacement = 0.5f;
    private float mTargetDisplacement = 0.5f;

    public EdgeEffect(Context context) {
        this.mPaint.setAntiAlias(true);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(R.styleable.EdgeEffect);
        int color = typedArrayObtainStyledAttributes.getColor(0, -10066330);
        typedArrayObtainStyledAttributes.recycle();
        this.mPaint.setColor((color & 16777215) | 855638016);
        this.mPaint.setStyle(Paint.Style.FILL);
        this.mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        this.mInterpolator = new DecelerateInterpolator();
    }

    public void setSize(int i, int i2) {
        float f = (i * 0.6f) / SIN;
        float f2 = f - (COS * f);
        float f3 = i2;
        float f4 = (0.6f * f3) / SIN;
        float f5 = f4 - (COS * f4);
        this.mRadius = f;
        this.mBaseGlowScale = f2 > 0.0f ? Math.min(f5 / f2, 1.0f) : 1.0f;
        this.mBounds.set(this.mBounds.left, this.mBounds.top, i, (int) Math.min(f3, f2));
    }

    public boolean isFinished() {
        return this.mState == 0;
    }

    public void finish() {
        this.mState = 0;
    }

    public void onPull(float f) {
        onPull(f, 0.5f);
    }

    public void onPull(float f, float f2) {
        long jCurrentAnimationTimeMillis = AnimationUtils.currentAnimationTimeMillis();
        this.mTargetDisplacement = f2;
        if (this.mState == 4 && jCurrentAnimationTimeMillis - this.mStartTime < this.mDuration) {
            return;
        }
        if (this.mState != 1) {
            this.mGlowScaleY = Math.max(0.0f, this.mGlowScaleY);
        }
        this.mState = 1;
        this.mStartTime = jCurrentAnimationTimeMillis;
        this.mDuration = 167.0f;
        this.mPullDistance += f;
        float fMin = Math.min(MAX_ALPHA, this.mGlowAlpha + (Math.abs(f) * PULL_DISTANCE_ALPHA_GLOW_FACTOR));
        this.mGlowAlphaStart = fMin;
        this.mGlowAlpha = fMin;
        if (this.mPullDistance == 0.0f) {
            this.mGlowScaleYStart = 0.0f;
            this.mGlowScaleY = 0.0f;
        } else {
            float fMax = (float) (Math.max(0.0d, (1.0d - (1.0d / Math.sqrt(Math.abs(this.mPullDistance) * this.mBounds.height()))) - 0.3d) / 0.7d);
            this.mGlowScaleYStart = fMax;
            this.mGlowScaleY = fMax;
        }
        this.mGlowAlphaFinish = this.mGlowAlpha;
        this.mGlowScaleYFinish = this.mGlowScaleY;
    }

    public void onRelease() {
        this.mPullDistance = 0.0f;
        if (this.mState != 1 && this.mState != 4) {
            return;
        }
        this.mState = 3;
        this.mGlowAlphaStart = this.mGlowAlpha;
        this.mGlowScaleYStart = this.mGlowScaleY;
        this.mGlowAlphaFinish = 0.0f;
        this.mGlowScaleYFinish = 0.0f;
        this.mStartTime = AnimationUtils.currentAnimationTimeMillis();
        this.mDuration = 600.0f;
    }

    public void onAbsorb(int i) {
        this.mState = 2;
        int iMin = Math.min(Math.max(100, Math.abs(i)), 10000);
        this.mStartTime = AnimationUtils.currentAnimationTimeMillis();
        this.mDuration = (iMin * 0.02f) + MAX_ALPHA;
        this.mGlowAlphaStart = GLOW_ALPHA_START;
        this.mGlowScaleYStart = Math.max(this.mGlowScaleY, 0.0f);
        this.mGlowScaleYFinish = Math.min(0.025f + ((((iMin / 100) * iMin) * 1.5E-4f) / MAX_GLOW_SCALE), 1.0f);
        this.mGlowAlphaFinish = Math.max(this.mGlowAlphaStart, Math.min(iMin * 6 * 1.0E-5f, MAX_ALPHA));
        this.mTargetDisplacement = 0.5f;
    }

    public void setColor(int i) {
        this.mPaint.setColor(i);
    }

    public int getColor() {
        return this.mPaint.getColor();
    }

    public boolean draw(Canvas canvas) {
        boolean z;
        update();
        int iSave = canvas.save();
        float fCenterX = this.mBounds.centerX();
        float fHeight = this.mBounds.height() - this.mRadius;
        canvas.scale(1.0f, Math.min(this.mGlowScaleY, 1.0f) * this.mBaseGlowScale, fCenterX, 0.0f);
        float fWidth = (this.mBounds.width() * (Math.max(0.0f, Math.min(this.mDisplacement, 1.0f)) - 0.5f)) / MAX_GLOW_SCALE;
        canvas.clipRect(this.mBounds);
        canvas.translate(fWidth, 0.0f);
        this.mPaint.setAlpha((int) (255.0f * this.mGlowAlpha));
        canvas.drawCircle(fCenterX, fHeight, this.mRadius, this.mPaint);
        canvas.restoreToCount(iSave);
        if (this.mState == 3 && this.mGlowScaleY == 0.0f) {
            this.mState = 0;
            z = true;
        } else {
            z = false;
        }
        return this.mState != 0 || z;
    }

    public int getMaxHeight() {
        return (int) ((this.mBounds.height() * MAX_GLOW_SCALE) + 0.5f);
    }

    private void update() {
        float fMin = Math.min((AnimationUtils.currentAnimationTimeMillis() - this.mStartTime) / this.mDuration, 1.0f);
        float interpolation = this.mInterpolator.getInterpolation(fMin);
        this.mGlowAlpha = this.mGlowAlphaStart + ((this.mGlowAlphaFinish - this.mGlowAlphaStart) * interpolation);
        this.mGlowScaleY = this.mGlowScaleYStart + ((this.mGlowScaleYFinish - this.mGlowScaleYStart) * interpolation);
        this.mDisplacement = (this.mDisplacement + this.mTargetDisplacement) / MAX_GLOW_SCALE;
        if (fMin >= 0.999f) {
            switch (this.mState) {
                case 1:
                    this.mState = 4;
                    this.mStartTime = AnimationUtils.currentAnimationTimeMillis();
                    this.mDuration = 2000.0f;
                    this.mGlowAlphaStart = this.mGlowAlpha;
                    this.mGlowScaleYStart = this.mGlowScaleY;
                    this.mGlowAlphaFinish = 0.0f;
                    this.mGlowScaleYFinish = 0.0f;
                    break;
                case 2:
                    this.mState = 3;
                    this.mStartTime = AnimationUtils.currentAnimationTimeMillis();
                    this.mDuration = 600.0f;
                    this.mGlowAlphaStart = this.mGlowAlpha;
                    this.mGlowScaleYStart = this.mGlowScaleY;
                    this.mGlowAlphaFinish = 0.0f;
                    this.mGlowScaleYFinish = 0.0f;
                    break;
                case 3:
                    this.mState = 0;
                    break;
                case 4:
                    this.mState = 3;
                    break;
            }
        }
    }
}
