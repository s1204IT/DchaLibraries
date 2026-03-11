package com.android.systemui.statusbar.stack;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.util.Property;
import android.view.View;
import android.view.animation.Interpolator;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.notification.AnimatableProperty;
import com.android.systemui.statusbar.notification.PropertyAnimator;
import com.android.systemui.statusbar.policy.HeadsUpUtil;

public class ViewState {
    protected static final AnimationProperties NO_NEW_ANIMATIONS = new AnimationProperties() {
        AnimationFilter mAnimationFilter = new AnimationFilter();

        @Override
        public AnimationFilter getAnimationFilter() {
            return this.mAnimationFilter;
        }
    };
    private static final AnimatableProperty SCALE_X_PROPERTY = new AnimatableProperty() {
        @Override
        public int getAnimationStartTag() {
            return R.id.scale_x_animator_start_value_tag;
        }

        @Override
        public int getAnimationEndTag() {
            return R.id.scale_x_animator_end_value_tag;
        }

        @Override
        public int getAnimatorTag() {
            return R.id.scale_x_animator_tag;
        }

        @Override
        public Property getProperty() {
            return View.SCALE_X;
        }
    };
    private static final AnimatableProperty SCALE_Y_PROPERTY = new AnimatableProperty() {
        @Override
        public int getAnimationStartTag() {
            return R.id.scale_y_animator_start_value_tag;
        }

        @Override
        public int getAnimationEndTag() {
            return R.id.scale_y_animator_end_value_tag;
        }

        @Override
        public int getAnimatorTag() {
            return R.id.scale_y_animator_tag;
        }

        @Override
        public Property getProperty() {
            return View.SCALE_Y;
        }
    };
    public float alpha;
    public boolean gone;
    public boolean hidden;
    public float scaleX = 1.0f;
    public float scaleY = 1.0f;
    public float xTranslation;
    public float yTranslation;
    public float zTranslation;

    public void copyFrom(ViewState viewState) {
        this.alpha = viewState.alpha;
        this.xTranslation = viewState.xTranslation;
        this.yTranslation = viewState.yTranslation;
        this.zTranslation = viewState.zTranslation;
        this.gone = viewState.gone;
        this.hidden = viewState.hidden;
        this.scaleX = viewState.scaleX;
        this.scaleY = viewState.scaleY;
    }

    public void initFrom(View view) {
        this.alpha = view.getAlpha();
        this.xTranslation = view.getTranslationX();
        this.yTranslation = view.getTranslationY();
        this.zTranslation = view.getTranslationZ();
        this.gone = view.getVisibility() == 8;
        this.hidden = view.getVisibility() == 4;
        this.scaleX = view.getScaleX();
        this.scaleY = view.getScaleY();
    }

    public void applyToView(View view) {
        int i;
        if (this.gone) {
            return;
        }
        if (isAnimating(view, R.id.translation_x_animator_tag)) {
            updateAnimationX(view);
        } else if (view.getTranslationX() != this.xTranslation) {
            view.setTranslationX(this.xTranslation);
        }
        if (isAnimating(view, R.id.translation_y_animator_tag)) {
            updateAnimationY(view);
        } else if (view.getTranslationY() != this.yTranslation) {
            view.setTranslationY(this.yTranslation);
        }
        if (isAnimating(view, R.id.translation_z_animator_tag)) {
            updateAnimationZ(view);
        } else if (view.getTranslationZ() != this.zTranslation) {
            view.setTranslationZ(this.zTranslation);
        }
        if (isAnimating(view, SCALE_X_PROPERTY)) {
            updateAnimation(view, SCALE_X_PROPERTY, this.scaleX);
        } else if (view.getScaleX() != this.scaleX) {
            view.setScaleX(this.scaleX);
        }
        if (isAnimating(view, SCALE_Y_PROPERTY)) {
            updateAnimation(view, SCALE_Y_PROPERTY, this.scaleY);
        } else if (view.getScaleY() != this.scaleY) {
            view.setScaleY(this.scaleY);
        }
        int visibility = view.getVisibility();
        boolean z = this.alpha == 0.0f || (this.hidden && !(isAnimating(view) && visibility == 0));
        if (isAnimating(view, R.id.alpha_animator_tag)) {
            updateAlphaAnimation(view);
        } else if (view.getAlpha() != this.alpha) {
            boolean z2 = (z || ((this.alpha > 1.0f ? 1 : (this.alpha == 1.0f ? 0 : -1)) == 0) || !view.hasOverlappingRendering()) ? false : true;
            int layerType = view.getLayerType();
            if (z2) {
                i = 2;
            } else {
                i = 0;
            }
            if (layerType != i) {
                view.setLayerType(i, null);
            }
            view.setAlpha(this.alpha);
        }
        int i2 = z ? 4 : 0;
        if (i2 != visibility) {
            if (!(view instanceof ExpandableView) || !((ExpandableView) view).willBeGone()) {
                view.setVisibility(i2);
            }
        }
    }

