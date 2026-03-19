package android.app;

import android.app.IServiceConnection;
import android.app.LoadedApk;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.dex.ArtManager;
import android.content.pm.split.SplitDependencyLoader;
import android.content.res.AssetManager;
import android.content.res.CompatibilityInfo;
import android.content.res.Resources;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.StrictMode;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.view.DisplayAdjustments;
import com.android.internal.util.ArrayUtils;
import dalvik.system.VMRuntime;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public final class LoadedApk {
    static final boolean $assertionsDisabled = false;
    static final boolean DEBUG = false;
    private static final String PROPERTY_NAME_APPEND_NATIVE = "pi.append_native_lib_paths";
    static final String TAG = "LoadedApk";
    private final ActivityThread mActivityThread;
    private AppComponentFactory mAppComponentFactory;
    private String mAppDir;
    private Application mApplication;
    private ApplicationInfo mApplicationInfo;
    private final ClassLoader mBaseClassLoader;
    private ClassLoader mClassLoader;
    private File mCredentialProtectedDataDirFile;
    private String mDataDir;
    private File mDataDirFile;
    private File mDeviceProtectedDataDirFile;
    private final DisplayAdjustments mDisplayAdjustments;
    private final boolean mIncludeCode;
    private String mLibDir;
    private String[] mOverlayDirs;
    final String mPackageName;
    private final ArrayMap<Context, ArrayMap<BroadcastReceiver, ReceiverDispatcher>> mReceivers;
    private final boolean mRegisterPackage;
    private String mResDir;
    Resources mResources;
    private final boolean mSecurityViolation;
    private final ArrayMap<Context, ArrayMap<ServiceConnection, ServiceDispatcher>> mServices;
    private String[] mSplitAppDirs;
    private String[] mSplitClassLoaderNames;
    private SplitDependencyLoaderImpl mSplitLoader;
    private String[] mSplitNames;
    private String[] mSplitResDirs;
    private final ArrayMap<Context, ArrayMap<ServiceConnection, ServiceDispatcher>> mUnboundServices;
    private final ArrayMap<Context, ArrayMap<BroadcastReceiver, ReceiverDispatcher>> mUnregisteredReceivers;

    Application getApplication() {
        return this.mApplication;
    }

    public LoadedApk(ActivityThread activityThread, ApplicationInfo applicationInfo, CompatibilityInfo compatibilityInfo, ClassLoader classLoader, boolean z, boolean z2, boolean z3) {
        this.mDisplayAdjustments = new DisplayAdjustments();
        this.mReceivers = new ArrayMap<>();
        this.mUnregisteredReceivers = new ArrayMap<>();
        this.mServices = new ArrayMap<>();
        this.mUnboundServices = new ArrayMap<>();
        this.mActivityThread = activityThread;
        setApplicationInfo(applicationInfo);
        this.mPackageName = applicationInfo.packageName;
        this.mBaseClassLoader = classLoader;
        this.mSecurityViolation = z;
        this.mIncludeCode = z2;
        this.mRegisterPackage = z3;
        this.mDisplayAdjustments.setCompatibilityInfo(compatibilityInfo);
        this.mAppComponentFactory = createAppFactory(this.mApplicationInfo, this.mBaseClassLoader);
    }

    private static ApplicationInfo adjustNativeLibraryPaths(ApplicationInfo applicationInfo) {
        if (applicationInfo.primaryCpuAbi != null && applicationInfo.secondaryCpuAbi != null) {
            String strVmInstructionSet = VMRuntime.getRuntime().vmInstructionSet();
            String instructionSet = VMRuntime.getInstructionSet(applicationInfo.secondaryCpuAbi);
            String str = SystemProperties.get("ro.dalvik.vm.isa." + instructionSet);
            if (!str.isEmpty()) {
                instructionSet = str;
            }
            if (strVmInstructionSet.equals(instructionSet)) {
                ApplicationInfo applicationInfo2 = new ApplicationInfo(applicationInfo);
                applicationInfo2.nativeLibraryDir = applicationInfo2.secondaryNativeLibraryDir;
                applicationInfo2.primaryCpuAbi = applicationInfo2.secondaryCpuAbi;
                return applicationInfo2;
            }
        }
        return applicationInfo;
    }

    LoadedApk(ActivityThread activityThread) {
        this.mDisplayAdjustments = new DisplayAdjustments();
        this.mReceivers = new ArrayMap<>();
        this.mUnregisteredReceivers = new ArrayMap<>();
        this.mServices = new ArrayMap<>();
        this.mUnboundServices = new ArrayMap<>();
        this.mActivityThread = activityThread;
        this.mApplicationInfo = new ApplicationInfo();
        this.mApplicationInfo.packageName = ZenModeConfig.SYSTEM_AUTHORITY;
        this.mPackageName = ZenModeConfig.SYSTEM_AUTHORITY;
        this.mAppDir = null;
        this.mResDir = null;
        this.mSplitAppDirs = null;
        this.mSplitResDirs = null;
        this.mSplitClassLoaderNames = null;
        this.mOverlayDirs = null;
        this.mDataDir = null;
        this.mDataDirFile = null;
        this.mDeviceProtectedDataDirFile = null;
        this.mCredentialProtectedDataDirFile = null;
        this.mLibDir = null;
        this.mBaseClassLoader = null;
        this.mSecurityViolation = false;
        this.mIncludeCode = true;
        this.mRegisterPackage = false;
        this.mClassLoader = ClassLoader.getSystemClassLoader();
        this.mResources = Resources.getSystem();
        this.mAppComponentFactory = createAppFactory(this.mApplicationInfo, this.mClassLoader);
    }

    void installSystemApplicationInfo(ApplicationInfo applicationInfo, ClassLoader classLoader) {
        this.mApplicationInfo = applicationInfo;
        this.mClassLoader = classLoader;
        this.mAppComponentFactory = createAppFactory(applicationInfo, classLoader);
    }

    private AppComponentFactory createAppFactory(ApplicationInfo applicationInfo, ClassLoader classLoader) {
        if (applicationInfo.appComponentFactory != null && classLoader != null) {
            try {
                return (AppComponentFactory) classLoader.loadClass(applicationInfo.appComponentFactory).newInstance();
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                Slog.e(TAG, "Unable to instantiate appComponentFactory", e);
            }
        }
        return AppComponentFactory.DEFAULT;
    }

    public AppComponentFactory getAppFactory() {
        return this.mAppComponentFactory;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public ApplicationInfo getApplicationInfo() {
        return this.mApplicationInfo;
    }

    public int getTargetSdkVersion() {
        return this.mApplicationInfo.targetSdkVersion;
    }

    public boolean isSecurityViolation() {
        return this.mSecurityViolation;
    }

    public CompatibilityInfo getCompatibilityInfo() {
        return this.mDisplayAdjustments.getCompatibilityInfo();
    }

    public void setCompatibilityInfo(CompatibilityInfo compatibilityInfo) {
        this.mDisplayAdjustments.setCompatibilityInfo(compatibilityInfo);
    }

    private static String[] getLibrariesFor(String str) {
        try {
            ApplicationInfo applicationInfo = ActivityThread.getPackageManager().getApplicationInfo(str, 1024, UserHandle.myUserId());
            if (applicationInfo == null) {
                return null;
            }
            return applicationInfo.sharedLibraryFiles;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void updateApplicationInfo(ApplicationInfo applicationInfo, List<String> list) {
        setApplicationInfo(applicationInfo);
        ArrayList<String> arrayList = new ArrayList();
        makePaths(this.mActivityThread, applicationInfo, arrayList);
        List<String> arrayList2 = new ArrayList<>(arrayList.size());
        if (list != null) {
            for (String str : arrayList) {
                String strSubstring = str.substring(str.lastIndexOf(File.separator));
                boolean z = false;
                Iterator<String> it = list.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    String next = it.next();
                    if (strSubstring.equals(next.substring(next.lastIndexOf(File.separator)))) {
                        z = true;
                        break;
                    }
                }
                if (!z) {
                    arrayList2.add(str);
                }
            }
        } else {
            arrayList2.addAll(arrayList);
        }
        synchronized (this) {
            createOrUpdateClassLoaderLocked(arrayList2);
            if (this.mResources != null) {
                try {
                    this.mResources = ResourcesManager.getInstance().getResources(null, this.mResDir, getSplitPaths(null), this.mOverlayDirs, this.mApplicationInfo.sharedLibraryFiles, 0, null, getCompatibilityInfo(), getClassLoader());
                } catch (PackageManager.NameNotFoundException e) {
                    throw new AssertionError("null split not found");
                }
            }
        }
        this.mAppComponentFactory = createAppFactory(applicationInfo, this.mClassLoader);
    }

    private void setApplicationInfo(ApplicationInfo applicationInfo) {
        int iMyUid = Process.myUid();
        ApplicationInfo applicationInfoAdjustNativeLibraryPaths = adjustNativeLibraryPaths(applicationInfo);
        this.mApplicationInfo = applicationInfoAdjustNativeLibraryPaths;
        this.mAppDir = applicationInfoAdjustNativeLibraryPaths.sourceDir;
        this.mResDir = applicationInfoAdjustNativeLibraryPaths.uid == iMyUid ? applicationInfoAdjustNativeLibraryPaths.sourceDir : applicationInfoAdjustNativeLibraryPaths.publicSourceDir;
        this.mOverlayDirs = applicationInfoAdjustNativeLibraryPaths.resourceDirs;
        this.mDataDir = applicationInfoAdjustNativeLibraryPaths.dataDir;
        this.mLibDir = applicationInfoAdjustNativeLibraryPaths.nativeLibraryDir;
        this.mDataDirFile = FileUtils.newFileOrNull(applicationInfoAdjustNativeLibraryPaths.dataDir);
        this.mDeviceProtectedDataDirFile = FileUtils.newFileOrNull(applicationInfoAdjustNativeLibraryPaths.deviceProtectedDataDir);
        this.mCredentialProtectedDataDirFile = FileUtils.newFileOrNull(applicationInfoAdjustNativeLibraryPaths.credentialProtectedDataDir);
        this.mSplitNames = applicationInfoAdjustNativeLibraryPaths.splitNames;
        this.mSplitAppDirs = applicationInfoAdjustNativeLibraryPaths.splitSourceDirs;
        this.mSplitResDirs = applicationInfoAdjustNativeLibraryPaths.uid == iMyUid ? applicationInfoAdjustNativeLibraryPaths.splitSourceDirs : applicationInfoAdjustNativeLibraryPaths.splitPublicSourceDirs;
        this.mSplitClassLoaderNames = applicationInfoAdjustNativeLibraryPaths.splitClassLoaderNames;
        if (applicationInfoAdjustNativeLibraryPaths.requestsIsolatedSplitLoading() && !ArrayUtils.isEmpty(this.mSplitNames)) {
            this.mSplitLoader = new SplitDependencyLoaderImpl(applicationInfoAdjustNativeLibraryPaths.splitDependencies);
        }
    }

    public static void makePaths(ActivityThread activityThread, ApplicationInfo applicationInfo, List<String> list) {
        makePaths(activityThread, false, applicationInfo, list, null);
    }

    public static void makePaths(ActivityThread activityThread, boolean z, ApplicationInfo applicationInfo, List<String> list, List<String> list2) {
        String str = applicationInfo.sourceDir;
        String str2 = applicationInfo.nativeLibraryDir;
        String[] strArr = applicationInfo.sharedLibraryFiles;
        list.clear();
        list.add(str);
        if (applicationInfo.splitSourceDirs != null && !applicationInfo.requestsIsolatedSplitLoading()) {
            Collections.addAll(list, applicationInfo.splitSourceDirs);
        }
        if (list2 != null) {
            list2.clear();
        }
        String[] librariesFor = null;
        if (activityThread != null) {
            String str3 = activityThread.mInstrumentationPackageName;
            String str4 = activityThread.mInstrumentationAppDir;
            String[] strArr2 = activityThread.mInstrumentationSplitAppDirs;
            String str5 = activityThread.mInstrumentationLibDir;
            String str6 = activityThread.mInstrumentedAppDir;
            String[] strArr3 = activityThread.mInstrumentedSplitAppDirs;
            String str7 = activityThread.mInstrumentedLibDir;
            if (str.equals(str4) || str.equals(str6)) {
                list.clear();
                list.add(str4);
                if (!applicationInfo.requestsIsolatedSplitLoading()) {
                    if (strArr2 != null) {
                        Collections.addAll(list, strArr2);
                    }
                    if (!str4.equals(str6)) {
                        list.add(str6);
                        if (strArr3 != null) {
                            Collections.addAll(list, strArr3);
                        }
                    }
                }
                if (list2 != null) {
                    list2.add(str5);
                    if (!str5.equals(str7)) {
                        list2.add(str7);
                    }
                }
                if (!str6.equals(str4)) {
                    librariesFor = getLibrariesFor(str3);
                }
            }
        }
        if (list2 != null) {
            if (list2.isEmpty()) {
                list2.add(str2);
            }
            if (applicationInfo.primaryCpuAbi != null) {
                if (applicationInfo.targetSdkVersion < 24) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("/system/fake-libs");
                    sb.append(VMRuntime.is64BitAbi(applicationInfo.primaryCpuAbi) ? "64" : "");
                    list2.add(sb.toString());
                }
                Iterator<String> it = list.iterator();
                while (it.hasNext()) {
                    list2.add(it.next() + "!/lib/" + applicationInfo.primaryCpuAbi);
                }
            }
            if (z) {
                list2.add(System.getProperty("java.library.path"));
            }
        }
        if (strArr != null) {
            int i = 0;
            for (String str8 : strArr) {
                if (!list.contains(str8)) {
                    list.add(i, str8);
                    i++;
                    appendApkLibPathIfNeeded(str8, applicationInfo, list2);
                }
            }
        }
        if (librariesFor != null) {
            for (String str9 : librariesFor) {
                if (!list.contains(str9)) {
                    list.add(0, str9);
                    appendApkLibPathIfNeeded(str9, applicationInfo, list2);
                }
            }
        }
    }

    private static void appendApkLibPathIfNeeded(String str, ApplicationInfo applicationInfo, List<String> list) {
        if (list != null && applicationInfo.primaryCpuAbi != null && str.endsWith(PackageParser.APK_FILE_EXTENSION) && applicationInfo.targetSdkVersion >= 26) {
            list.add(str + "!/lib/" + applicationInfo.primaryCpuAbi);
        }
    }

    private class SplitDependencyLoaderImpl extends SplitDependencyLoader<PackageManager.NameNotFoundException> {
        private final ClassLoader[] mCachedClassLoaders;
        private final String[][] mCachedResourcePaths;

        SplitDependencyLoaderImpl(SparseArray<int[]> sparseArray) {
            super(sparseArray);
            this.mCachedResourcePaths = new String[LoadedApk.this.mSplitNames.length + 1][];
            this.mCachedClassLoaders = new ClassLoader[LoadedApk.this.mSplitNames.length + 1];
        }

        @Override
        protected boolean isSplitCached(int i) {
            return this.mCachedClassLoaders[i] != null;
        }

        @Override
        protected void constructSplit(int i, int[] iArr, int i2) throws PackageManager.NameNotFoundException {
            ArrayList arrayList = new ArrayList();
            if (i == 0) {
                LoadedApk.this.createOrUpdateClassLoaderLocked(null);
                this.mCachedClassLoaders[0] = LoadedApk.this.mClassLoader;
                for (int i3 : iArr) {
                    arrayList.add(LoadedApk.this.mSplitResDirs[i3 - 1]);
                }
                this.mCachedResourcePaths[0] = (String[]) arrayList.toArray(new String[arrayList.size()]);
                return;
            }
            int i4 = i - 1;
            this.mCachedClassLoaders[i] = ApplicationLoaders.getDefault().getClassLoader(LoadedApk.this.mSplitAppDirs[i4], LoadedApk.this.getTargetSdkVersion(), false, null, null, this.mCachedClassLoaders[i2], LoadedApk.this.mSplitClassLoaderNames[i4]);
            Collections.addAll(arrayList, this.mCachedResourcePaths[i2]);
            arrayList.add(LoadedApk.this.mSplitResDirs[i4]);
            for (int i5 : iArr) {
                arrayList.add(LoadedApk.this.mSplitResDirs[i5 - 1]);
            }
            this.mCachedResourcePaths[i] = (String[]) arrayList.toArray(new String[arrayList.size()]);
        }

        private int ensureSplitLoaded(String str) throws PackageManager.NameNotFoundException {
            int i;
            if (str != null) {
                int iBinarySearch = Arrays.binarySearch(LoadedApk.this.mSplitNames, str);
                if (iBinarySearch < 0) {
                    throw new PackageManager.NameNotFoundException("Split name '" + str + "' is not installed");
                }
                i = iBinarySearch + 1;
            } else {
                i = 0;
            }
            loadDependenciesForSplit(i);
            return i;
        }

        ClassLoader getClassLoaderForSplit(String str) throws PackageManager.NameNotFoundException {
            return this.mCachedClassLoaders[ensureSplitLoaded(str)];
        }

        String[] getSplitPathsForSplit(String str) throws PackageManager.NameNotFoundException {
            return this.mCachedResourcePaths[ensureSplitLoaded(str)];
        }
    }

    ClassLoader getSplitClassLoader(String str) throws PackageManager.NameNotFoundException {
        if (this.mSplitLoader == null) {
            return this.mClassLoader;
        }
        return this.mSplitLoader.getClassLoaderForSplit(str);
    }

    String[] getSplitPaths(String str) throws PackageManager.NameNotFoundException {
        if (this.mSplitLoader == null) {
            return this.mSplitResDirs;
        }
        return this.mSplitLoader.getSplitPathsForSplit(str);
    }

    private void createOrUpdateClassLoaderLocked(List<String> list) {
        if (this.mPackageName.equals(ZenModeConfig.SYSTEM_AUTHORITY)) {
            if (this.mClassLoader != null) {
                return;
            }
            if (this.mBaseClassLoader != null) {
                this.mClassLoader = this.mBaseClassLoader;
            } else {
                this.mClassLoader = ClassLoader.getSystemClassLoader();
            }
            this.mAppComponentFactory = createAppFactory(this.mApplicationInfo, this.mClassLoader);
            return;
        }
        if (!Objects.equals(this.mPackageName, ActivityThread.currentPackageName()) && this.mIncludeCode) {
            try {
                ActivityThread.getPackageManager().notifyPackageUse(this.mPackageName, 6);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        if (this.mRegisterPackage) {
            try {
                ActivityManager.getService().addPackageDependency(this.mPackageName);
            } catch (RemoteException e2) {
                throw e2.rethrowFromSystemServer();
            }
        }
        ArrayList arrayList = new ArrayList(10);
        ArrayList arrayList2 = new ArrayList(10);
        boolean z = false;
        boolean z2 = this.mApplicationInfo.isSystemApp() && !this.mApplicationInfo.isUpdatedSystemApp();
        String property = System.getProperty("java.library.path");
        boolean z3 = (this.mApplicationInfo.getCodePath() != null && this.mApplicationInfo.isVendor() && (property.contains("/vendor/lib") ^ true)) ? false : z2;
        makePaths(this.mActivityThread, z3, this.mApplicationInfo, arrayList, arrayList2);
        String str = this.mDataDir;
        if (z3) {
            str = (str + File.pathSeparator + Paths.get(getAppDir(), new String[0]).getParent().toString()) + File.pathSeparator + property;
        }
        String str2 = str;
        String strJoin = TextUtils.join(File.pathSeparator, arrayList2);
        if (!this.mIncludeCode) {
            if (this.mClassLoader == null) {
                StrictMode.ThreadPolicy threadPolicyAllowThreadDiskReads = StrictMode.allowThreadDiskReads();
                this.mClassLoader = ApplicationLoaders.getDefault().getClassLoader("", this.mApplicationInfo.targetSdkVersion, z3, strJoin, str2, this.mBaseClassLoader, null);
                StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads);
                this.mAppComponentFactory = AppComponentFactory.DEFAULT;
                return;
            }
            return;
        }
        String strJoin2 = arrayList.size() == 1 ? (String) arrayList.get(0) : TextUtils.join(File.pathSeparator, arrayList);
        if (this.mClassLoader == null) {
            StrictMode.ThreadPolicy threadPolicyAllowThreadDiskReads2 = StrictMode.allowThreadDiskReads();
            this.mClassLoader = ApplicationLoaders.getDefault().getClassLoader(strJoin2, this.mApplicationInfo.targetSdkVersion, z3, strJoin, str2, this.mBaseClassLoader, this.mApplicationInfo.classLoaderName);
            this.mAppComponentFactory = createAppFactory(this.mApplicationInfo, this.mClassLoader);
            StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads2);
            z = true;
        }
        if (!arrayList2.isEmpty() && SystemProperties.getBoolean(PROPERTY_NAME_APPEND_NATIVE, true)) {
            StrictMode.ThreadPolicy threadPolicyAllowThreadDiskReads3 = StrictMode.allowThreadDiskReads();
            try {
                ApplicationLoaders.getDefault().addNative(this.mClassLoader, arrayList2);
                StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads3);
            } catch (Throwable th) {
                StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads3);
                throw th;
            }
        }
        ArrayList arrayList3 = new ArrayList(3);
        String str3 = VMRuntime.getRuntime().is64Bit() ? "64" : "";
        if (!property.contains("/vendor/lib")) {
            arrayList3.add("/vendor/lib" + str3);
        }
        if (!property.contains("/odm/lib")) {
            arrayList3.add("/odm/lib" + str3);
        }
        if (!property.contains("/product/lib")) {
            arrayList3.add("/product/lib" + str3);
        }
        if (!arrayList3.isEmpty()) {
            StrictMode.ThreadPolicy threadPolicyAllowThreadDiskReads4 = StrictMode.allowThreadDiskReads();
            try {
                ApplicationLoaders.getDefault().addNative(this.mClassLoader, arrayList3);
                StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads4);
            } catch (Throwable th2) {
                StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads4);
                throw th2;
            }
        }
        if (list != null && list.size() > 0) {
            ApplicationLoaders.getDefault().addPath(this.mClassLoader, TextUtils.join(File.pathSeparator, list));
            z = true;
        }
        if (z && !ActivityThread.isSystem()) {
            setupJitProfileSupport();
        }
    }

    public ClassLoader getClassLoader() {
        ClassLoader classLoader;
        synchronized (this) {
            if (this.mClassLoader == null) {
                createOrUpdateClassLoaderLocked(null);
            }
            classLoader = this.mClassLoader;
        }
        return classLoader;
    }

    private void setupJitProfileSupport() {
        if (!SystemProperties.getBoolean("dalvik.vm.usejitprofiles", false) || this.mApplicationInfo.uid != Process.myUid()) {
            return;
        }
        ArrayList arrayList = new ArrayList();
        if ((this.mApplicationInfo.flags & 4) != 0) {
            arrayList.add(this.mApplicationInfo.sourceDir);
        }
        if (this.mApplicationInfo.splitSourceDirs != null) {
            Collections.addAll(arrayList, this.mApplicationInfo.splitSourceDirs);
        }
        if (arrayList.isEmpty()) {
            return;
        }
        int size = arrayList.size() - 1;
        while (size >= 0) {
            VMRuntime.registerAppInfo(ArtManager.getCurrentProfilePath(this.mPackageName, UserHandle.myUserId(), size == 0 ? null : this.mApplicationInfo.splitNames[size - 1]), new String[]{(String) arrayList.get(size)});
            size--;
        }
        DexLoadReporter.getInstance().registerAppDataDir(this.mPackageName, this.mDataDir);
    }

    private void initializeJavaContextClassLoader() {
        ClassLoader warningContextClassLoader;
        try {
            PackageInfo packageInfo = ActivityThread.getPackageManager().getPackageInfo(this.mPackageName, 268435456, UserHandle.myUserId());
            if (packageInfo == null) {
                throw new IllegalStateException("Unable to get package info for " + this.mPackageName + "; is package not installed?");
            }
            if ((packageInfo.sharedUserId != null) || (packageInfo.applicationInfo != null && !this.mPackageName.equals(packageInfo.applicationInfo.processName))) {
                warningContextClassLoader = new WarningContextClassLoader();
            } else {
                warningContextClassLoader = this.mClassLoader;
            }
            Thread.currentThread().setContextClassLoader(warningContextClassLoader);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private static class WarningContextClassLoader extends ClassLoader {
        private static boolean warned = false;

        private WarningContextClassLoader() {
        }

        private void warn(String str) {
            if (warned) {
                return;
            }
            warned = true;
            Thread.currentThread().setContextClassLoader(getParent());
            Slog.w(ActivityThread.TAG, "ClassLoader." + str + ": The class loader returned by Thread.getContextClassLoader() may fail for processes that host multiple applications. You should explicitly specify a context class loader. For example: Thread.setContextClassLoader(getClass().getClassLoader());");
        }

        @Override
        public URL getResource(String str) {
            warn("getResource");
            return getParent().getResource(str);
        }

        @Override
        public Enumeration<URL> getResources(String str) throws IOException {
            warn("getResources");
            return getParent().getResources(str);
        }

        @Override
        public InputStream getResourceAsStream(String str) {
            warn("getResourceAsStream");
            return getParent().getResourceAsStream(str);
        }

        @Override
        public Class<?> loadClass(String str) throws ClassNotFoundException {
            warn("loadClass");
            return getParent().loadClass(str);
        }

        @Override
        public void setClassAssertionStatus(String str, boolean z) {
            warn("setClassAssertionStatus");
            getParent().setClassAssertionStatus(str, z);
        }

        @Override
        public void setPackageAssertionStatus(String str, boolean z) {
            warn("setPackageAssertionStatus");
            getParent().setPackageAssertionStatus(str, z);
        }

        @Override
        public void setDefaultAssertionStatus(boolean z) {
            warn("setDefaultAssertionStatus");
            getParent().setDefaultAssertionStatus(z);
        }

        @Override
        public void clearAssertionStatus() {
            warn("clearAssertionStatus");
            getParent().clearAssertionStatus();
        }
    }

    public String getAppDir() {
        return this.mAppDir;
    }

    public String getLibDir() {
        return this.mLibDir;
    }

    public String getResDir() {
        return this.mResDir;
    }

    public String[] getSplitAppDirs() {
        return this.mSplitAppDirs;
    }

    public String[] getSplitResDirs() {
        return this.mSplitResDirs;
    }

    public String[] getOverlayDirs() {
        return this.mOverlayDirs;
    }

    public String getDataDir() {
        return this.mDataDir;
    }

    public File getDataDirFile() {
        return this.mDataDirFile;
    }

    public File getDeviceProtectedDataDirFile() {
        return this.mDeviceProtectedDataDirFile;
    }

    public File getCredentialProtectedDataDirFile() {
        return this.mCredentialProtectedDataDirFile;
    }

    public AssetManager getAssets() {
        return getResources().getAssets();
    }

    public Resources getResources() {
        if (this.mResources == null) {
            try {
                this.mResources = ResourcesManager.getInstance().getResources(null, this.mResDir, getSplitPaths(null), this.mOverlayDirs, this.mApplicationInfo.sharedLibraryFiles, 0, null, getCompatibilityInfo(), getClassLoader());
            } catch (PackageManager.NameNotFoundException e) {
                throw new AssertionError("null split not found");
            }
        }
        return this.mResources;
    }

    public Application makeApplication(boolean z, Instrumentation instrumentation) {
        Application applicationNewApplication;
        ContextImpl contextImplCreateAppContext;
        if (this.mApplication != null) {
            return this.mApplication;
        }
        Trace.traceBegin(64L, "makeApplication");
        Application application = null;
        String str = this.mApplicationInfo.className;
        if (z || str == null) {
            str = "android.app.Application";
        }
        try {
            ClassLoader classLoader = getClassLoader();
            if (!this.mPackageName.equals(ZenModeConfig.SYSTEM_AUTHORITY)) {
                Trace.traceBegin(64L, "initializeJavaContextClassLoader");
                initializeJavaContextClassLoader();
                Trace.traceEnd(64L);
            }
            contextImplCreateAppContext = ContextImpl.createAppContext(this.mActivityThread, this);
            applicationNewApplication = this.mActivityThread.mInstrumentation.newApplication(classLoader, str, contextImplCreateAppContext);
        } catch (Exception e) {
            e = e;
        }
        try {
            contextImplCreateAppContext.setOuterContext(applicationNewApplication);
        } catch (Exception e2) {
            application = applicationNewApplication;
            e = e2;
            if (!this.mActivityThread.mInstrumentation.onException(application, e)) {
                Trace.traceEnd(64L);
                throw new RuntimeException("Unable to instantiate application " + str + ": " + e.toString(), e);
            }
            applicationNewApplication = application;
        }
        this.mActivityThread.mAllApplications.add(applicationNewApplication);
        this.mApplication = applicationNewApplication;
        if (instrumentation != null) {
            try {
                instrumentation.callApplicationOnCreate(applicationNewApplication);
            } catch (Exception e3) {
                if (!instrumentation.onException(applicationNewApplication, e3)) {
                    Trace.traceEnd(64L);
                    throw new RuntimeException("Unable to create application " + applicationNewApplication.getClass().getName() + ": " + e3.toString(), e3);
                }
            }
        }
        SparseArray<String> assignedPackageIdentifiers = getAssets().getAssignedPackageIdentifiers();
        int size = assignedPackageIdentifiers.size();
        for (int i = 0; i < size; i++) {
            int iKeyAt = assignedPackageIdentifiers.keyAt(i);
            if (iKeyAt != 1 && iKeyAt != 127) {
                rewriteRValues(getClassLoader(), assignedPackageIdentifiers.valueAt(i), iKeyAt);
            }
        }
        Trace.traceEnd(64L);
        return applicationNewApplication;
    }

    private void rewriteRValues(ClassLoader classLoader, String str, int i) {
        Throwable e;
        try {
            try {
                try {
                    classLoader.loadClass(str + ".R").getMethod("onResourcesLoaded", Integer.TYPE).invoke(null, Integer.valueOf(i));
                } catch (IllegalAccessException e2) {
                    e = e2;
                    throw new RuntimeException("Failed to rewrite resource references for " + str, e);
                } catch (InvocationTargetException e3) {
                    e = e3.getCause();
                    throw new RuntimeException("Failed to rewrite resource references for " + str, e);
                }
            } catch (NoSuchMethodException e4) {
            }
        } catch (ClassNotFoundException e5) {
            Log.i(TAG, "No resource references to update in package " + str);
        }
    }

    public void removeContextRegistrations(Context context, String str, String str2) {
        int i;
        boolean zVmRegistrationLeaksEnabled = StrictMode.vmRegistrationLeaksEnabled();
        synchronized (this.mReceivers) {
            ArrayMap<BroadcastReceiver, ReceiverDispatcher> arrayMapRemove = this.mReceivers.remove(context);
            if (arrayMapRemove != null) {
                for (int i2 = 0; i2 < arrayMapRemove.size(); i2++) {
                    ReceiverDispatcher receiverDispatcherValueAt = arrayMapRemove.valueAt(i2);
                    IntentReceiverLeaked intentReceiverLeaked = new IntentReceiverLeaked(str2 + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + str + " has leaked IntentReceiver " + receiverDispatcherValueAt.getIntentReceiver() + " that was originally registered here. Are you missing a call to unregisterReceiver()?");
                    intentReceiverLeaked.setStackTrace(receiverDispatcherValueAt.getLocation().getStackTrace());
                    Slog.e(ActivityThread.TAG, intentReceiverLeaked.getMessage(), intentReceiverLeaked);
                    if (zVmRegistrationLeaksEnabled) {
                        StrictMode.onIntentReceiverLeaked(intentReceiverLeaked);
                    }
                    try {
                        ActivityManager.getService().unregisterReceiver(receiverDispatcherValueAt.getIIntentReceiver());
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                }
            }
            this.mUnregisteredReceivers.remove(context);
        }
        synchronized (this.mServices) {
            ArrayMap<ServiceConnection, ServiceDispatcher> arrayMapRemove2 = this.mServices.remove(context);
            if (arrayMapRemove2 != null) {
                for (i = 0; i < arrayMapRemove2.size(); i++) {
                    ServiceDispatcher serviceDispatcherValueAt = arrayMapRemove2.valueAt(i);
                    ServiceConnectionLeaked serviceConnectionLeaked = new ServiceConnectionLeaked(str2 + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + str + " has leaked ServiceConnection " + serviceDispatcherValueAt.getServiceConnection() + " that was originally bound here");
                    serviceConnectionLeaked.setStackTrace(serviceDispatcherValueAt.getLocation().getStackTrace());
                    Slog.e(ActivityThread.TAG, serviceConnectionLeaked.getMessage(), serviceConnectionLeaked);
                    if (zVmRegistrationLeaksEnabled) {
                        StrictMode.onServiceConnectionLeaked(serviceConnectionLeaked);
                    }
                    try {
                        ActivityManager.getService().unbindService(serviceDispatcherValueAt.getIServiceConnection());
                        serviceDispatcherValueAt.doForget();
                    } catch (RemoteException e2) {
                        throw e2.rethrowFromSystemServer();
                    }
                }
            }
            this.mUnboundServices.remove(context);
        }
    }

    public IIntentReceiver getReceiverDispatcher(BroadcastReceiver broadcastReceiver, Context context, Handler handler, Instrumentation instrumentation, boolean z) {
        ArrayMap<BroadcastReceiver, ReceiverDispatcher> arrayMap;
        IIntentReceiver iIntentReceiver;
        synchronized (this.mReceivers) {
            ReceiverDispatcher receiverDispatcher = null;
            if (z) {
                try {
                    arrayMap = this.mReceivers.get(context);
                    if (arrayMap != null) {
                        receiverDispatcher = arrayMap.get(broadcastReceiver);
                    }
                } catch (Throwable th) {
                    throw th;
                }
            } else {
                arrayMap = null;
            }
            if (receiverDispatcher == null) {
                receiverDispatcher = new ReceiverDispatcher(broadcastReceiver, context, handler, instrumentation, z);
                if (z) {
                    if (arrayMap == null) {
                        arrayMap = new ArrayMap<>();
                        this.mReceivers.put(context, arrayMap);
                    }
                    arrayMap.put(broadcastReceiver, receiverDispatcher);
                }
            } else {
                receiverDispatcher.validate(context, handler);
            }
            receiverDispatcher.mForgotten = false;
            iIntentReceiver = receiverDispatcher.getIIntentReceiver();
        }
        return iIntentReceiver;
    }

    public IIntentReceiver forgetReceiverDispatcher(Context context, BroadcastReceiver broadcastReceiver) {
        ReceiverDispatcher receiverDispatcher;
        ReceiverDispatcher receiverDispatcher2;
        IIntentReceiver iIntentReceiver;
        synchronized (this.mReceivers) {
            ArrayMap<BroadcastReceiver, ReceiverDispatcher> arrayMap = this.mReceivers.get(context);
            if (arrayMap != null && (receiverDispatcher2 = arrayMap.get(broadcastReceiver)) != null) {
                arrayMap.remove(broadcastReceiver);
                if (arrayMap.size() == 0) {
                    this.mReceivers.remove(context);
                }
                if (broadcastReceiver.getDebugUnregister()) {
                    ArrayMap<BroadcastReceiver, ReceiverDispatcher> arrayMap2 = this.mUnregisteredReceivers.get(context);
                    if (arrayMap2 == null) {
                        arrayMap2 = new ArrayMap<>();
                        this.mUnregisteredReceivers.put(context, arrayMap2);
                    }
                    IllegalArgumentException illegalArgumentException = new IllegalArgumentException("Originally unregistered here:");
                    illegalArgumentException.fillInStackTrace();
                    receiverDispatcher2.setUnregisterLocation(illegalArgumentException);
                    arrayMap2.put(broadcastReceiver, receiverDispatcher2);
                }
                receiverDispatcher2.mForgotten = true;
                iIntentReceiver = receiverDispatcher2.getIIntentReceiver();
            } else {
                ArrayMap<BroadcastReceiver, ReceiverDispatcher> arrayMap3 = this.mUnregisteredReceivers.get(context);
                if (arrayMap3 != null && (receiverDispatcher = arrayMap3.get(broadcastReceiver)) != null) {
                    throw new IllegalArgumentException("Unregistering Receiver " + broadcastReceiver + " that was already unregistered", receiverDispatcher.getUnregisterLocation());
                }
                if (context == null) {
                    throw new IllegalStateException("Unbinding Receiver " + broadcastReceiver + " from Context that is no longer in use: " + context);
                }
                throw new IllegalArgumentException("Receiver not registered: " + broadcastReceiver);
            }
        }
        return iIntentReceiver;
    }

    static final class ReceiverDispatcher {
        final Handler mActivityThread;
        final Context mContext;
        boolean mForgotten;
        final IIntentReceiver.Stub mIIntentReceiver;
        final Instrumentation mInstrumentation;
        final IntentReceiverLeaked mLocation;
        final BroadcastReceiver mReceiver;
        final boolean mRegistered;
        RuntimeException mUnregisterLocation;

        static final class InnerReceiver extends IIntentReceiver.Stub {
            final WeakReference<ReceiverDispatcher> mDispatcher;
            final ReceiverDispatcher mStrongRef;

            InnerReceiver(ReceiverDispatcher receiverDispatcher, boolean z) {
                this.mDispatcher = new WeakReference<>(receiverDispatcher);
                this.mStrongRef = z ? receiverDispatcher : null;
            }

            @Override
            public void performReceive(Intent intent, int i, String str, Bundle bundle, boolean z, boolean z2, int i2) {
                InnerReceiver innerReceiver;
                ReceiverDispatcher receiverDispatcher;
                if (intent == null) {
                    Log.wtf(LoadedApk.TAG, "Null intent received");
                    innerReceiver = this;
                    receiverDispatcher = null;
                } else {
                    innerReceiver = this;
                    receiverDispatcher = innerReceiver.mDispatcher.get();
                }
                if (ActivityThread.DEBUG_BROADCAST) {
                    int intExtra = intent.getIntExtra("seq", -1);
                    StringBuilder sb = new StringBuilder();
                    sb.append("Receiving broadcast ");
                    sb.append(intent.getAction());
                    sb.append(" seq=");
                    sb.append(intExtra);
                    sb.append(" to ");
                    sb.append(receiverDispatcher != null ? receiverDispatcher.mReceiver : null);
                    Slog.i(ActivityThread.TAG, sb.toString());
                }
                if (receiverDispatcher != null) {
                    receiverDispatcher.performReceive(intent, i, str, bundle, z, z2, i2);
                    return;
                }
                if (ActivityThread.DEBUG_BROADCAST) {
                    Slog.i(ActivityThread.TAG, "Finishing broadcast to unregistered receiver");
                }
                IActivityManager service = ActivityManager.getService();
                if (bundle != null) {
                    try {
                        bundle.setAllowFds(false);
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                }
                service.finishReceiver(innerReceiver, i, str, bundle, false, intent.getFlags());
            }
        }

        final class Args extends BroadcastReceiver.PendingResult {
            private Intent mCurIntent;
            private boolean mDispatched;
            private final boolean mOrdered;
            private Throwable mPreviousRunStacktrace;

            public Args(Intent intent, int i, String str, Bundle bundle, boolean z, boolean z2, int i2) {
                super(i, str, bundle, ReceiverDispatcher.this.mRegistered ? 1 : 2, z, z2, ReceiverDispatcher.this.mIIntentReceiver.asBinder(), i2, intent.getFlags());
                this.mCurIntent = intent;
                this.mOrdered = z;
            }

            public final Runnable getRunnable() {
                return new Runnable() {
                    @Override
                    public final void run() {
                        LoadedApk.ReceiverDispatcher.Args.lambda$getRunnable$0(this.f$0);
                    }
                };
            }

            public static void lambda$getRunnable$0(Args args) {
                BroadcastReceiver broadcastReceiver = ReceiverDispatcher.this.mReceiver;
                boolean z = args.mOrdered;
                if (ActivityThread.DEBUG_BROADCAST) {
                    Slog.i(ActivityThread.TAG, "Dispatching broadcast " + args.mCurIntent.getAction() + " seq=" + args.mCurIntent.getIntExtra("seq", -1) + " to " + ReceiverDispatcher.this.mReceiver);
                    StringBuilder sb = new StringBuilder();
                    sb.append("  mRegistered=");
                    sb.append(ReceiverDispatcher.this.mRegistered);
                    sb.append(" mOrderedHint=");
                    sb.append(z);
                    Slog.i(ActivityThread.TAG, sb.toString());
                }
                IActivityManager service = ActivityManager.getService();
                Intent intent = args.mCurIntent;
                if (intent == null) {
                    Log.wtf(LoadedApk.TAG, "Null intent being dispatched, mDispatched=" + args.mDispatched + ": run() previously called at " + Log.getStackTraceString(args.mPreviousRunStacktrace));
                }
                args.mCurIntent = null;
                args.mDispatched = true;
                args.mPreviousRunStacktrace = new Throwable("Previous stacktrace");
                if (broadcastReceiver == null || intent == null || ReceiverDispatcher.this.mForgotten) {
                    if (ReceiverDispatcher.this.mRegistered && z) {
                        if (ActivityThread.DEBUG_BROADCAST) {
                            Slog.i(ActivityThread.TAG, "Finishing null broadcast to " + ReceiverDispatcher.this.mReceiver);
                        }
                        args.sendFinished(service);
                        return;
                    }
                    return;
                }
                Trace.traceBegin(64L, "broadcastReceiveReg");
                try {
                    ClassLoader classLoader = ReceiverDispatcher.this.mReceiver.getClass().getClassLoader();
                    intent.setExtrasClassLoader(classLoader);
                    intent.prepareToEnterProcess();
                    args.setExtrasClassLoader(classLoader);
                    broadcastReceiver.setPendingResult(args);
                    broadcastReceiver.onReceive(ReceiverDispatcher.this.mContext, intent);
                } catch (Exception e) {
                    if (ReceiverDispatcher.this.mRegistered && z) {
                        if (ActivityThread.DEBUG_BROADCAST) {
                            Slog.i(ActivityThread.TAG, "Finishing failed broadcast to " + ReceiverDispatcher.this.mReceiver);
                        }
                        args.sendFinished(service);
                    }
                    if (ReceiverDispatcher.this.mInstrumentation == null || !ReceiverDispatcher.this.mInstrumentation.onException(ReceiverDispatcher.this.mReceiver, e)) {
                        Trace.traceEnd(64L);
                        throw new RuntimeException("Error receiving broadcast " + intent + " in " + ReceiverDispatcher.this.mReceiver, e);
                    }
                }
                if (broadcastReceiver.getPendingResult() != null) {
                    args.finish();
                }
                Trace.traceEnd(64L);
            }
        }

        ReceiverDispatcher(BroadcastReceiver broadcastReceiver, Context context, Handler handler, Instrumentation instrumentation, boolean z) {
            if (handler == null) {
                throw new NullPointerException("Handler must not be null");
            }
            this.mIIntentReceiver = new InnerReceiver(this, !z);
            this.mReceiver = broadcastReceiver;
            this.mContext = context;
            this.mActivityThread = handler;
            this.mInstrumentation = instrumentation;
            this.mRegistered = z;
            this.mLocation = new IntentReceiverLeaked(null);
            this.mLocation.fillInStackTrace();
        }

        void validate(Context context, Handler handler) {
            if (this.mContext != context) {
                throw new IllegalStateException("Receiver " + this.mReceiver + " registered with differing Context (was " + this.mContext + " now " + context + ")");
            }
            if (this.mActivityThread != handler) {
                throw new IllegalStateException("Receiver " + this.mReceiver + " registered with differing handler (was " + this.mActivityThread + " now " + handler + ")");
            }
        }

        IntentReceiverLeaked getLocation() {
            return this.mLocation;
        }

        BroadcastReceiver getIntentReceiver() {
            return this.mReceiver;
        }

        IIntentReceiver getIIntentReceiver() {
            return this.mIIntentReceiver;
        }

        void setUnregisterLocation(RuntimeException runtimeException) {
            this.mUnregisterLocation = runtimeException;
        }

        RuntimeException getUnregisterLocation() {
            return this.mUnregisterLocation;
        }

        public void performReceive(Intent intent, int i, String str, Bundle bundle, boolean z, boolean z2, int i2) {
            Args args = new Args(intent, i, str, bundle, z, z2, i2);
            if (intent == null) {
                Log.wtf(LoadedApk.TAG, "Null intent received");
            } else if (ActivityThread.DEBUG_BROADCAST) {
                Slog.i(ActivityThread.TAG, "Enqueueing broadcast " + intent.getAction() + " seq=" + intent.getIntExtra("seq", -1) + " to " + this.mReceiver);
            }
            if ((intent == null || !this.mActivityThread.post(args.getRunnable())) && this.mRegistered && z) {
                IActivityManager service = ActivityManager.getService();
                if (ActivityThread.DEBUG_BROADCAST) {
                    Slog.i(ActivityThread.TAG, "Finishing sync broadcast to " + this.mReceiver);
                }
                args.sendFinished(service);
            }
        }
    }

    public final IServiceConnection getServiceDispatcher(ServiceConnection serviceConnection, Context context, Handler handler, int i) {
        IServiceConnection iServiceConnection;
        synchronized (this.mServices) {
            ServiceDispatcher serviceDispatcher = null;
            ArrayMap<ServiceConnection, ServiceDispatcher> arrayMap = this.mServices.get(context);
            if (arrayMap != null) {
                serviceDispatcher = arrayMap.get(serviceConnection);
            }
            if (serviceDispatcher == null) {
                serviceDispatcher = new ServiceDispatcher(serviceConnection, context, handler, i);
                if (arrayMap == null) {
                    arrayMap = new ArrayMap<>();
                    this.mServices.put(context, arrayMap);
                }
                arrayMap.put(serviceConnection, serviceDispatcher);
            } else {
                serviceDispatcher.validate(context, handler);
            }
            iServiceConnection = serviceDispatcher.getIServiceConnection();
        }
        return iServiceConnection;
    }

    public final IServiceConnection forgetServiceDispatcher(Context context, ServiceConnection serviceConnection) {
        ServiceDispatcher serviceDispatcher;
        ServiceDispatcher serviceDispatcher2;
        IServiceConnection iServiceConnection;
        synchronized (this.mServices) {
            ArrayMap<ServiceConnection, ServiceDispatcher> arrayMap = this.mServices.get(context);
            if (arrayMap != null && (serviceDispatcher2 = arrayMap.get(serviceConnection)) != null) {
                arrayMap.remove(serviceConnection);
                serviceDispatcher2.doForget();
                if (arrayMap.size() == 0) {
                    this.mServices.remove(context);
                }
                if ((serviceDispatcher2.getFlags() & 2) != 0) {
                    ArrayMap<ServiceConnection, ServiceDispatcher> arrayMap2 = this.mUnboundServices.get(context);
                    if (arrayMap2 == null) {
                        arrayMap2 = new ArrayMap<>();
                        this.mUnboundServices.put(context, arrayMap2);
                    }
                    IllegalArgumentException illegalArgumentException = new IllegalArgumentException("Originally unbound here:");
                    illegalArgumentException.fillInStackTrace();
                    serviceDispatcher2.setUnbindLocation(illegalArgumentException);
                    arrayMap2.put(serviceConnection, serviceDispatcher2);
                }
                iServiceConnection = serviceDispatcher2.getIServiceConnection();
            } else {
                ArrayMap<ServiceConnection, ServiceDispatcher> arrayMap3 = this.mUnboundServices.get(context);
                if (arrayMap3 != null && (serviceDispatcher = arrayMap3.get(serviceConnection)) != null) {
                    throw new IllegalArgumentException("Unbinding Service " + serviceConnection + " that was already unbound", serviceDispatcher.getUnbindLocation());
                }
                if (context == null) {
                    throw new IllegalStateException("Unbinding Service " + serviceConnection + " from Context that is no longer in use: " + context);
                }
                throw new IllegalArgumentException("Service not registered: " + serviceConnection);
            }
        }
        return iServiceConnection;
    }

    static final class ServiceDispatcher {
        private final Handler mActivityThread;
        private final ServiceConnection mConnection;
        private final Context mContext;
        private final int mFlags;
        private boolean mForgotten;
        private RuntimeException mUnbindLocation;
        private final ArrayMap<ComponentName, ConnectionInfo> mActiveConnections = new ArrayMap<>();
        private final InnerConnection mIServiceConnection = new InnerConnection(this);
        private final ServiceConnectionLeaked mLocation = new ServiceConnectionLeaked(null);

        private static class ConnectionInfo {
            IBinder binder;
            IBinder.DeathRecipient deathMonitor;

            private ConnectionInfo() {
            }
        }

        private static class InnerConnection extends IServiceConnection.Stub {
            final WeakReference<ServiceDispatcher> mDispatcher;

            InnerConnection(ServiceDispatcher serviceDispatcher) {
                this.mDispatcher = new WeakReference<>(serviceDispatcher);
            }

            @Override
            public void connected(ComponentName componentName, IBinder iBinder, boolean z) throws RemoteException {
                ServiceDispatcher serviceDispatcher = this.mDispatcher.get();
                if (serviceDispatcher != null) {
                    serviceDispatcher.connected(componentName, iBinder, z);
                }
            }
        }

        ServiceDispatcher(ServiceConnection serviceConnection, Context context, Handler handler, int i) {
            this.mConnection = serviceConnection;
            this.mContext = context;
            this.mActivityThread = handler;
            this.mLocation.fillInStackTrace();
            this.mFlags = i;
        }

        void validate(Context context, Handler handler) {
            if (this.mContext != context) {
                throw new RuntimeException("ServiceConnection " + this.mConnection + " registered with differing Context (was " + this.mContext + " now " + context + ")");
            }
            if (this.mActivityThread != handler) {
                throw new RuntimeException("ServiceConnection " + this.mConnection + " registered with differing handler (was " + this.mActivityThread + " now " + handler + ")");
            }
        }

        void doForget() {
            synchronized (this) {
                for (int i = 0; i < this.mActiveConnections.size(); i++) {
                    ConnectionInfo connectionInfoValueAt = this.mActiveConnections.valueAt(i);
                    connectionInfoValueAt.binder.unlinkToDeath(connectionInfoValueAt.deathMonitor, 0);
                }
                this.mActiveConnections.clear();
                this.mForgotten = true;
            }
        }

        ServiceConnectionLeaked getLocation() {
            return this.mLocation;
        }

        ServiceConnection getServiceConnection() {
            return this.mConnection;
        }

        IServiceConnection getIServiceConnection() {
            return this.mIServiceConnection;
        }

        int getFlags() {
            return this.mFlags;
        }

        void setUnbindLocation(RuntimeException runtimeException) {
            this.mUnbindLocation = runtimeException;
        }

        RuntimeException getUnbindLocation() {
            return this.mUnbindLocation;
        }

        public void connected(ComponentName componentName, IBinder iBinder, boolean z) {
            if (this.mActivityThread != null) {
                this.mActivityThread.post(new RunConnection(componentName, iBinder, 0, z));
            } else {
                doConnected(componentName, iBinder, z);
            }
        }

        public void death(ComponentName componentName, IBinder iBinder) {
            if (this.mActivityThread != null) {
                this.mActivityThread.post(new RunConnection(componentName, iBinder, 1, false));
            } else {
                doDeath(componentName, iBinder);
            }
        }

        public void doConnected(ComponentName componentName, IBinder iBinder, boolean z) {
            synchronized (this) {
                if (this.mForgotten) {
                    return;
                }
                ConnectionInfo connectionInfo = this.mActiveConnections.get(componentName);
                if (connectionInfo == null || connectionInfo.binder != iBinder) {
                    if (iBinder != null) {
                        ConnectionInfo connectionInfo2 = new ConnectionInfo();
                        connectionInfo2.binder = iBinder;
                        connectionInfo2.deathMonitor = new DeathMonitor(componentName, iBinder);
                        try {
                            iBinder.linkToDeath(connectionInfo2.deathMonitor, 0);
                            this.mActiveConnections.put(componentName, connectionInfo2);
                        } catch (RemoteException e) {
                            this.mActiveConnections.remove(componentName);
                            return;
                        }
                    } else {
                        this.mActiveConnections.remove(componentName);
                    }
                    if (connectionInfo != null) {
                        connectionInfo.binder.unlinkToDeath(connectionInfo.deathMonitor, 0);
                    }
                    if (connectionInfo != null) {
                        this.mConnection.onServiceDisconnected(componentName);
                    }
                    if (z) {
                        this.mConnection.onBindingDied(componentName);
                    }
                    if (iBinder != null) {
                        this.mConnection.onServiceConnected(componentName, iBinder);
                    } else {
                        this.mConnection.onNullBinding(componentName);
                    }
                }
            }
        }

        public void doDeath(ComponentName componentName, IBinder iBinder) {
            synchronized (this) {
                ConnectionInfo connectionInfo = this.mActiveConnections.get(componentName);
                if (connectionInfo != null && connectionInfo.binder == iBinder) {
                    this.mActiveConnections.remove(componentName);
                    connectionInfo.binder.unlinkToDeath(connectionInfo.deathMonitor, 0);
                    this.mConnection.onServiceDisconnected(componentName);
                }
            }
        }

        private final class RunConnection implements Runnable {
            final int mCommand;
            final boolean mDead;
            final ComponentName mName;
            final IBinder mService;

            RunConnection(ComponentName componentName, IBinder iBinder, int i, boolean z) {
                this.mName = componentName;
                this.mService = iBinder;
                this.mCommand = i;
                this.mDead = z;
            }

            @Override
            public void run() {
                if (this.mCommand == 0) {
                    ServiceDispatcher.this.doConnected(this.mName, this.mService, this.mDead);
                } else if (this.mCommand == 1) {
                    ServiceDispatcher.this.doDeath(this.mName, this.mService);
                }
            }
        }

        private final class DeathMonitor implements IBinder.DeathRecipient {
            final ComponentName mName;
            final IBinder mService;

            DeathMonitor(ComponentName componentName, IBinder iBinder) {
                this.mName = componentName;
                this.mService = iBinder;
            }

            @Override
            public void binderDied() {
                ServiceDispatcher.this.death(this.mName, this.mService);
            }
        }
    }
}
