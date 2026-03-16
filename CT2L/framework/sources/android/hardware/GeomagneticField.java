package android.hardware;

import android.view.WindowManager;
import java.util.GregorianCalendar;

public class GeomagneticField {
    static final boolean $assertionsDisabled;
    private static final long BASE_TIME;
    private static final float[][] DELTA_G;
    private static final float[][] DELTA_H;
    private static final float EARTH_REFERENCE_RADIUS_KM = 6371.2f;
    private static final float EARTH_SEMI_MAJOR_AXIS_KM = 6378.137f;
    private static final float EARTH_SEMI_MINOR_AXIS_KM = 6356.7524f;
    private static final float[][] G_COEFF;
    private static final float[][] H_COEFF;
    private static final float[][] SCHMIDT_QUASI_NORM_FACTORS;
    private float mGcLatitudeRad;
    private float mGcLongitudeRad;
    private float mGcRadiusKm;
    private float mX;
    private float mY;
    private float mZ;

    static {
        $assertionsDisabled = !GeomagneticField.class.desiredAssertionStatus();
        G_COEFF = new float[][]{new float[]{0.0f}, new float[]{-29496.6f, -1586.3f}, new float[]{-2396.6f, 3026.1f, 1668.6f}, new float[]{1340.1f, -2326.2f, 1231.9f, 634.0f}, new float[]{912.6f, 808.9f, 166.7f, -357.1f, 89.4f}, new float[]{-230.9f, 357.2f, 200.3f, -141.1f, -163.0f, -7.8f}, new float[]{72.8f, 68.6f, 76.0f, -141.4f, -22.8f, 13.2f, -77.9f}, new float[]{80.5f, -75.1f, -4.7f, 45.3f, 13.9f, 10.4f, 1.7f, 4.9f}, new float[]{24.4f, 8.1f, -14.5f, -5.6f, -19.3f, 11.5f, 10.9f, -14.1f, -3.7f}, new float[]{5.4f, 9.4f, 3.4f, -5.2f, 3.1f, -12.4f, -0.7f, 8.4f, -8.5f, -10.1f}, new float[]{-2.0f, -6.3f, 0.9f, -1.1f, -0.2f, 2.5f, -0.3f, 2.2f, 3.1f, -1.0f, -2.8f}, new float[]{3.0f, -1.5f, -2.1f, 1.7f, -0.5f, 0.5f, -0.8f, 0.4f, 1.8f, 0.1f, 0.7f, 3.8f}, new float[]{-2.2f, -0.2f, 0.3f, 1.0f, -0.6f, 0.9f, -0.1f, 0.5f, -0.4f, -0.4f, 0.2f, -0.8f, 0.0f}};
        H_COEFF = new float[][]{new float[]{0.0f}, new float[]{0.0f, 4944.4f}, new float[]{0.0f, -2707.7f, -576.1f}, new float[]{0.0f, -160.2f, 251.9f, -536.6f}, new float[]{0.0f, 286.4f, -211.2f, 164.3f, -309.1f}, new float[]{0.0f, 44.6f, 188.9f, -118.2f, 0.0f, 100.9f}, new float[]{0.0f, -20.8f, 44.1f, 61.5f, -66.3f, 3.1f, 55.0f}, new float[]{0.0f, -57.9f, -21.1f, 6.5f, 24.9f, 7.0f, -27.7f, -3.3f}, new float[]{0.0f, 11.0f, -20.0f, 11.9f, -17.4f, 16.7f, 7.0f, -10.8f, 1.7f}, new float[]{0.0f, -20.5f, 11.5f, 12.8f, -7.2f, -7.4f, 8.0f, 2.1f, -6.1f, 7.0f}, new float[]{0.0f, 2.8f, -0.1f, 4.7f, 4.4f, -7.2f, -1.0f, -3.9f, -2.0f, -2.0f, -8.3f}, new float[]{0.0f, 0.2f, 1.7f, -0.6f, -1.8f, 0.9f, -0.4f, -2.5f, -1.3f, -2.1f, -1.9f, -1.8f}, new float[]{0.0f, -0.9f, 0.3f, 2.1f, -2.5f, 0.5f, 0.6f, 0.0f, 0.1f, 0.3f, -0.9f, -0.2f, 0.9f}};
        DELTA_G = new float[][]{new float[]{0.0f}, new float[]{11.6f, 16.5f}, new float[]{-12.1f, -4.4f, 1.9f}, new float[]{0.4f, -4.1f, -2.9f, -7.7f}, new float[]{-1.8f, 2.3f, -8.7f, 4.6f, -2.1f}, new float[]{-1.0f, 0.6f, -1.8f, -1.0f, 0.9f, 1.0f}, new float[]{-0.2f, -0.2f, -0.1f, 2.0f, -1.7f, -0.3f, 1.7f}, new float[]{0.1f, -0.1f, -0.6f, 1.3f, 0.4f, 0.3f, -0.7f, 0.6f}, new float[]{-0.1f, 0.1f, -0.6f, 0.2f, -0.2f, 0.3f, 0.3f, -0.6f, 0.2f}, new float[]{0.0f, -0.1f, 0.0f, 0.3f, -0.4f, -0.3f, 0.1f, -0.1f, -0.4f, -0.2f}, new float[]{0.0f, 0.0f, -0.1f, 0.2f, 0.0f, -0.1f, -0.2f, 0.0f, -0.1f, -0.2f, -0.2f}, new float[]{0.0f, 0.0f, 0.0f, 0.1f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -0.1f, 0.0f}, new float[]{0.0f, 0.0f, 0.1f, 0.1f, -0.1f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -0.1f, 0.1f}};
        DELTA_H = new float[][]{new float[]{0.0f}, new float[]{0.0f, -25.9f}, new float[]{0.0f, -22.5f, -11.8f}, new float[]{0.0f, 7.3f, -3.9f, -2.6f}, new float[]{0.0f, 1.1f, 2.7f, 3.9f, -0.8f}, new float[]{0.0f, 0.4f, 1.8f, 1.2f, 4.0f, -0.6f}, new float[]{0.0f, -0.2f, -2.1f, -0.4f, -0.6f, 0.5f, 0.9f}, new float[]{0.0f, 0.7f, 0.3f, -0.1f, -0.1f, -0.8f, -0.3f, 0.3f}, new float[]{0.0f, -0.1f, 0.2f, 0.4f, 0.4f, 0.1f, -0.1f, 0.4f, 0.3f}, new float[]{0.0f, 0.0f, -0.2f, 0.0f, -0.1f, 0.1f, 0.0f, -0.2f, 0.3f, 0.2f}, new float[]{0.0f, 0.1f, -0.1f, 0.0f, -0.1f, -0.1f, 0.0f, -0.1f, -0.2f, 0.0f, -0.1f}, new float[]{0.0f, 0.0f, 0.1f, 0.0f, 0.1f, 0.0f, 0.1f, 0.0f, -0.1f, -0.1f, 0.0f, -0.1f}, new float[]{0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.1f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f}};
        BASE_TIME = new GregorianCalendar(WindowManager.LayoutParams.TYPE_SYSTEM_ERROR, 1, 1).getTimeInMillis();
        SCHMIDT_QUASI_NORM_FACTORS = computeSchmidtQuasiNormFactors(G_COEFF.length);
    }

