package com.android.phone.settings;

import com.android.internal.telephony.CallForwardInfo;

public class VoicemailProviderSettings {
    private CallForwardInfo[] mForwardingSettings;
    private String mVoicemailNumber;
    public static final CallForwardInfo[] NO_FORWARDING = null;
    public static final int[] FORWARDING_SETTINGS_REASONS = {0, 1, 2, 3};

    public VoicemailProviderSettings(String str, String str2, int i) {
        this.mVoicemailNumber = str;
        if (str2 == null || str2.length() == 0) {
            this.mForwardingSettings = NO_FORWARDING;
            return;
        }
        this.mForwardingSettings = new CallForwardInfo[FORWARDING_SETTINGS_REASONS.length];
        for (int i2 = 0; i2 < this.mForwardingSettings.length; i2++) {
            CallForwardInfo callForwardInfo = new CallForwardInfo();
            this.mForwardingSettings[i2] = callForwardInfo;
            callForwardInfo.reason = FORWARDING_SETTINGS_REASONS[i2];
            callForwardInfo.status = callForwardInfo.reason == 0 ? 0 : 1;
            callForwardInfo.serviceClass = 1;
            callForwardInfo.toa = 145;
            callForwardInfo.number = str2;
            callForwardInfo.timeSeconds = i;
        }
    }

    public VoicemailProviderSettings(String str, CallForwardInfo[] callForwardInfoArr) {
        this.mVoicemailNumber = str;
        this.mForwardingSettings = callForwardInfoArr;
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof VoicemailProviderSettings)) {
            return false;
        }
        VoicemailProviderSettings voicemailProviderSettings = (VoicemailProviderSettings) obj;
        return (this.mVoicemailNumber == null && voicemailProviderSettings.getVoicemailNumber() == null) || (this.mVoicemailNumber != null && this.mVoicemailNumber.equals(voicemailProviderSettings.getVoicemailNumber()) && forwardingSettingsEqual(this.mForwardingSettings, voicemailProviderSettings.getForwardingSettings()));
    }

    public String toString() {
        String str;
        StringBuilder sb = new StringBuilder();
        sb.append(this.mVoicemailNumber);
        if (this.mForwardingSettings == null) {
            str = "";
        } else {
            str = ", " + this.mForwardingSettings.toString();
        }
        sb.append(str);
        return sb.toString();
    }

    public String getVoicemailNumber() {
        return this.mVoicemailNumber;
    }

    public CallForwardInfo[] getForwardingSettings() {
        return this.mForwardingSettings;
    }

    private boolean forwardingSettingsEqual(CallForwardInfo[] callForwardInfoArr, CallForwardInfo[] callForwardInfoArr2) {
        if (callForwardInfoArr == callForwardInfoArr2) {
            return true;
        }
        if (callForwardInfoArr == null || callForwardInfoArr2 == null || callForwardInfoArr.length != callForwardInfoArr2.length) {
            return false;
        }
        for (int i = 0; i < callForwardInfoArr.length; i++) {
            CallForwardInfo callForwardInfo = callForwardInfoArr[i];
            CallForwardInfo callForwardInfo2 = callForwardInfoArr2[i];
            if (callForwardInfo.status != callForwardInfo2.status || callForwardInfo.reason != callForwardInfo2.reason || callForwardInfo.serviceClass != callForwardInfo2.serviceClass || callForwardInfo.toa != callForwardInfo2.toa || callForwardInfo.number != callForwardInfo2.number || callForwardInfo.timeSeconds != callForwardInfo2.timeSeconds) {
                return false;
            }
        }
        return true;
    }
}
