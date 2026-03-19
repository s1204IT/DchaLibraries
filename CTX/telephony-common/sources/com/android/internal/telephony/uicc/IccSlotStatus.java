package com.android.internal.telephony.uicc;

import android.telephony.SubscriptionInfo;
import android.text.TextUtils;
import com.android.internal.telephony.uicc.IccCardStatus;

public class IccSlotStatus {
    public String atr;
    public IccCardStatus.CardState cardState;
    public String iccid;
    public int logicalSlotIndex;
    public SlotState slotState;

    public enum SlotState {
        SLOTSTATE_INACTIVE,
        SLOTSTATE_ACTIVE
    }

    public void setCardState(int i) {
        switch (i) {
            case 0:
                this.cardState = IccCardStatus.CardState.CARDSTATE_ABSENT;
                return;
            case 1:
                this.cardState = IccCardStatus.CardState.CARDSTATE_PRESENT;
                return;
            case 2:
                this.cardState = IccCardStatus.CardState.CARDSTATE_ERROR;
                return;
            case 3:
                this.cardState = IccCardStatus.CardState.CARDSTATE_RESTRICTED;
                return;
            default:
                throw new RuntimeException("Unrecognized RIL_CardState: " + i);
        }
    }

    public void setSlotState(int i) {
        switch (i) {
            case 0:
                this.slotState = SlotState.SLOTSTATE_INACTIVE;
                return;
            case 1:
                this.slotState = SlotState.SLOTSTATE_ACTIVE;
                return;
            default:
                throw new RuntimeException("Unrecognized RIL_SlotState: " + i);
        }
    }

    public String toString() {
        return "IccSlotStatus {" + this.cardState + "," + this.slotState + ",logicalSlotIndex=" + this.logicalSlotIndex + ",atr=" + this.atr + ",iccid=" + SubscriptionInfo.givePrintableIccid(this.iccid) + "}";
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        IccSlotStatus iccSlotStatus = (IccSlotStatus) obj;
        if (this.cardState == iccSlotStatus.cardState && this.slotState == iccSlotStatus.slotState && this.logicalSlotIndex == iccSlotStatus.logicalSlotIndex && TextUtils.equals(this.atr, iccSlotStatus.atr) && TextUtils.equals(this.iccid, iccSlotStatus.iccid)) {
            return true;
        }
        return false;
    }
}
