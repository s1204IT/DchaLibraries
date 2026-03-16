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
import com.android.internal.content.PackageMonitor;
import com.android.server.voiceinteraction.SoundTriggerHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class ServiceWatcher implements ServiceConnection {
    private static final boolean D = false;
    public static final String EXTRA_SERVICE_IS_MULTIUSER = "serviceIsMultiuser";
    public static final String EXTRA_SERVICE_VERSION = "serviceVersion";
    private final String mAction;
    private IBinder mBinder;
    private final Context mContext;
    private final Handler mHandler;
    private final Runnable mNewServiceWork;
    private String mPackageName;
    private final PackageManager mPm;
    private final String mServicePackageName;
    private final List<HashSet<Signature>> mSignatureSets;
    private final String mTag;
    private Object mLock = new Object();
    private int mVersion = SoundTriggerHelper.STATUS_ERROR;
    private boolean mIsMultiuser = D;
    private final PackageMonitor mPackageMonitor = new PackageMonitor() {
        public void onPackageUpdateFinished(String packageName, int uid) {
            synchronized (ServiceWatcher.this.mLock) {
                if (packageName.equals(ServiceWatcher.this.mPackageName)) {
                    ServiceWatcher.this.unbindLocked();
                }
                ServiceWatcher.this.bindBestPackageLocked(null);
            }
        }

        public void onPackageAdded(String packageName, int uid) {
            synchronized (ServiceWatcher.this.mLock) {
                if (packageName.equals(ServiceWatcher.this.mPackageName)) {
                    ServiceWatcher.this.unbindLocked();
                }
                ServiceWatcher.this.bindBestPackageLocked(null);
            }
        }

        public void onPackageRemoved(String packageName, int uid) {
            synchronized (ServiceWatcher.this.mLock) {
                if (packageName.equals(ServiceWatcher.this.mPackageName)) {
                    ServiceWatcher.this.unbindLocked();
                    ServiceWatcher.this.bindBestPackageLocked(null);
                }
            }
        }

        public boolean onPackageChanged(String packageName, int uid, String[] components) {
            synchronized (ServiceWatcher.this.mLock) {
                if (packageName.equals(ServiceWatcher.this.mPackageName)) {
                    ServiceWatcher.this.unbindLocked();
                }
                ServiceWatcher.this.bindBestPackageLocked(null);
            }
            return super.onPackageChanged(packageName, uid, components);
        }
    };

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
        } else {
            String servicePackageName = resources.getString(defaultServicePackageNameResId);
            if (servicePackageName != null) {
                initialPackageNames.add(servicePackageName);
            }
            this.mServicePackageName = servicePackageName;
        }
        this.mSignatureSets = getSignatureSets(context, initialPackageNames);
    }

    public boolean start() {
        synchronized (this.mLock) {
            if (!bindBestPackageLocked(this.mServicePackageName)) {
                return D;
            }
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.USER_SWITCHED");
            this.mContext.registerReceiverAsUser(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if ("android.intent.action.USER_SWITCHED".equals(action)) {
                        ServiceWatcher.this.switchUser();
                    }
                }
            }, UserHandle.ALL, intentFilter, null, this.mHandler);
            if (this.mServicePackageName == null) {
                this.mPackageMonitor.register(this.mContext, (Looper) null, UserHandle.ALL, true);
            }
            return true;
        }
    }

    private boolean bindBestPackageLocked(String justCheckThisPackage) {
        Intent intent = new Intent(this.mAction);
        if (justCheckThisPackage != null) {
            intent.setPackage(justCheckThisPackage);
        }
        List<ResolveInfo> rInfos = this.mPm.queryIntentServicesAsUser(intent, 128, 0);
        int bestVersion = SoundTriggerHelper.STATUS_ERROR;
        String bestPackage = null;
        boolean bestIsMultiuser = D;
        if (rInfos != null) {
            for (ResolveInfo rInfo : rInfos) {
                String packageName = rInfo.serviceInfo.packageName;
                try {
                    PackageInfo pInfo = this.mPm.getPackageInfo(packageName, 64);
                    if (!isSignatureMatch(pInfo.signatures)) {
                        Log.w(this.mTag, packageName + " resolves service " + this.mAction + ", but has wrong signature, ignoring");
                    } else {
                        int version = SoundTriggerHelper.STATUS_ERROR;
                        boolean isMultiuser = D;
                        if (rInfo.serviceInfo.metaData != null) {
                            version = rInfo.serviceInfo.metaData.getInt(EXTRA_SERVICE_VERSION, SoundTriggerHelper.STATUS_ERROR);
                            isMultiuser = rInfo.serviceInfo.metaData.getBoolean(EXTRA_SERVICE_IS_MULTIUSER);
                        }
                        if (version > this.mVersion) {
                            bestVersion = version;
                            bestPackage = packageName;
                            bestIsMultiuser = isMultiuser;
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.wtf(this.mTag, e);
                }
            }
        }
        if (bestPackage == null) {
            return D;
        }
        bindToPackageLocked(bestPackage, bestVersion, bestIsMultiuser);
        return true;
    }

    private void unbindLocked() {
        String pkg = this.mPackageName;
        this.mPackageName = null;
        this.mVersion = SoundTriggerHelper.STATUS_ERROR;
        this.mIsMultiuser = D;
        if (pkg != null) {
            this.mContext.unbindService(this);
        }
    }

    private void bindToPackageLocked(String packageName, int version, boolean isMultiuser) {
        unbindLocked();
        Intent intent = new Intent(this.mAction);
        intent.setPackage(packageName);
        this.mPackageName = packageName;
        this.mVersion = version;
        this.mIsMultiuser = isMultiuser;
        this.mContext.bindServiceAsUser(intent, this, 1073741829, this.mIsMultiuser ? UserHandle.OWNER : UserHandle.CURRENT);
    }

    public static boolean isSignatureMatch(Signature[] signatures, List<HashSet<Signature>> sigSets) {
        if (signatures == null) {
            return D;
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
        return D;
    }

    private boolean isSignatureMatch(Signature[] signatures) {
        return isSignatureMatch(signatures, this.mSignatureSets);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        synchronized (this.mLock) {
            String packageName = name.getPackageName();
            if (packageName.equals(this.mPackageName)) {
                this.mBinder = binder;
                if (this.mHandler != null && this.mNewServiceWork != null) {
                    this.mHandler.post(this.mNewServiceWork);
                }
            } else {
                Log.w(this.mTag, "unexpected onServiceConnected: " + packageName);
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        synchronized (this.mLock) {
            String packageName = name.getPackageName();
            if (packageName.equals(this.mPackageName)) {
                this.mBinder = null;
            }
        }
    }

    public String getBestPackageName() {
        String str;
        synchronized (this.mLock) {
            str = this.mPackageName;
        }
        return str;
    }

    public int getBestVersion() {
        int i;
        synchronized (this.mLock) {
            i = this.mVersion;
        }
        return i;
    }

    public IBinder getBinder() {
        IBinder iBinder;
        synchronized (this.mLock) {
            iBinder = this.mBinder;
        }
        return iBinder;
    }

    public void switchUser() {
        synchronized (this.mLock) {
            if (!this.mIsMultiuser) {
                unbindLocked();
                bindBestPackageLocked(this.mServicePackageName);
            }
        }
    }
}
