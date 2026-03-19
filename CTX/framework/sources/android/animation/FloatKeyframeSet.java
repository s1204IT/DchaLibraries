package android.animation;

import android.animation.Keyframe;
import android.animation.Keyframes;
import java.util.List;

class FloatKeyframeSet extends KeyframeSet implements Keyframes.FloatKeyframes {
    public FloatKeyframeSet(Keyframe.FloatKeyframe... floatKeyframeArr) {
        super(floatKeyframeArr);
    }

    @Override
    public Object getValue(float f) {
        return Float.valueOf(getFloatValue(f));
    }

    @Override
    public FloatKeyframeSet mo2clone() {
        List<Keyframe> list = this.mKeyframes;
        int size = this.mKeyframes.size();
        Keyframe.FloatKeyframe[] floatKeyframeArr = new Keyframe.FloatKeyframe[size];
        for (int i = 0; i < size; i++) {
            floatKeyframeArr[i] = (Keyframe.FloatKeyframe) list.get(i).mo3clone();
        }
        return new FloatKeyframeSet(floatKeyframeArr);
    }

    @Override
    public float getFloatValue(float f) {
        if (f <= 0.0f) {
            Keyframe.FloatKeyframe floatKeyframe = (Keyframe.FloatKeyframe) this.mKeyframes.get(0);
            Keyframe.FloatKeyframe floatKeyframe2 = (Keyframe.FloatKeyframe) this.mKeyframes.get(1);
            float floatValue = floatKeyframe.getFloatValue();
            float floatValue2 = floatKeyframe2.getFloatValue();
            float fraction = floatKeyframe.getFraction();
            float fraction2 = floatKeyframe2.getFraction();
            TimeInterpolator interpolator = floatKeyframe2.getInterpolator();
            if (interpolator != null) {
                f = interpolator.getInterpolation(f);
            }
            float f2 = (f - fraction) / (fraction2 - fraction);
            if (this.mEvaluator == null) {
                return floatValue + (f2 * (floatValue2 - floatValue));
            }
            return ((Number) this.mEvaluator.evaluate(f2, Float.valueOf(floatValue), Float.valueOf(floatValue2))).floatValue();
        }
        if (f >= 1.0f) {
            Keyframe.FloatKeyframe floatKeyframe3 = (Keyframe.FloatKeyframe) this.mKeyframes.get(this.mNumKeyframes - 2);
            Keyframe.FloatKeyframe floatKeyframe4 = (Keyframe.FloatKeyframe) this.mKeyframes.get(this.mNumKeyframes - 1);
            float floatValue3 = floatKeyframe3.getFloatValue();
            float floatValue4 = floatKeyframe4.getFloatValue();
            float fraction3 = floatKeyframe3.getFraction();
            float fraction4 = floatKeyframe4.getFraction();
            TimeInterpolator interpolator2 = floatKeyframe4.getInterpolator();
            if (interpolator2 != null) {
                f = interpolator2.getInterpolation(f);
            }
            float f3 = (f - fraction3) / (fraction4 - fraction3);
            if (this.mEvaluator == null) {
                return floatValue3 + (f3 * (floatValue4 - floatValue3));
            }
            return ((Number) this.mEvaluator.evaluate(f3, Float.valueOf(floatValue3), Float.valueOf(floatValue4))).floatValue();
        }
        Keyframe.FloatKeyframe floatKeyframe5 = (Keyframe.FloatKeyframe) this.mKeyframes.get(0);
        int i = 1;
        while (i < this.mNumKeyframes) {
            Keyframe.FloatKeyframe floatKeyframe6 = (Keyframe.FloatKeyframe) this.mKeyframes.get(i);
            if (f >= floatKeyframe6.getFraction()) {
                i++;
                floatKeyframe5 = floatKeyframe6;
            } else {
                TimeInterpolator interpolator3 = floatKeyframe6.getInterpolator();
                float fraction5 = (f - floatKeyframe5.getFraction()) / (floatKeyframe6.getFraction() - floatKeyframe5.getFraction());
                float floatValue5 = floatKeyframe5.getFloatValue();
                float floatValue6 = floatKeyframe6.getFloatValue();
                if (interpolator3 != null) {
                    fraction5 = interpolator3.getInterpolation(fraction5);
                }
                if (this.mEvaluator == null) {
                    return floatValue5 + (fraction5 * (floatValue6 - floatValue5));
                }
                return ((Number) this.mEvaluator.evaluate(fraction5, Float.valueOf(floatValue5), Float.valueOf(floatValue6))).floatValue();
            }
        }
        return ((Number) this.mKeyframes.get(this.mNumKeyframes - 1).getValue()).floatValue();
    }

    @Override
    public Class getType() {
        return Float.class;
    }
}
