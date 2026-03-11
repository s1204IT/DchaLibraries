package android.support.v7.graphics;

public final class Target {
    public static final Target DARK_MUTED;
    public static final Target DARK_VIBRANT;
    public static final Target LIGHT_MUTED;
    public static final Target LIGHT_VIBRANT = new Target();
    public static final Target MUTED;
    public static final Target VIBRANT;
    final float[] mSaturationTargets = new float[3];
    final float[] mLightnessTargets = new float[3];
    final float[] mWeights = new float[3];
    boolean mIsExclusive = true;

    static {
        setDefaultLightLightnessValues(LIGHT_VIBRANT);
        setDefaultVibrantSaturationValues(LIGHT_VIBRANT);
        VIBRANT = new Target();
        setDefaultNormalLightnessValues(VIBRANT);
        setDefaultVibrantSaturationValues(VIBRANT);
        DARK_VIBRANT = new Target();
        setDefaultDarkLightnessValues(DARK_VIBRANT);
        setDefaultVibrantSaturationValues(DARK_VIBRANT);
        LIGHT_MUTED = new Target();
        setDefaultLightLightnessValues(LIGHT_MUTED);
        setDefaultMutedSaturationValues(LIGHT_MUTED);
        MUTED = new Target();
        setDefaultNormalLightnessValues(MUTED);
        setDefaultMutedSaturationValues(MUTED);
        DARK_MUTED = new Target();
        setDefaultDarkLightnessValues(DARK_MUTED);
        setDefaultMutedSaturationValues(DARK_MUTED);
    }

    Target() {
        setTargetDefaultValues(this.mSaturationTargets);
        setTargetDefaultValues(this.mLightnessTargets);
        setDefaultWeights();
    }

    public float getMinimumSaturation() {
        return this.mSaturationTargets[0];
    }

    public float getTargetSaturation() {
        return this.mSaturationTargets[1];
    }

    public float getMaximumSaturation() {
        return this.mSaturationTargets[2];
    }

    public float getMinimumLightness() {
        return this.mLightnessTargets[0];
    }

    public float getTargetLightness() {
        return this.mLightnessTargets[1];
    }

    public float getMaximumLightness() {
        return this.mLightnessTargets[2];
    }

    public float getSaturationWeight() {
        return this.mWeights[0];
    }

    public float getLightnessWeight() {
        return this.mWeights[1];
    }

    public float getPopulationWeight() {
        return this.mWeights[2];
    }

    public boolean isExclusive() {
        return this.mIsExclusive;
    }

    private static void setTargetDefaultValues(float[] values) {
        values[0] = 0.0f;
        values[1] = 0.5f;
        values[2] = 1.0f;
    }

    private void setDefaultWeights() {
        this.mWeights[0] = 0.24f;
        this.mWeights[1] = 0.52f;
        this.mWeights[2] = 0.24f;
    }

    void normalizeWeights() {
        float sum = 0.0f;
        int z = this.mWeights.length;
        for (int i = 0; i < z; i++) {
            float weight = this.mWeights[i];
            if (weight > 0.0f) {
                sum += weight;
            }
        }
        if (sum != 0.0f) {
            int z2 = this.mWeights.length;
            for (int i2 = 0; i2 < z2; i2++) {
                if (this.mWeights[i2] > 0.0f) {
                    float[] fArr = this.mWeights;
                    fArr[i2] = fArr[i2] / sum;
                }
            }
        }
    }

    private static void setDefaultDarkLightnessValues(Target target) {
        target.mLightnessTargets[1] = 0.26f;
        target.mLightnessTargets[2] = 0.45f;
    }

    private static void setDefaultNormalLightnessValues(Target target) {
        target.mLightnessTargets[0] = 0.3f;
        target.mLightnessTargets[1] = 0.5f;
        target.mLightnessTargets[2] = 0.7f;
    }

    private static void setDefaultLightLightnessValues(Target target) {
        target.mLightnessTargets[0] = 0.55f;
        target.mLightnessTargets[1] = 0.74f;
    }

    private static void setDefaultVibrantSaturationValues(Target target) {
        target.mSaturationTargets[0] = 0.35f;
        target.mSaturationTargets[1] = 1.0f;
    }

    private static void setDefaultMutedSaturationValues(Target target) {
        target.mSaturationTargets[1] = 0.3f;
        target.mSaturationTargets[2] = 0.4f;
    }
}
