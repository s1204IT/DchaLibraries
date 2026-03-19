package com.android.gallery3d.filtershow.filters;

import java.lang.reflect.Array;

public class SplineMath {
    double[] mDerivatives;
    double[][] mPoints;

    SplineMath(int i) {
        this.mPoints = (double[][]) Array.newInstance((Class<?>) double.class, 6, 2);
        this.mPoints = (double[][]) Array.newInstance((Class<?>) double.class, i, 2);
    }

    public void setPoint(int i, double d, double d2) {
        this.mPoints[i][0] = d;
        this.mPoints[i][1] = d2;
        this.mDerivatives = null;
    }

    public float[][] calculatetCurve(int i) {
        float[][] fArr = (float[][]) Array.newInstance((Class<?>) float.class, i, 2);
        double[][] dArr = (double[][]) Array.newInstance((Class<?>) double.class, this.mPoints.length, 2);
        for (int i2 = 0; i2 < this.mPoints.length; i2++) {
            dArr[i2][0] = this.mPoints[i2][0];
            dArr[i2][1] = this.mPoints[i2][1];
        }
        double[] dArrSolveSystem = solveSystem(dArr);
        float f = (float) dArr[0][0];
        float f2 = (float) dArr[dArr.length - 1][0];
        fArr[0][0] = (float) dArr[0][0];
        fArr[0][1] = (float) dArr[0][1];
        int length = fArr.length - 1;
        fArr[length][0] = (float) dArr[dArr.length - 1][0];
        fArr[length][1] = (float) dArr[dArr.length - 1][1];
        for (int i3 = 0; i3 < fArr.length; i3++) {
            double length2 = ((i3 * (f2 - f)) / (fArr.length - 1)) + f;
            int i4 = 0;
            for (int i5 = 0; i5 < dArr.length - 1; i5++) {
                if (length2 >= dArr[i5][0] && length2 <= dArr[i5 + 1][0]) {
                    i4 = i5;
                }
            }
            double[] dArr2 = dArr[i4];
            int i6 = i4 + 1;
            double[] dArr3 = dArr[i6];
            if (length2 > dArr3[0]) {
                fArr[i3][0] = (float) dArr3[0];
                fArr[i3][1] = (float) dArr3[1];
            } else {
                double d = dArr2[0];
                double d2 = dArr3[0];
                double d3 = dArr2[1];
                double d4 = dArr3[1];
                double d5 = d2 - d;
                double d6 = (length2 - d) / d5;
                double d7 = 1.0d - d6;
                double d8 = (d3 * d7) + (d4 * d6) + (((d5 * d5) / 6.0d) * (((((d7 * d7) * d7) - d7) * dArrSolveSystem[i4]) + ((((d6 * d6) * d6) - d6) * dArrSolveSystem[i6])));
                fArr[i3][0] = (float) length2;
                fArr[i3][1] = (float) d8;
            }
        }
        return fArr;
    }

    double[] solveSystem(double[][] dArr) {
        int length = dArr.length;
        double[][] dArr2 = (double[][]) Array.newInstance((Class<?>) double.class, length, 3);
        double[] dArr3 = new double[length];
        double[] dArr4 = new double[length];
        dArr2[0][1] = 1.0d;
        int i = length - 1;
        dArr2[i][1] = 1.0d;
        int i2 = 1;
        while (i2 < i) {
            int i3 = i2 - 1;
            double d = dArr[i2][0] - dArr[i3][0];
            int i4 = i2 + 1;
            double d2 = dArr[i4][0] - dArr[i3][0];
            double d3 = dArr[i4][0] - dArr[i2][0];
            double d4 = dArr[i4][1] - dArr[i2][1];
            double d5 = dArr[i2][1] - dArr[i3][1];
            dArr2[i2][0] = 0.16666666666666666d * d;
            dArr2[i2][1] = 0.3333333333333333d * d2;
            dArr2[i2][2] = 0.16666666666666666d * d3;
            dArr3[i2] = (d4 / d3) - (d5 / d);
            i2 = i4;
        }
        for (int i5 = 1; i5 < length; i5++) {
            int i6 = i5 - 1;
            double d6 = dArr2[i5][0] / dArr2[i6][1];
            dArr2[i5][1] = dArr2[i5][1] - (dArr2[i6][2] * d6);
            dArr3[i5] = dArr3[i5] - (d6 * dArr3[i6]);
        }
        dArr4[i] = dArr3[i] / dArr2[i][1];
        for (int i7 = length - 2; i7 >= 0; i7--) {
            dArr4[i7] = (dArr3[i7] - (dArr2[i7][2] * dArr4[i7 + 1])) / dArr2[i7][1];
        }
        return dArr4;
    }
}
