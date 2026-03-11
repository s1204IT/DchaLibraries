package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.statusbar.notification.TransformState;
import java.util.Stack;

public class ViewTransformationHelper implements TransformableView {
    private ValueAnimator mViewTransformationAnimation;
    private ArrayMap<Integer, View> mTransformedViews = new ArrayMap<>();
    private ArrayMap<Integer, CustomTransformation> mCustomTransformations = new ArrayMap<>();

    public void addTransformedView(int key, View transformedView) {
        this.mTransformedViews.put(Integer.valueOf(key), transformedView);
    }

    public void reset() {
        this.mTransformedViews.clear();
    }

    public void setCustomTransformation(CustomTransformation transformation, int viewType) {
        this.mCustomTransformations.put(Integer.valueOf(viewType), transformation);
    }

    @Override
    public TransformState getCurrentState(int fadingView) {
        View view = this.mTransformedViews.get(Integer.valueOf(fadingView));
        if (view == null || view.getVisibility() == 8) {
            return null;
        }
        return TransformState.createFrom(view);
    }

    @Override
    public void transformTo(final TransformableView notification, final Runnable endRunnable) {
        if (this.mViewTransformationAnimation != null) {
            this.mViewTransformationAnimation.cancel();
        }
        this.mViewTransformationAnimation = ValueAnimator.ofFloat(0.0f, 1.0f);
        this.mViewTransformationAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                ViewTransformationHelper.this.transformTo(notification, animation.getAnimatedFraction());
            }
        });
        this.mViewTransformationAnimation.setInterpolator(Interpolators.LINEAR);
        this.mViewTransformationAnimation.setDuration(360L);
        this.mViewTransformationAnimation.addListener(new AnimatorListenerAdapter() {
            public boolean mCancelled;

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!this.mCancelled) {
                    if (endRunnable != null) {
                        endRunnable.run();
                    }
                    ViewTransformationHelper.this.setVisible(false);
                    return;
                }
                ViewTransformationHelper.this.abortTransformations();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                this.mCancelled = true;
            }
        });
        this.mViewTransformationAnimation.start();
    }

    @Override
    public void transformTo(TransformableView notification, float transformationAmount) {
        for (Integer viewType : this.mTransformedViews.keySet()) {
            TransformState ownState = getCurrentState(viewType.intValue());
            if (ownState != null) {
                CustomTransformation customTransformation = this.mCustomTransformations.get(viewType);
                if (customTransformation != null && customTransformation.transformTo(ownState, notification, transformationAmount)) {
                    ownState.recycle();
                } else {
                    TransformState otherState = notification.getCurrentState(viewType.intValue());
                    if (otherState != null) {
                        ownState.transformViewTo(otherState, transformationAmount);
                        otherState.recycle();
                    } else {
                        CrossFadeHelper.fadeOut(this.mTransformedViews.get(viewType), transformationAmount);
                    }
                    ownState.recycle();
                }
            }
        }
    }

    @Override
    public void transformFrom(final TransformableView notification) {
        if (this.mViewTransformationAnimation != null) {
            this.mViewTransformationAnimation.cancel();
        }
        this.mViewTransformationAnimation = ValueAnimator.ofFloat(0.0f, 1.0f);
        this.mViewTransformationAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                ViewTransformationHelper.this.transformFrom(notification, animation.getAnimatedFraction());
            }
        });
        this.mViewTransformationAnimation.addListener(new AnimatorListenerAdapter() {
            public boolean mCancelled;

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!this.mCancelled) {
                    ViewTransformationHelper.this.setVisible(true);
                } else {
                    ViewTransformationHelper.this.abortTransformations();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                this.mCancelled = true;
            }
        });
        this.mViewTransformationAnimation.setInterpolator(Interpolators.LINEAR);
        this.mViewTransformationAnimation.setDuration(360L);
        this.mViewTransformationAnimation.start();
    }

    @Override
    public void transformFrom(TransformableView notification, float transformationAmount) {
        for (Integer viewType : this.mTransformedViews.keySet()) {
            TransformState ownState = getCurrentState(viewType.intValue());
            if (ownState != null) {
                CustomTransformation customTransformation = this.mCustomTransformations.get(viewType);
                if (customTransformation != null && customTransformation.transformFrom(ownState, notification, transformationAmount)) {
                    ownState.recycle();
                } else {
                    TransformState otherState = notification.getCurrentState(viewType.intValue());
                    if (otherState != null) {
                        ownState.transformViewFrom(otherState, transformationAmount);
                        otherState.recycle();
                    } else {
                        if (transformationAmount == 0.0f) {
                            ownState.prepareFadeIn();
                        }
                        CrossFadeHelper.fadeIn(this.mTransformedViews.get(viewType), transformationAmount);
                    }
                    ownState.recycle();
                }
            }
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if (this.mViewTransformationAnimation != null) {
            this.mViewTransformationAnimation.cancel();
        }
        for (Integer viewType : this.mTransformedViews.keySet()) {
            TransformState ownState = getCurrentState(viewType.intValue());
            if (ownState != null) {
                ownState.setVisible(visible, false);
                ownState.recycle();
            }
        }
    }

    public void abortTransformations() {
        for (Integer viewType : this.mTransformedViews.keySet()) {
            TransformState ownState = getCurrentState(viewType.intValue());
            if (ownState != null) {
                ownState.abortTransformation();
                ownState.recycle();
            }
        }
    }

    public void addRemainingTransformTypes(View viewRoot) {
        int id;
        int numValues = this.mTransformedViews.size();
        for (int i = 0; i < numValues; i++) {
            Object objValueAt = this.mTransformedViews.valueAt(i);
            while (true) {
                View view = (View) objValueAt;
                if (view != viewRoot.getParent()) {
                    view.setTag(R.id.contains_transformed_view, true);
                    objValueAt = view.getParent();
                }
            }
        }
        Stack<View> stack = new Stack<>();
        stack.push(viewRoot);
        while (!stack.isEmpty()) {
            View child = stack.pop();
            if (child.getVisibility() != 8) {
                Boolean containsView = (Boolean) child.getTag(R.id.contains_transformed_view);
                if (containsView == null && (id = child.getId()) != -1) {
                    addTransformedView(id, child);
                } else {
                    child.setTag(R.id.contains_transformed_view, null);
                    if ((child instanceof ViewGroup) && !this.mTransformedViews.containsValue(child)) {
                        ViewGroup group = (ViewGroup) child;
                        for (int i2 = 0; i2 < group.getChildCount(); i2++) {
                            stack.push(group.getChildAt(i2));
                        }
                    }
                }
            }
        }
    }

    public void resetTransformedView(View view) {
        TransformState state = TransformState.createFrom(view);
        state.setVisible(true, true);
        state.recycle();
    }

    public ArraySet<View> getAllTransformingViews() {
        return new ArraySet<>(this.mTransformedViews.values());
    }

    public static abstract class CustomTransformation {
        public abstract boolean transformFrom(TransformState transformState, TransformableView transformableView, float f);

        public abstract boolean transformTo(TransformState transformState, TransformableView transformableView, float f);

        public boolean initTransformation(TransformState ownState, TransformState otherState) {
            return false;
        }

        public boolean customTransformTarget(TransformState ownState, TransformState otherState) {
            return false;
        }
    }
}
