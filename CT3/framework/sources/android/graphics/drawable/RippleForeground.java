package android.graphics.drawable;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.graphics.Canvas;
import android.graphics.CanvasProperty;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.RippleComponent;
import android.util.FloatProperty;
import android.util.MathUtils;
import android.view.DisplayListCanvas;
import android.view.RenderNodeAnimator;
import android.view.animation.LinearInterpolator;

class RippleForeground extends RippleComponent {
    private static final int BOUNDED_OPACITY_EXIT_DURATION = 400;
    private static final int BOUNDED_ORIGIN_EXIT_DURATION = 300;
    private static final int BOUNDED_RADIUS_EXIT_DURATION = 800;
    private static final float MAX_BOUNDED_RADIUS = 350.0f;
    private static final int OPACITY_ENTER_DURATION_FAST = 120;
    private static final int RIPPLE_ENTER_DELAY = 80;
    private static final float WAVE_OPACITY_DECAY_VELOCITY = 3.0f;
    private static final float WAVE_TOUCH_DOWN_ACCELERATION = 1024.0f;
    private static final float WAVE_TOUCH_UP_ACCELERATION = 3400.0f;
    private final AnimatorListenerAdapter mAnimationListener;
    private float mBoundedRadius;
    private float mClampedStartingX;
    private float mClampedStartingY;
    private boolean mHasFinishedExit;
    private boolean mIsBounded;
    private float mOpacity;
    private CanvasProperty<Paint> mPropPaint;
    private CanvasProperty<Float> mPropRadius;
    private CanvasProperty<Float> mPropX;
    private CanvasProperty<Float> mPropY;
    private float mStartingX;
    private float mStartingY;
    private float mTargetX;
    private float mTargetY;
    private float mTweenRadius;
    private float mTweenX;
    private float mTweenY;
    private static final TimeInterpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
    private static final TimeInterpolator DECELERATE_INTERPOLATOR = new LogDecelerateInterpolator(400.0f, 1.4f, 0.0f);
    private static final FloatProperty<RippleForeground> TWEEN_RADIUS = new FloatProperty<RippleForeground>("tweenRadius") {
        @Override
        public void setValue(RippleForeground object, float value) {
            object.mTweenRadius = value;
            object.invalidateSelf();
        }

        @Override
        public Float get(RippleForeground object) {
            return Float.valueOf(object.mTweenRadius);
        }
    };
    private static final FloatProperty<RippleForeground> TWEEN_ORIGIN = new FloatProperty<RippleForeground>("tweenOrigin") {
        @Override
        public void setValue(RippleForeground object, float value) {
            object.mTweenX = value;
            object.mTweenY = value;
            object.invalidateSelf();
        }

        @Override
        public Float get(RippleForeground object) {
            return Float.valueOf(object.mTweenX);
        }
    };
    private static final FloatProperty<RippleForeground> OPACITY = new FloatProperty<RippleForeground>("opacity") {
        @Override
        public void setValue(RippleForeground object, float value) {
            object.mOpacity = value;
            object.invalidateSelf();
        }

        @Override
        public Float get(RippleForeground object) {
            return Float.valueOf(object.mOpacity);
        }
    };

