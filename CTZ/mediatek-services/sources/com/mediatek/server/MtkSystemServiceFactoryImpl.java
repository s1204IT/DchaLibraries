package com.mediatek.server;

import android.util.Log;
import com.android.server.power.ShutdownThread;
import com.mediatek.server.am.AmsExt;
import com.mediatek.server.am.AmsExtImpl;
import com.mediatek.server.anr.AnrManager;
import com.mediatek.server.anr.AnrManagerImpl;
import com.mediatek.server.display.MtkDisplayManagerService;
import com.mediatek.server.display.MtkDisplayManagerServiceImpl;
import com.mediatek.server.pm.PmsExt;
import com.mediatek.server.pm.PmsExtImpl;
import com.mediatek.server.powerhal.PowerHalManager;
import com.mediatek.server.powerhal.PowerHalManagerImpl;
import com.mediatek.server.ppl.MtkPplManager;
import com.mediatek.server.ppl.MtkPplManagerImpl;
import com.mediatek.server.wm.WindowManagerDebugger;
import com.mediatek.server.wm.WindowManagerDebuggerImpl;
import com.mediatek.server.wm.WmsExt;
import com.mediatek.server.wm.WmsExtImpl;

public class MtkSystemServiceFactoryImpl extends MtkSystemServiceFactory {
    private static final String TAG = "MtkSystemServiceFactoryImpl";
    private WmsExt mWmsExt = new WmsExtImpl();

    public ShutdownThread makeMtkShutdownThread() {
        Log.i(TAG, "Start : MTK Shutdown Thread");
        return new MtkShutdownThread();
    }

    public AnrManager makeAnrManager() {
        return new AnrManagerImpl();
    }

    public PmsExt makePmsExt() {
        return new PmsExtImpl();
    }

    public PowerHalManager makePowerHalManager() {
        return new PowerHalManagerImpl();
    }

    public MtkPplManager makeMtkPplManager() {
        return new MtkPplManagerImpl();
    }

    public AmsExt makeAmsExt() {
        return new AmsExtImpl();
    }

    public WmsExt makeWmsExt() {
        return this.mWmsExt;
    }

    public WindowManagerDebugger makeWindowManagerDebugger() {
        return new WindowManagerDebuggerImpl();
    }

    public MtkDisplayManagerService makeMtkDisplayManagerService() {
        return new MtkDisplayManagerServiceImpl();
    }
}
