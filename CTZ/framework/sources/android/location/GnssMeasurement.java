package android.location;

import android.os.Parcel;
import android.os.Parcelable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class GnssMeasurement implements Parcelable {
    public static final int ADR_STATE_ALL = 31;
    public static final int ADR_STATE_CYCLE_SLIP = 4;
    public static final int ADR_STATE_HALF_CYCLE_REPORTED = 16;
    public static final int ADR_STATE_HALF_CYCLE_RESOLVED = 8;
    public static final int ADR_STATE_RESET = 2;
    public static final int ADR_STATE_UNKNOWN = 0;
    public static final int ADR_STATE_VALID = 1;
    public static final Parcelable.Creator<GnssMeasurement> CREATOR = new Parcelable.Creator<GnssMeasurement>() {
        @Override
        public GnssMeasurement createFromParcel(Parcel parcel) {
            GnssMeasurement gnssMeasurement = new GnssMeasurement();
            gnssMeasurement.mFlags = parcel.readInt();
            gnssMeasurement.mSvid = parcel.readInt();
            gnssMeasurement.mConstellationType = parcel.readInt();
            gnssMeasurement.mTimeOffsetNanos = parcel.readDouble();
            gnssMeasurement.mState = parcel.readInt();
            gnssMeasurement.mReceivedSvTimeNanos = parcel.readLong();
            gnssMeasurement.mReceivedSvTimeUncertaintyNanos = parcel.readLong();
            gnssMeasurement.mCn0DbHz = parcel.readDouble();
            gnssMeasurement.mPseudorangeRateMetersPerSecond = parcel.readDouble();
            gnssMeasurement.mPseudorangeRateUncertaintyMetersPerSecond = parcel.readDouble();
            gnssMeasurement.mAccumulatedDeltaRangeState = parcel.readInt();
            gnssMeasurement.mAccumulatedDeltaRangeMeters = parcel.readDouble();
            gnssMeasurement.mAccumulatedDeltaRangeUncertaintyMeters = parcel.readDouble();
            gnssMeasurement.mCarrierFrequencyHz = parcel.readFloat();
            gnssMeasurement.mCarrierCycles = parcel.readLong();
            gnssMeasurement.mCarrierPhase = parcel.readDouble();
            gnssMeasurement.mCarrierPhaseUncertainty = parcel.readDouble();
            gnssMeasurement.mMultipathIndicator = parcel.readInt();
            gnssMeasurement.mSnrInDb = parcel.readDouble();
            gnssMeasurement.mAutomaticGainControlLevelInDb = parcel.readDouble();
            return gnssMeasurement;
        }

        @Override
        public GnssMeasurement[] newArray(int i) {
            return new GnssMeasurement[i];
        }
    };
    private static final int HAS_AUTOMATIC_GAIN_CONTROL = 8192;
    private static final int HAS_CARRIER_CYCLES = 1024;
    private static final int HAS_CARRIER_FREQUENCY = 512;
    private static final int HAS_CARRIER_PHASE = 2048;
    private static final int HAS_CARRIER_PHASE_UNCERTAINTY = 4096;
    private static final int HAS_NO_FLAGS = 0;
    private static final int HAS_SNR = 1;
    public static final int MULTIPATH_INDICATOR_DETECTED = 1;
    public static final int MULTIPATH_INDICATOR_NOT_DETECTED = 2;
    public static final int MULTIPATH_INDICATOR_UNKNOWN = 0;
    private static final int STATE_ALL = 16383;
    public static final int STATE_BDS_D2_BIT_SYNC = 256;
    public static final int STATE_BDS_D2_SUBFRAME_SYNC = 512;
    public static final int STATE_BIT_SYNC = 2;
    public static final int STATE_CODE_LOCK = 1;
    public static final int STATE_GAL_E1BC_CODE_LOCK = 1024;
    public static final int STATE_GAL_E1B_PAGE_SYNC = 4096;
    public static final int STATE_GAL_E1C_2ND_CODE_LOCK = 2048;
    public static final int STATE_GLO_STRING_SYNC = 64;
    public static final int STATE_GLO_TOD_DECODED = 128;
    public static final int STATE_GLO_TOD_KNOWN = 32768;
    public static final int STATE_MSEC_AMBIGUOUS = 16;
    public static final int STATE_SBAS_SYNC = 8192;
    public static final int STATE_SUBFRAME_SYNC = 4;
    public static final int STATE_SYMBOL_SYNC = 32;
    public static final int STATE_TOW_DECODED = 8;
    public static final int STATE_TOW_KNOWN = 16384;
    public static final int STATE_UNKNOWN = 0;
    private double mAccumulatedDeltaRangeMeters;
    private int mAccumulatedDeltaRangeState;
    private double mAccumulatedDeltaRangeUncertaintyMeters;
    private double mAutomaticGainControlLevelInDb;
    private long mCarrierCycles;
    private float mCarrierFrequencyHz;
    private double mCarrierPhase;
    private double mCarrierPhaseUncertainty;
    private double mCn0DbHz;
    private int mConstellationType;
    private int mFlags;
    private int mMultipathIndicator;
    private double mPseudorangeRateMetersPerSecond;
    private double mPseudorangeRateUncertaintyMetersPerSecond;
    private long mReceivedSvTimeNanos;
    private long mReceivedSvTimeUncertaintyNanos;
    private double mSnrInDb;
    private int mState;
    private int mSvid;
    private double mTimeOffsetNanos;

    @Retention(RetentionPolicy.SOURCE)
    public @interface AdrState {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface MultipathIndicator {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface State {
    }

    public GnssMeasurement() {
        initialize();
    }

    public void set(GnssMeasurement gnssMeasurement) {
        this.mFlags = gnssMeasurement.mFlags;
        this.mSvid = gnssMeasurement.mSvid;
        this.mConstellationType = gnssMeasurement.mConstellationType;
        this.mTimeOffsetNanos = gnssMeasurement.mTimeOffsetNanos;
        this.mState = gnssMeasurement.mState;
        this.mReceivedSvTimeNanos = gnssMeasurement.mReceivedSvTimeNanos;
        this.mReceivedSvTimeUncertaintyNanos = gnssMeasurement.mReceivedSvTimeUncertaintyNanos;
        this.mCn0DbHz = gnssMeasurement.mCn0DbHz;
        this.mPseudorangeRateMetersPerSecond = gnssMeasurement.mPseudorangeRateMetersPerSecond;
        this.mPseudorangeRateUncertaintyMetersPerSecond = gnssMeasurement.mPseudorangeRateUncertaintyMetersPerSecond;
        this.mAccumulatedDeltaRangeState = gnssMeasurement.mAccumulatedDeltaRangeState;
        this.mAccumulatedDeltaRangeMeters = gnssMeasurement.mAccumulatedDeltaRangeMeters;
        this.mAccumulatedDeltaRangeUncertaintyMeters = gnssMeasurement.mAccumulatedDeltaRangeUncertaintyMeters;
        this.mCarrierFrequencyHz = gnssMeasurement.mCarrierFrequencyHz;
        this.mCarrierCycles = gnssMeasurement.mCarrierCycles;
        this.mCarrierPhase = gnssMeasurement.mCarrierPhase;
        this.mCarrierPhaseUncertainty = gnssMeasurement.mCarrierPhaseUncertainty;
        this.mMultipathIndicator = gnssMeasurement.mMultipathIndicator;
        this.mSnrInDb = gnssMeasurement.mSnrInDb;
        this.mAutomaticGainControlLevelInDb = gnssMeasurement.mAutomaticGainControlLevelInDb;
    }

    public void reset() {
        initialize();
    }

    public int getSvid() {
        return this.mSvid;
    }

    public void setSvid(int i) {
        this.mSvid = i;
    }

    public int getConstellationType() {
        return this.mConstellationType;
    }

    public void setConstellationType(int i) {
        this.mConstellationType = i;
    }

    public double getTimeOffsetNanos() {
        return this.mTimeOffsetNanos;
    }

    public void setTimeOffsetNanos(double d) {
        this.mTimeOffsetNanos = d;
    }

    public int getState() {
        return this.mState;
    }

    public void setState(int i) {
        this.mState = i;
    }

    private String getStateString() {
        if (this.mState == 0) {
            return "Unknown";
        }
        StringBuilder sb = new StringBuilder();
        if ((this.mState & 1) != 0) {
            sb.append("CodeLock|");
        }
        if ((this.mState & 2) != 0) {
            sb.append("BitSync|");
        }
        if ((this.mState & 4) != 0) {
            sb.append("SubframeSync|");
        }
        if ((this.mState & 8) != 0) {
            sb.append("TowDecoded|");
        }
        if ((this.mState & 16384) != 0) {
            sb.append("TowKnown|");
        }
        if ((this.mState & 16) != 0) {
            sb.append("MsecAmbiguous|");
        }
        if ((this.mState & 32) != 0) {
            sb.append("SymbolSync|");
        }
        if ((this.mState & 64) != 0) {
            sb.append("GloStringSync|");
        }
        if ((this.mState & 128) != 0) {
            sb.append("GloTodDecoded|");
        }
        if ((this.mState & 32768) != 0) {
            sb.append("GloTodKnown|");
        }
        if ((this.mState & 256) != 0) {
            sb.append("BdsD2BitSync|");
        }
        if ((this.mState & 512) != 0) {
            sb.append("BdsD2SubframeSync|");
        }
        if ((this.mState & 1024) != 0) {
            sb.append("GalE1bcCodeLock|");
        }
        if ((this.mState & 2048) != 0) {
            sb.append("E1c2ndCodeLock|");
        }
        if ((this.mState & 4096) != 0) {
            sb.append("GalE1bPageSync|");
        }
        if ((this.mState & 8192) != 0) {
            sb.append("SbasSync|");
        }
        int i = this.mState & (-16384);
        if (i > 0) {
            sb.append("Other(");
            sb.append(Integer.toBinaryString(i));
            sb.append(")|");
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    public long getReceivedSvTimeNanos() {
        return this.mReceivedSvTimeNanos;
    }

    public void setReceivedSvTimeNanos(long j) {
        this.mReceivedSvTimeNanos = j;
    }

    public long getReceivedSvTimeUncertaintyNanos() {
        return this.mReceivedSvTimeUncertaintyNanos;
    }

    public void setReceivedSvTimeUncertaintyNanos(long j) {
        this.mReceivedSvTimeUncertaintyNanos = j;
    }

    public double getCn0DbHz() {
        return this.mCn0DbHz;
    }

    public void setCn0DbHz(double d) {
        this.mCn0DbHz = d;
    }

    public double getPseudorangeRateMetersPerSecond() {
        return this.mPseudorangeRateMetersPerSecond;
    }

    public void setPseudorangeRateMetersPerSecond(double d) {
        this.mPseudorangeRateMetersPerSecond = d;
    }

    public double getPseudorangeRateUncertaintyMetersPerSecond() {
        return this.mPseudorangeRateUncertaintyMetersPerSecond;
    }

    public void setPseudorangeRateUncertaintyMetersPerSecond(double d) {
        this.mPseudorangeRateUncertaintyMetersPerSecond = d;
    }

    public int getAccumulatedDeltaRangeState() {
        return this.mAccumulatedDeltaRangeState;
    }

    public void setAccumulatedDeltaRangeState(int i) {
        this.mAccumulatedDeltaRangeState = i;
    }

    private String getAccumulatedDeltaRangeStateString() {
        if (this.mAccumulatedDeltaRangeState == 0) {
            return "Unknown";
        }
        StringBuilder sb = new StringBuilder();
        if ((this.mAccumulatedDeltaRangeState & 1) == 1) {
            sb.append("Valid|");
        }
        if ((this.mAccumulatedDeltaRangeState & 2) == 2) {
            sb.append("Reset|");
        }
        if ((this.mAccumulatedDeltaRangeState & 4) == 4) {
            sb.append("CycleSlip|");
        }
        if ((this.mAccumulatedDeltaRangeState & 8) == 8) {
            sb.append("HalfCycleResolved|");
        }
        if ((this.mAccumulatedDeltaRangeState & 16) == 16) {
            sb.append("HalfCycleReported|");
        }
        int i = this.mAccumulatedDeltaRangeState & (-32);
        if (i > 0) {
            sb.append("Other(");
            sb.append(Integer.toBinaryString(i));
            sb.append(")|");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public double getAccumulatedDeltaRangeMeters() {
        return this.mAccumulatedDeltaRangeMeters;
    }

    public void setAccumulatedDeltaRangeMeters(double d) {
        this.mAccumulatedDeltaRangeMeters = d;
    }

    public double getAccumulatedDeltaRangeUncertaintyMeters() {
        return this.mAccumulatedDeltaRangeUncertaintyMeters;
    }

    public void setAccumulatedDeltaRangeUncertaintyMeters(double d) {
        this.mAccumulatedDeltaRangeUncertaintyMeters = d;
    }

    public boolean hasCarrierFrequencyHz() {
        return isFlagSet(512);
    }

    public float getCarrierFrequencyHz() {
        return this.mCarrierFrequencyHz;
    }

    public void setCarrierFrequencyHz(float f) {
        setFlag(512);
        this.mCarrierFrequencyHz = f;
    }

    public void resetCarrierFrequencyHz() {
        resetFlag(512);
        this.mCarrierFrequencyHz = Float.NaN;
    }

    @Deprecated
    public boolean hasCarrierCycles() {
        return isFlagSet(1024);
    }

    @Deprecated
    public long getCarrierCycles() {
        return this.mCarrierCycles;
    }

    @Deprecated
    public void setCarrierCycles(long j) {
        setFlag(1024);
        this.mCarrierCycles = j;
    }

    @Deprecated
    public void resetCarrierCycles() {
        resetFlag(1024);
        this.mCarrierCycles = Long.MIN_VALUE;
    }

    @Deprecated
    public boolean hasCarrierPhase() {
        return isFlagSet(2048);
    }

    @Deprecated
    public double getCarrierPhase() {
        return this.mCarrierPhase;
    }

    @Deprecated
    public void setCarrierPhase(double d) {
        setFlag(2048);
        this.mCarrierPhase = d;
    }

    @Deprecated
    public void resetCarrierPhase() {
        resetFlag(2048);
        this.mCarrierPhase = Double.NaN;
    }

    @Deprecated
    public boolean hasCarrierPhaseUncertainty() {
        return isFlagSet(4096);
    }

    @Deprecated
    public double getCarrierPhaseUncertainty() {
        return this.mCarrierPhaseUncertainty;
    }

    @Deprecated
    public void setCarrierPhaseUncertainty(double d) {
        setFlag(4096);
        this.mCarrierPhaseUncertainty = d;
    }

    @Deprecated
    public void resetCarrierPhaseUncertainty() {
        resetFlag(4096);
        this.mCarrierPhaseUncertainty = Double.NaN;
    }

    public int getMultipathIndicator() {
        return this.mMultipathIndicator;
    }

    public void setMultipathIndicator(int i) {
        this.mMultipathIndicator = i;
    }

    private String getMultipathIndicatorString() {
        switch (this.mMultipathIndicator) {
            case 0:
                return "Unknown";
            case 1:
                return "Detected";
            case 2:
                return "NotDetected";
            default:
                return "<Invalid: " + this.mMultipathIndicator + ">";
        }
    }

    public boolean hasSnrInDb() {
        return isFlagSet(1);
    }

    public double getSnrInDb() {
        return this.mSnrInDb;
    }

    public void setSnrInDb(double d) {
        setFlag(1);
        this.mSnrInDb = d;
    }

    public void resetSnrInDb() {
        resetFlag(1);
        this.mSnrInDb = Double.NaN;
    }

    public boolean hasAutomaticGainControlLevelDb() {
        return isFlagSet(8192);
    }

    public double getAutomaticGainControlLevelDb() {
        return this.mAutomaticGainControlLevelInDb;
    }

    public void setAutomaticGainControlLevelInDb(double d) {
        setFlag(8192);
        this.mAutomaticGainControlLevelInDb = d;
    }

    public void resetAutomaticGainControlLevel() {
        resetFlag(8192);
        this.mAutomaticGainControlLevelInDb = Double.NaN;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mFlags);
        parcel.writeInt(this.mSvid);
        parcel.writeInt(this.mConstellationType);
        parcel.writeDouble(this.mTimeOffsetNanos);
        parcel.writeInt(this.mState);
        parcel.writeLong(this.mReceivedSvTimeNanos);
        parcel.writeLong(this.mReceivedSvTimeUncertaintyNanos);
        parcel.writeDouble(this.mCn0DbHz);
        parcel.writeDouble(this.mPseudorangeRateMetersPerSecond);
        parcel.writeDouble(this.mPseudorangeRateUncertaintyMetersPerSecond);
        parcel.writeInt(this.mAccumulatedDeltaRangeState);
        parcel.writeDouble(this.mAccumulatedDeltaRangeMeters);
        parcel.writeDouble(this.mAccumulatedDeltaRangeUncertaintyMeters);
        parcel.writeFloat(this.mCarrierFrequencyHz);
        parcel.writeLong(this.mCarrierCycles);
        parcel.writeDouble(this.mCarrierPhase);
        parcel.writeDouble(this.mCarrierPhaseUncertainty);
        parcel.writeInt(this.mMultipathIndicator);
        parcel.writeDouble(this.mSnrInDb);
        parcel.writeDouble(this.mAutomaticGainControlLevelInDb);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("GnssMeasurement:\n");
        sb.append(String.format("   %-29s = %s\n", "Svid", Integer.valueOf(this.mSvid)));
        sb.append(String.format("   %-29s = %s\n", "ConstellationType", Integer.valueOf(this.mConstellationType)));
        sb.append(String.format("   %-29s = %s\n", "TimeOffsetNanos", Double.valueOf(this.mTimeOffsetNanos)));
        sb.append(String.format("   %-29s = %s\n", "State", getStateString()));
        sb.append(String.format("   %-29s = %-25s   %-40s = %s\n", "ReceivedSvTimeNanos", Long.valueOf(this.mReceivedSvTimeNanos), "ReceivedSvTimeUncertaintyNanos", Long.valueOf(this.mReceivedSvTimeUncertaintyNanos)));
        sb.append(String.format("   %-29s = %s\n", "Cn0DbHz", Double.valueOf(this.mCn0DbHz)));
        sb.append(String.format("   %-29s = %-25s   %-40s = %s\n", "PseudorangeRateMetersPerSecond", Double.valueOf(this.mPseudorangeRateMetersPerSecond), "PseudorangeRateUncertaintyMetersPerSecond", Double.valueOf(this.mPseudorangeRateUncertaintyMetersPerSecond)));
        sb.append(String.format("   %-29s = %s\n", "AccumulatedDeltaRangeState", getAccumulatedDeltaRangeStateString()));
        sb.append(String.format("   %-29s = %-25s   %-40s = %s\n", "AccumulatedDeltaRangeMeters", Double.valueOf(this.mAccumulatedDeltaRangeMeters), "AccumulatedDeltaRangeUncertaintyMeters", Double.valueOf(this.mAccumulatedDeltaRangeUncertaintyMeters)));
        Object[] objArr = new Object[2];
        objArr[0] = "CarrierFrequencyHz";
        objArr[1] = hasCarrierFrequencyHz() ? Float.valueOf(this.mCarrierFrequencyHz) : null;
        sb.append(String.format("   %-29s = %s\n", objArr));
        Object[] objArr2 = new Object[2];
        objArr2[0] = "CarrierCycles";
        objArr2[1] = hasCarrierCycles() ? Long.valueOf(this.mCarrierCycles) : null;
        sb.append(String.format("   %-29s = %s\n", objArr2));
        Object[] objArr3 = new Object[4];
        objArr3[0] = "CarrierPhase";
        objArr3[1] = hasCarrierPhase() ? Double.valueOf(this.mCarrierPhase) : null;
        objArr3[2] = "CarrierPhaseUncertainty";
        objArr3[3] = hasCarrierPhaseUncertainty() ? Double.valueOf(this.mCarrierPhaseUncertainty) : null;
        sb.append(String.format("   %-29s = %-25s   %-40s = %s\n", objArr3));
        sb.append(String.format("   %-29s = %s\n", "MultipathIndicator", getMultipathIndicatorString()));
        Object[] objArr4 = new Object[2];
        objArr4[0] = "SnrInDb";
        objArr4[1] = hasSnrInDb() ? Double.valueOf(this.mSnrInDb) : null;
        sb.append(String.format("   %-29s = %s\n", objArr4));
        Object[] objArr5 = new Object[2];
        objArr5[0] = "AgcLevelDb";
        objArr5[1] = hasAutomaticGainControlLevelDb() ? Double.valueOf(this.mAutomaticGainControlLevelInDb) : null;
        sb.append(String.format("   %-29s = %s\n", objArr5));
        return sb.toString();
    }

    private void initialize() {
        this.mFlags = 0;
        setSvid(0);
        setTimeOffsetNanos(-9.223372036854776E18d);
        setState(0);
        setReceivedSvTimeNanos(Long.MIN_VALUE);
        setReceivedSvTimeUncertaintyNanos(Long.MAX_VALUE);
        setCn0DbHz(Double.MIN_VALUE);
        setPseudorangeRateMetersPerSecond(Double.MIN_VALUE);
        setPseudorangeRateUncertaintyMetersPerSecond(Double.MIN_VALUE);
        setAccumulatedDeltaRangeState(0);
        setAccumulatedDeltaRangeMeters(Double.MIN_VALUE);
        setAccumulatedDeltaRangeUncertaintyMeters(Double.MIN_VALUE);
        resetCarrierFrequencyHz();
        resetCarrierCycles();
        resetCarrierPhase();
        resetCarrierPhaseUncertainty();
        setMultipathIndicator(0);
        resetSnrInDb();
        resetAutomaticGainControlLevel();
    }

    private void setFlag(int i) {
        this.mFlags = i | this.mFlags;
    }

    private void resetFlag(int i) {
        this.mFlags = (~i) & this.mFlags;
    }

    private boolean isFlagSet(int i) {
        return (this.mFlags & i) == i;
    }
}
