package com.mediatek.server;

import android.content.Context;
import android.net.INetworkPolicyManager;
import android.net.INetworkStatsService;
import android.os.IInterface;
import android.os.INetworkManagementService;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Slog;
import android.util.TimingsTraceLog;
import com.android.server.NetworkManagementService;
import com.android.server.SystemService;
import com.android.server.SystemServiceManager;
import com.android.server.net.NetworkPolicyManagerService;
import com.android.server.net.NetworkStatsService;
import com.mediatek.search.SearchEngineManagerService;
import dalvik.system.PathClassLoader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;

public class MtkSystemServerImpl extends MtkSystemServer {
    private static TimingsTraceLog BOOT_TIMINGS_TRACE_LOG = null;
    private static final String DATASHPAING_SERVICE_CLASS = "com.mediatek.datashaping.DataShapingService";
    private static final String FULLSCREEN_SWITCH_SERVICE_CLASS = "com.mediatek.fullscreenswitch.FullscreenSwitchService";
    private static final String HDMI_LOCAL_SERVICE_CLASS = "com.mediatek.hdmilocalservice.HdmiLocalService";
    private static final String MTK_ALARM_MANAGER_SERVICE_CLASS = "com.mediatek.server.MtkAlarmManagerService";
    private static final String MTK_FM_RADIO_SERVICE_CLASS = "com.mediatek.fmradioservice.FmRadioService";
    private static final String MTK_STORAGE_MANAGER_SERVICE_CLASS = "com.mediatek.server.MtkStorageManagerService$MtkStorageManagerServiceLifecycle";
    private static final String POWER_HAL_SERVICE_CLASS = "com.mediatek.powerhalservice.PowerHalMgrService";
    private static final String SEARCH_ENGINE_SERVICE_CLASS = "com.mediatek.search.SearchEngineManagerService";
    private static final String TAG = "MtkSystemServerImpl";
    private boolean mMTPROF_disable = false;
    private Context mSystemContext;
    private SystemServiceManager mSystemServiceManager;

    public void setPrameters(TimingsTraceLog timingsTraceLog, SystemServiceManager systemServiceManager, Context context) {
        BOOT_TIMINGS_TRACE_LOG = timingsTraceLog;
        this.mSystemServiceManager = systemServiceManager;
        this.mSystemContext = context;
    }

    public void startMtkBootstrapServices() {
        Slog.i(TAG, "startMtkBootstrapServices");
    }

    public void startMtkCoreServices() {
        Slog.i(TAG, "startMtkCoreServices ");
    }

    public void startMtkOtherServices(boolean z) {
        Context context = this.mSystemContext;
        Slog.i(TAG, "startOtherMtkService ");
        boolean z2 = SystemProperties.getBoolean("config.disable_searchmanager", false);
        boolean z3 = SystemProperties.getBoolean("config.disable_noncore", false);
        boolean z4 = !"".equals(SystemProperties.get("ro.vendor.mtk_tb_hdmi"));
        if (!z) {
            traceBeginAndSlog("StartBenesseExtensionService");
            ServiceManager.addService("benesse_extension", new BenesseExtensionService(context));
            traceEnd();
        }
        if (!z3 && !z2) {
            traceBeginAndSlog("StartSearchEngineManagerService");
            try {
                ServiceManager.addService("search_engine_service", new SearchEngineManagerService(context));
            } catch (Throwable th) {
                Slog.e(TAG, "StartSearchEngineManagerService " + th.toString());
            }
        }
        if ("1".equals(SystemProperties.get("persist.vendor.datashaping"))) {
            traceBeginAndSlog("StartDataShapingService");
            try {
                startService(DATASHPAING_SERVICE_CLASS);
            } catch (Throwable th2) {
                reportWtf("starting DataShapingService", th2);
            }
            traceEnd();
        }
        try {
            if (Class.forName("com.mediatek.fmradio.FmRadioPackageManager") != null) {
                traceBeginAndSlog("addService FmRadioService");
                try {
                    startService(MTK_FM_RADIO_SERVICE_CLASS);
                } catch (Throwable th3) {
                    reportWtf("starting FmRadioService", th3);
                }
                traceEnd();
            }
        } catch (Exception e) {
            Slog.d(TAG, "com.mediatek.fmradio.FmRadioPackageManager not found ");
        }
        if (z4) {
            traceBeginAndSlog("StartHdmiLocalService");
            try {
                startService(HDMI_LOCAL_SERVICE_CLASS);
            } catch (Throwable th4) {
                reportWtf("starting HdmiLocalService", th4);
            }
            traceEnd();
        }
        traceBeginAndSlog("StartPowerHalMgrService");
        try {
            startService(POWER_HAL_SERVICE_CLASS);
        } catch (Throwable th5) {
            reportWtf("starting PowerHalMgrService", th5);
        }
        traceEnd();
        if ("1".equals(SystemProperties.get("ro.vendor.fullscreen_switch"))) {
            traceBeginAndSlog("addService FullscreenSwitchService");
            try {
                ServiceManager.addService("FullscreenSwitchService", ((IInterface) Class.forName(FULLSCREEN_SWITCH_SERVICE_CLASS).getConstructor(Context.class).newInstance(context)).asBinder());
            } catch (Throwable th6) {
                reportWtf("starting FullscreenSwitchService", th6);
            }
            traceEnd();
        }
    }

