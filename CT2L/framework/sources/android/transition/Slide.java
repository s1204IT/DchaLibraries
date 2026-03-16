package android.transition;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import com.android.internal.R;

public class Slide extends Visibility {
    private static final String PROPNAME_SCREEN_POSITION = "android:slide:screenPosition";
    private static final String TAG = "Slide";
    private CalculateSlide mSlideCalculator;
    private int mSlideEdge;
    private static final TimeInterpolator sDecelerate = new DecelerateInterpolator();
    private static final TimeInterpolator sAccelerate = new AccelerateInterpolator();
    private static final CalculateSlide sCalculateLeft = new CalculateSlideHorizontal() {
        @Override
        public float getGoneX(ViewGroup sceneRoot, View view) {
            return view.getTranslationX() - sceneRoot.getWidth();
        }
    };
    private static final CalculateSlide sCalculateStart = new CalculateSlideHorizontal() {
        @Override
        public float getGoneX(ViewGroup sceneRoot, View view) {
            boolean isRtl = sceneRoot.getLayoutDirection() == 1;
            if (isRtl) {
                float x = view.getTranslationX() + sceneRoot.getWidth();
                return x;
            }
            float x2 = view.getTranslationX() - sceneRoot.getWidth();
            return x2;
        }
    };
    private static final CalculateSlide sCalculateTop = new CalculateSlideVertical() {
        @Override
        public float getGoneY(ViewGroup sceneRoot, View view) {
            return view.getTranslationY() - sceneRoot.getHeight();
        }
    };
    private static final CalculateSlide sCalculateRight = new CalculateSlideHorizontal() {
        @Override
        public float getGoneX(ViewGroup sceneRoot, View view) {
            return view.getTranslationX() + sceneRoot.getWidth();
        }
    };
    private static final CalculateSlide sCalculateEnd = new CalculateSlideHorizontal() {
        @Override
        public float getGoneX(ViewGroup sceneRoot, View view) {
            boolean isRtl = sceneRoot.getLayoutDirection() == 1;
            if (isRtl) {
                float x = view.getTranslationX() - sceneRoot.getWidth();
                return x;
            }
            float x2 = view.getTranslationX() + sceneRoot.getWidth();
            return x2;
        }
    };
    private static final CalculateSlide sCalculateBottom = new CalculateSlideVertical() {
        @Override
        public float getGoneY(ViewGroup sceneRoot, View view) {
            return view.getTranslationY() + sceneRoot.getHeight();
        }
    };

    private interface CalculateSlide {
        float getGoneX(ViewGroup viewGroup, View view);

        float getGoneY(ViewGroup viewGroup, View view);
    }

    private static abstract class CalculateSlideHorizontal implements CalculateSlide {
        private CalculateSlideHorizontal() {
        }

        @Override
        public float getGoneY(ViewGroup sceneRoot, View view) {
            return view.getTranslationY();
        }
    }

    private static abstract class CalculateSlideVertical implements CalculateSlide {
        private CalculateSlideVertical() {
        }

        @Override
        public float getGoneX(ViewGroup sceneRoot, View view) {
            return view.getTranslationX();
        }
    }

    public Slide() {
        this.mSlideCalculator = sCalculateBottom;
        this.mSlideEdge = 80;
        setSlideEdge(80);
    }

    public Slide(int slideEdge) {
        this.mSlideCalculator = sCalculateBottom;
        this.mSlideEdge = 80;
        setSlideEdge(slideEdge);
    }

    public Slide(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mSlideCalculator = sCalculateBottom;
        this.mSlideEdge = 80;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Slide);
        int edge = a.getInt(0, 80);
        a.recycle();
        setSlideEdge(edge);
    }

    private void captureValues(TransitionValues transitionValues) {
        View view = transitionValues.view;
        int[] position = new int[2];
        view.getLocationOnScreen(position);
        transitionValues.values.put(PROPNAME_SCREEN_POSITION, position);
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        super.captureStartValues(transitionValues);
        captureValues(transitionValues);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        super.captureEndValues(transitionValues);
        captureValues(transitionValues);
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
            case Gravity.START:
                this.mSlideCalculator = sCalculateStart;
                break;
            case Gravity.END:
                this.mSlideCalculator = sCalculateEnd;
                break;
            default:
                throw new IllegalArgumentException("Invalid slide direction");
        }
        this.mSlideEdge = slideEdge;
        SidePropagation propagation = new SidePropagation();
        propagation.setSide(slideEdge);
        setPropagation(propagation);
    }

    public int getSlideEdge() {
        return this.mSlideEdge;
    }

    @Override
    public Animator onAppear(ViewGroup sceneRoot, View view, TransitionValues startValues, TransitionValues endValues) {
        if (endValues == null) {
            return null;
        }
        int[] position = (int[]) endValues.values.get(PROPNAME_SCREEN_POSITION);
        float endX = view.getTranslationX();
        float endY = view.getTranslationY();
        float startX = this.mSlideCalculator.getGoneX(sceneRoot, view);
        float startY = this.mSlideCalculator.getGoneY(sceneRoot, view);
        return TranslationAnimationCreator.createAnimation(view, endValues, position[0], position[1], startX, startY, endX, endY, sDecelerate);
    }

    @Override
    public Animator onDisappear(ViewGroup sceneRoot, View view, TransitionValues startValues, TransitionValues endValues) {
        if (startValues == null) {
            return null;
        }
        int[] position = (int[]) startValues.values.get(PROPNAME_SCREEN_POSITION);
        float startX = view.getTranslationX();
        float startY = view.getTranslationY();
        float endX = this.mSlideCalculator.getGoneX(sceneRoot, view);
        float endY = this.mSlideCalculator.getGoneY(sceneRoot, view);
        return TranslationAnimationCreator.createAnimation(view, startValues, position[0], position[1], startX, startY, endX, endY, sAccelerate);
    }
}
