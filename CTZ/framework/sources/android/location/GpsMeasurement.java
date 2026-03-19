package android.location;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Half;

@SystemApi
public class GpsMeasurement implements Parcelable {
    private static final short ADR_ALL = 7;
    public static final short ADR_STATE_CYCLE_SLIP = 4;
    public static final short ADR_STATE_RESET = 2;
    public static final short ADR_STATE_UNKNOWN = 0;
    public static final short ADR_STATE_VALID = 1;
    public static final Parcelable.Creator<GpsMeasurement> CREATOR = new Parcelable.Creator<GpsMeasurement>() {
        @Override
        public GpsMeasurement createFromParcel(Parcel parcel) {
            GpsMeasurement gpsMeasurement = new GpsMeasurement();
            gpsMeasurement.mFlags = parcel.readInt();
            gpsMeasurement.mPrn = parcel.readByte();
            gpsMeasurement.mTimeOffsetInNs = parcel.readDouble();
            gpsMeasurement.mState = (short) parcel.readInt();
            gpsMeasurement.mReceivedGpsTowInNs = parcel.readLong();
            gpsMeasurement.mReceivedGpsTowUncertaintyInNs = parcel.readLong();
            gpsMeasurement.mCn0InDbHz = parcel.readDouble();
            gpsMeasurement.mPseudorangeRateInMetersPerSec = parcel.readDouble();
            gpsMeasurement.mPseudorangeRateUncertaintyInMetersPerSec = parcel.readDouble();
            gpsMeasurement.mAccumulatedDeltaRangeState = (short) parcel.readInt();
            gpsMeasurement.mAccumulatedDeltaRangeInMeters = parcel.readDouble();
            gpsMeasurement.mAccumulatedDeltaRangeUncertaintyInMeters = parcel.readDouble();
            gpsMeasurement.mPseudorangeInMeters = parcel.readDouble();
            gpsMeasurement.mPseudorangeUncertaintyInMeters = parcel.readDouble();
            gpsMeasurement.mCodePhaseInChips = parcel.readDouble();
            gpsMeasurement.mCodePhaseUncertaintyInChips = parcel.readDouble();
            gpsMeasurement.mCarrierFrequencyInHz = parcel.readFloat();
            gpsMeasurement.mCarrierCycles = parcel.readLong();
            gpsMeasurement.mCarrierPhase = parcel.readDouble();
            gpsMeasurement.mCarrierPhaseUncertainty = parcel.readDouble();
            gpsMeasurement.mLossOfLock = parcel.readByte();
            gpsMeasurement.mBitNumber = parcel.readInt();
            gpsMeasurement.mTimeFromLastBitInMs = (short) parcel.readInt();
            gpsMeasurement.mDopplerShiftInHz = parcel.readDouble();
            gpsMeasurement.mDopplerShiftUncertaintyInHz = parcel.readDouble();
            gpsMeasurement.mMultipathIndicator = parcel.readByte();
            gpsMeasurement.mSnrInDb = parcel.readDouble();
            gpsMeasurement.mElevationInDeg = parcel.readDouble();
            gpsMeasurement.mElevationUncertaintyInDeg = parcel.readDouble();
            gpsMeasurement.mAzimuthInDeg = parcel.readDouble();
            gpsMeasurement.mAzimuthUncertaintyInDeg = parcel.readDouble();
            gpsMeasurement.mUsedInFix = parcel.readInt() != 0;
            return gpsMeasurement;
        }

        @Override
        public GpsMeasurement[] newArray(int i) {
            return new GpsMeasurement[i];
        }
    };
    private static final int GPS_MEASUREMENT_HAS_UNCORRECTED_PSEUDORANGE_RATE = 262144;
    private static final int HAS_AZIMUTH = 8;
    private static final int HAS_AZIMUTH_UNCERTAINTY = 16;
    private static final int HAS_BIT_NUMBER = 8192;
    private static final int HAS_CARRIER_CYCLES = 1024;
    private static final int HAS_CARRIER_FREQUENCY = 512;
    private static final int HAS_CARRIER_PHASE = 2048;
    private static final int HAS_CARRIER_PHASE_UNCERTAINTY = 4096;
    private static final int HAS_CODE_PHASE = 128;
    private static final int HAS_CODE_PHASE_UNCERTAINTY = 256;
    private static final int HAS_DOPPLER_SHIFT = 32768;
    private static final int HAS_DOPPLER_SHIFT_UNCERTAINTY = 65536;
    private static final int HAS_ELEVATION = 2;
    private static final int HAS_ELEVATION_UNCERTAINTY = 4;
    private static final int HAS_NO_FLAGS = 0;
    private static final int HAS_PSEUDORANGE = 32;
    private static final int HAS_PSEUDORANGE_UNCERTAINTY = 64;
    private static final int HAS_SNR = 1;
    private static final int HAS_TIME_FROM_LAST_BIT = 16384;
    private static final int HAS_USED_IN_FIX = 131072;
    public static final byte LOSS_OF_LOCK_CYCLE_SLIP = 2;
    public static final byte LOSS_OF_LOCK_OK = 1;
    public static final byte LOSS_OF_LOCK_UNKNOWN = 0;
    public static final byte MULTIPATH_INDICATOR_DETECTED = 1;
    public static final byte MULTIPATH_INDICATOR_NOT_USED = 2;
    public static final byte MULTIPATH_INDICATOR_UNKNOWN = 0;
    private static final short STATE_ALL = 31;
    public static final short STATE_BIT_SYNC = 2;
    public static final short STATE_CODE_LOCK = 1;
    public static final short STATE_MSEC_AMBIGUOUS = 16;
    public static final short STATE_SUBFRAME_SYNC = 4;
    public static final short STATE_TOW_DECODED = 8;
    public static final short STATE_UNKNOWN = 0;
    private double mAccumulatedDeltaRangeInMeters;
    private short mAccumulatedDeltaRangeState;
    private double mAccumulatedDeltaRangeUncertaintyInMeters;
    private double mAzimuthInDeg;
    private double mAzimuthUncertaintyInDeg;
    private int mBitNumber;
    private long mCarrierCycles;
    private float mCarrierFrequencyInHz;
    private double mCarrierPhase;
    private double mCarrierPhaseUncertainty;
    private double mCn0InDbHz;
    private double mCodePhaseInChips;
    private double mCodePhaseUncertaintyInChips;
    private double mDopplerShiftInHz;
    private double mDopplerShiftUncertaintyInHz;
    private double mElevationInDeg;
    private double mElevationUncertaintyInDeg;
    private int mFlags;
    private byte mLossOfLock;
    private byte mMultipathIndicator;
    private byte mPrn;
    private double mPseudorangeInMeters;
    private double mPseudorangeRateInMetersPerSec;
    private double mPseudorangeRateUncertaintyInMetersPerSec;
    private double mPseudorangeUncertaintyInMeters;
    private long mReceivedGpsTowInNs;
    private long mReceivedGpsTowUncertaintyInNs;
    private double mSnrInDb;
    private short mState;
    private short mTimeFromLastBitInMs;
    private double mTimeOffsetInNs;
    private boolean mUsedInFix;

