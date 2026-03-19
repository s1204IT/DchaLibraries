package com.mediatek.internal.telephony;

public class NetworkInfoWithAcT {
    int nAct;
    int nPriority;
    String operatorAlphaName;
    String operatorNumeric;

    public String getOperatorAlphaName() {
        return this.operatorAlphaName;
    }

    public String getOperatorNumeric() {
        return this.operatorNumeric;
    }

    public int getAccessTechnology() {
        return this.nAct;
    }

    public int getPriority() {
        return this.nPriority;
    }

    public void setOperatorAlphaName(String str) {
        this.operatorAlphaName = str;
    }

    public void setOperatorNumeric(String str) {
        this.operatorNumeric = str;
    }

    public void setAccessTechnology(int i) {
        this.nAct = i;
    }

    public void setPriority(int i) {
        this.nPriority = i;
    }

    public NetworkInfoWithAcT(String str, String str2, int i, int i2) {
        this.operatorAlphaName = str;
        this.operatorNumeric = str2;
        this.nAct = i;
        this.nPriority = i2;
    }

    public String toString() {
        return "NetworkInfoWithAcT " + this.operatorAlphaName + "/" + this.operatorNumeric + "/" + this.nAct + "/" + this.nPriority;
    }
}
