package com.android.systemui.qs;

import android.util.FloatProperty;
import android.util.MathUtils;
import android.util.Property;
import android.view.View;
import android.view.animation.Interpolator;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class TouchAnimator {
    private static final FloatProperty<TouchAnimator> POSITION = new FloatProperty<TouchAnimator>("position") { // from class: com.android.systemui.qs.TouchAnimator.1
        /* JADX DEBUG: Method merged with bridge method: setValue(Ljava/lang/Object;F)V */
        @Override // android.util.FloatProperty
        public void setValue(TouchAnimator touchAnimator, float f) {
            touchAnimator.setPosition(f);
        }

        /* JADX DEBUG: Method merged with bridge method: get(Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.util.Property
        public Float get(TouchAnimator touchAnimator) {
            return Float.valueOf(touchAnimator.mLastT);
        }
    };
    private final float mEndDelay;
    private final Interpolator mInterpolator;
    private final KeyframeSet[] mKeyframeSets;
    private float mLastT;
    private final Listener mListener;
    private final float mSpan;
    private final float mStartDelay;
    private final Object[] mTargets;

    public interface Listener {
        void onAnimationAtEnd();

        void onAnimationAtStart();

        void onAnimationStarted();
    }

    private TouchAnimator(Object[] objArr, KeyframeSet[] keyframeSetArr, float f, float f2, Interpolator interpolator, Listener listener) {
        this.mLastT = -1.0f;
        this.mTargets = objArr;
        this.mKeyframeSets = keyframeSetArr;
        this.mStartDelay = f;
        this.mEndDelay = f2;
        this.mSpan = (1.0f - this.mEndDelay) - this.mStartDelay;
        this.mInterpolator = interpolator;
        this.mListener = listener;
    }

    public void setPosition(float f) {
        float fConstrain = MathUtils.constrain((f - this.mStartDelay) / this.mSpan, 0.0f, 1.0f);
        if (this.mInterpolator != null) {
            fConstrain = this.mInterpolator.getInterpolation(fConstrain);
        }
        if (fConstrain == this.mLastT) {
            return;
        }
        if (this.mListener != null) {
            if (fConstrain == 1.0f) {
                this.mListener.onAnimationAtEnd();
            } else if (fConstrain == 0.0f) {
                this.mListener.onAnimationAtStart();
            } else if (this.mLastT <= 0.0f || this.mLastT == 1.0f) {
                this.mListener.onAnimationStarted();
            }
            this.mLastT = fConstrain;
        }
        for (int i = 0; i < this.mTargets.length; i++) {
            this.mKeyframeSets[i].setValue(fConstrain, this.mTargets[i]);
        }
    }

    public static class ListenerAdapter implements Listener {
        @Override // com.android.systemui.qs.TouchAnimator.Listener
        public void onAnimationAtStart() {
        }

        @Override // com.android.systemui.qs.TouchAnimator.Listener
        public void onAnimationAtEnd() {
        }

        @Override // com.android.systemui.qs.TouchAnimator.Listener
        public void onAnimationStarted() {
        }
    }

    public static class Builder {
        private float mEndDelay;
        private Interpolator mInterpolator;
        private Listener mListener;
        private float mStartDelay;
        private List<Object> mTargets = new ArrayList();
        private List<KeyframeSet> mValues = new ArrayList();

        public Builder addFloat(Object obj, String str, float... fArr) {
            add(obj, KeyframeSet.ofFloat(getProperty(obj, str, Float.TYPE), fArr));
            return this;
        }

        private void add(Object obj, KeyframeSet keyframeSet) {
            this.mTargets.add(obj);
            this.mValues.add(keyframeSet);
        }

        private static Property getProperty(Object obj, String str, Class<?> cls) {
            if (obj instanceof View) {
                switch (str) {
                    case "translationX":
                        return View.TRANSLATION_X;
                    case "translationY":
                        return View.TRANSLATION_Y;
                    case "translationZ":
                        return View.TRANSLATION_Z;
                    case "alpha":
                        return View.ALPHA;
                    case "rotation":
                        return View.ROTATION;
                    case "x":
                        return View.X;
                    case "y":
                        return View.Y;
                    case "scaleX":
                        return View.SCALE_X;
                    case "scaleY":
                        return View.SCALE_Y;
                }
            }
            if ((obj instanceof TouchAnimator) && "position".equals(str)) {
                return TouchAnimator.POSITION;
            }
            return Property.of(obj.getClass(), cls, str);
        }

        public Builder setStartDelay(float f) {
            this.mStartDelay = f;
            return this;
        }

        public Builder setEndDelay(float f) {
            this.mEndDelay = f;
            return this;
        }

        public Builder setInterpolator(Interpolator interpolator) {
            this.mInterpolator = interpolator;
            return this;
        }

        public Builder setListener(Listener listener) {
            this.mListener = listener;
            return this;
        }

        public TouchAnimator build() {
            return new TouchAnimator(this.mTargets.toArray(new Object[this.mTargets.size()]), (KeyframeSet[]) this.mValues.toArray(new KeyframeSet[this.mValues.size()]), this.mStartDelay, this.mEndDelay, this.mInterpolator, this.mListener);
        }
    }

    private static abstract class KeyframeSet {
        private final float mFrameWidth;
        private final int mSize;

        protected abstract void interpolate(int i, float f, Object obj);

        public KeyframeSet(int i) {
            this.mSize = i;
            this.mFrameWidth = 1.0f / (i - 1);
        }

        void setValue(float f, Object obj) {
            interpolate(MathUtils.constrain((int) Math.ceil(f / this.mFrameWidth), 1, this.mSize - 1), (f - (this.mFrameWidth * (r0 - 1))) / this.mFrameWidth, obj);
        }

        public static KeyframeSet ofFloat(Property property, float... fArr) {
            return new FloatKeyframeSet(property, fArr);
        }
    }

    private static class FloatKeyframeSet<T> extends KeyframeSet {
        private final Property<T, Float> mProperty;
        private final float[] mValues;

        public FloatKeyframeSet(Property<T, Float> property, float[] fArr) {
            super(fArr.length);
            this.mProperty = property;
            this.mValues = fArr;
        }

        @Override // com.android.systemui.qs.TouchAnimator.KeyframeSet
        protected void interpolate(int i, float f, Object obj) {
            float f2 = this.mValues[i - 1];
            this.mProperty.set(obj, Float.valueOf(f2 + ((this.mValues[i] - f2) * f)));
        }
    }
}
