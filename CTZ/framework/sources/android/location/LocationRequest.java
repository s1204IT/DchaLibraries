package android.location;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.os.WorkSource;
import android.util.TimeUtils;

@SystemApi
public final class LocationRequest implements Parcelable {
    public static final int ACCURACY_BLOCK = 102;
    public static final int ACCURACY_CITY = 104;
    public static final int ACCURACY_FINE = 100;
    public static final Parcelable.Creator<LocationRequest> CREATOR = new Parcelable.Creator<LocationRequest>() {
        @Override
        public LocationRequest createFromParcel(Parcel parcel) {
            LocationRequest locationRequest = new LocationRequest();
            locationRequest.setQuality(parcel.readInt());
            locationRequest.setFastestInterval(parcel.readLong());
            locationRequest.setInterval(parcel.readLong());
            locationRequest.setExpireAt(parcel.readLong());
            locationRequest.setNumUpdates(parcel.readInt());
            locationRequest.setSmallestDisplacement(parcel.readFloat());
            locationRequest.setHideFromAppOps(parcel.readInt() != 0);
            locationRequest.setLowPowerMode(parcel.readInt() != 0);
            String string = parcel.readString();
            if (string != null) {
                locationRequest.setProvider(string);
            }
            WorkSource workSource = (WorkSource) parcel.readParcelable(null);
            if (workSource != null) {
                locationRequest.setWorkSource(workSource);
            }
            return locationRequest;
        }

        @Override
        public LocationRequest[] newArray(int i) {
            return new LocationRequest[i];
        }
    };
    private static final double FASTEST_INTERVAL_FACTOR = 6.0d;
    public static final int POWER_HIGH = 203;
    public static final int POWER_LOW = 201;
    public static final int POWER_NONE = 200;
    private long mExpireAt;
    private boolean mExplicitFastestInterval;
    private long mFastestInterval;
    private boolean mHideFromAppOps;
    private long mInterval;
    private boolean mLowPowerMode;
    private int mNumUpdates;
    private String mProvider;
    private int mQuality;
    private float mSmallestDisplacement;
    private WorkSource mWorkSource;

    public static LocationRequest create() {
        return new LocationRequest();
    }

    @SystemApi
    public static LocationRequest createFromDeprecatedProvider(String str, long j, float f, boolean z) {
        int i;
        if (j < 0) {
            j = 0;
        }
        if (f < 0.0f) {
            f = 0.0f;
        }
        if (LocationManager.PASSIVE_PROVIDER.equals(str)) {
            i = 200;
        } else if (LocationManager.GPS_PROVIDER.equals(str)) {
            i = 100;
        } else {
            i = 201;
        }
        LocationRequest smallestDisplacement = new LocationRequest().setProvider(str).setQuality(i).setInterval(j).setFastestInterval(j).setSmallestDisplacement(f);
        if (z) {
            smallestDisplacement.setNumUpdates(1);
        }
        return smallestDisplacement;
    }

    @SystemApi
    public static LocationRequest createFromDeprecatedCriteria(Criteria criteria, long j, float f, boolean z) {
        int i;
        if (j < 0) {
            j = 0;
        }
        if (f < 0.0f) {
            f = 0.0f;
        }
        switch (criteria.getAccuracy()) {
            case 1:
                i = 100;
                break;
            case 2:
                i = 102;
                break;
            default:
                if (criteria.getPowerRequirement() == 3) {
                    i = 203;
                } else {
                    i = 201;
                }
                break;
        }
        LocationRequest smallestDisplacement = new LocationRequest().setQuality(i).setInterval(j).setFastestInterval(j).setSmallestDisplacement(f);
        if (z) {
            smallestDisplacement.setNumUpdates(1);
        }
        return smallestDisplacement;
    }

    public LocationRequest() {
        this.mQuality = 201;
        this.mInterval = 3600000L;
        this.mFastestInterval = (long) (this.mInterval / FASTEST_INTERVAL_FACTOR);
        this.mExplicitFastestInterval = false;
        this.mExpireAt = Long.MAX_VALUE;
        this.mNumUpdates = Integer.MAX_VALUE;
        this.mSmallestDisplacement = 0.0f;
        this.mWorkSource = null;
        this.mHideFromAppOps = false;
        this.mProvider = LocationManager.FUSED_PROVIDER;
        this.mLowPowerMode = false;
    }

    public LocationRequest(LocationRequest locationRequest) {
        this.mQuality = 201;
        this.mInterval = 3600000L;
        this.mFastestInterval = (long) (this.mInterval / FASTEST_INTERVAL_FACTOR);
        this.mExplicitFastestInterval = false;
        this.mExpireAt = Long.MAX_VALUE;
        this.mNumUpdates = Integer.MAX_VALUE;
        this.mSmallestDisplacement = 0.0f;
        this.mWorkSource = null;
        this.mHideFromAppOps = false;
        this.mProvider = LocationManager.FUSED_PROVIDER;
        this.mLowPowerMode = false;
        this.mQuality = locationRequest.mQuality;
        this.mInterval = locationRequest.mInterval;
        this.mFastestInterval = locationRequest.mFastestInterval;
        this.mExplicitFastestInterval = locationRequest.mExplicitFastestInterval;
        this.mExpireAt = locationRequest.mExpireAt;
        this.mNumUpdates = locationRequest.mNumUpdates;
        this.mSmallestDisplacement = locationRequest.mSmallestDisplacement;
        this.mProvider = locationRequest.mProvider;
        this.mWorkSource = locationRequest.mWorkSource;
        this.mHideFromAppOps = locationRequest.mHideFromAppOps;
        this.mLowPowerMode = locationRequest.mLowPowerMode;
    }

