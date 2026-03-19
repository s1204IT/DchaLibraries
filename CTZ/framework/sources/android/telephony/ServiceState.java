package android.telephony;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.CalendarContract;
import android.text.TextUtils;
import com.android.internal.telephony.IccCardConstants;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ServiceState implements Parcelable {
    public static final Parcelable.Creator<ServiceState> CREATOR = new Parcelable.Creator<ServiceState>() {
        @Override
        public ServiceState createFromParcel(Parcel parcel) {
            return ServiceState.makeServiceState(parcel);
        }

        @Override
        public ServiceState[] newArray(int i) {
            return new ServiceState[i];
        }
    };
    static final boolean DBG = false;
    public static final int DUPLEX_MODE_FDD = 1;
    public static final int DUPLEX_MODE_TDD = 2;
    public static final int DUPLEX_MODE_UNKNOWN = 0;
    static final String LOG_TAG = "PHONE";
    private static final int NEXT_RIL_RADIO_TECHNOLOGY = 20;
    public static final int RIL_RADIO_CDMA_TECHNOLOGY_BITMASK = 6392;
    public static final int RIL_RADIO_TECHNOLOGY_1xRTT = 6;
    public static final int RIL_RADIO_TECHNOLOGY_EDGE = 2;
    public static final int RIL_RADIO_TECHNOLOGY_EHRPD = 13;
    public static final int RIL_RADIO_TECHNOLOGY_EVDO_0 = 7;
    public static final int RIL_RADIO_TECHNOLOGY_EVDO_A = 8;
    public static final int RIL_RADIO_TECHNOLOGY_EVDO_B = 12;
    public static final int RIL_RADIO_TECHNOLOGY_GPRS = 1;
    public static final int RIL_RADIO_TECHNOLOGY_GSM = 16;
    public static final int RIL_RADIO_TECHNOLOGY_HSDPA = 9;
    public static final int RIL_RADIO_TECHNOLOGY_HSPA = 11;
    public static final int RIL_RADIO_TECHNOLOGY_HSPAP = 15;
    public static final int RIL_RADIO_TECHNOLOGY_HSUPA = 10;
    public static final int RIL_RADIO_TECHNOLOGY_IS95A = 4;
    public static final int RIL_RADIO_TECHNOLOGY_IS95B = 5;
    public static final int RIL_RADIO_TECHNOLOGY_IWLAN = 18;
    public static final int RIL_RADIO_TECHNOLOGY_LTE = 14;
    public static final int RIL_RADIO_TECHNOLOGY_LTE_CA = 19;
    public static final int RIL_RADIO_TECHNOLOGY_TD_SCDMA = 17;
    public static final int RIL_RADIO_TECHNOLOGY_UMTS = 3;
    public static final int RIL_RADIO_TECHNOLOGY_UNKNOWN = 0;
    public static final int ROAMING_TYPE_DOMESTIC = 2;
    public static final int ROAMING_TYPE_INTERNATIONAL = 3;
    public static final int ROAMING_TYPE_NOT_ROAMING = 0;
    public static final int ROAMING_TYPE_UNKNOWN = 1;
    public static final int STATE_EMERGENCY_ONLY = 2;
    public static final int STATE_IN_SERVICE = 0;
    public static final int STATE_OUT_OF_SERVICE = 1;
    public static final int STATE_POWER_OFF = 3;
    public static final int UNKNOWN_ID = -1;
    static final boolean VDBG = false;
    protected int mCdmaDefaultRoamingIndicator;
    protected int mCdmaEriIconIndex;
    protected int mCdmaEriIconMode;
    protected int mCdmaRoamingIndicator;
    protected int[] mCellBandwidths;
    protected int mChannelNumber;
    protected boolean mCssIndicator;
    protected String mDataOperatorAlphaLong;
    protected String mDataOperatorAlphaShort;
    protected String mDataOperatorNumeric;
    protected int mDataRegState;
    protected int mDataRoamingType;
    protected boolean mIsDataRoamingFromRegistration;
    protected boolean mIsEmergencyOnly;
    protected boolean mIsManualNetworkSelection;
    protected boolean mIsUsingCarrierAggregation;
    protected int mLteEarfcnRsrpBoost;
    protected int mNetworkId;
    protected List<NetworkRegistrationState> mNetworkRegistrationStates;
    protected int mRilDataRadioTechnology;
    protected int mRilVoiceRadioTechnology;
    protected int mSystemId;
    protected String mVoiceOperatorAlphaLong;
    protected String mVoiceOperatorAlphaShort;
    protected String mVoiceOperatorNumeric;
    protected int mVoiceRegState;
    protected int mVoiceRoamingType;

    @Retention(RetentionPolicy.SOURCE)
    public @interface DuplexMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface RilRadioTechnology {
    }

    public static final String getRoamingLogString(int i) {
        switch (i) {
            case 0:
                return CalendarContract.CalendarCache.TIMEZONE_TYPE_HOME;
            case 1:
                return "roaming";
            case 2:
                return "Domestic Roaming";
            case 3:
                return "International Roaming";
            default:
                return IccCardConstants.INTENT_VALUE_ICC_UNKNOWN;
        }
    }

    public static ServiceState newFromBundle(Bundle bundle) {
        ServiceState serviceState = new ServiceState();
        serviceState.setFromNotifierBundle(bundle);
        return serviceState;
    }

    public ServiceState() {
        this.mVoiceRegState = 1;
        this.mDataRegState = 1;
        this.mCellBandwidths = new int[0];
        this.mLteEarfcnRsrpBoost = 0;
        this.mNetworkRegistrationStates = new ArrayList();
    }

    public ServiceState(ServiceState serviceState) {
        this.mVoiceRegState = 1;
        this.mDataRegState = 1;
        this.mCellBandwidths = new int[0];
        this.mLteEarfcnRsrpBoost = 0;
        this.mNetworkRegistrationStates = new ArrayList();
        copyFrom(serviceState);
    }

    protected void copyFrom(ServiceState serviceState) {
        this.mVoiceRegState = serviceState.mVoiceRegState;
        this.mDataRegState = serviceState.mDataRegState;
        this.mVoiceRoamingType = serviceState.mVoiceRoamingType;
        this.mDataRoamingType = serviceState.mDataRoamingType;
        this.mVoiceOperatorAlphaLong = serviceState.mVoiceOperatorAlphaLong;
        this.mVoiceOperatorAlphaShort = serviceState.mVoiceOperatorAlphaShort;
        this.mVoiceOperatorNumeric = serviceState.mVoiceOperatorNumeric;
        this.mDataOperatorAlphaLong = serviceState.mDataOperatorAlphaLong;
        this.mDataOperatorAlphaShort = serviceState.mDataOperatorAlphaShort;
        this.mDataOperatorNumeric = serviceState.mDataOperatorNumeric;
        this.mIsManualNetworkSelection = serviceState.mIsManualNetworkSelection;
        this.mRilVoiceRadioTechnology = serviceState.mRilVoiceRadioTechnology;
        this.mRilDataRadioTechnology = serviceState.mRilDataRadioTechnology;
        this.mCssIndicator = serviceState.mCssIndicator;
        this.mNetworkId = serviceState.mNetworkId;
        this.mSystemId = serviceState.mSystemId;
        this.mCdmaRoamingIndicator = serviceState.mCdmaRoamingIndicator;
        this.mCdmaDefaultRoamingIndicator = serviceState.mCdmaDefaultRoamingIndicator;
        this.mCdmaEriIconIndex = serviceState.mCdmaEriIconIndex;
        this.mCdmaEriIconMode = serviceState.mCdmaEriIconMode;
        this.mIsEmergencyOnly = serviceState.mIsEmergencyOnly;
        this.mIsDataRoamingFromRegistration = serviceState.mIsDataRoamingFromRegistration;
        this.mIsUsingCarrierAggregation = serviceState.mIsUsingCarrierAggregation;
        this.mChannelNumber = serviceState.mChannelNumber;
        this.mCellBandwidths = Arrays.copyOf(serviceState.mCellBandwidths, serviceState.mCellBandwidths.length);
        this.mLteEarfcnRsrpBoost = serviceState.mLteEarfcnRsrpBoost;
        this.mNetworkRegistrationStates = new ArrayList(serviceState.mNetworkRegistrationStates);
    }

    public ServiceState(Parcel parcel) {
        boolean z;
        boolean z2;
        boolean z3;
        boolean z4;
        this.mVoiceRegState = 1;
        this.mDataRegState = 1;
        this.mCellBandwidths = new int[0];
        this.mLteEarfcnRsrpBoost = 0;
        this.mNetworkRegistrationStates = new ArrayList();
        this.mVoiceRegState = parcel.readInt();
        this.mDataRegState = parcel.readInt();
        this.mVoiceRoamingType = parcel.readInt();
        this.mDataRoamingType = parcel.readInt();
        this.mVoiceOperatorAlphaLong = parcel.readString();
        this.mVoiceOperatorAlphaShort = parcel.readString();
        this.mVoiceOperatorNumeric = parcel.readString();
        this.mDataOperatorAlphaLong = parcel.readString();
        this.mDataOperatorAlphaShort = parcel.readString();
        this.mDataOperatorNumeric = parcel.readString();
        if (parcel.readInt() != 0) {
            z = true;
        } else {
            z = false;
        }
        this.mIsManualNetworkSelection = z;
        this.mRilVoiceRadioTechnology = parcel.readInt();
        this.mRilDataRadioTechnology = parcel.readInt();
        if (parcel.readInt() != 0) {
            z2 = true;
        } else {
            z2 = false;
        }
        this.mCssIndicator = z2;
        this.mNetworkId = parcel.readInt();
        this.mSystemId = parcel.readInt();
        this.mCdmaRoamingIndicator = parcel.readInt();
        this.mCdmaDefaultRoamingIndicator = parcel.readInt();
        this.mCdmaEriIconIndex = parcel.readInt();
        this.mCdmaEriIconMode = parcel.readInt();
        if (parcel.readInt() != 0) {
            z3 = true;
        } else {
            z3 = false;
        }
        this.mIsEmergencyOnly = z3;
        if (parcel.readInt() != 0) {
            z4 = true;
        } else {
            z4 = false;
        }
        this.mIsDataRoamingFromRegistration = z4;
        this.mIsUsingCarrierAggregation = parcel.readInt() != 0;
        this.mLteEarfcnRsrpBoost = parcel.readInt();
        this.mNetworkRegistrationStates = new ArrayList();
        parcel.readList(this.mNetworkRegistrationStates, NetworkRegistrationState.class.getClassLoader());
        this.mChannelNumber = parcel.readInt();
        this.mCellBandwidths = parcel.createIntArray();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mVoiceRegState);
        parcel.writeInt(this.mDataRegState);
        parcel.writeInt(this.mVoiceRoamingType);
        parcel.writeInt(this.mDataRoamingType);
        parcel.writeString(this.mVoiceOperatorAlphaLong);
        parcel.writeString(this.mVoiceOperatorAlphaShort);
        parcel.writeString(this.mVoiceOperatorNumeric);
        parcel.writeString(this.mDataOperatorAlphaLong);
        parcel.writeString(this.mDataOperatorAlphaShort);
        parcel.writeString(this.mDataOperatorNumeric);
        parcel.writeInt(this.mIsManualNetworkSelection ? 1 : 0);
        parcel.writeInt(this.mRilVoiceRadioTechnology);
        parcel.writeInt(this.mRilDataRadioTechnology);
        parcel.writeInt(this.mCssIndicator ? 1 : 0);
        parcel.writeInt(this.mNetworkId);
        parcel.writeInt(this.mSystemId);
        parcel.writeInt(this.mCdmaRoamingIndicator);
        parcel.writeInt(this.mCdmaDefaultRoamingIndicator);
        parcel.writeInt(this.mCdmaEriIconIndex);
        parcel.writeInt(this.mCdmaEriIconMode);
        parcel.writeInt(this.mIsEmergencyOnly ? 1 : 0);
        parcel.writeInt(this.mIsDataRoamingFromRegistration ? 1 : 0);
        parcel.writeInt(this.mIsUsingCarrierAggregation ? 1 : 0);
        parcel.writeInt(this.mLteEarfcnRsrpBoost);
        parcel.writeList(this.mNetworkRegistrationStates);
        parcel.writeInt(this.mChannelNumber);
        parcel.writeIntArray(this.mCellBandwidths);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public int getState() {
        return getVoiceRegState();
    }

    public int getVoiceRegState() {
        return this.mVoiceRegState;
    }

    public int getDataRegState() {
        return this.mDataRegState;
    }

    public int getDuplexMode() {
        if (!isLte(this.mRilDataRadioTechnology)) {
            return 0;
        }
        return AccessNetworkUtils.getDuplexModeForEutranBand(AccessNetworkUtils.getOperatingBandForEarfcn(this.mChannelNumber));
    }

    public int getChannelNumber() {
        return this.mChannelNumber;
    }

    public int[] getCellBandwidths() {
        return this.mCellBandwidths == null ? new int[0] : this.mCellBandwidths;
    }

    public boolean getRoaming() {
        return getVoiceRoaming() || getDataRoaming();
    }

    public boolean getVoiceRoaming() {
        return this.mVoiceRoamingType != 0;
    }

    public int getVoiceRoamingType() {
        return this.mVoiceRoamingType;
    }

    public boolean getDataRoaming() {
        return this.mDataRoamingType != 0;
    }

    public void setDataRoamingFromRegistration(boolean z) {
        this.mIsDataRoamingFromRegistration = z;
    }

    public boolean getDataRoamingFromRegistration() {
        return this.mIsDataRoamingFromRegistration;
    }

    public int getDataRoamingType() {
        return this.mDataRoamingType;
    }

    public boolean isEmergencyOnly() {
        return this.mIsEmergencyOnly;
    }

    public int getCdmaRoamingIndicator() {
        return this.mCdmaRoamingIndicator;
    }

    public int getCdmaDefaultRoamingIndicator() {
        return this.mCdmaDefaultRoamingIndicator;
    }

    public int getCdmaEriIconIndex() {
        return this.mCdmaEriIconIndex;
    }

    public int getCdmaEriIconMode() {
        return this.mCdmaEriIconMode;
    }

    public String getOperatorAlphaLong() {
        return this.mVoiceOperatorAlphaLong;
    }

    public String getVoiceOperatorAlphaLong() {
        return this.mVoiceOperatorAlphaLong;
    }

    public String getDataOperatorAlphaLong() {
        return this.mDataOperatorAlphaLong;
    }

    public String getOperatorAlphaShort() {
        return this.mVoiceOperatorAlphaShort;
    }

    public String getVoiceOperatorAlphaShort() {
        return this.mVoiceOperatorAlphaShort;
    }

    public String getDataOperatorAlphaShort() {
        return this.mDataOperatorAlphaShort;
    }

    public String getOperatorAlpha() {
        if (TextUtils.isEmpty(this.mVoiceOperatorAlphaLong)) {
            return this.mVoiceOperatorAlphaShort;
        }
        return this.mVoiceOperatorAlphaLong;
    }

    public String getOperatorNumeric() {
        return this.mVoiceOperatorNumeric;
    }

    public String getVoiceOperatorNumeric() {
        return this.mVoiceOperatorNumeric;
    }

    public String getDataOperatorNumeric() {
        return this.mDataOperatorNumeric;
    }

    public boolean getIsManualSelection() {
        return this.mIsManualNetworkSelection;
    }

    public int hashCode() {
        return (this.mVoiceRegState * 31) + (this.mDataRegState * 37) + this.mVoiceRoamingType + this.mDataRoamingType + this.mChannelNumber + Arrays.hashCode(this.mCellBandwidths) + (this.mIsManualNetworkSelection ? 1 : 0) + (this.mVoiceOperatorAlphaLong == null ? 0 : this.mVoiceOperatorAlphaLong.hashCode()) + (this.mVoiceOperatorAlphaShort == null ? 0 : this.mVoiceOperatorAlphaShort.hashCode()) + (this.mVoiceOperatorNumeric == null ? 0 : this.mVoiceOperatorNumeric.hashCode()) + (this.mDataOperatorAlphaLong == null ? 0 : this.mDataOperatorAlphaLong.hashCode()) + (this.mDataOperatorAlphaShort == null ? 0 : this.mDataOperatorAlphaShort.hashCode()) + (this.mDataOperatorNumeric != null ? this.mDataOperatorNumeric.hashCode() : 0) + this.mCdmaRoamingIndicator + this.mCdmaDefaultRoamingIndicator + (this.mIsEmergencyOnly ? 1 : 0) + (this.mIsDataRoamingFromRegistration ? 1 : 0);
    }

    public boolean equals(Object obj) {
        try {
            ServiceState serviceState = (ServiceState) obj;
            return obj != null && this.mVoiceRegState == serviceState.mVoiceRegState && this.mDataRegState == serviceState.mDataRegState && this.mIsManualNetworkSelection == serviceState.mIsManualNetworkSelection && this.mVoiceRoamingType == serviceState.mVoiceRoamingType && this.mDataRoamingType == serviceState.mDataRoamingType && this.mChannelNumber == serviceState.mChannelNumber && Arrays.equals(this.mCellBandwidths, serviceState.mCellBandwidths) && equalsHandlesNulls(this.mVoiceOperatorAlphaLong, serviceState.mVoiceOperatorAlphaLong) && equalsHandlesNulls(this.mVoiceOperatorAlphaShort, serviceState.mVoiceOperatorAlphaShort) && equalsHandlesNulls(this.mVoiceOperatorNumeric, serviceState.mVoiceOperatorNumeric) && equalsHandlesNulls(this.mDataOperatorAlphaLong, serviceState.mDataOperatorAlphaLong) && equalsHandlesNulls(this.mDataOperatorAlphaShort, serviceState.mDataOperatorAlphaShort) && equalsHandlesNulls(this.mDataOperatorNumeric, serviceState.mDataOperatorNumeric) && equalsHandlesNulls(Integer.valueOf(this.mRilVoiceRadioTechnology), Integer.valueOf(serviceState.mRilVoiceRadioTechnology)) && equalsHandlesNulls(Integer.valueOf(this.mRilDataRadioTechnology), Integer.valueOf(serviceState.mRilDataRadioTechnology)) && equalsHandlesNulls(Boolean.valueOf(this.mCssIndicator), Boolean.valueOf(serviceState.mCssIndicator)) && equalsHandlesNulls(Integer.valueOf(this.mNetworkId), Integer.valueOf(serviceState.mNetworkId)) && equalsHandlesNulls(Integer.valueOf(this.mSystemId), Integer.valueOf(serviceState.mSystemId)) && equalsHandlesNulls(Integer.valueOf(this.mCdmaRoamingIndicator), Integer.valueOf(serviceState.mCdmaRoamingIndicator)) && equalsHandlesNulls(Integer.valueOf(this.mCdmaDefaultRoamingIndicator), Integer.valueOf(serviceState.mCdmaDefaultRoamingIndicator)) && this.mIsEmergencyOnly == serviceState.mIsEmergencyOnly && this.mIsDataRoamingFromRegistration == serviceState.mIsDataRoamingFromRegistration && this.mIsUsingCarrierAggregation == serviceState.mIsUsingCarrierAggregation && this.mNetworkRegistrationStates.containsAll(serviceState.mNetworkRegistrationStates);
        } catch (ClassCastException e) {
            return false;
        }
    }

    public static String rilRadioTechnologyToString(int i) {
        switch (i) {
            case 0:
                return "Unknown";
            case 1:
                return "GPRS";
            case 2:
                return "EDGE";
            case 3:
                return "UMTS";
            case 4:
                return "CDMA-IS95A";
            case 5:
                return "CDMA-IS95B";
            case 6:
                return "1xRTT";
            case 7:
                return "EvDo-rev.0";
            case 8:
                return "EvDo-rev.A";
            case 9:
                return "HSDPA";
            case 10:
                return "HSUPA";
            case 11:
                return "HSPA";
            case 12:
                return "EvDo-rev.B";
            case 13:
                return "eHRPD";
            case 14:
                return "LTE";
            case 15:
                return "HSPAP";
            case 16:
                return "GSM";
            case 17:
                return "TD-SCDMA";
            case 18:
                return "IWLAN";
            case 19:
                return "LTE_CA";
            default:
                Rlog.w(LOG_TAG, "Unexpected radioTechnology=" + i);
                return "Unexpected";
        }
    }

    public static String rilServiceStateToString(int i) {
        switch (i) {
            case 0:
                return "IN_SERVICE";
            case 1:
                return "OUT_OF_SERVICE";
            case 2:
                return "EMERGENCY_ONLY";
            case 3:
                return "POWER_OFF";
            default:
                return IccCardConstants.INTENT_VALUE_ICC_UNKNOWN;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{mVoiceRegState=");
        sb.append(this.mVoiceRegState);
        sb.append("(" + rilServiceStateToString(this.mVoiceRegState) + ")");
        sb.append(", mDataRegState=");
        sb.append(this.mDataRegState);
        sb.append("(" + rilServiceStateToString(this.mDataRegState) + ")");
        sb.append(", mChannelNumber=");
        sb.append(this.mChannelNumber);
        sb.append(", duplexMode()=");
        sb.append(getDuplexMode());
        sb.append(", mCellBandwidths=");
        sb.append(Arrays.toString(this.mCellBandwidths));
        sb.append(", mVoiceRoamingType=");
        sb.append(getRoamingLogString(this.mVoiceRoamingType));
        sb.append(", mDataRoamingType=");
        sb.append(getRoamingLogString(this.mDataRoamingType));
        sb.append(", mVoiceOperatorAlphaLong=");
        sb.append(this.mVoiceOperatorAlphaLong);
        sb.append(", mVoiceOperatorAlphaShort=");
        sb.append(this.mVoiceOperatorAlphaShort);
        sb.append(", mDataOperatorAlphaLong=");
        sb.append(this.mDataOperatorAlphaLong);
        sb.append(", mDataOperatorAlphaShort=");
        sb.append(this.mDataOperatorAlphaShort);
        sb.append(", isManualNetworkSelection=");
        sb.append(this.mIsManualNetworkSelection);
        sb.append(this.mIsManualNetworkSelection ? "(manual)" : "(automatic)");
        sb.append(", mRilVoiceRadioTechnology=");
        sb.append(this.mRilVoiceRadioTechnology);
        sb.append("(" + rilRadioTechnologyToString(this.mRilVoiceRadioTechnology) + ")");
        sb.append(", mRilDataRadioTechnology=");
        sb.append(this.mRilDataRadioTechnology);
        sb.append("(" + rilRadioTechnologyToString(this.mRilDataRadioTechnology) + ")");
        sb.append(", mCssIndicator=");
        sb.append(this.mCssIndicator ? "supported" : "unsupported");
        sb.append(", mNetworkId=");
        sb.append(this.mNetworkId);
        sb.append(", mSystemId=");
        sb.append(this.mSystemId);
        sb.append(", mCdmaRoamingIndicator=");
        sb.append(this.mCdmaRoamingIndicator);
        sb.append(", mCdmaDefaultRoamingIndicator=");
        sb.append(this.mCdmaDefaultRoamingIndicator);
        sb.append(", mIsEmergencyOnly=");
        sb.append(this.mIsEmergencyOnly);
        sb.append(", mIsDataRoamingFromRegistration=");
        sb.append(this.mIsDataRoamingFromRegistration);
        sb.append(", mIsUsingCarrierAggregation=");
        sb.append(this.mIsUsingCarrierAggregation);
        sb.append(", mLteEarfcnRsrpBoost=");
        sb.append(this.mLteEarfcnRsrpBoost);
        sb.append(", mNetworkRegistrationStates=");
        sb.append(this.mNetworkRegistrationStates);
        sb.append("}");
        return sb.toString();
    }

    protected void setNullState(int i) {
        this.mVoiceRegState = i;
        this.mDataRegState = i;
        this.mVoiceRoamingType = 0;
        this.mDataRoamingType = 0;
        this.mChannelNumber = -1;
        this.mCellBandwidths = new int[0];
        this.mVoiceOperatorAlphaLong = null;
        this.mVoiceOperatorAlphaShort = null;
        this.mVoiceOperatorNumeric = null;
        this.mDataOperatorAlphaLong = null;
        this.mDataOperatorAlphaShort = null;
        this.mDataOperatorNumeric = null;
        this.mIsManualNetworkSelection = false;
        this.mRilVoiceRadioTechnology = 0;
        this.mRilDataRadioTechnology = 0;
        this.mCssIndicator = false;
        this.mNetworkId = -1;
        this.mSystemId = -1;
        this.mCdmaRoamingIndicator = -1;
        this.mCdmaDefaultRoamingIndicator = -1;
        this.mCdmaEriIconIndex = -1;
        this.mCdmaEriIconMode = -1;
        this.mIsEmergencyOnly = false;
        this.mIsDataRoamingFromRegistration = false;
        this.mIsUsingCarrierAggregation = false;
        this.mLteEarfcnRsrpBoost = 0;
        this.mNetworkRegistrationStates = new ArrayList();
    }

    public void setStateOutOfService() {
        setNullState(1);
    }

    public void setStateOff() {
        setNullState(3);
    }

    public void setState(int i) {
        setVoiceRegState(i);
    }

    public void setVoiceRegState(int i) {
        this.mVoiceRegState = i;
    }

    public void setDataRegState(int i) {
        this.mDataRegState = i;
    }

    public void setCellBandwidths(int[] iArr) {
        this.mCellBandwidths = iArr;
    }

    public void setChannelNumber(int i) {
        this.mChannelNumber = i;
    }

    public void setRoaming(boolean z) {
        this.mVoiceRoamingType = z ? 1 : 0;
        this.mDataRoamingType = this.mVoiceRoamingType;
    }

    public void setVoiceRoaming(boolean z) {
        this.mVoiceRoamingType = z ? 1 : 0;
    }

    public void setVoiceRoamingType(int i) {
        this.mVoiceRoamingType = i;
    }

    public void setDataRoaming(boolean z) {
        this.mDataRoamingType = z ? 1 : 0;
    }

    public void setDataRoamingType(int i) {
        this.mDataRoamingType = i;
    }

    public void setEmergencyOnly(boolean z) {
        this.mIsEmergencyOnly = z;
    }

    public void setCdmaRoamingIndicator(int i) {
        this.mCdmaRoamingIndicator = i;
    }

    public void setCdmaDefaultRoamingIndicator(int i) {
        this.mCdmaDefaultRoamingIndicator = i;
    }

    public void setCdmaEriIconIndex(int i) {
        this.mCdmaEriIconIndex = i;
    }

    public void setCdmaEriIconMode(int i) {
        this.mCdmaEriIconMode = i;
    }

    public void setOperatorName(String str, String str2, String str3) {
        this.mVoiceOperatorAlphaLong = str;
        this.mVoiceOperatorAlphaShort = str2;
        this.mVoiceOperatorNumeric = str3;
        this.mDataOperatorAlphaLong = str;
        this.mDataOperatorAlphaShort = str2;
        this.mDataOperatorNumeric = str3;
    }

    public void setVoiceOperatorName(String str, String str2, String str3) {
        this.mVoiceOperatorAlphaLong = str;
        this.mVoiceOperatorAlphaShort = str2;
        this.mVoiceOperatorNumeric = str3;
    }

    public void setDataOperatorName(String str, String str2, String str3) {
        this.mDataOperatorAlphaLong = str;
        this.mDataOperatorAlphaShort = str2;
        this.mDataOperatorNumeric = str3;
    }

    public void setOperatorAlphaLong(String str) {
        this.mVoiceOperatorAlphaLong = str;
        this.mDataOperatorAlphaLong = str;
    }

    public void setVoiceOperatorAlphaLong(String str) {
        this.mVoiceOperatorAlphaLong = str;
    }

    public void setDataOperatorAlphaLong(String str) {
        this.mDataOperatorAlphaLong = str;
    }

    public void setIsManualSelection(boolean z) {
        this.mIsManualNetworkSelection = z;
    }

    protected static boolean equalsHandlesNulls(Object obj, Object obj2) {
        return obj == null ? obj2 == null : obj.equals(obj2);
    }

    protected void setFromNotifierBundle(Bundle bundle) {
        this.mVoiceRegState = bundle.getInt(Intent.EXTRA_VOICE_REG_STATE);
        this.mDataRegState = bundle.getInt(Intent.EXTRA_DATA_REG_STATE);
        this.mVoiceRoamingType = bundle.getInt(Intent.EXTRA_VOICE_ROAMING_TYPE);
        this.mDataRoamingType = bundle.getInt(Intent.EXTRA_DATA_ROAMING_TYPE);
        this.mVoiceOperatorAlphaLong = bundle.getString(Intent.EXTRA_OPERATOR_ALPHA_LONG);
        this.mVoiceOperatorAlphaShort = bundle.getString(Intent.EXTRA_OPERATOR_ALPHA_SHORT);
        this.mVoiceOperatorNumeric = bundle.getString(Intent.EXTRA_OPERATOR_NUMERIC);
        this.mDataOperatorAlphaLong = bundle.getString(Intent.EXTRA_DATA_OPERATOR_ALPHA_LONG);
        this.mDataOperatorAlphaShort = bundle.getString(Intent.EXTRA_DATA_OPERATOR_ALPHA_SHORT);
        this.mDataOperatorNumeric = bundle.getString(Intent.EXTRA_DATA_OPERATOR_NUMERIC);
        this.mIsManualNetworkSelection = bundle.getBoolean(Intent.EXTRA_MANUAL);
        this.mRilVoiceRadioTechnology = bundle.getInt(Intent.EXTRA_VOICE_RADIO_TECH);
        this.mRilDataRadioTechnology = bundle.getInt(Intent.EXTRA_DATA_RADIO_TECH);
        this.mCssIndicator = bundle.getBoolean(Intent.EXTRA_CSS_INDICATOR);
        this.mNetworkId = bundle.getInt(Intent.EXTRA_NETWORK_ID);
        this.mSystemId = bundle.getInt(Intent.EXTRA_SYSTEM_ID);
        this.mCdmaRoamingIndicator = bundle.getInt(Intent.EXTRA_CDMA_ROAMING_INDICATOR);
        this.mCdmaDefaultRoamingIndicator = bundle.getInt(Intent.EXTRA_CDMA_DEFAULT_ROAMING_INDICATOR);
        this.mIsEmergencyOnly = bundle.getBoolean(Intent.EXTRA_EMERGENCY_ONLY);
        this.mIsDataRoamingFromRegistration = bundle.getBoolean(Intent.EXTRA_IS_DATA_ROAMING_FROM_REGISTRATION);
        this.mIsUsingCarrierAggregation = bundle.getBoolean(Intent.EXTRA_IS_USING_CARRIER_AGGREGATION);
        this.mLteEarfcnRsrpBoost = bundle.getInt(Intent.EXTRA_LTE_EARFCN_RSRP_BOOST);
        this.mChannelNumber = bundle.getInt("ChannelNumber");
        this.mCellBandwidths = bundle.getIntArray("CellBandwidths");
    }

    public void fillInNotifierBundle(Bundle bundle) {
        bundle.putInt(Intent.EXTRA_VOICE_REG_STATE, this.mVoiceRegState);
        bundle.putInt(Intent.EXTRA_DATA_REG_STATE, this.mDataRegState);
        bundle.putInt(Intent.EXTRA_VOICE_ROAMING_TYPE, this.mVoiceRoamingType);
        bundle.putInt(Intent.EXTRA_DATA_ROAMING_TYPE, this.mDataRoamingType);
        bundle.putString(Intent.EXTRA_OPERATOR_ALPHA_LONG, this.mVoiceOperatorAlphaLong);
        bundle.putString(Intent.EXTRA_OPERATOR_ALPHA_SHORT, this.mVoiceOperatorAlphaShort);
        bundle.putString(Intent.EXTRA_OPERATOR_NUMERIC, this.mVoiceOperatorNumeric);
        bundle.putString(Intent.EXTRA_DATA_OPERATOR_ALPHA_LONG, this.mDataOperatorAlphaLong);
        bundle.putString(Intent.EXTRA_DATA_OPERATOR_ALPHA_SHORT, this.mDataOperatorAlphaShort);
        bundle.putString(Intent.EXTRA_DATA_OPERATOR_NUMERIC, this.mDataOperatorNumeric);
        bundle.putBoolean(Intent.EXTRA_MANUAL, this.mIsManualNetworkSelection);
        bundle.putInt(Intent.EXTRA_VOICE_RADIO_TECH, this.mRilVoiceRadioTechnology);
        bundle.putInt(Intent.EXTRA_DATA_RADIO_TECH, this.mRilDataRadioTechnology);
        bundle.putBoolean(Intent.EXTRA_CSS_INDICATOR, this.mCssIndicator);
        bundle.putInt(Intent.EXTRA_NETWORK_ID, this.mNetworkId);
        bundle.putInt(Intent.EXTRA_SYSTEM_ID, this.mSystemId);
        bundle.putInt(Intent.EXTRA_CDMA_ROAMING_INDICATOR, this.mCdmaRoamingIndicator);
        bundle.putInt(Intent.EXTRA_CDMA_DEFAULT_ROAMING_INDICATOR, this.mCdmaDefaultRoamingIndicator);
        bundle.putBoolean(Intent.EXTRA_EMERGENCY_ONLY, this.mIsEmergencyOnly);
        bundle.putBoolean(Intent.EXTRA_IS_DATA_ROAMING_FROM_REGISTRATION, this.mIsDataRoamingFromRegistration);
        bundle.putBoolean(Intent.EXTRA_IS_USING_CARRIER_AGGREGATION, this.mIsUsingCarrierAggregation);
        bundle.putInt(Intent.EXTRA_LTE_EARFCN_RSRP_BOOST, this.mLteEarfcnRsrpBoost);
        bundle.putInt("ChannelNumber", this.mChannelNumber);
        bundle.putIntArray("CellBandwidths", this.mCellBandwidths);
    }

    public void setRilVoiceRadioTechnology(int i) {
        if (i == 19) {
            i = 14;
        }
        this.mRilVoiceRadioTechnology = i;
    }

    public void setRilDataRadioTechnology(int i) {
        if (i == 19) {
            i = 14;
            this.mIsUsingCarrierAggregation = true;
        } else {
            this.mIsUsingCarrierAggregation = false;
        }
        this.mRilDataRadioTechnology = i;
    }

    public boolean isUsingCarrierAggregation() {
        return this.mIsUsingCarrierAggregation;
    }

    public void setIsUsingCarrierAggregation(boolean z) {
        this.mIsUsingCarrierAggregation = z;
    }

    public int getLteEarfcnRsrpBoost() {
        return this.mLteEarfcnRsrpBoost;
    }

    public void setLteEarfcnRsrpBoost(int i) {
        this.mLteEarfcnRsrpBoost = i;
    }

    public void setCssIndicator(int i) {
        this.mCssIndicator = i != 0;
    }

    public void setCdmaSystemAndNetworkId(int i, int i2) {
        this.mSystemId = i;
        this.mNetworkId = i2;
    }

    public int getRilVoiceRadioTechnology() {
        return this.mRilVoiceRadioTechnology;
    }

    public int getRilDataRadioTechnology() {
        return this.mRilDataRadioTechnology;
    }

    public int getRadioTechnology() {
        Rlog.e(LOG_TAG, "ServiceState.getRadioTechnology() DEPRECATED will be removed *******");
        return getRilDataRadioTechnology();
    }

    public static int rilRadioTechnologyToNetworkType(int i) {
        switch (i) {
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
            case 5:
                return 4;
            case 6:
                return 7;
            case 7:
                return 5;
            case 8:
                return 6;
            case 9:
                return 8;
            case 10:
                return 9;
            case 11:
                return 10;
            case 12:
                return 12;
            case 13:
                return 14;
            case 14:
                return 13;
            case 15:
                return 15;
            case 16:
                return 16;
            case 17:
                return 17;
            case 18:
                return 18;
            case 19:
                return 19;
            default:
                return 0;
        }
    }

    public static int rilRadioTechnologyToAccessNetworkType(int i) {
        switch (i) {
            case 1:
            case 2:
            case 16:
                return 1;
            case 3:
            case 9:
            case 10:
            case 11:
            case 15:
            case 17:
                return 2;
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 12:
            case 13:
                return 4;
            case 14:
            case 19:
                return 3;
            case 18:
                return 5;
            default:
                return 0;
        }
    }

    public static int networkTypeToRilRadioTechnology(int i) {
        switch (i) {
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 7;
            case 6:
                return 8;
            case 7:
                return 6;
            case 8:
                return 9;
            case 9:
                return 10;
            case 10:
                return 11;
            case 11:
            default:
                return 0;
            case 12:
                return 12;
            case 13:
                return 14;
            case 14:
                return 13;
            case 15:
                return 15;
            case 16:
                return 16;
            case 17:
                return 17;
            case 18:
                return 18;
            case 19:
                return 19;
        }
    }

    public int getDataNetworkType() {
        return rilRadioTechnologyToNetworkType(this.mRilDataRadioTechnology);
    }

    public int getVoiceNetworkType() {
        return rilRadioTechnologyToNetworkType(this.mRilVoiceRadioTechnology);
    }

    public int getCssIndicator() {
        return this.mCssIndicator ? 1 : 0;
    }

    public int getCdmaNetworkId() {
        return this.mNetworkId;
    }

    public int getCdmaSystemId() {
        return this.mSystemId;
    }

    public static boolean isGsm(int i) {
        return i == 1 || i == 2 || i == 3 || i == 9 || i == 10 || i == 11 || i == 14 || i == 15 || i == 16 || i == 17 || i == 18 || i == 19;
    }

    public static boolean isCdma(int i) {
        return i == 4 || i == 5 || i == 6 || i == 7 || i == 8 || i == 12 || i == 13;
    }

    public static boolean isLte(int i) {
        return i == 14 || i == 19;
    }

    public static boolean bearerBitmapHasCdma(int i) {
        return (i & RIL_RADIO_CDMA_TECHNOLOGY_BITMASK) != 0;
    }

    public static boolean bitmaskHasTech(int i, int i2) {
        if (i == 0) {
            return true;
        }
        if (i2 >= 1 && (i & (1 << (i2 - 1))) != 0) {
            return true;
        }
        return false;
    }

    public static int getBitmaskForTech(int i) {
        if (i >= 1) {
            return 1 << (i - 1);
        }
        return 0;
    }

    public static int getBitmaskFromString(String str) {
        int bitmaskForTech = 0;
        for (String str2 : str.split("\\|")) {
            try {
                int i = Integer.parseInt(str2.trim());
                if (i == 0) {
                    return 0;
                }
                bitmaskForTech |= getBitmaskForTech(i);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return bitmaskForTech;
    }

    public static int convertNetworkTypeBitmaskToBearerBitmask(int i) {
        if (i == 0) {
            return 0;
        }
        int bitmaskForTech = 0;
        for (int i2 = 0; i2 < 20; i2++) {
            if (bitmaskHasTech(i, rilRadioTechnologyToNetworkType(i2))) {
                bitmaskForTech |= getBitmaskForTech(i2);
            }
        }
        return bitmaskForTech;
    }

    public static int convertBearerBitmaskToNetworkTypeBitmask(int i) {
        if (i == 0) {
            return 0;
        }
        int bitmaskForTech = 0;
        for (int i2 = 0; i2 < 20; i2++) {
            if (bitmaskHasTech(i, i2)) {
                bitmaskForTech |= getBitmaskForTech(rilRadioTechnologyToNetworkType(i2));
            }
        }
        return bitmaskForTech;
    }

    public static ServiceState mergeServiceStates(ServiceState serviceState, ServiceState serviceState2) {
        if (serviceState2.mVoiceRegState != 0) {
            return serviceState;
        }
        ServiceState serviceState3 = new ServiceState(serviceState);
        serviceState3.mVoiceRegState = serviceState2.mVoiceRegState;
        serviceState3.mIsEmergencyOnly = false;
        return serviceState3;
    }

    public List<NetworkRegistrationState> getNetworkRegistrationStates() {
        ArrayList arrayList;
        synchronized (this.mNetworkRegistrationStates) {
            arrayList = new ArrayList(this.mNetworkRegistrationStates);
        }
        return arrayList;
    }

    public List<NetworkRegistrationState> getNetworkRegistrationStates(int i) {
        ArrayList arrayList = new ArrayList();
        synchronized (this.mNetworkRegistrationStates) {
            for (NetworkRegistrationState networkRegistrationState : this.mNetworkRegistrationStates) {
                if (networkRegistrationState.getTransportType() == i) {
                    arrayList.add(networkRegistrationState);
                }
            }
        }
        return arrayList;
    }

    public NetworkRegistrationState getNetworkRegistrationStates(int i, int i2) {
        synchronized (this.mNetworkRegistrationStates) {
            for (NetworkRegistrationState networkRegistrationState : this.mNetworkRegistrationStates) {
                if (networkRegistrationState.getTransportType() == i && networkRegistrationState.getDomain() == i2) {
                    return networkRegistrationState;
                }
            }
            return null;
        }
    }

    public void addNetworkRegistrationState(NetworkRegistrationState networkRegistrationState) {
        if (networkRegistrationState == null) {
            return;
        }
        synchronized (this.mNetworkRegistrationStates) {
            int i = 0;
            while (true) {
                if (i >= this.mNetworkRegistrationStates.size()) {
                    break;
                }
                NetworkRegistrationState networkRegistrationState2 = this.mNetworkRegistrationStates.get(i);
                if (networkRegistrationState2.getTransportType() != networkRegistrationState.getTransportType() || networkRegistrationState2.getDomain() != networkRegistrationState.getDomain()) {
                    i++;
                } else {
                    this.mNetworkRegistrationStates.remove(i);
                    break;
                }
            }
            this.mNetworkRegistrationStates.add(networkRegistrationState);
        }
    }

    private static ServiceState makeServiceState(Parcel parcel) {
        try {
            Constructor<?> constructor = Class.forName("mediatek.telephony.MtkServiceState").getConstructor(Parcel.class);
            constructor.setAccessible(true);
            return (ServiceState) constructor.newInstance(parcel);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Rlog.e(LOG_TAG, "MtkServiceState IllegalAccessException! Used AOSP instead!");
            return new ServiceState(parcel);
        } catch (InstantiationException e2) {
            e2.printStackTrace();
            Rlog.e(LOG_TAG, "MtkServiceState InstantiationException! Used AOSP instead!");
            return new ServiceState(parcel);
        } catch (InvocationTargetException e3) {
            e3.printStackTrace();
            Rlog.e(LOG_TAG, "MtkServiceState InvocationTargetException! Used AOSP instead!");
            return new ServiceState(parcel);
        } catch (Exception e4) {
            Rlog.e(LOG_TAG, "No MtkServiceState! Used AOSP instead!");
            return new ServiceState(parcel);
        }
    }
}
