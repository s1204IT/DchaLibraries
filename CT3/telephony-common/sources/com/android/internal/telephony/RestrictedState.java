package com.android.internal.telephony;

import com.mediatek.internal.telephony.gsm.GsmVTProviderUtil;

public class RestrictedState {
    private boolean mCsEmergencyRestricted;
    private boolean mCsNormalRestricted;
    private boolean mPsRestricted;

    public RestrictedState() {
        setPsRestricted(false);
        setCsNormalRestricted(false);
        setCsEmergencyRestricted(false);
    }

    public void setCsEmergencyRestricted(boolean csEmergencyRestricted) {
        this.mCsEmergencyRestricted = csEmergencyRestricted;
    }

    public boolean isCsEmergencyRestricted() {
        return this.mCsEmergencyRestricted;
    }

    public void setCsNormalRestricted(boolean csNormalRestricted) {
        this.mCsNormalRestricted = csNormalRestricted;
    }

    public boolean isCsNormalRestricted() {
        return this.mCsNormalRestricted;
    }

    public void setPsRestricted(boolean psRestricted) {
        this.mPsRestricted = psRestricted;
    }

    public boolean isPsRestricted() {
        return this.mPsRestricted;
    }

    public boolean isCsRestricted() {
        if (this.mCsNormalRestricted) {
            return this.mCsEmergencyRestricted;
        }
        return false;
    }

    public boolean equals(Object o) {
        try {
            RestrictedState s = (RestrictedState) o;
            return o != null && this.mPsRestricted == s.mPsRestricted && this.mCsNormalRestricted == s.mCsNormalRestricted && this.mCsEmergencyRestricted == s.mCsEmergencyRestricted;
        } catch (ClassCastException e) {
            return false;
        }
    }

    public String toString() {
        String csString = GsmVTProviderUtil.ParameterSet.EFFECT_NONE;
        if (this.mCsEmergencyRestricted && this.mCsNormalRestricted) {
            csString = "all";
        } else if (this.mCsEmergencyRestricted && !this.mCsNormalRestricted) {
            csString = "emergency";
        } else if (!this.mCsEmergencyRestricted && this.mCsNormalRestricted) {
            csString = "normal call";
        }
        return "Restricted State CS: " + csString + " PS:" + this.mPsRestricted;
    }
}