    GpsMeasurement() {
        initialize();
    }

    public void set(GpsMeasurement gpsMeasurement) {
        this.mFlags = gpsMeasurement.mFlags;
        this.mPrn = gpsMeasurement.mPrn;
        this.mTimeOffsetInNs = gpsMeasurement.mTimeOffsetInNs;
        this.mState = gpsMeasurement.mState;
        this.mReceivedGpsTowInNs = gpsMeasurement.mReceivedGpsTowInNs;
        this.mReceivedGpsTowUncertaintyInNs = gpsMeasurement.mReceivedGpsTowUncertaintyInNs;
        this.mCn0InDbHz = gpsMeasurement.mCn0InDbHz;
        this.mPseudorangeRateInMetersPerSec = gpsMeasurement.mPseudorangeRateInMetersPerSec;
        this.mPseudorangeRateUncertaintyInMetersPerSec = gpsMeasurement.mPseudorangeRateUncertaintyInMetersPerSec;
        this.mAccumulatedDeltaRangeState = gpsMeasurement.mAccumulatedDeltaRangeState;
        this.mAccumulatedDeltaRangeInMeters = gpsMeasurement.mAccumulatedDeltaRangeInMeters;
        this.mAccumulatedDeltaRangeUncertaintyInMeters = gpsMeasurement.mAccumulatedDeltaRangeUncertaintyInMeters;
        this.mPseudorangeInMeters = gpsMeasurement.mPseudorangeInMeters;
        this.mPseudorangeUncertaintyInMeters = gpsMeasurement.mPseudorangeUncertaintyInMeters;
        this.mCodePhaseInChips = gpsMeasurement.mCodePhaseInChips;
        this.mCodePhaseUncertaintyInChips = gpsMeasurement.mCodePhaseUncertaintyInChips;
        this.mCarrierFrequencyInHz = gpsMeasurement.mCarrierFrequencyInHz;
        this.mCarrierCycles = gpsMeasurement.mCarrierCycles;
        this.mCarrierPhase = gpsMeasurement.mCarrierPhase;
        this.mCarrierPhaseUncertainty = gpsMeasurement.mCarrierPhaseUncertainty;
        this.mLossOfLock = gpsMeasurement.mLossOfLock;
        this.mBitNumber = gpsMeasurement.mBitNumber;
        this.mTimeFromLastBitInMs = gpsMeasurement.mTimeFromLastBitInMs;
        this.mDopplerShiftInHz = gpsMeasurement.mDopplerShiftInHz;
        this.mDopplerShiftUncertaintyInHz = gpsMeasurement.mDopplerShiftUncertaintyInHz;
        this.mMultipathIndicator = gpsMeasurement.mMultipathIndicator;
        this.mSnrInDb = gpsMeasurement.mSnrInDb;
        this.mElevationInDeg = gpsMeasurement.mElevationInDeg;
        this.mElevationUncertaintyInDeg = gpsMeasurement.mElevationUncertaintyInDeg;
        this.mAzimuthInDeg = gpsMeasurement.mAzimuthInDeg;
        this.mAzimuthUncertaintyInDeg = gpsMeasurement.mAzimuthUncertaintyInDeg;
        this.mUsedInFix = gpsMeasurement.mUsedInFix;
    }

