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

/* loaded from: classes.dex */
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

    @Override // com.android.systemui.statusbar.TransformableView
    public TransformState getCurrentState(int i) {
        View view = this.mTransformedViews.get(Integer.valueOf(i));
        if (view != null && view.getVisibility() != 8) {
            return TransformState.createFrom(view, this);
        }
        return null;
    }

    @Override // com.android.systemui.statusbar.TransformableView
    public void transformTo(TransformableView transformableView, Runnable runnable) {
        if (this.mViewTransformationAnimation != null) {
            this.mViewTransformationAnimation.cancel();
        }
        this.mViewTransformationAnimation = ValueAnimator.ofFloat(0.0f, 1.0f);
        this.mViewTransformationAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.systemui.statusbar.ViewTransformationHelper.1
            final /* synthetic */ TransformableView val$notification;

            AnonymousClass1(TransformableView transformableView2) {
                transformableView = transformableView2;
            }

            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                ViewTransformationHelper.this.transformTo(transformableView, valueAnimator.getAnimatedFraction());
            }
        });
        this.mViewTransformationAnimation.setInterpolator(Interpolators.LINEAR);
        this.mViewTransformationAnimation.setDuration(360L);
        this.mViewTransformationAnimation.addListener(new AnimatorListenerAdapter() { // from class: com.android.systemui.statusbar.ViewTransformationHelper.2
            public boolean mCancelled;
            final /* synthetic */ Runnable val$endRunnable;

            AnonymousClass2(Runnable runnable2) {
                runnable = runnable2;
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
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

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationCancel(Animator animator) {
                this.mCancelled = true;
            }
        });
        this.mViewTransformationAnimation.start();
    }

    /* renamed from: com.android.systemui.statusbar.ViewTransformationHelper$1 */
    class AnonymousClass1 implements ValueAnimator.AnimatorUpdateListener {
        final /* synthetic */ TransformableView val$notification;

        AnonymousClass1(TransformableView transformableView2) {
            transformableView = transformableView2;
        }

        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            ViewTransformationHelper.this.transformTo(transformableView, valueAnimator.getAnimatedFraction());
        }
    }

    /* renamed from: com.android.systemui.statusbar.ViewTransformationHelper$2 */
    class AnonymousClass2 extends AnimatorListenerAdapter {
        public boolean mCancelled;
        final /* synthetic */ Runnable val$endRunnable;

        AnonymousClass2(Runnable runnable2) {
            runnable = runnable2;
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
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

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationCancel(Animator animator) {
            this.mCancelled = true;
        }
    }

    @Override // com.android.systemui.statusbar.TransformableView
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

    @Override // com.android.systemui.statusbar.TransformableView
    public void transformFrom(TransformableView transformableView) {
        if (this.mViewTransformationAnimation != null) {
            this.mViewTransformationAnimation.cancel();
        }
        this.mViewTransformationAnimation = ValueAnimator.ofFloat(0.0f, 1.0f);
        this.mViewTransformationAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.systemui.statusbar.ViewTransformationHelper.3
            final /* synthetic */ TransformableView val$notification;

            AnonymousClass3(TransformableView transformableView2) {
                transformableView = transformableView2;
            }

            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                ViewTransformationHelper.this.transformFrom(transformableView, valueAnimator.getAnimatedFraction());
            }
        });
        this.mViewTransformationAnimation.addListener(new AnimatorListenerAdapter() { // from class: com.android.systemui.statusbar.ViewTransformationHelper.4
            public boolean mCancelled;

            AnonymousClass4() {
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                if (this.mCancelled) {
                    ViewTransformationHelper.this.abortTransformations();
                } else {
                    ViewTransformationHelper.this.setVisible(true);
                }
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationCancel(Animator animator) {
                this.mCancelled = true;
            }
        });
        this.mViewTransformationAnimation.setInterpolator(Interpolators.LINEAR);
        this.mViewTransformationAnimation.setDuration(360L);
        this.mViewTransformationAnimation.start();
    }

    /* renamed from: com.android.systemui.statusbar.ViewTransformationHelper$3 */
    class AnonymousClass3 implements ValueAnimator.AnimatorUpdateListener {
        final /* synthetic */ TransformableView val$notification;

        AnonymousClass3(TransformableView transformableView2) {
            transformableView = transformableView2;
        }

        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            ViewTransformationHelper.this.transformFrom(transformableView, valueAnimator.getAnimatedFraction());
        }
    }

    /* renamed from: com.android.systemui.statusbar.ViewTransformationHelper$4 */
    class AnonymousClass4 extends AnimatorListenerAdapter {
        public boolean mCancelled;

        AnonymousClass4() {
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            if (this.mCancelled) {
                ViewTransformationHelper.this.abortTransformations();
            } else {
                ViewTransformationHelper.this.setVisible(true);
            }
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationCancel(Animator animator) {
            this.mCancelled = true;
        }
    }

    @Override // com.android.systemui.statusbar.TransformableView
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

    @Override // com.android.systemui.statusbar.TransformableView
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

    private void abortTransformations() {
        Iterator<Integer> it = this.mTransformedViews.keySet().iterator();
        while (it.hasNext()) {
            TransformState currentState = getCurrentState(it.next().intValue());
            if (currentState != null) {
                currentState.abortTransformation();
                currentState.recycle();
            }
        }
    }

    /* JADX DEBUG: Move duplicate insns, count: 1 to block B:47:0x0015 */
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

    @Override // com.android.systemui.statusbar.notification.TransformState.TransformInfo
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
