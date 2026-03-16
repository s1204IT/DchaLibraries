package android.app;

import android.app.IServiceConnection;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.CompatibilityInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.StrictMode;
import android.os.Trace;
import android.os.UserHandle;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.view.DisplayAdjustments;
import dalvik.system.VMRuntime;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Objects;

public final class LoadedApk {
    static final boolean $assertionsDisabled;
    private static final String TAG = "LoadedApk";
    private final ActivityThread mActivityThread;
    private final String mAppDir;
    private Application mApplication;
    private ApplicationInfo mApplicationInfo;
    private final ClassLoader mBaseClassLoader;
    private ClassLoader mClassLoader;
    int mClientCount;
    private final String mDataDir;
    private final File mDataDirFile;
    private final DisplayAdjustments mDisplayAdjustments;
    private final boolean mIncludeCode;
    private final String mLibDir;
    private final String[] mOverlayDirs;
    final String mPackageName;
    private final ArrayMap<Context, ArrayMap<BroadcastReceiver, ReceiverDispatcher>> mReceivers;
    private final boolean mRegisterPackage;
    private final String mResDir;
    Resources mResources;
    private final boolean mSecurityViolation;
    private final ArrayMap<Context, ArrayMap<ServiceConnection, ServiceDispatcher>> mServices;
    private final String[] mSharedLibraries;
    private final String[] mSplitAppDirs;
    private final String[] mSplitResDirs;
    private final ArrayMap<Context, ArrayMap<ServiceConnection, ServiceDispatcher>> mUnboundServices;
    private final ArrayMap<Context, ArrayMap<BroadcastReceiver, ReceiverDispatcher>> mUnregisteredReceivers;

    static {
        $assertionsDisabled = !LoadedApk.class.desiredAssertionStatus();
    }

    Application getApplication() {
        return this.mApplication;
    }

    public LoadedApk(ActivityThread activityThread, ApplicationInfo aInfo, CompatibilityInfo compatInfo, ClassLoader baseLoader, boolean securityViolation, boolean includeCode, boolean registerPackage) {
        this.mDisplayAdjustments = new DisplayAdjustments();
        this.mReceivers = new ArrayMap<>();
        this.mUnregisteredReceivers = new ArrayMap<>();
        this.mServices = new ArrayMap<>();
        this.mUnboundServices = new ArrayMap<>();
        this.mClientCount = 0;
        int myUid = Process.myUid();
        ApplicationInfo aInfo2 = adjustNativeLibraryPaths(aInfo);
        this.mActivityThread = activityThread;
        this.mApplicationInfo = aInfo2;
        this.mPackageName = aInfo2.packageName;
        this.mAppDir = aInfo2.sourceDir;
        this.mResDir = aInfo2.uid == myUid ? aInfo2.sourceDir : aInfo2.publicSourceDir;
        this.mSplitAppDirs = aInfo2.splitSourceDirs;
        this.mSplitResDirs = aInfo2.uid == myUid ? aInfo2.splitSourceDirs : aInfo2.splitPublicSourceDirs;
        this.mOverlayDirs = aInfo2.resourceDirs;
        if (!UserHandle.isSameUser(aInfo2.uid, myUid) && !Process.isIsolated()) {
            aInfo2.dataDir = PackageManager.getDataDirForUser(UserHandle.getUserId(myUid), this.mPackageName);
        }
        this.mSharedLibraries = aInfo2.sharedLibraryFiles;
        this.mDataDir = aInfo2.dataDir;
        this.mDataDirFile = this.mDataDir != null ? new File(this.mDataDir) : null;
        this.mLibDir = aInfo2.nativeLibraryDir;
        this.mBaseClassLoader = baseLoader;
        this.mSecurityViolation = securityViolation;
        this.mIncludeCode = includeCode;
        this.mRegisterPackage = registerPackage;
        this.mDisplayAdjustments.setCompatibilityInfo(compatInfo);
    }

