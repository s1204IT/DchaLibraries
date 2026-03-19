package com.mediatek.server.powerhal;

import com.mediatek.powerhalwrapper.PowerHalWrapper;

public class PowerHalManagerImpl extends PowerHalManager {
    private static final String TAG = "PowerHalManagerImpl";
    private static PowerHalWrapper mPowerHalWrap = null;
    public boolean mIsRotationBoostEnable = false;

    public PowerHalManagerImpl() {
        mPowerHalWrap = PowerHalWrapper.getInstance();
    }

    public void setRotationBoost(boolean z) {
        int i = 0;
        if (z && !this.mIsRotationBoostEnable) {
            i = 2000;
        } else if (z || this.mIsRotationBoostEnable) {
        }
        mPowerHalWrap.setRotationBoost(i);
        this.mIsRotationBoostEnable = z;
    }

    public void setWFD(boolean z) {
        mPowerHalWrap.setWFD(z);
    }

    public void setInstallationBoost(boolean z) {
        mPowerHalWrap.setInstallationBoost(z);
    }

    public void setSpeedDownload(int i) {
        mPowerHalWrap.setSpeedDownload(i);
    }

    public void amsBoostResume(String str, String str2) {
        mPowerHalWrap.amsBoostResume(str, str2);
    }

    public void amsBoostNotify(int i, String str, String str2) {
        mPowerHalWrap.amsBoostNotify(i, str, str2);
    }

    public void amsBoostProcessCreate(String str, String str2) {
        mPowerHalWrap.amsBoostProcessCreate(str, str2);
    }

    public void amsBoostStop() {
        mPowerHalWrap.amsBoostStop();
    }
}
