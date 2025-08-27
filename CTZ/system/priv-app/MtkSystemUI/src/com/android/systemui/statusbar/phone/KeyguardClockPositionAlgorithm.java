package com.android.systemui.statusbar.phone;

import android.content.res.Resources;
import android.util.MathUtils;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.statusbar.notification.NotificationUtils;

/* loaded from: classes.dex */
public class KeyguardClockPositionAlgorithm {
    private static float CLOCK_HEIGHT_WEIGHT = 0.7f;
    private int mBouncerTop;
    private int mBurnInPreventionOffsetX;
    private int mBurnInPreventionOffsetY;
    private int mClockNotificationsMargin;
    private int mContainerTopPadding;
    private boolean mCurrentlySecure;
    private float mDarkAmount;
    private int mHeight;
    private int mKeyguardStatusHeight;
    private int mMaxShadeBottom;
    private int mMinTopMargin;
    private int mNotificationStackHeight;
    private float mPanelExpansion;
    private boolean mPulsing;
    private int mPulsingPadding;

    public static class Result {
        public float clockAlpha;
        public int clockX;
        public int clockY;
        public int stackScrollerPadding;
    }

    public void loadDimens(Resources resources) {
        this.mClockNotificationsMargin = resources.getDimensionPixelSize(R.dimen.keyguard_clock_notifications_margin);
        this.mContainerTopPadding = resources.getDimensionPixelSize(R.dimen.keyguard_clock_top_margin);
        this.mBurnInPreventionOffsetX = resources.getDimensionPixelSize(R.dimen.burn_in_prevention_offset_x);
        this.mBurnInPreventionOffsetY = resources.getDimensionPixelSize(R.dimen.burn_in_prevention_offset_y);
        this.mPulsingPadding = resources.getDimensionPixelSize(R.dimen.widget_pulsing_bottom_padding);
    }

    public void setup(int i, int i2, int i3, float f, int i4, int i5, float f2, boolean z, boolean z2, int i6) {
        this.mMinTopMargin = i + this.mContainerTopPadding;
        this.mMaxShadeBottom = i2;
        this.mNotificationStackHeight = i3;
        this.mPanelExpansion = f;
        this.mHeight = i4;
        this.mKeyguardStatusHeight = i5;
        this.mDarkAmount = f2;
        this.mCurrentlySecure = z;
        this.mPulsing = z2;
        this.mBouncerTop = i6;
    }

    public void run(Result result) {
        int clockY = getClockY();
        result.clockY = clockY;
        result.clockAlpha = getClockAlpha(clockY);
        result.stackScrollerPadding = clockY + (this.mPulsing ? 0 : this.mKeyguardStatusHeight);
        result.clockX = (int) NotificationUtils.interpolate(0.0f, burnInPreventionOffsetX(), this.mDarkAmount);
    }

    public float getMinStackScrollerPadding() {
        return this.mMinTopMargin + this.mKeyguardStatusHeight + this.mClockNotificationsMargin;
    }

    private int getMaxClockY() {
        return ((this.mHeight / 2) - this.mKeyguardStatusHeight) - this.mClockNotificationsMargin;
    }

    public int getExpandedClockPosition() {
        float f = (((this.mMinTopMargin + ((this.mMaxShadeBottom - this.mMinTopMargin) / 2)) - (this.mKeyguardStatusHeight * CLOCK_HEIGHT_WEIGHT)) - this.mClockNotificationsMargin) - (this.mNotificationStackHeight / 2);
        if (f < this.mMinTopMargin) {
            f = this.mMinTopMargin;
        }
        float maxClockY = getMaxClockY();
        if (f > maxClockY) {
            f = maxClockY;
        }
        return (int) f;
    }

    private int getClockY() {
        float maxClockY = getMaxClockY() + burnInPreventionOffsetY();
        if (this.mPulsing) {
            maxClockY -= this.mPulsingPadding;
        }
        return (int) MathUtils.lerp(MathUtils.lerp((this.mCurrentlySecure && (this.mMinTopMargin + this.mKeyguardStatusHeight < this.mBouncerTop)) ? this.mMinTopMargin : -this.mKeyguardStatusHeight, getExpandedClockPosition(), Interpolators.FAST_OUT_LINEAR_IN.getInterpolation(this.mPanelExpansion)), maxClockY, this.mDarkAmount);
    }

    private float getClockAlpha(int i) {
        float interpolation;
        if (!this.mCurrentlySecure) {
            interpolation = Interpolators.ACCELERATE.getInterpolation(Math.max(0.0f, i / Math.max(1.0f, getExpandedClockPosition())));
        } else {
            interpolation = 1.0f;
        }
        return MathUtils.lerp(interpolation, 1.0f, this.mDarkAmount);
    }

    private float burnInPreventionOffsetY() {
        return zigzag(System.currentTimeMillis() / 60000, this.mBurnInPreventionOffsetY * 2, 521.0f) - this.mBurnInPreventionOffsetY;
    }

    private float burnInPreventionOffsetX() {
        return zigzag(System.currentTimeMillis() / 60000, this.mBurnInPreventionOffsetX * 2, 83.0f) - this.mBurnInPreventionOffsetX;
    }

    private float zigzag(float f, float f2, float f3) {
        float f4 = (f % f3) / (f3 / 2.0f);
        if (f4 > 1.0f) {
            f4 = 2.0f - f4;
        }
        return NotificationUtils.interpolate(0.0f, f2, f4);
    }
}
