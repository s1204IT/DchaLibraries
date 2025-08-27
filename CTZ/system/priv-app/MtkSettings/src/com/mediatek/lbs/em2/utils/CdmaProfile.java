package com.mediatek.lbs.em2.utils;

/* loaded from: classes.dex */
public class CdmaProfile {
    public String name = "";
    public boolean mcpEnable = false;
    public String mcpAddr = "";
    public int mcpPort = 0;
    public boolean pdeAddrValid = false;
    public int pdeIpType = 0;
    public String pdeAddr = "";
    public int pdePort = 0;
    public boolean pdeUrlValid = false;
    public String pdeUrlAddr = "";

    public String toString() {
        String str;
        String str2 = (((("name=[" + this.name + "] ") + "mcpEnable=[" + this.mcpEnable + "] ") + "mcpAddr=[" + this.mcpAddr + "] ") + "mcpPort=[" + this.mcpPort + "] ") + "pdeAddrValid=[" + this.pdeAddrValid + "] ";
        if (this.pdeIpType == 0) {
            str = str2 + "pdeIpType=[IPv4] ";
        } else if (this.pdeIpType == 1) {
            str = str2 + "pdeIpType=[IPv6] ";
        } else {
            str = str2 + "pdeIpType=[UNKNOWN " + this.pdeIpType + "] ";
        }
        return (((str + "pdeAddr=[" + this.pdeAddr + "] ") + "pdePort=[" + this.pdePort + "] ") + "pdeUrlValid=[" + this.pdeUrlValid + "] ") + "pdeUrlAddr=[" + this.pdeUrlAddr + "] ";
    }
}
