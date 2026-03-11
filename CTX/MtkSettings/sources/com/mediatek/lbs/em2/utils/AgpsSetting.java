package com.mediatek.lbs.em2.utils;

public class AgpsSetting {
    public boolean agpsEnable = false;
    public int agpsProtocol = 0;
    public boolean gpevt = false;
    public boolean e911GpsIconEnable = false;
    public boolean e911OpenGpsEnable = false;
    public boolean tc10IgnoreFwConfig = false;
    public boolean lppeHideWifiBtStatus = false;
    public boolean lppeNetworkLocationDisable = false;
    public boolean agpsNvramEnable = false;
    public boolean lbsLogEnable = false;
    public int lppeCrowdSourceConfident = 90;

    public String toString() {
        String str;
        String str2 = "agpsEnable=[" + this.agpsEnable + "] ";
        if (this.agpsProtocol == 0) {
            str = str2 + "agpsProtocol=[UP] ";
        } else if (this.agpsProtocol == 1) {
            str = str2 + "agpsProtocol=[CP] ";
        } else {
            str = str2 + "agpsProtocol=[UKNOWN " + this.agpsProtocol + "] ";
        }
        return ((((((((str + "gpevt=[" + this.gpevt + "] ") + "e911GpsIconEnable=[" + this.e911GpsIconEnable + "] ") + "e911OpenGpsEnable=[" + this.e911OpenGpsEnable + "] ") + "tc10IgnoreFwConfig=[" + this.tc10IgnoreFwConfig + "] ") + "lppeHideWifiBtStatus=[" + this.lppeHideWifiBtStatus + "] ") + "lppeNetworkLocationDisable=[" + this.lppeNetworkLocationDisable + "] ") + "agpsNvramEnable=[" + this.agpsNvramEnable + "] ") + "lbsLogEnable=[" + this.lbsLogEnable + "] ") + "lppeCrowdSourceConfident=[" + this.lppeCrowdSourceConfident + "] ";
    }
}
