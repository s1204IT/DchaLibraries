package com.mediatek.lbs.em2.utils;

import java.util.ArrayList;

public class AgpsConfig {
    public ArrayList<SuplProfile> suplProfiles = new ArrayList<>();
    public SuplProfile curSuplProfile = new SuplProfile();
    public CdmaProfile cdmaProfile = new CdmaProfile();
    public AgpsSetting agpsSetting = new AgpsSetting();
    public CpSetting cpSetting = new CpSetting();
    public UpSetting upSetting = new UpSetting();
    public GnssSetting gnssSetting = new GnssSetting();

    public SuplProfile getCurSuplProfile() {
        return this.curSuplProfile;
    }

    public CdmaProfile getCdmaProfile() {
        return this.cdmaProfile;
    }

    public AgpsSetting getAgpsSetting() {
        return this.agpsSetting;
    }

    public CpSetting getCpSetting() {
        return this.cpSetting;
    }

    public UpSetting getUpSetting() {
        return this.upSetting;
    }

    public GnssSetting getGnssSetting() {
        return this.gnssSetting;
    }

    public String toString() {
        String ret = "### SuplProfiles ###\n";
        for (SuplProfile p : this.suplProfiles) {
            ret = ret + p + "\n";
        }
        return (((((((((((ret + "### SuplProfile ###\n") + this.curSuplProfile + "\n") + "### CdmaProfile ###\n") + this.cdmaProfile + "\n") + "### AgpsSetting ###\n") + this.agpsSetting + "\n") + "### CpSetting ###\n") + this.cpSetting + "\n") + "### UpSetting ###\n") + this.upSetting + "\n") + "### GnssSetting ###\n") + this.gnssSetting + "\n";
    }
}
