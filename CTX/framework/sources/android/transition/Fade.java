package android.transition;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.R;

public class Fade extends Visibility {
    private static boolean DBG = false;
    public static final int IN = 1;
    private static final String LOG_TAG = "Fade";
    public static final int OUT = 2;
    static final String PROPNAME_TRANSITION_ALPHA = "android:fade:transitionAlpha";

    public Fade() {
    }

    public Fade(int i) {
        setMode(i);
    }

    public Fade(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.Fade);
        setMode(typedArrayObtainStyledAttributes.getInt(0, getMode()));
        typedArrayObtainStyledAttributes.recycle();
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        super.captureStartValues(transitionValues);
        transitionValues.values.put(PROPNAME_TRANSITION_ALPHA, Float.valueOf(transitionValues.view.getTransitionAlpha()));
    }

    private Animator createAnimation(final View view, float f, float f2) {
        if (f == f2) {
            return null;
        }
        view.setTransitionAlpha(f);
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(view, "transitionAlpha", f2);
        if (DBG) {
            Log.d(LOG_TAG, "Created animator " + objectAnimatorOfFloat);
        }
        objectAnimatorOfFloat.addListener(new FadeAnimatorListener(view));
        addListener(new TransitionListenerAdapter() {
            @Override
            public void onTransitionEnd(Transition transition) {
                view.setTransitionAlpha(1.0f);
                transition.removeListener(this);
            }
        });
        return objectAnimatorOfFloat;
    }

    @Override
    public Animator onAppear(ViewGroup viewGroup, View view, TransitionValues transitionValues, TransitionValues transitionValues2) {
        if (DBG) {
            Log.d(LOG_TAG, "Fade.onAppear: startView, startVis, endView, endVis = " + (transitionValues != null ? transitionValues.view : null) + ", " + view);
        }
        float f = 0.0f;
        float startAlpha = getStartAlpha(transitionValues, 0.0f);
        if (startAlpha != 1.0f) {
            f = startAlpha;
        }
        return createAnimation(view, f, 1.0f);
    }

    @Override
    public Animator onDisappear(ViewGroup viewGroup, View view, TransitionValues transitionValues, TransitionValues transitionValues2) {
        return createAnimation(view, getStartAlpha(transitionValues, 1.0f), 0.0f);
    }

    private static float getStartAlpha(TransitionValues transitionValues, float f) {
        Float f2;
        if (transitionValues != null && (f2 = (Float) transitionValues.values.get(PROPNAME_TRANSITION_ALPHA)) != null) {
            return f2.floatValue();
        }
        return f;
    }

    private static class FadeAnimatorListener extends AnimatorListenerAdapter {
        private boolean mLayerTypeChanged = false;
        private final View mView;

        public FadeAnimatorListener(View view) {
            this.mView = view;
        }

        @Override
        public void onAnimationStart(Animator animator) {
            if (this.mView.hasOverlappingRendering() && this.mView.getLayerType() == 0) {
                this.mLayerTypeChanged = true;
                this.mView.setLayerType(2, null);
            }
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            this.mView.setTransitionAlpha(1.0f);
            if (this.mLayerTypeChanged) {
                this.mView.setLayerType(0, null);
            }
        }
    }
}
