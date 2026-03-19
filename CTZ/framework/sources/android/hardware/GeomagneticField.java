package android.hardware;

import android.view.WindowManager;
import java.util.GregorianCalendar;

public class GeomagneticField {
    static final boolean $assertionsDisabled = false;
    private static final float EARTH_REFERENCE_RADIUS_KM = 6371.2f;
    private static final float EARTH_SEMI_MAJOR_AXIS_KM = 6378.137f;
    private static final float EARTH_SEMI_MINOR_AXIS_KM = 6356.7524f;
    private float mGcLatitudeRad;
    private float mGcLongitudeRad;
    private float mGcRadiusKm;
    private float mX;
    private float mY;
    private float mZ;
    private static final float[][] G_COEFF = {new float[]{0.0f}, new float[]{-29438.5f, -1501.1f}, new float[]{-2445.3f, 3012.5f, 1676.6f}, new float[]{1351.1f, -2352.3f, 1225.6f, 581.9f}, new float[]{907.2f, 813.7f, 120.3f, -335.0f, 70.3f}, new float[]{-232.6f, 360.1f, 192.4f, -141.0f, -157.4f, 4.3f}, new float[]{69.5f, 67.4f, 72.8f, -129.8f, -29.0f, 13.2f, -70.9f}, new float[]{81.6f, -76.1f, -6.8f, 51.9f, 15.0f, 9.3f, -2.8f, 6.7f}, new float[]{24.0f, 8.6f, -16.9f, -3.2f, -20.6f, 13.3f, 11.7f, -16.0f, -2.0f}, new float[]{5.4f, 8.8f, 3.1f, -3.1f, 0.6f, -13.3f, -0.1f, 8.7f, -9.1f, -10.5f}, new float[]{-1.9f, -6.5f, 0.2f, 0.6f, -0.6f, 1.7f, -0.7f, 2.1f, 2.3f, -1.8f, -3.6f}, new float[]{3.1f, -1.5f, -2.3f, 2.1f, -0.9f, 0.6f, -0.7f, 0.2f, 1.7f, -0.2f, 0.4f, 3.5f}, new float[]{-2.0f, -0.3f, 0.4f, 1.3f, -0.9f, 0.9f, 0.1f, 0.5f, -0.4f, -0.4f, 0.2f, -0.9f, 0.0f}};
    private static final float[][] H_COEFF = {new float[]{0.0f}, new float[]{0.0f, 4796.2f}, new float[]{0.0f, -2845.6f, -642.0f}, new float[]{0.0f, -115.3f, 245.0f, -538.3f}, new float[]{0.0f, 283.4f, -188.6f, 180.9f, -329.5f}, new float[]{0.0f, 47.4f, 196.9f, -119.4f, 16.1f, 100.1f}, new float[]{0.0f, -20.7f, 33.2f, 58.8f, -66.5f, 7.3f, 62.5f}, new float[]{0.0f, -54.1f, -19.4f, 5.6f, 24.4f, 3.3f, -27.5f, -2.3f}, new float[]{0.0f, 10.2f, -18.1f, 13.2f, -14.6f, 16.2f, 5.7f, -9.1f, 2.2f}, new float[]{0.0f, -21.6f, 10.8f, 11.7f, -6.8f, -6.9f, 7.8f, 1.0f, -3.9f, 8.5f}, new float[]{0.0f, 3.3f, -0.3f, 4.6f, 4.4f, -7.9f, -0.6f, -4.1f, -2.8f, -1.1f, -8.7f}, new float[]{0.0f, -0.1f, 2.1f, -0.7f, -1.1f, 0.7f, -0.2f, -2.1f, -1.5f, -2.5f, -2.0f, -2.3f}, new float[]{0.0f, -1.0f, 0.5f, 1.8f, -2.2f, 0.3f, 0.7f, -0.1f, 0.3f, 0.2f, -0.9f, -0.2f, 0.7f}};
    private static final float[][] DELTA_G = {new float[]{0.0f}, new float[]{10.7f, 17.9f}, new float[]{-8.6f, -3.3f, 2.4f}, new float[]{3.1f, -6.2f, -0.4f, -10.4f}, new float[]{-0.4f, 0.8f, -9.2f, 4.0f, -4.2f}, new float[]{-0.2f, 0.1f, -1.4f, 0.0f, 1.3f, 3.8f}, new float[]{-0.5f, -0.2f, -0.6f, 2.4f, -1.1f, 0.3f, 1.5f}, new float[]{0.2f, -0.2f, -0.4f, 1.3f, 0.2f, -0.4f, -0.9f, 0.3f}, new float[]{0.0f, 0.1f, -0.5f, 0.5f, -0.2f, 0.4f, 0.2f, -0.4f, 0.3f}, new float[]{0.0f, -0.1f, -0.1f, 0.4f, -0.5f, -0.2f, 0.1f, 0.0f, -0.2f, -0.1f}, new float[]{0.0f, 0.0f, -0.1f, 0.3f, -0.1f, -0.1f, -0.1f, 0.0f, -0.2f, -0.1f, -0.2f}, new float[]{0.0f, 0.0f, -0.1f, 0.1f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -0.1f, -0.1f}, new float[]{0.1f, 0.0f, 0.0f, 0.1f, -0.1f, 0.0f, 0.1f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f}};
    private static final float[][] DELTA_H = {new float[]{0.0f}, new float[]{0.0f, -26.8f}, new float[]{0.0f, -27.1f, -13.3f}, new float[]{0.0f, 8.4f, -0.4f, 2.3f}, new float[]{0.0f, -0.6f, 5.3f, 3.0f, -5.3f}, new float[]{0.0f, 0.4f, 1.6f, -1.1f, 3.3f, 0.1f}, new float[]{0.0f, 0.0f, -2.2f, -0.7f, 0.1f, 1.0f, 1.3f}, new float[]{0.0f, 0.7f, 0.5f, -0.2f, -0.1f, -0.7f, 0.1f, 0.1f}, new float[]{0.0f, -0.3f, 0.3f, 0.3f, 0.6f, -0.1f, -0.2f, 0.3f, 0.0f}, new float[]{0.0f, -0.2f, -0.1f, -0.2f, 0.1f, 0.1f, 0.0f, -0.2f, 0.4f, 0.3f}, new float[]{0.0f, 0.1f, -0.1f, 0.0f, 0.0f, -0.2f, 0.1f, -0.1f, -0.2f, 0.1f, -0.1f}, new float[]{0.0f, 0.0f, 0.1f, 0.0f, 0.1f, 0.0f, 0.0f, 0.1f, 0.0f, -0.1f, 0.0f, -0.1f}, new float[]{0.0f, 0.0f, 0.0f, -0.1f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f}};
    private static final long BASE_TIME = new GregorianCalendar(WindowManager.LayoutParams.TYPE_SECURE_SYSTEM_OVERLAY, 1, 1).getTimeInMillis();
    private static final float[][] SCHMIDT_QUASI_NORM_FACTORS = computeSchmidtQuasiNormFactors(G_COEFF.length);

