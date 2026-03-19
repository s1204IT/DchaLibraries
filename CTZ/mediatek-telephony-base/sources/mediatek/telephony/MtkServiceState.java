package mediatek.telephony;

import android.os.Bundle;
import android.os.Parcel;
import android.os.SystemProperties;
import android.telephony.NetworkRegistrationState;
import android.telephony.ServiceState;
import java.util.ArrayList;
import java.util.Arrays;

public class MtkServiceState extends ServiceState {
    static final boolean DBG = false;
    static final String LOG_TAG = "MTKSS";
    public static final int REGISTRATION_STATE_NOT_REGISTERED_AND_NOT_SEARCHING_EMERGENCY_CALL_ENABLED = 10;
    public static final int REGISTRATION_STATE_NOT_REGISTERED_AND_SEARCHING_EMERGENCY_CALL_ENABLED = 12;
    public static final int REGISTRATION_STATE_REGISTRATION_DENIED_EMERGENCY_CALL_ENABLED = 13;
    public static final int REGISTRATION_STATE_UNKNOWN_EMERGENCY_CALL_ENABLED = 14;
    public static final int RIL_RADIO_TECHNOLOGY_DC_DPA = 133;
    public static final int RIL_RADIO_TECHNOLOGY_DC_HSDPAP = 135;
    public static final int RIL_RADIO_TECHNOLOGY_DC_HSDPAP_DPA = 137;
    public static final int RIL_RADIO_TECHNOLOGY_DC_HSDPAP_UPA = 136;
    public static final int RIL_RADIO_TECHNOLOGY_DC_HSPAP = 138;
    public static final int RIL_RADIO_TECHNOLOGY_DC_UPA = 134;
    public static final int RIL_RADIO_TECHNOLOGY_HSDPAP = 129;
    public static final int RIL_RADIO_TECHNOLOGY_HSDPAP_UPA = 130;
    public static final int RIL_RADIO_TECHNOLOGY_HSUPAP = 131;
    public static final int RIL_RADIO_TECHNOLOGY_HSUPAP_DPA = 132;
    public static final int RIL_RADIO_TECHNOLOGY_MTK = 128;
    private int mCellularDataRegState;
    private int mCellularDataRoamingType;
    private int mCellularVoiceRegState;
    private int mDataRejectCause;
    private boolean mIsUsingCellularCarrierAggregation;
    private int mIwlanRegState;
    private int mProprietaryDataRadioTechnology;
    private int mRilCellularDataRadioTechnology;
    private int mRilCellularDataRegState;
    private int mRilDataRegState;
    private int mRilVoiceRegState;
    private int mVoiceRejectCause;

    public static ServiceState newFromBundle(Bundle bundle) {
        MtkServiceState mtkServiceState = new MtkServiceState();
        mtkServiceState.setFromNotifierBundle(bundle);
        return mtkServiceState;
    }

    public MtkServiceState() {
        this.mRilVoiceRegState = 0;
        this.mRilDataRegState = 0;
        this.mVoiceRejectCause = -1;
        this.mDataRejectCause = -1;
        this.mIwlanRegState = 1;
        this.mCellularVoiceRegState = 1;
        this.mCellularDataRegState = 1;
        this.mRilCellularDataRegState = 0;
        this.mCellularDataRoamingType = 0;
        this.mRilCellularDataRadioTechnology = 0;
        this.mIsUsingCellularCarrierAggregation = false;
        setStateOutOfService();
    }

    public MtkServiceState(MtkServiceState mtkServiceState) {
        this.mRilVoiceRegState = 0;
        this.mRilDataRegState = 0;
        this.mVoiceRejectCause = -1;
        this.mDataRejectCause = -1;
        this.mIwlanRegState = 1;
        this.mCellularVoiceRegState = 1;
        this.mCellularDataRegState = 1;
        this.mRilCellularDataRegState = 0;
        this.mCellularDataRoamingType = 0;
        this.mRilCellularDataRadioTechnology = 0;
        this.mIsUsingCellularCarrierAggregation = false;
        copyFrom(mtkServiceState);
    }

