package com.android.systemui.qs.tileimpl;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSIconView;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.AlphaControlledSignalTileView;
import java.util.Objects;

public class QSIconViewImpl extends QSIconView {
    private boolean mAnimationEnabled;
    protected final View mIcon;
    protected final int mIconSizePx;
    private int mState;
    protected final int mTilePaddingBelowIconPx;
    private int mTint;

    public QSIconViewImpl(Context context) {
        super(context);
        this.mAnimationEnabled = true;
        this.mState = -1;
        Resources resources = context.getResources();
        this.mIconSizePx = resources.getDimensionPixelSize(R.dimen.qs_tile_icon_size);
        this.mTilePaddingBelowIconPx = resources.getDimensionPixelSize(R.dimen.qs_tile_padding_below_icon);
        this.mIcon = createIcon();
        addView(this.mIcon);
    }

    @Override
    public void disableAnimation() {
        this.mAnimationEnabled = false;
    }

    @Override
    public View getIconView() {
        return this.mIcon;
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int size = View.MeasureSpec.getSize(i);
        this.mIcon.measure(View.MeasureSpec.makeMeasureSpec(size, getIconMeasureMode()), exactly(this.mIconSizePx));
        setMeasuredDimension(size, this.mIcon.getMeasuredHeight() + this.mTilePaddingBelowIconPx);
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        layout(this.mIcon, (getMeasuredWidth() - this.mIcon.getMeasuredWidth()) / 2, 0);
    }

    @Override
    public void setIcon(QSTile.State state) {
        setIcon((ImageView) this.mIcon, state);
    }

    public void updateIcon(ImageView imageView, QSTile.State state) {
        Drawable drawable;
        QSTile.Icon icon = state.iconSupplier != null ? state.iconSupplier.get() : state.icon;
        if (!Objects.equals(icon, imageView.getTag(R.id.qs_icon_tag)) || !Objects.equals(state.slash, imageView.getTag(R.id.qs_slash_tag))) {
            boolean z = imageView.isShown() && this.mAnimationEnabled && imageView.getDrawable() != null;
            if (icon != null) {
                drawable = z ? icon.getDrawable(this.mContext) : icon.getInvisibleDrawable(this.mContext);
            } else {
                drawable = 0;
            }
            int padding = icon != null ? icon.getPadding() : 0;
            if (drawable != 0) {
                drawable.setAutoMirrored(false);
                drawable.setLayoutDirection(getLayoutDirection());
            }
            if (imageView instanceof SlashImageView) {
                SlashImageView slashImageView = (SlashImageView) imageView;
                slashImageView.setAnimationEnabled(z);
                slashImageView.setState(null, drawable);
            } else {
                imageView.setImageDrawable(drawable);
            }
            imageView.setTag(R.id.qs_icon_tag, icon);
            imageView.setTag(R.id.qs_slash_tag, state.slash);
            imageView.setPadding(0, padding, 0, padding);
            if (drawable instanceof Animatable2) {
                final Animatable2 animatable2 = (Animatable2) drawable;
                animatable2.start();
                if (state.isTransient) {
                    animatable2.registerAnimationCallback(new Animatable2.AnimationCallback() {
                        @Override
                        public void onAnimationEnd(Drawable drawable2) {
                            animatable2.start();
                        }
                    });
                }
            }
        }
    }

    protected void setIcon(final ImageView imageView, final QSTile.State state) {
        if (state.disabledByPolicy) {
            imageView.setColorFilter(getContext().getColor(R.color.qs_tile_disabled_color));
        } else {
            imageView.clearColorFilter();
        }
        if (state.state != this.mState) {
            int color = getColor(state.state);
            this.mState = state.state;
            if (imageView.isShown() && this.mTint != 0) {
                animateGrayScale(this.mTint, color, imageView, new Runnable() {
                    @Override
                    public final void run() {
                        this.f$0.updateIcon(imageView, state);
                    }
                });
                this.mTint = color;
                return;
            }
            if (imageView instanceof AlphaControlledSignalTileView.AlphaControlledSlashImageView) {
                ((AlphaControlledSignalTileView.AlphaControlledSlashImageView) imageView).setFinalImageTintList(ColorStateList.valueOf(color));
            } else {
                setTint(imageView, color);
            }
            this.mTint = color;
            updateIcon(imageView, state);
            return;
        }
        updateIcon(imageView, state);
    }

    protected int getColor(int i) {
        return QSTileImpl.getColorForState(getContext(), i);
    }

    private void animateGrayScale(int i, int i2, final ImageView imageView, final Runnable runnable) {
        if (imageView instanceof AlphaControlledSignalTileView.AlphaControlledSlashImageView) {
            ((AlphaControlledSignalTileView.AlphaControlledSlashImageView) imageView).setFinalImageTintList(ColorStateList.valueOf(i2));
        }
        if (this.mAnimationEnabled && ValueAnimator.areAnimatorsEnabled()) {
            final float fAlpha = Color.alpha(i);
            final float fAlpha2 = Color.alpha(i2);
            final float fRed = Color.red(i);
            final float fRed2 = Color.red(i2);
            ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
            valueAnimatorOfFloat.setDuration(350L);
            valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    QSIconViewImpl.lambda$animateGrayScale$1(fAlpha, fAlpha2, fRed, fRed2, imageView, valueAnimator);
                }
            });
            valueAnimatorOfFloat.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    runnable.run();
                }
            });
            valueAnimatorOfFloat.start();
            return;
        }
        setTint(imageView, i2);
        runnable.run();
    }

    static void lambda$animateGrayScale$1(float f, float f2, float f3, float f4, ImageView imageView, ValueAnimator valueAnimator) {
        float animatedFraction = valueAnimator.getAnimatedFraction();
        int i = (int) (f + ((f2 - f) * animatedFraction));
        int i2 = (int) (f3 + ((f4 - f3) * animatedFraction));
        setTint(imageView, Color.argb(i, i2, i2, i2));
    }

    public static void setTint(ImageView imageView, int i) {
        imageView.setImageTintList(ColorStateList.valueOf(i));
    }

    protected int getIconMeasureMode() {
        return 1073741824;
    }

    protected View createIcon() {
        SlashImageView slashImageView = new SlashImageView(this.mContext);
        slashImageView.setId(android.R.id.icon);
        slashImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        return slashImageView;
    }

    protected final int exactly(int i) {
        return View.MeasureSpec.makeMeasureSpec(i, 1073741824);
    }

    protected final void layout(View view, int i, int i2) {
        view.layout(i, i2, view.getMeasuredWidth() + i, view.getMeasuredHeight() + i2);
    }
}
