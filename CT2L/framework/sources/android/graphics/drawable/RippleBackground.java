package android.graphics.drawable;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.graphics.Canvas;
import android.graphics.CanvasProperty;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.MathUtils;
import android.view.HardwareCanvas;
import android.view.RenderNodeAnimator;
import android.view.animation.LinearInterpolator;
import com.android.ims.ImsReasonInfo;
import java.util.ArrayList;

class RippleBackground {
    private static final int ENTER_DURATION = 667;
    private static final int ENTER_DURATION_FAST = 100;
    private static final float GLOBAL_SPEED = 1.0f;
    private static final TimeInterpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
    private static final float WAVE_OPACITY_DECAY_VELOCITY = 3.0f;
    private static final float WAVE_OUTER_OPACITY_EXIT_VELOCITY_MAX = 4.5f;
    private static final float WAVE_OUTER_OPACITY_EXIT_VELOCITY_MIN = 1.5f;
    private static final float WAVE_OUTER_SIZE_INFLUENCE_MAX = 200.0f;
    private static final float WAVE_OUTER_SIZE_INFLUENCE_MIN = 40.0f;
    private ObjectAnimator mAnimOuterOpacity;
    private final Rect mBounds;
    private boolean mCanUseHardware;
    private int mColor;
    private float mDensity;
    private boolean mHardwareAnimating;
    private boolean mHasMaxRadius;
    private boolean mHasPendingHardwareExit;
    private float mOuterRadius;
    private float mOuterX;
    private float mOuterY;
    private final RippleDrawable mOwner;
    private int mPendingInflectionDuration;
    private int mPendingInflectionOpacity;
    private int mPendingOpacityDuration;
    private CanvasProperty<Paint> mPropOuterPaint;
    private CanvasProperty<Float> mPropOuterRadius;
    private CanvasProperty<Float> mPropOuterX;
    private CanvasProperty<Float> mPropOuterY;
    private Paint mTempPaint;
    private final ArrayList<RenderNodeAnimator> mRunningAnimations = new ArrayList<>();
    private float mOuterOpacity = 0.0f;
    private final AnimatorListenerAdapter mAnimationListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            RippleBackground.this.mHardwareAnimating = false;
        }
    };

    public RippleBackground(RippleDrawable owner, Rect bounds) {
        this.mOwner = owner;
        this.mBounds = bounds;
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
    }

    public void onHotspotBoundsChanged() {
        if (!this.mHasMaxRadius) {
            float halfWidth = this.mBounds.width() / 2.0f;
            float halfHeight = this.mBounds.height() / 2.0f;
            this.mOuterRadius = (float) Math.sqrt((halfWidth * halfWidth) + (halfHeight * halfHeight));
        }
    }

    public void setOuterOpacity(float a) {
        this.mOuterOpacity = a;
        invalidateSelf();
    }

    public float getOuterOpacity() {
        return this.mOuterOpacity;
    }

    public boolean draw(Canvas c, Paint p) {
        this.mColor = p.getColor();
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

    public boolean shouldDraw() {
        return (this.mCanUseHardware && this.mHardwareAnimating) || (this.mOuterOpacity > 0.0f && this.mOuterRadius > 0.0f);
    }

    private boolean drawHardware(HardwareCanvas c, Paint p) {
        if (this.mHasPendingHardwareExit) {
            cancelHardwareAnimations(false);
            startPendingHardwareExit(c, p);
        }
        c.drawCircle(this.mPropOuterX, this.mPropOuterY, this.mPropOuterRadius, this.mPropOuterPaint);
        return true;
    }

    private boolean drawSoftware(Canvas c, Paint p) {
        int paintAlpha = p.getAlpha();
        int alpha = (int) ((paintAlpha * this.mOuterOpacity) + 0.5f);
        float radius = this.mOuterRadius;
        if (alpha <= 0 || radius <= 0.0f) {
            return false;
        }
        p.setAlpha(alpha);
        c.drawCircle(this.mOuterX, this.mOuterY, radius, p);
        p.setAlpha(paintAlpha);
        return true;
    }

    public void getBounds(Rect bounds) {
        int outerX = (int) this.mOuterX;
        int outerY = (int) this.mOuterY;
        int r = ((int) this.mOuterRadius) + 1;
        bounds.set(outerX - r, outerY - r, outerX + r, outerY + r);
    }

    public void enter(boolean fast) {
        cancel();
        ObjectAnimator opacity = ObjectAnimator.ofFloat(this, "outerOpacity", 0.0f, 1.0f);
        opacity.setAutoCancel(true);
        opacity.setDuration(fast ? 100L : 667L);
        opacity.setInterpolator(LINEAR_INTERPOLATOR);
        this.mAnimOuterOpacity = opacity;
        opacity.start();
    }

    public void exit() {
        cancel();
        float outerSizeInfluence = MathUtils.constrain((this.mOuterRadius - (WAVE_OUTER_SIZE_INFLUENCE_MIN * this.mDensity)) / (WAVE_OUTER_SIZE_INFLUENCE_MAX * this.mDensity), 0.0f, 1.0f);
        float outerOpacityVelocity = MathUtils.lerp(WAVE_OUTER_OPACITY_EXIT_VELOCITY_MIN, WAVE_OUTER_OPACITY_EXIT_VELOCITY_MAX, outerSizeInfluence);
        int inflectionDuration = Math.max(0, (int) ((((1.0f - this.mOuterOpacity) * 1000.0f) / (WAVE_OPACITY_DECAY_VELOCITY + outerOpacityVelocity)) + 0.5f));
        int inflectionOpacity = (int) ((Color.alpha(this.mColor) * (this.mOuterOpacity + (((inflectionDuration * outerOpacityVelocity) * outerSizeInfluence) / 1000.0f))) + 0.5f);
        if (this.mCanUseHardware) {
            createPendingHardwareExit(ImsReasonInfo.CODE_SIP_NOT_FOUND, inflectionDuration, inflectionOpacity);
        } else {
            exitSoftware(ImsReasonInfo.CODE_SIP_NOT_FOUND, inflectionDuration, inflectionOpacity);
        }
    }

    private void createPendingHardwareExit(int opacityDuration, int inflectionDuration, int inflectionOpacity) {
        this.mHasPendingHardwareExit = true;
        this.mPendingOpacityDuration = opacityDuration;
        this.mPendingInflectionDuration = inflectionDuration;
        this.mPendingInflectionOpacity = inflectionOpacity;
        invalidateSelf();
    }

    private void startPendingHardwareExit(HardwareCanvas c, Paint p) {
        RenderNodeAnimator outerOpacityAnim;
        this.mHasPendingHardwareExit = false;
        int opacityDuration = this.mPendingOpacityDuration;
        int inflectionDuration = this.mPendingInflectionDuration;
        int inflectionOpacity = this.mPendingInflectionOpacity;
        Paint outerPaint = getTempPaint(p);
        outerPaint.setAlpha((int) ((outerPaint.getAlpha() * this.mOuterOpacity) + 0.5f));
        this.mPropOuterPaint = CanvasProperty.createPaint(outerPaint);
        this.mPropOuterRadius = CanvasProperty.createFloat(this.mOuterRadius);
        this.mPropOuterX = CanvasProperty.createFloat(this.mOuterX);
        this.mPropOuterY = CanvasProperty.createFloat(this.mOuterY);
        if (inflectionDuration > 0) {
            outerOpacityAnim = new RenderNodeAnimator(this.mPropOuterPaint, 1, inflectionOpacity);
            outerOpacityAnim.setDuration(inflectionDuration);
            outerOpacityAnim.setInterpolator(LINEAR_INTERPOLATOR);
            int outerDuration = opacityDuration - inflectionDuration;
            if (outerDuration > 0) {
                RenderNodeAnimator outerFadeOutAnim = new RenderNodeAnimator(this.mPropOuterPaint, 1, 0.0f);
                outerFadeOutAnim.setDuration(outerDuration);
                outerFadeOutAnim.setInterpolator(LINEAR_INTERPOLATOR);
                outerFadeOutAnim.setStartDelay(inflectionDuration);
                outerFadeOutAnim.setStartValue(inflectionOpacity);
                outerFadeOutAnim.addListener(this.mAnimationListener);
                outerFadeOutAnim.setTarget((Canvas) c);
                outerFadeOutAnim.start();
                this.mRunningAnimations.add(outerFadeOutAnim);
            } else {
                outerOpacityAnim.addListener(this.mAnimationListener);
            }
        } else {
            outerOpacityAnim = new RenderNodeAnimator(this.mPropOuterPaint, 1, 0.0f);
            outerOpacityAnim.setInterpolator(LINEAR_INTERPOLATOR);
            outerOpacityAnim.setDuration(opacityDuration);
            outerOpacityAnim.addListener(this.mAnimationListener);
        }
        outerOpacityAnim.setTarget((Canvas) c);
        outerOpacityAnim.start();
        this.mRunningAnimations.add(outerOpacityAnim);
        this.mHardwareAnimating = true;
        this.mOuterOpacity = 0.0f;
    }

    public void jump() {
        endSoftwareAnimations();
        cancelHardwareAnimations(true);
    }

    private void endSoftwareAnimations() {
        if (this.mAnimOuterOpacity != null) {
            this.mAnimOuterOpacity.end();
            this.mAnimOuterOpacity = null;
        }
    }

    private Paint getTempPaint(Paint original) {
        if (this.mTempPaint == null) {
            this.mTempPaint = new Paint();
        }
        this.mTempPaint.set(original);
        return this.mTempPaint;
    }

    private void exitSoftware(int opacityDuration, int inflectionDuration, int inflectionOpacity) {
        ObjectAnimator outerOpacityAnim;
        if (inflectionDuration > 0) {
            outerOpacityAnim = ObjectAnimator.ofFloat(this, "outerOpacity", inflectionOpacity / 255.0f);
            outerOpacityAnim.setAutoCancel(true);
            outerOpacityAnim.setDuration(inflectionDuration);
            outerOpacityAnim.setInterpolator(LINEAR_INTERPOLATOR);
            final int outerDuration = opacityDuration - inflectionDuration;
            if (outerDuration > 0) {
                outerOpacityAnim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        ObjectAnimator outerFadeOutAnim = ObjectAnimator.ofFloat(RippleBackground.this, "outerOpacity", 0.0f);
                        outerFadeOutAnim.setAutoCancel(true);
                        outerFadeOutAnim.setDuration(outerDuration);
                        outerFadeOutAnim.setInterpolator(RippleBackground.LINEAR_INTERPOLATOR);
                        outerFadeOutAnim.addListener(RippleBackground.this.mAnimationListener);
                        RippleBackground.this.mAnimOuterOpacity = outerFadeOutAnim;
                        outerFadeOutAnim.start();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        animation.removeListener(this);
                    }
                });
            } else {
                outerOpacityAnim.addListener(this.mAnimationListener);
            }
        } else {
            outerOpacityAnim = ObjectAnimator.ofFloat(this, "outerOpacity", 0.0f);
            outerOpacityAnim.setAutoCancel(true);
            outerOpacityAnim.setDuration(opacityDuration);
            outerOpacityAnim.addListener(this.mAnimationListener);
        }
        this.mAnimOuterOpacity = outerOpacityAnim;
        outerOpacityAnim.start();
    }

    public void cancel() {
        cancelSoftwareAnimations();
        cancelHardwareAnimations(false);
    }

    private void cancelSoftwareAnimations() {
        if (this.mAnimOuterOpacity != null) {
            this.mAnimOuterOpacity.cancel();
            this.mAnimOuterOpacity = null;
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
                this.mOuterOpacity = 0.0f;
            }
        }
        this.mHardwareAnimating = false;
    }

    private void invalidateSelf() {
        this.mOwner.invalidateSelf();
    }
}