    public MtkServiceState(ServiceState serviceState) {
        this.mRilVoiceRegState = 0;
        this.mRilDataRegState = 0;
        this.mVoiceRejectCause = -1;
        this.mDataRejectCause = -1;
        this.mIwlanRegState = 1;
        this.mCellularVoiceRegState = 1;
        this.mCellularDataRegState = 1;
        this.mRilCellularDataRegState = 0;
        this.mCellularDataRoamingType = 0;
        this.mRilCellularDataRadioTechnology = 0;
        this.mIsUsingCellularCarrierAggregation = false;
        copyFrom((MtkServiceState) serviceState);
    }

    protected void copyFrom(MtkServiceState mtkServiceState) {
        this.mVoiceRegState = mtkServiceState.mVoiceRegState;
        this.mDataRegState = mtkServiceState.mDataRegState;
        this.mVoiceRoamingType = mtkServiceState.mVoiceRoamingType;
        this.mDataRoamingType = mtkServiceState.mDataRoamingType;
        this.mVoiceOperatorAlphaLong = mtkServiceState.mVoiceOperatorAlphaLong;
        this.mVoiceOperatorAlphaShort = mtkServiceState.mVoiceOperatorAlphaShort;
        this.mVoiceOperatorNumeric = mtkServiceState.mVoiceOperatorNumeric;
        this.mDataOperatorAlphaLong = mtkServiceState.mDataOperatorAlphaLong;
        this.mDataOperatorAlphaShort = mtkServiceState.mDataOperatorAlphaShort;
        this.mDataOperatorNumeric = mtkServiceState.mDataOperatorNumeric;
        this.mIsManualNetworkSelection = mtkServiceState.mIsManualNetworkSelection;
        this.mRilVoiceRadioTechnology = mtkServiceState.mRilVoiceRadioTechnology;
        this.mRilDataRadioTechnology = mtkServiceState.mRilDataRadioTechnology;
        this.mCssIndicator = mtkServiceState.mCssIndicator;
        this.mNetworkId = mtkServiceState.mNetworkId;
        this.mSystemId = mtkServiceState.mSystemId;
        this.mCdmaRoamingIndicator = mtkServiceState.mCdmaRoamingIndicator;
        this.mCdmaDefaultRoamingIndicator = mtkServiceState.mCdmaDefaultRoamingIndicator;
        this.mCdmaEriIconIndex = mtkServiceState.mCdmaEriIconIndex;
        this.mCdmaEriIconMode = mtkServiceState.mCdmaEriIconMode;
        this.mIsEmergencyOnly = mtkServiceState.mIsEmergencyOnly;
        this.mIsDataRoamingFromRegistration = mtkServiceState.mIsDataRoamingFromRegistration;
        this.mIsUsingCarrierAggregation = mtkServiceState.mIsUsingCarrierAggregation;
        this.mChannelNumber = mtkServiceState.mChannelNumber;
        this.mCellBandwidths = Arrays.copyOf(mtkServiceState.mCellBandwidths, mtkServiceState.mCellBandwidths.length);
        this.mLteEarfcnRsrpBoost = mtkServiceState.mLteEarfcnRsrpBoost;
        this.mNetworkRegistrationStates = new ArrayList(mtkServiceState.mNetworkRegistrationStates);
        this.mRilVoiceRegState = mtkServiceState.mRilVoiceRegState;
        this.mRilDataRegState = mtkServiceState.mRilDataRegState;
        this.mProprietaryDataRadioTechnology = mtkServiceState.mProprietaryDataRadioTechnology;
        this.mVoiceRejectCause = mtkServiceState.mVoiceRejectCause;
        this.mDataRejectCause = mtkServiceState.mDataRejectCause;
        this.mIwlanRegState = mtkServiceState.mIwlanRegState;
        this.mCellularVoiceRegState = mtkServiceState.mCellularVoiceRegState;
        this.mCellularDataRegState = mtkServiceState.mCellularDataRegState;
        this.mRilCellularDataRegState = mtkServiceState.mRilCellularDataRegState;
        this.mCellularDataRoamingType = mtkServiceState.mCellularDataRoamingType;
        this.mRilCellularDataRadioTechnology = mtkServiceState.mRilCellularDataRadioTechnology;
        this.mIsUsingCellularCarrierAggregation = mtkServiceState.mIsUsingCellularCarrierAggregation;
    }

