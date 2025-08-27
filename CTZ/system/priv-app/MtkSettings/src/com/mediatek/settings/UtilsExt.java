package com.mediatek.settings;

import android.content.Context;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import com.mediatek.settings.ext.IApnSettingsExt;
import com.mediatek.settings.ext.IDataUsageSummaryExt;
import com.mediatek.settings.ext.IDevExt;
import com.mediatek.settings.ext.IDeviceInfoSettingsExt;
import com.mediatek.settings.ext.IDisplaySettingsExt;
import com.mediatek.settings.ext.IRCSSettings;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.settings.ext.ISimManagementExt;
import com.mediatek.settings.ext.ISimRoamingExt;
import com.mediatek.settings.ext.IWWOPJoynSettingsExt;
import com.mediatek.settings.ext.IWfcSettingsExt;
import com.mediatek.settings.ext.IWifiExt;
import com.mediatek.settings.ext.IWifiSettingsExt;
import com.mediatek.settings.ext.IWifiTetherSettingsExt;
import com.mediatek.settings.ext.OpSettingsCustomizationUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/* loaded from: classes.dex */
public class UtilsExt {
    public static ArrayList<String> disableAppList = readFile("/vendor/etc/disableapplist.txt");
    private static IDataUsageSummaryExt sDataUsageSummaryExt;
    private static IDeviceInfoSettingsExt sDeviceInfoSettingsExt;
    private static ISimManagementExt sSimManagementExt;
    private static ISimRoamingExt sSimRoamingExt;
    private static IWfcSettingsExt sWfcSettingsExt;
    private static IWifiTetherSettingsExt sWifiTetherSettingsExt;

    public static IDataUsageSummaryExt getDataUsageSummaryExt(Context context) {
        if (sDataUsageSummaryExt == null) {
            synchronized (IDataUsageSummaryExt.class) {
                if (sDataUsageSummaryExt == null) {
                    sDataUsageSummaryExt = OpSettingsCustomizationUtils.getOpFactory(context).makeDataUsageSummaryExt();
                    log("[getDataUsageSummaryExt]create ext instance: " + sDataUsageSummaryExt);
                }
            }
        }
        return sDataUsageSummaryExt;
    }

    public static ISimManagementExt getSimManagementExt(Context context) {
        if (sSimManagementExt == null) {
            synchronized (ISimManagementExt.class) {
                if (sSimManagementExt == null) {
                    sSimManagementExt = OpSettingsCustomizationUtils.getOpFactory(context).makeSimManagementExt();
                    log("[getSimManagementExt] create ext instance: " + sSimManagementExt);
                }
            }
        }
        return sSimManagementExt;
    }

    public static ISimRoamingExt getSimRoamingExt(Context context) {
        if (sSimRoamingExt == null) {
            synchronized (ISimRoamingExt.class) {
                if (sSimRoamingExt == null) {
                    sSimRoamingExt = OpSettingsCustomizationUtils.getOpFactory(context).makeSimRoamingExt();
                    log("[getSimRoamingExt] create ext instance: " + sSimRoamingExt);
                }
            }
        }
        return sSimRoamingExt;
    }

    public static IWfcSettingsExt getWfcSettingsExt(Context context) {
        if (sWfcSettingsExt == null) {
            synchronized (IWfcSettingsExt.class) {
                if (sWfcSettingsExt == null) {
                    sWfcSettingsExt = OpSettingsCustomizationUtils.getOpFactory(context).makeWfcSettingsExt();
                    log("[getWfcSettingsExt] create ext instance: " + sWfcSettingsExt);
                }
            }
        }
        return sWfcSettingsExt;
    }

    public static IWifiTetherSettingsExt getWifiTetherSettingsExt(Context context) {
        if (sWifiTetherSettingsExt == null) {
            synchronized (IWifiTetherSettingsExt.class) {
                if (sWifiTetherSettingsExt == null) {
                    sWifiTetherSettingsExt = OpSettingsCustomizationUtils.getOpFactory(context).makeWifiTetherSettingsExt(context);
                    log("[getWifiTetherSettingsExt] create ext instance: " + sWifiTetherSettingsExt);
                }
            }
        }
        return sWifiTetherSettingsExt;
    }

    public static ISettingsMiscExt getMiscPlugin(Context context) {
        return OpSettingsCustomizationUtils.getOpFactory(context).makeSettingsMiscExt(context);
    }

    public static IDisplaySettingsExt getDisplaySettingsExt(Context context) {
        return OpSettingsCustomizationUtils.getOpFactory(context).makeDisplaySettingsExt(context);
    }

    public static IApnSettingsExt getApnSettingsExt(Context context) {
        return OpSettingsCustomizationUtils.getOpFactory(context).makeApnSettingsExt(context);
    }

