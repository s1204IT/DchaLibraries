package com.mediatek.lbs.em2.utils;

public class AgpsSetting {
    public boolean agpsEnable = false;
    public int agpsProtocol = 0;
    public boolean gpevt = false;
    public boolean e911GpsIconEnable = false;
    public boolean e911OpenGpsEnable = false;

    public String toString() {
        String ret;
        String ret2 = "agpsEnable=[" + this.agpsEnable + "] ";
        if (this.agpsProtocol == 0) {
            ret = ret2 + "agpsProtocol=[UP] ";
        } else if (this.agpsProtocol == 1) {
            ret = ret2 + "agpsProtocol=[CP] ";
        } else {
            ret = ret2 + "agpsProtocol=[UKNOWN " + this.agpsProtocol + "] ";
        }
        return ((ret + "gpevt=[" + this.gpevt + "] ") + "e911GpsIconEnable=[" + this.e911GpsIconEnable + "] ") + "e911OpenGpsEnable=[" + this.e911OpenGpsEnable + "] ";
    }
}
