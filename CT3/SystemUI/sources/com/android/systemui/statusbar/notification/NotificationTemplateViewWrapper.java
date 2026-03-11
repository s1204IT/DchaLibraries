package com.android.systemui.statusbar.notification;

import android.R;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.service.notification.StatusBarNotification;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.systemui.statusbar.CrossFadeHelper;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.TransformableView;
import com.android.systemui.statusbar.ViewTransformationHelper;

public class NotificationTemplateViewWrapper extends NotificationHeaderViewWrapper {
    private View mActionsContainer;
    private int mContentHeight;
    private int mMinHeightHint;
    protected ImageView mPicture;
    private ProgressBar mProgressBar;
    private TextView mText;
    private TextView mTitle;

    protected NotificationTemplateViewWrapper(Context ctx, View view, ExpandableNotificationRow row) {
        super(ctx, view, row);
        this.mTransformationHelper.setCustomTransformation(new ViewTransformationHelper.CustomTransformation() {
            @Override
            public boolean transformTo(TransformState ownState, TransformableView notification, float transformationAmount) {
                if (!(notification instanceof HybridNotificationView)) {
                    return false;
                }
                TransformState otherState = notification.getCurrentState(1);
                View text = ownState.getTransformedView();
                CrossFadeHelper.fadeOut(text, transformationAmount);
                if (otherState != null) {
                    ownState.transformViewVerticalTo(otherState, this, transformationAmount);
                    otherState.recycle();
                }
                return true;
            }

            @Override
            public boolean customTransformTarget(TransformState ownState, TransformState otherState) {
                float endY = getTransformationY(ownState, otherState);
                ownState.setTransformationEndY(endY);
                return true;
            }

            @Override
            public boolean transformFrom(TransformState ownState, TransformableView notification, float transformationAmount) {
                if (!(notification instanceof HybridNotificationView)) {
                    return false;
                }
                TransformState otherState = notification.getCurrentState(1);
                View text = ownState.getTransformedView();
                CrossFadeHelper.fadeIn(text, transformationAmount);
                if (otherState != null) {
                    ownState.transformViewVerticalFrom(otherState, this, transformationAmount);
                    otherState.recycle();
                }
                return true;
            }

            @Override
            public boolean initTransformation(TransformState ownState, TransformState otherState) {
                float startY = getTransformationY(ownState, otherState);
                ownState.setTransformationStartY(startY);
                return true;
            }

            private float getTransformationY(TransformState ownState, TransformState otherState) {
                int[] otherStablePosition = otherState.getLaidOutLocationOnScreen();
                int[] ownStablePosition = ownState.getLaidOutLocationOnScreen();
                return ((otherStablePosition[1] + otherState.getTransformedView().getHeight()) - ownStablePosition[1]) * 0.33f;
            }
        }, 2);
    }

    private void resolveTemplateViews(StatusBarNotification notification) {
        this.mPicture = (ImageView) this.mView.findViewById(R.id.accessibilityActionShowTooltip);
        this.mPicture.setTag(com.android.systemui.R.id.image_icon_tag, notification.getNotification().getLargeIcon());
        this.mTitle = (TextView) this.mView.findViewById(R.id.title);
        this.mText = (TextView) this.mView.findViewById(R.id.KEYCODE_BOOKMARK);
        View progress = this.mView.findViewById(R.id.progress);
        if (progress instanceof ProgressBar) {
            this.mProgressBar = (ProgressBar) progress;
        } else {
            this.mProgressBar = null;
        }
        this.mActionsContainer = this.mView.findViewById(R.id.issued_to_header);
    }

    @Override
    public void notifyContentUpdated(StatusBarNotification notification) {
        resolveTemplateViews(notification);
        super.notifyContentUpdated(notification);
    }

    @Override
    protected void updateInvertHelper() {
        super.updateInvertHelper();
        View mainColumn = this.mView.findViewById(R.id.language_item);
        if (mainColumn == null) {
            return;
        }
        this.mInvertHelper.addTarget(mainColumn);
    }

    @Override
    protected void updateTransformedTypes() {
        super.updateTransformedTypes();
        if (this.mTitle != null) {
            this.mTransformationHelper.addTransformedView(1, this.mTitle);
        }
        if (this.mText != null) {
            this.mTransformationHelper.addTransformedView(2, this.mText);
        }
        if (this.mPicture != null) {
            this.mTransformationHelper.addTransformedView(3, this.mPicture);
        }
        if (this.mProgressBar == null) {
            return;
        }
        this.mTransformationHelper.addTransformedView(4, this.mProgressBar);
    }

    @Override
    public void setDark(boolean dark, boolean fade, long delay) {
        if (dark == this.mDark && this.mDarkInitialized) {
            return;
        }
        super.setDark(dark, fade, delay);
        setPictureGrayscale(dark, fade, delay);
        setProgressBarDark(dark, fade, delay);
    }

    private void setProgressBarDark(boolean dark, boolean fade, long delay) {
        if (this.mProgressBar == null) {
            return;
        }
        if (fade) {
            fadeProgressDark(this.mProgressBar, dark, delay);
        } else {
            updateProgressDark(this.mProgressBar, dark);
        }
    }

    private void fadeProgressDark(final ProgressBar target, boolean dark, long delay) {
        startIntensityAnimation(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float t = ((Float) animation.getAnimatedValue()).floatValue();
                NotificationTemplateViewWrapper.this.updateProgressDark(target, t);
            }
        }, dark, delay, null);
    }

    public void updateProgressDark(ProgressBar target, float intensity) {
        int color = interpolateColor(this.mColor, -1, intensity);
        target.getIndeterminateDrawable().mutate().setTint(color);
        target.getProgressDrawable().mutate().setTint(color);
    }

    private void updateProgressDark(ProgressBar target, boolean dark) {
        updateProgressDark(target, dark ? 1.0f : 0.0f);
    }

    protected void setPictureGrayscale(boolean grayscale, boolean fade, long delay) {
        if (this.mPicture == null) {
            return;
        }
        if (fade) {
            fadeGrayscale(this.mPicture, grayscale, delay);
        } else {
            updateGrayscale(this.mPicture, grayscale);
        }
    }

    private static int interpolateColor(int source, int target, float t) {
        int aSource = Color.alpha(source);
        int rSource = Color.red(source);
        int gSource = Color.green(source);
        int bSource = Color.blue(source);
        int aTarget = Color.alpha(target);
        int rTarget = Color.red(target);
        int gTarget = Color.green(target);
        int bTarget = Color.blue(target);
        return Color.argb((int) ((aSource * (1.0f - t)) + (aTarget * t)), (int) ((rSource * (1.0f - t)) + (rTarget * t)), (int) ((gSource * (1.0f - t)) + (gTarget * t)), (int) ((bSource * (1.0f - t)) + (bTarget * t)));
    }

    @Override
    public void setContentHeight(int contentHeight, int minHeightHint) {
        super.setContentHeight(contentHeight, minHeightHint);
        this.mContentHeight = contentHeight;
        this.mMinHeightHint = minHeightHint;
        updateActionOffset();
    }

    private void updateActionOffset() {
        if (this.mActionsContainer == null) {
            return;
        }
        int constrainedContentHeight = Math.max(this.mContentHeight, this.mMinHeightHint);
        this.mActionsContainer.setTranslationY(constrainedContentHeight - this.mView.getHeight());
    }
}
