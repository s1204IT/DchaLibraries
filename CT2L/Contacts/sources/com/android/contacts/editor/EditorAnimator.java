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
import android.widget.ScrollView;
import com.android.contacts.util.SchedulingUtils;
import com.google.common.collect.Lists;
import java.util.List;

public class EditorAnimator {
    private static EditorAnimator sInstance = new EditorAnimator();
    private AnimatorRunner mRunner = new AnimatorRunner();

    public static EditorAnimator getInstance() {
        return sInstance;
    }

    private EditorAnimator() {
    }

    public void removeEditorView(final View victim) {
        this.mRunner.endOldAnimation();
        int offset = victim.getHeight();
        final List<View> viewsToMove = getViewsBelowOf(victim);
        List<Animator> animators = Lists.newArrayList();
        ObjectAnimator fadeOutAnimator = ObjectAnimator.ofFloat(victim, (Property<View, Float>) View.ALPHA, 1.0f, 0.0f);
        fadeOutAnimator.setDuration(200L);
        animators.add(fadeOutAnimator);
        translateViews(animators, viewsToMove, 0.0f, -offset, 100, 200);
        this.mRunner.run(animators, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                for (int i = 0; i < viewsToMove.size(); i++) {
                    View view = (View) viewsToMove.get(i);
                    view.setTranslationY(0.0f);
                }
                ViewGroup victimParent = (ViewGroup) victim.getParent();
                if (victimParent != null) {
                    victimParent.removeView(victim);
                }
            }
        });
    }

    public void slideAndFadeIn(final ViewGroup target, final int previousHeight) {
        this.mRunner.endOldAnimation();
        target.setVisibility(0);
        target.setAlpha(0.0f);
        SchedulingUtils.doAfterLayout(target, new Runnable() {
            @Override
            public void run() {
                int offset = target.getHeight() - previousHeight;
                List<Animator> animators = Lists.newArrayList();
                List<View> viewsToMove = EditorAnimator.getViewsBelowOf(target);
                EditorAnimator.translateViews(animators, viewsToMove, -offset, 0.0f, 0, 200);
                ObjectAnimator fadeInAnimator = ObjectAnimator.ofFloat(target, (Property<ViewGroup, Float>) View.ALPHA, 0.0f, 1.0f);
                fadeInAnimator.setDuration(200L);
                fadeInAnimator.setStartDelay(200L);
                animators.add(fadeInAnimator);
                EditorAnimator.this.mRunner.run(animators);
            }
        });
    }

    public void showFieldFooter(final View view) {
        this.mRunner.endOldAnimation();
        if (view.getVisibility() != 0) {
            view.setVisibility(0);
            view.setAlpha(0.0f);
            SchedulingUtils.doAfterLayout(view, new Runnable() {
                @Override
                public void run() {
                    int offset = view.getHeight();
                    List<Animator> animators = Lists.newArrayList();
                    List<View> viewsToMove = EditorAnimator.getViewsBelowOf(view);
                    EditorAnimator.translateViews(animators, viewsToMove, -offset, 0.0f, 0, 200);
                    ObjectAnimator fadeInAnimator = ObjectAnimator.ofFloat(view, (Property<View, Float>) View.ALPHA, 0.0f, 1.0f);
                    fadeInAnimator.setDuration(200L);
                    fadeInAnimator.setStartDelay(200L);
                    animators.add(fadeInAnimator);
                    EditorAnimator.this.mRunner.run(animators);
                }
            });
        }
    }

    public void scrollViewToTop(final View targetView) {
        ScrollView scrollView = getParentScrollView(targetView);
        SchedulingUtils.doAfterLayout(scrollView, new Runnable() {
            @Override
            public void run() {
                ScrollView scrollView2 = EditorAnimator.getParentScrollView(targetView);
                scrollView2.smoothScrollTo(0, EditorAnimator.this.offsetFromTopOfViewGroup(targetView, scrollView2) + scrollView2.getScrollY());
            }
        });
        View view = scrollView.findFocus();
        if (view != null) {
            view.clearFocus();
        }
    }

    public static void placeFocusAtTopOfScreenAfterReLayout(final View view) {
        SchedulingUtils.doAfterLayout(view, new Runnable() {
            @Override
            public void run() {
                EditorAnimator.getParentScrollView(view).clearFocus();
            }
        });
    }

    private int offsetFromTopOfViewGroup(View view, ViewGroup viewGroup) {
        int[] viewLocation = new int[2];
        int[] viewGroupLocation = new int[2];
        viewGroup.getLocationOnScreen(viewGroupLocation);
        view.getLocationOnScreen(viewLocation);
        return viewLocation[1] - viewGroupLocation[1];
    }

    private static ScrollView getParentScrollView(View view) {
        while (true) {
            Object parent = view.getParent();
            if (parent instanceof ScrollView) {
                return (ScrollView) parent;
            }
            if (!(parent instanceof View)) {
                throw new IllegalArgumentException("The editor should be contained inside a ScrollView.");
            }
            view = (View) parent;
        }
    }

    private static void translateViews(List<Animator> animators, List<View> views, float fromY, float toY, int startDelay, int duration) {
        for (int i = 0; i < views.size(); i++) {
            View child = views.get(i);
            ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(child, (Property<View, Float>) View.TRANSLATION_Y, fromY, toY);
            translateAnimator.setStartDelay(startDelay);
            translateAnimator.setDuration(duration);
            animators.add(translateAnimator);
        }
    }

    private static List<View> getViewsBelowOf(View view) {
        ViewGroup victimParent = (ViewGroup) view.getParent();
        List<View> result = Lists.newArrayList();
        if (victimParent != null) {
            int index = victimParent.indexOfChild(view);
            getViewsBelowOfRecursive(result, victimParent, index + 1, view);
        }
        return result;
    }

    private static void getViewsBelowOfRecursive(List<View> result, ViewGroup container, int index, View target) {
        for (int i = index; i < container.getChildCount(); i++) {
            View view = container.getChildAt(i);
            if (view.getY() > target.getY() + (target.getHeight() / 2)) {
                result.add(view);
            }
        }
        ViewParent parent = container.getParent();
        if (parent instanceof LinearLayout) {
            LinearLayout parentLayout = (LinearLayout) parent;
            int containerIndex = parentLayout.indexOfChild(container);
            getViewsBelowOfRecursive(result, parentLayout, containerIndex + 1, target);
        }
    }

    static class AnimatorRunner extends AnimatorListenerAdapter {
        private Animator mLastAnimator;

        AnimatorRunner() {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            this.mLastAnimator = null;
        }

        public void run(List<Animator> animators) {
            run(animators, null);
        }

        public void run(List<Animator> animators, Animator.AnimatorListener listener) {
            AnimatorSet set = new AnimatorSet();
            set.playTogether(animators);
            if (listener != null) {
                set.addListener(listener);
            }
            set.addListener(this);
            this.mLastAnimator = set;
            set.start();
        }

        public void endOldAnimation() {
            if (this.mLastAnimator != null) {
                this.mLastAnimator.end();
            }
        }
    }
}
