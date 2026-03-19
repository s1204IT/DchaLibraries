package com.mediatek.internal.telephony.dataconnection;

import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;

public class IaExtendParam {
    public boolean mCanHandleIms;
    public String[] mDualApnPlmnList;
    public String mOperatorNumeric;
    public String mRoamingProtocol;

    public IaExtendParam() {
        this.mOperatorNumeric = UsimPBMemInfo.STRING_NOT_SET;
        this.mCanHandleIms = false;
        this.mDualApnPlmnList = null;
        this.mRoamingProtocol = UsimPBMemInfo.STRING_NOT_SET;
    }

    public IaExtendParam(String operatorNumeric) {
        this.mOperatorNumeric = operatorNumeric;
        this.mCanHandleIms = false;
        this.mDualApnPlmnList = null;
        this.mRoamingProtocol = UsimPBMemInfo.STRING_NOT_SET;
    }

    public IaExtendParam(String operatorNumeric, String[] dualApnPlmnList) {
        this.mOperatorNumeric = operatorNumeric;
        this.mCanHandleIms = false;
        this.mDualApnPlmnList = dualApnPlmnList;
        this.mRoamingProtocol = UsimPBMemInfo.STRING_NOT_SET;
    }

    public IaExtendParam(String operatorNumeric, String[] dualApnPlmnList, String roamingProtocol) {
        this.mOperatorNumeric = operatorNumeric;
        this.mCanHandleIms = false;
        this.mDualApnPlmnList = dualApnPlmnList;
        this.mRoamingProtocol = roamingProtocol;
    }

    public IaExtendParam(String operatorNumeric, boolean canHandleIms, String[] dualApnPlmnList) {
        this.mOperatorNumeric = operatorNumeric;
        this.mCanHandleIms = canHandleIms;
        this.mDualApnPlmnList = dualApnPlmnList;
        this.mRoamingProtocol = UsimPBMemInfo.STRING_NOT_SET;
    }

    public IaExtendParam(String operatorNumeric, boolean canHandleIms, String[] dualApnPlmnList, String roamingProtocol) {
        this.mOperatorNumeric = operatorNumeric;
        this.mCanHandleIms = canHandleIms;
        this.mDualApnPlmnList = dualApnPlmnList;
        this.mRoamingProtocol = roamingProtocol;
    }

    public String toString() {
        return "[OperatorNumberic: " + this.mOperatorNumeric + ", CanHandleIms: " + this.mCanHandleIms + ", DualApnPlmnList: " + this.mDualApnPlmnList + ", RoamingProtocol: " + this.mRoamingProtocol + "]";
    }
}
