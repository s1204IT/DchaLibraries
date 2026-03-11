package com.mediatek.lbs.em2.utils;

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
        String ret = "name=[" + this.name + "] addr=[" + this.addr + "] port=[" + this.port + "] tls=[" + this.tls + "] ";
        if (!this.mccMnc.equals("")) {
            ret = ret + "mccMnc=[" + this.mccMnc + "] ";
        }
        if (!this.appId.equals("")) {
            ret = ret + "appId=[" + this.appId + "] ";
        }
        if (!this.providerId.equals("")) {
            ret = ret + "providerId=[" + this.providerId + "] ";
        }
        if (!this.defaultApn.equals("")) {
            ret = ret + "defaultApn=[" + this.defaultApn + "] ";
        }
        if (!this.optionalApn.equals("")) {
            ret = ret + "optionalApn=[" + this.optionalApn + "] ";
        }
        if (!this.optionalApn2.equals("")) {
            ret = ret + "optionalApn2=[" + this.optionalApn2 + "] ";
        }
        if (!this.addressType.equals("")) {
            return ret + "addressType=[" + this.addressType + "] ";
        }
        return ret;
    }
}
