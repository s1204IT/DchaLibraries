package android.telephony;

import android.content.pm.PackageManager;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class SignalStrength implements Parcelable {
    private static final boolean DBG = false;
    public static final int INVALID = Integer.MAX_VALUE;
    private static final String LOG_TAG = "SignalStrength";
    private static final int LTE_RSRP_THRESHOLDS_NUM = 4;
    private static final int MAX_LTE_RSRP = -44;
    private static final int MAX_WCDMA_RSCP = -24;
    private static final String MEASUMENT_TYPE_RSCP = "rscp";
    private static final int MIN_LTE_RSRP = -140;
    private static final int MIN_WCDMA_RSCP = -120;
    public static final int NUM_SIGNAL_STRENGTH_BINS = 5;
    public static final int SIGNAL_STRENGTH_GOOD = 3;
    public static final int SIGNAL_STRENGTH_GREAT = 4;
    public static final int SIGNAL_STRENGTH_MODERATE = 2;
    public static final int SIGNAL_STRENGTH_NONE_OR_UNKNOWN = 0;
    public static final int SIGNAL_STRENGTH_POOR = 1;
    private static final int WCDMA_RSCP_THRESHOLDS_NUM = 4;
    private int mCdmaDbm;
    private int mCdmaEcio;
    private int mEvdoDbm;
    private int mEvdoEcio;
    private int mEvdoSnr;
    private int mGsmBitErrorRate;
    private int mGsmSignalStrength;
    private boolean mIsGsm;
    private int mLteCqi;
    protected int mLteRsrp;
    private int mLteRsrpBoost;
    private int[] mLteRsrpThresholds;
    private int mLteRsrq;
    protected int mLteRssnr;
    protected int mLteSignalStrength;
    private int mTdScdmaRscp;
    private boolean mUseOnlyRsrpForLteLevel;
    private String mWcdmaDefaultSignalMeasurement;
    private int mWcdmaRscp;
    private int mWcdmaRscpAsu;
    private int[] mWcdmaRscpThresholds;
    private int mWcdmaSignalStrength;
    public static final String[] SIGNAL_STRENGTH_NAMES = {"none", "poor", "moderate", "good", "great"};
    public static final Parcelable.Creator<SignalStrength> CREATOR = new Parcelable.Creator() {
        @Override
        public SignalStrength createFromParcel(Parcel parcel) {
            return SignalStrength.makeSignalStrength(parcel);
        }

        @Override
        public SignalStrength[] newArray(int i) {
            return new SignalStrength[i];
        }
    };

    public static SignalStrength newFromBundle(Bundle bundle) {
        SignalStrength signalStrength = new SignalStrength();
        signalStrength.setFromNotifierBundle(bundle);
        return signalStrength;
    }

    public SignalStrength() {
        this(true);
    }

    public SignalStrength(boolean z) {
        this.mLteRsrpThresholds = new int[4];
        this.mWcdmaRscpThresholds = new int[4];
        this.mGsmSignalStrength = 99;
        this.mGsmBitErrorRate = -1;
        this.mCdmaDbm = -1;
        this.mCdmaEcio = -1;
        this.mEvdoDbm = -1;
        this.mEvdoEcio = -1;
        this.mEvdoSnr = -1;
        this.mLteSignalStrength = 99;
        this.mLteRsrp = Integer.MAX_VALUE;
        this.mLteRsrq = Integer.MAX_VALUE;
        this.mLteRssnr = Integer.MAX_VALUE;
        this.mLteCqi = Integer.MAX_VALUE;
        this.mTdScdmaRscp = Integer.MAX_VALUE;
        this.mWcdmaSignalStrength = 99;
        this.mWcdmaRscp = Integer.MAX_VALUE;
        this.mWcdmaRscpAsu = 255;
        this.mLteRsrpBoost = 0;
        this.mIsGsm = z;
        this.mUseOnlyRsrpForLteLevel = false;
        this.mWcdmaDefaultSignalMeasurement = "";
        setLteRsrpThresholds(getDefaultLteRsrpThresholds());
        setWcdmaRscpThresholds(getDefaultWcdmaRscpThresholds());
    }

    public SignalStrength(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10, int i11, int i12, int i13, int i14, int i15, int i16, boolean z, boolean z2, String str) {
        this.mLteRsrpThresholds = new int[4];
        this.mWcdmaRscpThresholds = new int[4];
        this.mGsmSignalStrength = i;
        this.mGsmBitErrorRate = i2;
        this.mCdmaDbm = i3;
        this.mCdmaEcio = i4;
        this.mEvdoDbm = i5;
        this.mEvdoEcio = i6;
        this.mEvdoSnr = i7;
        this.mLteSignalStrength = i8;
        this.mLteRsrp = i9;
        this.mLteRsrq = i10;
        this.mLteRssnr = i11;
        this.mLteCqi = i12;
        this.mTdScdmaRscp = Integer.MAX_VALUE;
        this.mWcdmaSignalStrength = i14;
        this.mWcdmaRscpAsu = i15;
        this.mWcdmaRscp = i15 + MIN_WCDMA_RSCP;
        this.mLteRsrpBoost = i16;
        this.mIsGsm = z;
        this.mUseOnlyRsrpForLteLevel = z2;
        this.mWcdmaDefaultSignalMeasurement = str;
        setLteRsrpThresholds(getDefaultLteRsrpThresholds());
        setWcdmaRscpThresholds(getDefaultWcdmaRscpThresholds());
    }

    public SignalStrength(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10, int i11, int i12, int i13) {
        this(i, i2, i3, i4, i5, i6, i7, i8, i9, i10, i11, i12, i13, 99, Integer.MAX_VALUE, 0, true, false, "");
    }

    public SignalStrength(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10, int i11, int i12, int i13, int i14, int i15) {
        this(i, i2, i3, i4, i5, i6, i7, i8, i9, i10, i11, i12, i13, i14, i15, 0, true, false, "");
    }

    public SignalStrength(SignalStrength signalStrength) {
        this.mLteRsrpThresholds = new int[4];
        this.mWcdmaRscpThresholds = new int[4];
        copyFrom(signalStrength);
    }

    protected void copyFrom(SignalStrength signalStrength) {
        this.mGsmSignalStrength = signalStrength.mGsmSignalStrength;
        this.mGsmBitErrorRate = signalStrength.mGsmBitErrorRate;
        this.mCdmaDbm = signalStrength.mCdmaDbm;
        this.mCdmaEcio = signalStrength.mCdmaEcio;
        this.mEvdoDbm = signalStrength.mEvdoDbm;
        this.mEvdoEcio = signalStrength.mEvdoEcio;
        this.mEvdoSnr = signalStrength.mEvdoSnr;
        this.mLteSignalStrength = signalStrength.mLteSignalStrength;
        this.mLteRsrp = signalStrength.mLteRsrp;
        this.mLteRsrq = signalStrength.mLteRsrq;
        this.mLteRssnr = signalStrength.mLteRssnr;
        this.mLteCqi = signalStrength.mLteCqi;
        this.mTdScdmaRscp = signalStrength.mTdScdmaRscp;
        this.mWcdmaSignalStrength = signalStrength.mWcdmaSignalStrength;
        this.mWcdmaRscpAsu = signalStrength.mWcdmaRscpAsu;
        this.mWcdmaRscp = signalStrength.mWcdmaRscp;
        this.mLteRsrpBoost = signalStrength.mLteRsrpBoost;
        this.mIsGsm = signalStrength.mIsGsm;
        this.mUseOnlyRsrpForLteLevel = signalStrength.mUseOnlyRsrpForLteLevel;
        this.mWcdmaDefaultSignalMeasurement = signalStrength.mWcdmaDefaultSignalMeasurement;
        setLteRsrpThresholds(signalStrength.mLteRsrpThresholds);
        setWcdmaRscpThresholds(signalStrength.mWcdmaRscpThresholds);
    }

    public SignalStrength(Parcel parcel) {
        this.mLteRsrpThresholds = new int[4];
        this.mWcdmaRscpThresholds = new int[4];
        this.mGsmSignalStrength = parcel.readInt();
        this.mGsmBitErrorRate = parcel.readInt();
        this.mCdmaDbm = parcel.readInt();
        this.mCdmaEcio = parcel.readInt();
        this.mEvdoDbm = parcel.readInt();
        this.mEvdoEcio = parcel.readInt();
        this.mEvdoSnr = parcel.readInt();
        this.mLteSignalStrength = parcel.readInt();
        this.mLteRsrp = parcel.readInt();
        this.mLteRsrq = parcel.readInt();
        this.mLteRssnr = parcel.readInt();
        this.mLteCqi = parcel.readInt();
        this.mTdScdmaRscp = parcel.readInt();
        this.mWcdmaSignalStrength = parcel.readInt();
        this.mWcdmaRscpAsu = parcel.readInt();
        this.mWcdmaRscp = parcel.readInt();
        this.mLteRsrpBoost = parcel.readInt();
        this.mIsGsm = parcel.readBoolean();
        this.mUseOnlyRsrpForLteLevel = parcel.readBoolean();
        this.mWcdmaDefaultSignalMeasurement = parcel.readString();
        parcel.readIntArray(this.mLteRsrpThresholds);
        parcel.readIntArray(this.mWcdmaRscpThresholds);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mGsmSignalStrength);
        parcel.writeInt(this.mGsmBitErrorRate);
        parcel.writeInt(this.mCdmaDbm);
        parcel.writeInt(this.mCdmaEcio);
        parcel.writeInt(this.mEvdoDbm);
        parcel.writeInt(this.mEvdoEcio);
        parcel.writeInt(this.mEvdoSnr);
        parcel.writeInt(this.mLteSignalStrength);
        parcel.writeInt(this.mLteRsrp);
        parcel.writeInt(this.mLteRsrq);
        parcel.writeInt(this.mLteRssnr);
        parcel.writeInt(this.mLteCqi);
        parcel.writeInt(this.mTdScdmaRscp);
        parcel.writeInt(this.mWcdmaSignalStrength);
        parcel.writeInt(this.mWcdmaRscpAsu);
        parcel.writeInt(this.mWcdmaRscp);
        parcel.writeInt(this.mLteRsrpBoost);
        parcel.writeBoolean(this.mIsGsm);
        parcel.writeBoolean(this.mUseOnlyRsrpForLteLevel);
        parcel.writeString(this.mWcdmaDefaultSignalMeasurement);
        parcel.writeIntArray(this.mLteRsrpThresholds);
        parcel.writeIntArray(this.mWcdmaRscpThresholds);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public void validateInput() {
        this.mGsmSignalStrength = this.mGsmSignalStrength >= 0 ? this.mGsmSignalStrength : 99;
        this.mWcdmaSignalStrength = this.mWcdmaSignalStrength >= 0 ? this.mWcdmaSignalStrength : 99;
        this.mLteSignalStrength = this.mLteSignalStrength >= 0 ? this.mLteSignalStrength : 99;
        int i = this.mWcdmaRscpAsu + MIN_WCDMA_RSCP;
        int i2 = MIN_WCDMA_RSCP;
        this.mWcdmaRscpAsu = (i < MIN_WCDMA_RSCP || this.mWcdmaRscpAsu + MIN_WCDMA_RSCP > -24) ? 255 : this.mWcdmaRscpAsu;
        int i3 = Integer.MAX_VALUE;
        this.mWcdmaRscp = (this.mWcdmaRscp < MIN_WCDMA_RSCP || this.mWcdmaRscp > -24) ? Integer.MAX_VALUE : this.mWcdmaRscp;
        this.mCdmaDbm = this.mCdmaDbm > 0 ? -this.mCdmaDbm : MIN_WCDMA_RSCP;
        this.mCdmaEcio = this.mCdmaEcio >= 0 ? -this.mCdmaEcio : -160;
        if (this.mEvdoDbm > 0) {
            i2 = -this.mEvdoDbm;
        }
        this.mEvdoDbm = i2;
        this.mEvdoEcio = this.mEvdoEcio >= 0 ? -this.mEvdoEcio : -160;
        this.mEvdoSnr = (this.mEvdoSnr < 0 || this.mEvdoSnr > 8) ? -1 : this.mEvdoSnr;
        this.mLteRsrp = ((-this.mLteRsrp) < MIN_LTE_RSRP || (-this.mLteRsrp) > -44) ? Integer.MAX_VALUE : -this.mLteRsrp;
        this.mLteRsrq = (this.mLteRsrq < 3 || this.mLteRsrq > 20) ? Integer.MAX_VALUE : -this.mLteRsrq;
        this.mLteRssnr = (this.mLteRssnr < -200 || this.mLteRssnr > 300) ? Integer.MAX_VALUE : this.mLteRssnr;
        if (this.mTdScdmaRscp >= 0 && this.mTdScdmaRscp <= 96) {
            i3 = this.mTdScdmaRscp + MIN_WCDMA_RSCP;
        }
        this.mTdScdmaRscp = i3;
    }

    public void fixType() {
        this.mIsGsm = getCdmaRelatedSignalStrength() == 0;
    }

    public void setGsm(boolean z) {
        this.mIsGsm = z;
    }

    public void setUseOnlyRsrpForLteLevel(boolean z) {
        this.mUseOnlyRsrpForLteLevel = z;
    }

    public void setWcdmaDefaultSignalMeasurement(String str) {
        this.mWcdmaDefaultSignalMeasurement = str;
    }

    public void setLteRsrpBoost(int i) {
        this.mLteRsrpBoost = i;
    }

    public void setLteRsrpThresholds(int[] iArr) {
        if (iArr == null || iArr.length != 4) {
            Log.wtf(LOG_TAG, "setLteRsrpThresholds - lteRsrpThresholds is invalid.");
        } else {
            System.arraycopy(iArr, 0, this.mLteRsrpThresholds, 0, 4);
        }
    }

    public int getGsmSignalStrength() {
        return this.mGsmSignalStrength;
    }

    public int getGsmBitErrorRate() {
        return this.mGsmBitErrorRate;
    }

    public void setWcdmaRscpThresholds(int[] iArr) {
        if (iArr == null || iArr.length != 4) {
            Log.wtf(LOG_TAG, "setWcdmaRscpThresholds - wcdmaRscpThresholds is invalid.");
        } else {
            System.arraycopy(iArr, 0, this.mWcdmaRscpThresholds, 0, 4);
        }
    }

    public int getCdmaDbm() {
        return this.mCdmaDbm;
    }

    public int getCdmaEcio() {
        return this.mCdmaEcio;
    }

    public int getEvdoDbm() {
        return this.mEvdoDbm;
    }

    public int getEvdoEcio() {
        return this.mEvdoEcio;
    }

    public int getEvdoSnr() {
        return this.mEvdoSnr;
    }

    public int getLteSignalStrength() {
        return this.mLteSignalStrength;
    }

    public int getLteRsrp() {
        return this.mLteRsrp;
    }

    public int getLteRsrq() {
        return this.mLteRsrq;
    }

    public int getLteRssnr() {
        return this.mLteRssnr;
    }

    public int getLteCqi() {
        return this.mLteCqi;
    }

    public int getLteRsrpBoost() {
        return this.mLteRsrpBoost;
    }

    public int getLevel() {
        return this.mIsGsm ? getGsmRelatedSignalStrength() : getCdmaRelatedSignalStrength();
    }

    public int getAsuLevel() {
        if (this.mIsGsm) {
            if (this.mLteRsrp != Integer.MAX_VALUE) {
                return getLteAsuLevel();
            }
            if (this.mTdScdmaRscp != Integer.MAX_VALUE) {
                return getTdScdmaAsuLevel();
            }
            if (this.mWcdmaRscp != Integer.MAX_VALUE) {
                return getWcdmaAsuLevel();
            }
            return getGsmAsuLevel();
        }
        int cdmaAsuLevel = getCdmaAsuLevel();
        int evdoAsuLevel = getEvdoAsuLevel();
        if (evdoAsuLevel == 0) {
            return cdmaAsuLevel;
        }
        return (cdmaAsuLevel != 0 && cdmaAsuLevel < evdoAsuLevel) ? cdmaAsuLevel : evdoAsuLevel;
    }

    public int getDbm() {
        if (isGsm()) {
            int lteDbm = getLteDbm();
            if (lteDbm == Integer.MAX_VALUE) {
                if (getTdScdmaLevel() == 0) {
                    if (getWcdmaDbm() == Integer.MAX_VALUE) {
                        return getGsmDbm();
                    }
                    return getWcdmaDbm();
                }
                return getTdScdmaDbm();
            }
            return lteDbm;
        }
        int cdmaDbm = getCdmaDbm();
        int evdoDbm = getEvdoDbm();
        return evdoDbm == MIN_WCDMA_RSCP ? cdmaDbm : (cdmaDbm != MIN_WCDMA_RSCP && cdmaDbm < evdoDbm) ? cdmaDbm : evdoDbm;
    }

    public int getGsmDbm() {
        int gsmSignalStrength = getGsmSignalStrength();
        if (gsmSignalStrength == 99) {
            gsmSignalStrength = -1;
        }
        if (gsmSignalStrength == -1) {
            return -1;
        }
        return PackageManager.INSTALL_FAILED_NO_MATCHING_ABIS + (2 * gsmSignalStrength);
    }

    public int getGsmLevel() {
        int gsmSignalStrength = getGsmSignalStrength();
        if (gsmSignalStrength <= 2 || gsmSignalStrength == 99) {
            return 0;
        }
        if (gsmSignalStrength >= 12) {
            return 4;
        }
        if (gsmSignalStrength >= 8) {
            return 3;
        }
        if (gsmSignalStrength >= 5) {
            return 2;
        }
        return 1;
    }

    public int getGsmAsuLevel() {
        return getGsmSignalStrength();
    }

    public int getCdmaLevel() {
        int cdmaDbm = getCdmaDbm();
        int cdmaEcio = getCdmaEcio();
        int i = 0;
        int i2 = cdmaDbm >= -75 ? 4 : cdmaDbm >= -85 ? 3 : cdmaDbm >= -95 ? 2 : cdmaDbm >= -100 ? 1 : 0;
        if (cdmaEcio >= -90) {
            i = 4;
        } else if (cdmaEcio >= -110) {
            i = 3;
        } else if (cdmaEcio >= -130) {
            i = 2;
        } else if (cdmaEcio >= -150) {
            i = 1;
        }
        return i2 < i ? i2 : i;
    }

    public int getCdmaAsuLevel() {
        int cdmaDbm = getCdmaDbm();
        int cdmaEcio = getCdmaEcio();
        int i = 99;
        int i2 = cdmaDbm >= -75 ? 16 : cdmaDbm >= -82 ? 8 : cdmaDbm >= -90 ? 4 : cdmaDbm >= -95 ? 2 : cdmaDbm >= -100 ? 1 : 99;
        if (cdmaEcio >= -90) {
            i = 16;
        } else if (cdmaEcio >= -100) {
            i = 8;
        } else if (cdmaEcio >= -115) {
            i = 4;
        } else if (cdmaEcio >= -130) {
            i = 2;
        } else if (cdmaEcio >= -150) {
            i = 1;
        }
        return i2 < i ? i2 : i;
    }

    public int getEvdoLevel() {
        int evdoDbm = getEvdoDbm();
        int evdoSnr = getEvdoSnr();
        int i = 0;
        int i2 = evdoDbm >= -65 ? 4 : evdoDbm >= -75 ? 3 : evdoDbm >= -90 ? 2 : evdoDbm >= -105 ? 1 : 0;
        if (evdoSnr >= 7) {
            i = 4;
        } else if (evdoSnr >= 5) {
            i = 3;
        } else if (evdoSnr >= 3) {
            i = 2;
        } else if (evdoSnr >= 1) {
            i = 1;
        }
        return i2 < i ? i2 : i;
    }

    public int getEvdoAsuLevel() {
        int evdoDbm = getEvdoDbm();
        int evdoSnr = getEvdoSnr();
        int i = 99;
        int i2 = evdoDbm >= -65 ? 16 : evdoDbm >= -75 ? 8 : evdoDbm >= -85 ? 4 : evdoDbm >= -95 ? 2 : evdoDbm >= -105 ? 1 : 99;
        if (evdoSnr >= 7) {
            i = 16;
        } else if (evdoSnr >= 6) {
            i = 8;
        } else if (evdoSnr >= 5) {
            i = 4;
        } else if (evdoSnr >= 3) {
            i = 2;
        } else if (evdoSnr >= 1) {
            i = 1;
        }
        return i2 < i ? i2 : i;
    }

    public int getLteDbm() {
        return this.mLteRsrp;
    }

    public int getLteLevel() {
        int i;
        int i2;
        if (this.mLteRsrp > -44 || this.mLteRsrp < MIN_LTE_RSRP) {
            if (this.mLteRsrp != Integer.MAX_VALUE) {
                Log.wtf(LOG_TAG, "getLteLevel - invalid lte rsrp: mLteRsrp=" + this.mLteRsrp);
            }
            i = -1;
        } else {
            i = this.mLteRsrp >= this.mLteRsrpThresholds[3] - this.mLteRsrpBoost ? 4 : this.mLteRsrp >= this.mLteRsrpThresholds[2] - this.mLteRsrpBoost ? 3 : this.mLteRsrp >= this.mLteRsrpThresholds[1] - this.mLteRsrpBoost ? 2 : this.mLteRsrp >= this.mLteRsrpThresholds[0] - this.mLteRsrpBoost ? 1 : 0;
        }
        if (useOnlyRsrpForLteLevel()) {
            log("getLTELevel - rsrp = " + i);
            if (i != -1) {
                return i;
            }
        }
        if (this.mLteRssnr <= 300) {
            i2 = this.mLteRssnr >= 130 ? 4 : this.mLteRssnr >= 45 ? 3 : this.mLteRssnr >= 10 ? 2 : this.mLteRssnr >= -30 ? 1 : this.mLteRssnr >= -200 ? 0 : -1;
        }
        if (i2 != -1 && i != -1) {
            return i < i2 ? i : i2;
        }
        if (i2 != -1) {
            return i2;
        }
        if (i != -1) {
            return i;
        }
        if (this.mLteSignalStrength > 63) {
            return 0;
        }
        if (this.mLteSignalStrength >= 12) {
            return 4;
        }
        if (this.mLteSignalStrength >= 8) {
            return 3;
        }
        if (this.mLteSignalStrength >= 5) {
            return 2;
        }
        return this.mLteSignalStrength >= 0 ? 1 : 0;
    }

    public int getLteAsuLevel() {
        int lteDbm = getLteDbm();
        if (lteDbm == Integer.MAX_VALUE) {
            return 255;
        }
        return lteDbm + 140;
    }

    public boolean isGsm() {
        return this.mIsGsm;
    }

    public boolean useOnlyRsrpForLteLevel() {
        return this.mUseOnlyRsrpForLteLevel;
    }

    public int getTdScdmaDbm() {
        return this.mTdScdmaRscp;
    }

    public int getTdScdmaLevel() {
        int tdScdmaDbm = getTdScdmaDbm();
        if (tdScdmaDbm > -25 || tdScdmaDbm == Integer.MAX_VALUE) {
            return 0;
        }
        if (tdScdmaDbm >= -49) {
            return 4;
        }
        if (tdScdmaDbm >= -73) {
            return 3;
        }
        if (tdScdmaDbm >= -97) {
            return 2;
        }
        return tdScdmaDbm >= -110 ? 1 : 0;
    }

    public int getTdScdmaAsuLevel() {
        int tdScdmaDbm = getTdScdmaDbm();
        if (tdScdmaDbm == Integer.MAX_VALUE) {
            return 255;
        }
        return tdScdmaDbm + 120;
    }

    public int getWcdmaRscp() {
        return this.mWcdmaRscp;
    }

    public int getWcdmaAsuLevel() {
        int wcdmaDbm = getWcdmaDbm();
        if (wcdmaDbm == Integer.MAX_VALUE) {
            return 255;
        }
        return wcdmaDbm + 120;
    }

    public int getWcdmaDbm() {
        return this.mWcdmaRscp;
    }

    public int getWcdmaLevel() {
        if (this.mWcdmaDefaultSignalMeasurement == null) {
            Log.wtf(LOG_TAG, "getWcdmaLevel - WCDMA default signal measurement is invalid.");
            return 0;
        }
        String str = this.mWcdmaDefaultSignalMeasurement;
        byte b = -1;
        if (str.hashCode() == 3509870 && str.equals(MEASUMENT_TYPE_RSCP)) {
            b = 0;
        }
        if (b != 0) {
            if (this.mWcdmaSignalStrength >= 0 && this.mWcdmaSignalStrength <= 31) {
                if (this.mWcdmaSignalStrength >= 18) {
                    return 4;
                }
                if (this.mWcdmaSignalStrength < 13) {
                    if (this.mWcdmaSignalStrength < 8) {
                    }
                    return 2;
                }
                return 3;
            }
            if (this.mWcdmaSignalStrength != 99) {
                Log.wtf(LOG_TAG, "getWcdmaLevel - invalid WCDMA RSSI: mWcdmaSignalStrength=" + this.mWcdmaSignalStrength);
            }
            return 0;
        }
        if (this.mWcdmaRscp >= MIN_WCDMA_RSCP && this.mWcdmaRscp <= -24) {
            if (this.mWcdmaRscp >= this.mWcdmaRscpThresholds[3]) {
                return 4;
            }
            if (this.mWcdmaRscp < this.mWcdmaRscpThresholds[2]) {
                if (this.mWcdmaRscp < this.mWcdmaRscpThresholds[1]) {
                }
                return 2;
            }
            return 3;
        }
        if (this.mWcdmaRscp != Integer.MAX_VALUE) {
            Log.wtf(LOG_TAG, "getWcdmaLevel - invalid WCDMA RSCP: mWcdmaRscp=" + this.mWcdmaRscp);
        }
        return 0;
    }

    public int hashCode() {
        return (this.mGsmSignalStrength * 31) + (this.mGsmBitErrorRate * 31) + (this.mCdmaDbm * 31) + (this.mCdmaEcio * 31) + (this.mEvdoDbm * 31) + (this.mEvdoEcio * 31) + (this.mEvdoSnr * 31) + (this.mLteSignalStrength * 31) + (this.mLteRsrp * 31) + (this.mLteRsrq * 31) + (this.mLteRssnr * 31) + (this.mLteCqi * 31) + (this.mLteRsrpBoost * 31) + (this.mTdScdmaRscp * 31) + (this.mWcdmaSignalStrength * 31) + (this.mWcdmaRscpAsu * 31) + (this.mWcdmaRscp * 31) + (this.mIsGsm ? 1 : 0) + (this.mUseOnlyRsrpForLteLevel ? 1 : 0) + Objects.hashCode(this.mWcdmaDefaultSignalMeasurement) + Arrays.hashCode(this.mLteRsrpThresholds) + Arrays.hashCode(this.mWcdmaRscpThresholds);
    }

    public boolean equals(Object obj) {
        try {
            SignalStrength signalStrength = (SignalStrength) obj;
            return obj != null && this.mGsmSignalStrength == signalStrength.mGsmSignalStrength && this.mGsmBitErrorRate == signalStrength.mGsmBitErrorRate && this.mCdmaDbm == signalStrength.mCdmaDbm && this.mCdmaEcio == signalStrength.mCdmaEcio && this.mEvdoDbm == signalStrength.mEvdoDbm && this.mEvdoEcio == signalStrength.mEvdoEcio && this.mEvdoSnr == signalStrength.mEvdoSnr && this.mLteSignalStrength == signalStrength.mLteSignalStrength && this.mLteRsrp == signalStrength.mLteRsrp && this.mLteRsrq == signalStrength.mLteRsrq && this.mLteRssnr == signalStrength.mLteRssnr && this.mLteCqi == signalStrength.mLteCqi && this.mLteRsrpBoost == signalStrength.mLteRsrpBoost && this.mTdScdmaRscp == signalStrength.mTdScdmaRscp && this.mWcdmaSignalStrength == signalStrength.mWcdmaSignalStrength && this.mWcdmaRscpAsu == signalStrength.mWcdmaRscpAsu && this.mWcdmaRscp == signalStrength.mWcdmaRscp && this.mIsGsm == signalStrength.mIsGsm && this.mUseOnlyRsrpForLteLevel == signalStrength.mUseOnlyRsrpForLteLevel && Objects.equals(this.mWcdmaDefaultSignalMeasurement, signalStrength.mWcdmaDefaultSignalMeasurement) && Arrays.equals(this.mLteRsrpThresholds, signalStrength.mLteRsrpThresholds) && Arrays.equals(this.mWcdmaRscpThresholds, signalStrength.mWcdmaRscpThresholds);
        } catch (ClassCastException e) {
            return false;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SignalStrength: ");
        sb.append(this.mGsmSignalStrength);
        sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        sb.append(this.mGsmBitErrorRate);
        sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        sb.append(this.mCdmaDbm);
        sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        sb.append(this.mCdmaEcio);
        sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        sb.append(this.mEvdoDbm);
        sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        sb.append(this.mEvdoEcio);
        sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        sb.append(this.mEvdoSnr);
        sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        sb.append(this.mLteSignalStrength);
        sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        sb.append(this.mLteRsrp);
        sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        sb.append(this.mLteRsrq);
        sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        sb.append(this.mLteRssnr);
        sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        sb.append(this.mLteCqi);
        sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        sb.append(this.mLteRsrpBoost);
        sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        sb.append(this.mTdScdmaRscp);
        sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        sb.append(this.mWcdmaSignalStrength);
        sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        sb.append(this.mWcdmaRscpAsu);
        sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        sb.append(this.mWcdmaRscp);
        sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        sb.append(this.mIsGsm ? "gsm|lte" : "cdma");
        sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        sb.append(this.mUseOnlyRsrpForLteLevel ? "use_only_rsrp_for_lte_level" : "use_rsrp_and_rssnr_for_lte_level");
        sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        sb.append(this.mWcdmaDefaultSignalMeasurement);
        sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        sb.append(Arrays.toString(this.mLteRsrpThresholds));
        sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        sb.append(Arrays.toString(this.mWcdmaRscpThresholds));
        return sb.toString();
    }

    private int getGsmRelatedSignalStrength() {
        int lteLevel = getLteLevel();
        if (lteLevel == 0) {
            int tdScdmaLevel = getTdScdmaLevel();
            if (tdScdmaLevel == 0) {
                int wcdmaLevel = getWcdmaLevel();
                if (wcdmaLevel == 0) {
                    return getGsmLevel();
                }
                return wcdmaLevel;
            }
            return tdScdmaLevel;
        }
        return lteLevel;
    }

    private int getCdmaRelatedSignalStrength() {
        int cdmaLevel = getCdmaLevel();
        int evdoLevel = getEvdoLevel();
        if (evdoLevel == 0) {
            return cdmaLevel;
        }
        return (cdmaLevel != 0 && cdmaLevel < evdoLevel) ? cdmaLevel : evdoLevel;
    }

    private void setFromNotifierBundle(Bundle bundle) {
        this.mGsmSignalStrength = bundle.getInt("GsmSignalStrength");
        this.mGsmBitErrorRate = bundle.getInt("GsmBitErrorRate");
        this.mCdmaDbm = bundle.getInt("CdmaDbm");
        this.mCdmaEcio = bundle.getInt("CdmaEcio");
        this.mEvdoDbm = bundle.getInt("EvdoDbm");
        this.mEvdoEcio = bundle.getInt("EvdoEcio");
        this.mEvdoSnr = bundle.getInt("EvdoSnr");
        this.mLteSignalStrength = bundle.getInt("LteSignalStrength");
        this.mLteRsrp = bundle.getInt("LteRsrp");
        this.mLteRsrq = bundle.getInt("LteRsrq");
        this.mLteRssnr = bundle.getInt("LteRssnr");
        this.mLteCqi = bundle.getInt("LteCqi");
        this.mLteRsrpBoost = bundle.getInt("LteRsrpBoost");
        this.mTdScdmaRscp = bundle.getInt("TdScdma");
        this.mWcdmaSignalStrength = bundle.getInt("WcdmaSignalStrength");
        this.mWcdmaRscpAsu = bundle.getInt("WcdmaRscpAsu");
        this.mWcdmaRscp = bundle.getInt("WcdmaRscp");
        this.mIsGsm = bundle.getBoolean("IsGsm");
        this.mUseOnlyRsrpForLteLevel = bundle.getBoolean("UseOnlyRsrpForLteLevel");
        this.mWcdmaDefaultSignalMeasurement = bundle.getString("WcdmaDefaultSignalMeasurement");
        ArrayList<Integer> integerArrayList = bundle.getIntegerArrayList("lteRsrpThresholds");
        for (int i = 0; i < integerArrayList.size(); i++) {
            this.mLteRsrpThresholds[i] = integerArrayList.get(i).intValue();
        }
        ArrayList<Integer> integerArrayList2 = bundle.getIntegerArrayList("wcdmaRscpThresholds");
        for (int i2 = 0; i2 < integerArrayList2.size(); i2++) {
            this.mWcdmaRscpThresholds[i2] = integerArrayList2.get(i2).intValue();
        }
    }

    public void fillInNotifierBundle(Bundle bundle) {
        bundle.putInt("GsmSignalStrength", this.mGsmSignalStrength);
        bundle.putInt("GsmBitErrorRate", this.mGsmBitErrorRate);
        bundle.putInt("CdmaDbm", this.mCdmaDbm);
        bundle.putInt("CdmaEcio", this.mCdmaEcio);
        bundle.putInt("EvdoDbm", this.mEvdoDbm);
        bundle.putInt("EvdoEcio", this.mEvdoEcio);
        bundle.putInt("EvdoSnr", this.mEvdoSnr);
        bundle.putInt("LteSignalStrength", this.mLteSignalStrength);
        bundle.putInt("LteRsrp", this.mLteRsrp);
        bundle.putInt("LteRsrq", this.mLteRsrq);
        bundle.putInt("LteRssnr", this.mLteRssnr);
        bundle.putInt("LteCqi", this.mLteCqi);
        bundle.putInt("LteRsrpBoost", this.mLteRsrpBoost);
        bundle.putInt("TdScdma", this.mTdScdmaRscp);
        bundle.putInt("WcdmaSignalStrength", this.mWcdmaSignalStrength);
        bundle.putInt("WcdmaRscpAsu", this.mWcdmaRscpAsu);
        bundle.putInt("WcdmaRscp", this.mWcdmaRscp);
        bundle.putBoolean("IsGsm", this.mIsGsm);
        bundle.putBoolean("UseOnlyRsrpForLteLevel", this.mUseOnlyRsrpForLteLevel);
        bundle.putString("WcdmaDefaultSignalMeasurement", this.mWcdmaDefaultSignalMeasurement);
        ArrayList<Integer> arrayList = new ArrayList<>();
        for (int i : this.mLteRsrpThresholds) {
            arrayList.add(Integer.valueOf(i));
        }
        bundle.putIntegerArrayList("lteRsrpThresholds", arrayList);
        ArrayList<Integer> arrayList2 = new ArrayList<>();
        for (int i2 : this.mWcdmaRscpThresholds) {
            arrayList2.add(Integer.valueOf(i2));
        }
        bundle.putIntegerArrayList("wcdmaRscpThresholds", arrayList2);
    }

    private int[] getDefaultLteRsrpThresholds() {
        return CarrierConfigManager.getDefaultConfig().getIntArray(CarrierConfigManager.KEY_LTE_RSRP_THRESHOLDS_INT_ARRAY);
    }

    private int[] getDefaultWcdmaRscpThresholds() {
        return CarrierConfigManager.getDefaultConfig().getIntArray(CarrierConfigManager.KEY_WCDMA_RSCP_THRESHOLDS_INT_ARRAY);
    }

    private static void log(String str) {
        Rlog.w(LOG_TAG, str);
    }

    private static SignalStrength makeSignalStrength(Parcel parcel) {
        try {
            Constructor<?> constructor = Class.forName("mediatek.telephony.MtkSignalStrength").getConstructor(Parcel.class);
            constructor.setAccessible(true);
            return (SignalStrength) constructor.newInstance(parcel);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Rlog.e(LOG_TAG, "MtkSignalStrength IllegalAccessException! Used AOSP instead!");
            return new SignalStrength(parcel);
        } catch (InstantiationException e2) {
            e2.printStackTrace();
            Rlog.e(LOG_TAG, "MtkSignalStrength InstantiationException! Used AOSP instead!");
            return new SignalStrength(parcel);
        } catch (InvocationTargetException e3) {
            e3.printStackTrace();
            Rlog.e(LOG_TAG, "MtkSignalStrength InvocationTargetException! Used AOSP instead!");
            return new SignalStrength(parcel);
        } catch (Exception e4) {
            Rlog.e(LOG_TAG, "No MtkSignalStrength! Used AOSP instead!");
            return new SignalStrength(parcel);
        }
    }
}