    public GeomagneticField(float gdLatitudeDeg, float gdLongitudeDeg, float altitudeMeters, long timeMillis) {
        int MAX_N = G_COEFF.length;
        float gdLatitudeDeg2 = Math.min(89.99999f, Math.max(-89.99999f, gdLatitudeDeg));
        computeGeocentricCoordinates(gdLatitudeDeg2, gdLongitudeDeg, altitudeMeters);
        if (!$assertionsDisabled && G_COEFF.length != H_COEFF.length) {
            throw new AssertionError();
        }
        LegendreTable legendre = new LegendreTable(MAX_N - 1, (float) (1.5707963267948966d - ((double) this.mGcLatitudeRad)));
        float[] relativeRadiusPower = new float[MAX_N + 2];
        relativeRadiusPower[0] = 1.0f;
        relativeRadiusPower[1] = EARTH_REFERENCE_RADIUS_KM / this.mGcRadiusKm;
        for (int i = 2; i < relativeRadiusPower.length; i++) {
            relativeRadiusPower[i] = relativeRadiusPower[i - 1] * relativeRadiusPower[1];
        }
        float[] sinMLon = new float[MAX_N];
        float[] cosMLon = new float[MAX_N];
        sinMLon[0] = 0.0f;
        cosMLon[0] = 1.0f;
        sinMLon[1] = (float) Math.sin(this.mGcLongitudeRad);
        cosMLon[1] = (float) Math.cos(this.mGcLongitudeRad);
        for (int m = 2; m < MAX_N; m++) {
            int x = m >> 1;
            sinMLon[m] = (sinMLon[m - x] * cosMLon[x]) + (cosMLon[m - x] * sinMLon[x]);
            cosMLon[m] = (cosMLon[m - x] * cosMLon[x]) - (sinMLon[m - x] * sinMLon[x]);
        }
        float inverseCosLatitude = 1.0f / ((float) Math.cos(this.mGcLatitudeRad));
        float yearsSinceBase = (timeMillis - BASE_TIME) / 3.1536E10f;
        float gcX = 0.0f;
        float gcY = 0.0f;
        float gcZ = 0.0f;
        for (int n = 1; n < MAX_N; n++) {
            for (int m2 = 0; m2 <= n; m2++) {
                float g = G_COEFF[n][m2] + (DELTA_G[n][m2] * yearsSinceBase);
                float h = H_COEFF[n][m2] + (DELTA_H[n][m2] * yearsSinceBase);
                gcX += relativeRadiusPower[n + 2] * ((cosMLon[m2] * g) + (sinMLon[m2] * h)) * legendre.mPDeriv[n][m2] * SCHMIDT_QUASI_NORM_FACTORS[n][m2];
                gcY += relativeRadiusPower[n + 2] * m2 * ((sinMLon[m2] * g) - (cosMLon[m2] * h)) * legendre.mP[n][m2] * SCHMIDT_QUASI_NORM_FACTORS[n][m2] * inverseCosLatitude;
                gcZ -= ((((n + 1) * relativeRadiusPower[n + 2]) * ((cosMLon[m2] * g) + (sinMLon[m2] * h))) * legendre.mP[n][m2]) * SCHMIDT_QUASI_NORM_FACTORS[n][m2];
            }
        }
        double latDiffRad = Math.toRadians(gdLatitudeDeg2) - ((double) this.mGcLatitudeRad);
        this.mX = (float) ((((double) gcX) * Math.cos(latDiffRad)) + (((double) gcZ) * Math.sin(latDiffRad)));
        this.mY = gcY;
        this.mZ = (float) ((((double) (-gcX)) * Math.sin(latDiffRad)) + (((double) gcZ) * Math.cos(latDiffRad)));
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
        return (float) Math.sqrt((this.mX * this.mX) + (this.mY * this.mY));
    }

