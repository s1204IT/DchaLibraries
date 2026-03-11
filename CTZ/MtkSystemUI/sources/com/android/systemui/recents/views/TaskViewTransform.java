package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Property;
import android.view.View;
import com.android.systemui.shared.recents.utilities.AnimationProps;
import com.android.systemui.shared.recents.utilities.Utilities;
import java.util.ArrayList;

public class TaskViewTransform {
    public static final Property<View, Rect> LTRB = new Property<View, Rect>(Rect.class, "leftTopRightBottom") {
        private Rect mTmpRect = new Rect();

        @Override
        public void set(View view, Rect rect) {
            view.setLeftTopRightBottom(rect.left, rect.top, rect.right, rect.bottom);
        }

        @Override
        public Rect get(View view) {
            this.mTmpRect.set(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
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

    public void fillIn(TaskView taskView) {
        this.translationZ = taskView.getTranslationZ();
        this.scale = taskView.getScaleX();
        this.alpha = taskView.getAlpha();
        this.visible = true;
        this.dimAlpha = taskView.getDimAlpha();
        this.viewOutlineAlpha = taskView.getViewBounds().getAlpha();
        this.rect.set(taskView.getLeft(), taskView.getTop(), taskView.getRight(), taskView.getBottom());
    }

    public void copyFrom(TaskViewTransform taskViewTransform) {
        this.translationZ = taskViewTransform.translationZ;
        this.scale = taskViewTransform.scale;
        this.alpha = taskViewTransform.alpha;
        this.visible = taskViewTransform.visible;
        this.dimAlpha = taskViewTransform.dimAlpha;
        this.viewOutlineAlpha = taskViewTransform.viewOutlineAlpha;
        this.rect.set(taskViewTransform.rect);
    }

    public boolean isSame(TaskViewTransform taskViewTransform) {
        return this.translationZ == taskViewTransform.translationZ && this.scale == taskViewTransform.scale && taskViewTransform.alpha == this.alpha && this.dimAlpha == taskViewTransform.dimAlpha && this.visible == taskViewTransform.visible && this.rect.equals(taskViewTransform.rect);
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

    public boolean hasAlphaChangedFrom(float f) {
        return Float.compare(this.alpha, f) != 0;
    }

    public boolean hasScaleChangedFrom(float f) {
        return Float.compare(this.scale, f) != 0;
    }

    public boolean hasTranslationZChangedFrom(float f) {
        return Float.compare(this.translationZ, f) != 0;
    }

    public boolean hasRectChangedFrom(View view) {
        return (((int) this.rect.left) == view.getLeft() && ((int) this.rect.right) == view.getRight() && ((int) this.rect.top) == view.getTop() && ((int) this.rect.bottom) == view.getBottom()) ? false : true;
    }

    public void applyToTaskView(TaskView taskView, ArrayList<Animator> arrayList, AnimationProps animationProps, boolean z) {
        if (!this.visible) {
            return;
        }
        if (animationProps.isImmediate()) {
            if (z && hasTranslationZChangedFrom(taskView.getTranslationZ())) {
                taskView.setTranslationZ(this.translationZ);
            }
            if (hasScaleChangedFrom(taskView.getScaleX())) {
                taskView.setScaleX(this.scale);
                taskView.setScaleY(this.scale);
            }
            if (hasAlphaChangedFrom(taskView.getAlpha())) {
                taskView.setAlpha(this.alpha);
            }
            if (hasRectChangedFrom(taskView)) {
                taskView.setLeftTopRightBottom((int) this.rect.left, (int) this.rect.top, (int) this.rect.right, (int) this.rect.bottom);
                return;
            }
            return;
        }
        if (z && hasTranslationZChangedFrom(taskView.getTranslationZ())) {
            arrayList.add(animationProps.apply(3, ObjectAnimator.ofFloat(taskView, (Property<TaskView, Float>) View.TRANSLATION_Z, taskView.getTranslationZ(), this.translationZ)));
        }
        if (hasScaleChangedFrom(taskView.getScaleX())) {
            arrayList.add(animationProps.apply(5, ObjectAnimator.ofPropertyValuesHolder(taskView, PropertyValuesHolder.ofFloat((Property<?, Float>) View.SCALE_X, taskView.getScaleX(), this.scale), PropertyValuesHolder.ofFloat((Property<?, Float>) View.SCALE_Y, taskView.getScaleX(), this.scale))));
        }
        if (hasAlphaChangedFrom(taskView.getAlpha())) {
            arrayList.add(animationProps.apply(4, ObjectAnimator.ofFloat(taskView, (Property<TaskView, Float>) View.ALPHA, taskView.getAlpha(), this.alpha)));
        }
        if (hasRectChangedFrom(taskView)) {
            Rect rect = new Rect(taskView.getLeft(), taskView.getTop(), taskView.getRight(), taskView.getBottom());
            Rect rect2 = new Rect();
            this.rect.round(rect2);
            arrayList.add(animationProps.apply(6, ObjectAnimator.ofPropertyValuesHolder(taskView, PropertyValuesHolder.ofObject(LTRB, Utilities.RECT_EVALUATOR, rect, rect2))));
        }
    }

    public static void reset(TaskView taskView) {
        taskView.setTranslationX(0.0f);
        taskView.setTranslationY(0.0f);
        taskView.setTranslationZ(0.0f);
        taskView.setScaleX(1.0f);
        taskView.setScaleY(1.0f);
        taskView.setAlpha(1.0f);
        taskView.getViewBounds().setClipBottom(0);
        taskView.setLeftTopRightBottom(0, 0, 0, 0);
    }

    public String toString() {
        return "R: " + this.rect + " V: " + this.visible;
    }
}
