package com.mediatek.server.telecom.ext;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telecom.Log;

public final class ExtensionManager {
    private static Context sApplicationContext;
    private static ICallMgrExt sCallMgrExt;
    private static IDigitsUtilExt sDigitsUtilExt;
    private static IGttUtilExt sGttUtilExt;
    private static IRttUtilExt sRttUtilExt;

    private static void log(String str) {
        Log.d("ExtensionManager", str, new Object[0]);
    }

    public static void registerApplicationContext(Context context) {
        if (sApplicationContext == null) {
            sApplicationContext = context;
            sApplicationContext.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context2, Intent intent) {
                    ExtensionManager.log("[onReceive] Clear plugin due to " + intent.getAction());
                    synchronized (ICallMgrExt.class) {
                        ICallMgrExt unused = ExtensionManager.sCallMgrExt = null;
                    }
                    synchronized (IRttUtilExt.class) {
                        IRttUtilExt unused2 = ExtensionManager.sRttUtilExt = null;
                    }
                    synchronized (IDigitsUtilExt.class) {
                        IDigitsUtilExt unused3 = ExtensionManager.sDigitsUtilExt = null;
                    }
                    synchronized (IGttUtilExt.class) {
                        IGttUtilExt unused4 = ExtensionManager.sGttUtilExt = null;
                    }
                }
            }, new IntentFilter("com.mediatek.common.carrierexpress.operator_config_changed"));
        }
    }

    public static ICallMgrExt getCallMgrExt() {
        if (sCallMgrExt == null) {
            synchronized (ICallMgrExt.class) {
                if (sCallMgrExt == null) {
                    sCallMgrExt = OpTelecomCustomizationUtils.getOpFactory(sApplicationContext).makeCallMgrExt();
                    log("[getCallMgrExt]create ext instance: " + sCallMgrExt);
                }
            }
        }
        return sCallMgrExt;
    }

    public static IGttEventExt makeGttEventExt() {
        return CommonTelecomCustomizationUtils.getOpFactory(sApplicationContext).makeGttEventExt();
    }

    public static IRttUtilExt getRttUtilExt() {
        if (sRttUtilExt == null) {
            synchronized (IRttUtilExt.class) {
                if (sRttUtilExt == null) {
                    sRttUtilExt = CommonTelecomCustomizationUtils.getOpFactory(sApplicationContext).makeRttUtilExt();
                    log("[getRttUtilExt] create ext instance: " + sRttUtilExt);
                }
            }
        }
        return sRttUtilExt;
    }

    public static IDigitsUtilExt getDigitsUtilExt() {
        if (sDigitsUtilExt == null) {
            synchronized (IDigitsUtilExt.class) {
                if (sDigitsUtilExt == null) {
                    sDigitsUtilExt = OpTelecomCustomizationUtils.getOpFactory(sApplicationContext).makeDigitsUtilExt();
                    log("[getDigitsUtilExt] create ext instance: " + sDigitsUtilExt);
                }
            }
        }
        return sDigitsUtilExt;
    }

    public static IGttUtilExt getGttUtilExt() {
        if (sGttUtilExt == null) {
            synchronized (IGttUtilExt.class) {
                if (sGttUtilExt == null) {
                    sGttUtilExt = CommonTelecomCustomizationUtils.getOpFactory(sApplicationContext).makeGttUtilExt();
                    log("[getGttUtilExt] create ext instance: " + sGttUtilExt);
                }
            }
        }
        return sGttUtilExt;
    }

    public static IRttEventExt makeRttEventExt() {
        return CommonTelecomCustomizationUtils.getOpFactory(sApplicationContext).makeRttEventExt();
    }
}