    public boolean isAnimating(View view) {
        return isAnimating(view, R.id.translation_x_animator_tag) || isAnimating(view, R.id.translation_y_animator_tag) || isAnimating(view, R.id.translation_z_animator_tag) || isAnimating(view, R.id.alpha_animator_tag) || isAnimating(view, SCALE_X_PROPERTY) || isAnimating(view, SCALE_Y_PROPERTY);
    }

    private static boolean isAnimating(View view, int i) {
        return getChildTag(view, i) != null;
    }

    public static boolean isAnimating(View view, AnimatableProperty animatableProperty) {
        return getChildTag(view, animatableProperty.getAnimatorTag()) != null;
    }

    public void animateTo(View view, AnimationProperties animationProperties) {
        boolean z = view.getVisibility() == 0;
        float f = this.alpha;
        if (!z && ((f != 0.0f || view.getAlpha() != 0.0f) && !this.gone && !this.hidden)) {
            view.setVisibility(0);
        }
        boolean z2 = this.alpha != view.getAlpha();
        if (view instanceof ExpandableView) {
            z2 &= !((ExpandableView) view).willBeGone();
        }
        if (view.getTranslationX() != this.xTranslation) {
            startXTranslationAnimation(view, animationProperties);
        } else {
            abortAnimation(view, R.id.translation_x_animator_tag);
        }
        if (view.getTranslationY() != this.yTranslation) {
            startYTranslationAnimation(view, animationProperties);
        } else {
            abortAnimation(view, R.id.translation_y_animator_tag);
        }
        if (view.getTranslationZ() != this.zTranslation) {
            startZTranslationAnimation(view, animationProperties);
        } else {
            abortAnimation(view, R.id.translation_z_animator_tag);
        }
        if (view.getScaleX() != this.scaleX) {
            PropertyAnimator.startAnimation(view, SCALE_X_PROPERTY, this.scaleX, animationProperties);
        } else {
            abortAnimation(view, SCALE_X_PROPERTY.getAnimatorTag());
        }
        if (view.getScaleY() != this.scaleY) {
            PropertyAnimator.startAnimation(view, SCALE_Y_PROPERTY, this.scaleY, animationProperties);
        } else {
            abortAnimation(view, SCALE_Y_PROPERTY.getAnimatorTag());
        }
        if (z2) {
            startAlphaAnimation(view, animationProperties);
        } else {
            abortAnimation(view, R.id.alpha_animator_tag);
        }
    }

    private void updateAlphaAnimation(View view) {
        startAlphaAnimation(view, NO_NEW_ANIMATIONS);
    }

