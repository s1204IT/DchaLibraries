package android.location;

import android.annotation.SystemApi;
import android.bluetooth.BluetoothHidDevice;
import android.net.wifi.WifiScanner;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.provider.SettingsStringUtil;
import android.util.Printer;
import android.util.TimeUtils;
import java.text.DecimalFormat;
import java.util.StringTokenizer;

public class Location implements Parcelable {
    public static final String EXTRA_COARSE_LOCATION = "coarseLocation";
    public static final String EXTRA_NO_GPS_LOCATION = "noGPSLocation";
    public static final int FORMAT_DEGREES = 0;
    public static final int FORMAT_MINUTES = 1;
    public static final int FORMAT_SECONDS = 2;
    private static final int HAS_ALTITUDE_MASK = 1;
    private static final int HAS_BEARING_ACCURACY_MASK = 128;
    private static final int HAS_BEARING_MASK = 4;
    private static final int HAS_HORIZONTAL_ACCURACY_MASK = 8;
    private static final int HAS_MOCK_PROVIDER_MASK = 16;
    private static final int HAS_SPEED_ACCURACY_MASK = 64;
    private static final int HAS_SPEED_MASK = 2;
    private static final int HAS_VERTICAL_ACCURACY_MASK = 32;
    private String mProvider;
    private static ThreadLocal<BearingDistanceCache> sBearingDistanceCache = new ThreadLocal<BearingDistanceCache>() {
        @Override
        protected BearingDistanceCache initialValue() {
            return new BearingDistanceCache();
        }
    };
    public static final Parcelable.Creator<Location> CREATOR = new Parcelable.Creator<Location>() {
        @Override
        public Location createFromParcel(Parcel parcel) {
            Location location = new Location(parcel.readString());
            location.mTime = parcel.readLong();
            location.mElapsedRealtimeNanos = parcel.readLong();
            location.mFieldsMask = parcel.readByte();
            location.mLatitude = parcel.readDouble();
            location.mLongitude = parcel.readDouble();
            location.mAltitude = parcel.readDouble();
            location.mSpeed = parcel.readFloat();
            location.mBearing = parcel.readFloat();
            location.mHorizontalAccuracyMeters = parcel.readFloat();
            location.mVerticalAccuracyMeters = parcel.readFloat();
            location.mSpeedAccuracyMetersPerSecond = parcel.readFloat();
            location.mBearingAccuracyDegrees = parcel.readFloat();
            location.mExtras = Bundle.setDefusable(parcel.readBundle(), true);
            return location;
        }

        @Override
        public Location[] newArray(int i) {
            return new Location[i];
        }
    };
    private long mTime = 0;
    private long mElapsedRealtimeNanos = 0;
    private double mLatitude = 0.0d;
    private double mLongitude = 0.0d;
    private double mAltitude = 0.0d;
    private float mSpeed = 0.0f;
    private float mBearing = 0.0f;
    private float mHorizontalAccuracyMeters = 0.0f;
    private float mVerticalAccuracyMeters = 0.0f;
    private float mSpeedAccuracyMetersPerSecond = 0.0f;
    private float mBearingAccuracyDegrees = 0.0f;
    private Bundle mExtras = null;
    private byte mFieldsMask = 0;

    public Location(String str) {
        this.mProvider = str;
    }

    public Location(Location location) {
        set(location);
    }

    public void set(Location location) {
        this.mProvider = location.mProvider;
        this.mTime = location.mTime;
        this.mElapsedRealtimeNanos = location.mElapsedRealtimeNanos;
        this.mFieldsMask = location.mFieldsMask;
        this.mLatitude = location.mLatitude;
        this.mLongitude = location.mLongitude;
        this.mAltitude = location.mAltitude;
        this.mSpeed = location.mSpeed;
        this.mBearing = location.mBearing;
        this.mHorizontalAccuracyMeters = location.mHorizontalAccuracyMeters;
        this.mVerticalAccuracyMeters = location.mVerticalAccuracyMeters;
        this.mSpeedAccuracyMetersPerSecond = location.mSpeedAccuracyMetersPerSecond;
        this.mBearingAccuracyDegrees = location.mBearingAccuracyDegrees;
        this.mExtras = location.mExtras == null ? null : new Bundle(location.mExtras);
    }

