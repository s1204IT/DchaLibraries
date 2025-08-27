package com.mediatek.lbs.em2.utils;

/* loaded from: classes.dex */
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
    public byte ut1 = 11;
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
    public int shaVersion = 0;
    public int preferred2g3gCellAge = 0;
    public boolean noSensitiveLog = false;
    public boolean tlsReuseEnable = false;
    public boolean imsiCacheEnable = false;
    public boolean suplRawDataEnable = false;
    public boolean tc10Enable = false;
    public boolean tc10UseApn = false;
    public boolean tc10UseFwDns = false;
    public boolean allowNiForGpsOff = false;
    public boolean forceOtdoaAssistReq = false;
    public boolean upLppeEnable = false;
    public int esuplApnMode = 0;
    public int tcpKeepAlive = 0;
    public boolean aospProfileEnable = false;
    public boolean bindNlpSettingToSupl = false;

    public String toString() {
        String str;
        String str2;
        String str3 = (("caEnable=[" + this.caEnable + "] ") + "niRequest=[" + this.niRequest + "] ") + "roaming=[" + this.roaming + "] ";
        if (this.cdmaPreferred == 0) {
            str = str3 + "cdmaPreferred=[WCDMA] ";
        } else if (this.cdmaPreferred == 1) {
            str = str3 + "cdmaPreferred=[CDMA] ";
        } else if (this.cdmaPreferred == 2) {
            str = str3 + "cdmaPreferred=[CDMA_FORCE] ";
        } else {
            str = str3 + "cdmaPreferred=[UNKNOWN " + this.cdmaPreferred + "] ";
        }
        if (this.prefMethod == 0) {
            str2 = str + "prefMethod=[MSA] ";
        } else if (this.prefMethod == 1) {
            str2 = str + "prefMethod=[MSB] ";
        } else if (this.prefMethod == 2) {
            str2 = str + "prefMethod=[NO_PREF] ";
        } else {
            str2 = str + "prefMethod=[UNKNOWN " + this.prefMethod + "] ";
        }
        return ((((((((((((((((((((((((((((((((((((((((str2 + "suplVersion=[" + this.suplVersion + "] ") + "tlsVersion=[" + this.tlsVersion + "] ") + "suplLog=[" + this.suplLog + "] ") + "msaEnable=[" + this.msaEnable + "] ") + "msbEnable=[" + this.msbEnable + "] ") + "ecidEnable=[" + this.ecidEnable + "] ") + "otdoaEnable=[" + this.otdoaEnable + "] ") + "qopHacc=[" + this.qopHacc + "] ") + "qopVacc=[" + this.qopVacc + "] ") + "qopLocAge=[" + this.qopLocAge + "] ") + "qopDelay=[" + this.qopDelay + "] ") + "lppEnable=[" + this.lppEnable + "] ") + "certFromSdcard=[" + this.certFromSdcard + "] ") + "autoProfileEnable=[" + this.autoProfileEnable + "] ") + "ut1=[" + ((int) this.ut1) + "] ") + "ut2=[" + ((int) this.ut2) + "] ") + "ut3=[" + ((int) this.ut3) + "] ") + "apnEnable=[" + this.apnEnable + "] ") + "syncToslp=[" + this.syncToslp + "] ") + "udpEnable=[" + this.udpEnable + "] ") + "autonomousEnable=[" + this.autonomousEnable + "] ") + "afltEnable=[" + this.afltEnable + "] ") + "imsiEnable=[" + this.imsiEnable + "] ") + "suplVerMinor=[" + ((int) this.suplVerMinor) + "] ") + "suplVerSerInd=[" + ((int) this.suplVerSerInd) + "] ") + "shaVersion=[" + this.shaVersion + "] ") + "preferred2g3gCellAge=[" + this.preferred2g3gCellAge + "] ") + "noSensitiveLog=[" + this.noSensitiveLog + "] ") + "tlsReuseEnable=[" + this.tlsReuseEnable + "] ") + "imsiCacheEnable=[" + this.imsiCacheEnable + "] ") + "suplRawDataEnable=[" + this.suplRawDataEnable + "] ") + "tc10Enable=[" + this.tc10Enable + "] ") + "tc10UseApn=[" + this.tc10UseApn + "] ") + "tc10UseFwDns=[" + this.tc10UseFwDns + "] ") + "allowNiForGpsOff=[" + this.allowNiForGpsOff + "] ") + "forceOtdoaAssistReq=[" + this.forceOtdoaAssistReq + "] ") + "upLppeEnable=[" + this.upLppeEnable + "] ") + "esuplApnMode=[" + this.esuplApnMode + "] ") + "tcpKeepAlive=[" + this.tcpKeepAlive + "] ") + "aospProfileEnable=[" + this.aospProfileEnable + "] ") + "bindNlpSettingToSupl=[" + this.bindNlpSettingToSupl + "] ";
    }
}