    public LocationRequest setQuality(int i) {
        checkQuality(i);
        this.mQuality = i;
        return this;
    }

    public int getQuality() {
        return this.mQuality;
    }

    public LocationRequest setInterval(long j) {
        checkInterval(j);
        this.mInterval = j;
        if (!this.mExplicitFastestInterval) {
            this.mFastestInterval = (long) (this.mInterval / FASTEST_INTERVAL_FACTOR);
        }
        return this;
    }

    public long getInterval() {
        return this.mInterval;
    }

    @SystemApi
    public LocationRequest setLowPowerMode(boolean z) {
        this.mLowPowerMode = z;
        return this;
    }

    @SystemApi
    public boolean isLowPowerMode() {
        return this.mLowPowerMode;
    }

    public LocationRequest setFastestInterval(long j) {
        checkInterval(j);
        this.mExplicitFastestInterval = true;
        this.mFastestInterval = j;
        return this;
    }

    public long getFastestInterval() {
        return this.mFastestInterval;
    }

    public LocationRequest setExpireIn(long j) {
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        if (j > Long.MAX_VALUE - jElapsedRealtime) {
            this.mExpireAt = Long.MAX_VALUE;
        } else {
            this.mExpireAt = j + jElapsedRealtime;
        }
        if (this.mExpireAt < 0) {
            this.mExpireAt = 0L;
        }
        return this;
    }

    public LocationRequest setExpireAt(long j) {
        this.mExpireAt = j;
        if (this.mExpireAt < 0) {
            this.mExpireAt = 0L;
        }
        return this;
    }

    public long getExpireAt() {
        return this.mExpireAt;
    }

    public LocationRequest setNumUpdates(int i) {
        if (i <= 0) {
            throw new IllegalArgumentException("invalid numUpdates: " + i);
        }
        this.mNumUpdates = i;
        return this;
    }

    public int getNumUpdates() {
        return this.mNumUpdates;
    }

    public void decrementNumUpdates() {
        if (this.mNumUpdates != Integer.MAX_VALUE) {
            this.mNumUpdates--;
        }
        if (this.mNumUpdates < 0) {
            this.mNumUpdates = 0;
        }
    }

    @SystemApi
    public LocationRequest setProvider(String str) {
        checkProvider(str);
        this.mProvider = str;
        return this;
    }

    @SystemApi
    public String getProvider() {
        return this.mProvider;
    }

    @SystemApi
    public LocationRequest setSmallestDisplacement(float f) {
        checkDisplacement(f);
        this.mSmallestDisplacement = f;
        return this;
    }

    @SystemApi
    public float getSmallestDisplacement() {
        return this.mSmallestDisplacement;
    }

    @SystemApi
    public void setWorkSource(WorkSource workSource) {
        this.mWorkSource = workSource;
    }

    @SystemApi
    public WorkSource getWorkSource() {
        return this.mWorkSource;
    }

    @SystemApi
    public void setHideFromAppOps(boolean z) {
        this.mHideFromAppOps = z;
    }

    @SystemApi
    public boolean getHideFromAppOps() {
        return this.mHideFromAppOps;
    }

    private static void checkInterval(long j) {
        if (j < 0) {
            throw new IllegalArgumentException("invalid interval: " + j);
        }
    }

    private static void checkQuality(int i) {
        if (i == 100 || i == 102 || i == 104 || i == 203) {
            return;
        }
        switch (i) {
            case 200:
            case 201:
                return;
            default:
                throw new IllegalArgumentException("invalid quality: " + i);
        }
    }

    private static void checkDisplacement(float f) {
        if (f < 0.0f) {
            throw new IllegalArgumentException("invalid displacement: " + f);
        }
    }

    private static void checkProvider(String str) {
        if (str == null) {
            throw new IllegalArgumentException("invalid provider: " + str);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mQuality);
        parcel.writeLong(this.mFastestInterval);
        parcel.writeLong(this.mInterval);
        parcel.writeLong(this.mExpireAt);
        parcel.writeInt(this.mNumUpdates);
        parcel.writeFloat(this.mSmallestDisplacement);
        parcel.writeInt(this.mHideFromAppOps ? 1 : 0);
        parcel.writeInt(this.mLowPowerMode ? 1 : 0);
        parcel.writeString(this.mProvider);
        parcel.writeParcelable(this.mWorkSource, 0);
    }

    public static String qualityToString(int i) {
        if (i == 100) {
            return "ACCURACY_FINE";
        }
        if (i == 102) {
            return "ACCURACY_BLOCK";
        }
        if (i == 104) {
            return "ACCURACY_CITY";
        }
        if (i != 203) {
            switch (i) {
                case 200:
                    return "POWER_NONE";
                case 201:
                    return "POWER_LOW";
                default:
                    return "???";
            }
        }
        return "POWER_HIGH";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Request[");
        sb.append(qualityToString(this.mQuality));
        if (this.mProvider != null) {
            sb.append(' ');
            sb.append(this.mProvider);
        }
        if (this.mQuality != 200) {
            sb.append(" requested=");
            TimeUtils.formatDuration(this.mInterval, sb);
        }
        sb.append(" fastest=");
        TimeUtils.formatDuration(this.mFastestInterval, sb);
        if (this.mExpireAt != Long.MAX_VALUE) {
            long jElapsedRealtime = this.mExpireAt - SystemClock.elapsedRealtime();
            sb.append(" expireIn=");
            TimeUtils.formatDuration(jElapsedRealtime, sb);
        }
        if (this.mNumUpdates != Integer.MAX_VALUE) {
            sb.append(" num=");
            sb.append(this.mNumUpdates);
        }
        sb.append(" lowPowerMode=");
        sb.append(this.mLowPowerMode);
        sb.append(']');
        return sb.toString();
    }
}
