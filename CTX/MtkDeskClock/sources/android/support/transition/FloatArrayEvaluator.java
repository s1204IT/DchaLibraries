package android.support.transition;

import android.animation.TypeEvaluator;

class FloatArrayEvaluator implements TypeEvaluator<float[]> {
    private float[] mArray;

    FloatArrayEvaluator(float[] reuseArray) {
        this.mArray = reuseArray;
    }

    @Override
    public float[] evaluate(float fraction, float[] startValue, float[] endValue) {
        float[] array = this.mArray;
        if (array == null) {
            array = new float[startValue.length];
        }
        for (int i = 0; i < array.length; i++) {
            float start = startValue[i];
            float end = endValue[i];
            array[i] = ((end - start) * fraction) + start;
        }
        return array;
    }
}