    public void reset() {
        this.mProvider = null;
        this.mTime = 0L;
        this.mElapsedRealtimeNanos = 0L;
        this.mFieldsMask = (byte) 0;
        this.mLatitude = 0.0d;
        this.mLongitude = 0.0d;
        this.mAltitude = 0.0d;
        this.mSpeed = 0.0f;
        this.mBearing = 0.0f;
        this.mHorizontalAccuracyMeters = 0.0f;
        this.mVerticalAccuracyMeters = 0.0f;
        this.mSpeedAccuracyMetersPerSecond = 0.0f;
        this.mBearingAccuracyDegrees = 0.0f;
        this.mExtras = null;
    }

    public static String convert(double d, int i) {
        if (d < -180.0d || d > 180.0d || Double.isNaN(d)) {
            throw new IllegalArgumentException("coordinate=" + d);
        }
        if (i != 0 && i != 1 && i != 2) {
            throw new IllegalArgumentException("outputType=" + i);
        }
        StringBuilder sb = new StringBuilder();
        if (d < 0.0d) {
            sb.append('-');
            d = -d;
        }
        DecimalFormat decimalFormat = new DecimalFormat("###.#####");
        if (i == 1 || i == 2) {
            int iFloor = (int) Math.floor(d);
            sb.append(iFloor);
            sb.append(':');
            d = (d - ((double) iFloor)) * 60.0d;
            if (i == 2) {
                int iFloor2 = (int) Math.floor(d);
                sb.append(iFloor2);
                sb.append(':');
                d = (d - ((double) iFloor2)) * 60.0d;
            }
        }
        sb.append(decimalFormat.format(d));
        return sb.toString();
    }

    public static double convert(String str) {
        boolean z;
        double d;
        boolean z2;
        double d2;
        if (str == null) {
            throw new NullPointerException("coordinate");
        }
        boolean z3 = false;
        if (str.charAt(0) != '-') {
            z = false;
        } else {
            str = str.substring(1);
            z = true;
        }
        StringTokenizer stringTokenizer = new StringTokenizer(str, SettingsStringUtil.DELIMITER);
        int iCountTokens = stringTokenizer.countTokens();
        if (iCountTokens < 1) {
            throw new IllegalArgumentException("coordinate=" + str);
        }
        try {
            String strNextToken = stringTokenizer.nextToken();
            if (iCountTokens == 1) {
                double d3 = Double.parseDouble(strNextToken);
                return z ? -d3 : d3;
            }
            String strNextToken2 = stringTokenizer.nextToken();
            int i = Integer.parseInt(strNextToken);
            if (stringTokenizer.hasMoreTokens()) {
                d = Integer.parseInt(strNextToken2);
                d2 = Double.parseDouble(stringTokenizer.nextToken());
                z2 = true;
            } else {
                d = Double.parseDouble(strNextToken2);
                z2 = false;
                d2 = 0.0d;
            }
            if (z && i == 180 && d == 0.0d && d2 == 0.0d) {
                z3 = true;
            }
            double d4 = i;
            if (d4 < 0.0d || (i > 179 && !z3)) {
                throw new IllegalArgumentException("coordinate=" + str);
            }
            if (d < 0.0d || d >= 60.0d || (z2 && d > 59.0d)) {
                throw new IllegalArgumentException("coordinate=" + str);
            }
            if (d2 < 0.0d || d2 >= 60.0d) {
                throw new IllegalArgumentException("coordinate=" + str);
            }
            double d5 = (((d4 * 3600.0d) + (d * 60.0d)) + d2) / 3600.0d;
            return z ? -d5 : d5;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("coordinate=" + str);
        }
    }