    private static ApplicationInfo adjustNativeLibraryPaths(ApplicationInfo info) {
        if (info.primaryCpuAbi != null && info.secondaryCpuAbi != null) {
            String runtimeIsa = VMRuntime.getRuntime().vmInstructionSet();
            String secondaryIsa = VMRuntime.getInstructionSet(info.secondaryCpuAbi);
            if (runtimeIsa.equals(secondaryIsa)) {
                ApplicationInfo modified = new ApplicationInfo(info);
                modified.nativeLibraryDir = modified.secondaryNativeLibraryDir;
                return modified;
            }
        }
        return info;
    }

    LoadedApk(ActivityThread activityThread) {
        this.mDisplayAdjustments = new DisplayAdjustments();
        this.mReceivers = new ArrayMap<>();
        this.mUnregisteredReceivers = new ArrayMap<>();
        this.mServices = new ArrayMap<>();
        this.mUnboundServices = new ArrayMap<>();
        this.mClientCount = 0;
        this.mActivityThread = activityThread;
        this.mApplicationInfo = new ApplicationInfo();
        this.mApplicationInfo.packageName = ZenModeConfig.SYSTEM_AUTHORITY;
        this.mPackageName = ZenModeConfig.SYSTEM_AUTHORITY;
        this.mAppDir = null;
        this.mResDir = null;
        this.mSplitAppDirs = null;
        this.mSplitResDirs = null;
        this.mOverlayDirs = null;
        this.mSharedLibraries = null;
        this.mDataDir = null;
        this.mDataDirFile = null;
        this.mLibDir = null;
        this.mBaseClassLoader = null;
        this.mSecurityViolation = false;
        this.mIncludeCode = true;
        this.mRegisterPackage = false;
        this.mClassLoader = ClassLoader.getSystemClassLoader();
        this.mResources = Resources.getSystem();
    }

