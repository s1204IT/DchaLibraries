package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Property;
import android.view.View;
import com.android.systemui.recents.misc.Utilities;
import java.util.ArrayList;

public class TaskViewTransform {
    public static final Property<View, Rect> LTRB = new Property<View, Rect>(Rect.class, "leftTopRightBottom") {
        private Rect mTmpRect = new Rect();

        @Override
        public void set(View v, Rect ltrb) {
            v.setLeftTopRightBottom(ltrb.left, ltrb.top, ltrb.right, ltrb.bottom);
        }

        @Override
        public Rect get(View v) {
            this.mTmpRect.set(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
            return this.mTmpRect;
        }
    };
    public float translationZ = 0.0f;
    public float scale = 1.0f;
    public float alpha = 1.0f;
    public float dimAlpha = 0.0f;
    public float viewOutlineAlpha = 0.0f;
    public boolean visible = false;
    public RectF rect = new RectF();

    public void fillIn(TaskView tv) {
        this.translationZ = tv.getTranslationZ();
        this.scale = tv.getScaleX();
        this.alpha = tv.getAlpha();
        this.visible = true;
        this.dimAlpha = tv.getDimAlpha();
        this.viewOutlineAlpha = tv.getViewBounds().getAlpha();
        this.rect.set(tv.getLeft(), tv.getTop(), tv.getRight(), tv.getBottom());
    }

    public void copyFrom(TaskViewTransform other) {
        this.translationZ = other.translationZ;
        this.scale = other.scale;
        this.alpha = other.alpha;
        this.visible = other.visible;
        this.dimAlpha = other.dimAlpha;
        this.viewOutlineAlpha = other.viewOutlineAlpha;
        this.rect.set(other.rect);
    }

    public boolean isSame(TaskViewTransform other) {
        if (this.translationZ == other.translationZ && this.scale == other.scale && other.alpha == this.alpha && this.dimAlpha == other.dimAlpha && this.visible == other.visible) {
            return this.rect.equals(other.rect);
        }
        return false;
    }

    public void reset() {
        this.translationZ = 0.0f;
        this.scale = 1.0f;
        this.alpha = 1.0f;
        this.dimAlpha = 0.0f;
        this.viewOutlineAlpha = 0.0f;
        this.visible = false;
        this.rect.setEmpty();
    }

    public boolean hasAlphaChangedFrom(float v) {
        return Float.compare(this.alpha, v) != 0;
    }

    public boolean hasScaleChangedFrom(float v) {
        return Float.compare(this.scale, v) != 0;
    }

    public boolean hasTranslationZChangedFrom(float v) {
        return Float.compare(this.translationZ, v) != 0;
    }

    public boolean hasRectChangedFrom(View v) {
        return (((int) this.rect.left) == v.getLeft() && ((int) this.rect.right) == v.getRight() && ((int) this.rect.top) == v.getTop() && ((int) this.rect.bottom) == v.getBottom()) ? false : true;
    }

    public void applyToTaskView(TaskView v, ArrayList<Animator> animators, AnimationProps animation, boolean allowShadows) {
        if (!this.visible) {
            return;
        }
        if (animation.isImmediate()) {
            if (allowShadows && hasTranslationZChangedFrom(v.getTranslationZ())) {
                v.setTranslationZ(this.translationZ);
            }
            if (hasScaleChangedFrom(v.getScaleX())) {
                v.setScaleX(this.scale);
                v.setScaleY(this.scale);
            }
            if (hasAlphaChangedFrom(v.getAlpha())) {
                v.setAlpha(this.alpha);
            }
            if (!hasRectChangedFrom(v)) {
                return;
            }
            v.setLeftTopRightBottom((int) this.rect.left, (int) this.rect.top, (int) this.rect.right, (int) this.rect.bottom);
            return;
        }
        if (allowShadows && hasTranslationZChangedFrom(v.getTranslationZ())) {
            ObjectAnimator anim = ObjectAnimator.ofFloat(v, (Property<TaskView, Float>) View.TRANSLATION_Z, v.getTranslationZ(), this.translationZ);
            animators.add(animation.apply(3, anim));
        }
        if (hasScaleChangedFrom(v.getScaleX())) {
            ObjectAnimator anim2 = ObjectAnimator.ofPropertyValuesHolder(v, PropertyValuesHolder.ofFloat((Property<?, Float>) View.SCALE_X, v.getScaleX(), this.scale), PropertyValuesHolder.ofFloat((Property<?, Float>) View.SCALE_Y, v.getScaleX(), this.scale));
            animators.add(animation.apply(5, anim2));
        }
        if (hasAlphaChangedFrom(v.getAlpha())) {
            ObjectAnimator anim3 = ObjectAnimator.ofFloat(v, (Property<TaskView, Float>) View.ALPHA, v.getAlpha(), this.alpha);
            animators.add(animation.apply(4, anim3));
        }
        if (!hasRectChangedFrom(v)) {
            return;
        }
        Rect fromViewRect = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
        Rect toViewRect = new Rect();
        this.rect.round(toViewRect);
        ObjectAnimator anim4 = ObjectAnimator.ofPropertyValuesHolder(v, PropertyValuesHolder.ofObject(LTRB, Utilities.RECT_EVALUATOR, fromViewRect, toViewRect));
        animators.add(animation.apply(6, anim4));
    }

    public static void reset(TaskView v) {
        v.setTranslationX(0.0f);
        v.setTranslationY(0.0f);
        v.setTranslationZ(0.0f);
        v.setScaleX(1.0f);
        v.setScaleY(1.0f);
        v.setAlpha(1.0f);
        v.getViewBounds().setClipBottom(0);
        v.setLeftTopRightBottom(0, 0, 0, 0);
    }

    public String toString() {
        return "R: " + this.rect + " V: " + this.visible;
    }
}