    public RippleForeground(RippleDrawable owner, Rect bounds, float startingX, float startingY, boolean isBounded, boolean forceSoftware) {
        super(owner, bounds, forceSoftware);
        this.mTargetX = 0.0f;
        this.mTargetY = 0.0f;
        this.mBoundedRadius = 0.0f;
        this.mOpacity = 1.0f;
        this.mTweenRadius = 0.0f;
        this.mTweenX = 0.0f;
        this.mTweenY = 0.0f;
        this.mAnimationListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                RippleForeground.this.mHasFinishedExit = true;
            }
        };
        this.mIsBounded = isBounded;
        this.mStartingX = startingX;
        this.mStartingY = startingY;
        if (isBounded) {
            this.mBoundedRadius = ((float) (Math.random() * 350.0d * 0.1d)) + 315.0f;
        } else {
            this.mBoundedRadius = 0.0f;
        }
    }

    @Override
    protected void onTargetRadiusChanged(float targetRadius) {
        clampStartingPosition();
    }

    @Override
    protected boolean drawSoftware(Canvas c, Paint p) {
        int origAlpha = p.getAlpha();
        int alpha = (int) ((origAlpha * this.mOpacity) + 0.5f);
        float radius = getCurrentRadius();
        if (alpha <= 0 || radius <= 0.0f) {
            return false;
        }
        float x = getCurrentX();
        float y = getCurrentY();
        p.setAlpha(alpha);
        c.drawCircle(x, y, radius, p);
        p.setAlpha(origAlpha);
        return true;
    }

    @Override
    protected boolean drawHardware(DisplayListCanvas c) {
        c.drawCircle(this.mPropX, this.mPropY, this.mPropRadius, this.mPropPaint);
        return true;
    }

    @Override
    public void getBounds(Rect bounds) {
        int outerX = (int) this.mTargetX;
        int outerY = (int) this.mTargetY;
        int r = ((int) this.mTargetRadius) + 1;
        bounds.set(outerX - r, outerY - r, outerX + r, outerY + r);
    }

    public void move(float x, float y) {
        this.mStartingX = x;
        this.mStartingY = y;
        clampStartingPosition();
    }

    public boolean hasFinishedExit() {
        return this.mHasFinishedExit;
    }

    @Override
    protected Animator createSoftwareEnter(boolean fast) {
        if (this.mIsBounded) {
            return null;
        }
        int duration = (int) ((Math.sqrt((this.mTargetRadius / WAVE_TOUCH_DOWN_ACCELERATION) * this.mDensityScale) * 1000.0d) + 0.5d);
        ObjectAnimator tweenRadius = ObjectAnimator.ofFloat(this, TWEEN_RADIUS, 1.0f);
        tweenRadius.setAutoCancel(true);
        tweenRadius.setDuration(duration);
        tweenRadius.setInterpolator(LINEAR_INTERPOLATOR);
        tweenRadius.setStartDelay(80L);
        ObjectAnimator tweenOrigin = ObjectAnimator.ofFloat(this, TWEEN_ORIGIN, 1.0f);
        tweenOrigin.setAutoCancel(true);
        tweenOrigin.setDuration(duration);
        tweenOrigin.setInterpolator(LINEAR_INTERPOLATOR);
        tweenOrigin.setStartDelay(80L);
        ObjectAnimator opacity = ObjectAnimator.ofFloat(this, OPACITY, 1.0f);
        opacity.setAutoCancel(true);
        opacity.setDuration(120L);
        opacity.setInterpolator(LINEAR_INTERPOLATOR);
        AnimatorSet set = new AnimatorSet();
        set.play(tweenOrigin).with(tweenRadius).with(opacity);
        return set;
    }

    private float getCurrentX() {
        return MathUtils.lerp(this.mClampedStartingX - this.mBounds.exactCenterX(), this.mTargetX, this.mTweenX);
    }

    private float getCurrentY() {
        return MathUtils.lerp(this.mClampedStartingY - this.mBounds.exactCenterY(), this.mTargetY, this.mTweenY);
    }

    private int getRadiusExitDuration() {
        float remainingRadius = this.mTargetRadius - getCurrentRadius();
        return (int) ((Math.sqrt((remainingRadius / 4424.0f) * this.mDensityScale) * 1000.0d) + 0.5d);
    }

    private float getCurrentRadius() {
        return MathUtils.lerp(0.0f, this.mTargetRadius, this.mTweenRadius);
    }

    private int getOpacityExitDuration() {
        return (int) (((this.mOpacity * 1000.0f) / WAVE_OPACITY_DECAY_VELOCITY) + 0.5f);
    }

    private void computeBoundedTargetValues() {
        this.mTargetX = (this.mClampedStartingX - this.mBounds.exactCenterX()) * 0.7f;
        this.mTargetY = (this.mClampedStartingY - this.mBounds.exactCenterY()) * 0.7f;
        this.mTargetRadius = this.mBoundedRadius;
    }

    @Override
    protected Animator createSoftwareExit() {
        int radiusDuration;
        int originDuration;
        int opacityDuration;
        if (this.mIsBounded) {
            computeBoundedTargetValues();
            radiusDuration = 800;
            originDuration = 300;
            opacityDuration = 400;
        } else {
            radiusDuration = getRadiusExitDuration();
            originDuration = radiusDuration;
            opacityDuration = getOpacityExitDuration();
        }
        ObjectAnimator tweenRadius = ObjectAnimator.ofFloat(this, TWEEN_RADIUS, 1.0f);
        tweenRadius.setAutoCancel(true);
        tweenRadius.setDuration(radiusDuration);
        tweenRadius.setInterpolator(DECELERATE_INTERPOLATOR);
        ObjectAnimator tweenOrigin = ObjectAnimator.ofFloat(this, TWEEN_ORIGIN, 1.0f);
        tweenOrigin.setAutoCancel(true);
        tweenOrigin.setDuration(originDuration);
        tweenOrigin.setInterpolator(DECELERATE_INTERPOLATOR);
        ObjectAnimator opacity = ObjectAnimator.ofFloat(this, OPACITY, 0.0f);
        opacity.setAutoCancel(true);
        opacity.setDuration(opacityDuration);
        opacity.setInterpolator(LINEAR_INTERPOLATOR);
        AnimatorSet set = new AnimatorSet();
        set.play(tweenOrigin).with(tweenRadius).with(opacity);
        set.addListener(this.mAnimationListener);
        return set;
    }

    @Override
    protected RippleComponent.RenderNodeAnimatorSet createHardwareExit(Paint p) {
        int radiusDuration;
        int originDuration;
        int opacityDuration;
        if (this.mIsBounded) {
            computeBoundedTargetValues();
            radiusDuration = 800;
            originDuration = 300;
            opacityDuration = 400;
        } else {
            radiusDuration = getRadiusExitDuration();
            originDuration = radiusDuration;
            opacityDuration = getOpacityExitDuration();
        }
        float startX = getCurrentX();
        float startY = getCurrentY();
        float startRadius = getCurrentRadius();
        p.setAlpha((int) ((p.getAlpha() * this.mOpacity) + 0.5f));
        this.mPropPaint = CanvasProperty.createPaint(p);
        this.mPropRadius = CanvasProperty.createFloat(startRadius);
        this.mPropX = CanvasProperty.createFloat(startX);
        this.mPropY = CanvasProperty.createFloat(startY);
        RenderNodeAnimator radius = new RenderNodeAnimator(this.mPropRadius, this.mTargetRadius);
        radius.setDuration(radiusDuration);
        radius.setInterpolator(DECELERATE_INTERPOLATOR);
        RenderNodeAnimator x = new RenderNodeAnimator(this.mPropX, this.mTargetX);
        x.setDuration(originDuration);
        x.setInterpolator(DECELERATE_INTERPOLATOR);
        RenderNodeAnimator y = new RenderNodeAnimator(this.mPropY, this.mTargetY);
        y.setDuration(originDuration);
        y.setInterpolator(DECELERATE_INTERPOLATOR);
        RenderNodeAnimator opacity = new RenderNodeAnimator(this.mPropPaint, 1, 0.0f);
        opacity.setDuration(opacityDuration);
        opacity.setInterpolator(LINEAR_INTERPOLATOR);
        opacity.addListener(this.mAnimationListener);
        RippleComponent.RenderNodeAnimatorSet set = new RippleComponent.RenderNodeAnimatorSet();
        set.add(radius);
        set.add(opacity);
        set.add(x);
        set.add(y);
        return set;
    }

    @Override
    protected void jumpValuesToExit() {
        this.mOpacity = 0.0f;
        this.mTweenX = 1.0f;
        this.mTweenY = 1.0f;
        this.mTweenRadius = 1.0f;
    }

    private void clampStartingPosition() {
        float cX = this.mBounds.exactCenterX();
        float cY = this.mBounds.exactCenterY();
        float dX = this.mStartingX - cX;
        float dY = this.mStartingY - cY;
        float r = this.mTargetRadius;
        if ((dX * dX) + (dY * dY) > r * r) {
            double angle = Math.atan2(dY, dX);
            this.mClampedStartingX = ((float) (Math.cos(angle) * ((double) r))) + cX;
            this.mClampedStartingY = ((float) (Math.sin(angle) * ((double) r))) + cY;
        } else {
            this.mClampedStartingX = this.mStartingX;
            this.mClampedStartingY = this.mStartingY;
        }
    }

    private static final class LogDecelerateInterpolator implements TimeInterpolator {
        private final float mBase;
        private final float mDrift;
        private final float mOutputScale = 1.0f / computeLog(1.0f);
        private final float mTimeScale;

        public LogDecelerateInterpolator(float base, float timeScale, float drift) {
            this.mBase = base;
            this.mDrift = drift;
            this.mTimeScale = 1.0f / timeScale;
        }

        private float computeLog(float t) {
            return (1.0f - ((float) Math.pow(this.mBase, (-t) * this.mTimeScale))) + (this.mDrift * t);
        }

        @Override
        public float getInterpolation(float t) {
            return computeLog(t) * this.mOutputScale;
        }
    }
}