    public MtkServiceState(Parcel parcel) {
        boolean z;
        boolean z2;
        boolean z3;
        boolean z4;
        boolean z5;
        this.mRilVoiceRegState = 0;
        this.mRilDataRegState = 0;
        this.mVoiceRejectCause = -1;
        this.mDataRejectCause = -1;
        this.mIwlanRegState = 1;
        this.mCellularVoiceRegState = 1;
        this.mCellularDataRegState = 1;
        this.mRilCellularDataRegState = 0;
        this.mCellularDataRoamingType = 0;
        this.mRilCellularDataRadioTechnology = 0;
        this.mIsUsingCellularCarrierAggregation = false;
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
        if (parcel.readInt() == 0) {
            z = false;
        } else {
            z = true;
        }
        this.mIsManualNetworkSelection = z;
        this.mRilVoiceRadioTechnology = parcel.readInt();
        this.mRilDataRadioTechnology = parcel.readInt();
        if (parcel.readInt() == 0) {
            z2 = false;
        } else {
            z2 = true;
        }
        this.mCssIndicator = z2;
        this.mNetworkId = parcel.readInt();
        this.mSystemId = parcel.readInt();
        this.mCdmaRoamingIndicator = parcel.readInt();
        this.mCdmaDefaultRoamingIndicator = parcel.readInt();
        this.mCdmaEriIconIndex = parcel.readInt();
        this.mCdmaEriIconMode = parcel.readInt();
        if (parcel.readInt() == 0) {
            z3 = false;
        } else {
            z3 = true;
        }
        this.mIsEmergencyOnly = z3;
        if (parcel.readInt() == 0) {
            z4 = false;
        } else {
            z4 = true;
        }
        this.mIsDataRoamingFromRegistration = z4;
        if (parcel.readInt() == 0) {
            z5 = false;
        } else {
            z5 = true;
        }
        this.mIsUsingCarrierAggregation = z5;
        this.mLteEarfcnRsrpBoost = parcel.readInt();
        this.mNetworkRegistrationStates = new ArrayList();
        parcel.readList(this.mNetworkRegistrationStates, NetworkRegistrationState.class.getClassLoader());
        this.mChannelNumber = parcel.readInt();
        this.mCellBandwidths = parcel.createIntArray();
        this.mRilVoiceRegState = parcel.readInt();
        this.mRilDataRegState = parcel.readInt();
        this.mProprietaryDataRadioTechnology = parcel.readInt();
        this.mVoiceRejectCause = parcel.readInt();
        this.mDataRejectCause = parcel.readInt();
        this.mIwlanRegState = parcel.readInt();
        this.mCellularVoiceRegState = parcel.readInt();
        this.mCellularDataRegState = parcel.readInt();
        this.mRilCellularDataRegState = parcel.readInt();
        this.mCellularDataRoamingType = parcel.readInt();
        this.mRilCellularDataRadioTechnology = parcel.readInt();
        this.mIsUsingCellularCarrierAggregation = parcel.readInt() != 0;
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
        parcel.writeInt(this.mRilVoiceRegState);
        parcel.writeInt(this.mRilDataRegState);
        parcel.writeInt(this.mProprietaryDataRadioTechnology);
        parcel.writeInt(this.mVoiceRejectCause);
        parcel.writeInt(this.mDataRejectCause);
        parcel.writeInt(this.mIwlanRegState);
        parcel.writeInt(this.mCellularVoiceRegState);
        parcel.writeInt(this.mCellularDataRegState);
        parcel.writeInt(this.mRilCellularDataRegState);
        parcel.writeInt(this.mCellularDataRoamingType);
        parcel.writeInt(this.mRilCellularDataRadioTechnology);
        parcel.writeInt(this.mIsUsingCellularCarrierAggregation ? 1 : 0);
    }

