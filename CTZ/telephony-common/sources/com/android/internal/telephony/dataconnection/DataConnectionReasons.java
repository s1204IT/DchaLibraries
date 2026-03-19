package com.android.internal.telephony.dataconnection;

import java.util.HashSet;
import java.util.Iterator;

public class DataConnectionReasons {
    public HashSet<DataDisallowedReasonType> mDataDisallowedReasonSet = new HashSet<>();
    private DataAllowedReasonType mDataAllowedReason = DataAllowedReasonType.NONE;

    public enum DataAllowedReasonType {
        NONE,
        NORMAL,
        UNMETERED_APN,
        RESTRICTED_REQUEST,
        EMERGENCY_APN
    }

    public void add(DataDisallowedReasonType dataDisallowedReasonType) {
        this.mDataAllowedReason = DataAllowedReasonType.NONE;
        this.mDataDisallowedReasonSet.add(dataDisallowedReasonType);
    }

    public void add(DataAllowedReasonType dataAllowedReasonType) {
        this.mDataDisallowedReasonSet.clear();
        if (dataAllowedReasonType.ordinal() > this.mDataAllowedReason.ordinal()) {
            this.mDataAllowedReason = dataAllowedReasonType;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.mDataDisallowedReasonSet.size() > 0) {
            sb.append("Data disallowed, reasons:");
            for (DataDisallowedReasonType dataDisallowedReasonType : this.mDataDisallowedReasonSet) {
                sb.append(" ");
                sb.append(dataDisallowedReasonType);
            }
        } else {
            sb.append("Data allowed, reason:");
            sb.append(" ");
            sb.append(this.mDataAllowedReason);
        }
        return sb.toString();
    }

    public void copyFrom(DataConnectionReasons dataConnectionReasons) {
        this.mDataDisallowedReasonSet = dataConnectionReasons.mDataDisallowedReasonSet;
        this.mDataAllowedReason = dataConnectionReasons.mDataAllowedReason;
    }

    public boolean allowed() {
        return this.mDataDisallowedReasonSet.size() == 0;
    }

    public boolean contains(DataDisallowedReasonType dataDisallowedReasonType) {
        return this.mDataDisallowedReasonSet.contains(dataDisallowedReasonType);
    }

    public boolean containsOnly(DataDisallowedReasonType dataDisallowedReasonType) {
        return this.mDataDisallowedReasonSet.size() == 1 && contains(dataDisallowedReasonType);
    }

    public boolean contains(DataAllowedReasonType dataAllowedReasonType) {
        return dataAllowedReasonType == this.mDataAllowedReason;
    }

    public boolean containsHardDisallowedReasons() {
        Iterator<DataDisallowedReasonType> it = this.mDataDisallowedReasonSet.iterator();
        while (it.hasNext()) {
            if (it.next().isHardReason()) {
                return true;
            }
        }
        return false;
    }

    public enum DataDisallowedReasonType {
        DATA_DISABLED(false),
        ROAMING_DISABLED(false),
        NOT_ATTACHED(true),
        RECORD_NOT_LOADED(true),
        INVALID_PHONE_STATE(true),
        CONCURRENT_VOICE_DATA_NOT_ALLOWED(true),
        PS_RESTRICTED(true),
        UNDESIRED_POWER_STATE(true),
        INTERNAL_DATA_DISABLED(true),
        DEFAULT_DATA_UNSELECTED(true),
        RADIO_DISABLED_BY_CARRIER(true),
        APN_NOT_CONNECTABLE(true),
        ON_IWLAN(true),
        IN_ECBM(true),
        MTK_FDN_ENABLED(true),
        MTK_NOT_ALLOWED(true),
        MTK_LOCATED_PLMN_CHANGED(true),
        MTK_NON_VSIM_PDN_NOT_ALLOWED(true),
        MTK_PREEMPT_PDN_NOT_ALLOWED(true);

        private boolean mIsHardReason;

        boolean isHardReason() {
            return this.mIsHardReason;
        }

        DataDisallowedReasonType(boolean z) {
            this.mIsHardReason = z;
        }
    }
}
