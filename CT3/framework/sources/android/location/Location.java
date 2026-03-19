package android.location;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
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
    private static final byte HAS_ACCURACY_MASK = 8;
    private static final byte HAS_ALTITUDE_MASK = 1;
    private static final byte HAS_BEARING_MASK = 4;
    private static final byte HAS_MOCK_PROVIDER_MASK = 16;
    private static final byte HAS_SPEED_MASK = 2;
    private String mProvider;
    private static ThreadLocal<BearingDistanceCache> sBearingDistanceCache = new ThreadLocal<BearingDistanceCache>() {
        @Override
        protected BearingDistanceCache initialValue() {
            return new BearingDistanceCache(null);
        }
    };
    public static final Parcelable.Creator<Location> CREATOR = new Parcelable.Creator<Location>() {
        @Override
        public Location createFromParcel(Parcel in) {
            String provider = in.readString();
            Location l = new Location(provider);
            l.mTime = in.readLong();
            l.mElapsedRealtimeNanos = in.readLong();
            l.mFieldsMask = in.readByte();
            l.mLatitude = in.readDouble();
            l.mLongitude = in.readDouble();
            l.mAltitude = in.readDouble();
            l.mSpeed = in.readFloat();
            l.mBearing = in.readFloat();
            l.mAccuracy = in.readFloat();
            l.mExtras = Bundle.setDefusable(in.readBundle(), true);
            return l;
        }

        @Override
        public Location[] newArray(int size) {
            return new Location[size];
        }
    };
    private long mTime = 0;
    private long mElapsedRealtimeNanos = 0;
    private double mLatitude = 0.0d;
    private double mLongitude = 0.0d;
    private double mAltitude = 0.0d;
    private float mSpeed = 0.0f;
    private float mBearing = 0.0f;
    private float mAccuracy = 0.0f;
    private Bundle mExtras = null;
    private byte mFieldsMask = 0;

    public Location(String provider) {
        this.mProvider = provider;
    }

    public Location(Location l) {
        set(l);
    }

    public void set(Location l) {
        this.mProvider = l.mProvider;
        this.mTime = l.mTime;
        this.mElapsedRealtimeNanos = l.mElapsedRealtimeNanos;
        this.mFieldsMask = l.mFieldsMask;
        this.mLatitude = l.mLatitude;
        this.mLongitude = l.mLongitude;
        this.mAltitude = l.mAltitude;
        this.mSpeed = l.mSpeed;
        this.mBearing = l.mBearing;
        this.mAccuracy = l.mAccuracy;
        this.mExtras = l.mExtras != null ? new Bundle(l.mExtras) : null;
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
        this.mAccuracy = 0.0f;
        this.mExtras = null;
    }

    public static String convert(double coordinate, int outputType) {
        if (coordinate < -180.0d || coordinate > 180.0d || Double.isNaN(coordinate)) {
            throw new IllegalArgumentException("coordinate=" + coordinate);
        }
        if (outputType != 0 && outputType != 1 && outputType != 2) {
            throw new IllegalArgumentException("outputType=" + outputType);
        }
        StringBuilder sb = new StringBuilder();
        if (coordinate < 0.0d) {
            sb.append('-');
            coordinate = -coordinate;
        }
        DecimalFormat df = new DecimalFormat("###.#####");
        if (outputType == 1 || outputType == 2) {
            int degrees = (int) Math.floor(coordinate);
            sb.append(degrees);
            sb.append(':');
            coordinate = (coordinate - ((double) degrees)) * 60.0d;
            if (outputType == 2) {
                int minutes = (int) Math.floor(coordinate);
                sb.append(minutes);
                sb.append(':');
                coordinate = (coordinate - ((double) minutes)) * 60.0d;
            }
        }
        sb.append(df.format(coordinate));
        return sb.toString();
    }

    public static double convert(String coordinate) {
        double min;
        if (coordinate == null) {
            throw new NullPointerException("coordinate");
        }
        boolean negative = false;
        if (coordinate.charAt(0) == '-') {
            coordinate = coordinate.substring(1);
            negative = true;
        }
        StringTokenizer st = new StringTokenizer(coordinate, ":");
        int tokens = st.countTokens();
        if (tokens < 1) {
            throw new IllegalArgumentException("coordinate=" + coordinate);
        }
        try {
            String degrees = st.nextToken();
            if (tokens == 1) {
                double val = Double.parseDouble(degrees);
                return negative ? -val : val;
            }
            String minutes = st.nextToken();
            int deg = Integer.parseInt(degrees);
            double sec = 0.0d;
            boolean secPresent = false;
            if (st.hasMoreTokens()) {
                min = Integer.parseInt(minutes);
                String seconds = st.nextToken();
                sec = Double.parseDouble(seconds);
                secPresent = true;
            } else {
                min = Double.parseDouble(minutes);
            }
            boolean isNegative180 = negative && deg == 180 && min == 0.0d && sec == 0.0d;
            if (deg < 0.0d || (deg > 179 && !isNegative180)) {
                throw new IllegalArgumentException("coordinate=" + coordinate);
            }
            if (min < 0.0d || min >= 60.0d || (secPresent && min > 59.0d)) {
                throw new IllegalArgumentException("coordinate=" + coordinate);
            }
            if (sec < 0.0d || sec >= 60.0d) {
                throw new IllegalArgumentException("coordinate=" + coordinate);
            }
            double val2 = (((((double) deg) * 3600.0d) + (60.0d * min)) + sec) / 3600.0d;
            return negative ? -val2 : val2;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("coordinate=" + coordinate);
        }
    }

    private static void computeDistanceAndBearing(double lat1, double lon1, double lat2, double lon2, BearingDistanceCache results) {
        double lat12 = lat1 * 0.017453292519943295d;
        double lat22 = lat2 * 0.017453292519943295d;
        double lon12 = lon1 * 0.017453292519943295d;
        double lon22 = lon2 * 0.017453292519943295d;
        double f = 21384.685800000094d / 6378137.0d;
        double aSqMinusBSqOverBSq = (4.0680631590769E13d - 4.0408299984087055E13d) / 4.0408299984087055E13d;
        double L = lon22 - lon12;
        double A = 0.0d;
        double U1 = Math.atan((1.0d - f) * Math.tan(lat12));
        double U2 = Math.atan((1.0d - f) * Math.tan(lat22));
        double cosU1 = Math.cos(U1);
        double cosU2 = Math.cos(U2);
        double sinU1 = Math.sin(U1);
        double sinU2 = Math.sin(U2);
        double cosU1cosU2 = cosU1 * cosU2;
        double sinU1sinU2 = sinU1 * sinU2;
        double sigma = 0.0d;
        double deltaSigma = 0.0d;
        double cosLambda = 0.0d;
        double sinLambda = 0.0d;
        double lambda = L;
        for (int iter = 0; iter < 20; iter++) {
            double lambdaOrig = lambda;
            cosLambda = Math.cos(lambda);
            sinLambda = Math.sin(lambda);
            double t1 = cosU2 * sinLambda;
            double t2 = (cosU1 * sinU2) - ((sinU1 * cosU2) * cosLambda);
            double sinSqSigma = (t1 * t1) + (t2 * t2);
            double sinSigma = Math.sqrt(sinSqSigma);
            double cosSigma = sinU1sinU2 + (cosU1cosU2 * cosLambda);
            sigma = Math.atan2(sinSigma, cosSigma);
            double sinAlpha = sinSigma == 0.0d ? 0.0d : (cosU1cosU2 * sinLambda) / sinSigma;
            double cosSqAlpha = 1.0d - (sinAlpha * sinAlpha);
            double cos2SM = cosSqAlpha == 0.0d ? 0.0d : cosSigma - ((2.0d * sinU1sinU2) / cosSqAlpha);
            double uSquared = cosSqAlpha * aSqMinusBSqOverBSq;
            A = 1.0d + ((uSquared / 16384.0d) * (((((320.0d - (175.0d * uSquared)) * uSquared) - 768.0d) * uSquared) + 4096.0d));
            double B = (uSquared / 1024.0d) * (((((74.0d - (47.0d * uSquared)) * uSquared) - 128.0d) * uSquared) + 256.0d);
            double C = (f / 16.0d) * cosSqAlpha * (((4.0d - (3.0d * cosSqAlpha)) * f) + 4.0d);
            double cos2SMSq = cos2SM * cos2SM;
            deltaSigma = B * sinSigma * (((B / 4.0d) * ((((2.0d * cos2SMSq) - 1.0d) * cosSigma) - ((((B / 6.0d) * cos2SM) * (((4.0d * sinSigma) * sinSigma) - 3.0d)) * ((4.0d * cos2SMSq) - 3.0d)))) + cos2SM);
            lambda = L + ((1.0d - C) * f * sinAlpha * ((C * sinSigma * ((C * cosSigma * (((2.0d * cos2SM) * cos2SM) - 1.0d)) + cos2SM)) + sigma));
            double delta = (lambda - lambdaOrig) / lambda;
            if (Math.abs(delta) < 1.0E-12d) {
                break;
            }
        }
        float distance = (float) (6356752.3142d * A * (sigma - deltaSigma));
        results.mDistance = distance;
        float initialBearing = (float) Math.atan2(cosU2 * sinLambda, (cosU1 * sinU2) - ((sinU1 * cosU2) * cosLambda));
        results.mInitialBearing = (float) (((double) initialBearing) * 57.29577951308232d);
        float finalBearing = (float) Math.atan2(cosU1 * sinLambda, ((-sinU1) * cosU2) + (cosU1 * sinU2 * cosLambda));
        results.mFinalBearing = (float) (((double) finalBearing) * 57.29577951308232d);
        results.mLat1 = lat12;
        results.mLat2 = lat22;
        results.mLon1 = lon12;
        results.mLon2 = lon22;
    }

    public static void distanceBetween(double startLatitude, double startLongitude, double endLatitude, double endLongitude, float[] results) {
        if (results == null || results.length < 1) {
            throw new IllegalArgumentException("results is null or has length < 1");
        }
        BearingDistanceCache cache = sBearingDistanceCache.get();
        computeDistanceAndBearing(startLatitude, startLongitude, endLatitude, endLongitude, cache);
        results[0] = cache.mDistance;
        if (results.length <= 1) {
            return;
        }
        results[1] = cache.mInitialBearing;
        if (results.length <= 2) {
            return;
        }
        results[2] = cache.mFinalBearing;
    }

    public float distanceTo(Location dest) {
        BearingDistanceCache cache = sBearingDistanceCache.get();
        if (this.mLatitude != cache.mLat1 || this.mLongitude != cache.mLon1 || dest.mLatitude != cache.mLat2 || dest.mLongitude != cache.mLon2) {
            computeDistanceAndBearing(this.mLatitude, this.mLongitude, dest.mLatitude, dest.mLongitude, cache);
        }
        return cache.mDistance;
    }

    public float bearingTo(Location dest) {
        BearingDistanceCache cache = sBearingDistanceCache.get();
        if (this.mLatitude != cache.mLat1 || this.mLongitude != cache.mLon1 || dest.mLatitude != cache.mLat2 || dest.mLongitude != cache.mLon2) {
            computeDistanceAndBearing(this.mLatitude, this.mLongitude, dest.mLatitude, dest.mLongitude, cache);
        }
        return cache.mInitialBearing;
    }

    public String getProvider() {
        return this.mProvider;
    }

    public void setProvider(String provider) {
        this.mProvider = provider;
    }

    public long getTime() {
        return this.mTime;
    }

    public void setTime(long time) {
        this.mTime = time;
    }

    public long getElapsedRealtimeNanos() {
        return this.mElapsedRealtimeNanos;
    }

    public void setElapsedRealtimeNanos(long time) {
        this.mElapsedRealtimeNanos = time;
    }

    public double getLatitude() {
        return this.mLatitude;
    }

    public void setLatitude(double latitude) {
        this.mLatitude = latitude;
    }

    public double getLongitude() {
        return this.mLongitude;
    }

    public void setLongitude(double longitude) {
        this.mLongitude = longitude;
    }

    public boolean hasAltitude() {
        return (this.mFieldsMask & 1) != 0;
    }

    public double getAltitude() {
        return this.mAltitude;
    }

    public void setAltitude(double altitude) {
        this.mAltitude = altitude;
        this.mFieldsMask = (byte) (this.mFieldsMask | 1);
    }

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

    public void setSpeed(float speed) {
        this.mSpeed = speed;
        this.mFieldsMask = (byte) (this.mFieldsMask | 2);
    }

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

    public void setBearing(float bearing) {
        while (bearing < 0.0f) {
            bearing += 360.0f;
        }
        while (bearing >= 360.0f) {
            bearing -= 360.0f;
        }
        this.mBearing = bearing;
        this.mFieldsMask = (byte) (this.mFieldsMask | 4);
    }

    public void removeBearing() {
        this.mBearing = 0.0f;
        this.mFieldsMask = (byte) (this.mFieldsMask & (-5));
    }

    public boolean hasAccuracy() {
        return (this.mFieldsMask & 8) != 0;
    }

    public float getAccuracy() {
        return this.mAccuracy;
    }

    public void setAccuracy(float accuracy) {
        this.mAccuracy = accuracy;
        this.mFieldsMask = (byte) (this.mFieldsMask | 8);
    }

    public void removeAccuracy() {
        this.mAccuracy = 0.0f;
        this.mFieldsMask = (byte) (this.mFieldsMask & (-9));
    }

    public boolean isComplete() {
        return (this.mProvider == null || !hasAccuracy() || this.mTime == 0 || this.mElapsedRealtimeNanos == 0) ? false : true;
    }

    public void makeComplete() {
        if (this.mProvider == null) {
            this.mProvider = "?";
        }
        if (!hasAccuracy()) {
            this.mFieldsMask = (byte) (this.mFieldsMask | 8);
            this.mAccuracy = 100.0f;
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

    public void setExtras(Bundle extras) {
        this.mExtras = extras != null ? new Bundle(extras) : null;
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("Location[");
        s.append(this.mProvider);
        s.append(String.format(" %.6f,%.6f", Double.valueOf(this.mLatitude), Double.valueOf(this.mLongitude)));
        if (hasAccuracy()) {
            s.append(String.format(" acc=%.0f", Float.valueOf(this.mAccuracy)));
        } else {
            s.append(" acc=???");
        }
        if (this.mTime == 0) {
            s.append(" t=?!?");
        }
        if (this.mElapsedRealtimeNanos == 0) {
            s.append(" et=?!?");
        } else {
            s.append(" et=");
            TimeUtils.formatDuration(this.mElapsedRealtimeNanos / 1000000, s);
        }
        if (hasAltitude()) {
            s.append(" alt=").append(this.mAltitude);
        }
        if (hasSpeed()) {
            s.append(" vel=").append(this.mSpeed);
        }
        if (hasBearing()) {
            s.append(" bear=").append(this.mBearing);
        }
        if (isFromMockProvider()) {
            s.append(" mock");
        }
        if (this.mExtras != null) {
            s.append(" {").append(this.mExtras).append('}');
        }
        s.append(']');
        return s.toString();
    }

    public void dump(Printer pw, String prefix) {
        pw.println(prefix + toString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(this.mProvider);
        parcel.writeLong(this.mTime);
        parcel.writeLong(this.mElapsedRealtimeNanos);
        parcel.writeByte(this.mFieldsMask);
        parcel.writeDouble(this.mLatitude);
        parcel.writeDouble(this.mLongitude);
        parcel.writeDouble(this.mAltitude);
        parcel.writeFloat(this.mSpeed);
        parcel.writeFloat(this.mBearing);
        parcel.writeFloat(this.mAccuracy);
        parcel.writeBundle(this.mExtras);
    }

    public Location getExtraLocation(String key) {
        if (this.mExtras != null) {
            Parcelable value = this.mExtras.getParcelable(key);
            if (value instanceof Location) {
                return (Location) value;
            }
        }
        return null;
    }

    public void setExtraLocation(String key, Location value) {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putParcelable(key, value);
    }

    public boolean isFromMockProvider() {
        return (this.mFieldsMask & 16) != 0;
    }

    public void setIsFromMockProvider(boolean isFromMockProvider) {
        if (isFromMockProvider) {
            this.mFieldsMask = (byte) (this.mFieldsMask | 16);
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

        BearingDistanceCache(BearingDistanceCache bearingDistanceCache) {
            this();
        }

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
