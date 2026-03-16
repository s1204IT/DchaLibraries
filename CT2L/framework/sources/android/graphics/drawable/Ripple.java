package android.graphics.drawable;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.graphics.Canvas;
import android.graphics.CanvasProperty;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.MathUtils;
import android.view.HardwareCanvas;
import android.view.RenderNodeAnimator;
import android.view.animation.LinearInterpolator;
import java.util.ArrayList;

class Ripple {
    private static final float GLOBAL_SPEED = 1.0f;
    private static final long RIPPLE_ENTER_DELAY = 80;
    private static final float WAVE_OPACITY_DECAY_VELOCITY = 3.0f;
    private static final float WAVE_TOUCH_DOWN_ACCELERATION = 1024.0f;
    private static final float WAVE_TOUCH_UP_ACCELERATION = 3400.0f;
    private ObjectAnimator mAnimOpacity;
    private ObjectAnimator mAnimRadius;
    private ObjectAnimator mAnimX;
    private ObjectAnimator mAnimY;
    private final Rect mBounds;
    private boolean mCanUseHardware;
    private boolean mCanceled;
    private float mClampedStartingX;
    private float mClampedStartingY;
    private float mDensity;
    private boolean mHardwareAnimating;
    private boolean mHasMaxRadius;
    private boolean mHasPendingHardwareExit;
    private float mOuterRadius;
    private float mOuterX;
    private float mOuterY;
    private final RippleDrawable mOwner;
    private int mPendingOpacityDuration;
    private int mPendingRadiusDuration;
    private CanvasProperty<Paint> mPropPaint;
    private CanvasProperty<Float> mPropRadius;
    private CanvasProperty<Float> mPropX;
    private CanvasProperty<Float> mPropY;
    private float mStartingX;
    private float mStartingY;
    private Paint mTempPaint;
    private static final TimeInterpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
    private static final TimeInterpolator DECEL_INTERPOLATOR = new LogInterpolator();
    private final ArrayList<RenderNodeAnimator> mRunningAnimations = new ArrayList<>();
    private float mOpacity = 1.0f;
    private float mTweenRadius = 0.0f;
    private float mTweenX = 0.0f;
    private float mTweenY = 0.0f;
    private final AnimatorListenerAdapter mAnimationListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            Ripple.this.removeSelf();
        }
    };

    public Ripple(RippleDrawable owner, Rect bounds, float startingX, float startingY) {
        this.mOwner = owner;
        this.mBounds = bounds;
        this.mStartingX = startingX;
        this.mStartingY = startingY;
    }

    public void setup(int maxRadius, float density) {
        if (maxRadius != -1) {
            this.mHasMaxRadius = true;
            this.mOuterRadius = maxRadius;
        } else {
            float halfWidth = this.mBounds.width() / 2.0f;
            float halfHeight = this.mBounds.height() / 2.0f;
            this.mOuterRadius = (float) Math.sqrt((halfWidth * halfWidth) + (halfHeight * halfHeight));
        }
        this.mOuterX = 0.0f;
        this.mOuterY = 0.0f;
        this.mDensity = density;
        clampStartingPosition();
    }

    public boolean isHardwareAnimating() {
        return this.mHardwareAnimating;
    }

    private void clampStartingPosition() {
        float cX = this.mBounds.exactCenterX();
        float cY = this.mBounds.exactCenterY();
        float dX = this.mStartingX - cX;
        float dY = this.mStartingY - cY;
        float r = this.mOuterRadius;
        if ((dX * dX) + (dY * dY) > r * r) {
            double angle = Math.atan2(dY, dX);
            this.mClampedStartingX = ((float) (Math.cos(angle) * ((double) r))) + cX;
            this.mClampedStartingY = ((float) (Math.sin(angle) * ((double) r))) + cY;
        } else {
            this.mClampedStartingX = this.mStartingX;
            this.mClampedStartingY = this.mStartingY;
        }
    }

    public void onHotspotBoundsChanged() {
        if (!this.mHasMaxRadius) {
            float halfWidth = this.mBounds.width() / 2.0f;
            float halfHeight = this.mBounds.height() / 2.0f;
            this.mOuterRadius = (float) Math.sqrt((halfWidth * halfWidth) + (halfHeight * halfHeight));
            clampStartingPosition();
        }
    }

    public void setOpacity(float a) {
        this.mOpacity = a;
        invalidateSelf();
    }

    public float getOpacity() {
        return this.mOpacity;
    }

    public void setRadiusGravity(float r) {
        this.mTweenRadius = r;
        invalidateSelf();
    }

    public float getRadiusGravity() {
        return this.mTweenRadius;
    }

    public void setXGravity(float x) {
        this.mTweenX = x;
        invalidateSelf();
    }

    public float getXGravity() {
        return this.mTweenX;
    }

    public void setYGravity(float y) {
        this.mTweenY = y;
        invalidateSelf();
    }

    public float getYGravity() {
        return this.mTweenY;
    }

    public boolean draw(Canvas c, Paint p) {
        boolean canUseHardware = c.isHardwareAccelerated();
        if (this.mCanUseHardware != canUseHardware && this.mCanUseHardware) {
            cancelHardwareAnimations(true);
        }
        this.mCanUseHardware = canUseHardware;
        if (canUseHardware && (this.mHardwareAnimating || this.mHasPendingHardwareExit)) {
            boolean hasContent = drawHardware((HardwareCanvas) c, p);
            return hasContent;
        }
        boolean hasContent2 = drawSoftware(c, p);
        return hasContent2;
    }

    private boolean drawHardware(HardwareCanvas c, Paint p) {
        if (this.mHasPendingHardwareExit) {
            cancelHardwareAnimations(false);
            startPendingHardwareExit(c, p);
        }
        c.drawCircle(this.mPropX, this.mPropY, this.mPropRadius, this.mPropPaint);
        return true;
    }

    private boolean drawSoftware(Canvas c, Paint p) {
        int paintAlpha = p.getAlpha();
        int alpha = (int) ((paintAlpha * this.mOpacity) + 0.5f);
        float radius = MathUtils.lerp(0.0f, this.mOuterRadius, this.mTweenRadius);
        if (alpha <= 0 || radius <= 0.0f) {
            return false;
        }
        float x = MathUtils.lerp(this.mClampedStartingX - this.mBounds.exactCenterX(), this.mOuterX, this.mTweenX);
        float y = MathUtils.lerp(this.mClampedStartingY - this.mBounds.exactCenterY(), this.mOuterY, this.mTweenY);
        p.setAlpha(alpha);
        c.drawCircle(x, y, radius, p);
        p.setAlpha(paintAlpha);
        return true;
    }

    public void getBounds(Rect bounds) {
        int outerX = (int) this.mOuterX;
        int outerY = (int) this.mOuterY;
        int r = ((int) this.mOuterRadius) + 1;
        bounds.set(outerX - r, outerY - r, outerX + r, outerY + r);
    }

    public void move(float x, float y) {
        this.mStartingX = x;
        this.mStartingY = y;
        clampStartingPosition();
    }

    public void enter() {
        cancel();
        int radiusDuration = (int) ((1000.0d * Math.sqrt((this.mOuterRadius / WAVE_TOUCH_DOWN_ACCELERATION) * this.mDensity)) + 0.5d);
        ObjectAnimator radius = ObjectAnimator.ofFloat(this, "radiusGravity", 1.0f);
        radius.setAutoCancel(true);
        radius.setDuration(radiusDuration);
        radius.setInterpolator(LINEAR_INTERPOLATOR);
        radius.setStartDelay(RIPPLE_ENTER_DELAY);
        ObjectAnimator cX = ObjectAnimator.ofFloat(this, "xGravity", 1.0f);
        cX.setAutoCancel(true);
        cX.setDuration(radiusDuration);
        cX.setInterpolator(LINEAR_INTERPOLATOR);
        cX.setStartDelay(RIPPLE_ENTER_DELAY);
        ObjectAnimator cY = ObjectAnimator.ofFloat(this, "yGravity", 1.0f);
        cY.setAutoCancel(true);
        cY.setDuration(radiusDuration);
        cY.setInterpolator(LINEAR_INTERPOLATOR);
        cY.setStartDelay(RIPPLE_ENTER_DELAY);
        this.mAnimRadius = radius;
        this.mAnimX = cX;
        this.mAnimY = cY;
        radius.start();
        cX.start();
        cY.start();
    }

    public void exit() {
        float remaining;
        float radius = MathUtils.lerp(0.0f, this.mOuterRadius, this.mTweenRadius);
        if (this.mAnimRadius != null && this.mAnimRadius.isRunning()) {
            remaining = this.mOuterRadius - radius;
        } else {
            remaining = this.mOuterRadius;
        }
        cancel();
        int radiusDuration = (int) ((1000.0d * Math.sqrt((remaining / 4424.0f) * this.mDensity)) + 0.5d);
        int opacityDuration = (int) (((1000.0f * this.mOpacity) / WAVE_OPACITY_DECAY_VELOCITY) + 0.5f);
        if (this.mCanUseHardware) {
            createPendingHardwareExit(radiusDuration, opacityDuration);
        } else {
            exitSoftware(radiusDuration, opacityDuration);
        }
    }

    private void createPendingHardwareExit(int radiusDuration, int opacityDuration) {
        this.mHasPendingHardwareExit = true;
        this.mPendingRadiusDuration = radiusDuration;
        this.mPendingOpacityDuration = opacityDuration;
        invalidateSelf();
    }

    private void startPendingHardwareExit(HardwareCanvas c, Paint p) {
        this.mHasPendingHardwareExit = false;
        int radiusDuration = this.mPendingRadiusDuration;
        int opacityDuration = this.mPendingOpacityDuration;
        float startX = MathUtils.lerp(this.mClampedStartingX - this.mBounds.exactCenterX(), this.mOuterX, this.mTweenX);
        float startY = MathUtils.lerp(this.mClampedStartingY - this.mBounds.exactCenterY(), this.mOuterY, this.mTweenY);
        float startRadius = MathUtils.lerp(0.0f, this.mOuterRadius, this.mTweenRadius);
        Paint paint = getTempPaint(p);
        paint.setAlpha((int) ((paint.getAlpha() * this.mOpacity) + 0.5f));
        this.mPropPaint = CanvasProperty.createPaint(paint);
        this.mPropRadius = CanvasProperty.createFloat(startRadius);
        this.mPropX = CanvasProperty.createFloat(startX);
        this.mPropY = CanvasProperty.createFloat(startY);
        RenderNodeAnimator radiusAnim = new RenderNodeAnimator(this.mPropRadius, this.mOuterRadius);
        radiusAnim.setDuration(radiusDuration);
        radiusAnim.setInterpolator(DECEL_INTERPOLATOR);
        radiusAnim.setTarget((Canvas) c);
        radiusAnim.start();
        RenderNodeAnimator xAnim = new RenderNodeAnimator(this.mPropX, this.mOuterX);
        xAnim.setDuration(radiusDuration);
        xAnim.setInterpolator(DECEL_INTERPOLATOR);
        xAnim.setTarget((Canvas) c);
        xAnim.start();
        RenderNodeAnimator yAnim = new RenderNodeAnimator(this.mPropY, this.mOuterY);
        yAnim.setDuration(radiusDuration);
        yAnim.setInterpolator(DECEL_INTERPOLATOR);
        yAnim.setTarget((Canvas) c);
        yAnim.start();
        RenderNodeAnimator opacityAnim = new RenderNodeAnimator(this.mPropPaint, 1, 0.0f);
        opacityAnim.setDuration(opacityDuration);
        opacityAnim.setInterpolator(LINEAR_INTERPOLATOR);
        opacityAnim.addListener(this.mAnimationListener);
        opacityAnim.setTarget((Canvas) c);
        opacityAnim.start();
        this.mRunningAnimations.add(radiusAnim);
        this.mRunningAnimations.add(opacityAnim);
        this.mRunningAnimations.add(xAnim);
        this.mRunningAnimations.add(yAnim);
        this.mHardwareAnimating = true;
        this.mOpacity = 0.0f;
        this.mTweenX = 1.0f;
        this.mTweenY = 1.0f;
        this.mTweenRadius = 1.0f;
    }

    public void jump() {
        this.mCanceled = true;
        endSoftwareAnimations();
        cancelHardwareAnimations(true);
        this.mCanceled = false;
    }

    private void endSoftwareAnimations() {
        if (this.mAnimRadius != null) {
            this.mAnimRadius.end();
            this.mAnimRadius = null;
        }
        if (this.mAnimOpacity != null) {
            this.mAnimOpacity.end();
            this.mAnimOpacity = null;
        }
        if (this.mAnimX != null) {
            this.mAnimX.end();
            this.mAnimX = null;
        }
        if (this.mAnimY != null) {
            this.mAnimY.end();
            this.mAnimY = null;
        }
    }

    private Paint getTempPaint(Paint original) {
        if (this.mTempPaint == null) {
            this.mTempPaint = new Paint();
        }
        this.mTempPaint.set(original);
        return this.mTempPaint;
    }

    private void exitSoftware(int radiusDuration, int opacityDuration) {
        ObjectAnimator radiusAnim = ObjectAnimator.ofFloat(this, "radiusGravity", 1.0f);
        radiusAnim.setAutoCancel(true);
        radiusAnim.setDuration(radiusDuration);
        radiusAnim.setInterpolator(DECEL_INTERPOLATOR);
        ObjectAnimator xAnim = ObjectAnimator.ofFloat(this, "xGravity", 1.0f);
        xAnim.setAutoCancel(true);
        xAnim.setDuration(radiusDuration);
        xAnim.setInterpolator(DECEL_INTERPOLATOR);
        ObjectAnimator yAnim = ObjectAnimator.ofFloat(this, "yGravity", 1.0f);
        yAnim.setAutoCancel(true);
        yAnim.setDuration(radiusDuration);
        yAnim.setInterpolator(DECEL_INTERPOLATOR);
        ObjectAnimator opacityAnim = ObjectAnimator.ofFloat(this, "opacity", 0.0f);
        opacityAnim.setAutoCancel(true);
        opacityAnim.setDuration(opacityDuration);
        opacityAnim.setInterpolator(LINEAR_INTERPOLATOR);
        opacityAnim.addListener(this.mAnimationListener);
        this.mAnimRadius = radiusAnim;
        this.mAnimOpacity = opacityAnim;
        this.mAnimX = xAnim;
        this.mAnimY = yAnim;
        radiusAnim.start();
        opacityAnim.start();
        xAnim.start();
        yAnim.start();
    }

    public void cancel() {
        this.mCanceled = true;
        cancelSoftwareAnimations();
        cancelHardwareAnimations(false);
        this.mCanceled = false;
    }

    private void cancelSoftwareAnimations() {
        if (this.mAnimRadius != null) {
            this.mAnimRadius.cancel();
            this.mAnimRadius = null;
        }
        if (this.mAnimOpacity != null) {
            this.mAnimOpacity.cancel();
            this.mAnimOpacity = null;
        }
        if (this.mAnimX != null) {
            this.mAnimX.cancel();
            this.mAnimX = null;
        }
        if (this.mAnimY != null) {
            this.mAnimY.cancel();
            this.mAnimY = null;
        }
    }

    private void cancelHardwareAnimations(boolean jumpToEnd) {
        ArrayList<RenderNodeAnimator> runningAnimations = this.mRunningAnimations;
        int N = runningAnimations.size();
        for (int i = 0; i < N; i++) {
            if (jumpToEnd) {
                runningAnimations.get(i).end();
            } else {
                runningAnimations.get(i).cancel();
            }
        }
        runningAnimations.clear();
        if (this.mHasPendingHardwareExit) {
            this.mHasPendingHardwareExit = false;
            if (jumpToEnd) {
                this.mOpacity = 0.0f;
                this.mTweenX = 1.0f;
                this.mTweenY = 1.0f;
                this.mTweenRadius = 1.0f;
            }
        }
        this.mHardwareAnimating = false;
    }

    private void removeSelf() {
        if (!this.mCanceled) {
            this.mOwner.removeRipple(this);
        }
    }

    private void invalidateSelf() {
        this.mOwner.invalidateSelf();
    }

    private static final class LogInterpolator implements TimeInterpolator {
        private LogInterpolator() {
        }

        @Override
        public float getInterpolation(float input) {
            return 1.0f - ((float) Math.pow(400.0d, ((double) (-input)) * 1.4d));
        }
    }
}