    private static void computeDistanceAndBearing(double d, double d2, double d3, double d4, BearingDistanceCache bearingDistanceCache) {
        double d5;
        double d6;
        double d7;
        double d8;
        double d9 = d * 0.017453292519943295d;
        double d10 = d3 * 0.017453292519943295d;
        double d11 = d2 * 0.017453292519943295d;
        double d12 = 0.017453292519943295d * d4;
        double d13 = d12 - d11;
        double dAtan = Math.atan(Math.tan(d9) * 0.996647189328169d);
        double dAtan2 = Math.atan(0.996647189328169d * Math.tan(d10));
        double dCos = Math.cos(dAtan);
        double dCos2 = Math.cos(dAtan2);
        double dSin = Math.sin(dAtan);
        double dSin2 = Math.sin(dAtan2);
        double d14 = dCos * dCos2;
        double d15 = dSin * dSin2;
        double d16 = d13;
        double dAtan22 = 0.0d;
        double d17 = 0.0d;
        double dCos3 = 0.0d;
        double d18 = 0.0d;
        double dSin3 = 0.0d;
        int i = 0;
        while (true) {
            if (i < 20) {
                dCos3 = Math.cos(d16);
                dSin3 = Math.sin(d16);
                double d19 = dCos2 * dSin3;
                double d20 = (dCos * dSin2) - ((dSin * dCos2) * dCos3);
                d6 = d10;
                double dSqrt = Math.sqrt((d19 * d19) + (d20 * d20));
                d5 = d9;
                double d21 = d15 + (d14 * dCos3);
                dAtan22 = Math.atan2(dSqrt, d21);
                if (dSqrt != 0.0d) {
                    d7 = (d14 * dSin3) / dSqrt;
                } else {
                    d7 = 0.0d;
                }
                double d22 = 1.0d - (d7 * d7);
                if (d22 != 0.0d) {
                    d8 = d21 - ((2.0d * d15) / d22);
                } else {
                    d8 = 0.0d;
                }
                double d23 = 0.006739496756586903d * d22;
                double d24 = 1.0d + ((d23 / 16384.0d) * (4096.0d + (((-768.0d) + ((320.0d - (175.0d * d23)) * d23)) * d23)));
                double d25 = (d23 / 1024.0d) * (256.0d + (d23 * ((-128.0d) + ((74.0d - (47.0d * d23)) * d23))));
                double d26 = 2.0955066698943685E-4d * d22 * (4.0d + ((4.0d - (3.0d * d22)) * 0.0033528106718309896d));
                double d27 = d8 * d8;
                double d28 = d25 * dSqrt * (d8 + ((d25 / 4.0d) * ((((-1.0d) + (2.0d * d27)) * d21) - ((((d25 / 6.0d) * d8) * ((-3.0d) + ((4.0d * dSqrt) * dSqrt))) * ((-3.0d) + (4.0d * d27))))));
                double d29 = d13 + ((1.0d - d26) * 0.0033528106718309896d * d7 * (dAtan22 + (dSqrt * d26 * (d8 + (d26 * d21 * ((-1.0d) + (2.0d * d8 * d8)))))));
                if (Math.abs((d29 - d16) / d29) >= 1.0E-12d) {
                    i++;
                    d16 = d29;
                    d17 = d28;
                    d10 = d6;
                    d9 = d5;
                    d18 = d24;
                } else {
                    d17 = d28;
                    d18 = d24;
                    break;
                }
            } else {
                d5 = d9;
                d6 = d10;
                break;
            }
        }
        bearingDistanceCache.mDistance = (float) (6356752.3142d * d18 * (dAtan22 - d17));
        double d30 = dSin2 * dCos;
        bearingDistanceCache.mInitialBearing = (float) (((double) ((float) Math.atan2(dCos2 * dSin3, d30 - ((dSin * dCos2) * dCos3)))) * 57.29577951308232d);
        bearingDistanceCache.mFinalBearing = (float) (((double) ((float) Math.atan2(dCos * dSin3, ((-dSin) * dCos2) + (d30 * dCos3)))) * 57.29577951308232d);
        bearingDistanceCache.mLat1 = d5;
        bearingDistanceCache.mLat2 = d6;
        bearingDistanceCache.mLon1 = d11;
        bearingDistanceCache.mLon2 = d12;
    }

