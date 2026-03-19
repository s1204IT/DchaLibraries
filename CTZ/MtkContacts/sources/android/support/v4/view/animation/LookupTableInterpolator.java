package android.support.v4.view.animation;

import android.view.animation.Interpolator;
import com.android.contacts.ContactPhotoManager;

abstract class LookupTableInterpolator implements Interpolator {
    private final float mStepSize;
    private final float[] mValues;

    protected LookupTableInterpolator(float[] values) {
        this.mValues = values;
        this.mStepSize = 1.0f / (this.mValues.length - 1);
    }

    @Override
    public float getInterpolation(float input) {
        if (input >= 1.0f) {
            return 1.0f;
        }
        if (input <= ContactPhotoManager.OFFSET_DEFAULT) {
            return ContactPhotoManager.OFFSET_DEFAULT;
        }
        int position = Math.min((int) ((this.mValues.length - 1) * input), this.mValues.length - 2);
        float quantized = position * this.mStepSize;
        float diff = input - quantized;
        float weight = diff / this.mStepSize;
        return this.mValues[position] + ((this.mValues[position + 1] - this.mValues[position]) * weight);
    }
}
