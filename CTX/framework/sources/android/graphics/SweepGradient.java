package android.graphics;

public class SweepGradient extends Shader {
    private static final int TYPE_COLORS_AND_POSITIONS = 1;
    private static final int TYPE_COLOR_START_AND_COLOR_END = 2;
    private int mColor0;
    private int mColor1;
    private int[] mColors;
    private float mCx;
    private float mCy;
    private float[] mPositions;
    private int mType;

    private static native long nativeCreate1(long j, float f, float f2, int[] iArr, float[] fArr);

    private static native long nativeCreate2(long j, float f, float f2, int i, int i2);

    public SweepGradient(float f, float f2, int[] iArr, float[] fArr) {
        if (iArr.length < 2) {
            throw new IllegalArgumentException("needs >= 2 number of colors");
        }
        if (fArr != null && iArr.length != fArr.length) {
            throw new IllegalArgumentException("color and position arrays must be of equal length");
        }
        this.mType = 1;
        this.mCx = f;
        this.mCy = f2;
        this.mColors = (int[]) iArr.clone();
        this.mPositions = fArr != null ? (float[]) fArr.clone() : null;
    }

    public SweepGradient(float f, float f2, int i, int i2) {
        this.mType = 2;
        this.mCx = f;
        this.mCy = f2;
        this.mColor0 = i;
        this.mColor1 = i2;
        this.mColors = null;
        this.mPositions = null;
    }

    @Override
    long createNativeInstance(long j) {
        if (this.mType == 1) {
            return nativeCreate1(j, this.mCx, this.mCy, this.mColors, this.mPositions);
        }
        return nativeCreate2(j, this.mCx, this.mCy, this.mColor0, this.mColor1);
    }

    @Override
    protected Shader copy() {
        SweepGradient sweepGradient;
        if (this.mType == 1) {
            sweepGradient = new SweepGradient(this.mCx, this.mCy, (int[]) this.mColors.clone(), this.mPositions != null ? (float[]) this.mPositions.clone() : null);
        } else {
            sweepGradient = new SweepGradient(this.mCx, this.mCy, this.mColor0, this.mColor1);
        }
        copyLocalMatrix(sweepGradient);
        return sweepGradient;
    }
}