    void installSystemApplicationInfo(ApplicationInfo info, ClassLoader classLoader) {
        if (!$assertionsDisabled && !info.packageName.equals(ZenModeConfig.SYSTEM_AUTHORITY)) {
            throw new AssertionError();
        }
        this.mApplicationInfo = info;
        this.mClassLoader = classLoader;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public ApplicationInfo getApplicationInfo() {
        return this.mApplicationInfo;
    }

    public boolean isSecurityViolation() {
        return this.mSecurityViolation;
    }

    public CompatibilityInfo getCompatibilityInfo() {
        return this.mDisplayAdjustments.getCompatibilityInfo();
    }

    public void setCompatibilityInfo(CompatibilityInfo compatInfo) {
        this.mDisplayAdjustments.setCompatibilityInfo(compatInfo);
    }

    private static String[] getLibrariesFor(String packageName) {
        try {
            ApplicationInfo ai = ActivityThread.getPackageManager().getApplicationInfo(packageName, 1024, UserHandle.myUserId());
            if (ai == null) {
                return null;
            }
            return ai.sharedLibraryFiles;
        } catch (RemoteException e) {
            throw new AssertionError(e);
        }
    }

    public ClassLoader getClassLoader() {
        ClassLoader classLoader;
        synchronized (this) {
            if (this.mClassLoader != null) {
                classLoader = this.mClassLoader;
            } else {
                if (this.mIncludeCode && !this.mPackageName.equals(ZenModeConfig.SYSTEM_AUTHORITY)) {
                    if (!Objects.equals(this.mPackageName, ActivityThread.currentPackageName())) {
                        String isa = VMRuntime.getRuntime().vmInstructionSet();
                        try {
                            ActivityThread.getPackageManager().performDexOptIfNeeded(this.mPackageName, isa);
                        } catch (RemoteException e) {
                        }
                    }
                    ArrayList<String> zipPaths = new ArrayList<>();
                    ArrayList<String> libPaths = new ArrayList<>();
                    if (this.mRegisterPackage) {
                        try {
                            ActivityManagerNative.getDefault().addPackageDependency(this.mPackageName);
                        } catch (RemoteException e2) {
                        }
                    }
                    zipPaths.add(this.mAppDir);
                    if (this.mSplitAppDirs != null) {
                        Collections.addAll(zipPaths, this.mSplitAppDirs);
                    }
                    libPaths.add(this.mLibDir);
                    String instrumentationPackageName = this.mActivityThread.mInstrumentationPackageName;
                    String instrumentationAppDir = this.mActivityThread.mInstrumentationAppDir;
                    String[] instrumentationSplitAppDirs = this.mActivityThread.mInstrumentationSplitAppDirs;
                    String instrumentationLibDir = this.mActivityThread.mInstrumentationLibDir;
                    String instrumentedAppDir = this.mActivityThread.mInstrumentedAppDir;
                    String[] instrumentedSplitAppDirs = this.mActivityThread.mInstrumentedSplitAppDirs;
                    String instrumentedLibDir = this.mActivityThread.mInstrumentedLibDir;
                    String[] instrumentationLibs = null;
                    if (this.mAppDir.equals(instrumentationAppDir) || this.mAppDir.equals(instrumentedAppDir)) {
                        zipPaths.clear();
                        zipPaths.add(instrumentationAppDir);
                        if (instrumentationSplitAppDirs != null) {
                            Collections.addAll(zipPaths, instrumentationSplitAppDirs);
                        }
                        zipPaths.add(instrumentedAppDir);
                        if (instrumentedSplitAppDirs != null) {
                            Collections.addAll(zipPaths, instrumentedSplitAppDirs);
                        }
                        libPaths.clear();
                        libPaths.add(instrumentationLibDir);
                        libPaths.add(instrumentedLibDir);
                        if (!instrumentedAppDir.equals(instrumentationAppDir)) {
                            instrumentationLibs = getLibrariesFor(instrumentationPackageName);
                        }
                    }
                    if (this.mSharedLibraries != null) {
                        String[] arr$ = this.mSharedLibraries;
                        for (String lib : arr$) {
                            if (!zipPaths.contains(lib)) {
                                zipPaths.add(0, lib);
                            }
                        }
                    }
                    if (instrumentationLibs != null) {
                        String[] arr$2 = instrumentationLibs;
                        for (String lib2 : arr$2) {
                            if (!zipPaths.contains(lib2)) {
                                zipPaths.add(0, lib2);
                            }
                        }
                    }
                    String zip = TextUtils.join(File.pathSeparator, zipPaths);
                    String lib3 = TextUtils.join(File.pathSeparator, libPaths);
                    StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskReads();
                    this.mClassLoader = ApplicationLoaders.getDefault().getClassLoader(zip, lib3, this.mBaseClassLoader);
                    StrictMode.setThreadPolicy(oldPolicy);
                } else if (this.mBaseClassLoader == null) {
                    this.mClassLoader = ClassLoader.getSystemClassLoader();
                } else {
                    this.mClassLoader = this.mBaseClassLoader;
                }
                classLoader = this.mClassLoader;
            }
        }
        return classLoader;
    }

    private void initializeJavaContextClassLoader() {
        IPackageManager pm = ActivityThread.getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(this.mPackageName, 0, UserHandle.myUserId());
            if (pi == null) {
                throw new IllegalStateException("Unable to get package info for " + this.mPackageName + "; is package not installed?");
            }
            boolean sharedUserIdSet = pi.sharedUserId != null;
            boolean processNameNotDefault = (pi.applicationInfo == null || this.mPackageName.equals(pi.applicationInfo.processName)) ? false : true;
            boolean sharable = sharedUserIdSet || processNameNotDefault;
            ClassLoader contextClassLoader = sharable ? new WarningContextClassLoader() : this.mClassLoader;
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        } catch (RemoteException e) {
            throw new IllegalStateException("Unable to get package info for " + this.mPackageName + "; is system dying?", e);
        }
    }

    private static class WarningContextClassLoader extends ClassLoader {
        private static boolean warned = false;

        private WarningContextClassLoader() {
        }

        private void warn(String methodName) {
            if (!warned) {
                warned = true;
                Thread.currentThread().setContextClassLoader(getParent());
                Slog.w(ActivityThread.TAG, "ClassLoader." + methodName + ": The class loader returned by Thread.getContextClassLoader() may fail for processes that host multiple applications. You should explicitly specify a context class loader. For example: Thread.setContextClassLoader(getClass().getClassLoader());");
            }
        }

        @Override
        public URL getResource(String resName) {
            warn("getResource");
            return getParent().getResource(resName);
        }

        @Override
        public Enumeration<URL> getResources(String resName) throws IOException {
            warn("getResources");
            return getParent().getResources(resName);
        }