    public static void distanceBetween(double d, double d2, double d3, double d4, float[] fArr) {
        if (fArr == null || fArr.length < 1) {
            throw new IllegalArgumentException("results is null or has length < 1");
        }
        BearingDistanceCache bearingDistanceCache = sBearingDistanceCache.get();
        computeDistanceAndBearing(d, d2, d3, d4, bearingDistanceCache);
        fArr[0] = bearingDistanceCache.mDistance;
        if (fArr.length > 1) {
            fArr[1] = bearingDistanceCache.mInitialBearing;
            if (fArr.length > 2) {
                fArr[2] = bearingDistanceCache.mFinalBearing;
            }
        }
    }

    public float distanceTo(Location location) {
        BearingDistanceCache bearingDistanceCache = sBearingDistanceCache.get();
        if (this.mLatitude != bearingDistanceCache.mLat1 || this.mLongitude != bearingDistanceCache.mLon1 || location.mLatitude != bearingDistanceCache.mLat2 || location.mLongitude != bearingDistanceCache.mLon2) {
            computeDistanceAndBearing(this.mLatitude, this.mLongitude, location.mLatitude, location.mLongitude, bearingDistanceCache);
        }
        return bearingDistanceCache.mDistance;
    }

    public float bearingTo(Location location) {
        BearingDistanceCache bearingDistanceCache = sBearingDistanceCache.get();
        if (this.mLatitude != bearingDistanceCache.mLat1 || this.mLongitude != bearingDistanceCache.mLon1 || location.mLatitude != bearingDistanceCache.mLat2 || location.mLongitude != bearingDistanceCache.mLon2) {
            computeDistanceAndBearing(this.mLatitude, this.mLongitude, location.mLatitude, location.mLongitude, bearingDistanceCache);
        }
        return bearingDistanceCache.mInitialBearing;
    }

    public String getProvider() {
        return this.mProvider;
    }

    public void setProvider(String str) {
        this.mProvider = str;
    }

    public long getTime() {
        return this.mTime;
    }

    public void setTime(long j) {
        this.mTime = j;
    }

    public long getElapsedRealtimeNanos() {
        return this.mElapsedRealtimeNanos;
    }

    public void setElapsedRealtimeNanos(long j) {
        this.mElapsedRealtimeNanos = j;
    }

    public double getLatitude() {
        return this.mLatitude;
    }

    public void setLatitude(double d) {
        this.mLatitude = d;
    }

    public double getLongitude() {
        return this.mLongitude;
    }

    public void setLongitude(double d) {
        this.mLongitude = d;
    }

    public boolean hasAltitude() {
        return (this.mFieldsMask & 1) != 0;
    }

    public double getAltitude() {
        return this.mAltitude;
    }

    public void setAltitude(double d) {
        this.mAltitude = d;
        this.mFieldsMask = (byte) (this.mFieldsMask | 1);
    }

    @Deprecated
    public void removeAltitude() {
        this.mAltitude = 0.0d;
        this.mFieldsMask = (byte) (this.mFieldsMask & (-2));
    }

    public boolean hasSpeed() {
        return (this.mFieldsMask & 2) != 0;
    }

    public float getSpeed() {
        return this.mSpeed;
    }

    public void setSpeed(float f) {
        this.mSpeed = f;
        this.mFieldsMask = (byte) (this.mFieldsMask | 2);
    }

    @Deprecated
    public void removeSpeed() {
        this.mSpeed = 0.0f;
        this.mFieldsMask = (byte) (this.mFieldsMask & (-3));
    }

    public boolean hasBearing() {
        return (this.mFieldsMask & 4) != 0;
    }

    public float getBearing() {
        return this.mBearing;
    }

    public void setBearing(float f) {
        while (f < 0.0f) {
            f += 360.0f;
        }
        while (f >= 360.0f) {
            f -= 360.0f;
        }
        this.mBearing = f;
        this.mFieldsMask = (byte) (this.mFieldsMask | 4);
    }

