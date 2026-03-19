package com.mediatek.common;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;
import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class MPlugin {
    static final boolean $assertionsDisabled;
    private static final int BUF_SIZE = 32768;
    private static HashMap<String, String> CLASSLOADER_CACHE = null;
    private static final boolean DEBUG = true;
    private static final boolean DEBUG_GETINSTANCE = false;
    private static final boolean DEBUG_PERFORMANCE = false;
    private static HashMap<Key, PathClassLoader> PATHCLASSLOADER_CACHE = null;
    private static final String TAG = "MPlugin";
    private static ClassLoader mClassLoader;
    private static Context mHostContext;
    private static Class<?> mPluginClazz;
    private static Context mPluginContext;
    private static PathClassLoader mPluginloader;

    static {
        $assertionsDisabled = MPlugin.class.desiredAssertionStatus() ? false : DEBUG;
        mPluginloader = null;
        mPluginClazz = null;
        CLASSLOADER_CACHE = new HashMap<>();
        PATHCLASSLOADER_CACHE = new HashMap<>();
    }

    private static class LoadedDex {
        private File mDexFile;
        private ZipEntry mZipEntry;

        private LoadedDex(File file, String str) {
            this.mDexFile = new File(file, str);
        }

        private LoadedDex(File file, String str, ZipEntry zipEntry) {
            this.mDexFile = new File(file, str);
            this.mZipEntry = zipEntry;
        }
    }

    private static class Key {
        private ClassLoader mLoader;
        private String mSourcePath;

        Key(String str, ClassLoader classLoader) {
            this.mSourcePath = str;
            this.mLoader = classLoader;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return MPlugin.DEBUG;
            }
            if ((obj instanceof Key) && this.mLoader.equals(obj.mLoader) && this.mSourcePath.equals(obj.mSourcePath)) {
                return MPlugin.DEBUG;
            }
            return false;
        }

        public int hashCode() {
            return (this.mSourcePath.hashCode() * 31) + this.mLoader.hashCode();
        }
    }

    public static synchronized <T> T createInstance(String str, Context context) {
        T t = null;
        synchronized (MPlugin.class) {
            mHostContext = context;
            Log.d(TAG, str + "Clazz exists on mapping table : " + PluginLoader.getContainsKey(str));
            if (PluginLoader.getContainsKey(str)) {
                PluginInfo value = PluginLoader.getValue(str);
                if (MPluginGuard.checkAuthorizedApk(new File(value.getApkName()), value.getPackageName(), DEBUG) == 1) {
                    t = (T) getInstanceHelper(value, context);
                } else {
                    Log.w(TAG, "The plugin '" + value.getPackageName() + "' did not signed by legal certificate");
                }
            } else {
                Log.e(TAG, "Unsupported class: " + str);
            }
        }
        return t;
    }

    public static <T> T createInstance(String str) {
        return (T) createInstance(str, null);
    }

    private static PathClassLoader getPathClassLoader(String str, ClassLoader classLoader) {
        PathClassLoader pathClassLoader;
        Key key = new Key(str, classLoader);
        synchronized (PATHCLASSLOADER_CACHE) {
            pathClassLoader = PATHCLASSLOADER_CACHE.get(key);
            if (pathClassLoader == null) {
                pathClassLoader = null;
            }
            if (pathClassLoader == null) {
                Log.v(TAG, "Create new path class loader (" + str + ")");
                pathClassLoader = new PathClassLoader(str, classLoader);
                PATHCLASSLOADER_CACHE.put(key, pathClassLoader);
            }
        }
        return pathClassLoader;
    }

    private static Object getInstanceHelper(PluginInfo pluginInfo, Context context) {
        try {
            if (context != null) {
                mPluginloader = getPathClassLoader(pluginInfo.getApkName(), context.getClassLoader());
            } else {
                mPluginloader = getPathClassLoader(pluginInfo.getApkName(), ClassLoader.getSystemClassLoader().getParent());
            }
            mPluginClazz = mPluginloader.loadClass(pluginInfo.getImplementationName());
            Log.d(TAG, "Load class : " + pluginInfo.getImplementationName() + " successfully PathClassLoader :" + mPluginloader);
            if (context != null) {
                mPluginContext = context.createPackageContext(pluginInfo.getPackageName(), 3);
                try {
                    return mPluginClazz.getConstructor(Context.class).newInstance(mPluginContext);
                } catch (NoSuchMethodException e) {
                } catch (InvocationTargetException e2) {
                }
            }
            return mPluginClazz.newInstance();
        } catch (Exception e3) {
            Log.e(TAG, "Exception when initial instance", e3);
            return null;
        }
    }

    public static ResourceHelper getResourceHelper(String str, Context context) {
        return new ResourceHelper(str, context);
    }

    private static void MergeClassloader(PluginInfo pluginInfo, Context context) throws Throwable {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(pluginInfo.getPackageName(), 543);
            String str = packageInfo.applicationInfo.sourceDir;
            String str2 = packageInfo.applicationInfo.dataDir;
            synchronized (CLASSLOADER_CACHE) {
                CLASSLOADER_CACHE.put(pluginInfo.getPackageName(), pluginInfo.getApkName());
            }
            Log.v(TAG, "install begin");
            ZipFile zipFile = new ZipFile(str);
            ZipEntry entry = zipFile.getEntry("classes.dex");
            String str3 = pluginInfo.getApkName().split("\\/")[r1.length - 1].split("\\.apk")[0] + "classes.dex";
            File dir = context.getDir("odex", 0);
            LoadedDex loadedDex = new LoadedDex(dir, str3, entry);
            File file = loadedDex.mDexFile;
            if (!file.exists() || file.length() != entry.getSize()) {
                try {
                    Log.i(TAG, "extractDex start: ");
                    extractDex(zipFile.getInputStream(loadedDex.mZipEntry), file);
                    Log.i(TAG, "extractDex result:" + file);
                } catch (Exception e) {
                    Log.i(TAG, "extract error:" + Log.getStackTraceString(e));
                }
            }
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (Exception e2) {
                }
            }
            Log.i(TAG, "doDexInject start: ");
            doDexInject(context, dir, loadedDex);
            Log.i(TAG, "doDexInject end : ");
        } catch (Exception e3) {
            Log.e(TAG, "Exception when MergeClassloader", e3);
        }
    }

    private static void doDexInject(Context context, File file, LoadedDex loadedDex) {
        File file2 = new File(file.getAbsolutePath() + File.separator + "opt_dex");
        if (!file2.exists()) {
            file2.mkdirs();
        }
        try {
            new ArrayList().add(loadedDex.mDexFile);
            Log.v(TAG, " doDexInject new classloader start");
            DexClassLoader dexClassLoader = new DexClassLoader(loadedDex.mDexFile.getAbsolutePath(), file2.getAbsolutePath(), null, context.getClassLoader());
            Log.v(TAG, " doDexInject new classloader end");
            mergeLoader(dexClassLoader, context);
        } catch (Exception e) {
            Log.i("multidex", "install dex error:" + Log.getStackTraceString(e));
        }
    }

    private static void mergeLoader(DexClassLoader dexClassLoader, Context context) {
        PathClassLoader pathClassLoader = (PathClassLoader) context.getClassLoader();
        try {
            Object objCombineArray = combineArray(getDexElements(getPathList(pathClassLoader)), getDexElements(getPathList(dexClassLoader)));
            Object pathList = getPathList(pathClassLoader);
            setField(pathList, pathList.getClass(), "dexElements", objCombineArray);
        } catch (Exception e) {
            Log.i(TAG, " dexclassloader error:" + Log.getStackTraceString(e));
        }
    }

    private static Object getPathList(Object obj) throws IllegalAccessException, NoSuchFieldException, ClassNotFoundException, IllegalArgumentException {
        return getField(obj, Class.forName("dalvik.system.BaseDexClassLoader"), "pathList");
    }

    private static Object getField(Object obj, Class<?> cls, String str) throws IllegalAccessException, NoSuchFieldException, IllegalArgumentException {
        Field declaredField = cls.getDeclaredField(str);
        declaredField.setAccessible(DEBUG);
        return declaredField.get(obj);
    }

    private static Object getDexElements(Object obj) throws IllegalAccessException, NoSuchFieldException, IllegalArgumentException {
        return getField(obj, obj.getClass(), "dexElements");
    }

    private static void setField(Object obj, Class<?> cls, String str, Object obj2) throws IllegalAccessException, NoSuchFieldException, IllegalArgumentException {
        Field declaredField = cls.getDeclaredField(str);
        declaredField.setAccessible(DEBUG);
        declaredField.set(obj, obj2);
    }

    private static Object combineArray(Object obj, Object obj2) {
        Class<?> componentType = obj.getClass().getComponentType();
        int length = Array.getLength(obj);
        int length2 = Array.getLength(obj2) + length;
        Object objNewInstance = Array.newInstance(componentType, length2);
        for (int i = 0; i < length2; i++) {
            if (i < length) {
                Array.set(objNewInstance, i, Array.get(obj, i));
            } else {
                Array.set(objNewInstance, i, Array.get(obj2, i - length));
            }
        }
        return objNewInstance;
    }

    private static boolean extractDex(InputStream inputStream, File file) throws Throwable {
        BufferedInputStream bufferedInputStream;
        Throwable th;
        BufferedOutputStream bufferedOutputStream;
        BufferedInputStream bufferedInputStream2;
        BufferedOutputStream bufferedOutputStream2 = null;
        try {
            bufferedInputStream = new BufferedInputStream(inputStream);
            try {
                if (!$assertionsDisabled && bufferedInputStream == null) {
                    throw new AssertionError();
                }
                bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file));
                try {
                    byte[] bArr = new byte[BUF_SIZE];
                    while (true) {
                        int i = bufferedInputStream.read(bArr, 0, BUF_SIZE);
                        if (i <= 0) {
                            break;
                        }
                        bufferedOutputStream.write(bArr, 0, i);
                    }
                    if (bufferedOutputStream != null) {
                        try {
                            bufferedOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (bufferedInputStream == null) {
                        return DEBUG;
                    }
                    try {
                        bufferedInputStream.close();
                        return DEBUG;
                    } catch (IOException e2) {
                        e2.printStackTrace();
                        return DEBUG;
                    }
                } catch (IOException e3) {
                    bufferedOutputStream2 = bufferedOutputStream;
                    bufferedInputStream2 = bufferedInputStream;
                    if (bufferedOutputStream2 != null) {
                        try {
                            bufferedOutputStream2.close();
                        } catch (IOException e4) {
                            e4.printStackTrace();
                        }
                    }
                    if (bufferedInputStream2 != null) {
                        try {
                            bufferedInputStream2.close();
                        } catch (IOException e5) {
                            e5.printStackTrace();
                        }
                    }
                    return false;
                } catch (Throwable th2) {
                    th = th2;
                    if (bufferedOutputStream != null) {
                        try {
                            bufferedOutputStream.close();
                        } catch (IOException e6) {
                            e6.printStackTrace();
                        }
                    }
                    if (bufferedInputStream != null) {
                        try {
                            bufferedInputStream.close();
                        } catch (IOException e7) {
                            e7.printStackTrace();
                        }
                    }
                    throw th;
                }
            } catch (IOException e8) {
                bufferedInputStream2 = bufferedInputStream;
            } catch (Throwable th3) {
                bufferedOutputStream = null;
                th = th3;
            }
        } catch (IOException e9) {
            bufferedInputStream2 = null;
        } catch (Throwable th4) {
            bufferedInputStream = null;
            th = th4;
            bufferedOutputStream = null;
        }
    }

    public static void startActivity(Intent intent, String str) throws Throwable {
        needMergeClass(intent, str);
        mHostContext.startActivity(intent);
    }

    public static void startActivity(Intent intent, Bundle bundle, String str) throws Throwable {
        needMergeClass(intent, str);
        mHostContext.startActivity(intent, bundle);
    }

    public static void startActivityForResult(Intent intent, int i, String str, Activity activity) throws Throwable {
        needMergeClass(intent, str);
        activity.startActivityForResult(intent, i);
    }

    public static void startActivityForResult(Intent intent, int i, Bundle bundle, String str, Activity activity) throws Throwable {
        needMergeClass(intent, str);
        activity.startActivityForResult(intent, i, bundle);
    }

    public static boolean targetDefineInHost(Intent intent) {
        PackageManager packageManager = mHostContext.getPackageManager();
        String packageName = mHostContext.getPackageName();
        for (ResolveInfo resolveInfo : packageManager.queryIntentActivities(intent, 65536)) {
            Log.v(TAG, "targetDefineInHost :" + ((PackageItemInfo) resolveInfo.activityInfo).packageName);
            if (packageName.equals(((PackageItemInfo) resolveInfo.activityInfo).packageName)) {
                return DEBUG;
            }
        }
        return false;
    }

    public static void needMergeClass(Intent intent, String str) throws Throwable {
        synchronized (CLASSLOADER_CACHE) {
            if (CLASSLOADER_CACHE.get(str) == null) {
                if (targetDefineInHost(intent)) {
                    try {
                        MergeClassloader(PluginLoader.PackageMap.get(str), mHostContext);
                    } catch (Exception e) {
                        Log.i(TAG, " MergeClassloader  error:" + e);
                    }
                }
            }
        }
    }
}
