package android.graphics;

import android.graphics.Shader;

public class LinearGradient extends Shader {
    private static final int TYPE_COLORS_AND_POSITIONS = 1;
    private static final int TYPE_COLOR_START_AND_COLOR_END = 2;
    private int mColor0;
    private int mColor1;
    private int[] mColors;
    private float[] mPositions;
    private Shader.TileMode mTileMode;
    private int mType;
    private float mX0;
    private float mX1;
    private float mY0;
    private float mY1;

    private native long nativeCreate1(long j, float f, float f2, float f3, float f4, int[] iArr, float[] fArr, int i);

    private native long nativeCreate2(long j, float f, float f2, float f3, float f4, int i, int i2, int i3);

    public LinearGradient(float f, float f2, float f3, float f4, int[] iArr, float[] fArr, Shader.TileMode tileMode) {
        if (iArr.length < 2) {
            throw new IllegalArgumentException("needs >= 2 number of colors");
        }
        if (fArr != null && iArr.length != fArr.length) {
            throw new IllegalArgumentException("color and position arrays must be of equal length");
        }
        this.mType = 1;
        this.mX0 = f;
        this.mY0 = f2;
        this.mX1 = f3;
        this.mY1 = f4;
        this.mColors = (int[]) iArr.clone();
        this.mPositions = fArr != null ? (float[]) fArr.clone() : null;
        this.mTileMode = tileMode;
    }

    public LinearGradient(float f, float f2, float f3, float f4, int i, int i2, Shader.TileMode tileMode) {
        this.mType = 2;
        this.mX0 = f;
        this.mY0 = f2;
        this.mX1 = f3;
        this.mY1 = f4;
        this.mColor0 = i;
        this.mColor1 = i2;
        this.mColors = null;
        this.mPositions = null;
        this.mTileMode = tileMode;
    }

    @Override
    long createNativeInstance(long j) {
        if (this.mType == 1) {
            return nativeCreate1(j, this.mX0, this.mY0, this.mX1, this.mY1, this.mColors, this.mPositions, this.mTileMode.nativeInt);
        }
        return nativeCreate2(j, this.mX0, this.mY0, this.mX1, this.mY1, this.mColor0, this.mColor1, this.mTileMode.nativeInt);
    }

    @Override
    protected Shader copy() {
        LinearGradient linearGradient;
        if (this.mType == 1) {
            linearGradient = new LinearGradient(this.mX0, this.mY0, this.mX1, this.mY1, (int[]) this.mColors.clone(), this.mPositions != null ? (float[]) this.mPositions.clone() : null, this.mTileMode);
        } else {
            linearGradient = new LinearGradient(this.mX0, this.mY0, this.mX1, this.mY1, this.mColor0, this.mColor1, this.mTileMode);
        }
        copyLocalMatrix(linearGradient);
        return linearGradient;
    }
}