    @Deprecated
    public void removeBearing() {
        this.mBearing = 0.0f;
        this.mFieldsMask = (byte) (this.mFieldsMask & (-5));
    }

    public boolean hasAccuracy() {
        return (this.mFieldsMask & 8) != 0;
    }

    public float getAccuracy() {
        return this.mHorizontalAccuracyMeters;
    }

    public void setAccuracy(float f) {
        this.mHorizontalAccuracyMeters = f;
        this.mFieldsMask = (byte) (this.mFieldsMask | 8);
    }

    @Deprecated
    public void removeAccuracy() {
        this.mHorizontalAccuracyMeters = 0.0f;
        this.mFieldsMask = (byte) (this.mFieldsMask & (-9));
    }

    public boolean hasVerticalAccuracy() {
        return (this.mFieldsMask & 32) != 0;
    }

    public float getVerticalAccuracyMeters() {
        return this.mVerticalAccuracyMeters;
    }

    public void setVerticalAccuracyMeters(float f) {
        this.mVerticalAccuracyMeters = f;
        this.mFieldsMask = (byte) (this.mFieldsMask | 32);
    }

    @Deprecated
    public void removeVerticalAccuracy() {
        this.mVerticalAccuracyMeters = 0.0f;
        this.mFieldsMask = (byte) (this.mFieldsMask & (-33));
    }

    public boolean hasSpeedAccuracy() {
        return (this.mFieldsMask & BluetoothHidDevice.SUBCLASS1_KEYBOARD) != 0;
    }

    public float getSpeedAccuracyMetersPerSecond() {
        return this.mSpeedAccuracyMetersPerSecond;
    }

    public void setSpeedAccuracyMetersPerSecond(float f) {
        this.mSpeedAccuracyMetersPerSecond = f;
        this.mFieldsMask = (byte) (this.mFieldsMask | BluetoothHidDevice.SUBCLASS1_KEYBOARD);
    }

    @Deprecated
    public void removeSpeedAccuracy() {
        this.mSpeedAccuracyMetersPerSecond = 0.0f;
        this.mFieldsMask = (byte) (this.mFieldsMask & (-65));
    }

    public boolean hasBearingAccuracy() {
        return (this.mFieldsMask & 128) != 0;
    }

    public float getBearingAccuracyDegrees() {
        return this.mBearingAccuracyDegrees;
    }

    public void setBearingAccuracyDegrees(float f) {
        this.mBearingAccuracyDegrees = f;
        this.mFieldsMask = (byte) (this.mFieldsMask | 128);
    }

    @Deprecated
    public void removeBearingAccuracy() {
        this.mBearingAccuracyDegrees = 0.0f;
        this.mFieldsMask = (byte) (this.mFieldsMask & (-129));
    }

    @SystemApi
    public boolean isComplete() {
        return (this.mProvider == null || !hasAccuracy() || this.mTime == 0 || this.mElapsedRealtimeNanos == 0) ? false : true;
    }

    @SystemApi
    public void makeComplete() {
        if (this.mProvider == null) {
            this.mProvider = "?";
        }
        if (!hasAccuracy()) {
            this.mFieldsMask = (byte) (this.mFieldsMask | 8);
            this.mHorizontalAccuracyMeters = 100.0f;
        }
        if (this.mTime == 0) {
            this.mTime = System.currentTimeMillis();
        }
        if (this.mElapsedRealtimeNanos == 0) {
            this.mElapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos();
        }
    }

    public Bundle getExtras() {
        return this.mExtras;
    }