    public GeomagneticField(float f, float f2, float f3, long j) {
        int length = G_COEFF.length;
        float fMin = Math.min(89.99999f, Math.max(-89.99999f, f));
        computeGeocentricCoordinates(fMin, f2, f3);
        LegendreTable legendreTable = new LegendreTable(length - 1, (float) (1.5707963267948966d - ((double) this.mGcLatitudeRad)));
        float[] fArr = new float[length + 2];
        int i = 0;
        fArr[0] = 1.0f;
        int i2 = 1;
        fArr[1] = EARTH_REFERENCE_RADIUS_KM / this.mGcRadiusKm;
        for (int i3 = 2; i3 < fArr.length; i3++) {
            fArr[i3] = fArr[i3 - 1] * fArr[1];
        }
        float[] fArr2 = new float[length];
        float[] fArr3 = new float[length];
        float f4 = 0.0f;
        fArr2[0] = 0.0f;
        fArr3[0] = 1.0f;
        fArr2[1] = (float) Math.sin(this.mGcLongitudeRad);
        fArr3[1] = (float) Math.cos(this.mGcLongitudeRad);
        for (int i4 = 2; i4 < length; i4++) {
            int i5 = i4 >> 1;
            int i6 = i4 - i5;
            fArr2[i4] = (fArr2[i6] * fArr3[i5]) + (fArr3[i6] * fArr2[i5]);
            fArr3[i4] = (fArr3[i6] * fArr3[i5]) - (fArr2[i6] * fArr2[i5]);
        }
        float fCos = 1.0f / ((float) Math.cos(this.mGcLatitudeRad));
        float f5 = (j - BASE_TIME) / 3.1536E10f;
        float f6 = 0.0f;
        float f7 = 0.0f;
        while (i2 < length) {
            float f8 = f6;
            float f9 = f4;
            int i7 = i;
            while (i7 <= i2) {
                float f10 = G_COEFF[i2][i7] + (DELTA_G[i2][i7] * f5);
                float f11 = H_COEFF[i2][i7] + (DELTA_H[i2][i7] * f5);
                int i8 = i2 + 2;
                f9 += fArr[i8] * ((fArr3[i7] * f10) + (fArr2[i7] * f11)) * legendreTable.mPDeriv[i2][i7] * SCHMIDT_QUASI_NORM_FACTORS[i2][i7];
                f7 += fArr[i8] * i7 * ((fArr2[i7] * f10) - (fArr3[i7] * f11)) * legendreTable.mP[i2][i7] * SCHMIDT_QUASI_NORM_FACTORS[i2][i7] * fCos;
                f8 -= ((((i2 + 1) * fArr[i8]) * ((f10 * fArr3[i7]) + (f11 * fArr2[i7]))) * legendreTable.mP[i2][i7]) * SCHMIDT_QUASI_NORM_FACTORS[i2][i7];
                i7++;
                length = length;
            }
            i2++;
            f4 = f9;
            f6 = f8;
            i = 0;
        }
        double radians = Math.toRadians(fMin) - ((double) this.mGcLatitudeRad);
        double d = f6;
        this.mX = (float) ((((double) f4) * Math.cos(radians)) + (Math.sin(radians) * d));
        this.mY = f7;
        this.mZ = (float) ((((double) (-f4)) * Math.sin(radians)) + (d * Math.cos(radians)));
    }

