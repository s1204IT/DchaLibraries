package com.android.systemui.statusbar.policy;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.CanvasProperty;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.HardwareCanvas;
import android.view.RenderNodeAnimator;
import android.view.View;
import android.view.animation.Interpolator;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import java.util.ArrayList;
import java.util.HashSet;

public class KeyButtonRipple extends Drawable {
    private CanvasProperty<Float> mBottomProp;
    private boolean mDrawingHardwareGlow;
    private CanvasProperty<Float> mLeftProp;
    private int mMaxWidth;
    private CanvasProperty<Paint> mPaintProp;
    private boolean mPressed;
    private CanvasProperty<Float> mRightProp;
    private Paint mRipplePaint;
    private CanvasProperty<Float> mRxProp;
    private CanvasProperty<Float> mRyProp;
    private boolean mSupportHardware;
    private final View mTargetView;
    private CanvasProperty<Float> mTopProp;
    private float mGlowAlpha = 0.0f;
    private float mGlowScale = 1.0f;
    private final Interpolator mInterpolator = new LogInterpolator();
    private final Interpolator mAlphaExitInterpolator = PhoneStatusBar.ALPHA_OUT;
    private final HashSet<Animator> mRunningAnimations = new HashSet<>();
    private final ArrayList<Animator> mTmpArray = new ArrayList<>();
    private final AnimatorListenerAdapter mAnimatorListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            KeyButtonRipple.this.mRunningAnimations.remove(animation);
            if (KeyButtonRipple.this.mRunningAnimations.isEmpty() && !KeyButtonRipple.this.mPressed) {
                KeyButtonRipple.this.mDrawingHardwareGlow = false;
                KeyButtonRipple.this.invalidateSelf();
            }
        }
    };

    public KeyButtonRipple(Context ctx, View targetView) {
        this.mMaxWidth = ctx.getResources().getDimensionPixelSize(R.dimen.key_button_ripple_max_width);
        this.mTargetView = targetView;
    }

    private Paint getRipplePaint() {
        if (this.mRipplePaint == null) {
            this.mRipplePaint = new Paint();
            this.mRipplePaint.setAntiAlias(true);
            this.mRipplePaint.setColor(-1);
        }
        return this.mRipplePaint;
    }

    private void drawSoftware(Canvas canvas) {
        if (this.mGlowAlpha > 0.0f) {
            Paint p = getRipplePaint();
            p.setAlpha((int) (this.mGlowAlpha * 255.0f));
            float w = getBounds().width();
            float h = getBounds().height();
            boolean horizontal = w > h;
            float diameter = getRippleSize() * this.mGlowScale;
            float radius = diameter * 0.5f;
            float cx = w * 0.5f;
            float cy = h * 0.5f;
            float rx = horizontal ? radius : cx;
            float ry = horizontal ? cy : radius;
            float corner = horizontal ? cy : cx;
            canvas.drawRoundRect(cx - rx, cy - ry, cx + rx, cy + ry, corner, corner, p);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        this.mSupportHardware = canvas.isHardwareAccelerated();
        if (this.mSupportHardware) {
            drawHardware((HardwareCanvas) canvas);
        } else {
            drawSoftware(canvas);
        }
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }

    @Override
    public int getOpacity() {
        return -3;
    }

    private boolean isHorizontal() {
        return getBounds().width() > getBounds().height();
    }

    private void drawHardware(HardwareCanvas c) {
        if (this.mDrawingHardwareGlow) {
            c.drawRoundRect(this.mLeftProp, this.mTopProp, this.mRightProp, this.mBottomProp, this.mRxProp, this.mRyProp, this.mPaintProp);
        }
    }

    public float getGlowAlpha() {
        return this.mGlowAlpha;
    }

    public void setGlowAlpha(float x) {
        this.mGlowAlpha = x;
        invalidateSelf();
    }

    public float getGlowScale() {
        return this.mGlowScale;
    }

    public void setGlowScale(float x) {
        this.mGlowScale = x;
        invalidateSelf();
    }

    @Override
    protected boolean onStateChange(int[] state) {
        boolean pressed = false;
        int i = 0;
        while (true) {
            if (i >= state.length) {
                break;
            }
            if (state[i] != 16842919) {
                i++;
            } else {
                pressed = true;
                break;
            }
        }
        if (pressed != this.mPressed) {
            setPressed(pressed);
            this.mPressed = pressed;
            return true;
        }
        return false;
    }

    @Override
    public void jumpToCurrentState() {
        cancelAnimations();
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    public void setPressed(boolean pressed) {
        if (this.mSupportHardware) {
            setPressedHardware(pressed);
        } else {
            setPressedSoftware(pressed);
        }
    }

    private void cancelAnimations() {
        this.mTmpArray.addAll(this.mRunningAnimations);
        int size = this.mTmpArray.size();
        for (int i = 0; i < size; i++) {
            Animator a = this.mTmpArray.get(i);
            a.cancel();
        }
        this.mTmpArray.clear();
        this.mRunningAnimations.clear();
    }

    private void setPressedSoftware(boolean pressed) {
        if (pressed) {
            enterSoftware();
        } else {
            exitSoftware();
        }
    }

    private void enterSoftware() {
        cancelAnimations();
        this.mGlowAlpha = 0.2f;
        ObjectAnimator scaleAnimator = ObjectAnimator.ofFloat(this, "glowScale", 0.0f, 1.35f);
        scaleAnimator.setInterpolator(this.mInterpolator);
        scaleAnimator.setDuration(350L);
        scaleAnimator.addListener(this.mAnimatorListener);
        scaleAnimator.start();
        this.mRunningAnimations.add(scaleAnimator);
    }

    private void exitSoftware() {
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(this, "glowAlpha", this.mGlowAlpha, 0.0f);
        alphaAnimator.setInterpolator(this.mAlphaExitInterpolator);
        alphaAnimator.setDuration(450L);
        alphaAnimator.addListener(this.mAnimatorListener);
        alphaAnimator.start();
        this.mRunningAnimations.add(alphaAnimator);
    }

    private void setPressedHardware(boolean pressed) {
        if (pressed) {
            enterHardware();
        } else {
            exitHardware();
        }
    }

    private void setExtendStart(CanvasProperty<Float> prop) {
        if (isHorizontal()) {
            this.mLeftProp = prop;
        } else {
            this.mTopProp = prop;
        }
    }

    private CanvasProperty<Float> getExtendStart() {
        return isHorizontal() ? this.mLeftProp : this.mTopProp;
    }

    private void setExtendEnd(CanvasProperty<Float> prop) {
        if (isHorizontal()) {
            this.mRightProp = prop;
        } else {
            this.mBottomProp = prop;
        }
    }

    private CanvasProperty<Float> getExtendEnd() {
        return isHorizontal() ? this.mRightProp : this.mBottomProp;
    }

    private int getExtendSize() {
        return isHorizontal() ? getBounds().width() : getBounds().height();
    }

    private int getRippleSize() {
        int size = isHorizontal() ? getBounds().width() : getBounds().height();
        return Math.min(size, this.mMaxWidth);
    }

    private void enterHardware() {
        cancelAnimations();
        this.mDrawingHardwareGlow = true;
        setExtendStart(CanvasProperty.createFloat(getExtendSize() / 2));
        Animator renderNodeAnimator = new RenderNodeAnimator(getExtendStart(), (getExtendSize() / 2) - ((getRippleSize() * 1.35f) / 2.0f));
        renderNodeAnimator.setDuration(350L);
        renderNodeAnimator.setInterpolator(this.mInterpolator);
        renderNodeAnimator.addListener(this.mAnimatorListener);
        renderNodeAnimator.setTarget(this.mTargetView);
        setExtendEnd(CanvasProperty.createFloat(getExtendSize() / 2));
        Animator renderNodeAnimator2 = new RenderNodeAnimator(getExtendEnd(), (getExtendSize() / 2) + ((getRippleSize() * 1.35f) / 2.0f));
        renderNodeAnimator2.setDuration(350L);
        renderNodeAnimator2.setInterpolator(this.mInterpolator);
        renderNodeAnimator2.addListener(this.mAnimatorListener);
        renderNodeAnimator2.setTarget(this.mTargetView);
        if (isHorizontal()) {
            this.mTopProp = CanvasProperty.createFloat(0.0f);
            this.mBottomProp = CanvasProperty.createFloat(getBounds().height());
            this.mRxProp = CanvasProperty.createFloat(getBounds().height() / 2);
            this.mRyProp = CanvasProperty.createFloat(getBounds().height() / 2);
        } else {
            this.mLeftProp = CanvasProperty.createFloat(0.0f);
            this.mRightProp = CanvasProperty.createFloat(getBounds().width());
            this.mRxProp = CanvasProperty.createFloat(getBounds().width() / 2);
            this.mRyProp = CanvasProperty.createFloat(getBounds().width() / 2);
        }
        this.mGlowScale = 1.35f;
        this.mGlowAlpha = 0.2f;
        this.mRipplePaint = getRipplePaint();
        this.mRipplePaint.setAlpha((int) (this.mGlowAlpha * 255.0f));
        this.mPaintProp = CanvasProperty.createPaint(this.mRipplePaint);
        renderNodeAnimator.start();
        renderNodeAnimator2.start();
        this.mRunningAnimations.add(renderNodeAnimator);
        this.mRunningAnimations.add(renderNodeAnimator2);
        invalidateSelf();
    }

    private void exitHardware() {
        this.mPaintProp = CanvasProperty.createPaint(getRipplePaint());
        Animator renderNodeAnimator = new RenderNodeAnimator(this.mPaintProp, 1, 0.0f);
        renderNodeAnimator.setDuration(450L);
        renderNodeAnimator.setInterpolator(this.mAlphaExitInterpolator);
        renderNodeAnimator.addListener(this.mAnimatorListener);
        renderNodeAnimator.setTarget(this.mTargetView);
        renderNodeAnimator.start();
        this.mRunningAnimations.add(renderNodeAnimator);
        invalidateSelf();
    }

    private static final class LogInterpolator implements Interpolator {
        private LogInterpolator() {
        }

        @Override
        public float getInterpolation(float input) {
            return 1.0f - ((float) Math.pow(400.0d, ((double) (-input)) * 1.4d));
        }
    }
}
