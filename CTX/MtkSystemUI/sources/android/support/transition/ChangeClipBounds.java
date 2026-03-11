package android.support.transition;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.content.Context;
import android.graphics.Rect;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;

public class ChangeClipBounds extends Transition {
    private static final String[] sTransitionProperties = {"android:clipBounds:clip"};

    @Override
    public String[] getTransitionProperties() {
        return sTransitionProperties;
    }

    public ChangeClipBounds() {
    }

    public ChangeClipBounds(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private void captureValues(TransitionValues values) {
        View view = values.view;
        if (view.getVisibility() == 8) {
            return;
        }
        Rect clip = ViewCompat.getClipBounds(view);
        values.values.put("android:clipBounds:clip", clip);
        if (clip == null) {
            Rect bounds = new Rect(0, 0, view.getWidth(), view.getHeight());
            values.values.put("android:clipBounds:bounds", bounds);
        }
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    @Override
    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues) {
        if (startValues == null || endValues == null || !startValues.values.containsKey("android:clipBounds:clip") || !endValues.values.containsKey("android:clipBounds:clip")) {
            return null;
        }
        Rect start = (Rect) startValues.values.get("android:clipBounds:clip");
        Rect end = (Rect) endValues.values.get("android:clipBounds:clip");
        boolean endIsNull = end == null;
        if (start == null && end == null) {
            return null;
        }
        if (start == null) {
            start = (Rect) startValues.values.get("android:clipBounds:bounds");
        } else if (end == null) {
            end = (Rect) endValues.values.get("android:clipBounds:bounds");
        }
        if (start.equals(end)) {
            return null;
        }
        ViewCompat.setClipBounds(endValues.view, start);
        RectEvaluator evaluator = new RectEvaluator(new Rect());
        ObjectAnimator animator = ObjectAnimator.ofObject(endValues.view, (Property<View, V>) ViewUtils.CLIP_BOUNDS, (TypeEvaluator) evaluator, (Object[]) new Rect[]{start, end});
        if (endIsNull) {
            final View endView = endValues.view;
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    ViewCompat.setClipBounds(endView, null);
                }
            });
        }
        return animator;
    }
}
