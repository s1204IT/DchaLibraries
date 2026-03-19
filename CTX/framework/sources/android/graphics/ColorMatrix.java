package android.graphics;

import java.util.Arrays;

public class ColorMatrix {
    private final float[] mArray;

    public ColorMatrix() {
        this.mArray = new float[20];
        reset();
    }

    public ColorMatrix(float[] fArr) {
        this.mArray = new float[20];
        System.arraycopy(fArr, 0, this.mArray, 0, 20);
    }

    public ColorMatrix(ColorMatrix colorMatrix) {
        this.mArray = new float[20];
        System.arraycopy(colorMatrix.mArray, 0, this.mArray, 0, 20);
    }

    public final float[] getArray() {
        return this.mArray;
    }

    public void reset() {
        float[] fArr = this.mArray;
        Arrays.fill(fArr, 0.0f);
        fArr[18] = 1.0f;
        fArr[12] = 1.0f;
        fArr[6] = 1.0f;
        fArr[0] = 1.0f;
    }

    public void set(ColorMatrix colorMatrix) {
        System.arraycopy(colorMatrix.mArray, 0, this.mArray, 0, 20);
    }

    public void set(float[] fArr) {
        System.arraycopy(fArr, 0, this.mArray, 0, 20);
    }

    public void setScale(float f, float f2, float f3, float f4) {
        float[] fArr = this.mArray;
        for (int i = 19; i > 0; i--) {
            fArr[i] = 0.0f;
        }
        fArr[0] = f;
        fArr[6] = f2;
        fArr[12] = f3;
        fArr[18] = f4;
    }

    public void setRotate(int i, float f) {
        reset();
        double d = (((double) f) * 3.141592653589793d) / 180.0d;
        float fCos = (float) Math.cos(d);
        float fSin = (float) Math.sin(d);
        switch (i) {
            case 0:
                float[] fArr = this.mArray;
                this.mArray[12] = fCos;
                fArr[6] = fCos;
                this.mArray[7] = fSin;
                this.mArray[11] = -fSin;
                return;
            case 1:
                float[] fArr2 = this.mArray;
                this.mArray[12] = fCos;
                fArr2[0] = fCos;
                this.mArray[2] = -fSin;
                this.mArray[10] = fSin;
                return;
            case 2:
                float[] fArr3 = this.mArray;
                this.mArray[6] = fCos;
                fArr3[0] = fCos;
                this.mArray[1] = fSin;
                this.mArray[5] = -fSin;
                return;
            default:
                throw new RuntimeException();
        }
    }

    public void setConcat(ColorMatrix colorMatrix, ColorMatrix colorMatrix2) {
        float[] fArr;
        if (colorMatrix == this || colorMatrix2 == this) {
            fArr = new float[20];
        } else {
            fArr = this.mArray;
        }
        float[] fArr2 = colorMatrix.mArray;
        float[] fArr3 = colorMatrix2.mArray;
        int i = 0;
        for (int i2 = 0; i2 < 20; i2 += 5) {
            int i3 = i;
            int i4 = 0;
            while (i4 < 4) {
                fArr[i3] = (fArr2[i2 + 0] * fArr3[i4 + 0]) + (fArr2[i2 + 1] * fArr3[i4 + 5]) + (fArr2[i2 + 2] * fArr3[i4 + 10]) + (fArr2[i2 + 3] * fArr3[i4 + 15]);
                i4++;
                i3++;
            }
            i = i3 + 1;
            fArr[i3] = (fArr2[i2 + 0] * fArr3[4]) + (fArr2[i2 + 1] * fArr3[9]) + (fArr2[i2 + 2] * fArr3[14]) + (fArr2[i2 + 3] * fArr3[19]) + fArr2[i2 + 4];
        }
        if (fArr != this.mArray) {
            System.arraycopy(fArr, 0, this.mArray, 0, 20);
        }
    }

    public void preConcat(ColorMatrix colorMatrix) {
        setConcat(this, colorMatrix);
    }

    public void postConcat(ColorMatrix colorMatrix) {
        setConcat(colorMatrix, this);
    }

    public void setSaturation(float f) {
        reset();
        float[] fArr = this.mArray;
        float f2 = 1.0f - f;
        float f3 = 0.213f * f2;
        float f4 = 0.715f * f2;
        float f5 = 0.072f * f2;
        fArr[0] = f3 + f;
        fArr[1] = f4;
        fArr[2] = f5;
        fArr[5] = f3;
        fArr[6] = f4 + f;
        fArr[7] = f5;
        fArr[10] = f3;
        fArr[11] = f4;
        fArr[12] = f5 + f;
    }

    public void setRGB2YUV() {
        reset();
        float[] fArr = this.mArray;
        fArr[0] = 0.299f;
        fArr[1] = 0.587f;
        fArr[2] = 0.114f;
        fArr[5] = -0.16874f;
        fArr[6] = -0.33126f;
        fArr[7] = 0.5f;
        fArr[10] = 0.5f;
        fArr[11] = -0.41869f;
        fArr[12] = -0.08131f;
    }

    public void setYUV2RGB() {
        reset();
        float[] fArr = this.mArray;
        fArr[2] = 1.402f;
        fArr[5] = 1.0f;
        fArr[6] = -0.34414f;
        fArr[7] = -0.71414f;
        fArr[10] = 1.0f;
        fArr[11] = 1.772f;
        fArr[12] = 0.0f;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ColorMatrix)) {
            return false;
        }
        float[] fArr = ((ColorMatrix) obj).mArray;
        for (int i = 0; i < 20; i++) {
            if (fArr[i] != this.mArray[i]) {
                return false;
            }
        }
        return true;
    }
}
