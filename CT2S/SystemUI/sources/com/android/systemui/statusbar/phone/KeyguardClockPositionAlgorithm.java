package com.android.systemui.statusbar.phone;

import android.content.res.Resources;
import android.graphics.Path;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.PathInterpolator;
import com.android.systemui.R;

public class KeyguardClockPositionAlgorithm {
    private static final PathInterpolator sSlowDownInterpolator;
    private AccelerateInterpolator mAccelerateInterpolator = new AccelerateInterpolator();
    private int mClockNotificationsMarginMax;
    private int mClockNotificationsMarginMin;
    private float mClockYFractionMax;
    private float mClockYFractionMin;
    private float mDensity;
    private float mEmptyDragAmount;
    private float mExpandedHeight;
    private int mHeight;
    private int mKeyguardStatusHeight;
    private int mMaxKeyguardNotifications;
    private int mMaxPanelHeight;
    private float mMoreCardNotificationAmount;
    private int mNotificationCount;

    public static class Result {
        public float clockAlpha;
        public float clockScale;
        public int clockY;
        public int stackScrollerPadding;
        public int stackScrollerPaddingAdjustment;
    }

    static {
        Path path = new Path();
        path.moveTo(0.0f, 0.0f);
        path.cubicTo(0.3f, 0.875f, 0.6f, 1.0f, 1.0f, 1.0f);
        sSlowDownInterpolator = new PathInterpolator(path);
    }

    public void loadDimens(Resources res) {
        this.mClockNotificationsMarginMin = res.getDimensionPixelSize(R.dimen.keyguard_clock_notifications_margin_min);
        this.mClockNotificationsMarginMax = res.getDimensionPixelSize(R.dimen.keyguard_clock_notifications_margin_max);
        this.mClockYFractionMin = res.getFraction(R.fraction.keyguard_clock_y_fraction_min, 1, 1);
        this.mClockYFractionMax = res.getFraction(R.fraction.keyguard_clock_y_fraction_max, 1, 1);
        this.mMoreCardNotificationAmount = res.getDimensionPixelSize(R.dimen.notification_summary_height) / res.getDimensionPixelSize(R.dimen.notification_min_height);
        this.mDensity = res.getDisplayMetrics().density;
    }

    public void setup(int maxKeyguardNotifications, int maxPanelHeight, float expandedHeight, int notificationCount, int height, int keyguardStatusHeight, float emptyDragAmount) {
        this.mMaxKeyguardNotifications = maxKeyguardNotifications;
        this.mMaxPanelHeight = maxPanelHeight;
        this.mExpandedHeight = expandedHeight;
        this.mNotificationCount = notificationCount;
        this.mHeight = height;
        this.mKeyguardStatusHeight = keyguardStatusHeight;
        this.mEmptyDragAmount = emptyDragAmount;
    }

    public void run(Result result) {
        int y = getClockY() - (this.mKeyguardStatusHeight / 2);
        float clockAdjustment = getClockYExpansionAdjustment();
        float topPaddingAdjMultiplier = getTopPaddingAdjMultiplier();
        result.stackScrollerPaddingAdjustment = (int) (clockAdjustment * topPaddingAdjMultiplier);
        int clockNotificationsPadding = getClockNotificationsPadding() + result.stackScrollerPaddingAdjustment;
        int padding = y + clockNotificationsPadding;
        result.clockY = y;
        result.stackScrollerPadding = this.mKeyguardStatusHeight + padding;
        result.clockScale = getClockScale(result.stackScrollerPadding, result.clockY, getClockNotificationsPadding() + y + this.mKeyguardStatusHeight);
        result.clockAlpha = getClockAlpha(result.clockScale);
    }

    private float getClockScale(int notificationPadding, int clockY, int startPadding) {
        float scaleMultiplier = getNotificationAmountT() == 0.0f ? 6.0f : 5.0f;
        float scaleEnd = clockY - (this.mKeyguardStatusHeight * scaleMultiplier);
        float distanceToScaleEnd = notificationPadding - scaleEnd;
        float progress = distanceToScaleEnd / (startPadding - scaleEnd);
        return (float) (((double) this.mAccelerateInterpolator.getInterpolation(Math.max(0.0f, Math.min(progress, 1.0f)))) * Math.pow(((this.mEmptyDragAmount / this.mDensity) / 300.0f) + 1.0f, 0.30000001192092896d));
    }

    private int getClockNotificationsPadding() {
        float t = Math.min(getNotificationAmountT(), 1.0f);
        return (int) ((this.mClockNotificationsMarginMin * t) + ((1.0f - t) * this.mClockNotificationsMarginMax));
    }

    private float getClockYFraction() {
        float t = Math.min(getNotificationAmountT(), 1.0f);
        return ((1.0f - t) * this.mClockYFractionMax) + (this.mClockYFractionMin * t);
    }

    private int getClockY() {
        return (int) (getClockYFraction() * this.mHeight);
    }

    private float getClockYExpansionAdjustment() {
        float rubberbandFactor = getClockYExpansionRubberbandFactor();
        float value = rubberbandFactor * (this.mMaxPanelHeight - this.mExpandedHeight);
        float t = value / this.mMaxPanelHeight;
        float slowedDownValue = (-sSlowDownInterpolator.getInterpolation(t)) * 0.4f * this.mMaxPanelHeight;
        if (this.mNotificationCount == 0) {
            return (((-2.0f) * value) + slowedDownValue) / 3.0f;
        }
        return slowedDownValue;
    }

    private float getClockYExpansionRubberbandFactor() {
        float t = (float) Math.pow(Math.min(getNotificationAmountT(), 1.0f), 0.30000001192092896d);
        return ((1.0f - t) * 0.8f) + (0.08f * t);
    }

    private float getTopPaddingAdjMultiplier() {
        float t = Math.min(getNotificationAmountT(), 1.0f);
        return ((1.0f - t) * 1.4f) + (3.2f * t);
    }

    private float getClockAlpha(float scale) {
        float fadeEnd = getNotificationAmountT() == 0.0f ? 0.5f : 0.75f;
        float alpha = (scale - fadeEnd) / (0.95f - fadeEnd);
        return Math.max(0.0f, Math.min(1.0f, alpha));
    }

    private float getNotificationAmountT() {
        return this.mNotificationCount / (this.mMaxKeyguardNotifications + this.mMoreCardNotificationAmount);
    }
}
