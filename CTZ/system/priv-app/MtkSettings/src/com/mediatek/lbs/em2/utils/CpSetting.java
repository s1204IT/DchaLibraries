package com.mediatek.lbs.em2.utils;

/* loaded from: classes.dex */
public class CpSetting {
    public int molrPosMethod = 0;
    public boolean externalAddrEnable = false;
    public String externalAddr = "";
    public boolean mlcNumberEnable = false;
    public String mlcNumber = "";
    public boolean cpAutoReset = false;
    public boolean epcMolrLppPayloadEnable = false;
    public byte[] epcMolrLppPayload = new byte[0];
    public boolean rejectNon911NilrEnable = false;
    public boolean cp2gDisable = false;
    public boolean cp3gDisable = false;
    public boolean cp4gDisable = false;
    public boolean cpLppeEnable = false;
    public boolean cpLppeSupport = false;

    public String toString() {
        String str;
        if (this.molrPosMethod == 0) {
            str = "molrPosMethod=[LOC_EST] ";
        } else if (this.molrPosMethod == 1) {
            str = "molrPosMethod=[ASSIST_DATA] ";
        } else {
            str = "molrPosMethod=[UNKNOWN " + this.molrPosMethod + "] ";
        }
        String str2 = ((((((((((((str + "externalAddrEnable=[" + this.externalAddrEnable + "] ") + "externalAddr=[" + this.externalAddr + "] ") + "mlcNumberEnable=[" + this.mlcNumberEnable + "] ") + "mlcNumber=[" + this.mlcNumber + "] ") + "cpAutoReset=[" + this.cpAutoReset + "] ") + "rejectNon911NilrEnable=[" + this.rejectNon911NilrEnable + "] ") + "cp2gDisable=[" + this.cp2gDisable + "] ") + "cp3gDisable=[" + this.cp3gDisable + "] ") + "cp4gDisable=[" + this.cp4gDisable + "] ") + "cpLppeEnable=[" + this.cpLppeEnable + "] ") + "cpLppeSupport=[" + this.cpLppeSupport + "] ") + "epcMolrLppPayloadEnable=[" + this.epcMolrLppPayloadEnable + "] ") + "epcMolrLppPayload.len=[" + this.epcMolrLppPayload.length + "][";
        for (int i = 0; i < this.epcMolrLppPayload.length; i++) {
            str2 = str2 + String.format("%02x", Byte.valueOf(this.epcMolrLppPayload[i]));
        }
        return str2 + "]";
    }
}