        @Override
        public InputStream getResourceAsStream(String resName) {
            warn("getResourceAsStream");
            return getParent().getResourceAsStream(resName);
        }

        @Override
        public Class<?> loadClass(String className) throws ClassNotFoundException {
            warn("loadClass");
            return getParent().loadClass(className);
        }

        @Override
        public void setClassAssertionStatus(String cname, boolean enable) {
            warn("setClassAssertionStatus");
            getParent().setClassAssertionStatus(cname, enable);
        }

        @Override
        public void setPackageAssertionStatus(String pname, boolean enable) {
            warn("setPackageAssertionStatus");
            getParent().setPackageAssertionStatus(pname, enable);
        }

        @Override
        public void setDefaultAssertionStatus(boolean enable) {
            warn("setDefaultAssertionStatus");
            getParent().setDefaultAssertionStatus(enable);
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

    public AssetManager getAssets(ActivityThread mainThread) {
        return getResources(mainThread).getAssets();
    }

    public Resources getResources(ActivityThread mainThread) {
        if (this.mResources == null) {
            this.mResources = mainThread.getTopLevelResources(this.mResDir, this.mSplitResDirs, this.mOverlayDirs, this.mApplicationInfo.sharedLibraryFiles, 0, null, this);
        }
        return this.mResources;
    }

    public Application makeApplication(boolean forceDefaultAppClass, Instrumentation instrumentation) {
        if (this.mApplication != null) {
            return this.mApplication;
        }
        Application app = null;
        String appClass = this.mApplicationInfo.className;
        if (forceDefaultAppClass || appClass == null) {
            appClass = "android.app.Application";
        }
        try {
            ClassLoader cl = getClassLoader();
            if (!this.mPackageName.equals(ZenModeConfig.SYSTEM_AUTHORITY)) {
                initializeJavaContextClassLoader();
            }
            ContextImpl appContext = ContextImpl.createAppContext(this.mActivityThread, this);
            app = this.mActivityThread.mInstrumentation.newApplication(cl, appClass, appContext);
            appContext.setOuterContext(app);
        } catch (Exception e) {
            if (!this.mActivityThread.mInstrumentation.onException(app, e)) {
                throw new RuntimeException("Unable to instantiate application " + appClass + ": " + e.toString(), e);
            }
        }
        this.mActivityThread.mAllApplications.add(app);
        this.mApplication = app;
        if (instrumentation != null) {
            try {
                instrumentation.callApplicationOnCreate(app);
            } catch (Exception e2) {
                if (!instrumentation.onException(app, e2)) {
                    throw new RuntimeException("Unable to create application " + app.getClass().getName() + ": " + e2.toString(), e2);
                }
            }
        }
        SparseArray<String> packageIdentifiers = getAssets(this.mActivityThread).getAssignedPackageIdentifiers();
        int N = packageIdentifiers.size();
        for (int i = 0; i < N; i++) {
            int id = packageIdentifiers.keyAt(i);
            if (id != 1 && id != 127) {
                rewriteRValues(getClassLoader(), packageIdentifiers.valueAt(i), id);
            }
        }
        return app;
    }

    private void rewriteRValues(ClassLoader cl, String packageName, int id) {
        Throwable cause;
        try {
            Class<?> rClazz = cl.loadClass(packageName + ".R");
            try {
                Method callback = rClazz.getMethod("onResourcesLoaded", Integer.TYPE);
                try {
                    callback.invoke(null, Integer.valueOf(id));
                } catch (IllegalAccessException e) {
                    cause = e;
                    throw new RuntimeException("Failed to rewrite resource references for " + packageName, cause);
                } catch (InvocationTargetException e2) {
                    cause = e2.getCause();
                    throw new RuntimeException("Failed to rewrite resource references for " + packageName, cause);
                }
            } catch (NoSuchMethodException e3) {
            }
        } catch (ClassNotFoundException e4) {
            Log.i(TAG, "No resource references to update in package " + packageName);
        }
    }

    public void removeContextRegistrations(Context context, String who, String what) {
        boolean reportRegistrationLeaks = StrictMode.vmRegistrationLeaksEnabled();
        synchronized (this.mReceivers) {
            ArrayMap<BroadcastReceiver, ReceiverDispatcher> rmap = this.mReceivers.remove(context);
            if (rmap != null) {
                for (int i = 0; i < rmap.size(); i++) {
                    ReceiverDispatcher rd = rmap.valueAt(i);
                    IntentReceiverLeaked leak = new IntentReceiverLeaked(what + " " + who + " has leaked IntentReceiver " + rd.getIntentReceiver() + " that was originally registered here. Are you missing a call to unregisterReceiver()?");
                    leak.setStackTrace(rd.getLocation().getStackTrace());
                    Slog.e(ActivityThread.TAG, leak.getMessage(), leak);
                    if (reportRegistrationLeaks) {
                        StrictMode.onIntentReceiverLeaked(leak);
                    }
                    try {
                        ActivityManagerNative.getDefault().unregisterReceiver(rd.getIIntentReceiver());
                    } catch (RemoteException e) {
                    }
                }
            }
            this.mUnregisteredReceivers.remove(context);
        }
        synchronized (this.mServices) {
            ArrayMap<ServiceConnection, ServiceDispatcher> smap = this.mServices.remove(context);
            if (smap != null) {
                for (int i2 = 0; i2 < smap.size(); i2++) {
                    ServiceDispatcher sd = smap.valueAt(i2);
                    ServiceConnectionLeaked leak2 = new ServiceConnectionLeaked(what + " " + who + " has leaked ServiceConnection " + sd.getServiceConnection() + " that was originally bound here");
                    leak2.setStackTrace(sd.getLocation().getStackTrace());
                    Slog.e(ActivityThread.TAG, leak2.getMessage(), leak2);
                    if (reportRegistrationLeaks) {
                        StrictMode.onServiceConnectionLeaked(leak2);
                    }
                    try {
                        ActivityManagerNative.getDefault().unbindService(sd.getIServiceConnection());
                    } catch (RemoteException e2) {
                    }
                    sd.doForget();
                }
            }
            this.mUnboundServices.remove(context);
        }
    }

    public IIntentReceiver getReceiverDispatcher(BroadcastReceiver r, Context context, Handler handler, Instrumentation instrumentation, boolean registered) {
        ArrayMap<BroadcastReceiver, ReceiverDispatcher> map;
        ReceiverDispatcher rd;
        ReceiverDispatcher rd2;
        ArrayMap<BroadcastReceiver, ReceiverDispatcher> map2;
        synchronized (this.mReceivers) {
            ArrayMap<BroadcastReceiver, ReceiverDispatcher> map3 = null;
            if (registered) {
                try {
                    map3 = this.mReceivers.get(context);
                    if (map3 == null) {
                        map = map3;
                        rd = null;
                    } else {
                        ReceiverDispatcher rd3 = map3.get(r);
                        map = map3;
                        rd = rd3;
                    }
                    try {
                        if (rd == null) {
                            rd2 = new ReceiverDispatcher(r, context, handler, instrumentation, registered);
                            if (registered) {
                                if (map == null) {
                                    try {
                                        map2 = new ArrayMap<>();
                                        this.mReceivers.put(context, map2);
                                    } catch (Throwable th) {
                                        th = th;
                                    }
                                } else {
                                    map2 = map;
                                }
                                map2.put(r, rd2);
                            }
                        } else {
                            rd.validate(context, handler);
                            rd2 = rd;
                        }
                        rd2.mForgotten = false;
                        return rd2.getIIntentReceiver();
                    } catch (Throwable th2) {
                        th = th2;
                    }
                } catch (Throwable th3) {
                    th = th3;
                }
            }
            throw th;
        }
    }

    public IIntentReceiver forgetReceiverDispatcher(Context context, BroadcastReceiver r) {
        ReceiverDispatcher rd;
        IIntentReceiver iIntentReceiver;
        synchronized (this.mReceivers) {
            ArrayMap<BroadcastReceiver, ReceiverDispatcher> map = this.mReceivers.get(context);
            if (map != null) {
                ReceiverDispatcher rd2 = map.get(r);
                if (rd2 != null) {
                    map.remove(r);
                    if (map.size() == 0) {
                        this.mReceivers.remove(context);
                    }
                    if (r.getDebugUnregister()) {
                        ArrayMap<BroadcastReceiver, ReceiverDispatcher> holder = this.mUnregisteredReceivers.get(context);
                        if (holder == null) {
                            holder = new ArrayMap<>();
                            this.mUnregisteredReceivers.put(context, holder);
                        }
                        RuntimeException ex = new IllegalArgumentException("Originally unregistered here:");
                        ex.fillInStackTrace();
                        rd2.setUnregisterLocation(ex);
                        holder.put(r, rd2);
                    }
                    rd2.mForgotten = true;
                    iIntentReceiver = rd2.getIIntentReceiver();
                }
            }
            ArrayMap<BroadcastReceiver, ReceiverDispatcher> holder2 = this.mUnregisteredReceivers.get(context);
            if (holder2 != null && (rd = holder2.get(r)) != null) {
                throw new IllegalArgumentException("Unregistering Receiver " + r + " that was already unregistered", rd.getUnregisterLocation());
            }
            if (context == null) {
                throw new IllegalStateException("Unbinding Receiver " + r + " from Context that is no longer in use: " + context);
            }
            throw new IllegalArgumentException("Receiver not registered: " + r);
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

            InnerReceiver(ReceiverDispatcher rd, boolean strong) {
                this.mDispatcher = new WeakReference<>(rd);
                this.mStrongRef = strong ? rd : null;
            }

            @Override
            public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) {
                ReceiverDispatcher rd = this.mDispatcher.get();
                if (rd != null) {
                    rd.performReceive(intent, resultCode, data, extras, ordered, sticky, sendingUser);
                    return;
                }
                IActivityManager mgr = ActivityManagerNative.getDefault();
                if (extras != null) {
                    try {
                        extras.setAllowFds(false);
                    } catch (RemoteException e) {
                        Slog.w(ActivityThread.TAG, "Couldn't finish broadcast to unregistered receiver");
                        return;
                    }
                }
                mgr.finishReceiver(this, resultCode, data, extras, false);
            }
        }

        final class Args extends BroadcastReceiver.PendingResult implements Runnable {
            private Intent mCurIntent;
            private final boolean mOrdered;

            public Args(Intent intent, int resultCode, String resultData, Bundle resultExtras, boolean ordered, boolean sticky, int sendingUser) {
                super(resultCode, resultData, resultExtras, ReceiverDispatcher.this.mRegistered ? 1 : 2, ordered, sticky, ReceiverDispatcher.this.mIIntentReceiver.asBinder(), sendingUser);
                this.mCurIntent = intent;
                this.mOrdered = ordered;
            }

            @Override
            public void run() {
                BroadcastReceiver receiver = ReceiverDispatcher.this.mReceiver;
                boolean ordered = this.mOrdered;
                IActivityManager mgr = ActivityManagerNative.getDefault();
                Intent intent = this.mCurIntent;
                this.mCurIntent = null;
                if (receiver == null || ReceiverDispatcher.this.mForgotten) {
                    if (ReceiverDispatcher.this.mRegistered && ordered) {
                        sendFinished(mgr);
                        return;
                    }
                    return;
                }
                Trace.traceBegin(64L, "broadcastReceiveReg");
                try {
                    ClassLoader cl = ReceiverDispatcher.this.mReceiver.getClass().getClassLoader();
                    intent.setExtrasClassLoader(cl);
                    setExtrasClassLoader(cl);
                    receiver.setPendingResult(this);
                    receiver.onReceive(ReceiverDispatcher.this.mContext, intent);
                } catch (Exception e) {
                    if (ReceiverDispatcher.this.mRegistered && ordered) {
                        sendFinished(mgr);
                    }
                    if (ReceiverDispatcher.this.mInstrumentation == null || !ReceiverDispatcher.this.mInstrumentation.onException(ReceiverDispatcher.this.mReceiver, e)) {
                        Trace.traceEnd(64L);
                        throw new RuntimeException("Error receiving broadcast " + intent + " in " + ReceiverDispatcher.this.mReceiver, e);
                    }
                }
                if (receiver.getPendingResult() != null) {
                    finish();
                }
                Trace.traceEnd(64L);
            }
        }

        ReceiverDispatcher(BroadcastReceiver receiver, Context context, Handler activityThread, Instrumentation instrumentation, boolean registered) {
            if (activityThread == null) {
                throw new NullPointerException("Handler must not be null");
            }
            this.mIIntentReceiver = new InnerReceiver(this, !registered);
            this.mReceiver = receiver;
            this.mContext = context;
            this.mActivityThread = activityThread;
            this.mInstrumentation = instrumentation;
            this.mRegistered = registered;
            this.mLocation = new IntentReceiverLeaked(null);
            this.mLocation.fillInStackTrace();
        }

        void validate(Context context, Handler activityThread) {
            if (this.mContext != context) {
                throw new IllegalStateException("Receiver " + this.mReceiver + " registered with differing Context (was " + this.mContext + " now " + context + ")");
            }
            if (this.mActivityThread != activityThread) {
                throw new IllegalStateException("Receiver " + this.mReceiver + " registered with differing handler (was " + this.mActivityThread + " now " + activityThread + ")");
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

        void setUnregisterLocation(RuntimeException ex) {
            this.mUnregisterLocation = ex;
        }

        RuntimeException getUnregisterLocation() {
            return this.mUnregisterLocation;
        }

        public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) {
            Args args = new Args(intent, resultCode, data, extras, ordered, sticky, sendingUser);
            if (!this.mActivityThread.post(args) && this.mRegistered && ordered) {
                IActivityManager mgr = ActivityManagerNative.getDefault();
                args.sendFinished(mgr);
            }
        }
    }

    public final IServiceConnection getServiceDispatcher(ServiceConnection c, Context context, Handler handler, int flags) {
        ArrayMap<ServiceConnection, ServiceDispatcher> map;
        ServiceDispatcher sd;
        ServiceDispatcher sd2;
        synchronized (this.mServices) {
            try {
                map = this.mServices.get(context);
                if (map == null) {
                    sd = null;
                } else {
                    ServiceDispatcher sd3 = map.get(c);
                    sd = sd3;
                }
            } catch (Throwable th) {
                th = th;
            }
            try {
                if (sd == null) {
                    sd2 = new ServiceDispatcher(c, context, handler, flags);
                    if (map == null) {
                        map = new ArrayMap<>();
                        this.mServices.put(context, map);
                    }
                    map.put(c, sd2);
                } else {
                    sd.validate(context, handler);
                    sd2 = sd;
                }
                return sd2.getIServiceConnection();
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    public final IServiceConnection forgetServiceDispatcher(Context context, ServiceConnection c) {
        ServiceDispatcher sd;
        IServiceConnection iServiceConnection;
        synchronized (this.mServices) {
            ArrayMap<ServiceConnection, ServiceDispatcher> map = this.mServices.get(context);
            if (map != null) {
                ServiceDispatcher sd2 = map.get(c);
                if (sd2 != null) {
                    map.remove(c);
                    sd2.doForget();
                    if (map.size() == 0) {
                        this.mServices.remove(context);
                    }
                    if ((sd2.getFlags() & 2) != 0) {
                        ArrayMap<ServiceConnection, ServiceDispatcher> holder = this.mUnboundServices.get(context);
                        if (holder == null) {
                            holder = new ArrayMap<>();
                            this.mUnboundServices.put(context, holder);
                        }
                        RuntimeException ex = new IllegalArgumentException("Originally unbound here:");
                        ex.fillInStackTrace();
                        sd2.setUnbindLocation(ex);
                        holder.put(c, sd2);
                    }
                    iServiceConnection = sd2.getIServiceConnection();
                }
            }
            ArrayMap<ServiceConnection, ServiceDispatcher> holder2 = this.mUnboundServices.get(context);
            if (holder2 != null && (sd = holder2.get(c)) != null) {
                throw new IllegalArgumentException("Unbinding Service " + c + " that was already unbound", sd.getUnbindLocation());
            }
            if (context == null) {
                throw new IllegalStateException("Unbinding Service " + c + " from Context that is no longer in use: " + context);
            }
            throw new IllegalArgumentException("Service not registered: " + c);
        }
        return iServiceConnection;
    }

    static final class ServiceDispatcher {
        private final Handler mActivityThread;
        private final ServiceConnection mConnection;
        private final Context mContext;
        private boolean mDied;
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

            InnerConnection(ServiceDispatcher sd) {
                this.mDispatcher = new WeakReference<>(sd);
            }

            @Override
            public void connected(ComponentName name, IBinder service) throws RemoteException {
                ServiceDispatcher sd = this.mDispatcher.get();
                if (sd != null) {
                    sd.connected(name, service);
                }
            }
        }

        ServiceDispatcher(ServiceConnection conn, Context context, Handler activityThread, int flags) {
            this.mConnection = conn;
            this.mContext = context;
            this.mActivityThread = activityThread;
            this.mLocation.fillInStackTrace();
            this.mFlags = flags;
        }

        void validate(Context context, Handler activityThread) {
            if (this.mContext != context) {
                throw new RuntimeException("ServiceConnection " + this.mConnection + " registered with differing Context (was " + this.mContext + " now " + context + ")");
            }
            if (this.mActivityThread != activityThread) {
                throw new RuntimeException("ServiceConnection " + this.mConnection + " registered with differing handler (was " + this.mActivityThread + " now " + activityThread + ")");
            }
        }

        void doForget() {
            synchronized (this) {
                for (int i = 0; i < this.mActiveConnections.size(); i++) {
                    ConnectionInfo ci = this.mActiveConnections.valueAt(i);
                    ci.binder.unlinkToDeath(ci.deathMonitor, 0);
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

        void setUnbindLocation(RuntimeException ex) {
            this.mUnbindLocation = ex;
        }

        RuntimeException getUnbindLocation() {
            return this.mUnbindLocation;
        }

        public void connected(ComponentName name, IBinder service) {
            if (this.mActivityThread != null) {
                this.mActivityThread.post(new RunConnection(name, service, 0));
            } else {
                doConnected(name, service);
            }
        }

        public void death(ComponentName name, IBinder service) {
            synchronized (this) {
                this.mDied = true;
                ConnectionInfo old = this.mActiveConnections.remove(name);
                if (old != null && old.binder == service) {
                    old.binder.unlinkToDeath(old.deathMonitor, 0);
                    if (this.mActivityThread != null) {
                        this.mActivityThread.post(new RunConnection(name, service, 1));
                    } else {
                        doDeath(name, service);
                    }
                }
            }
        }

        public void doConnected(ComponentName name, IBinder service) {
            synchronized (this) {
                if (!this.mForgotten) {
                    ConnectionInfo old = this.mActiveConnections.get(name);
                    if (old == null || old.binder != service) {
                        if (service != null) {
                            this.mDied = false;
                            ConnectionInfo info = new ConnectionInfo();
                            info.binder = service;
                            info.deathMonitor = new DeathMonitor(name, service);
                            try {
                                service.linkToDeath(info.deathMonitor, 0);
                                this.mActiveConnections.put(name, info);
                            } catch (RemoteException e) {
                                this.mActiveConnections.remove(name);
                                return;
                            }
                        } else {
                            this.mActiveConnections.remove(name);
                        }
                        if (old != null) {
                            old.binder.unlinkToDeath(old.deathMonitor, 0);
                        }
                        if (old != null) {
                            this.mConnection.onServiceDisconnected(name);
                        }
                        if (service != null) {
                            this.mConnection.onServiceConnected(name, service);
                        }
                    }
                }
            }
        }

        public void doDeath(ComponentName name, IBinder service) {
            this.mConnection.onServiceDisconnected(name);
        }

        private final class RunConnection implements Runnable {
            final int mCommand;
            final ComponentName mName;
            final IBinder mService;

            RunConnection(ComponentName name, IBinder service, int command) {
                this.mName = name;
                this.mService = service;
                this.mCommand = command;
            }

            @Override
            public void run() {
                if (this.mCommand == 0) {
                    ServiceDispatcher.this.doConnected(this.mName, this.mService);
                } else if (this.mCommand == 1) {
                    ServiceDispatcher.this.doDeath(this.mName, this.mService);
                }
            }
        }

        private final class DeathMonitor implements IBinder.DeathRecipient {
            final ComponentName mName;
            final IBinder mService;

            DeathMonitor(ComponentName name, IBinder service) {
                this.mName = name;
                this.mService = service;
            }

            @Override
            public void binderDied() {
                ServiceDispatcher.this.death(this.mName, this.mService);
            }
        }
    }
}
