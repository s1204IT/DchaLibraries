package com.android.systemui.statusbar.notification;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.service.notification.StatusBarNotification;
import android.support.v4.graphics.ColorUtils;
import android.view.View;
import com.android.systemui.R;
import com.android.systemui.ViewInvertHelper;
import com.android.systemui.statusbar.ExpandableNotificationRow;

public class NotificationCustomViewWrapper extends NotificationViewWrapper {
    private int mBackgroundColor;
    private final Paint mGreyPaint;
    private final ViewInvertHelper mInvertHelper;
    private boolean mShouldInvertDark;
    private boolean mShowingLegacyBackground;

    protected NotificationCustomViewWrapper(View view, ExpandableNotificationRow row) {
        super(view, row);
        this.mGreyPaint = new Paint();
        this.mBackgroundColor = 0;
        this.mInvertHelper = new ViewInvertHelper(view, 700L);
    }

    @Override
    public void setDark(boolean dark, boolean fade, long delay) {
        if (dark == this.mDark && this.mDarkInitialized) {
            return;
        }
        super.setDark(dark, fade, delay);
        if (!this.mShowingLegacyBackground && this.mShouldInvertDark) {
            if (fade) {
                this.mInvertHelper.fade(dark, delay);
                return;
            } else {
                this.mInvertHelper.update(dark);
                return;
            }
        }
        this.mView.setLayerType(dark ? 2 : 0, null);
        if (fade) {
            fadeGrayscale(dark, delay);
        } else {
            updateGrayscale(dark);
        }
    }

    protected void fadeGrayscale(final boolean dark, long delay) {
        startIntensityAnimation(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                NotificationCustomViewWrapper.this.updateGrayscaleMatrix(((Float) animation.getAnimatedValue()).floatValue());
                NotificationCustomViewWrapper.this.mGreyPaint.setColorFilter(new ColorMatrixColorFilter(NotificationCustomViewWrapper.this.mGrayscaleColorMatrix));
                NotificationCustomViewWrapper.this.mView.setLayerPaint(NotificationCustomViewWrapper.this.mGreyPaint);
            }
        }, dark, delay, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (dark) {
                    return;
                }
                NotificationCustomViewWrapper.this.mView.setLayerType(0, null);
            }
        });
    }

    protected void updateGrayscale(boolean dark) {
        if (!dark) {
            return;
        }
        updateGrayscaleMatrix(1.0f);
        this.mGreyPaint.setColorFilter(new ColorMatrixColorFilter(this.mGrayscaleColorMatrix));
        this.mView.setLayerPaint(this.mGreyPaint);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        this.mView.setAlpha(visible ? 1.0f : 0.0f);
    }

    @Override
    public void notifyContentUpdated(StatusBarNotification notification) {
        super.notifyContentUpdated(notification);
        Drawable background = this.mView.getBackground();
        this.mBackgroundColor = 0;
        if (background instanceof ColorDrawable) {
            this.mBackgroundColor = ((ColorDrawable) background).getColor();
            this.mView.setBackground(null);
            this.mView.setTag(R.id.custom_background_color, Integer.valueOf(this.mBackgroundColor));
        } else if (this.mView.getTag(R.id.custom_background_color) != null) {
            this.mBackgroundColor = ((Integer) this.mView.getTag(R.id.custom_background_color)).intValue();
        }
        this.mShouldInvertDark = this.mBackgroundColor != 0 ? isColorLight(this.mBackgroundColor) : true;
    }

    private boolean isColorLight(int backgroundColor) {
        return Color.alpha(backgroundColor) == 0 || ColorUtils.calculateLuminance(backgroundColor) > 0.5d;
    }

    @Override
    public int getCustomBackgroundColor() {
        if (this.mRow.isSummaryWithChildren()) {
            return 0;
        }
        return this.mBackgroundColor;
    }

    @Override
    public void setShowingLegacyBackground(boolean showing) {
        super.setShowingLegacyBackground(showing);
        this.mShowingLegacyBackground = showing;
    }
}