    private void startAlphaAnimation(final View view, AnimationProperties animationProperties) {
        Float f = (Float) getChildTag(view, R.id.alpha_animator_start_value_tag);
        Float f2 = (Float) getChildTag(view, R.id.alpha_animator_end_value_tag);
        final float f3 = this.alpha;
        if (f2 != null && f2.floatValue() == f3) {
            return;
        }
        ObjectAnimator objectAnimator = (ObjectAnimator) getChildTag(view, R.id.alpha_animator_tag);
        if (!animationProperties.getAnimationFilter().animateAlpha) {
            if (objectAnimator != null) {
                PropertyValuesHolder[] values = objectAnimator.getValues();
                float fFloatValue = f.floatValue() + (f3 - f2.floatValue());
                values[0].setFloatValues(fFloatValue, f3);
                view.setTag(R.id.alpha_animator_start_value_tag, Float.valueOf(fFloatValue));
                view.setTag(R.id.alpha_animator_end_value_tag, Float.valueOf(f3));
                objectAnimator.setCurrentPlayTime(objectAnimator.getCurrentPlayTime());
                return;
            }
            view.setAlpha(f3);
            if (f3 == 0.0f) {
                view.setVisibility(4);
            }
        }
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(view, (Property<View, Float>) View.ALPHA, view.getAlpha(), f3);
        objectAnimatorOfFloat.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        view.setLayerType(2, null);
        objectAnimatorOfFloat.addListener(new AnimatorListenerAdapter() {
            public boolean mWasCancelled;

            @Override
            public void onAnimationEnd(Animator animator) {
                view.setLayerType(0, null);
                if (f3 == 0.0f && !this.mWasCancelled) {
                    view.setVisibility(4);
                }
                view.setTag(R.id.alpha_animator_tag, null);
                view.setTag(R.id.alpha_animator_start_value_tag, null);
                view.setTag(R.id.alpha_animator_end_value_tag, null);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                this.mWasCancelled = true;
            }

            @Override
            public void onAnimationStart(Animator animator) {
                this.mWasCancelled = false;
            }
        });
        objectAnimatorOfFloat.setDuration(cancelAnimatorAndGetNewDuration(animationProperties.duration, objectAnimator));
        if (animationProperties.delay > 0 && (objectAnimator == null || objectAnimator.getAnimatedFraction() == 0.0f)) {
            objectAnimatorOfFloat.setStartDelay(animationProperties.delay);
        }
        AnimatorListenerAdapter animationFinishListener = animationProperties.getAnimationFinishListener();
        if (animationFinishListener != null) {
            objectAnimatorOfFloat.addListener(animationFinishListener);
        }
        startAnimator(objectAnimatorOfFloat, animationFinishListener);
        view.setTag(R.id.alpha_animator_tag, objectAnimatorOfFloat);
        view.setTag(R.id.alpha_animator_start_value_tag, Float.valueOf(view.getAlpha()));
        view.setTag(R.id.alpha_animator_end_value_tag, Float.valueOf(f3));
    }

    private void updateAnimationZ(View view) {
        startZTranslationAnimation(view, NO_NEW_ANIMATIONS);
    }

    private void updateAnimation(View view, AnimatableProperty animatableProperty, float f) {
        PropertyAnimator.startAnimation(view, animatableProperty, f, NO_NEW_ANIMATIONS);
    }

