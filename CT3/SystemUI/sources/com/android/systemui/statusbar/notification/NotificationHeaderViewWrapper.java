package com.android.systemui.statusbar.notification;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.service.notification.StatusBarNotification;
import android.util.ArraySet;
import android.view.NotificationHeaderView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.android.systemui.R;
import com.android.systemui.ViewInvertHelper;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.TransformableView;
import com.android.systemui.statusbar.ViewTransformationHelper;
import java.util.Stack;

public class NotificationHeaderViewWrapper extends NotificationViewWrapper {
    protected int mColor;
    private ImageView mExpandButton;
    private ImageView mIcon;
    private final PorterDuffColorFilter mIconColorFilter;
    private final int mIconDarkAlpha;
    private final int mIconDarkColor;
    protected final ViewInvertHelper mInvertHelper;
    private NotificationHeaderView mNotificationHeader;
    protected final ViewTransformationHelper mTransformationHelper;

    protected NotificationHeaderViewWrapper(Context ctx, View view, ExpandableNotificationRow row) {
        super(view, row);
        this.mIconColorFilter = new PorterDuffColorFilter(0, PorterDuff.Mode.SRC_ATOP);
        this.mIconDarkColor = -1;
        this.mIconDarkAlpha = ctx.getResources().getInteger(R.integer.doze_small_icon_alpha);
        this.mInvertHelper = new ViewInvertHelper(ctx, 700L);
        this.mTransformationHelper = new ViewTransformationHelper();
        resolveHeaderViews();
        updateInvertHelper();
    }

    protected void resolveHeaderViews() {
        this.mIcon = (ImageView) this.mView.findViewById(android.R.id.icon);
        this.mExpandButton = (ImageView) this.mView.findViewById(android.R.id.label_hour);
        this.mColor = resolveColor(this.mExpandButton);
        this.mNotificationHeader = this.mView.findViewById(android.R.id.keyguard);
    }

    private int resolveColor(ImageView icon) {
        if (icon != null && icon.getDrawable() != null) {
            ColorFilter filter = icon.getDrawable().getColorFilter();
            if (filter instanceof PorterDuffColorFilter) {
                return ((PorterDuffColorFilter) filter).getColor();
            }
            return 0;
        }
        return 0;
    }

    @Override
    public void notifyContentUpdated(StatusBarNotification notification) {
        super.notifyContentUpdated(notification);
        ArraySet<View> previousViews = this.mTransformationHelper.getAllTransformingViews();
        resolveHeaderViews();
        updateInvertHelper();
        updateTransformedTypes();
        addRemainingTransformTypes();
        updateCropToPaddingForImageViews();
        ArraySet<View> currentViews = this.mTransformationHelper.getAllTransformingViews();
        for (int i = 0; i < previousViews.size(); i++) {
            View view = previousViews.valueAt(i);
            if (!currentViews.contains(view)) {
                this.mTransformationHelper.resetTransformedView(view);
            }
        }
    }

    private void addRemainingTransformTypes() {
        this.mTransformationHelper.addRemainingTransformTypes(this.mView);
    }

