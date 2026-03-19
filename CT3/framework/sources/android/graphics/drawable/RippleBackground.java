package android.graphics.drawable;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.graphics.Canvas;
import android.graphics.CanvasProperty;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.RippleComponent;
import android.util.FloatProperty;
import android.view.DisplayListCanvas;
import android.view.RenderNodeAnimator;
import android.view.animation.LinearInterpolator;

class RippleBackground extends RippleComponent {
    private static final TimeInterpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
    private static final BackgroundProperty OPACITY = new BackgroundProperty("opacity") {
        @Override
        public void setValue(RippleBackground object, float value) {
            object.mOpacity = value;
            object.invalidateSelf();
        }

        @Override
        public Float get(RippleBackground object) {
            return Float.valueOf(object.mOpacity);
        }
    };
    private static final int OPACITY_ENTER_DURATION = 600;
    private static final int OPACITY_ENTER_DURATION_FAST = 120;
    private static final int OPACITY_EXIT_DURATION = 480;
    private boolean mIsBounded;
    private float mOpacity;
    private CanvasProperty<Paint> mPropPaint;
    private CanvasProperty<Float> mPropRadius;
    private CanvasProperty<Float> mPropX;
    private CanvasProperty<Float> mPropY;

    public RippleBackground(RippleDrawable owner, Rect bounds, boolean isBounded, boolean forceSoftware) {
        super(owner, bounds, forceSoftware);
        this.mOpacity = 0.0f;
        this.mIsBounded = isBounded;
    }

    public boolean isVisible() {
        if (this.mOpacity <= 0.0f) {
            return isHardwareAnimating();
        }
        return true;
    }

    @Override
    protected boolean drawSoftware(Canvas c, Paint p) {
        int origAlpha = p.getAlpha();
        int alpha = (int) ((origAlpha * this.mOpacity) + 0.5f);
        if (alpha <= 0) {
            return false;
        }
        p.setAlpha(alpha);
        c.drawCircle(0.0f, 0.0f, this.mTargetRadius, p);
        p.setAlpha(origAlpha);
        return true;
    }

    @Override
    protected boolean drawHardware(DisplayListCanvas c) {
        c.drawCircle(this.mPropX, this.mPropY, this.mPropRadius, this.mPropPaint);
        return true;
    }

    @Override
    protected Animator createSoftwareEnter(boolean fast) {
        int maxDuration = fast ? 120 : 600;
        int duration = (int) ((1.0f - this.mOpacity) * maxDuration);
        ObjectAnimator opacity = ObjectAnimator.ofFloat(this, OPACITY, 1.0f);
        opacity.setAutoCancel(true);
        opacity.setDuration(duration);
        opacity.setInterpolator(LINEAR_INTERPOLATOR);
        return opacity;
    }

    @Override
    protected Animator createSoftwareExit() {
        AnimatorSet set = new AnimatorSet();
        ObjectAnimator exit = ObjectAnimator.ofFloat(this, OPACITY, 0.0f);
        exit.setInterpolator(LINEAR_INTERPOLATOR);
        exit.setDuration(480L);
        exit.setAutoCancel(true);
        AnimatorSet.Builder builder = set.play(exit);
        int fastEnterDuration = this.mIsBounded ? (int) ((1.0f - this.mOpacity) * 120.0f) : 0;
        if (fastEnterDuration > 0) {
            ObjectAnimator enter = ObjectAnimator.ofFloat(this, OPACITY, 1.0f);
            enter.setInterpolator(LINEAR_INTERPOLATOR);
            enter.setDuration(fastEnterDuration);
            enter.setAutoCancel(true);
            builder.after(enter);
        }
        return set;
    }

    @Override
    protected RippleComponent.RenderNodeAnimatorSet createHardwareExit(Paint p) {
        RippleComponent.RenderNodeAnimatorSet set = new RippleComponent.RenderNodeAnimatorSet();
        int targetAlpha = p.getAlpha();
        int currentAlpha = (int) ((this.mOpacity * targetAlpha) + 0.5f);
        p.setAlpha(currentAlpha);
        this.mPropPaint = CanvasProperty.createPaint(p);
        this.mPropRadius = CanvasProperty.createFloat(this.mTargetRadius);
        this.mPropX = CanvasProperty.createFloat(0.0f);
        this.mPropY = CanvasProperty.createFloat(0.0f);
        int fastEnterDuration = this.mIsBounded ? (int) ((1.0f - this.mOpacity) * 120.0f) : 0;
        RenderNodeAnimator exit = new RenderNodeAnimator(this.mPropPaint, 1, 0.0f);
        exit.setInterpolator(LINEAR_INTERPOLATOR);
        exit.setDuration(480L);
        if (fastEnterDuration > 0) {
            exit.setStartDelay(fastEnterDuration);
            exit.setStartValue(targetAlpha);
        }
        set.add(exit);
        if (fastEnterDuration > 0) {
            RenderNodeAnimator enter = new RenderNodeAnimator(this.mPropPaint, 1, targetAlpha);
            enter.setInterpolator(LINEAR_INTERPOLATOR);
            enter.setDuration(fastEnterDuration);
            set.add(enter);
        }
        return set;
    }

    @Override
    protected void jumpValuesToExit() {
        this.mOpacity = 0.0f;
    }

    private static abstract class BackgroundProperty extends FloatProperty<RippleBackground> {
        public BackgroundProperty(String name) {
            super(name);
        }
    }
}