    @Override
    public boolean equals(Object obj) {
        try {
            MtkServiceState mtkServiceState = (MtkServiceState) obj;
            return obj != null && this.mVoiceRegState == mtkServiceState.mVoiceRegState && this.mDataRegState == mtkServiceState.mDataRegState && this.mIsManualNetworkSelection == mtkServiceState.mIsManualNetworkSelection && this.mVoiceRoamingType == mtkServiceState.mVoiceRoamingType && this.mDataRoamingType == mtkServiceState.mDataRoamingType && this.mChannelNumber == mtkServiceState.mChannelNumber && Arrays.equals(this.mCellBandwidths, mtkServiceState.mCellBandwidths) && equalsHandlesNulls(this.mVoiceOperatorAlphaLong, mtkServiceState.mVoiceOperatorAlphaLong) && equalsHandlesNulls(this.mVoiceOperatorAlphaShort, mtkServiceState.mVoiceOperatorAlphaShort) && equalsHandlesNulls(this.mVoiceOperatorNumeric, mtkServiceState.mVoiceOperatorNumeric) && equalsHandlesNulls(this.mDataOperatorAlphaLong, mtkServiceState.mDataOperatorAlphaLong) && equalsHandlesNulls(this.mDataOperatorAlphaShort, mtkServiceState.mDataOperatorAlphaShort) && equalsHandlesNulls(this.mDataOperatorNumeric, mtkServiceState.mDataOperatorNumeric) && equalsHandlesNulls(Integer.valueOf(this.mRilVoiceRadioTechnology), Integer.valueOf(mtkServiceState.mRilVoiceRadioTechnology)) && equalsHandlesNulls(Integer.valueOf(this.mRilDataRadioTechnology), Integer.valueOf(mtkServiceState.mRilDataRadioTechnology)) && equalsHandlesNulls(Boolean.valueOf(this.mCssIndicator), Boolean.valueOf(mtkServiceState.mCssIndicator)) && equalsHandlesNulls(Integer.valueOf(this.mNetworkId), Integer.valueOf(mtkServiceState.mNetworkId)) && equalsHandlesNulls(Integer.valueOf(this.mSystemId), Integer.valueOf(mtkServiceState.mSystemId)) && equalsHandlesNulls(Integer.valueOf(this.mCdmaRoamingIndicator), Integer.valueOf(mtkServiceState.mCdmaRoamingIndicator)) && equalsHandlesNulls(Integer.valueOf(this.mCdmaDefaultRoamingIndicator), Integer.valueOf(mtkServiceState.mCdmaDefaultRoamingIndicator)) && this.mIsEmergencyOnly == mtkServiceState.mIsEmergencyOnly && this.mIsDataRoamingFromRegistration == mtkServiceState.mIsDataRoamingFromRegistration && this.mIsUsingCarrierAggregation == mtkServiceState.mIsUsingCarrierAggregation && this.mNetworkRegistrationStates.containsAll(mtkServiceState.mNetworkRegistrationStates) && this.mRilVoiceRegState == mtkServiceState.mRilVoiceRegState && this.mRilDataRegState == mtkServiceState.mRilDataRegState && equalsHandlesNulls(Integer.valueOf(this.mProprietaryDataRadioTechnology), Integer.valueOf(mtkServiceState.mProprietaryDataRadioTechnology)) && this.mVoiceRejectCause == mtkServiceState.mVoiceRejectCause && this.mDataRejectCause == mtkServiceState.mDataRejectCause && this.mIwlanRegState == mtkServiceState.mIwlanRegState && this.mCellularVoiceRegState == this.mCellularVoiceRegState && this.mCellularDataRegState == mtkServiceState.mCellularDataRegState && this.mRilCellularDataRegState == mtkServiceState.mRilCellularDataRegState && this.mCellularDataRoamingType == mtkServiceState.mCellularDataRoamingType && this.mRilCellularDataRadioTechnology == mtkServiceState.mRilCellularDataRadioTechnology && this.mIsUsingCellularCarrierAggregation == mtkServiceState.mIsUsingCellularCarrierAggregation;
        } catch (ClassCastException e) {
            return false;
        }
    }

