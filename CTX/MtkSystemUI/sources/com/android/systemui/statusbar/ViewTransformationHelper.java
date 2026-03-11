package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.statusbar.notification.TransformState;
import java.util.Iterator;
import java.util.Stack;

public class ViewTransformationHelper implements TransformableView, TransformState.TransformInfo {
    private ValueAnimator mViewTransformationAnimation;
    private ArrayMap<Integer, View> mTransformedViews = new ArrayMap<>();
    private ArrayMap<Integer, CustomTransformation> mCustomTransformations = new ArrayMap<>();

    public void addTransformedView(int i, View view) {
        this.mTransformedViews.put(Integer.valueOf(i), view);
    }

    public void reset() {
        this.mTransformedViews.clear();
    }

    public void setCustomTransformation(CustomTransformation customTransformation, int i) {
        this.mCustomTransformations.put(Integer.valueOf(i), customTransformation);
    }

    @Override
    public TransformState getCurrentState(int i) {
        View view = this.mTransformedViews.get(Integer.valueOf(i));
        if (view != null && view.getVisibility() != 8) {
            return TransformState.createFrom(view, this);
        }
        return null;
    }

    @Override
    public void transformTo(final TransformableView transformableView, final Runnable runnable) {
        if (this.mViewTransformationAnimation != null) {
            this.mViewTransformationAnimation.cancel();
        }
        this.mViewTransformationAnimation = ValueAnimator.ofFloat(0.0f, 1.0f);
        this.mViewTransformationAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                ViewTransformationHelper.this.transformTo(transformableView, valueAnimator.getAnimatedFraction());
            }
        });
        this.mViewTransformationAnimation.setInterpolator(Interpolators.LINEAR);
        this.mViewTransformationAnimation.setDuration(360L);
        this.mViewTransformationAnimation.addListener(new AnimatorListenerAdapter() {
            public boolean mCancelled;

            @Override
            public void onAnimationEnd(Animator animator) {
                if (this.mCancelled) {
                    ViewTransformationHelper.this.abortTransformations();
                    return;
                }
                if (runnable != null) {
                    runnable.run();
                }
                ViewTransformationHelper.this.setVisible(false);
                ViewTransformationHelper.this.mViewTransformationAnimation = null;
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                this.mCancelled = true;
            }
        });
        this.mViewTransformationAnimation.start();
    }

    @Override
    public void transformTo(TransformableView transformableView, float f) {
        for (Integer num : this.mTransformedViews.keySet()) {
            TransformState currentState = getCurrentState(num.intValue());
            if (currentState != null) {
                CustomTransformation customTransformation = this.mCustomTransformations.get(num);
                if (customTransformation != null && customTransformation.transformTo(currentState, transformableView, f)) {
                    currentState.recycle();
                } else {
                    TransformState currentState2 = transformableView.getCurrentState(num.intValue());
                    if (currentState2 != null) {
                        currentState.transformViewTo(currentState2, f);
                        currentState2.recycle();
                    } else {
                        currentState.disappear(f, transformableView);
                    }
                    currentState.recycle();
                }
            }
        }
    }

    @Override
    public void transformFrom(final TransformableView transformableView) {
        if (this.mViewTransformationAnimation != null) {
            this.mViewTransformationAnimation.cancel();
        }
        this.mViewTransformationAnimation = ValueAnimator.ofFloat(0.0f, 1.0f);
        this.mViewTransformationAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                ViewTransformationHelper.this.transformFrom(transformableView, valueAnimator.getAnimatedFraction());
            }
        });
        this.mViewTransformationAnimation.addListener(new AnimatorListenerAdapter() {
            public boolean mCancelled;

            @Override
            public void onAnimationEnd(Animator animator) {
                if (this.mCancelled) {
                    ViewTransformationHelper.this.abortTransformations();
                } else {
                    ViewTransformationHelper.this.setVisible(true);
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                this.mCancelled = true;
            }
        });
        this.mViewTransformationAnimation.setInterpolator(Interpolators.LINEAR);
        this.mViewTransformationAnimation.setDuration(360L);
        this.mViewTransformationAnimation.start();
    }

    @Override
    public void transformFrom(TransformableView transformableView, float f) {
        for (Integer num : this.mTransformedViews.keySet()) {
            TransformState currentState = getCurrentState(num.intValue());
            if (currentState != null) {
                CustomTransformation customTransformation = this.mCustomTransformations.get(num);
                if (customTransformation != null && customTransformation.transformFrom(currentState, transformableView, f)) {
                    currentState.recycle();
                } else {
                    TransformState currentState2 = transformableView.getCurrentState(num.intValue());
                    if (currentState2 != null) {
                        currentState.transformViewFrom(currentState2, f);
                        currentState2.recycle();
                    } else {
                        currentState.appear(f, transformableView);
                    }
                    currentState.recycle();
                }
            }
        }
    }

    @Override
    public void setVisible(boolean z) {
        if (this.mViewTransformationAnimation != null) {
            this.mViewTransformationAnimation.cancel();
        }
        Iterator<Integer> it = this.mTransformedViews.keySet().iterator();
        while (it.hasNext()) {
            TransformState currentState = getCurrentState(it.next().intValue());
            if (currentState != null) {
                currentState.setVisible(z, false);
                currentState.recycle();
            }
        }
    }

    public void abortTransformations() {
        Iterator<Integer> it = this.mTransformedViews.keySet().iterator();
        while (it.hasNext()) {
            TransformState currentState = getCurrentState(it.next().intValue());
            if (currentState != null) {
                currentState.abortTransformation();
                currentState.recycle();
            }
        }
    }

    public void addRemainingTransformTypes(View view) {
        int id;
        int size = this.mTransformedViews.size();
        for (int i = 0; i < size; i++) {
            Object objValueAt = this.mTransformedViews.valueAt(i);
            while (true) {
                View view2 = (View) objValueAt;
                if (view2 != view.getParent()) {
                    view2.setTag(R.id.contains_transformed_view, true);
                    objValueAt = view2.getParent();
                }
            }
        }
        Stack stack = new Stack();
        stack.push(view);
        while (!stack.isEmpty()) {
            View view3 = (View) stack.pop();
            if (((Boolean) view3.getTag(R.id.contains_transformed_view)) == null && (id = view3.getId()) != -1) {
                addTransformedView(id, view3);
            } else {
                view3.setTag(R.id.contains_transformed_view, null);
                if ((view3 instanceof ViewGroup) && !this.mTransformedViews.containsValue(view3)) {
                    ViewGroup viewGroup = (ViewGroup) view3;
                    for (int i2 = 0; i2 < viewGroup.getChildCount(); i2++) {
                        stack.push(viewGroup.getChildAt(i2));
                    }
                }
            }
        }
    }

    public void resetTransformedView(View view) {
        TransformState transformStateCreateFrom = TransformState.createFrom(view, this);
        transformStateCreateFrom.setVisible(true, true);
        transformStateCreateFrom.recycle();
    }

    public ArraySet<View> getAllTransformingViews() {
        return new ArraySet<>(this.mTransformedViews.values());
    }

    @Override
    public boolean isAnimating() {
        return this.mViewTransformationAnimation != null && this.mViewTransformationAnimation.isRunning();
    }

    public static abstract class CustomTransformation {
        public abstract boolean transformFrom(TransformState transformState, TransformableView transformableView, float f);

        public abstract boolean transformTo(TransformState transformState, TransformableView transformableView, float f);

        public boolean initTransformation(TransformState transformState, TransformState transformState2) {
            return false;
        }

        public boolean customTransformTarget(TransformState transformState, TransformState transformState2) {
            return false;
        }

        public Interpolator getCustomInterpolator(int i, boolean z) {
            return null;
        }
    }
}
