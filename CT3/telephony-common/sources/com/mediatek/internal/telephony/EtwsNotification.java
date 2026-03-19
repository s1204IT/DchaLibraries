package com.mediatek.internal.telephony;

import com.android.internal.telephony.uicc.IccUtils;

public class EtwsNotification {
    public int messageId;
    public String plmnId;
    public String securityInfo;
    public int serialNumber;
    public int warningType;

    public String toString() {
        return "EtwsNotification: " + this.warningType + ", " + this.messageId + ", " + this.serialNumber + ", " + this.plmnId + ", " + this.securityInfo;
    }

    public boolean isDuplicatedEtws(EtwsNotification other) {
        if (this.warningType == other.warningType && this.messageId == other.messageId && this.serialNumber == other.serialNumber && this.plmnId.equals(other.plmnId)) {
            return true;
        }
        return false;
    }

    public byte[] getEtwsPdu() {
        byte[] etwsPdu = new byte[56];
        byte[] serialNumberBytes = EtwsUtils.intToBytes(this.serialNumber);
        System.arraycopy(serialNumberBytes, 2, etwsPdu, 0, 2);
        byte[] messageIdBytes = EtwsUtils.intToBytes(this.messageId);
        System.arraycopy(messageIdBytes, 2, etwsPdu, 2, 2);
        byte[] warningTypeBytes = EtwsUtils.intToBytes(this.warningType);
        System.arraycopy(warningTypeBytes, 2, etwsPdu, 4, 2);
        if (this.securityInfo != null) {
            System.arraycopy(IccUtils.hexStringToBytes(this.securityInfo), 0, etwsPdu, 6, 50);
        }
        return etwsPdu;
    }
}
