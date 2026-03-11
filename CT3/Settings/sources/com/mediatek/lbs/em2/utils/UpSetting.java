package com.mediatek.lbs.em2.utils;

public class UpSetting {
    public boolean caEnable = false;
    public boolean niRequest = false;
    public boolean roaming = false;
    public int cdmaPreferred = 0;
    public int prefMethod = 1;
    public int suplVersion = 1;
    public int tlsVersion = 0;
    public boolean suplLog = false;
    public boolean msaEnable = false;
    public boolean msbEnable = false;
    public boolean ecidEnable = false;
    public boolean otdoaEnable = false;
    public int qopHacc = 0;
    public int qopVacc = 0;
    public int qopLocAge = 0;
    public int qopDelay = 0;
    public boolean lppEnable = false;
    public boolean certFromSdcard = false;
    public boolean autoProfileEnable = false;
    public byte ut2 = 11;
    public byte ut3 = 10;
    public boolean apnEnable = false;
    public boolean syncToslp = false;
    public boolean udpEnable = false;
    public boolean autonomousEnable = false;
    public boolean afltEnable = false;
    public boolean imsiEnable = false;
    public byte suplVerMinor = 0;
    public byte suplVerSerInd = 0;

    public String toString() {
        String ret;
        String ret2;
        String ret3 = (("caEnable=[" + this.caEnable + "] ") + "niRequest=[" + this.niRequest + "] ") + "roaming=[" + this.roaming + "] ";
        if (this.cdmaPreferred == 0) {
            ret = ret3 + "cdmaPreferred=[WCDMA] ";
        } else if (this.cdmaPreferred == 1) {
            ret = ret3 + "cdmaPreferred=[CDMA] ";
        } else if (this.cdmaPreferred == 2) {
            ret = ret3 + "cdmaPreferred=[CDMA_FORCE] ";
        } else {
            ret = ret3 + "cdmaPreferred=[UNKNOWN " + this.cdmaPreferred + "] ";
        }
        if (this.prefMethod == 0) {
            ret2 = ret + "prefMethod=[MSA] ";
        } else if (this.prefMethod == 1) {
            ret2 = ret + "prefMethod=[MSB] ";
        } else if (this.prefMethod == 2) {
            ret2 = ret + "prefMethod=[NO_PREF] ";
        } else {
            ret2 = ret + "prefMethod=[UNKNOWN " + this.prefMethod + "] ";
        }
        return (((((((((((((((((((((((ret2 + "suplVersion=[" + this.suplVersion + "] ") + "tlsVersion=[" + this.tlsVersion + "] ") + "suplLog=[" + this.suplLog + "] ") + "msaEnable=[" + this.msaEnable + "] ") + "msbEnable=[" + this.msbEnable + "] ") + "ecidEnable=[" + this.ecidEnable + "] ") + "otdoaEnable=[" + this.otdoaEnable + "] ") + "qopHacc=[" + this.qopHacc + "] ") + "qopVacc=[" + this.qopVacc + "] ") + "qopLocAge=[" + this.qopLocAge + "] ") + "qopDelay=[" + this.qopDelay + "] ") + "lppEnable=[" + this.lppEnable + "] ") + "certFromSdcard=[" + this.certFromSdcard + "] ") + "autoProfileEnable=[" + this.autoProfileEnable + "] ") + "ut2=[" + ((int) this.ut2) + "] ") + "ut3=[" + ((int) this.ut3) + "] ") + "apnEnable=[" + this.apnEnable + "] ") + "syncToslp=[" + this.syncToslp + "] ") + "udpEnable=[" + this.udpEnable + "] ") + "autonomousEnable=[" + this.autonomousEnable + "] ") + "afltEnable=[" + this.afltEnable + "] ") + "imsiEnable=[" + this.imsiEnable + "] ") + "suplVerMinor=[" + ((int) this.suplVerMinor) + "] ") + "suplVerSerInd=[" + ((int) this.suplVerSerInd) + "] ";
    }
}