    private void startZTranslationAnimation(final View view, AnimationProperties animationProperties) {
        Float f = (Float) getChildTag(view, R.id.translation_z_animator_start_value_tag);
        Float f2 = (Float) getChildTag(view, R.id.translation_z_animator_end_value_tag);
        float f3 = this.zTranslation;
        if (f2 != null && f2.floatValue() == f3) {
            return;
        }
        ObjectAnimator objectAnimator = (ObjectAnimator) getChildTag(view, R.id.translation_z_animator_tag);
        if (!animationProperties.getAnimationFilter().animateZ) {
            if (objectAnimator != null) {
                PropertyValuesHolder[] values = objectAnimator.getValues();
                float fFloatValue = f.floatValue() + (f3 - f2.floatValue());
                values[0].setFloatValues(fFloatValue, f3);
                view.setTag(R.id.translation_z_animator_start_value_tag, Float.valueOf(fFloatValue));
                view.setTag(R.id.translation_z_animator_end_value_tag, Float.valueOf(f3));
                objectAnimator.setCurrentPlayTime(objectAnimator.getCurrentPlayTime());
                return;
            }
            view.setTranslationZ(f3);
        }
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(view, (Property<View, Float>) View.TRANSLATION_Z, view.getTranslationZ(), f3);
        objectAnimatorOfFloat.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        objectAnimatorOfFloat.setDuration(cancelAnimatorAndGetNewDuration(animationProperties.duration, objectAnimator));
        if (animationProperties.delay > 0 && (objectAnimator == null || objectAnimator.getAnimatedFraction() == 0.0f)) {
            objectAnimatorOfFloat.setStartDelay(animationProperties.delay);
        }
        AnimatorListenerAdapter animationFinishListener = animationProperties.getAnimationFinishListener();
        if (animationFinishListener != null) {
            objectAnimatorOfFloat.addListener(animationFinishListener);
        }
        objectAnimatorOfFloat.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                view.setTag(R.id.translation_z_animator_tag, null);
                view.setTag(R.id.translation_z_animator_start_value_tag, null);
                view.setTag(R.id.translation_z_animator_end_value_tag, null);
            }
        });
        startAnimator(objectAnimatorOfFloat, animationFinishListener);
        view.setTag(R.id.translation_z_animator_tag, objectAnimatorOfFloat);
        view.setTag(R.id.translation_z_animator_start_value_tag, Float.valueOf(view.getTranslationZ()));
        view.setTag(R.id.translation_z_animator_end_value_tag, Float.valueOf(f3));
    }

    private void updateAnimationX(View view) {
        startXTranslationAnimation(view, NO_NEW_ANIMATIONS);
    }

    private void startXTranslationAnimation(final View view, AnimationProperties animationProperties) {
        Float f = (Float) getChildTag(view, R.id.translation_x_animator_start_value_tag);
        Float f2 = (Float) getChildTag(view, R.id.translation_x_animator_end_value_tag);
        float f3 = this.xTranslation;
        if (f2 != null && f2.floatValue() == f3) {
            return;
        }
        ObjectAnimator objectAnimator = (ObjectAnimator) getChildTag(view, R.id.translation_x_animator_tag);
        if (!animationProperties.getAnimationFilter().animateX) {
            if (objectAnimator != null) {
                PropertyValuesHolder[] values = objectAnimator.getValues();
                float fFloatValue = f.floatValue() + (f3 - f2.floatValue());
                values[0].setFloatValues(fFloatValue, f3);
                view.setTag(R.id.translation_x_animator_start_value_tag, Float.valueOf(fFloatValue));
                view.setTag(R.id.translation_x_animator_end_value_tag, Float.valueOf(f3));
                objectAnimator.setCurrentPlayTime(objectAnimator.getCurrentPlayTime());
                return;
            }
            view.setTranslationX(f3);
            return;
        }
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(view, (Property<View, Float>) View.TRANSLATION_X, view.getTranslationX(), f3);
        Interpolator customInterpolator = animationProperties.getCustomInterpolator(view, View.TRANSLATION_X);
        if (customInterpolator == null) {
            customInterpolator = Interpolators.FAST_OUT_SLOW_IN;
        }
        objectAnimatorOfFloat.setInterpolator(customInterpolator);
        objectAnimatorOfFloat.setDuration(cancelAnimatorAndGetNewDuration(animationProperties.duration, objectAnimator));
        if (animationProperties.delay > 0 && (objectAnimator == null || objectAnimator.getAnimatedFraction() == 0.0f)) {
            objectAnimatorOfFloat.setStartDelay(animationProperties.delay);
        }
        AnimatorListenerAdapter animationFinishListener = animationProperties.getAnimationFinishListener();
        if (animationFinishListener != null) {
            objectAnimatorOfFloat.addListener(animationFinishListener);
        }
        objectAnimatorOfFloat.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                view.setTag(R.id.translation_x_animator_tag, null);
                view.setTag(R.id.translation_x_animator_start_value_tag, null);
                view.setTag(R.id.translation_x_animator_end_value_tag, null);
            }
        });
        startAnimator(objectAnimatorOfFloat, animationFinishListener);
        view.setTag(R.id.translation_x_animator_tag, objectAnimatorOfFloat);
        view.setTag(R.id.translation_x_animator_start_value_tag, Float.valueOf(view.getTranslationX()));
        view.setTag(R.id.translation_x_animator_end_value_tag, Float.valueOf(f3));
    }

    private void updateAnimationY(View view) {
        startYTranslationAnimation(view, NO_NEW_ANIMATIONS);
    }

    private void startYTranslationAnimation(final View view, AnimationProperties animationProperties) {
        Float f = (Float) getChildTag(view, R.id.translation_y_animator_start_value_tag);
        Float f2 = (Float) getChildTag(view, R.id.translation_y_animator_end_value_tag);
        float f3 = this.yTranslation;
        if (f2 != null && f2.floatValue() == f3) {
            return;
        }
        ObjectAnimator objectAnimator = (ObjectAnimator) getChildTag(view, R.id.translation_y_animator_tag);
        if (!animationProperties.getAnimationFilter().shouldAnimateY(view)) {
            if (objectAnimator != null) {
                PropertyValuesHolder[] values = objectAnimator.getValues();
                float fFloatValue = f.floatValue() + (f3 - f2.floatValue());
                values[0].setFloatValues(fFloatValue, f3);
                view.setTag(R.id.translation_y_animator_start_value_tag, Float.valueOf(fFloatValue));
                view.setTag(R.id.translation_y_animator_end_value_tag, Float.valueOf(f3));
                objectAnimator.setCurrentPlayTime(objectAnimator.getCurrentPlayTime());
                return;
            }
            view.setTranslationY(f3);
            return;
        }
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(view, (Property<View, Float>) View.TRANSLATION_Y, view.getTranslationY(), f3);
        Interpolator customInterpolator = animationProperties.getCustomInterpolator(view, View.TRANSLATION_Y);
        if (customInterpolator == null) {
            customInterpolator = Interpolators.FAST_OUT_SLOW_IN;
        }
        objectAnimatorOfFloat.setInterpolator(customInterpolator);
        objectAnimatorOfFloat.setDuration(cancelAnimatorAndGetNewDuration(animationProperties.duration, objectAnimator));
        if (animationProperties.delay > 0 && (objectAnimator == null || objectAnimator.getAnimatedFraction() == 0.0f)) {
            objectAnimatorOfFloat.setStartDelay(animationProperties.delay);
        }
        AnimatorListenerAdapter animationFinishListener = animationProperties.getAnimationFinishListener();
        if (animationFinishListener != null) {
            objectAnimatorOfFloat.addListener(animationFinishListener);
        }
        objectAnimatorOfFloat.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                HeadsUpUtil.setIsClickedHeadsUpNotification(view, false);
                view.setTag(R.id.translation_y_animator_tag, null);
                view.setTag(R.id.translation_y_animator_start_value_tag, null);
                view.setTag(R.id.translation_y_animator_end_value_tag, null);
                ViewState.this.onYTranslationAnimationFinished(view);
            }
        });
        startAnimator(objectAnimatorOfFloat, animationFinishListener);
        view.setTag(R.id.translation_y_animator_tag, objectAnimatorOfFloat);
        view.setTag(R.id.translation_y_animator_start_value_tag, Float.valueOf(view.getTranslationY()));
        view.setTag(R.id.translation_y_animator_end_value_tag, Float.valueOf(f3));
    }

    protected void onYTranslationAnimationFinished(View view) {
        if (this.hidden && !this.gone) {
            view.setVisibility(4);
        }
    }

    public static void startAnimator(Animator animator, AnimatorListenerAdapter animatorListenerAdapter) {
        if (animatorListenerAdapter != null) {
            animatorListenerAdapter.onAnimationStart(animator);
        }
        animator.start();
    }

    public static <T> T getChildTag(View view, int i) {
        return (T) view.getTag(i);
    }

    protected void abortAnimation(View view, int i) {
        Animator animator = (Animator) getChildTag(view, i);
        if (animator != null) {
            animator.cancel();
        }
    }

    public static long cancelAnimatorAndGetNewDuration(long j, ValueAnimator valueAnimator) {
        if (valueAnimator != null) {
            long jMax = Math.max(valueAnimator.getDuration() - valueAnimator.getCurrentPlayTime(), j);
            valueAnimator.cancel();
            return jMax;
        }
        return j;
    }

    public static float getFinalTranslationX(View view) {
        if (view == null) {
            return 0.0f;
        }
        if (((ValueAnimator) getChildTag(view, R.id.translation_x_animator_tag)) == null) {
            return view.getTranslationX();
        }
        return ((Float) getChildTag(view, R.id.translation_x_animator_end_value_tag)).floatValue();
    }

    public static float getFinalTranslationY(View view) {
        if (view == null) {
            return 0.0f;
        }
        if (((ValueAnimator) getChildTag(view, R.id.translation_y_animator_tag)) == null) {
            return view.getTranslationY();
        }
        return ((Float) getChildTag(view, R.id.translation_y_animator_end_value_tag)).floatValue();
    }

    public static float getFinalTranslationZ(View view) {
        if (view == null) {
            return 0.0f;
        }
        if (((ValueAnimator) getChildTag(view, R.id.translation_z_animator_tag)) == null) {
            return view.getTranslationZ();
        }
        return ((Float) getChildTag(view, R.id.translation_z_animator_end_value_tag)).floatValue();
    }

    public static boolean isAnimatingY(View view) {
        return getChildTag(view, R.id.translation_y_animator_tag) != null;
    }

    public void cancelAnimations(View view) {
        Animator animator = (Animator) getChildTag(view, R.id.translation_x_animator_tag);
        if (animator != null) {
            animator.cancel();
        }
        Animator animator2 = (Animator) getChildTag(view, R.id.translation_y_animator_tag);
        if (animator2 != null) {
            animator2.cancel();
        }
        Animator animator3 = (Animator) getChildTag(view, R.id.translation_z_animator_tag);
        if (animator3 != null) {
            animator3.cancel();
        }
        Animator animator4 = (Animator) getChildTag(view, R.id.alpha_animator_tag);
        if (animator4 != null) {
            animator4.cancel();
        }
    }
}
