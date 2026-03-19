package com.android.server;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
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
    private static final boolean D = false;
    public static final String EXTRA_SERVICE_IS_MULTIUSER = "serviceIsMultiuser";
    public static final String EXTRA_SERVICE_VERSION = "serviceVersion";
    private final String mAction;

    @GuardedBy("mLock")
    private ComponentName mBoundComponent;

    @GuardedBy("mLock")
    private String mBoundPackageName;

    @GuardedBy("mLock")
    private IBinder mBoundService;
    private final Context mContext;
    private final Handler mHandler;
    private final Runnable mNewServiceWork;
    private final PackageManager mPm;
    private final String mServicePackageName;
    private final List<HashSet<Signature>> mSignatureSets;
    private final String mTag;
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private int mCurrentUserId = 0;

    @GuardedBy("mLock")
    private int mBoundVersion = Integer.MIN_VALUE;

    @GuardedBy("mLock")
    private int mBoundUserId = -10000;
    private final PackageMonitor mPackageMonitor = new PackageMonitor() {
        public void onPackageUpdateFinished(String str, int i) {
            synchronized (ServiceWatcher.this.mLock) {
                ServiceWatcher.this.bindBestPackageLocked(null, Objects.equals(str, ServiceWatcher.this.mBoundPackageName));
            }
        }

        public void onPackageAdded(String str, int i) {
            synchronized (ServiceWatcher.this.mLock) {
                ServiceWatcher.this.bindBestPackageLocked(null, Objects.equals(str, ServiceWatcher.this.mBoundPackageName));
            }
        }

        public void onPackageRemoved(String str, int i) {
            synchronized (ServiceWatcher.this.mLock) {
                ServiceWatcher.this.bindBestPackageLocked(null, Objects.equals(str, ServiceWatcher.this.mBoundPackageName));
            }
        }

        public boolean onPackageChanged(String str, int i, String[] strArr) {
            synchronized (ServiceWatcher.this.mLock) {
                ServiceWatcher.this.bindBestPackageLocked(null, Objects.equals(str, ServiceWatcher.this.mBoundPackageName));
            }
            return super.onPackageChanged(str, i, strArr);
        }
    };

    public interface BinderRunner {
        void run(IBinder iBinder);
    }

    public static ArrayList<HashSet<Signature>> getSignatureSets(Context context, List<String> list) {
        PackageManager packageManager = context.getPackageManager();
        ArrayList<HashSet<Signature>> arrayList = new ArrayList<>();
        int size = list.size();
        for (int i = 0; i < size; i++) {
            String str = list.get(i);
            try {
                HashSet<Signature> hashSet = new HashSet<>();
                hashSet.addAll(Arrays.asList(packageManager.getPackageInfo(str, 1048640).signatures));
                arrayList.add(hashSet);
            } catch (PackageManager.NameNotFoundException e) {
                Log.w("ServiceWatcher", str + " not found");
            }
        }
        return arrayList;
    }

    public ServiceWatcher(Context context, String str, String str2, int i, int i2, int i3, Runnable runnable, Handler handler) {
        this.mContext = context;
        this.mTag = str;
        this.mAction = str2;
        this.mPm = this.mContext.getPackageManager();
        this.mNewServiceWork = runnable;
        this.mHandler = handler;
        Resources resources = context.getResources();
        boolean z = resources.getBoolean(i);
        ArrayList arrayList = new ArrayList();
        if (z) {
            String[] stringArray = resources.getStringArray(i3);
            if (stringArray != null) {
                arrayList.addAll(Arrays.asList(stringArray));
            }
            this.mServicePackageName = null;
        } else {
            String string = resources.getString(i2);
            if (string != null) {
                arrayList.add(string);
            }
            this.mServicePackageName = string;
        }
        this.mSignatureSets = getSignatureSets(context, arrayList);
    }

    public boolean start() {
        if (isServiceMissing()) {
            return false;
        }
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
                int intExtra = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                if ("android.intent.action.USER_SWITCHED".equals(action)) {
                    ServiceWatcher.this.switchUser(intExtra);
                } else if ("android.intent.action.USER_UNLOCKED".equals(action)) {
                    ServiceWatcher.this.unlockUser(intExtra);
                }
            }
        }, UserHandle.ALL, intentFilter, null, this.mHandler);
        if (this.mServicePackageName == null) {
            this.mPackageMonitor.register(this.mContext, (Looper) null, UserHandle.ALL, true);
        }
        return true;
    }

    private boolean isServiceMissing() {
        return this.mPm.queryIntentServicesAsUser(new Intent(this.mAction), 786432, this.mCurrentUserId).isEmpty();
    }

    @GuardedBy("mLock")
    private boolean bindBestPackageLocked(String str, boolean z) {
        boolean z2;
        int i;
        int i2;
        boolean z3;
        Intent intent = new Intent(this.mAction);
        if (str != null) {
            intent.setPackage(str);
        }
        List<ResolveInfo> listQueryIntentServicesAsUser = this.mPm.queryIntentServicesAsUser(intent, 268435584, this.mCurrentUserId);
        ComponentName componentName = null;
        int i3 = Integer.MIN_VALUE;
        boolean z4 = false;
        if (listQueryIntentServicesAsUser != null) {
            int i4 = Integer.MIN_VALUE;
            z2 = false;
            for (ResolveInfo resolveInfo : listQueryIntentServicesAsUser) {
                ComponentName componentName2 = resolveInfo.serviceInfo.getComponentName();
                String packageName = componentName2.getPackageName();
                try {
                    if (!isSignatureMatch(this.mPm.getPackageInfo(packageName, 268435520).signatures)) {
                        Log.w(this.mTag, packageName + " resolves service " + this.mAction + ", but has wrong signature, ignoring");
                    } else {
                        if (resolveInfo.serviceInfo.metaData != null) {
                            i2 = resolveInfo.serviceInfo.metaData.getInt(EXTRA_SERVICE_VERSION, Integer.MIN_VALUE);
                            z3 = resolveInfo.serviceInfo.metaData.getBoolean(EXTRA_SERVICE_IS_MULTIUSER);
                        } else {
                            i2 = Integer.MIN_VALUE;
                            z3 = false;
                        }
                        if (i2 > i4) {
                            z2 = z3;
                            componentName = componentName2;
                            i4 = i2;
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.wtf(this.mTag, e);
                }
            }
            i3 = i4;
        } else {
            z2 = false;
        }
        if (componentName == null) {
            Slog.w(this.mTag, "Odd, no component found for service " + this.mAction);
            unbindLocked();
            return false;
        }
        if (!z2) {
            i = this.mCurrentUserId;
        } else {
            i = 0;
        }
        if (Objects.equals(componentName, this.mBoundComponent) && i3 == this.mBoundVersion && i == this.mBoundUserId) {
            z4 = true;
        }
        if (z || !z4) {
            unbindLocked();
            bindToPackageLocked(componentName, i3, i);
        }
        return true;
    }

    @GuardedBy("mLock")
    private void unbindLocked() {
        ComponentName componentName = this.mBoundComponent;
        this.mBoundComponent = null;
        this.mBoundPackageName = null;
        this.mBoundVersion = Integer.MIN_VALUE;
        this.mBoundUserId = -10000;
        if (componentName != null) {
            this.mBoundService = null;
            this.mContext.unbindService(this);
        }
    }

    @GuardedBy("mLock")
    private void bindToPackageLocked(ComponentName componentName, int i, int i2) {
        Intent intent = new Intent(this.mAction);
        intent.setComponent(componentName);
        this.mBoundComponent = componentName;
        this.mBoundPackageName = componentName.getPackageName();
        this.mBoundVersion = i;
        this.mBoundUserId = i2;
        this.mContext.bindServiceAsUser(intent, this, 1073741829, new UserHandle(i2));
    }

    public static boolean isSignatureMatch(Signature[] signatureArr, List<HashSet<Signature>> list) {
        if (signatureArr == null) {
            return false;
        }
        HashSet hashSet = new HashSet();
        for (Signature signature : signatureArr) {
            hashSet.add(signature);
        }
        Iterator<HashSet<Signature>> it = list.iterator();
        while (it.hasNext()) {
            if (it.next().equals(hashSet)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSignatureMatch(Signature[] signatureArr) {
        return isSignatureMatch(signatureArr, this.mSignatureSets);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        synchronized (this.mLock) {
            if (componentName.equals(this.mBoundComponent)) {
                this.mBoundService = iBinder;
                if (this.mHandler != null && this.mNewServiceWork != null) {
                    this.mHandler.post(this.mNewServiceWork);
                }
            } else {
                Log.w(this.mTag, "unexpected onServiceConnected: " + componentName);
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        synchronized (this.mLock) {
            if (componentName.equals(this.mBoundComponent)) {
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

    public boolean runOnBinder(BinderRunner binderRunner) {
        synchronized (this.mLock) {
            if (this.mBoundService == null) {
                return false;
            }
            binderRunner.run(this.mBoundService);
            return true;
        }
    }

    public void switchUser(int i) {
        synchronized (this.mLock) {
            this.mCurrentUserId = i;
            bindBestPackageLocked(this.mServicePackageName, false);
        }
    }

    public void unlockUser(int i) {
        synchronized (this.mLock) {
            if (i == this.mCurrentUserId) {
                bindBestPackageLocked(this.mServicePackageName, false);
            }
        }
    }
}
