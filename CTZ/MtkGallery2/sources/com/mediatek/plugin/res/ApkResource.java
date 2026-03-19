package com.mediatek.plugin.res;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import com.mediatek.plugin.utils.Log;
import com.mediatek.plugin.utils.ReflectUtils;
import com.mediatek.plugin.utils.TraceHelper;
import java.lang.reflect.Method;

public class ApkResource implements IResource {
    private static final String FORMAT_DRAWABLE = "@drawable/";
    private static final String FORMAT_STRING = "@string/";
    private static final String TAG = "PluginManager/ApkResource";
    private AssetManager mAssetManager;
    private Configuration mConfiguration;
    protected String mFilePath;
    private DisplayMetrics mMetrics;
    protected String mPackageProcessName;
    private Resources mResources;

    public ApkResource(Context context, String str, String str2) {
        this.mFilePath = null;
        this.mPackageProcessName = null;
        this.mMetrics = context.getResources().getDisplayMetrics();
        this.mConfiguration = context.getResources().getConfiguration();
        this.mFilePath = str;
        this.mPackageProcessName = str2;
        Log.d(TAG, "<ApkResParser> mPackageProcessName = " + this.mPackageProcessName);
    }

    public Resources getResources() {
        if (this.mResources == null) {
            getResource();
        }
        return this.mResources;
    }

    @Override
    public String getString(String str) {
        Log.d(TAG, "<getString> value = " + str);
        if (str == null || !str.startsWith(FORMAT_STRING)) {
            return str;
        }
        String strSubstring = str.substring(str.indexOf("@") + 1, str.indexOf("/"));
        Log.d(TAG, "<getString> defType = " + strSubstring);
        String strSubstring2 = str.substring(str.indexOf("/") + 1);
        Log.d(TAG, "<getString> name = " + strSubstring2);
        int identifier = getResource().getIdentifier(strSubstring2, strSubstring, this.mPackageProcessName);
        Log.d(TAG, "<getString> id = " + identifier);
        return getResource().getString(identifier);
    }

    @Override
    public Drawable getDrawable(String str) {
        Log.d(TAG, "<getDrawable> value = " + str);
        if (str == null || !str.startsWith(FORMAT_DRAWABLE)) {
            return null;
        }
        String strSubstring = str.substring(str.indexOf("@") + 1, str.indexOf("/"));
        Log.d(TAG, "<getDrawable> defType = " + strSubstring);
        String strSubstring2 = str.substring(str.indexOf("/") + 1);
        Log.d(TAG, "<getDrawable> name = " + strSubstring2);
        int identifier = getResource().getIdentifier(strSubstring2, strSubstring, this.mPackageProcessName);
        Log.d(TAG, "<getDrawable> id = " + identifier);
        if (identifier <= 0) {
            return null;
        }
        return getResource().getDrawable(identifier);
    }

    private Resources getResource() {
        if (this.mResources == null) {
            TraceHelper.beginSection(">>>>ApkResource-getResource");
            this.mAssetManager = (AssetManager) ReflectUtils.createInstance(ReflectUtils.getConstructor((Class<?>) AssetManager.class, (Class<?>[]) new Class[0]), new Object[0]);
            Method method = ReflectUtils.getMethod(this.mAssetManager.getClass(), "addAssetPath", String.class);
            Log.d(TAG, "<getResource> addAssertPath " + this.mFilePath);
            ReflectUtils.callMethodOnObject(this.mAssetManager, method, this.mFilePath);
            Resources resources = new Resources(this.mAssetManager, this.mMetrics, this.mConfiguration);
            Log.d(TAG, "<getResource> resources " + resources);
            this.mResources = resources;
            TraceHelper.endSection();
        }
        return this.mResources;
    }

    public AssetManager getAssetManager() {
        if (this.mAssetManager == null) {
            getResources();
        }
        return this.mAssetManager;
    }

    @Override
    public String[] getString(String str, String str2) {
        if (str != null) {
            return str.split(str2);
        }
        return null;
    }
}
