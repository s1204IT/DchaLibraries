package com.mediatek.settings.ext;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v7.preference.PreferenceGroup;
import android.util.Log;
import dalvik.system.PathClassLoader;
import java.lang.reflect.InvocationTargetException;

/* loaded from: classes.dex */
public class DefaultPplSettingsEntryExt implements IPplSettingsEntryExt {
    private static final String APK_PATH = "/system/priv-app/PrivacyProtectionLock/PrivacyProtectionLock.apk";
    private static final String PKG_NAME = "com.mediatek.ppl";
    private static final String TAG = "PPL/PplSettingsEntryExt";
    private static final String TARGET_NAME = "com.mediatek.ppl.ext.PplSettingsEntryPlugin";
    private static Object mPplExt;

    public static IPplSettingsEntryExt getInstance(Context context) throws ClassNotFoundException {
        if (context == null) {
            Log.e(TAG, "[getInstance] context is null !!!");
            return new DefaultPplSettingsEntryExt();
        }
        Log.d(TAG, "[getInstance] context=" + context);
        try {
            PathClassLoader pathClassLoader = new PathClassLoader(APK_PATH, context.getClassLoader());
            Class<?> clsLoadClass = pathClassLoader.loadClass(TARGET_NAME);
            Log.d(TAG, "Load class : com.mediatek.ppl.ext.PplSettingsEntryPlugin successfully with classLoader:" + pathClassLoader);
            mPplExt = clsLoadClass.getConstructor(Context.class).newInstance(context.createPackageContext(PKG_NAME, 3));
            Log.d(TAG, "[getInstance] return plugin:" + mPplExt);
            return (IPplSettingsEntryExt) mPplExt;
        } catch (PackageManager.NameNotFoundException | ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            Log.d(TAG, "Exception occurs when initial instance", e);
            Log.d(TAG, "[getInstance] return default()");
            return new DefaultPplSettingsEntryExt();
        }
    }

    private DefaultPplSettingsEntryExt() {
    }

    @Override // com.mediatek.settings.ext.IPplSettingsEntryExt
    public void addPplPrf(PreferenceGroup preferenceGroup) {
        Log.d(TAG, "addPplPrf() default");
    }

    @Override // com.mediatek.settings.ext.IPplSettingsEntryExt
    public void enablerResume() {
        Log.d(TAG, "enablerResume() default");
    }

    @Override // com.mediatek.settings.ext.IPplSettingsEntryExt
    public void enablerPause() {
        Log.d(TAG, "enablerPause() default");
    }
}
