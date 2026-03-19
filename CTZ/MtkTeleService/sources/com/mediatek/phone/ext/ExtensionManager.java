package com.mediatek.phone.ext;

import android.app.Application;
import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;
import com.android.phone.settings.SettingsConstants;
import java.lang.reflect.InvocationTargetException;

public final class ExtensionManager {
    private static final String LOG_TAG = "ExtensionManager";
    private static IAccessibilitySettingsExt sAccessibilitySettingsExt;
    private static ICallFeaturesSettingExt sCallFeaturesSettingExt;
    private static Context sContext;
    private static IDigitsUtilExt sDigitsUtilExt;
    private static IEmergencyDialerExt sEmergencyDialerExt;
    private static IGttInfoExt sGttInfoExt;
    private static IIncomingCallExt sIncomingCallExt;
    private static IMmiCodeExt sMmiCodeExt;
    private static IMobileNetworkSettingsExt sMobileNetworkSettingsExt;
    private static INetworkSettingExt sNetworkSettingExt;
    private static IPhoneGlobalsExt sPhoneGlobalsExt;
    private static IRttUtilExt sRttUtilExt;
    private static ISimDialogExt sSimDialogExt;
    private static ITtyModeListPreferenceExt sTtyModeListPreferenceExt;

    private ExtensionManager() {
    }

    private static void log(String str) {
        Log.d(LOG_TAG, str);
    }

    public static void init(Application application) {
        sContext = application.getApplicationContext();
    }

    public static void resetApplicationContext(Context context) {
        sContext = context;
        log("resetApplicationContext");
        OpPhoneCustomizationUtils.resetOpFactory(sContext);
        synchronized (IMobileNetworkSettingsExt.class) {
            sMobileNetworkSettingsExt = null;
        }
        synchronized (INetworkSettingExt.class) {
            sNetworkSettingExt = null;
        }
        synchronized (ISimDialogExt.class) {
            sSimDialogExt = null;
        }
        synchronized (IAccessibilitySettingsExt.class) {
            sAccessibilitySettingsExt = null;
        }
        synchronized (ICallFeaturesSettingExt.class) {
            sCallFeaturesSettingExt = null;
        }
        synchronized (IMmiCodeExt.class) {
            sMmiCodeExt = null;
        }
        synchronized (IEmergencyDialerExt.class) {
            sEmergencyDialerExt = null;
        }
        synchronized (IGttInfoExt.class) {
            sGttInfoExt = null;
        }
        synchronized (IRttUtilExt.class) {
            sRttUtilExt = null;
        }
        synchronized (IDigitsUtilExt.class) {
            sDigitsUtilExt = null;
        }
        synchronized (IIncomingCallExt.class) {
            sIncomingCallExt = null;
        }
        synchronized (ITtyModeListPreferenceExt.class) {
            sTtyModeListPreferenceExt = null;
        }
    }

    public static IMobileNetworkSettingsExt getMobileNetworkSettingsExt() {
        if (sMobileNetworkSettingsExt == null) {
            synchronized (IMobileNetworkSettingsExt.class) {
                if (sMobileNetworkSettingsExt == null) {
                    sMobileNetworkSettingsExt = OpPhoneCustomizationUtils.getOpFactory(sContext).makeMobileNetworkSettingsExt();
                    log("[sMobileNetworkSettingsExt] create ext instance: " + sMobileNetworkSettingsExt);
                }
            }
        }
        return sMobileNetworkSettingsExt;
    }

    public static INetworkSettingExt getNetworkSettingExt() {
        if (sNetworkSettingExt == null) {
            synchronized (INetworkSettingExt.class) {
                if (sNetworkSettingExt == null) {
                    sNetworkSettingExt = OpPhoneCustomizationUtils.getOpFactory(sContext).makeNetworkSettingExt();
                    log("[sNetworkSettingExt] create ext instance: " + sNetworkSettingExt);
                }
            }
        }
        return sNetworkSettingExt;
    }

    public static ISimDialogExt getSimDialogExt() {
        if (sSimDialogExt == null) {
            synchronized (ISimDialogExt.class) {
                if (sSimDialogExt == null) {
                    sSimDialogExt = OpPhoneCustomizationUtils.getOpFactory(sContext).makeSimDialogExt();
                    log("[sSimDialogExt] create ext instance: " + sSimDialogExt);
                }
            }
        }
        return sSimDialogExt;
    }

    public static IAccessibilitySettingsExt getAccessibilitySettingsExt() {
        if (sAccessibilitySettingsExt == null) {
            synchronized (IAccessibilitySettingsExt.class) {
                if (sAccessibilitySettingsExt == null) {
                    sAccessibilitySettingsExt = OpPhoneCustomizationUtils.getOpFactory(sContext).makeAccessibilitySettingsExt();
                    log("[getAccessibilitySettingsExt]create ext instance: " + sAccessibilitySettingsExt);
                }
            }
        }
        return sAccessibilitySettingsExt;
    }