    public void setExtras(Bundle bundle) {
        this.mExtras = bundle == null ? null : new Bundle(bundle);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Location[");
        sb.append(this.mProvider);
        sb.append(String.format(" %.6f,%.6f", Double.valueOf(this.mLatitude), Double.valueOf(this.mLongitude)));
        if (hasAccuracy()) {
            sb.append(String.format(" hAcc=%.0f", Float.valueOf(this.mHorizontalAccuracyMeters)));
        } else {
            sb.append(" hAcc=???");
        }
        if (this.mTime == 0) {
            sb.append(" t=?!?");
        }
        if (this.mElapsedRealtimeNanos == 0) {
            sb.append(" et=?!?");
        } else {
            sb.append(" et=");
            TimeUtils.formatDuration(this.mElapsedRealtimeNanos / TimeUtils.NANOS_PER_MS, sb);
        }
        if (hasAltitude()) {
            sb.append(" alt=");
            sb.append(this.mAltitude);
        }
        if (hasSpeed()) {
            sb.append(" vel=");
            sb.append(this.mSpeed);
        }
        if (hasBearing()) {
            sb.append(" bear=");
            sb.append(this.mBearing);
        }
        if (hasVerticalAccuracy()) {
            sb.append(String.format(" vAcc=%.0f", Float.valueOf(this.mVerticalAccuracyMeters)));
        } else {
            sb.append(" vAcc=???");
        }
        if (hasSpeedAccuracy()) {
            sb.append(String.format(" sAcc=%.0f", Float.valueOf(this.mSpeedAccuracyMetersPerSecond)));
        } else {
            sb.append(" sAcc=???");
        }
        if (hasBearingAccuracy()) {
            sb.append(String.format(" bAcc=%.0f", Float.valueOf(this.mBearingAccuracyDegrees)));
        } else {
            sb.append(" bAcc=???");
        }
        if (isFromMockProvider()) {
            sb.append(" mock");
        }
        if (this.mExtras != null) {
            sb.append(" {");
            sb.append(this.mExtras);
            sb.append('}');
        }
        sb.append(']');
        return sb.toString();
    }

    public void dump(Printer printer, String str) {
        printer.println(str + toString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mProvider);
        parcel.writeLong(this.mTime);
        parcel.writeLong(this.mElapsedRealtimeNanos);
        parcel.writeByte(this.mFieldsMask);
        parcel.writeDouble(this.mLatitude);
        parcel.writeDouble(this.mLongitude);
        parcel.writeDouble(this.mAltitude);
        parcel.writeFloat(this.mSpeed);
        parcel.writeFloat(this.mBearing);
        parcel.writeFloat(this.mHorizontalAccuracyMeters);
        parcel.writeFloat(this.mVerticalAccuracyMeters);
        parcel.writeFloat(this.mSpeedAccuracyMetersPerSecond);
        parcel.writeFloat(this.mBearingAccuracyDegrees);
        parcel.writeBundle(this.mExtras);
    }

    public Location getExtraLocation(String str) {
        if (this.mExtras != null) {
            Parcelable parcelable = this.mExtras.getParcelable(str);
            if (parcelable instanceof Location) {
                return (Location) parcelable;
            }
            return null;
        }
        return null;
    }

    public void setExtraLocation(String str, Location location) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putParcelable(str, location);
    }

    public boolean isFromMockProvider() {
        return (this.mFieldsMask & WifiScanner.PnoSettings.PnoNetwork.FLAG_SAME_NETWORK) != 0;
    }

    @SystemApi
    public void setIsFromMockProvider(boolean z) {
        if (z) {
            this.mFieldsMask = (byte) (this.mFieldsMask | WifiScanner.PnoSettings.PnoNetwork.FLAG_SAME_NETWORK);
        } else {
            this.mFieldsMask = (byte) (this.mFieldsMask & (-17));
        }
    }

    private static class BearingDistanceCache {
        private float mDistance;
        private float mFinalBearing;
        private float mInitialBearing;
        private double mLat1;
        private double mLat2;
        private double mLon1;
        private double mLon2;

        private BearingDistanceCache() {
            this.mLat1 = 0.0d;
            this.mLon1 = 0.0d;
            this.mLat2 = 0.0d;
            this.mLon2 = 0.0d;
            this.mDistance = 0.0f;
            this.mInitialBearing = 0.0f;
            this.mFinalBearing = 0.0f;
        }
    }
}
