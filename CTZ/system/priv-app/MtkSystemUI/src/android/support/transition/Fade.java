package android.support.transition;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
/* loaded from: classes.dex */
public class Fade extends Visibility {
    public Fade(int fadingMode) {
        setMode(fadingMode);
    }

    public Fade() {
    }

    public Fade(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, Styleable.FADE);
        int fadingMode = TypedArrayUtils.getNamedInt(a, (XmlResourceParser) attrs, "fadingMode", 0, getMode());
        setMode(fadingMode);
        a.recycle();
    }

    @Override // android.support.transition.Visibility, android.support.transition.Transition
    public void captureStartValues(TransitionValues transitionValues) {
        super.captureStartValues(transitionValues);
        transitionValues.values.put("android:fade:transitionAlpha", Float.valueOf(ViewUtils.getTransitionAlpha(transitionValues.view)));
    }

    private Animator createAnimation(final View view, float startAlpha, float endAlpha) {
        if (startAlpha == endAlpha) {
            return null;
        }
        ViewUtils.setTransitionAlpha(view, startAlpha);
        ObjectAnimator anim = ObjectAnimator.ofFloat(view, ViewUtils.TRANSITION_ALPHA, endAlpha);
        FadeAnimatorListener listener = new FadeAnimatorListener(view);
        anim.addListener(listener);
        addListener(new TransitionListenerAdapter() { // from class: android.support.transition.Fade.1
            @Override // android.support.transition.TransitionListenerAdapter, android.support.transition.Transition.TransitionListener
            public void onTransitionEnd(Transition transition) {
                ViewUtils.setTransitionAlpha(view, 1.0f);
                ViewUtils.clearNonTransitionAlpha(view);
                transition.removeListener(this);
            }
        });
        return anim;
    }

    @Override // android.support.transition.Visibility
    public Animator onAppear(ViewGroup sceneRoot, View view, TransitionValues startValues, TransitionValues endValues) {
        float startAlpha = getStartAlpha(startValues, 0.0f);
        if (startAlpha == 1.0f) {
            startAlpha = 0.0f;
        }
        return createAnimation(view, startAlpha, 1.0f);
    }

    @Override // android.support.transition.Visibility
    public Animator onDisappear(ViewGroup sceneRoot, View view, TransitionValues startValues, TransitionValues endValues) {
        ViewUtils.saveNonTransitionAlpha(view);
        float startAlpha = getStartAlpha(startValues, 1.0f);
        return createAnimation(view, startAlpha, 0.0f);
    }

    private static float getStartAlpha(TransitionValues startValues, float fallbackValue) {
        Float startAlphaFloat;
        if (startValues == null || (startAlphaFloat = (Float) startValues.values.get("android:fade:transitionAlpha")) == null) {
            return fallbackValue;
        }
        float startAlpha = startAlphaFloat.floatValue();
        return startAlpha;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class FadeAnimatorListener extends AnimatorListenerAdapter {
        private boolean mLayerTypeChanged = false;
        private final View mView;

        FadeAnimatorListener(View view) {
            this.mView = view;
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationStart(Animator animation) {
            if (ViewCompat.hasOverlappingRendering(this.mView) && this.mView.getLayerType() == 0) {
                this.mLayerTypeChanged = true;
                this.mView.setLayerType(2, null);
            }
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animation) {
            ViewUtils.setTransitionAlpha(this.mView, 1.0f);
            if (this.mLayerTypeChanged) {
                this.mView.setLayerType(0, null);
            }
        }
    }
}
