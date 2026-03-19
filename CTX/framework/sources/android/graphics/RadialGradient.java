package android.graphics;

import android.graphics.Shader;

public class RadialGradient extends Shader {
    private static final int TYPE_COLORS_AND_POSITIONS = 1;
    private static final int TYPE_COLOR_CENTER_AND_COLOR_EDGE = 2;
    private int mCenterColor;
    private int[] mColors;
    private int mEdgeColor;
    private float[] mPositions;
    private float mRadius;
    private Shader.TileMode mTileMode;
    private int mType;
    private float mX;
    private float mY;

    private static native long nativeCreate1(long j, float f, float f2, float f3, int[] iArr, float[] fArr, int i);

    private static native long nativeCreate2(long j, float f, float f2, float f3, int i, int i2, int i3);

    public RadialGradient(float f, float f2, float f3, int[] iArr, float[] fArr, Shader.TileMode tileMode) {
        if (f3 <= 0.0f) {
            throw new IllegalArgumentException("radius must be > 0");
        }
        if (iArr.length < 2) {
            throw new IllegalArgumentException("needs >= 2 number of colors");
        }
        if (fArr != null && iArr.length != fArr.length) {
            throw new IllegalArgumentException("color and position arrays must be of equal length");
        }
        this.mType = 1;
        this.mX = f;
        this.mY = f2;
        this.mRadius = f3;
        this.mColors = (int[]) iArr.clone();
        this.mPositions = fArr != null ? (float[]) fArr.clone() : null;
        this.mTileMode = tileMode;
    }

    public RadialGradient(float f, float f2, float f3, int i, int i2, Shader.TileMode tileMode) {
        if (f3 <= 0.0f) {
            throw new IllegalArgumentException("radius must be > 0");
        }
        this.mType = 2;
        this.mX = f;
        this.mY = f2;
        this.mRadius = f3;
        this.mCenterColor = i;
        this.mEdgeColor = i2;
        this.mTileMode = tileMode;
    }

    @Override
    long createNativeInstance(long j) {
        if (this.mType == 1) {
            return nativeCreate1(j, this.mX, this.mY, this.mRadius, this.mColors, this.mPositions, this.mTileMode.nativeInt);
        }
        return nativeCreate2(j, this.mX, this.mY, this.mRadius, this.mCenterColor, this.mEdgeColor, this.mTileMode.nativeInt);
    }

    @Override
    protected Shader copy() {
        RadialGradient radialGradient;
        if (this.mType == 1) {
            radialGradient = new RadialGradient(this.mX, this.mY, this.mRadius, (int[]) this.mColors.clone(), this.mPositions != null ? (float[]) this.mPositions.clone() : null, this.mTileMode);
        } else {
            radialGradient = new RadialGradient(this.mX, this.mY, this.mRadius, this.mCenterColor, this.mEdgeColor, this.mTileMode);
        }
        copyLocalMatrix(radialGradient);
        return radialGradient;
    }
}
