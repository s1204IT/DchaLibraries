package com.android.systemui.shared.recents.utilities;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.RectEvaluator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.ArraySet;
import android.util.IntProperty;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewStub;
import java.util.ArrayList;
import java.util.Collections;

/* loaded from: classes.dex */
public class Utilities {
    public static final Property<Drawable, Integer> DRAWABLE_ALPHA = new IntProperty<Drawable>("drawableAlpha") { // from class: com.android.systemui.shared.recents.utilities.Utilities.1
        /* JADX DEBUG: Method merged with bridge method: setValue(Ljava/lang/Object;I)V */
        @Override // android.util.IntProperty
        public void setValue(Drawable drawable, int i) {
            drawable.setAlpha(i);
        }

        /* JADX DEBUG: Method merged with bridge method: get(Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.util.Property
        public Integer get(Drawable drawable) {
            return Integer.valueOf(drawable.getAlpha());
        }
    };
    public static final Property<Drawable, Rect> DRAWABLE_RECT = new Property<Drawable, Rect>(Rect.class, "drawableBounds") { // from class: com.android.systemui.shared.recents.utilities.Utilities.2
        /* JADX DEBUG: Method merged with bridge method: set(Ljava/lang/Object;Ljava/lang/Object;)V */
        @Override // android.util.Property
        public void set(Drawable drawable, Rect rect) {
            drawable.setBounds(rect);
        }

        /* JADX DEBUG: Method merged with bridge method: get(Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.util.Property
        public Rect get(Drawable drawable) {
            return drawable.getBounds();
        }
    };
    public static final RectFEvaluator RECTF_EVALUATOR = new RectFEvaluator();
    public static final RectEvaluator RECT_EVALUATOR = new RectEvaluator(new Rect());

    public static <T extends View> T findParent(View view, Class<T> cls) {
        for (ViewParent parent = view.getParent(); parent != null; parent = parent.getParent()) {
            if (cls.isAssignableFrom(parent.getClass())) {
                return (T) parent;
            }
        }
        return null;
    }

    public static <T> ArraySet<T> arrayToSet(T[] tArr, ArraySet<T> arraySet) {
        arraySet.clear();
        if (tArr != null) {
            Collections.addAll(arraySet, tArr);
        }
        return arraySet;
    }

    public static float clamp(float f, float f2, float f3) {
        return Math.max(f2, Math.min(f3, f));
    }

    public static int clamp(int i, int i2, int i3) {
        return Math.max(i2, Math.min(i3, i));
    }

    public static float clamp01(float f) {
        return Math.max(0.0f, Math.min(1.0f, f));
    }

    public static float mapRange(float f, float f2, float f3) {
        return f2 + (f * (f3 - f2));
    }

    public static float unmapRange(float f, float f2, float f3) {
        return (f - f2) / (f3 - f2);
    }

    public static void scaleRectAboutCenter(RectF rectF, float f) {
        if (f != 1.0f) {
            float fCenterX = rectF.centerX();
            float fCenterY = rectF.centerY();
            rectF.offset(-fCenterX, -fCenterY);
            rectF.left *= f;
            rectF.top *= f;
            rectF.right *= f;
            rectF.bottom *= f;
            rectF.offset(fCenterX, fCenterY);
        }
    }

    public static float computeContrastBetweenColors(int i, int i2) {
        float fRed = Color.red(i) / 255.0f;
        float fGreen = Color.green(i) / 255.0f;
        float fBlue = Color.blue(i) / 255.0f;
        float fPow = ((fRed < 0.03928f ? fRed / 12.92f : (float) Math.pow((fRed + 0.055f) / 1.055f, 2.4000000953674316d)) * 0.2126f) + ((fGreen < 0.03928f ? fGreen / 12.92f : (float) Math.pow((fGreen + 0.055f) / 1.055f, 2.4000000953674316d)) * 0.7152f) + ((fBlue < 0.03928f ? fBlue / 12.92f : (float) Math.pow((fBlue + 0.055f) / 1.055f, 2.4000000953674316d)) * 0.0722f);
        float fRed2 = Color.red(i2) / 255.0f;
        float fGreen2 = Color.green(i2) / 255.0f;
        float fBlue2 = Color.blue(i2) / 255.0f;
        return Math.abs(((((0.2126f * (fRed2 < 0.03928f ? fRed2 / 12.92f : (float) Math.pow((fRed2 + 0.055f) / 1.055f, 2.4000000953674316d))) + (0.7152f * (fGreen2 < 0.03928f ? fGreen2 / 12.92f : (float) Math.pow((fGreen2 + 0.055f) / 1.055f, 2.4000000953674316d)))) + (0.0722f * (fBlue2 < 0.03928f ? fBlue2 / 12.92f : (float) Math.pow((fBlue2 + 0.055f) / 1.055f, 2.4000000953674316d)))) + 0.05f) / (fPow + 0.05f));
    }

    public static int getColorWithOverlay(int i, int i2, float f) {
        float f2 = 1.0f - f;
        return Color.rgb((int) ((Color.red(i) * f) + (Color.red(i2) * f2)), (int) ((Color.green(i) * f) + (Color.green(i2) * f2)), (int) ((f * Color.blue(i)) + (f2 * Color.blue(i2))));
    }

    public static void cancelAnimationWithoutCallbacks(Animator animator) {
        if (animator != null && animator.isStarted()) {
            removeAnimationListenersRecursive(animator);
            animator.cancel();
        }
    }

    public static void removeAnimationListenersRecursive(Animator animator) {
        if (animator instanceof AnimatorSet) {
            ArrayList<Animator> childAnimations = ((AnimatorSet) animator).getChildAnimations();
            for (int size = childAnimations.size() - 1; size >= 0; size--) {
                removeAnimationListenersRecursive(childAnimations.get(size));
            }
        }
        animator.removeAllListeners();
    }

    public static void setViewFrameFromTranslation(View view) {
        RectF rectF = new RectF(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
        rectF.offset(view.getTranslationX(), view.getTranslationY());
        view.setTranslationX(0.0f);
        view.setTranslationY(0.0f);
        view.setLeftTopRightBottom((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
    }

    public static ViewStub findViewStubById(View view, int i) {
        return (ViewStub) view.findViewById(i);
    }

    public static ViewStub findViewStubById(Activity activity, int i) {
        return (ViewStub) activity.findViewById(i);
    }

    public static boolean isDescendentAccessibilityFocused(View view) {
        if (view.isAccessibilityFocused()) {
            return true;
        }
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            int childCount = viewGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                if (isDescendentAccessibilityFocused(viewGroup.getChildAt(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Configuration getAppConfiguration(Context context) {
        return context.getApplicationContext().getResources().getConfiguration();
    }

    public static String dumpRect(Rect rect) {
        if (rect == null) {
            return "N:0,0-0,0";
        }
        return rect.left + "," + rect.top + "-" + rect.right + "," + rect.bottom;
    }
}