    public Object getMtkConnectivityService(NetworkManagementService networkManagementService, NetworkStatsService networkStatsService, NetworkPolicyManagerService networkPolicyManagerService) {
        try {
            Constructor constructor = new PathClassLoader("/system/framework/mediatek-framework-net.jar", this.mSystemContext.getClassLoader()).loadClass("com.android.server.MtkConnectivityService").getConstructor(Context.class, INetworkManagementService.class, INetworkStatsService.class, INetworkPolicyManager.class);
            constructor.setAccessible(true);
            return constructor.newInstance(this.mSystemContext, networkManagementService, networkStatsService, networkPolicyManagerService);
        } catch (Exception e) {
            Slog.e(TAG, "No MtkConnectivityService! Used AOSP for instead!", e);
            return null;
        }
    }

    public boolean startMtkAlarmManagerService() {
        traceBeginAndSlog("startMtkAlarmManagerService");
        try {
            startService(MTK_ALARM_MANAGER_SERVICE_CLASS);
            traceEnd();
            return true;
        } catch (Throwable th) {
            Slog.e(TAG, "Exception while starting MtkAlarmManagerService" + th.toString());
            return false;
        }
    }

    public boolean startMtkStorageManagerService() {
        if (!SystemProperties.get("ro.vendor.mtk_privacy_protection_lock").equals("1")) {
            Slog.i(TAG, "PPL not supported, retruning, will start AOSP StorageManagerService");
            return false;
        }
        traceBeginAndSlog("StartMtkStorageManagerService");
        try {
            startService(MTK_STORAGE_MANAGER_SERVICE_CLASS);
            traceEnd();
            return true;
        } catch (Throwable th) {
            Slog.e(TAG, "Exception while starting MtkStorageManagerService" + th.toString());
            return false;
        }
    }

    private SystemService startService(String str) {
        try {
            return this.mSystemServiceManager.startService(Class.forName(str));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to create service " + str + ": service class not found, usually indicates that the caller should have called PackageManager.hasSystemFeature() to check whether the feature is available on this device before trying to start the services that implement it", e);
        }
    }

    private static void traceBeginAndSlog(String str) {
        Slog.i(TAG, str);
        BOOT_TIMINGS_TRACE_LOG.traceBegin(str);
    }

    private static void traceEnd() {
        BOOT_TIMINGS_TRACE_LOG.traceEnd();
    }

    private void reportWtf(String str, Throwable th) {
        Slog.w(TAG, "***********************************************");
        Slog.wtf(TAG, "BOOT FAILURE " + str, th);
    }

    public void addBootEvent(String str) throws Throwable {
        FileOutputStream fileOutputStream;
        if (this.mMTPROF_disable) {
            return;
        }
        ?? r0 = 0;
        r0 = 0;
        FileOutputStream fileOutputStream2 = null;
        FileOutputStream fileOutputStream3 = null;
        try {
            try {
                try {
                    fileOutputStream = new FileOutputStream("/proc/bootprof");
                } catch (Throwable th) {
                    th = th;
                }
            } catch (FileNotFoundException e) {
                e = e;
            } catch (IOException e2) {
                e = e2;
            }
            try {
                fileOutputStream.write(str.getBytes());
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch (FileNotFoundException e3) {
                e = e3;
                fileOutputStream2 = fileOutputStream;
                Slog.e("BOOTPROF", "Failure open /proc/bootprof, not found!", e);
                r0 = fileOutputStream2;
                if (fileOutputStream2 != null) {
                    fileOutputStream2.close();
                    r0 = fileOutputStream2;
                }
            } catch (IOException e4) {
                e = e4;
                fileOutputStream3 = fileOutputStream;
                Slog.e("BOOTPROF", "Failure open /proc/bootprof entry", e);
                r0 = fileOutputStream3;
                if (fileOutputStream3 != null) {
                    fileOutputStream3.close();
                    r0 = fileOutputStream3;
                }
            } catch (Throwable th2) {
                th = th2;
                r0 = fileOutputStream;
                if (r0 != 0) {
                    try {
                        r0.close();
                    } catch (IOException e5) {
                        Slog.e("BOOTPROF", "Failure close /proc/bootprof entry", e5);
                    }
                }
                throw th;
            }
        } catch (IOException e6) {
            Slog.e("BOOTPROF", "Failure close /proc/bootprof entry", e6);
            r0 = "BOOTPROF";
        }
    }
}
