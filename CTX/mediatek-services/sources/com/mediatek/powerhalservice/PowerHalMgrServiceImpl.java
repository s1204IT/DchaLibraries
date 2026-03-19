package com.mediatek.powerhalservice;

import android.util.Log;
import com.mediatek.powerhalmgr.IPowerHalMgr;
import com.mediatek.powerhalwrapper.PowerHalWrapper;

public class PowerHalMgrServiceImpl extends IPowerHalMgr.Stub {
    private static PowerHalWrapper mPowerHalWrap = null;
    private final String TAG = "PowerHalMgrServiceImpl";

    public PowerHalMgrServiceImpl() {
        mPowerHalWrap = PowerHalWrapper.getInstance();
    }

    public int scnReg() {
        return mPowerHalWrap.scnReg();
    }

    public void scnConfig(int i, int i2, int i3, int i4, int i5, int i6) {
        mPowerHalWrap.scnConfig(i, i2, i3, i4, i5, i6);
    }

    public void scnUnreg(int i) {
        mPowerHalWrap.scnUnreg(i);
    }

    public void scnEnable(int i, int i2) {
        mPowerHalWrap.scnEnable(i, i2);
    }

    public void scnDisable(int i) {
        mPowerHalWrap.scnDisable(i);
    }

    public void scnUltraCfg(int i, int i2, int i3, int i4, int i5, int i6) {
        mPowerHalWrap.scnUltraCfg(i, i2, i3, i4, i5, i6);
    }

    public void mtkCusPowerHint(int i, int i2) {
        mPowerHalWrap.mtkCusPowerHint(i, i2);
    }

    private void log(String str) {
        Log.d("@M_PowerHalMgrServiceImpl", "[PowerHalMgrServiceImpl] " + str + " ");
    }

    private void loge(String str) {
        Log.e("@M_PowerHalMgrServiceImpl", "[PowerHalMgrServiceImpl] ERR: " + str + " ");
    }
}