    public void reset() {
        initialize();
    }

    public byte getPrn() {
        return this.mPrn;
    }

    public void setPrn(byte b) {
        this.mPrn = b;
    }

    public double getTimeOffsetInNs() {
        return this.mTimeOffsetInNs;
    }

    public void setTimeOffsetInNs(double d) {
        this.mTimeOffsetInNs = d;
    }

    public short getState() {
        return this.mState;
    }

    public void setState(short s) {
        this.mState = s;
    }

    private String getStateString() {
        if (this.mState == 0) {
            return "Unknown";
        }
        StringBuilder sb = new StringBuilder();
        if ((this.mState & 1) == 1) {
            sb.append("CodeLock|");
        }
        if ((this.mState & 2) == 2) {
            sb.append("BitSync|");
        }
        if ((this.mState & 4) == 4) {
            sb.append("SubframeSync|");
        }
        if ((this.mState & 8) == 8) {
            sb.append("TowDecoded|");
        }
        if ((this.mState & 16) == 16) {
            sb.append("MsecAmbiguous");
        }
        int i = this.mState & (-32);
        if (i > 0) {
            sb.append("Other(");
            sb.append(Integer.toBinaryString(i));
            sb.append(")|");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public long getReceivedGpsTowInNs() {
        return this.mReceivedGpsTowInNs;
    }

    public void setReceivedGpsTowInNs(long j) {
        this.mReceivedGpsTowInNs = j;
    }

    public long getReceivedGpsTowUncertaintyInNs() {
        return this.mReceivedGpsTowUncertaintyInNs;
    }

    public void setReceivedGpsTowUncertaintyInNs(long j) {
        this.mReceivedGpsTowUncertaintyInNs = j;
    }

    public double getCn0InDbHz() {
        return this.mCn0InDbHz;
    }

    public void setCn0InDbHz(double d) {
        this.mCn0InDbHz = d;
    }

    public double getPseudorangeRateInMetersPerSec() {
        return this.mPseudorangeRateInMetersPerSec;
    }

    public void setPseudorangeRateInMetersPerSec(double d) {
        this.mPseudorangeRateInMetersPerSec = d;
    }

    public boolean isPseudorangeRateCorrected() {
        return !isFlagSet(262144);
    }

    public double getPseudorangeRateUncertaintyInMetersPerSec() {
        return this.mPseudorangeRateUncertaintyInMetersPerSec;
    }

    public void setPseudorangeRateUncertaintyInMetersPerSec(double d) {
        this.mPseudorangeRateUncertaintyInMetersPerSec = d;
    }

    public short getAccumulatedDeltaRangeState() {
        return this.mAccumulatedDeltaRangeState;
    }

    public void setAccumulatedDeltaRangeState(short s) {
        this.mAccumulatedDeltaRangeState = s;
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
        int i = this.mAccumulatedDeltaRangeState & (-8);
        if (i > 0) {
            sb.append("Other(");
            sb.append(Integer.toBinaryString(i));
            sb.append(")|");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public double getAccumulatedDeltaRangeInMeters() {
        return this.mAccumulatedDeltaRangeInMeters;
    }

    public void setAccumulatedDeltaRangeInMeters(double d) {
        this.mAccumulatedDeltaRangeInMeters = d;
    }

    public double getAccumulatedDeltaRangeUncertaintyInMeters() {
        return this.mAccumulatedDeltaRangeUncertaintyInMeters;
    }

    public void setAccumulatedDeltaRangeUncertaintyInMeters(double d) {
        this.mAccumulatedDeltaRangeUncertaintyInMeters = d;
    }

    public boolean hasPseudorangeInMeters() {
        return isFlagSet(32);
    }

    public double getPseudorangeInMeters() {
        return this.mPseudorangeInMeters;
    }

    public void setPseudorangeInMeters(double d) {
        setFlag(32);
        this.mPseudorangeInMeters = d;
    }

    public void resetPseudorangeInMeters() {
        resetFlag(32);
        this.mPseudorangeInMeters = Double.NaN;
    }

    public boolean hasPseudorangeUncertaintyInMeters() {
        return isFlagSet(64);
    }

    public double getPseudorangeUncertaintyInMeters() {
        return this.mPseudorangeUncertaintyInMeters;
    }

    public void setPseudorangeUncertaintyInMeters(double d) {
        setFlag(64);
        this.mPseudorangeUncertaintyInMeters = d;
    }

    public void resetPseudorangeUncertaintyInMeters() {
        resetFlag(64);
        this.mPseudorangeUncertaintyInMeters = Double.NaN;
    }

    public boolean hasCodePhaseInChips() {
        return isFlagSet(128);
    }

    public double getCodePhaseInChips() {
        return this.mCodePhaseInChips;
    }

    public void setCodePhaseInChips(double d) {
        setFlag(128);
        this.mCodePhaseInChips = d;
    }

    public void resetCodePhaseInChips() {
        resetFlag(128);
        this.mCodePhaseInChips = Double.NaN;
    }

    public boolean hasCodePhaseUncertaintyInChips() {
        return isFlagSet(256);
    }

    public double getCodePhaseUncertaintyInChips() {
        return this.mCodePhaseUncertaintyInChips;
    }

    public void setCodePhaseUncertaintyInChips(double d) {
        setFlag(256);
        this.mCodePhaseUncertaintyInChips = d;
    }

    public void resetCodePhaseUncertaintyInChips() {
        resetFlag(256);
        this.mCodePhaseUncertaintyInChips = Double.NaN;
    }

    public boolean hasCarrierFrequencyInHz() {
        return isFlagSet(512);
    }

    public float getCarrierFrequencyInHz() {
        return this.mCarrierFrequencyInHz;
    }

    public void setCarrierFrequencyInHz(float f) {
        setFlag(512);
        this.mCarrierFrequencyInHz = f;
    }

    public void resetCarrierFrequencyInHz() {
        resetFlag(512);
        this.mCarrierFrequencyInHz = Float.NaN;
    }

    public boolean hasCarrierCycles() {
        return isFlagSet(1024);
    }

    public long getCarrierCycles() {
        return this.mCarrierCycles;
    }

    public void setCarrierCycles(long j) {
        setFlag(1024);
        this.mCarrierCycles = j;
    }

    public void resetCarrierCycles() {
        resetFlag(1024);
        this.mCarrierCycles = Long.MIN_VALUE;
    }

    public boolean hasCarrierPhase() {
        return isFlagSet(2048);
    }

    public double getCarrierPhase() {
        return this.mCarrierPhase;
    }

    public void setCarrierPhase(double d) {
        setFlag(2048);
        this.mCarrierPhase = d;
    }

    public void resetCarrierPhase() {
        resetFlag(2048);
        this.mCarrierPhase = Double.NaN;
    }

    public boolean hasCarrierPhaseUncertainty() {
        return isFlagSet(4096);
    }

    public double getCarrierPhaseUncertainty() {
        return this.mCarrierPhaseUncertainty;
    }

    public void setCarrierPhaseUncertainty(double d) {
        setFlag(4096);
        this.mCarrierPhaseUncertainty = d;
    }

    public void resetCarrierPhaseUncertainty() {
        resetFlag(4096);
        this.mCarrierPhaseUncertainty = Double.NaN;
    }

    public byte getLossOfLock() {
        return this.mLossOfLock;
    }

    public void setLossOfLock(byte b) {
        this.mLossOfLock = b;
    }

    private String getLossOfLockString() {
        switch (this.mLossOfLock) {
            case 0:
                return "Unknown";
            case 1:
                return "Ok";
            case 2:
                return "CycleSlip";
            default:
                return "<Invalid:" + ((int) this.mLossOfLock) + ">";
        }
    }

    public boolean hasBitNumber() {
        return isFlagSet(8192);
    }

    public int getBitNumber() {
        return this.mBitNumber;
    }

    public void setBitNumber(int i) {
        setFlag(8192);
        this.mBitNumber = i;
    }

    public void resetBitNumber() {
        resetFlag(8192);
        this.mBitNumber = Integer.MIN_VALUE;
    }

    public boolean hasTimeFromLastBitInMs() {
        return isFlagSet(16384);
    }

    public short getTimeFromLastBitInMs() {
        return this.mTimeFromLastBitInMs;
    }

    public void setTimeFromLastBitInMs(short s) {
        setFlag(16384);
        this.mTimeFromLastBitInMs = s;
    }

    public void resetTimeFromLastBitInMs() {
        resetFlag(16384);
        this.mTimeFromLastBitInMs = Half.NEGATIVE_ZERO;
    }

    public boolean hasDopplerShiftInHz() {
        return isFlagSet(32768);
    }

    public double getDopplerShiftInHz() {
        return this.mDopplerShiftInHz;
    }

    public void setDopplerShiftInHz(double d) {
        setFlag(32768);
        this.mDopplerShiftInHz = d;
    }

    public void resetDopplerShiftInHz() {
        resetFlag(32768);
        this.mDopplerShiftInHz = Double.NaN;
    }

    public boolean hasDopplerShiftUncertaintyInHz() {
        return isFlagSet(65536);
    }

    public double getDopplerShiftUncertaintyInHz() {
        return this.mDopplerShiftUncertaintyInHz;
    }

    public void setDopplerShiftUncertaintyInHz(double d) {
        setFlag(65536);
        this.mDopplerShiftUncertaintyInHz = d;
    }

    public void resetDopplerShiftUncertaintyInHz() {
        resetFlag(65536);
        this.mDopplerShiftUncertaintyInHz = Double.NaN;
    }

    public byte getMultipathIndicator() {
        return this.mMultipathIndicator;
    }

    public void setMultipathIndicator(byte b) {
        this.mMultipathIndicator = b;
    }

    private String getMultipathIndicatorString() {
        switch (this.mMultipathIndicator) {
            case 0:
                return "Unknown";
            case 1:
                return "Detected";
            case 2:
                return "NotUsed";
            default:
                return "<Invalid:" + ((int) this.mMultipathIndicator) + ">";
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

    public boolean hasElevationInDeg() {
        return isFlagSet(2);
    }

    public double getElevationInDeg() {
        return this.mElevationInDeg;
    }

    public void setElevationInDeg(double d) {
        setFlag(2);
        this.mElevationInDeg = d;
    }

    public void resetElevationInDeg() {
        resetFlag(2);
        this.mElevationInDeg = Double.NaN;
    }

    public boolean hasElevationUncertaintyInDeg() {
        return isFlagSet(4);
    }

    public double getElevationUncertaintyInDeg() {
        return this.mElevationUncertaintyInDeg;
    }

    public void setElevationUncertaintyInDeg(double d) {
        setFlag(4);
        this.mElevationUncertaintyInDeg = d;
    }

    public void resetElevationUncertaintyInDeg() {
        resetFlag(4);
        this.mElevationUncertaintyInDeg = Double.NaN;
    }

    public boolean hasAzimuthInDeg() {
        return isFlagSet(8);
    }

    public double getAzimuthInDeg() {
        return this.mAzimuthInDeg;
    }

    public void setAzimuthInDeg(double d) {
        setFlag(8);
        this.mAzimuthInDeg = d;
    }

    public void resetAzimuthInDeg() {
        resetFlag(8);
        this.mAzimuthInDeg = Double.NaN;
    }

    public boolean hasAzimuthUncertaintyInDeg() {
        return isFlagSet(16);
    }

    public double getAzimuthUncertaintyInDeg() {
        return this.mAzimuthUncertaintyInDeg;
    }

    public void setAzimuthUncertaintyInDeg(double d) {
        setFlag(16);
        this.mAzimuthUncertaintyInDeg = d;
    }

    public void resetAzimuthUncertaintyInDeg() {
        resetFlag(16);
        this.mAzimuthUncertaintyInDeg = Double.NaN;
    }

    public boolean isUsedInFix() {
        return this.mUsedInFix;
    }

    public void setUsedInFix(boolean z) {
        this.mUsedInFix = z;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mFlags);
        parcel.writeByte(this.mPrn);
        parcel.writeDouble(this.mTimeOffsetInNs);
        parcel.writeInt(this.mState);
        parcel.writeLong(this.mReceivedGpsTowInNs);
        parcel.writeLong(this.mReceivedGpsTowUncertaintyInNs);
        parcel.writeDouble(this.mCn0InDbHz);
        parcel.writeDouble(this.mPseudorangeRateInMetersPerSec);
        parcel.writeDouble(this.mPseudorangeRateUncertaintyInMetersPerSec);
        parcel.writeInt(this.mAccumulatedDeltaRangeState);
        parcel.writeDouble(this.mAccumulatedDeltaRangeInMeters);
        parcel.writeDouble(this.mAccumulatedDeltaRangeUncertaintyInMeters);
        parcel.writeDouble(this.mPseudorangeInMeters);
        parcel.writeDouble(this.mPseudorangeUncertaintyInMeters);
        parcel.writeDouble(this.mCodePhaseInChips);
        parcel.writeDouble(this.mCodePhaseUncertaintyInChips);
        parcel.writeFloat(this.mCarrierFrequencyInHz);
        parcel.writeLong(this.mCarrierCycles);
        parcel.writeDouble(this.mCarrierPhase);
        parcel.writeDouble(this.mCarrierPhaseUncertainty);
        parcel.writeByte(this.mLossOfLock);
        parcel.writeInt(this.mBitNumber);
        parcel.writeInt(this.mTimeFromLastBitInMs);
        parcel.writeDouble(this.mDopplerShiftInHz);
        parcel.writeDouble(this.mDopplerShiftUncertaintyInHz);
        parcel.writeByte(this.mMultipathIndicator);
        parcel.writeDouble(this.mSnrInDb);
        parcel.writeDouble(this.mElevationInDeg);
        parcel.writeDouble(this.mElevationUncertaintyInDeg);
        parcel.writeDouble(this.mAzimuthInDeg);
        parcel.writeDouble(this.mAzimuthUncertaintyInDeg);
        parcel.writeInt(this.mUsedInFix ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("GpsMeasurement:\n");
        sb.append(String.format("   %-29s = %s\n", "Prn", Byte.valueOf(this.mPrn)));
        sb.append(String.format("   %-29s = %s\n", "TimeOffsetInNs", Double.valueOf(this.mTimeOffsetInNs)));
        sb.append(String.format("   %-29s = %s\n", "State", getStateString()));
        sb.append(String.format("   %-29s = %-25s   %-40s = %s\n", "ReceivedGpsTowInNs", Long.valueOf(this.mReceivedGpsTowInNs), "ReceivedGpsTowUncertaintyInNs", Long.valueOf(this.mReceivedGpsTowUncertaintyInNs)));
        sb.append(String.format("   %-29s = %s\n", "Cn0InDbHz", Double.valueOf(this.mCn0InDbHz)));
        sb.append(String.format("   %-29s = %-25s   %-40s = %s\n", "PseudorangeRateInMetersPerSec", Double.valueOf(this.mPseudorangeRateInMetersPerSec), "PseudorangeRateUncertaintyInMetersPerSec", Double.valueOf(this.mPseudorangeRateUncertaintyInMetersPerSec)));
        sb.append(String.format("   %-29s = %s\n", "PseudorangeRateIsCorrected", Boolean.valueOf(isPseudorangeRateCorrected())));
        sb.append(String.format("   %-29s = %s\n", "AccumulatedDeltaRangeState", getAccumulatedDeltaRangeStateString()));
        sb.append(String.format("   %-29s = %-25s   %-40s = %s\n", "AccumulatedDeltaRangeInMeters", Double.valueOf(this.mAccumulatedDeltaRangeInMeters), "AccumulatedDeltaRangeUncertaintyInMeters", Double.valueOf(this.mAccumulatedDeltaRangeUncertaintyInMeters)));
        Object[] objArr = new Object[4];
        objArr[0] = "PseudorangeInMeters";
        objArr[1] = hasPseudorangeInMeters() ? Double.valueOf(this.mPseudorangeInMeters) : null;
        objArr[2] = "PseudorangeUncertaintyInMeters";
        objArr[3] = hasPseudorangeUncertaintyInMeters() ? Double.valueOf(this.mPseudorangeUncertaintyInMeters) : null;
        sb.append(String.format("   %-29s = %-25s   %-40s = %s\n", objArr));
        Object[] objArr2 = new Object[4];
        objArr2[0] = "CodePhaseInChips";
        objArr2[1] = hasCodePhaseInChips() ? Double.valueOf(this.mCodePhaseInChips) : null;
        objArr2[2] = "CodePhaseUncertaintyInChips";
        objArr2[3] = hasCodePhaseUncertaintyInChips() ? Double.valueOf(this.mCodePhaseUncertaintyInChips) : null;
        sb.append(String.format("   %-29s = %-25s   %-40s = %s\n", objArr2));
        Object[] objArr3 = new Object[2];
        objArr3[0] = "CarrierFrequencyInHz";
        objArr3[1] = hasCarrierFrequencyInHz() ? Float.valueOf(this.mCarrierFrequencyInHz) : null;
        sb.append(String.format("   %-29s = %s\n", objArr3));
        Object[] objArr4 = new Object[2];
        objArr4[0] = "CarrierCycles";
        objArr4[1] = hasCarrierCycles() ? Long.valueOf(this.mCarrierCycles) : null;
        sb.append(String.format("   %-29s = %s\n", objArr4));
        Object[] objArr5 = new Object[4];
        objArr5[0] = "CarrierPhase";
        objArr5[1] = hasCarrierPhase() ? Double.valueOf(this.mCarrierPhase) : null;
        objArr5[2] = "CarrierPhaseUncertainty";
        objArr5[3] = hasCarrierPhaseUncertainty() ? Double.valueOf(this.mCarrierPhaseUncertainty) : null;
        sb.append(String.format("   %-29s = %-25s   %-40s = %s\n", objArr5));
        sb.append(String.format("   %-29s = %s\n", "LossOfLock", getLossOfLockString()));
        Object[] objArr6 = new Object[2];
        objArr6[0] = "BitNumber";
        objArr6[1] = hasBitNumber() ? Integer.valueOf(this.mBitNumber) : null;
        sb.append(String.format("   %-29s = %s\n", objArr6));
        Object[] objArr7 = new Object[2];
        objArr7[0] = "TimeFromLastBitInMs";
        objArr7[1] = hasTimeFromLastBitInMs() ? Short.valueOf(this.mTimeFromLastBitInMs) : null;
        sb.append(String.format("   %-29s = %s\n", objArr7));
        Object[] objArr8 = new Object[4];
        objArr8[0] = "DopplerShiftInHz";
        objArr8[1] = hasDopplerShiftInHz() ? Double.valueOf(this.mDopplerShiftInHz) : null;
        objArr8[2] = "DopplerShiftUncertaintyInHz";
        objArr8[3] = hasDopplerShiftUncertaintyInHz() ? Double.valueOf(this.mDopplerShiftUncertaintyInHz) : null;
        sb.append(String.format("   %-29s = %-25s   %-40s = %s\n", objArr8));
        sb.append(String.format("   %-29s = %s\n", "MultipathIndicator", getMultipathIndicatorString()));
        Object[] objArr9 = new Object[2];
        objArr9[0] = "SnrInDb";
        objArr9[1] = hasSnrInDb() ? Double.valueOf(this.mSnrInDb) : null;
        sb.append(String.format("   %-29s = %s\n", objArr9));
        Object[] objArr10 = new Object[4];
        objArr10[0] = "ElevationInDeg";
        objArr10[1] = hasElevationInDeg() ? Double.valueOf(this.mElevationInDeg) : null;
        objArr10[2] = "ElevationUncertaintyInDeg";
        objArr10[3] = hasElevationUncertaintyInDeg() ? Double.valueOf(this.mElevationUncertaintyInDeg) : null;
        sb.append(String.format("   %-29s = %-25s   %-40s = %s\n", objArr10));
        Object[] objArr11 = new Object[4];
        objArr11[0] = "AzimuthInDeg";
        objArr11[1] = hasAzimuthInDeg() ? Double.valueOf(this.mAzimuthInDeg) : null;
        objArr11[2] = "AzimuthUncertaintyInDeg";
        objArr11[3] = hasAzimuthUncertaintyInDeg() ? Double.valueOf(this.mAzimuthUncertaintyInDeg) : null;
        sb.append(String.format("   %-29s = %-25s   %-40s = %s\n", objArr11));
        sb.append(String.format("   %-29s = %s\n", "UsedInFix", Boolean.valueOf(this.mUsedInFix)));
        return sb.toString();
    }

    private void initialize() {
        this.mFlags = 0;
        setPrn((byte) -128);
        setTimeOffsetInNs(-9.223372036854776E18d);
        setState((short) 0);
        setReceivedGpsTowInNs(Long.MIN_VALUE);
        setReceivedGpsTowUncertaintyInNs(Long.MAX_VALUE);
        setCn0InDbHz(Double.MIN_VALUE);
        setPseudorangeRateInMetersPerSec(Double.MIN_VALUE);
        setPseudorangeRateUncertaintyInMetersPerSec(Double.MIN_VALUE);
        setAccumulatedDeltaRangeState((short) 0);
        setAccumulatedDeltaRangeInMeters(Double.MIN_VALUE);
        setAccumulatedDeltaRangeUncertaintyInMeters(Double.MIN_VALUE);
        resetPseudorangeInMeters();
        resetPseudorangeUncertaintyInMeters();
        resetCodePhaseInChips();
        resetCodePhaseUncertaintyInChips();
        resetCarrierFrequencyInHz();
        resetCarrierCycles();
        resetCarrierPhase();
        resetCarrierPhaseUncertainty();
        setLossOfLock((byte) 0);
        resetBitNumber();
        resetTimeFromLastBitInMs();
        resetDopplerShiftInHz();
        resetDopplerShiftUncertaintyInHz();
        setMultipathIndicator((byte) 0);
        resetSnrInDb();
        resetElevationInDeg();
        resetElevationUncertaintyInDeg();
        resetAzimuthInDeg();
        resetAzimuthUncertaintyInDeg();
        setUsedInFix(false);
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
