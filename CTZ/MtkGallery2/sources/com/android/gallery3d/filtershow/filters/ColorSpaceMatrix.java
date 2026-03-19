package com.android.gallery3d.filtershow.filters;

import java.util.Arrays;

public class ColorSpaceMatrix {
    private final float[] mMatrix = new float[16];

    public ColorSpaceMatrix() {
        identity();
    }

    public float[] getMatrix() {
        return this.mMatrix;
    }

    public void identity() {
        Arrays.fill(this.mMatrix, 0.0f);
        float[] fArr = this.mMatrix;
        float[] fArr2 = this.mMatrix;
        float[] fArr3 = this.mMatrix;
        this.mMatrix[15] = 1.0f;
        fArr3[10] = 1.0f;
        fArr2[5] = 1.0f;
        fArr[0] = 1.0f;
    }

    private void multiply(float[] fArr) {
        float[] fArr2 = new float[16];
        for (int i = 0; i < 4; i++) {
            int i2 = i * 4;
            for (int i3 = 0; i3 < 4; i3++) {
                fArr2[i2 + i3] = (this.mMatrix[i2 + 0] * fArr[i3]) + (this.mMatrix[i2 + 1] * fArr[4 + i3]) + (this.mMatrix[i2 + 2] * fArr[8 + i3]) + (this.mMatrix[i2 + 3] * fArr[12 + i3]);
            }
        }
        for (int i4 = 0; i4 < 16; i4++) {
            this.mMatrix[i4] = fArr2[i4];
        }
    }

    private void xRotateMatrix(float f, float f2) {
        float[] fArr = new ColorSpaceMatrix().mMatrix;
        fArr[5] = f2;
        fArr[6] = f;
        fArr[9] = -f;
        fArr[10] = f2;
        multiply(fArr);
    }

    private void yRotateMatrix(float f, float f2) {
        float[] fArr = new ColorSpaceMatrix().mMatrix;
        fArr[0] = f2;
        fArr[2] = -f;
        fArr[8] = f;
        fArr[10] = f2;
        multiply(fArr);
    }

    private void zRotateMatrix(float f, float f2) {
        float[] fArr = new ColorSpaceMatrix().mMatrix;
        fArr[0] = f2;
        fArr[1] = f;
        fArr[4] = -f;
        fArr[5] = f2;
        multiply(fArr);
    }

    private void zShearMatrix(float f, float f2) {
        float[] fArr = new ColorSpaceMatrix().mMatrix;
        fArr[2] = f;
        fArr[6] = f2;
        multiply(fArr);
    }

    public void setHue(float f) {
        float fSqrt = 1.0f / ((float) Math.sqrt(2.0d));
        xRotateMatrix(fSqrt, fSqrt);
        float fSqrt2 = (float) Math.sqrt(3.0d);
        float f2 = (-1.0f) / fSqrt2;
        float fSqrt3 = ((float) Math.sqrt(2.0d)) / fSqrt2;
        yRotateMatrix(f2, fSqrt3);
        float redf = getRedf(0.3086f, 0.6094f, 0.082f);
        float greenf = getGreenf(0.3086f, 0.6094f, 0.082f);
        float bluef = getBluef(0.3086f, 0.6094f, 0.082f);
        float f3 = redf / bluef;
        float f4 = greenf / bluef;
        zShearMatrix(f3, f4);
        double d = (((double) f) * 3.141592653589793d) / 180.0d;
        zRotateMatrix((float) Math.sin(d), (float) Math.cos(d));
        zShearMatrix(-f3, -f4);
        yRotateMatrix(-f2, fSqrt3);
        xRotateMatrix(-fSqrt, fSqrt);
    }

    private float getRedf(float f, float f2, float f3) {
        return (f * this.mMatrix[0]) + (f2 * this.mMatrix[4]) + (f3 * this.mMatrix[8]) + this.mMatrix[12];
    }

    private float getGreenf(float f, float f2, float f3) {
        return (f * this.mMatrix[1]) + (f2 * this.mMatrix[5]) + (f3 * this.mMatrix[9]) + this.mMatrix[13];
    }

    private float getBluef(float f, float f2, float f3) {
        return (f * this.mMatrix[2]) + (f2 * this.mMatrix[6]) + (f3 * this.mMatrix[10]) + this.mMatrix[14];
    }
}
