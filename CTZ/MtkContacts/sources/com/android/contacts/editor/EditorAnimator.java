package com.android.contacts.editor;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.util.SchedulingUtils;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;

public class EditorAnimator {
    private static EditorAnimator sInstance = new EditorAnimator();
    private AnimatorRunner mRunner = new AnimatorRunner();

    public static EditorAnimator getInstance() {
        return sInstance;
    }

    private EditorAnimator() {
    }

    public void removeEditorView(final View view) {
        this.mRunner.endOldAnimation();
        int height = view.getHeight();
        final List<View> viewsBelowOf = getViewsBelowOf(view);
        ArrayList arrayListNewArrayList = Lists.newArrayList();
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(view, (Property<View, Float>) View.ALPHA, 1.0f, ContactPhotoManager.OFFSET_DEFAULT);
        objectAnimatorOfFloat.setDuration(200L);
        arrayListNewArrayList.add(objectAnimatorOfFloat);
        translateViews(arrayListNewArrayList, viewsBelowOf, ContactPhotoManager.OFFSET_DEFAULT, -height, 100, 200);
        this.mRunner.run(arrayListNewArrayList, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                for (int i = 0; i < viewsBelowOf.size(); i++) {
                    ((View) viewsBelowOf.get(i)).setTranslationY(ContactPhotoManager.OFFSET_DEFAULT);
                }
                ViewGroup viewGroup = (ViewGroup) view.getParent();
                if (viewGroup != null) {
                    viewGroup.removeView(view);
                }
            }
        });
    }

    public void slideAndFadeIn(final ViewGroup viewGroup, final int i) {
        this.mRunner.endOldAnimation();
        viewGroup.setVisibility(0);
        viewGroup.setAlpha(ContactPhotoManager.OFFSET_DEFAULT);
        SchedulingUtils.doAfterLayout(viewGroup, new Runnable() {
            @Override
            public void run() {
                int height = viewGroup.getHeight() - i;
                ArrayList arrayListNewArrayList = Lists.newArrayList();
                EditorAnimator.translateViews(arrayListNewArrayList, EditorAnimator.getViewsBelowOf(viewGroup), -height, ContactPhotoManager.OFFSET_DEFAULT, 0, 200);
                ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(viewGroup, (Property<ViewGroup, Float>) View.ALPHA, ContactPhotoManager.OFFSET_DEFAULT, 1.0f);
                objectAnimatorOfFloat.setDuration(200L);
                objectAnimatorOfFloat.setStartDelay(200L);
                arrayListNewArrayList.add(objectAnimatorOfFloat);
                EditorAnimator.this.mRunner.run(arrayListNewArrayList);
            }
        });
    }

    public void showFieldFooter(final View view) {
        this.mRunner.endOldAnimation();
        if (view.getVisibility() == 0) {
            return;
        }
        view.setVisibility(0);
        view.setAlpha(ContactPhotoManager.OFFSET_DEFAULT);
        SchedulingUtils.doAfterLayout(view, new Runnable() {
            @Override
            public void run() {
                int height = view.getHeight();
                ArrayList arrayListNewArrayList = Lists.newArrayList();
                EditorAnimator.translateViews(arrayListNewArrayList, EditorAnimator.getViewsBelowOf(view), -height, ContactPhotoManager.OFFSET_DEFAULT, 0, 200);
                ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(view, (Property<View, Float>) View.ALPHA, ContactPhotoManager.OFFSET_DEFAULT, 1.0f);
                objectAnimatorOfFloat.setDuration(200L);
                objectAnimatorOfFloat.setStartDelay(200L);
                arrayListNewArrayList.add(objectAnimatorOfFloat);
                EditorAnimator.this.mRunner.run(arrayListNewArrayList);
            }
        });
    }

    private static void translateViews(List<Animator> list, List<View> list2, float f, float f2, int i, int i2) {
        for (int i3 = 0; i3 < list2.size(); i3++) {
            ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(list2.get(i3), (Property<View, Float>) View.TRANSLATION_Y, f, f2);
            objectAnimatorOfFloat.setStartDelay(i);
            objectAnimatorOfFloat.setDuration(i2);
            list.add(objectAnimatorOfFloat);
        }
    }

    private static List<View> getViewsBelowOf(View view) {
        ViewGroup viewGroup = (ViewGroup) view.getParent();
        ArrayList arrayListNewArrayList = Lists.newArrayList();
        if (viewGroup != null) {
            getViewsBelowOfRecursive(arrayListNewArrayList, viewGroup, viewGroup.indexOfChild(view) + 1, view);
        }
        return arrayListNewArrayList;
    }

    private static void getViewsBelowOfRecursive(List<View> list, ViewGroup viewGroup, int i, View view) {
        while (i < viewGroup.getChildCount()) {
            View childAt = viewGroup.getChildAt(i);
            if (childAt.getY() > view.getY() + (view.getHeight() / 2)) {
                list.add(childAt);
            }
            i++;
        }
        ViewParent parent = viewGroup.getParent();
        if (parent instanceof LinearLayout) {
            LinearLayout linearLayout = (LinearLayout) parent;
            getViewsBelowOfRecursive(list, linearLayout, linearLayout.indexOfChild(viewGroup) + 1, view);
        }
    }

    static class AnimatorRunner extends AnimatorListenerAdapter {
        private Animator mLastAnimator;

        AnimatorRunner() {
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            this.mLastAnimator = null;
        }

        public void run(List<Animator> list) {
            run(list, null);
        }

        public void run(List<Animator> list, Animator.AnimatorListener animatorListener) {
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(list);
            if (animatorListener != null) {
                animatorSet.addListener(animatorListener);
            }
            animatorSet.addListener(this);
            this.mLastAnimator = animatorSet;
            animatorSet.start();
        }

        public void endOldAnimation() {
            if (this.mLastAnimator != null) {
                this.mLastAnimator.end();
            }
        }
    }
}
