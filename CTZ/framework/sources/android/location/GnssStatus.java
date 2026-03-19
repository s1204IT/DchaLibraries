package android.location;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class GnssStatus {
    public static final int CONSTELLATION_BEIDOU = 5;
    public static final int CONSTELLATION_GALILEO = 6;
    public static final int CONSTELLATION_GLONASS = 3;
    public static final int CONSTELLATION_GPS = 1;
    public static final int CONSTELLATION_QZSS = 4;
    public static final int CONSTELLATION_SBAS = 2;
    public static final int CONSTELLATION_TYPE_MASK = 15;
    public static final int CONSTELLATION_TYPE_SHIFT_WIDTH = 4;
    public static final int CONSTELLATION_UNKNOWN = 0;
    public static final int GNSS_SV_FLAGS_HAS_ALMANAC_DATA = 2;
    public static final int GNSS_SV_FLAGS_HAS_CARRIER_FREQUENCY = 8;
    public static final int GNSS_SV_FLAGS_HAS_EPHEMERIS_DATA = 1;
    public static final int GNSS_SV_FLAGS_NONE = 0;
    public static final int GNSS_SV_FLAGS_USED_IN_FIX = 4;
    public static final int SVID_SHIFT_WIDTH = 8;
    final float[] mAzimuths;
    final float[] mCarrierFrequencies;
    final float[] mCn0DbHz;
    final float[] mElevations;
    final int mSvCount;
    final int[] mSvidWithFlags;

    @Retention(RetentionPolicy.SOURCE)
    public @interface ConstellationType {
    }

    public static abstract class Callback {
        public void onStarted() {
        }

        public void onStopped() {
        }

        public void onFirstFix(int i) {
        }

        public void onSatelliteStatusChanged(GnssStatus gnssStatus) {
        }
    }

    GnssStatus(int i, int[] iArr, float[] fArr, float[] fArr2, float[] fArr3, float[] fArr4) {
        this.mSvCount = i;
        this.mSvidWithFlags = iArr;
        this.mCn0DbHz = fArr;
        this.mElevations = fArr2;
        this.mAzimuths = fArr3;
        this.mCarrierFrequencies = fArr4;
    }

    public int getSatelliteCount() {
        return this.mSvCount;
    }

    public int getConstellationType(int i) {
        return (this.mSvidWithFlags[i] >> 4) & 15;
    }

    public int getSvid(int i) {
        return this.mSvidWithFlags[i] >> 8;
    }

    public float getCn0DbHz(int i) {
        return this.mCn0DbHz[i];
    }

    public float getElevationDegrees(int i) {
        return this.mElevations[i];
    }

    public float getAzimuthDegrees(int i) {
        return this.mAzimuths[i];
    }

    public boolean hasEphemerisData(int i) {
        return (this.mSvidWithFlags[i] & 1) != 0;
    }

    public boolean hasAlmanacData(int i) {
        return (this.mSvidWithFlags[i] & 2) != 0;
    }

    public boolean usedInFix(int i) {
        return (this.mSvidWithFlags[i] & 4) != 0;
    }

    public boolean hasCarrierFrequencyHz(int i) {
        return (this.mSvidWithFlags[i] & 8) != 0;
    }

    public float getCarrierFrequencyHz(int i) {
        return this.mCarrierFrequencies[i];
    }
}