    public float getFieldStrength() {
        return (float) Math.sqrt((this.mX * this.mX) + (this.mY * this.mY) + (this.mZ * this.mZ));
    }

    private void computeGeocentricCoordinates(float gdLatitudeDeg, float gdLongitudeDeg, float altitudeMeters) {
        float altitudeKm = altitudeMeters / 1000.0f;
        double gdLatRad = Math.toRadians(gdLatitudeDeg);
        float clat = (float) Math.cos(gdLatRad);
        float slat = (float) Math.sin(gdLatRad);
        float tlat = slat / clat;
        float latRad = (float) Math.sqrt((4.0680636E7f * clat * clat) + (4.04083E7f * slat * slat));
        this.mGcLatitudeRad = (float) Math.atan((((latRad * altitudeKm) + 4.04083E7f) * tlat) / ((latRad * altitudeKm) + 4.0680636E7f));
        this.mGcLongitudeRad = (float) Math.toRadians(gdLongitudeDeg);
        float radSq = (altitudeKm * altitudeKm) + (2.0f * altitudeKm * ((float) Math.sqrt((4.0680636E7f * clat * clat) + (4.04083E7f * slat * slat)))) + (((((4.0680636E7f * 4.0680636E7f) * clat) * clat) + (((4.04083E7f * 4.04083E7f) * slat) * slat)) / (((4.0680636E7f * clat) * clat) + ((4.04083E7f * slat) * slat)));
        this.mGcRadiusKm = (float) Math.sqrt(radSq);
    }

