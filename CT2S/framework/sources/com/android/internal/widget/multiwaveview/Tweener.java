package com.android.internal.widget.multiwaveview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class Tweener {
    private static final boolean DEBUG = false;
    private static final String TAG = "Tweener";
    ObjectAnimator animator;
    private static HashMap<Object, Tweener> sTweens = new HashMap<>();
    private static Animator.AnimatorListener mCleanupListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            Tweener.remove(animation);
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            Tweener.remove(animation);
        }
    };

    public Tweener(ObjectAnimator anim) {
        this.animator = anim;
    }

    private static void remove(Animator animator) {
        Iterator<Map.Entry<Object, Tweener>> iter = sTweens.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Object, Tweener> entry = iter.next();
            if (entry.getValue().animator == animator) {
                iter.remove();
                return;
            }
        }
    }

    public static Tweener to(Object object, long duration, Object... vars) {
        ObjectAnimator anim;
        long delay = 0;
        ValueAnimator.AnimatorUpdateListener updateListener = null;
        Animator.AnimatorListener listener = null;
        TimeInterpolator interpolator = null;
        ArrayList<PropertyValuesHolder> props = new ArrayList<>(vars.length / 2);
        for (int i = 0; i < vars.length; i += 2) {
            if (!(vars[i] instanceof String)) {
                throw new IllegalArgumentException("Key must be a string: " + vars[i]);
            }
            String key = (String) vars[i];
            Object value = vars[i + 1];
            if (!"simultaneousTween".equals(key)) {
                if ("ease".equals(key)) {
                    interpolator = (TimeInterpolator) value;
                } else if ("onUpdate".equals(key) || "onUpdateListener".equals(key)) {
                    updateListener = (ValueAnimator.AnimatorUpdateListener) value;
                } else if ("onComplete".equals(key) || "onCompleteListener".equals(key)) {
                    listener = (Animator.AnimatorListener) value;
                } else if ("delay".equals(key)) {
                    delay = ((Number) value).longValue();
                } else if ("syncWith".equals(key)) {
                    continue;
                } else if (value instanceof float[]) {
                    props.add(PropertyValuesHolder.ofFloat(key, ((float[]) value)[0], ((float[]) value)[1]));
                } else if (value instanceof int[]) {
                    props.add(PropertyValuesHolder.ofInt(key, ((int[]) value)[0], ((int[]) value)[1]));
                } else if (value instanceof Number) {
                    float floatValue = ((Number) value).floatValue();
                    props.add(PropertyValuesHolder.ofFloat(key, floatValue));
                } else {
                    throw new IllegalArgumentException("Bad argument for key \"" + key + "\" with value " + value.getClass());
                }
            }
        }
        Tweener tween = sTweens.get(object);
        if (tween == null) {
            anim = ObjectAnimator.ofPropertyValuesHolder(object, (PropertyValuesHolder[]) props.toArray(new PropertyValuesHolder[props.size()]));
            tween = new Tweener(anim);
            sTweens.put(object, tween);
        } else {
            anim = sTweens.get(object).animator;
            replace(props, object);
        }
        if (interpolator != null) {
            anim.setInterpolator(interpolator);
        }
        anim.setStartDelay(delay);
        anim.setDuration(duration);
        if (updateListener != null) {
            anim.removeAllUpdateListeners();
            anim.addUpdateListener(updateListener);
        }
        if (listener != null) {
            anim.removeAllListeners();
            anim.addListener(listener);
        }
        anim.addListener(mCleanupListener);
        return tween;
    }

    Tweener from(Object object, long duration, Object... vars) {
        return to(object, duration, vars);
    }

    public static void reset() {
        sTweens.clear();
    }

    private static void replace(ArrayList<PropertyValuesHolder> props, Object... args) {
        for (Object killobject : args) {
            Tweener tween = sTweens.get(killobject);
            if (tween != null) {
                tween.animator.cancel();
                if (props != null) {
                    tween.animator.setValues((PropertyValuesHolder[]) props.toArray(new PropertyValuesHolder[props.size()]));
                } else {
                    sTweens.remove(tween);
                }
            }
        }
    }
}
