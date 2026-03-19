package android.os;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.opengl.EGL14;
import android.provider.Settings;
import android.provider.SettingsStringUtil;
import android.util.Log;
import dalvik.system.VMRuntime;
import java.io.File;

public class GraphicsEnvironment {
    private static final boolean DEBUG = false;
    private static final String PROPERTY_GFX_DRIVER = "ro.gfx.driver.0";
    private static final String TAG = "GraphicsEnvironment";
    private static final GraphicsEnvironment sInstance = new GraphicsEnvironment();
    private ClassLoader mClassLoader;
    private String mDebugLayerPath;
    private String mLayerPath;

    private static native void setDebugLayers(String str);

    private static native void setDriverPath(String str);

    private static native void setLayerPaths(ClassLoader classLoader, String str);

    public static GraphicsEnvironment getInstance() {
        return sInstance;
    }

    public void setup(Context context) {
        setupGpuLayers(context);
        chooseDriver(context);
    }

    private static boolean isDebuggable(Context context) {
        return (context.getApplicationInfo().flags & 2) > 0;
    }

    public void setLayerPaths(ClassLoader classLoader, String str, String str2) {
        this.mClassLoader = classLoader;
        this.mLayerPath = str;
        this.mDebugLayerPath = str2;
    }

    private void setupGpuLayers(Context context) {
        String str = "";
        if (isDebuggable(context) && Settings.Global.getInt(context.getContentResolver(), Settings.Global.ENABLE_GPU_DEBUG_LAYERS, 0) != 0) {
            String string = Settings.Global.getString(context.getContentResolver(), Settings.Global.GPU_DEBUG_APP);
            String packageName = context.getPackageName();
            if (string != null && packageName != null && !string.isEmpty() && !packageName.isEmpty() && string.equals(packageName)) {
                Log.i(TAG, "GPU debug layers enabled for " + packageName);
                str = this.mDebugLayerPath + SettingsStringUtil.DELIMITER;
                String string2 = Settings.Global.getString(context.getContentResolver(), Settings.Global.GPU_DEBUG_LAYERS);
                Log.i(TAG, "Debug layer list: " + string2);
                if (string2 != null && !string2.isEmpty()) {
                    setDebugLayers(string2);
                }
            }
        }
        setLayerPaths(this.mClassLoader, str + this.mLayerPath);
    }

    private static void chooseDriver(Context context) {
        String str = SystemProperties.get(PROPERTY_GFX_DRIVER);
        if (str == null || str.isEmpty()) {
            return;
        }
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        if (applicationInfo.isPrivilegedApp()) {
            return;
        }
        if (applicationInfo.isSystemApp() && !applicationInfo.isUpdatedSystemApp()) {
            return;
        }
        try {
            ApplicationInfo applicationInfo2 = context.getPackageManager().getApplicationInfo(str, 1048576);
            String strChooseAbi = chooseAbi(applicationInfo2);
            if (strChooseAbi == null) {
                return;
            }
            if (applicationInfo2.targetSdkVersion < 26) {
                Log.w(TAG, "updated driver package is not known to be compatible with O");
                return;
            }
            setDriverPath(applicationInfo2.nativeLibraryDir + File.pathSeparator + applicationInfo2.sourceDir + "!/lib/" + strChooseAbi);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "driver package '" + str + "' not installed");
        }
    }

    public static void earlyInitEGL() {
        new Thread(new Runnable() {
            @Override
            public final void run() {
                EGL14.eglGetDisplay(0);
            }
        }, "EGL Init").start();
    }

    private static String chooseAbi(ApplicationInfo applicationInfo) {
        String currentInstructionSet = VMRuntime.getCurrentInstructionSet();
        if (applicationInfo.primaryCpuAbi != null && currentInstructionSet.equals(VMRuntime.getInstructionSet(applicationInfo.primaryCpuAbi))) {
            return applicationInfo.primaryCpuAbi;
        }
        if (applicationInfo.secondaryCpuAbi != null && currentInstructionSet.equals(VMRuntime.getInstructionSet(applicationInfo.secondaryCpuAbi))) {
            return applicationInfo.secondaryCpuAbi;
        }
        return null;
    }
}
