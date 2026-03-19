package com.android.internal.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.util.IntProperty;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import com.android.internal.widget.MessagingLinearLayout;
import com.android.internal.widget.ViewClippingUtil;

public class MessagingPropertyAnimator implements View.OnLayoutChangeListener {
    private static final long APPEAR_ANIMATION_LENGTH = 210;
    private static final int TAG_ALPHA_ANIMATOR = 16909368;
    private static final int TAG_FIRST_LAYOUT = 16909369;
    private static final int TAG_LAYOUT_TOP = 16909370;
    private static final int TAG_TOP = 16909372;
    private static final int TAG_TOP_ANIMATOR = 16909371;
    private static final Interpolator ALPHA_IN = new PathInterpolator(0.4f, 0.0f, 1.0f, 1.0f);
    public static final Interpolator ALPHA_OUT = new PathInterpolator(0.0f, 0.0f, 0.8f, 1.0f);
    private static final ViewClippingUtil.ClippingParameters CLIPPING_PARAMETERS = new ViewClippingUtil.ClippingParameters() {
        @Override
        public final boolean shouldFinish(View view) {
            return MessagingPropertyAnimator.lambda$static$0(view);
        }
    };
    private static final IntProperty<View> TOP = new IntProperty<View>("top") {
        @Override
        public void setValue(View view, int i) {
            MessagingPropertyAnimator.setTop(view, i);
        }

        @Override
        public Integer get(View view) {
            return Integer.valueOf(MessagingPropertyAnimator.getTop(view));
        }
    };

    static boolean lambda$static$0(View view) {
        return view.getId() == 16909125;
    }

    @Override
    public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        setLayoutTop(view, i2);
        if (isFirstLayout(view)) {
            setFirstLayout(view, false);
            setTop(view, i2);
        } else {
            startTopAnimation(view, getTop(view), i2, MessagingLayout.FAST_OUT_SLOW_IN);
        }
    }

    private static boolean isFirstLayout(View view) {
        Boolean bool = (Boolean) view.getTag(16909369);
        if (bool == null) {
            return true;
        }
        return bool.booleanValue();
    }

    public static void recycle(View view) {
        setFirstLayout(view, true);
    }

    private static void setFirstLayout(View view, boolean z) {
        view.setTagInternal(16909369, Boolean.valueOf(z));
    }

    private static void setLayoutTop(View view, int i) {
        view.setTagInternal(16909370, Integer.valueOf(i));
    }

    public static int getLayoutTop(View view) {
        Integer num = (Integer) view.getTag(16909370);
        if (num == null) {
            return getTop(view);
        }
        return num.intValue();
    }

    public static void startLocalTranslationFrom(View view, int i, Interpolator interpolator) {
        startTopAnimation(view, getTop(view) + i, getLayoutTop(view), interpolator);
    }

    public static void startLocalTranslationTo(View view, int i, Interpolator interpolator) {
        int top = getTop(view);
        startTopAnimation(view, top, i + top, interpolator);
    }

    public static int getTop(View view) {
        Integer num = (Integer) view.getTag(16909372);
        if (num == null) {
            return view.getTop();
        }
        return num.intValue();
    }

    private static void setTop(View view, int i) {
        view.setTagInternal(16909372, Integer.valueOf(i));
        updateTopAndBottom(view);
    }

    private static void updateTopAndBottom(View view) {
        int top = getTop(view);
        int height = view.getHeight();
        view.setTop(top);
        view.setBottom(height + top);
    }

    private static void startTopAnimation(final View view, int i, int i2, Interpolator interpolator) {
        ObjectAnimator objectAnimator = (ObjectAnimator) view.getTag(16909371);
        if (objectAnimator != null) {
            objectAnimator.cancel();
        }
        if (!view.isShown() || i == i2 || (MessagingLinearLayout.isGone(view) && !isHidingAnimated(view))) {
            setTop(view, i2);
            return;
        }
        ObjectAnimator objectAnimatorOfInt = ObjectAnimator.ofInt(view, TOP, i, i2);
        setTop(view, i);
        objectAnimatorOfInt.setInterpolator(interpolator);
        objectAnimatorOfInt.setDuration(APPEAR_ANIMATION_LENGTH);
        objectAnimatorOfInt.addListener(new AnimatorListenerAdapter() {
            public boolean mCancelled;

            @Override
            public void onAnimationEnd(Animator animator) {
                view.setTagInternal(16909371, null);
                MessagingPropertyAnimator.setClippingDeactivated(view, false);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                this.mCancelled = true;
            }
        });
        setClippingDeactivated(view, true);
        view.setTagInternal(16909371, objectAnimatorOfInt);
        objectAnimatorOfInt.start();
    }

    private static boolean isHidingAnimated(View view) {
        if (view instanceof MessagingLinearLayout.MessagingChild) {
            return ((MessagingLinearLayout.MessagingChild) view).isHidingAnimated();
        }
        return false;
    }

    public static void fadeIn(final View view) {
        ObjectAnimator objectAnimator = (ObjectAnimator) view.getTag(16909368);
        if (objectAnimator != null) {
            objectAnimator.cancel();
        }
        if (view.getVisibility() == 4) {
            view.setVisibility(0);
        }
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(view, View.ALPHA, 0.0f, 1.0f);
        view.setAlpha(0.0f);
        objectAnimatorOfFloat.setInterpolator(ALPHA_IN);
        objectAnimatorOfFloat.setDuration(APPEAR_ANIMATION_LENGTH);
        objectAnimatorOfFloat.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                view.setTagInternal(16909368, null);
                MessagingPropertyAnimator.updateLayerType(view, false);
            }
        });
        updateLayerType(view, true);
        view.setTagInternal(16909368, objectAnimatorOfFloat);
        objectAnimatorOfFloat.start();
    }

    private static void updateLayerType(View view, boolean z) {
        if (view.hasOverlappingRendering() && z) {
            view.setLayerType(2, null);
        } else if (view.getLayerType() == 2) {
            view.setLayerType(0, null);
        }
    }

    public static void fadeOut(final View view, final Runnable runnable) {
        ObjectAnimator objectAnimator = (ObjectAnimator) view.getTag(16909368);
        if (objectAnimator != null) {
            objectAnimator.cancel();
        }
        if (!view.isShown() || (MessagingLinearLayout.isGone(view) && !isHidingAnimated(view))) {
            view.setAlpha(0.0f);
            if (runnable != null) {
                runnable.run();
                return;
            }
            return;
        }
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(view, View.ALPHA, view.getAlpha(), 0.0f);
        objectAnimatorOfFloat.setInterpolator(ALPHA_OUT);
        objectAnimatorOfFloat.setDuration(APPEAR_ANIMATION_LENGTH);
        objectAnimatorOfFloat.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                view.setTagInternal(16909368, null);
                MessagingPropertyAnimator.updateLayerType(view, false);
                if (runnable != null) {
                    runnable.run();
                }
            }
        });
        updateLayerType(view, true);
        view.setTagInternal(16909368, objectAnimatorOfFloat);
        objectAnimatorOfFloat.start();
    }

    public static void setClippingDeactivated(View view, boolean z) {
        ViewClippingUtil.setClippingDeactivated(view, z, CLIPPING_PARAMETERS);
    }

    public static boolean isAnimatingTranslation(View view) {
        return view.getTag(16909371) != null;
    }

    public static boolean isAnimatingAlpha(View view) {
        return view.getTag(16909368) != null;
    }

    public static void setToLaidOutPosition(View view) {
        setTop(view, getLayoutTop(view));
    }
}