    public float getX() {
        return this.mX;
    }

    public float getY() {
        return this.mY;
    }

    public float getZ() {
        return this.mZ;
    }

    public float getDeclination() {
        return (float) Math.toDegrees(Math.atan2(this.mY, this.mX));
    }

    public float getInclination() {
        return (float) Math.toDegrees(Math.atan2(this.mZ, getHorizontalStrength()));
    }

    public float getHorizontalStrength() {
        return (float) Math.hypot(this.mX, this.mY);
    }

    public float getFieldStrength() {
        return (float) Math.sqrt((this.mX * this.mX) + (this.mY * this.mY) + (this.mZ * this.mZ));
    }

    private void computeGeocentricCoordinates(float f, float f2, float f3) {
        double radians = Math.toRadians(f);
        float fCos = (float) Math.cos(radians);
        float fSin = (float) Math.sin(radians);
        float fSqrt = ((float) Math.sqrt((4.0680636E7f * fCos * fCos) + (4.04083E7f * fSin * fSin))) * (f3 / 1000.0f);
        this.mGcLatitudeRad = (float) Math.atan(((fSin / fCos) * (4.04083E7f + fSqrt)) / (fSqrt + 4.0680636E7f));
        this.mGcLongitudeRad = (float) Math.toRadians(f2);
        this.mGcRadiusKm = (float) Math.sqrt((r12 * r12) + (2.0f * r12 * ((float) Math.sqrt(r5))) + ((((1.6549141E15f * fCos) * fCos) + ((1.6328307E15f * fSin) * fSin)) / r3));
    }

    private static class LegendreTable {
        static final boolean $assertionsDisabled = false;
        public final float[][] mP;
        public final float[][] mPDeriv;

        public LegendreTable(int i, float f) {
            int i2;
            double d = f;
            float fCos = (float) Math.cos(d);
            float fSin = (float) Math.sin(d);
            int i3 = i + 1;
            this.mP = new float[i3][];
            this.mPDeriv = new float[i3][];
            this.mP[0] = new float[]{1.0f};
            this.mPDeriv[0] = new float[]{0.0f};
            int i4 = 1;
            while (i4 <= i) {
                int i5 = i4 + 1;
                this.mP[i4] = new float[i5];
                this.mPDeriv[i4] = new float[i5];
                for (int i6 = 0; i6 <= i4; i6++) {
                    if (i4 == i6) {
                        int i7 = i4 - 1;
                        int i8 = i6 - 1;
                        this.mP[i4][i6] = this.mP[i7][i8] * fSin;
                        this.mPDeriv[i4][i6] = (this.mP[i7][i8] * fCos) + (this.mPDeriv[i7][i8] * fSin);
                    } else if (i4 == 1 || i6 == i4 - 1) {
                        int i9 = i4 - 1;
                        this.mP[i4][i6] = this.mP[i9][i6] * fCos;
                        this.mPDeriv[i4][i6] = ((-fSin) * this.mP[i9][i6]) + (this.mPDeriv[i9][i6] * fCos);
                    } else {
                        int i10 = 2 * i4;
                        float f2 = ((i2 * i2) - (i6 * i6)) / ((i10 - 1) * (i10 - 3));
                        int i11 = i4 - 2;
                        this.mP[i4][i6] = (this.mP[i2][i6] * fCos) - (this.mP[i11][i6] * f2);
                        this.mPDeriv[i4][i6] = (((-fSin) * this.mP[i2][i6]) + (this.mPDeriv[i2][i6] * fCos)) - (f2 * this.mPDeriv[i11][i6]);
                    }
                }
                i4 = i5;
            }
        }
    }

    private static float[][] computeSchmidtQuasiNormFactors(int i) {
        float[][] fArr = new float[i + 1][];
        fArr[0] = new float[]{1.0f};
        int i2 = 1;
        while (i2 <= i) {
            int i3 = i2 + 1;
            fArr[i2] = new float[i3];
            fArr[i2][0] = (fArr[i2 - 1][0] * ((2 * i2) - 1)) / i2;
            int i4 = 1;
            while (i4 <= i2) {
                fArr[i2][i4] = fArr[i2][i4 - 1] * ((float) Math.sqrt((((i2 - i4) + 1) * (i4 == 1 ? 2 : 1)) / (i2 + i4)));
                i4++;
            }
            i2 = i3;
        }
        return fArr;
    }
}
