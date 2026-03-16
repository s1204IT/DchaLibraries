package com.android.phone.settings;

import com.android.internal.telephony.CallForwardInfo;

public class VoicemailProviderSettings {
    private CallForwardInfo[] mForwardingSettings;
    private String mVoicemailNumber;
    public static final CallForwardInfo[] NO_FORWARDING = null;
    public static final int[] FORWARDING_SETTINGS_REASONS = {0, 1, 2, 3};

    public VoicemailProviderSettings(String voicemailNumber, String forwardingNumber, int timeSeconds) {
        this.mVoicemailNumber = voicemailNumber;
        if (forwardingNumber == null || forwardingNumber.length() == 0) {
            this.mForwardingSettings = NO_FORWARDING;
            return;
        }
        this.mForwardingSettings = new CallForwardInfo[FORWARDING_SETTINGS_REASONS.length];
        for (int i = 0; i < this.mForwardingSettings.length; i++) {
            CallForwardInfo fi = new CallForwardInfo();
            this.mForwardingSettings[i] = fi;
            fi.reason = FORWARDING_SETTINGS_REASONS[i];
            fi.status = fi.reason == 0 ? 0 : 1;
            fi.serviceClass = 1;
            fi.toa = 145;
            fi.number = forwardingNumber;
            fi.timeSeconds = timeSeconds;
        }
    }

    public VoicemailProviderSettings(String voicemailNumber, CallForwardInfo[] infos) {
        this.mVoicemailNumber = voicemailNumber;
        this.mForwardingSettings = infos;
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof VoicemailProviderSettings)) {
            return false;
        }
        VoicemailProviderSettings v = (VoicemailProviderSettings) o;
        return (this.mVoicemailNumber == null && v.getVoicemailNumber() == null) || (this.mVoicemailNumber != null && this.mVoicemailNumber.equals(v.getVoicemailNumber()) && forwardingSettingsEqual(this.mForwardingSettings, v.getForwardingSettings()));
    }

    public String toString() {
        return this.mVoicemailNumber + (this.mForwardingSettings == null ? "" : ", " + this.mForwardingSettings.toString());
    }

    public String getVoicemailNumber() {
        return this.mVoicemailNumber;
    }

    public CallForwardInfo[] getForwardingSettings() {
        return this.mForwardingSettings;
    }

    private boolean forwardingSettingsEqual(CallForwardInfo[] infos1, CallForwardInfo[] infos2) {
        if (infos1 == infos2) {
            return true;
        }
        if (infos1 == null || infos2 == null) {
            return false;
        }
        if (infos1.length != infos2.length) {
            return false;
        }
        for (int i = 0; i < infos1.length; i++) {
            CallForwardInfo i1 = infos1[i];
            CallForwardInfo i2 = infos2[i];
            if (i1.status != i2.status || i1.reason != i2.reason || i1.serviceClass != i2.serviceClass || i1.toa != i2.toa || i1.number != i2.number || i1.timeSeconds != i2.timeSeconds) {
                return false;
            }
        }
        return true;
    }
}