    public static IRCSSettings getRCSSettingsExt(Context context) {
        return OpSettingsCustomizationUtils.getOpFactory(context).makeRCSSettings(context);
    }

    public static IWWOPJoynSettingsExt getWWOPJoynSettingsExt(Context context) {
        return OpSettingsCustomizationUtils.getOpFactory(context).makeWWOPJoynSettingsExt(context);
    }

    public static IWifiExt getWifiExt(Context context) {
        return OpSettingsCustomizationUtils.getOpFactory(context).makeWifiExt(context);
    }

    public static IWifiSettingsExt getWifiSettingsExt(Context context) {
        return OpSettingsCustomizationUtils.getOpFactory(context).makeWifiSettingsExt();
    }

    public static IDevExt getDevExt(Context context) {
        return OpSettingsCustomizationUtils.getOpFactory(context).makeDevExt(context);
    }

    public static IDeviceInfoSettingsExt getDeviceInfoSettingsExt(Context context) {
        if (sDeviceInfoSettingsExt == null) {
            synchronized (IDeviceInfoSettingsExt.class) {
                if (sDeviceInfoSettingsExt == null) {
                    sDeviceInfoSettingsExt = OpSettingsCustomizationUtils.getOpFactory(context).makeDeviceInfoSettingsExt();
                    log("[sDeviceInfoSettingsExt]create ext instance: " + sDeviceInfoSettingsExt);
                }
            }
        }
        return sDeviceInfoSettingsExt;
    }

    public static IDeviceInfoSettingsExt useDeviceInfoSettingsExt() {
        return sDeviceInfoSettingsExt;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [271=4, 274=4, 277=6, 279=4] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:20:0x0054 */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:22:0x0056 */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v0 */
    /* JADX WARN: Type inference failed for: r2v1 */
    /* JADX WARN: Type inference failed for: r2v2, types: [java.io.BufferedReader] */
    /* JADX WARN: Type inference failed for: r2v3 */
    /* JADX WARN: Type inference failed for: r7v0, types: [java.lang.String] */
    /* JADX WARN: Type inference failed for: r7v1 */
    /* JADX WARN: Type inference failed for: r7v2 */
    /* JADX WARN: Type inference failed for: r7v3, types: [java.io.FileReader] */
    /* JADX WARN: Type inference failed for: r7v5, types: [java.io.FileReader] */
    /* JADX WARN: Type inference failed for: r7v9, types: [java.io.FileReader, java.io.Reader] */
    private static ArrayList<String> readFile(String str) throws Throwable {
        BufferedReader bufferedReader;
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.clear();
        File file = new File((String) str);
        ?? r2 = 0;
        r2 = 0;
        try {
            try {
                if (!file.exists()) {
                    Log.d("UtilsExt", "file in " + ((String) str) + " does not exist!");
                    return null;
                }
                str = new FileReader(file);
                try {
                    bufferedReader = new BufferedReader(str);
                    while (true) {
                        try {
                            String line = bufferedReader.readLine();
                            if (line != null) {
                                Log.d("UtilsExt", " read line " + line);
                                arrayList.add(line);
                            } else {
                                try {
                                    break;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (IOException e2) {
                            e = e2;
                            Log.d("UtilsExt", "IOException");
                            e.printStackTrace();
                            if (bufferedReader != null) {
                                try {
                                    bufferedReader.close();
                                } catch (IOException e3) {
                                    e3.printStackTrace();
                                    return null;
                                }
                            }
                            if (str != 0) {
                                str.close();
                            }
                            return null;
                        }
                    }
                    bufferedReader.close();
                    str.close();
                    return arrayList;
                } catch (IOException e4) {
                    e = e4;
                    bufferedReader = null;
                } catch (Throwable th) {
                    th = th;
                    if (r2 != 0) {
                        try {
                            r2.close();
                        } catch (IOException e5) {
                            e5.printStackTrace();
                            throw th;
                        }
                    }
                    if (str != 0) {
                        str.close();
                    }
                    throw th;
                }
            } catch (IOException e6) {
                e = e6;
                str = 0;
                bufferedReader = null;
            } catch (Throwable th2) {
                th = th2;
                str = 0;
            }
        } catch (Throwable th3) {
            th = th3;
            r2 = file;
        }
    }

    public static boolean shouldDisableForAutoSanity() {
        boolean zEquals = SystemProperties.get("ro.mtk.autosanity").equals("1");
        String str = SystemProperties.get("ro.build.type", "");
        Log.d("UtilsExt", "autoSanity: " + zEquals + " buildType: " + str);
        if (zEquals && !TextUtils.isEmpty(str) && str.endsWith("eng")) {
            Log.d("UtilsExt", "ShouldDisableForAutoSanity()...");
            return true;
        }
        return false;
    }

    private static void log(String str) {
        Log.d("UtilsExt", str);
    }
}
