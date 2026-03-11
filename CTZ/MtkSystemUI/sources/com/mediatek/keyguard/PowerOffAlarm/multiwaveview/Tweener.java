package com.mediatek.keyguard.PowerOffAlarm.multiwaveview;

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
    ObjectAnimator animator;
    private static HashMap<Object, Tweener> sTweens = new HashMap<>();
    private static Animator.AnimatorListener mCleanupListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animator) {
            Tweener.remove(animator);
        }

        @Override
        public void onAnimationCancel(Animator animator) {
            Tweener.remove(animator);
        }
    };

    public Tweener(ObjectAnimator objectAnimator) {
        this.animator = objectAnimator;
    }

    public static void remove(Animator animator) {
        Iterator<Map.Entry<Object, Tweener>> it = sTweens.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().animator == animator) {
                it.remove();
                return;
            }
        }
    }

    public static Tweener to(Object obj, long j, Object... objArr) {
        Tweener tweener;
        ObjectAnimator objectAnimatorOfPropertyValuesHolder;
        ArrayList arrayList = new ArrayList(objArr.length / 2);
        ValueAnimator.AnimatorUpdateListener animatorUpdateListener = null;
        Animator.AnimatorListener animatorListener = null;
        long jLongValue = 0;
        TimeInterpolator timeInterpolator = null;
        for (int i = 0; i < objArr.length; i += 2) {
            if (!(objArr[i] instanceof String)) {
                throw new IllegalArgumentException("Key must be a string: " + objArr[i]);
            }
            String str = (String) objArr[i];
            Object obj2 = objArr[i + 1];
            if (!"simultaneousTween".equals(str)) {
                if ("ease".equals(str)) {
                    timeInterpolator = (TimeInterpolator) obj2;
                } else if ("onUpdate".equals(str) || "onUpdateListener".equals(str)) {
                    animatorUpdateListener = (ValueAnimator.AnimatorUpdateListener) obj2;
                } else if ("onComplete".equals(str) || "onCompleteListener".equals(str)) {
                    animatorListener = (Animator.AnimatorListener) obj2;
                } else if ("delay".equals(str)) {
                    jLongValue = ((Number) obj2).longValue();
                } else if ("syncWith".equals(str)) {
                    continue;
                } else if (obj2 instanceof float[]) {
                    float[] fArr = (float[]) obj2;
                    arrayList.add(PropertyValuesHolder.ofFloat(str, fArr[0], fArr[1]));
                } else if (obj2 instanceof int[]) {
                    int[] iArr = (int[]) obj2;
                    arrayList.add(PropertyValuesHolder.ofInt(str, iArr[0], iArr[1]));
                } else {
                    if (!(obj2 instanceof Number)) {
                        throw new IllegalArgumentException("Bad argument for key \"" + str + "\" with value " + obj2.getClass());
                    }
                    arrayList.add(PropertyValuesHolder.ofFloat(str, ((Number) obj2).floatValue()));
                }
            }
        }
        Tweener tweener2 = sTweens.get(obj);
        if (tweener2 == null) {
            objectAnimatorOfPropertyValuesHolder = ObjectAnimator.ofPropertyValuesHolder(obj, (PropertyValuesHolder[]) arrayList.toArray(new PropertyValuesHolder[arrayList.size()]));
            tweener = new Tweener(objectAnimatorOfPropertyValuesHolder);
            sTweens.put(obj, tweener);
        } else {
            ObjectAnimator objectAnimator = sTweens.get(obj).animator;
            replace(arrayList, obj);
            tweener = tweener2;
            objectAnimatorOfPropertyValuesHolder = objectAnimator;
        }
        if (timeInterpolator != null) {
            objectAnimatorOfPropertyValuesHolder.setInterpolator(timeInterpolator);
        }
        objectAnimatorOfPropertyValuesHolder.setStartDelay(jLongValue);
        objectAnimatorOfPropertyValuesHolder.setDuration(j);
        if (animatorUpdateListener != null) {
            objectAnimatorOfPropertyValuesHolder.removeAllUpdateListeners();
            objectAnimatorOfPropertyValuesHolder.addUpdateListener(animatorUpdateListener);
        }
        if (animatorListener != null) {
            objectAnimatorOfPropertyValuesHolder.removeAllListeners();
            objectAnimatorOfPropertyValuesHolder.addListener(animatorListener);
        }
        objectAnimatorOfPropertyValuesHolder.addListener(mCleanupListener);
        return tweener;
    }

    private static void replace(ArrayList<PropertyValuesHolder> arrayList, Object... objArr) {
        for (Object obj : objArr) {
            Tweener tweener = sTweens.get(obj);
            if (tweener != null) {
                tweener.animator.cancel();
                if (arrayList != null) {
                    tweener.animator.setValues((PropertyValuesHolder[]) arrayList.toArray(new PropertyValuesHolder[arrayList.size()]));
                } else {
                    sTweens.remove(tweener);
                }
            }
        }
    }
}
