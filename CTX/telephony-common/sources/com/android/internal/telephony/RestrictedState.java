package com.android.internal.telephony;

public class RestrictedState {
    private boolean mCsEmergencyRestricted;
    private boolean mCsNormalRestricted;
    private boolean mPsRestricted;

    public RestrictedState() {
        setPsRestricted(false);
        setCsNormalRestricted(false);
        setCsEmergencyRestricted(false);
    }

    public void setCsEmergencyRestricted(boolean z) {
        this.mCsEmergencyRestricted = z;
    }

    public boolean isCsEmergencyRestricted() {
        return this.mCsEmergencyRestricted;
    }

    public void setCsNormalRestricted(boolean z) {
        this.mCsNormalRestricted = z;
    }

    public boolean isCsNormalRestricted() {
        return this.mCsNormalRestricted;
    }

    public void setPsRestricted(boolean z) {
        this.mPsRestricted = z;
    }

    public boolean isPsRestricted() {
        return this.mPsRestricted;
    }

    public boolean isCsRestricted() {
        return this.mCsNormalRestricted && this.mCsEmergencyRestricted;
    }

    public boolean isAnyCsRestricted() {
        return this.mCsNormalRestricted || this.mCsEmergencyRestricted;
    }

    public boolean equals(Object obj) {
        try {
            RestrictedState restrictedState = (RestrictedState) obj;
            return obj != null && this.mPsRestricted == restrictedState.mPsRestricted && this.mCsNormalRestricted == restrictedState.mCsNormalRestricted && this.mCsEmergencyRestricted == restrictedState.mCsEmergencyRestricted;
        } catch (ClassCastException e) {
            return false;
        }
    }

    public String toString() {
        String str = "none";
        if (this.mCsEmergencyRestricted && this.mCsNormalRestricted) {
            str = "all";
        } else if (this.mCsEmergencyRestricted && !this.mCsNormalRestricted) {
            str = "emergency";
        } else if (!this.mCsEmergencyRestricted && this.mCsNormalRestricted) {
            str = "normal call";
        }
        return "Restricted State CS: " + str + " PS:" + this.mPsRestricted;
    }
}
