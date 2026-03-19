package com.mediatek.powerhalmgr;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.mediatek.powerhalmgr.IPowerHalMgr;

public class PowerHalMgrImpl extends PowerHalMgr {
    private static final String TAG = "PowerHalMgrImpl";
    private Context mContext;
    private static PowerHalMgrImpl sInstance = null;
    private static Object lock = new Object();
    private IPowerHalMgr sService = null;
    private int inited = 0;
    private int setTid = 0;
    private long mPreviousTime = 0;

    public static native int nativeGetPid();

    public static native int nativeGetTid();

    private void init() {
        IBinder iBinderCheckService;
        if (this.inited == 0 && (iBinderCheckService = ServiceManager.checkService("power_hal_mgr_service")) != null) {
            this.sService = IPowerHalMgr.Stub.asInterface(iBinderCheckService);
            if (this.sService != null) {
                this.inited = 1;
            } else {
                log("ERR: getService() sService is still null..");
            }
        }
    }

    public static PowerHalMgrImpl getInstance() {
        if (sInstance == null) {
            synchronized (lock) {
                if (sInstance == null) {
                    sInstance = new PowerHalMgrImpl();
                }
            }
        }
        return sInstance;
    }

    public int scnReg() {
        try {
            init();
            if (this.sService == null) {
                return -1;
            }
            return this.sService.scnReg();
        } catch (RemoteException e) {
            loge("ERR: RemoteException in scnReg:" + e);
            return -1;
        }
    }

    public void scnConfig(int i, int i2, int i3, int i4, int i5, int i6) {
        try {
            init();
            if (this.sService != null) {
                this.sService.scnConfig(i, i2, i3, i4, i5, i6);
            }
        } catch (RemoteException e) {
            loge("ERR: RemoteException in scnConfig:" + e);
        }
    }

    public void scnUnreg(int i) {
        try {
            init();
            if (this.sService != null) {
                this.sService.scnUnreg(i);
            }
        } catch (RemoteException e) {
            loge("ERR: RemoteException in scnUnreg:" + e);
        }
    }

    public void scnEnable(int i, int i2) {
        try {
            init();
            if (this.sService != null) {
                this.sService.scnEnable(i, i2);
            }
        } catch (RemoteException e) {
            loge("ERR: RemoteException in scnEnable:" + e);
        }
    }

    public void scnDisable(int i) {
        try {
            init();
            if (this.sService != null) {
                this.sService.scnDisable(i);
            }
        } catch (RemoteException e) {
            loge("ERR: RemoteException in scnDisable:" + e);
        }
    }

    public void scnUltraCfg(int i, int i2, int i3, int i4, int i5, int i6) {
        try {
            init();
            if (this.sService != null) {
                this.sService.scnUltraCfg(i, i2, i3, i4, i5, i6);
            }
        } catch (RemoteException e) {
            loge("ERR: RemoteException in scnConfig:" + e);
        }
    }

    public void mtkCusPowerHint(int i, int i2) {
        try {
            init();
            if (this.sService != null) {
                this.sService.mtkCusPowerHint(i, i2);
            }
        } catch (RemoteException e) {
            loge("ERR: RemoteException in mtkCusPowerHint:" + e);
        }
    }

    private void log(String str) {
        Log.d("@M_PowerHalMgrImpl", "[PowerHalMgrImpl] " + str + " ");
    }

    private void loge(String str) {
        Log.e("@M_PowerHalMgrImpl", "[PowerHalMgrImpl] ERR: " + str + " ");
    }
}
