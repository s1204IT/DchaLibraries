package com.android.server;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.content.PackageMonitor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class ServiceWatcher implements ServiceConnection {
    private static final boolean D = LocationManagerService.D;
    public static final String EXTRA_SERVICE_IS_MULTIUSER = "serviceIsMultiuser";
    public static final String EXTRA_SERVICE_VERSION = "serviceVersion";
    private static final String GMS_PACKAGE_NAME = "com.google.android.gms";
    private final String mAction;

    @GuardedBy("mLock")
    private ComponentName mBoundComponent;

    @GuardedBy("mLock")
    private String mBoundPackageName;

    @GuardedBy("mLock")
    private IBinder mBoundService;

    @GuardedBy("mLock")
    private int mBoundUserId;

    @GuardedBy("mLock")
    private int mBoundVersion;
    private final Context mContext;

    @GuardedBy("mLock")
    private int mCurrentUserId;
    private final Handler mHandler;
    private final Object mLock;
    private final Runnable mNewServiceWork;
    private final PackageMonitor mPackageMonitor;
    private final PackageManager mPm;
    private int mPreferPackageNameResId;
    String[] mPreferPkgs;
    private final String mServicePackageName;
    private final List<HashSet<Signature>> mSignatureSets;
    private final String mTag;

    public static ArrayList<HashSet<Signature>> getSignatureSets(Context context, List<String> initialPackageNames) {
        PackageManager pm = context.getPackageManager();
        ArrayList<HashSet<Signature>> sigSets = new ArrayList<>();
        int size = initialPackageNames.size();
        for (int i = 0; i < size; i++) {
            String pkg = initialPackageNames.get(i);
            try {
                HashSet<Signature> set = new HashSet<>();
                Signature[] sigs = pm.getPackageInfo(pkg, 64).signatures;
                set.addAll(Arrays.asList(sigs));
                sigSets.add(set);
            } catch (PackageManager.NameNotFoundException e) {
                Log.w("ServiceWatcher", pkg + " not found");
            }
        }
        return sigSets;
    }

    public ServiceWatcher(Context context, String logTag, String action, int overlaySwitchResId, int defaultServicePackageNameResId, int initialPackageNamesResId, Runnable newServiceWork, Handler handler) {
        this.mPreferPackageNameResId = 0;
        this.mPreferPkgs = null;
        this.mLock = new Object();
        this.mCurrentUserId = 0;
        this.mBoundVersion = Integer.MIN_VALUE;
        this.mBoundUserId = -10000;
        this.mPackageMonitor = new PackageMonitor() {
            public void onPackageUpdateFinished(String packageName, int uid) {
                synchronized (ServiceWatcher.this.mLock) {
                    boolean forceRebind = Objects.equals(packageName, ServiceWatcher.this.mBoundPackageName);
                    ServiceWatcher.this.bindBestPackageLocked(null, forceRebind);
                }
            }

            public void onPackageAdded(String packageName, int uid) {
                synchronized (ServiceWatcher.this.mLock) {
                    boolean forceRebind = Objects.equals(packageName, ServiceWatcher.this.mBoundPackageName);
                    ServiceWatcher.this.bindBestPackageLocked(null, forceRebind);
                }
            }

            public void onPackageRemoved(String packageName, int uid) {
                synchronized (ServiceWatcher.this.mLock) {
                    boolean forceRebind = Objects.equals(packageName, ServiceWatcher.this.mBoundPackageName);
                    ServiceWatcher.this.bindBestPackageLocked(null, forceRebind);
                }
            }

            public boolean onPackageChanged(String packageName, int uid, String[] components) {
                synchronized (ServiceWatcher.this.mLock) {
                    boolean forceRebind = Objects.equals(packageName, ServiceWatcher.this.mBoundPackageName);
                    ServiceWatcher.this.bindBestPackageLocked(null, forceRebind);
                }
                return super.onPackageChanged(packageName, uid, components);
            }
        };
        this.mContext = context;
        this.mTag = logTag;
        this.mAction = action;
        this.mPm = this.mContext.getPackageManager();
        this.mNewServiceWork = newServiceWork;
        this.mHandler = handler;
        Resources resources = context.getResources();
        boolean enableOverlay = resources.getBoolean(overlaySwitchResId);
        ArrayList<String> initialPackageNames = new ArrayList<>();
        if (enableOverlay) {
            String[] pkgs = resources.getStringArray(initialPackageNamesResId);
            if (pkgs != null) {
                initialPackageNames.addAll(Arrays.asList(pkgs));
            }
            this.mServicePackageName = null;
            Log.d(this.mTag, "Overlay enabled, packages=" + Arrays.toString(pkgs));
        } else {
            String servicePackageName = resources.getString(defaultServicePackageNameResId);
            if (servicePackageName != null) {
                initialPackageNames.add(servicePackageName);
            }
            this.mServicePackageName = servicePackageName;
            Log.d(this.mTag, "Overlay disabled, default package=" + servicePackageName);
        }
        this.mSignatureSets = getSignatureSets(context, initialPackageNames);
    }

    public boolean start() {
        synchronized (this.mLock) {
            bindBestPackageLocked(this.mServicePackageName, false);
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        intentFilter.addAction("android.intent.action.USER_UNLOCKED");
        this.mContext.registerReceiverAsUser(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                if ("android.intent.action.USER_SWITCHED".equals(action)) {
                    ServiceWatcher.this.switchUser(userId);
                } else {
                    if (!"android.intent.action.USER_UNLOCKED".equals(action)) {
                        return;
                    }
                    ServiceWatcher.this.unlockUser(userId);
                }
            }
        }, UserHandle.ALL, intentFilter, null, this.mHandler);
        if (this.mServicePackageName == null) {
            this.mPackageMonitor.register(this.mContext, (Looper) null, UserHandle.ALL, true);
        }
        return true;
    }

    public ServiceWatcher(Context context, String logTag, String action, int overlaySwitchResId, int defaultServicePackageNameResId, int initialPackageNamesResId, int vendorPackageNamesResId, int preferPackageNamesResId, Runnable newServiceWork, Handler handler) {
        this.mPreferPackageNameResId = 0;
        this.mPreferPkgs = null;
        this.mLock = new Object();
        this.mCurrentUserId = 0;
        this.mBoundVersion = Integer.MIN_VALUE;
        this.mBoundUserId = -10000;
        this.mPackageMonitor = new PackageMonitor() {
            public void onPackageUpdateFinished(String packageName, int uid) {
                synchronized (ServiceWatcher.this.mLock) {
                    boolean forceRebind = Objects.equals(packageName, ServiceWatcher.this.mBoundPackageName);
                    ServiceWatcher.this.bindBestPackageLocked(null, forceRebind);
                }
            }

            public void onPackageAdded(String packageName, int uid) {
                synchronized (ServiceWatcher.this.mLock) {
                    boolean forceRebind = Objects.equals(packageName, ServiceWatcher.this.mBoundPackageName);
                    ServiceWatcher.this.bindBestPackageLocked(null, forceRebind);
                }
            }

            public void onPackageRemoved(String packageName, int uid) {
                synchronized (ServiceWatcher.this.mLock) {
                    boolean forceRebind = Objects.equals(packageName, ServiceWatcher.this.mBoundPackageName);
                    ServiceWatcher.this.bindBestPackageLocked(null, forceRebind);
                }
            }

            public boolean onPackageChanged(String packageName, int uid, String[] components) {
                synchronized (ServiceWatcher.this.mLock) {
                    boolean forceRebind = Objects.equals(packageName, ServiceWatcher.this.mBoundPackageName);
                    ServiceWatcher.this.bindBestPackageLocked(null, forceRebind);
                }
                return super.onPackageChanged(packageName, uid, components);
            }
        };
        this.mContext = context;
        this.mTag = logTag;
        this.mAction = action;
        this.mPm = this.mContext.getPackageManager();
        this.mNewServiceWork = newServiceWork;
        this.mHandler = handler;
        Resources resources = context.getResources();
        boolean enableOverlay = resources.getBoolean(overlaySwitchResId);
        ArrayList<String> initialPackageNames = new ArrayList<>();
        if (enableOverlay) {
            String[] pkgs = resources.getStringArray(initialPackageNamesResId);
            if (pkgs != null) {
                initialPackageNames.addAll(Arrays.asList(pkgs));
            }
            String[] vendorPkgs = resources.getStringArray(vendorPackageNamesResId);
            if (vendorPkgs != null) {
                initialPackageNames.addAll(Arrays.asList(vendorPkgs));
            }
            this.mServicePackageName = null;
            Log.d(this.mTag, "Overlay enabled, packages=" + Arrays.toString(pkgs));
        } else {
            String servicePackageName = resources.getString(defaultServicePackageNameResId);
            if (servicePackageName != null) {
                initialPackageNames.add(servicePackageName);
            }
            this.mServicePackageName = servicePackageName;
            Log.d(this.mTag, "Overlay disabled, default package=" + servicePackageName);
        }
        this.mSignatureSets = getSignatureSets(context, initialPackageNames);
        this.mPreferPackageNameResId = preferPackageNamesResId;
        if (this.mPreferPackageNameResId > 0) {
            this.mPreferPkgs = resources.getStringArray(this.mPreferPackageNameResId);
        } else {
            this.mPreferPkgs = new String[1];
            this.mPreferPkgs[0] = GMS_PACKAGE_NAME;
        }
        if (D) {
            Log.d(this.mTag, "Constructed, mPreferPkgs[0] = " + this.mPreferPkgs[0]);
        }
    }

    public void stop() {
        if (D) {
            Log.d(this.mTag, "Stop");
        }
        synchronized (this.mLock) {
            unbindLocked();
            try {
                if (D) {
                    Log.d(this.mTag, "mPackageMonitor.unregister()");
                }
                this.mPackageMonitor.unregister();
            } catch (Exception e) {
            }
        }
    }

    private boolean isPreferredPackage(String packageName) {
        if (this.mPreferPkgs != null) {
            for (String s : this.mPreferPkgs) {
                if (packageName != null && s.equals(packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean bindBestPackageLocked(String justCheckThisPackage, boolean forceRebind) {
        Intent intent = new Intent(this.mAction);
        if (justCheckThisPackage != null) {
            intent.setPackage(justCheckThisPackage);
        }
        List<ResolveInfo> rInfos = this.mPm.queryIntentServicesAsUser(intent, 268435584, this.mCurrentUserId);
        int bestVersion = Integer.MIN_VALUE;
        ComponentName bestComponent = null;
        boolean bestIsMultiuser = false;
        if (rInfos != null) {
            Iterator rInfo$iterator = rInfos.iterator();
            while (true) {
                if (!rInfo$iterator.hasNext()) {
                    break;
                }
                ResolveInfo rInfo = (ResolveInfo) rInfo$iterator.next();
                ComponentName component = rInfo.serviceInfo.getComponentName();
                String packageName = component.getPackageName();
                try {
                    PackageInfo pInfo = this.mPm.getPackageInfo(packageName, 268435520);
                    if (!isSignatureMatch(pInfo.signatures)) {
                        Log.w(this.mTag, packageName + " resolves service " + this.mAction + ", but has wrong signature, ignoring");
                    } else {
                        int version = Integer.MIN_VALUE;
                        boolean isMultiuser = false;
                        if (rInfo.serviceInfo.metaData != null) {
                            version = rInfo.serviceInfo.metaData.getInt(EXTRA_SERVICE_VERSION, Integer.MIN_VALUE);
                            isMultiuser = rInfo.serviceInfo.metaData.getBoolean(EXTRA_SERVICE_IS_MULTIUSER);
                        }
                        if (isPreferredPackage(packageName)) {
                            bestVersion = version;
                            bestComponent = component;
                            bestIsMultiuser = isMultiuser;
                            break;
                        }
                        if (version > bestVersion) {
                            bestVersion = version;
                            bestComponent = component;
                            bestIsMultiuser = isMultiuser;
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.wtf(this.mTag, e);
                }
            }
            if (D) {
                String str = this.mTag;
                Object[] objArr = new Object[4];
                objArr[0] = this.mAction;
                objArr[1] = justCheckThisPackage == null ? "" : "(" + justCheckThisPackage + ") ";
                objArr[2] = Integer.valueOf(rInfos.size());
                objArr[3] = bestComponent == null ? "no new best component" : "new best component: " + bestComponent;
                Log.d(str, String.format("bindBestPackage for %s : %s found %d, %s", objArr));
            }
        } else if (D) {
            Log.d(this.mTag, "Unable to query intent services for action: " + this.mAction);
        }
        if (bestComponent == null) {
            Slog.w(this.mTag, "Odd, no component found for service " + this.mAction);
            unbindLocked();
            return false;
        }
        int userId = bestIsMultiuser ? 0 : this.mCurrentUserId;
        boolean alreadyBound = Objects.equals(bestComponent, this.mBoundComponent) && bestVersion == this.mBoundVersion && userId == this.mBoundUserId;
        if (forceRebind || !alreadyBound) {
            unbindLocked();
            bindToPackageLocked(bestComponent, bestVersion, userId);
            return true;
        }
        return true;
    }

    private void unbindLocked() {
        ComponentName component = this.mBoundComponent;
        this.mBoundComponent = null;
        this.mBoundPackageName = null;
        this.mBoundVersion = Integer.MIN_VALUE;
        this.mBoundUserId = -10000;
        if (component == null) {
            return;
        }
        if (D) {
            Log.d(this.mTag, "unbinding " + component);
        }
        this.mContext.unbindService(this);
    }

    private void bindToPackageLocked(ComponentName component, int version, int userId) {
        Intent intent = new Intent(this.mAction);
        intent.setComponent(component);
        this.mBoundComponent = component;
        this.mBoundPackageName = component.getPackageName();
        this.mBoundVersion = version;
        this.mBoundUserId = userId;
        Log.d(this.mTag, "binding " + component + " (v" + version + ") (u" + userId + ")");
        this.mContext.bindServiceAsUser(intent, this, 1073741829, new UserHandle(userId));
    }

    public static boolean isSignatureMatch(Signature[] signatures, List<HashSet<Signature>> sigSets) {
        if (signatures == null) {
            return false;
        }
        HashSet<Signature> inputSet = new HashSet<>();
        for (Signature s : signatures) {
            inputSet.add(s);
        }
        for (HashSet<Signature> referenceSet : sigSets) {
            if (referenceSet.equals(inputSet)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSignatureMatch(Signature[] signatures) {
        return isSignatureMatch(signatures, this.mSignatureSets);
    }

    @Override
    public void onServiceConnected(ComponentName component, IBinder binder) {
        synchronized (this.mLock) {
            if (component.equals(this.mBoundComponent)) {
                Log.d(this.mTag, component + " connected");
                this.mBoundService = binder;
                if (this.mHandler != null && this.mNewServiceWork != null) {
                    this.mHandler.post(this.mNewServiceWork);
                }
            } else {
                Log.w(this.mTag, "unexpected onServiceConnected: " + component);
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName component) {
        synchronized (this.mLock) {
            Log.d(this.mTag, component + " disconnected");
            if (component.equals(this.mBoundComponent)) {
                this.mBoundService = null;
            }
        }
    }

    public String getBestPackageName() {
        String str;
        synchronized (this.mLock) {
            str = this.mBoundPackageName;
        }
        return str;
    }

    public int getBestVersion() {
        int i;
        synchronized (this.mLock) {
            i = this.mBoundVersion;
        }
        return i;
    }

    public IBinder getBinder() {
        IBinder iBinder;
        synchronized (this.mLock) {
            iBinder = this.mBoundService;
        }
        return iBinder;
    }

    public void switchUser(int userId) {
        synchronized (this.mLock) {
            this.mCurrentUserId = userId;
            bindBestPackageLocked(this.mServicePackageName, false);
        }
    }

    public void unlockUser(int userId) {
        synchronized (this.mLock) {
            if (userId == this.mCurrentUserId) {
                bindBestPackageLocked(this.mServicePackageName, false);
            }
        }
    }
}
