package android.support.v17.leanback.transition;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v17.leanback.R$id;
import android.support.v17.leanback.R$styleable;
import android.transition.TransitionValues;
import android.transition.Visibility;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;

class SlideKitkat extends Visibility {
    private CalculateSlide mSlideCalculator;
    private int mSlideEdge;
    private static final TimeInterpolator sDecelerate = new DecelerateInterpolator();
    private static final TimeInterpolator sAccelerate = new AccelerateInterpolator();
    private static final CalculateSlide sCalculateLeft = new CalculateSlideHorizontal() {
        @Override
        public float getGone(View view) {
            return view.getTranslationX() - view.getWidth();
        }
    };
    private static final CalculateSlide sCalculateTop = new CalculateSlideVertical() {
        @Override
        public float getGone(View view) {
            return view.getTranslationY() - view.getHeight();
        }
    };
    private static final CalculateSlide sCalculateRight = new CalculateSlideHorizontal() {
        @Override
        public float getGone(View view) {
            return view.getTranslationX() + view.getWidth();
        }
    };
    private static final CalculateSlide sCalculateBottom = new CalculateSlideVertical() {
        @Override
        public float getGone(View view) {
            return view.getTranslationY() + view.getHeight();
        }
    };
    private static final CalculateSlide sCalculateStart = new CalculateSlideHorizontal() {
        @Override
        public float getGone(View view) {
            if (view.getLayoutDirection() == 1) {
                return view.getTranslationX() + view.getWidth();
            }
            return view.getTranslationX() - view.getWidth();
        }
    };
    private static final CalculateSlide sCalculateEnd = new CalculateSlideHorizontal() {
        @Override
        public float getGone(View view) {
            if (view.getLayoutDirection() == 1) {
                return view.getTranslationX() - view.getWidth();
            }
            return view.getTranslationX() + view.getWidth();
        }
    };

    private interface CalculateSlide {
        float getGone(View view);

        float getHere(View view);

        Property<View, Float> getProperty();
    }

    private static abstract class CalculateSlideHorizontal implements CalculateSlide {
        CalculateSlideHorizontal(CalculateSlideHorizontal calculateSlideHorizontal) {
            this();
        }

        private CalculateSlideHorizontal() {
        }

        @Override
        public float getHere(View view) {
            return view.getTranslationX();
        }

        @Override
        public Property<View, Float> getProperty() {
            return View.TRANSLATION_X;
        }
    }

    private static abstract class CalculateSlideVertical implements CalculateSlide {
        CalculateSlideVertical(CalculateSlideVertical calculateSlideVertical) {
            this();
        }

        private CalculateSlideVertical() {
        }

        @Override
        public float getHere(View view) {
            return view.getTranslationY();
        }

        @Override
        public Property<View, Float> getProperty() {
            return View.TRANSLATION_Y;
        }
    }

    public SlideKitkat() {
        setSlideEdge(80);
    }

    public SlideKitkat(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R$styleable.lbSlide);
        int edge = a.getInt(R$styleable.lbSlide_lb_slideEdge, 80);
        setSlideEdge(edge);
        long duration = a.getInt(R$styleable.lbSlide_android_duration, -1);
        if (duration >= 0) {
            setDuration(duration);
        }
        long startDelay = a.getInt(R$styleable.lbSlide_android_startDelay, -1);
        if (startDelay > 0) {
            setStartDelay(startDelay);
        }
        int resID = a.getResourceId(R$styleable.lbSlide_android_interpolator, 0);
        if (resID > 0) {
            setInterpolator(AnimationUtils.loadInterpolator(context, resID));
        }
        a.recycle();
    }

    public void setSlideEdge(int slideEdge) {
        switch (slideEdge) {
            case 3:
                this.mSlideCalculator = sCalculateLeft;
                break;
            case 5:
                this.mSlideCalculator = sCalculateRight;
                break;
            case 48:
                this.mSlideCalculator = sCalculateTop;
                break;
            case 80:
                this.mSlideCalculator = sCalculateBottom;
                break;
            case 8388611:
                this.mSlideCalculator = sCalculateStart;
                break;
            case 8388613:
                this.mSlideCalculator = sCalculateEnd;
                break;
            default:
                throw new IllegalArgumentException("Invalid slide direction");
        }
        this.mSlideEdge = slideEdge;
    }

    private Animator createAnimation(View view, Property<View, Float> property, float start, float end, float terminalValue, TimeInterpolator interpolator, int finalVisibility) {
        float[] startPosition = (float[]) view.getTag(R$id.lb_slide_transition_value);
        if (startPosition != null) {
            start = View.TRANSLATION_Y == property ? startPosition[1] : startPosition[0];
            view.setTag(R$id.lb_slide_transition_value, null);
        }
        ObjectAnimator anim = ObjectAnimator.ofFloat(view, property, start, end);
        SlideAnimatorListener listener = new SlideAnimatorListener(view, property, terminalValue, end, finalVisibility);
        anim.addListener(listener);
        anim.addPauseListener(listener);
        anim.setInterpolator(interpolator);
        return anim;
    }

    @Override
    public Animator onAppear(ViewGroup sceneRoot, TransitionValues startValues, int startVisibility, TransitionValues endValues, int endVisibility) {
        View view = endValues != null ? endValues.view : null;
        if (view == null) {
            return null;
        }
        float end = this.mSlideCalculator.getHere(view);
        float start = this.mSlideCalculator.getGone(view);
        return createAnimation(view, this.mSlideCalculator.getProperty(), start, end, end, sDecelerate, 0);
    }

    @Override
    public Animator onDisappear(ViewGroup sceneRoot, TransitionValues startValues, int startVisibility, TransitionValues endValues, int endVisibility) {
        View view = startValues != null ? startValues.view : null;
        if (view == null) {
            return null;
        }
        float start = this.mSlideCalculator.getHere(view);
        float end = this.mSlideCalculator.getGone(view);
        return createAnimation(view, this.mSlideCalculator.getProperty(), start, end, start, sAccelerate, 4);
    }

    private static class SlideAnimatorListener extends AnimatorListenerAdapter {
        private boolean mCanceled = false;
        private final float mEndValue;
        private final int mFinalVisibility;
        private float mPausedValue;
        private final Property<View, Float> mProp;
        private final float mTerminalValue;
        private final View mView;

        public SlideAnimatorListener(View view, Property<View, Float> prop, float terminalValue, float endValue, int finalVisibility) {
            this.mProp = prop;
            this.mView = view;
            this.mTerminalValue = terminalValue;
            this.mEndValue = endValue;
            this.mFinalVisibility = finalVisibility;
            view.setVisibility(0);
        }

        @Override
        public void onAnimationCancel(Animator animator) {
            float[] transitionPosition = {this.mView.getTranslationX(), this.mView.getTranslationY()};
            this.mView.setTag(R$id.lb_slide_transition_value, transitionPosition);
            this.mProp.set(this.mView, Float.valueOf(this.mTerminalValue));
            this.mCanceled = true;
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            if (!this.mCanceled) {
                this.mProp.set(this.mView, Float.valueOf(this.mTerminalValue));
            }
            this.mView.setVisibility(this.mFinalVisibility);
        }

        @Override
        public void onAnimationPause(Animator animator) {
            this.mPausedValue = this.mProp.get(this.mView).floatValue();
            this.mProp.set(this.mView, Float.valueOf(this.mEndValue));
            this.mView.setVisibility(this.mFinalVisibility);
        }

        @Override
        public void onAnimationResume(Animator animator) {
            this.mProp.set(this.mView, Float.valueOf(this.mPausedValue));
            this.mView.setVisibility(0);
        }
    }
}
