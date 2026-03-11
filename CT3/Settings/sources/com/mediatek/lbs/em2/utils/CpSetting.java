package com.mediatek.lbs.em2.utils;

public class CpSetting {
    public int molrPosMethod = 0;
    public boolean externalAddrEnable = false;
    public String externalAddr = "";
    public boolean mlcNumberEnable = false;
    public String mlcNumber = "";
    public boolean cpAutoReset = false;
    public boolean epcMolrLppPayloadEnable = false;
    public byte[] epcMolrLppPayload = new byte[0];

    public String toString() {
        String ret;
        if (this.molrPosMethod == 0) {
            ret = "molrPosMethod=[LOC_EST] ";
        } else if (this.molrPosMethod == 1) {
            ret = "molrPosMethod=[ASSIST_DATA] ";
        } else {
            ret = "molrPosMethod=[UNKNOWN " + this.molrPosMethod + "] ";
        }
        String ret2 = ((((((ret + "externalAddrEnable=[" + this.externalAddrEnable + "] ") + "externalAddr=[" + this.externalAddr + "] ") + "mlcNumberEnable=[" + this.mlcNumberEnable + "] ") + "mlcNumber=[" + this.mlcNumber + "] ") + "cpAutoReset=[" + this.cpAutoReset + "] ") + "epcMolrLppPayloadEnable=[" + this.epcMolrLppPayloadEnable + "] ") + "epcMolrLppPayload.len=[" + this.epcMolrLppPayload.length + "][";
        for (int i = 0; i < this.epcMolrLppPayload.length; i++) {
            ret2 = ret2 + String.format("%02x", Byte.valueOf(this.epcMolrLppPayload[i]));
        }
        return ret2 + "]";
    }
}
