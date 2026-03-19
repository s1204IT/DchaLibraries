package android.support.design.internal;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.support.annotation.RestrictTo;
import android.support.transition.Transition;
import android.support.transition.TransitionValues;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.Map;

@RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
public class TextScale extends Transition {
    private static final String PROPNAME_SCALE = "android:textscale:scale";

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    private void captureValues(TransitionValues transitionValues) {
        if (transitionValues.view instanceof TextView) {
            TextView textview = (TextView) transitionValues.view;
            transitionValues.values.put(PROPNAME_SCALE, Float.valueOf(textview.getScaleX()));
        }
    }

    @Override
    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues) {
        if (startValues == null || endValues == null || !(startValues.view instanceof TextView) || !(endValues.view instanceof TextView)) {
            return null;
        }
        final TextView view = (TextView) endValues.view;
        Map<String, Object> startVals = startValues.values;
        Map<String, Object> endVals = endValues.values;
        float startSize = startVals.get(PROPNAME_SCALE) != null ? ((Float) startVals.get(PROPNAME_SCALE)).floatValue() : 1.0f;
        float endSize = endVals.get(PROPNAME_SCALE) != null ? ((Float) endVals.get(PROPNAME_SCALE)).floatValue() : 1.0f;
        if (startSize == endSize) {
            return null;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(startSize, endSize);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float animatedValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                view.setScaleX(animatedValue);
                view.setScaleY(animatedValue);
            }
        });
        return animator;
    }
}
