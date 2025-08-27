package com.mediatek.lbs.em2.utils;

/* loaded from: classes.dex */
public class SuplProfile {
    public String name = "";
    public String addr = "";
    public int port = 0;
    public boolean tls = false;
    public String mccMnc = "";
    public String appId = "";
    public String providerId = "";
    public String defaultApn = "";
    public String optionalApn = "";
    public String optionalApn2 = "";
    public String addressType = "";

    public String toString() {
        String str = "name=[" + this.name + "] addr=[" + this.addr + "] port=[" + this.port + "] tls=[" + this.tls + "] ";
        if (!this.mccMnc.equals("")) {
            str = str + "mccMnc=[" + this.mccMnc + "] ";
        }
        if (!this.appId.equals("")) {
            str = str + "appId=[" + this.appId + "] ";
        }
        if (!this.providerId.equals("")) {
            str = str + "providerId=[" + this.providerId + "] ";
        }
        if (!this.defaultApn.equals("")) {
            str = str + "defaultApn=[" + this.defaultApn + "] ";
        }
        if (!this.optionalApn.equals("")) {
            str = str + "optionalApn=[" + this.optionalApn + "] ";
        }
        if (!this.optionalApn2.equals("")) {
            str = str + "optionalApn2=[" + this.optionalApn2 + "] ";
        }
        if (!this.addressType.equals("")) {
            return str + "addressType=[" + this.addressType + "] ";
        }
        return str;
    }
}