    private void updateCropToPaddingForImageViews() {
        Stack<View> stack = new Stack<>();
        stack.push(this.mView);
        while (!stack.isEmpty()) {
            View child = stack.pop();
            if (child instanceof ImageView) {
                ((ImageView) child).setCropToPadding(true);
            } else if (child instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) child;
                for (int i = 0; i < group.getChildCount(); i++) {
                    stack.push(group.getChildAt(i));
                }
            }
        }
    }

    protected void updateInvertHelper() {
        this.mInvertHelper.clearTargets();
        for (int i = 0; i < this.mNotificationHeader.getChildCount(); i++) {
            View child = this.mNotificationHeader.getChildAt(i);
            if (child != this.mIcon) {
                this.mInvertHelper.addTarget(child);
            }
        }
    }

    protected void updateTransformedTypes() {
        this.mTransformationHelper.reset();
        this.mTransformationHelper.addTransformedView(0, this.mNotificationHeader);
    }

    @Override
    public void setDark(boolean dark, boolean fade, long delay) {
        if (dark == this.mDark && this.mDarkInitialized) {
            return;
        }
        super.setDark(dark, fade, delay);
        if (fade) {
            this.mInvertHelper.fade(dark, delay);
        } else {
            this.mInvertHelper.update(dark);
        }
        if (this.mIcon == null || this.mRow.isChildInGroup()) {
            return;
        }
        boolean hadColorFilter = this.mNotificationHeader.getOriginalIconColor() != -1;
        if (fade) {
            if (hadColorFilter) {
                fadeIconColorFilter(this.mIcon, dark, delay);
                fadeIconAlpha(this.mIcon, dark, delay);
                return;
            } else {
                fadeGrayscale(this.mIcon, dark, delay);
                return;
            }
        }
        if (hadColorFilter) {
            updateIconColorFilter(this.mIcon, dark);
            updateIconAlpha(this.mIcon, dark);
        } else {
            updateGrayscale(this.mIcon, dark);
        }
    }

    private void fadeIconColorFilter(final ImageView target, boolean dark, long delay) {
        startIntensityAnimation(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                NotificationHeaderViewWrapper.this.updateIconColorFilter(target, ((Float) animation.getAnimatedValue()).floatValue());
            }
        }, dark, delay, null);
    }

    private void fadeIconAlpha(final ImageView target, boolean dark, long delay) {
        startIntensityAnimation(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float t = ((Float) animation.getAnimatedValue()).floatValue();
                target.setImageAlpha((int) (((1.0f - t) * 255.0f) + (NotificationHeaderViewWrapper.this.mIconDarkAlpha * t)));
            }
        }, dark, delay, null);
    }

    protected void fadeGrayscale(final ImageView target, final boolean dark, long delay) {
        startIntensityAnimation(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                NotificationHeaderViewWrapper.this.updateGrayscaleMatrix(((Float) animation.getAnimatedValue()).floatValue());
                target.setColorFilter(new ColorMatrixColorFilter(NotificationHeaderViewWrapper.this.mGrayscaleColorMatrix));
            }
        }, dark, delay, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (dark) {
                    return;
                }
                target.setColorFilter((ColorFilter) null);
            }
        });
    }

    private void updateIconColorFilter(ImageView target, boolean dark) {
        updateIconColorFilter(target, dark ? 1.0f : 0.0f);
    }

    public void updateIconColorFilter(ImageView target, float intensity) {
        int color = interpolateColor(this.mColor, -1, intensity);
        this.mIconColorFilter.setColor(color);
        Drawable iconDrawable = target.getDrawable();
        if (iconDrawable == null) {
            return;
        }
        iconDrawable.mutate().setColorFilter(this.mIconColorFilter);
    }

    private void updateIconAlpha(ImageView target, boolean dark) {
        target.setImageAlpha(dark ? this.mIconDarkAlpha : 255);
    }

    protected void updateGrayscale(ImageView target, boolean dark) {
        if (dark) {
            updateGrayscaleMatrix(1.0f);
            target.setColorFilter(new ColorMatrixColorFilter(this.mGrayscaleColorMatrix));
        } else {
            target.setColorFilter((ColorFilter) null);
        }
    }

    @Override
    public void updateExpandability(boolean expandable, View.OnClickListener onClickListener) {
        this.mExpandButton.setVisibility(expandable ? 0 : 8);
        NotificationHeaderView notificationHeaderView = this.mNotificationHeader;
        if (!expandable) {
            onClickListener = null;
        }
        notificationHeaderView.setOnClickListener(onClickListener);
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
    public NotificationHeaderView getNotificationHeader() {
        return this.mNotificationHeader;
    }

    @Override
    public TransformState getCurrentState(int fadingView) {
        return this.mTransformationHelper.getCurrentState(fadingView);
    }

    @Override
    public void transformTo(TransformableView notification, Runnable endRunnable) {
        this.mTransformationHelper.transformTo(notification, endRunnable);
    }

    @Override
    public void transformTo(TransformableView notification, float transformationAmount) {
        this.mTransformationHelper.transformTo(notification, transformationAmount);
    }

    @Override
    public void transformFrom(TransformableView notification) {
        this.mTransformationHelper.transformFrom(notification);
    }

    @Override
    public void transformFrom(TransformableView notification, float transformationAmount) {
        this.mTransformationHelper.transformFrom(notification, transformationAmount);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        this.mTransformationHelper.setVisible(visible);
    }
}
