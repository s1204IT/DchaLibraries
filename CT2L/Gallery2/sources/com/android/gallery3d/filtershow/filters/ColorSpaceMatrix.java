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

    private void multiply(float[] a) {
        float[] temp = new float[16];
        for (int y = 0; y < 4; y++) {
            int y4 = y * 4;
            for (int x = 0; x < 4; x++) {
                temp[y4 + x] = (this.mMatrix[y4 + 0] * a[x]) + (this.mMatrix[y4 + 1] * a[x + 4]) + (this.mMatrix[y4 + 2] * a[x + 8]) + (this.mMatrix[y4 + 3] * a[x + 12]);
            }
        }
        for (int i = 0; i < 16; i++) {
            this.mMatrix[i] = temp[i];
        }
    }

    private void xRotateMatrix(float rs, float rc) {
        ColorSpaceMatrix c = new ColorSpaceMatrix();
        float[] tmp = c.mMatrix;
        tmp[5] = rc;
        tmp[6] = rs;
        tmp[9] = -rs;
        tmp[10] = rc;
        multiply(tmp);
    }

    private void yRotateMatrix(float rs, float rc) {
        ColorSpaceMatrix c = new ColorSpaceMatrix();
        float[] tmp = c.mMatrix;
        tmp[0] = rc;
        tmp[2] = -rs;
        tmp[8] = rs;
        tmp[10] = rc;
        multiply(tmp);
    }

    private void zRotateMatrix(float rs, float rc) {
        ColorSpaceMatrix c = new ColorSpaceMatrix();
        float[] tmp = c.mMatrix;
        tmp[0] = rc;
        tmp[1] = rs;
        tmp[4] = -rs;
        tmp[5] = rc;
        multiply(tmp);
    }

    private void zShearMatrix(float dx, float dy) {
        ColorSpaceMatrix c = new ColorSpaceMatrix();
        float[] tmp = c.mMatrix;
        tmp[2] = dx;
        tmp[6] = dy;
        multiply(tmp);
    }

    public void setHue(float rot) {
        float mag = (float) Math.sqrt(2.0d);
        float xrs = 1.0f / mag;
        float xrc = 1.0f / mag;
        xRotateMatrix(xrs, xrc);
        float mag2 = (float) Math.sqrt(3.0d);
        float yrs = (-1.0f) / mag2;
        float yrc = ((float) Math.sqrt(2.0d)) / mag2;
        yRotateMatrix(yrs, yrc);
        float lx = getRedf(0.3086f, 0.6094f, 0.082f);
        float ly = getGreenf(0.3086f, 0.6094f, 0.082f);
        float lz = getBluef(0.3086f, 0.6094f, 0.082f);
        float zsx = lx / lz;
        float zsy = ly / lz;
        zShearMatrix(zsx, zsy);
        float zrs = (float) Math.sin((((double) rot) * 3.141592653589793d) / 180.0d);
        float zrc = (float) Math.cos((((double) rot) * 3.141592653589793d) / 180.0d);
        zRotateMatrix(zrs, zrc);
        zShearMatrix(-zsx, -zsy);
        yRotateMatrix(-yrs, yrc);
        xRotateMatrix(-xrs, xrc);
    }

    private float getRedf(float r, float g, float b) {
        return (this.mMatrix[0] * r) + (this.mMatrix[4] * g) + (this.mMatrix[8] * b) + this.mMatrix[12];
    }

    private float getGreenf(float r, float g, float b) {
        return (this.mMatrix[1] * r) + (this.mMatrix[5] * g) + (this.mMatrix[9] * b) + this.mMatrix[13];
    }

    private float getBluef(float r, float g, float b) {
        return (this.mMatrix[2] * r) + (this.mMatrix[6] * g) + (this.mMatrix[10] * b) + this.mMatrix[14];
    }
}
