package com.android.managedprovisioning.preprovisioning.anim;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.R;
import com.android.managedprovisioning.model.CustomizationParams;
import java.util.List;

public class BenefitsAnimation {
    private static final int[][] ID_ANIMATION_TARGET = {new int[]{R.anim.text_scene_0_animation, R.id.text_0}, new int[]{R.anim.text_scene_1_animation, R.id.text_1}, new int[]{R.anim.text_scene_2_animation, R.id.text_2}, new int[]{R.anim.text_scene_3_animation, R.id.text_3}, new int[]{R.anim.text_scene_master_animation, R.id.text_master}};
    private static final int[] SLIDE_CAPTION_TEXT_VIEWS = {R.id.text_0, R.id.text_1, R.id.text_2, R.id.text_3};
    private final Activity mActivity;
    private boolean mStopped;
    private final Animator mTextAnimation;
    private final AnimatedVectorDrawable mTopAnimation;

    public BenefitsAnimation(Activity activity, List<Integer> list, int i, CustomizationParams customizationParams) {
        if (list.size() != 3) {
            throw new IllegalArgumentException("Wrong number of slide captions. Expected: 3");
        }
        this.mActivity = (Activity) Preconditions.checkNotNull(activity);
        this.mTextAnimation = (Animator) Preconditions.checkNotNull(assembleTextAnimation());
        applySlideCaptions(list);
        applyContentDescription(i);
        setTopInfoDrawable(customizationParams);
        this.mTopAnimation = (AnimatedVectorDrawable) Preconditions.checkNotNull(extractAnimationFromImageView(R.id.animated_info));
        chainAnimations();
        this.mActivity.findViewById(android.R.id.content).post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.adjustToScreenSize();
            }
        });
    }

    private void setTopInfoDrawable(CustomizationParams customizationParams) {
        ((ImageView) this.mActivity.findViewById(R.id.animated_info)).setImageDrawable(this.mActivity.getResources().getDrawable(R.drawable.topinfo_animation, new ContextThemeWrapper(this.mActivity, new SwiperThemeMatcher(this.mActivity, new ColorMatcher()).findTheme(customizationParams.mainColor)).getTheme()));
    }

    public void start() {
        this.mStopped = false;
        this.mTopAnimation.start();
    }

    public void stop() {
        this.mStopped = true;
        this.mTopAnimation.stop();
    }

    private void adjustToScreenSize() {
        int i;
        if (this.mActivity.isDestroyed()) {
            return;
        }
        ImageView imageView = (ImageView) this.mActivity.findViewById(R.id.animated_info);
        float width = imageView.getWidth() / 1080.0f;
        ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
        int height = imageView.getHeight();
        int i2 = (int) (height * width);
        layoutParams.height = i2;
        imageView.setLayoutParams(layoutParams);
        if (width < 1.0f) {
            for (int i3 : SLIDE_CAPTION_TEXT_VIEWS) {
                View viewFindViewById = this.mActivity.findViewById(i3);
                viewFindViewById.setScaleX(width);
                viewFindViewById.setScaleY(width);
            }
        }
        int height2 = this.mActivity.findViewById(R.id.intro_po_content).getHeight() + (i2 - height);
        int height3 = this.mActivity.findViewById(R.id.suw_layout_content).getHeight();
        if (height2 > height3 && (i = layoutParams.height - (height2 - height3)) >= this.mActivity.getResources().getDimensionPixelSize(R.dimen.intro_animation_min_height)) {
            layoutParams.height = i;
            imageView.setLayoutParams(layoutParams);
        }
    }

    private void chainAnimations() {
        this.mTopAnimation.registerAnimationCallback(new Animatable2.AnimationCallback() {
            @Override
            public void onAnimationStart(Drawable drawable) {
                super.onAnimationStart(drawable);
                BenefitsAnimation.this.mTextAnimation.start();
            }

            @Override
            public void onAnimationEnd(Drawable drawable) {
                super.onAnimationEnd(drawable);
                BenefitsAnimation.this.mTextAnimation.cancel();
                if (!BenefitsAnimation.this.mStopped) {
                    BenefitsAnimation.this.mTopAnimation.start();
                }
            }
        });
    }

    private AnimatorSet assembleTextAnimation() {
        Animator[] animatorArr = new Animator[ID_ANIMATION_TARGET.length];
        for (int i = 0; i < ID_ANIMATION_TARGET.length; i++) {
            int[] iArr = ID_ANIMATION_TARGET[i];
            animatorArr[i] = AnimatorInflater.loadAnimator(this.mActivity, iArr[0]);
            animatorArr[i].setTarget(this.mActivity.findViewById(iArr[1]));
        }
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animatorArr);
        return animatorSet;
    }

    private void applySlideCaptions(List<Integer> list) {
        int[] iArr = SLIDE_CAPTION_TEXT_VIEWS;
        int length = iArr.length;
        int i = 0;
        int i2 = 0;
        while (i < length) {
            ((TextView) this.mActivity.findViewById(iArr[i])).setText(list.get(i2 % list.size()).intValue());
            i++;
            i2++;
        }
    }

    private void applyContentDescription(int i) {
        this.mActivity.findViewById(R.id.animation_top_level_frame).setContentDescription(this.mActivity.getString(i));
    }

    private AnimatedVectorDrawable extractAnimationFromImageView(int i) {
        return (AnimatedVectorDrawable) ((ImageView) this.mActivity.findViewById(i)).getDrawable();
    }
}
