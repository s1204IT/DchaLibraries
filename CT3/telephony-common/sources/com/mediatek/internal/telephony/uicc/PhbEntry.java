package com.mediatek.internal.telephony.uicc;

public class PhbEntry {
    public String alphaId;
    public int index;
    public String number;
    public int ton;
    public int type;

    public String toString() {
        return "type: " + this.type + " index: " + this.index + " number: " + this.number + " ton: " + this.ton + " alphaId: " + this.alphaId;
    }
}
