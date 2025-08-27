package com.android.systemui.statusbar.notification;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.view.View;
import android.widget.ImageView;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public class NotificationDozeHelper {
    private final ColorMatrix mGrayscaleColorMatrix = new ColorMatrix();

    public void updateGrayscale(ImageView imageView, float f) {
        if (f > 0.0f) {
            updateGrayscaleMatrix(f);
            imageView.setColorFilter(new ColorMatrixColorFilter(this.mGrayscaleColorMatrix));
        } else {
            imageView.setColorFilter((ColorFilter) null);
        }
    }

    public void startIntensityAnimation(ValueAnimator.AnimatorUpdateListener animatorUpdateListener, boolean z, long j, Animator.AnimatorListener animatorListener) {
        float f = 1.0f;
        float f2 = z ? 0.0f : 1.0f;
        if (!z) {
            f = 0.0f;
        }
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(f2, f);
        valueAnimatorOfFloat.addUpdateListener(animatorUpdateListener);
        valueAnimatorOfFloat.setDuration(700L);
        valueAnimatorOfFloat.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
        valueAnimatorOfFloat.setStartDelay(j);
        if (animatorListener != null) {
            valueAnimatorOfFloat.addListener(animatorListener);
        }
        valueAnimatorOfFloat.start();
    }

    public void setIntensityDark(final Consumer<Float> consumer, boolean z, boolean z2, long j, final View view) {
        if (z2) {
            startIntensityAnimation(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.systemui.statusbar.notification.-$$Lambda$NotificationDozeHelper$cAhupcRGr8viAUyxaXt90IAG2is
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    consumer.accept((Float) valueAnimator.getAnimatedValue());
                }
            }, z, j, new AnimatorListenerAdapter() { // from class: com.android.systemui.statusbar.notification.NotificationDozeHelper.3
                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animator) {
                    view.setTag(R.id.doze_intensity_tag, null);
                }

                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationStart(Animator animator) {
                    view.setTag(R.id.doze_intensity_tag, animator);
                }
            });
            return;
        }
        Animator animator = (Animator) view.getTag(R.id.doze_intensity_tag);
        if (animator != null) {
            animator.cancel();
        }
        consumer.accept(Float.valueOf(z ? 1.0f : 0.0f));
    }

    public void updateGrayscaleMatrix(float f) {
        this.mGrayscaleColorMatrix.setSaturation(1.0f - f);
    }
}
