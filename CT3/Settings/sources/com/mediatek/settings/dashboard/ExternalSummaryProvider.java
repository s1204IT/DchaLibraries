package com.mediatek.settings.dashboard;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settingslib.drawer.Tile;
import dalvik.system.PathClassLoader;
import java.lang.reflect.Field;
import java.util.HashMap;

public class ExternalSummaryProvider {
    private static final String TAG = ExternalSummaryProvider.class.getSimpleName();
    private static HashMap<String, PathClassLoader> sLoaderMap = new HashMap<>();

    public static SummaryLoader.SummaryProvider createExternalSummaryProvider(Activity activity, SummaryLoader summaryLoader, Tile tile) {
        Bundle metaData = tile.metaData;
        Log.d(TAG, "createExternalSummaryProvider for: " + tile.intent.getComponent());
        if (metaData == null) {
            Log.d(TAG, "No metadata specified for " + tile.intent.getComponent());
            return null;
        }
        String clsName = metaData.getString("com.mediatek.settings.summary");
        if (clsName == null) {
            Log.d(TAG, "No summary provider specified for " + tile.intent.getComponent());
            return null;
        }
        ComponentName cn = tile.intent.getComponent();
        String pkgName = cn.getPackageName();
        try {
            PathClassLoader newLoader = sLoaderMap.get(pkgName);
            if (newLoader == null) {
                String sourceDir = activity.getPackageManager().getApplicationInfo(pkgName, 0).sourceDir;
                Log.d(TAG, "clsName: " + clsName + " sourceDir: " + sourceDir);
                newLoader = new PathClassLoader(sourceDir, activity.getClassLoader());
                sLoaderMap.put(pkgName, newLoader);
            }
            Class<?> cls = newLoader.loadClass(clsName);
            Field field = cls.getField("SUMMARY_PROVIDER_FACTORY");
            SummaryLoader.SummaryProviderFactory factory = (SummaryLoader.SummaryProviderFactory) field.get(null);
            activity.createPackageContext(cn.getPackageName(), 3);
            return factory.createSummaryProvider(activity, summaryLoader);
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "Couldn't find package " + cn.getPackageName(), e);
            return null;
        } catch (ClassCastException e2) {
            Log.d(TAG, "Couldn't cast SUMMARY_PROVIDER_FACTORY", e2);
            return null;
        } catch (ClassNotFoundException e3) {
            Log.d(TAG, "Couldn't find " + clsName, e3);
            return null;
        } catch (IllegalAccessException e4) {
            Log.d(TAG, "Couldn't get SUMMARY_PROVIDER_FACTORY", e4);
            return null;
        } catch (NoSuchFieldException e5) {
            Log.d(TAG, "Couldn't find SUMMARY_PROVIDER_FACTORY", e5);
            return null;
        }
    }
}