    private static class LegendreTable {
        static final boolean $assertionsDisabled;
        public final float[][] mP;
        public final float[][] mPDeriv;

        static {
            $assertionsDisabled = !GeomagneticField.class.desiredAssertionStatus();
        }

        public LegendreTable(int maxN, float thetaRad) {
            float cos = (float) Math.cos(thetaRad);
            float sin = (float) Math.sin(thetaRad);
            this.mP = new float[maxN + 1][];
            this.mPDeriv = new float[maxN + 1][];
            this.mP[0] = new float[]{1.0f};
            this.mPDeriv[0] = new float[]{0.0f};
            for (int n = 1; n <= maxN; n++) {
                this.mP[n] = new float[n + 1];
                this.mPDeriv[n] = new float[n + 1];
                for (int m = 0; m <= n; m++) {
                    if (n == m) {
                        this.mP[n][m] = this.mP[n - 1][m - 1] * sin;
                        this.mPDeriv[n][m] = (this.mP[n - 1][m - 1] * cos) + (this.mPDeriv[n - 1][m - 1] * sin);
                    } else if (n == 1 || m == n - 1) {
                        this.mP[n][m] = this.mP[n - 1][m] * cos;
                        this.mPDeriv[n][m] = ((-sin) * this.mP[n - 1][m]) + (this.mPDeriv[n - 1][m] * cos);
                    } else {
                        if (!$assertionsDisabled && (n <= 1 || m >= n - 1)) {
                            throw new AssertionError();
                        }
                        float k = (((n - 1) * (n - 1)) - (m * m)) / (((n * 2) - 1) * ((n * 2) - 3));
                        this.mP[n][m] = (this.mP[n - 1][m] * cos) - (this.mP[n - 2][m] * k);
                        this.mPDeriv[n][m] = (((-sin) * this.mP[n - 1][m]) + (this.mPDeriv[n - 1][m] * cos)) - (this.mPDeriv[n - 2][m] * k);
                    }
                }
            }
        }
    }

    private static float[][] computeSchmidtQuasiNormFactors(int maxN) {
        float[][] schmidtQuasiNorm = new float[maxN + 1][];
        schmidtQuasiNorm[0] = new float[]{1.0f};
        for (int n = 1; n <= maxN; n++) {
            schmidtQuasiNorm[n] = new float[n + 1];
            schmidtQuasiNorm[n][0] = (schmidtQuasiNorm[n - 1][0] * ((n * 2) - 1)) / n;
            int m = 1;
            while (m <= n) {
                schmidtQuasiNorm[n][m] = ((float) Math.sqrt(((m == 1 ? 2 : 1) * ((n - m) + 1)) / (n + m))) * schmidtQuasiNorm[n][m - 1];
                m++;
            }
        }
        return schmidtQuasiNorm;
    }
}