    public static ITtyModeListPreferenceExt getTtyModeListPreferenceExt() {
        if (sTtyModeListPreferenceExt == null) {
            synchronized (ITtyModeListPreferenceExt.class) {
                if (sTtyModeListPreferenceExt == null) {
                    sTtyModeListPreferenceExt = OpPhoneCustomizationUtils.getOpFactory(sContext).makeTtyModeListPreferenceExt();
                    log("[getTtyModeListPreferenceExt]create ext instance: " + sTtyModeListPreferenceExt);
                }
            }
        }
        return sTtyModeListPreferenceExt;
    }

    public static ICallFeaturesSettingExt getCallFeaturesSettingExt() {
        if (sCallFeaturesSettingExt == null) {
            synchronized (ICallFeaturesSettingExt.class) {
                if (sCallFeaturesSettingExt == null) {
                    sCallFeaturesSettingExt = OpPhoneCustomizationUtils.getOpFactory(sContext).makeCallFeaturesSettingExt();
                    log("[getCallFeaturesSettingExt]create ext instance: " + sCallFeaturesSettingExt);
                }
            }
        }
        return sCallFeaturesSettingExt;
    }

    public static IMmiCodeExt getMmiCodeExt() {
        if (sMmiCodeExt == null) {
            synchronized (IMmiCodeExt.class) {
                if (sMmiCodeExt == null) {
                    sMmiCodeExt = OpPhoneCustomizationUtils.getOpFactory(sContext).makeMmiCodeExt();
                    log("[getMmiCodeExt]create ext instance: " + sMmiCodeExt);
                }
            }
        }
        return sMmiCodeExt;
    }

    public static IEmergencyDialerExt getEmergencyDialerExt() {
        if (sEmergencyDialerExt == null) {
            synchronized (IEmergencyDialerExt.class) {
                if (sEmergencyDialerExt == null) {
                    sEmergencyDialerExt = OpPhoneCustomizationUtils.getOpFactory(sContext).makeEmergencyDialerExt();
                    log("[sEmergencyDialerExt] create ext instance: " + sEmergencyDialerExt);
                }
            }
        }
        return sEmergencyDialerExt;
    }

    public static IGttInfoExt getGttInfoExt() {
        if (sGttInfoExt == null) {
            synchronized (IGttInfoExt.class) {
                if (sGttInfoExt == null) {
                    sGttInfoExt = CommonPhoneCustomizationUtils.getOpFactory(sContext).makeGttInfoExt();
                    log("[sGttInfoExt] create ext instance: " + sGttInfoExt);
                }
            }
        }
        return sGttInfoExt;
    }

    public static IRttUtilExt getRttUtilExt() {
        if (sRttUtilExt == null) {
            synchronized (IRttUtilExt.class) {
                if (sRttUtilExt == null) {
                    sRttUtilExt = CommonPhoneCustomizationUtils.getOpFactory(sContext).makeRttUtilExt();
                    log("[sRttUtilExt] create ext instance: " + sRttUtilExt);
                }
            }
        }
        return sRttUtilExt;
    }

    public static IDigitsUtilExt getDigitsUtilExt() {
        if (sDigitsUtilExt == null) {
            synchronized (IDigitsUtilExt.class) {
                if (sDigitsUtilExt == null) {
                    sDigitsUtilExt = OpPhoneCustomizationUtils.getOpFactory(sContext).makeDigitsUtilExt();
                    log("[sDigitsUtilExt] create ext instance: " + sDigitsUtilExt);
                }
            }
        }
        return sDigitsUtilExt;
    }

    public static IIncomingCallExt getIncomingCallExt() {
        if (sIncomingCallExt == null) {
            synchronized (IIncomingCallExt.class) {
                if (sIncomingCallExt == null) {
                    sIncomingCallExt = OpPhoneCustomizationUtils.getOpFactory(sContext).makeIncomingCallExt();
                    log("[sIncomingCallExt] create ext instance: " + sIncomingCallExt);
                }
            }
        }
        return sIncomingCallExt;
    }

    public static IPhoneGlobalsExt getPhoneGlobalsExt() {
        if (sPhoneGlobalsExt == null) {
            synchronized (IPhoneGlobalsExt.class) {
                if (sPhoneGlobalsExt == null) {
                    sPhoneGlobalsExt = OpPhoneCustomizationUtils.getOpFactory(sContext).makePhoneGlobalsExt();
                    log("[sPhoneGlobalsExt] create ext instance: " + sPhoneGlobalsExt);
                }
            }
        }
        return sPhoneGlobalsExt;
    }

    public static void initPhoneHelper() {
        if ("OP01".equals(SystemProperties.get("persist.vendor.operator.optr", "")) || SettingsConstants.DUA_VAL_ON.equals(SystemProperties.get("ro.vendor.cmcc_light_cust_support", ""))) {
            try {
                Class.forName("cn.richinfo.dm.CtmApplication").getMethod("getInstance", Application.class).invoke(null, (Application) sContext);
            } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }
}
