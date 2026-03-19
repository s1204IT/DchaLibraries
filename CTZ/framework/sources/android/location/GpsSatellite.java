package android.location;

@Deprecated
public final class GpsSatellite {
    float mAzimuth;
    float mElevation;
    boolean mHasAlmanac;
    boolean mHasEphemeris;
    int mPrn;
    float mSnr;
    boolean mUsedInFix;
    boolean mValid;

    GpsSatellite(int i) {
        this.mPrn = i;
    }

    void setStatus(GpsSatellite gpsSatellite) {
        if (gpsSatellite == null) {
            this.mValid = false;
            return;
        }
        this.mValid = gpsSatellite.mValid;
        this.mHasEphemeris = gpsSatellite.mHasEphemeris;
        this.mHasAlmanac = gpsSatellite.mHasAlmanac;
        this.mUsedInFix = gpsSatellite.mUsedInFix;
        this.mSnr = gpsSatellite.mSnr;
        this.mElevation = gpsSatellite.mElevation;
        this.mAzimuth = gpsSatellite.mAzimuth;
    }

    public int getPrn() {
        return this.mPrn;
    }

    public float getSnr() {
        return this.mSnr;
    }

    public float getElevation() {
        return this.mElevation;
    }

    public float getAzimuth() {
        return this.mAzimuth;
    }

    public boolean hasEphemeris() {
        return this.mHasEphemeris;
    }

    public boolean hasAlmanac() {
        return this.mHasAlmanac;
    }

    public boolean usedInFix() {
        return this.mUsedInFix;
    }
}
