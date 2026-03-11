package com.android.systemui.statusbar.notification;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.ColorMatrix;
import android.service.notification.StatusBarNotification;
import android.view.NotificationHeaderView;
import android.view.View;
import com.android.systemui.Interpolators;
import com.android.systemui.statusbar.CrossFadeHelper;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.TransformableView;

public abstract class NotificationViewWrapper implements TransformableView {
    protected boolean mDark;
    protected final ExpandableNotificationRow mRow;
    protected final View mView;
    protected final ColorMatrix mGrayscaleColorMatrix = new ColorMatrix();
    protected boolean mDarkInitialized = false;

    public static NotificationViewWrapper wrap(Context ctx, View v, ExpandableNotificationRow row) {
        if (v.getId() == 16909232) {
            if ("bigPicture".equals(v.getTag())) {
                return new NotificationBigPictureTemplateViewWrapper(ctx, v, row);
            }
            if ("bigText".equals(v.getTag())) {
                return new NotificationBigTextTemplateViewWrapper(ctx, v, row);
            }
            if ("media".equals(v.getTag()) || "bigMediaNarrow".equals(v.getTag())) {
                return new NotificationMediaTemplateViewWrapper(ctx, v, row);
            }
            if ("messaging".equals(v.getTag())) {
                return new NotificationMessagingTemplateViewWrapper(ctx, v, row);
            }
            return new NotificationTemplateViewWrapper(ctx, v, row);
        }
        if (v instanceof NotificationHeaderView) {
            return new NotificationHeaderViewWrapper(ctx, v, row);
        }
        return new NotificationCustomViewWrapper(v, row);
    }

    protected NotificationViewWrapper(View view, ExpandableNotificationRow row) {
        this.mView = view;
        this.mRow = row;
    }

    public void setDark(boolean dark, boolean fade, long delay) {
        this.mDark = dark;
        this.mDarkInitialized = true;
    }

    public void notifyContentUpdated(StatusBarNotification notification) {
        this.mDarkInitialized = false;
    }

    protected void startIntensityAnimation(ValueAnimator.AnimatorUpdateListener updateListener, boolean dark, long delay, Animator.AnimatorListener listener) {
        float startIntensity = dark ? 0.0f : 1.0f;
        float endIntensity = dark ? 1.0f : 0.0f;
        ValueAnimator animator = ValueAnimator.ofFloat(startIntensity, endIntensity);
        animator.addUpdateListener(updateListener);
        animator.setDuration(700L);
        animator.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
        animator.setStartDelay(delay);
        if (listener != null) {
            animator.addListener(listener);
        }
        animator.start();
    }

    protected void updateGrayscaleMatrix(float intensity) {
        this.mGrayscaleColorMatrix.setSaturation(1.0f - intensity);
    }

    public void updateExpandability(boolean expandable, View.OnClickListener onClickListener) {
    }

    public NotificationHeaderView getNotificationHeader() {
        return null;
    }

    @Override
    public TransformState getCurrentState(int fadingView) {
        return null;
    }

    @Override
    public void transformTo(TransformableView notification, Runnable endRunnable) {
        CrossFadeHelper.fadeOut(this.mView, endRunnable);
    }

    @Override
    public void transformTo(TransformableView notification, float transformationAmount) {
        CrossFadeHelper.fadeOut(this.mView, transformationAmount);
    }

    @Override
    public void transformFrom(TransformableView notification) {
        CrossFadeHelper.fadeIn(this.mView);
    }

    @Override
    public void transformFrom(TransformableView notification, float transformationAmount) {
        CrossFadeHelper.fadeIn(this.mView, transformationAmount);
    }

    @Override
    public void setVisible(boolean visible) {
        this.mView.animate().cancel();
        this.mView.setVisibility(visible ? 0 : 4);
    }

    public int getCustomBackgroundColor() {
        return 0;
    }

    public void setShowingLegacyBackground(boolean showing) {
    }

    public void setContentHeight(int contentHeight, int minHeightHint) {
    }
}