    @Override
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
        sb.append(", Ril Voice Regist state=");
        sb.append(this.mRilVoiceRegState);
        sb.append(", Ril Data Regist state=");
        sb.append(this.mRilDataRegState);
        sb.append(", mProprietaryDataRadioTechnology=");
        sb.append(this.mProprietaryDataRadioTechnology);
        sb.append(", VoiceRejectCause=");
        sb.append(this.mVoiceRejectCause);
        sb.append(", DataRejectCause=");
        sb.append(this.mDataRejectCause);
        sb.append(", IwlanRegState=");
        sb.append(this.mIwlanRegState);
        sb.append(", CellularVoiceRegState=");
        sb.append(this.mCellularVoiceRegState);
        sb.append(", CellularDataRegState=");
        sb.append(this.mCellularDataRegState);
        sb.append(", RilCellularDataRegState=");
        sb.append(this.mRilCellularDataRegState);
        sb.append(", CellularDataRoamingType=");
        sb.append(this.mCellularDataRoamingType);
        sb.append(", RilCellularDataRadioTechnology=");
        sb.append(this.mRilCellularDataRadioTechnology);
        sb.append(", IsUsingCellularCarrierAggregation=");
        sb.append(this.mIsUsingCellularCarrierAggregation);
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
        this.mRilVoiceRegState = 0;
        this.mRilDataRegState = 0;
        this.mProprietaryDataRadioTechnology = 0;
        this.mVoiceRejectCause = -1;
        this.mDataRejectCause = -1;
        this.mIwlanRegState = 1;
        this.mCellularVoiceRegState = i;
        this.mCellularDataRegState = i;
        this.mRilCellularDataRegState = 0;
        this.mCellularDataRoamingType = 0;
        this.mRilCellularDataRadioTechnology = 0;
        this.mIsUsingCellularCarrierAggregation = false;
    }

    protected void setFromNotifierBundle(Bundle bundle) {
        this.mVoiceRegState = bundle.getInt("voiceRegState");
        this.mDataRegState = bundle.getInt("dataRegState");
        this.mVoiceRoamingType = bundle.getInt("voiceRoamingType");
        this.mDataRoamingType = bundle.getInt("dataRoamingType");
        this.mVoiceOperatorAlphaLong = bundle.getString("operator-alpha-long");
        this.mVoiceOperatorAlphaShort = bundle.getString("operator-alpha-short");
        this.mVoiceOperatorNumeric = bundle.getString("operator-numeric");
        this.mDataOperatorAlphaLong = bundle.getString("data-operator-alpha-long");
        this.mDataOperatorAlphaShort = bundle.getString("data-operator-alpha-short");
        this.mDataOperatorNumeric = bundle.getString("data-operator-numeric");
        this.mIsManualNetworkSelection = bundle.getBoolean("manual");
        this.mRilVoiceRadioTechnology = bundle.getInt("radioTechnology");
        this.mRilDataRadioTechnology = bundle.getInt("dataRadioTechnology");
        this.mCssIndicator = bundle.getBoolean("cssIndicator");
        this.mNetworkId = bundle.getInt("networkId");
        this.mSystemId = bundle.getInt("systemId");
        this.mCdmaRoamingIndicator = bundle.getInt("cdmaRoamingIndicator");
        this.mCdmaDefaultRoamingIndicator = bundle.getInt("cdmaDefaultRoamingIndicator");
        this.mIsEmergencyOnly = bundle.getBoolean("emergencyOnly");
        this.mIsDataRoamingFromRegistration = bundle.getBoolean("isDataRoamingFromRegistration");
        this.mIsUsingCarrierAggregation = bundle.getBoolean("isUsingCarrierAggregation");
        this.mLteEarfcnRsrpBoost = bundle.getInt("LteEarfcnRsrpBoost");
        this.mChannelNumber = bundle.getInt("ChannelNumber");
        this.mCellBandwidths = bundle.getIntArray("CellBandwidths");
        this.mRilVoiceRegState = bundle.getInt("RilVoiceRegState");
        this.mRilDataRegState = bundle.getInt("RilDataRegState");
        this.mProprietaryDataRadioTechnology = bundle.getInt("proprietaryDataRadioTechnology");
        this.mVoiceRejectCause = bundle.getInt("VoiceRejectCause");
        this.mDataRejectCause = bundle.getInt("DataRejectCause");
        this.mIwlanRegState = bundle.getInt("IwlanRegState");
        this.mCellularVoiceRegState = bundle.getInt("CellularVoiceRegState");
        this.mCellularDataRegState = bundle.getInt("CellularDataRegState");
        this.mRilCellularDataRegState = bundle.getInt("RilCellularDataRegState");
        this.mCellularDataRoamingType = bundle.getInt("CellularDataRoamingType");
        this.mRilCellularDataRadioTechnology = bundle.getInt("RilCellularDataRadioTechnology");
        this.mIsUsingCellularCarrierAggregation = bundle.getBoolean("IsUsingCellularCarrierAggregation");
    }

    public void fillInNotifierBundle(Bundle bundle) {
        bundle.putInt("voiceRegState", this.mVoiceRegState);
        bundle.putInt("dataRegState", this.mDataRegState);
        bundle.putInt("voiceRoamingType", this.mVoiceRoamingType);
        bundle.putInt("dataRoamingType", this.mDataRoamingType);
        bundle.putString("operator-alpha-long", this.mVoiceOperatorAlphaLong);
        bundle.putString("operator-alpha-short", this.mVoiceOperatorAlphaShort);
        bundle.putString("operator-numeric", this.mVoiceOperatorNumeric);
        bundle.putString("data-operator-alpha-long", this.mDataOperatorAlphaLong);
        bundle.putString("data-operator-alpha-short", this.mDataOperatorAlphaShort);
        bundle.putString("data-operator-numeric", this.mDataOperatorNumeric);
        bundle.putBoolean("manual", this.mIsManualNetworkSelection);
        bundle.putInt("radioTechnology", this.mRilVoiceRadioTechnology);
        bundle.putInt("dataRadioTechnology", this.mRilDataRadioTechnology);
        bundle.putBoolean("cssIndicator", this.mCssIndicator);
        bundle.putInt("networkId", this.mNetworkId);
        bundle.putInt("systemId", this.mSystemId);
        bundle.putInt("cdmaRoamingIndicator", this.mCdmaRoamingIndicator);
        bundle.putInt("cdmaDefaultRoamingIndicator", this.mCdmaDefaultRoamingIndicator);
        bundle.putBoolean("emergencyOnly", this.mIsEmergencyOnly);
        bundle.putBoolean("isDataRoamingFromRegistration", this.mIsDataRoamingFromRegistration);
        bundle.putBoolean("isUsingCarrierAggregation", this.mIsUsingCarrierAggregation);
        bundle.putInt("LteEarfcnRsrpBoost", this.mLteEarfcnRsrpBoost);
        bundle.putInt("ChannelNumber", this.mChannelNumber);
        bundle.putIntArray("CellBandwidths", this.mCellBandwidths);
        bundle.putInt("RilVoiceRegState", this.mRilVoiceRegState);
        bundle.putInt("RilDataRegState", this.mRilDataRegState);
        bundle.putInt("proprietaryDataRadioTechnology", this.mProprietaryDataRadioTechnology);
        bundle.putInt("VoiceRejectCause", this.mVoiceRejectCause);
        bundle.putInt("DataRejectCause", this.mDataRejectCause);
        bundle.putInt("IwlanRegState", this.mIwlanRegState);
        bundle.putInt("CellularVoiceRegState", this.mCellularVoiceRegState);
        bundle.putInt("CellularDataRegState", this.mCellularDataRegState);
        bundle.putInt("RilCellularDataRegState", this.mRilCellularDataRegState);
        bundle.putInt("CellularDataRoamingType", this.mCellularDataRoamingType);
        bundle.putInt("RilCellularDataRadioTechnology", this.mRilCellularDataRadioTechnology);
        bundle.putBoolean("IsUsingCellularCarrierAggregation", this.mIsUsingCellularCarrierAggregation);
    }

    public static MtkServiceState mergeMtkServiceStates(MtkServiceState mtkServiceState, MtkServiceState mtkServiceState2) {
        if (mtkServiceState2.mVoiceRegState != 0) {
            return mtkServiceState;
        }
        MtkServiceState mtkServiceState3 = new MtkServiceState(mtkServiceState);
        mtkServiceState3.mVoiceRegState = mtkServiceState2.mVoiceRegState;
        mtkServiceState3.mIsEmergencyOnly = false;
        return mtkServiceState3;
    }

    public int getVoiceRejectCause() {
        return this.mVoiceRejectCause;
    }

    public int getDataRejectCause() {
        return this.mDataRejectCause;
    }

    public void setVoiceRejectCause(int i) {
        this.mVoiceRejectCause = i;
    }

    public void setDataRejectCause(int i) {
        this.mDataRejectCause = i;
    }

    public int getProprietaryDataRadioTechnology() {
        return this.mProprietaryDataRadioTechnology;
    }

    public void setProprietaryDataRadioTechnology(int i) {
        this.mProprietaryDataRadioTechnology = i;
    }

    public int rilRadioTechnologyToNetworkTypeEx(int i) {
        return rilRadioTechnologyToNetworkType(i);
    }

    public int getRilVoiceRegState() {
        return this.mRilVoiceRegState;
    }

    public int getRilDataRegState() {
        return this.mRilDataRegState;
    }

    public void setRilVoiceRegState(int i) {
        this.mRilVoiceRegState = i;
    }

    public void setRilDataRegState(int i) {
        this.mRilDataRegState = i;
    }

    public boolean isVoiceRadioTechnologyHigher(int i) {
        return compareTwoRadioTechnology(this.mRilVoiceRadioTechnology, i);
    }

    public boolean isDataRadioTechnologyHigher(int i) {
        return compareTwoRadioTechnology(this.mRilDataRadioTechnology, i);
    }

    public boolean compareTwoRadioTechnology(int i, int i2) {
        if (i == i2) {
            return false;
        }
        if (i == 14) {
            return true;
        }
        if (i2 == 14) {
            return false;
        }
        return i == 16 ? i2 == 0 : i2 == 16 ? i != 0 : i > i2;
    }

    public boolean getCellularDataRoaming() {
        return this.mCellularDataRoamingType != 0;
    }

    public int getCellularDataNetworkType() {
        return rilRadioTechnologyToNetworkType(this.mRilCellularDataRadioTechnology);
    }

    public int getCellularRegState() {
        if (this.mCellularDataRegState == 0) {
            return this.mCellularDataRegState;
        }
        return this.mCellularVoiceRegState;
    }

    public int getCellularVoiceRegState() {
        return this.mCellularVoiceRegState;
    }

    public int getCellularDataRegState() {
        return this.mCellularDataRegState;
    }

    public int getRilCellularDataRegState() {
        return this.mRilCellularDataRegState;
    }

    public int getCellularDataRoamingType() {
        return this.mCellularDataRoamingType;
    }

    public boolean isUsingCellularCarrierAggregation() {
        return this.mIsUsingCellularCarrierAggregation;
    }

    public void setIwlanRegState(int i) {
        this.mIwlanRegState = i;
    }

    public int getIwlanRegState() {
        return this.mIwlanRegState;
    }

    public void mergeIwlanServiceState() {
        this.mCellularVoiceRegState = this.mVoiceRegState;
        this.mCellularDataRegState = this.mDataRegState;
        this.mRilCellularDataRegState = this.mRilDataRegState;
        this.mCellularDataRoamingType = this.mDataRoamingType;
        this.mRilCellularDataRadioTechnology = this.mRilDataRadioTechnology;
        this.mIsUsingCellularCarrierAggregation = this.mIsUsingCarrierAggregation;
        boolean zEquals = "c6m_1rild".equals(SystemProperties.get("ro.vendor.mtk_ril_mode"));
        if ((zEquals && getIwlanRegState() == 0) || (!zEquals && getIwlanRegState() == 0 && getDataRegState() != 0)) {
            this.mDataRegState = 0;
            this.mRilDataRegState = 1;
            this.mDataRoamingType = 0;
            this.mRilDataRadioTechnology = 18;
            this.mIsUsingCarrierAggregation = false;
        }
    }
}
