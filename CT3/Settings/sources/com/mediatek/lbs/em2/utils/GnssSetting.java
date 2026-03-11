package com.mediatek.lbs.em2.utils;

public class GnssSetting {
    public boolean sib8sib16Enable = true;
    public boolean gpsSatelliteEnable = true;
    public boolean glonassSatelliteEnable = true;
    public boolean beidouSatelliteEnable = true;
    public boolean galileoSatelliteEnable = true;
    public boolean aGpsSatelliteEnable = true;
    public boolean aGlonassSatelliteEnable = true;
    public boolean aBeidouSatelliteEnable = false;
    public boolean aGalileoSatelliteEnable = false;
    public boolean gpsSatelliteSupport = true;
    public boolean glonassSatelliteSupport = true;
    public boolean beidousSatelliteSupport = true;
    public boolean galileoSatelliteSupport = true;

    public String toString() {
        String ret = "sib8sib16Enable=[" + this.sib8sib16Enable + "] ";
        return (((((((((((ret + "gpsSatelliteEnable=[" + this.gpsSatelliteEnable + "] ") + "glonassSatelliteEnable=[" + this.glonassSatelliteEnable + "] ") + "beidouSatelliteEnable=[" + this.beidouSatelliteEnable + "] ") + "galileoSatelliteEnable=[" + this.galileoSatelliteEnable + "] ") + "aGpsSatelliteEnable=[" + this.aGpsSatelliteEnable + "] ") + "aGlonassSatelliteEnable=[" + this.aGlonassSatelliteEnable + "] ") + "aBeidouSatelliteEnable=[" + this.aBeidouSatelliteEnable + "] ") + "aGalileoSatelliteEnable=[" + this.aGalileoSatelliteEnable + "] ") + "gpsSatelliteSupport=[" + this.gpsSatelliteSupport + "] ") + "glonassSatelliteSupport=[" + this.glonassSatelliteSupport + "] ") + "beidousSatelliteSupport=[" + this.beidousSatelliteSupport + "] ") + "galileoSatelliteSupport=[" + this.galileoSatelliteSupport + "] ";
    }
}
