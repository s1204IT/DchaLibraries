package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import com.android.systemui.R;
import com.android.systemui.ViewInvertHelper;

public class NotificationTemplateViewWrapper extends NotificationViewWrapper {
    private final ColorMatrix mGrayscaleColorMatrix;
    private ImageView mIcon;
    private int mIconBackgroundColor;
    private final int mIconBackgroundDarkColor;
    private final PorterDuffColorFilter mIconColorFilter;
    private final int mIconDarkAlpha;
    private boolean mIconForceGraysaleWhenDark;
    private ViewInvertHelper mInvertHelper;
    private final Interpolator mLinearOutSlowInInterpolator;
    protected ImageView mPicture;

    protected NotificationTemplateViewWrapper(Context ctx, View view) {
        super(view);
        this.mGrayscaleColorMatrix = new ColorMatrix();
        this.mIconColorFilter = new PorterDuffColorFilter(0, PorterDuff.Mode.SRC_ATOP);
        this.mIconDarkAlpha = ctx.getResources().getInteger(R.integer.doze_small_icon_alpha);
        this.mIconBackgroundDarkColor = ctx.getResources().getColor(R.color.doze_small_icon_background_color);
        this.mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(ctx, android.R.interpolator.linear_out_slow_in);
        resolveViews();
    }

    private void resolveViews() {
        View mainColumn = this.mView.findViewById(android.R.id.floating_toolbar_menu_item_image_button);
        this.mInvertHelper = mainColumn != null ? new ViewInvertHelper(mainColumn, 700L) : null;
        ImageView largeIcon = (ImageView) this.mView.findViewById(android.R.id.icon);
        ImageView rightIcon = (ImageView) this.mView.findViewById(android.R.id.replaceText);
        this.mIcon = resolveIcon(largeIcon, rightIcon);
        this.mPicture = resolvePicture(largeIcon);
        this.mIconBackgroundColor = resolveBackgroundColor(this.mIcon);
        this.mIconForceGraysaleWhenDark = (this.mIcon == null || this.mIcon.getDrawable().getColorFilter() == null) ? false : true;
    }

    private ImageView resolveIcon(ImageView largeIcon, ImageView rightIcon) {
        if (largeIcon != null && largeIcon.getBackground() != null) {
            return largeIcon;
        }
        if (rightIcon == null || rightIcon.getVisibility() != 0) {
            return null;
        }
        return rightIcon;
    }

    private ImageView resolvePicture(ImageView largeIcon) {
        if (largeIcon == null || largeIcon.getBackground() != null) {
            return null;
        }
        return largeIcon;
    }

    private int resolveBackgroundColor(ImageView icon) {
        if (icon != null && icon.getBackground() != null) {
            ColorFilter filter = icon.getBackground().getColorFilter();
            if (filter instanceof PorterDuffColorFilter) {
                return ((PorterDuffColorFilter) filter).getColor();
            }
        }
        return 0;
    }

    @Override
    public void notifyContentUpdated() {
        super.notifyContentUpdated();
        resolveViews();
    }

    @Override
    public void setDark(boolean dark, boolean fade, long delay) {
        if (this.mInvertHelper != null) {
            if (fade) {
                this.mInvertHelper.fade(dark, delay);
            } else {
                this.mInvertHelper.update(dark);
            }
        }
        if (this.mIcon != null) {
            if (fade) {
                fadeIconColorFilter(this.mIcon, dark, delay);
                fadeIconAlpha(this.mIcon, dark, delay);
                if (!this.mIconForceGraysaleWhenDark) {
                    fadeGrayscale(this.mIcon, dark, delay);
                }
            } else {
                updateIconColorFilter(this.mIcon, dark);
                updateIconAlpha(this.mIcon, dark);
                if (!this.mIconForceGraysaleWhenDark) {
                    updateGrayscale(this.mIcon, dark);
                }
            }
        }
        setPictureGrayscale(dark, fade, delay);
    }

    protected void setPictureGrayscale(boolean grayscale, boolean fade, long delay) {
        if (this.mPicture != null) {
            if (fade) {
                fadeGrayscale(this.mPicture, grayscale, delay);
            } else {
                updateGrayscale(this.mPicture, grayscale);
            }
        }
    }

    private void startIntensityAnimation(ValueAnimator.AnimatorUpdateListener updateListener, boolean dark, long delay, Animator.AnimatorListener listener) {
        float startIntensity = dark ? 0.0f : 1.0f;
        float endIntensity = dark ? 1.0f : 0.0f;
        ValueAnimator animator = ValueAnimator.ofFloat(startIntensity, endIntensity);
        animator.addUpdateListener(updateListener);
        animator.setDuration(700L);
        animator.setInterpolator(this.mLinearOutSlowInInterpolator);
        animator.setStartDelay(delay);
        if (listener != null) {
            animator.addListener(listener);
        }
        animator.start();
    }

    private void fadeIconColorFilter(final ImageView target, boolean dark, long delay) {
        startIntensityAnimation(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                NotificationTemplateViewWrapper.this.updateIconColorFilter(target, ((Float) animation.getAnimatedValue()).floatValue());
            }
        }, dark, delay, null);
    }

    private void fadeIconAlpha(final ImageView target, boolean dark, long delay) {
        startIntensityAnimation(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float t = ((Float) animation.getAnimatedValue()).floatValue();
                target.setImageAlpha((int) ((255.0f * (1.0f - t)) + (NotificationTemplateViewWrapper.this.mIconDarkAlpha * t)));
            }
        }, dark, delay, null);
    }

    protected void fadeGrayscale(final ImageView target, final boolean dark, long delay) {
        startIntensityAnimation(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                NotificationTemplateViewWrapper.this.updateGrayscaleMatrix(((Float) animation.getAnimatedValue()).floatValue());
                target.setColorFilter(new ColorMatrixColorFilter(NotificationTemplateViewWrapper.this.mGrayscaleColorMatrix));
            }
        }, dark, delay, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!dark) {
                    target.setColorFilter((ColorFilter) null);
                }
            }
        });
    }

    private void updateIconColorFilter(ImageView target, boolean dark) {
        updateIconColorFilter(target, dark ? 1.0f : 0.0f);
    }

    public void updateIconColorFilter(ImageView target, float intensity) {
        int color = interpolateColor(this.mIconBackgroundColor, this.mIconBackgroundDarkColor, intensity);
        this.mIconColorFilter.setColor(color);
        Drawable background = target.getBackground();
        if (background != null) {
            background.mutate().setColorFilter(this.mIconColorFilter);
        }
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

    public void updateGrayscaleMatrix(float intensity) {
        this.mGrayscaleColorMatrix.setSaturation(1.0f - intensity);
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
}
