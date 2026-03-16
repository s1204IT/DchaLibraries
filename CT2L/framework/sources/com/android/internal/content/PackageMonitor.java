package com.android.internal.content;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import java.util.HashSet;

public abstract class PackageMonitor extends BroadcastReceiver {
    public static final int PACKAGE_PERMANENT_CHANGE = 3;
    public static final int PACKAGE_TEMPORARY_CHANGE = 2;
    public static final int PACKAGE_UNCHANGED = 0;
    public static final int PACKAGE_UPDATING = 1;
    String[] mAppearingPackages;
    int mChangeType;
    String[] mDisappearingPackages;
    String[] mModifiedPackages;
    Context mRegisteredContext;
    Handler mRegisteredHandler;
    boolean mSomePackagesChanged;
    static final IntentFilter sPackageFilt = new IntentFilter();
    static final IntentFilter sNonDataFilt = new IntentFilter();
    static final IntentFilter sExternalFilt = new IntentFilter();
    final HashSet<String> mUpdatingPackages = new HashSet<>();
    int mChangeUserId = -10000;
    String[] mTempArray = new String[1];

    static {
        sPackageFilt.addAction(Intent.ACTION_PACKAGE_ADDED);
        sPackageFilt.addAction(Intent.ACTION_PACKAGE_REMOVED);
        sPackageFilt.addAction(Intent.ACTION_PACKAGE_CHANGED);
        sPackageFilt.addAction(Intent.ACTION_QUERY_PACKAGE_RESTART);
        sPackageFilt.addAction(Intent.ACTION_PACKAGE_RESTARTED);
        sPackageFilt.addAction(Intent.ACTION_UID_REMOVED);
        sPackageFilt.addDataScheme("package");
        sNonDataFilt.addAction(Intent.ACTION_UID_REMOVED);
        sNonDataFilt.addAction(Intent.ACTION_USER_STOPPED);
        sExternalFilt.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        sExternalFilt.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
    }

    public void register(Context context, Looper thread, boolean externalStorage) {
        register(context, thread, null, externalStorage);
    }

    public void register(Context context, Looper thread, UserHandle user, boolean externalStorage) {
        if (this.mRegisteredContext != null) {
            throw new IllegalStateException("Already registered");
        }
        this.mRegisteredContext = context;
        if (thread == null) {
            this.mRegisteredHandler = BackgroundThread.getHandler();
        } else {
            this.mRegisteredHandler = new Handler(thread);
        }
        if (user != null) {
            context.registerReceiverAsUser(this, user, sPackageFilt, null, this.mRegisteredHandler);
            context.registerReceiverAsUser(this, user, sNonDataFilt, null, this.mRegisteredHandler);
            if (externalStorage) {
                context.registerReceiverAsUser(this, user, sExternalFilt, null, this.mRegisteredHandler);
                return;
            }
            return;
        }
        context.registerReceiver(this, sPackageFilt, null, this.mRegisteredHandler);
        context.registerReceiver(this, sNonDataFilt, null, this.mRegisteredHandler);
        if (externalStorage) {
            context.registerReceiver(this, sExternalFilt, null, this.mRegisteredHandler);
        }
    }

    public Handler getRegisteredHandler() {
        return this.mRegisteredHandler;
    }

    public void unregister() {
        if (this.mRegisteredContext == null) {
            throw new IllegalStateException("Not registered");
        }
        this.mRegisteredContext.unregisterReceiver(this);
        this.mRegisteredContext = null;
    }

    boolean isPackageUpdating(String packageName) {
        boolean zContains;
        synchronized (this.mUpdatingPackages) {
            zContains = this.mUpdatingPackages.contains(packageName);
        }
        return zContains;
    }

    public void onBeginPackageChanges() {
    }

    public void onPackageAdded(String packageName, int uid) {
    }

    public void onPackageRemoved(String packageName, int uid) {
    }

    public void onPackageRemovedAllUsers(String packageName, int uid) {
    }

    public void onPackageUpdateStarted(String packageName, int uid) {
    }

    public void onPackageUpdateFinished(String packageName, int uid) {
    }

    public boolean onPackageChanged(String packageName, int uid, String[] components) {
        if (components != null) {
            for (String name : components) {
                if (packageName.equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean onHandleForceStop(Intent intent, String[] packages, int uid, boolean doit) {
        return false;
    }

    public void onHandleUserStop(Intent intent, int userHandle) {
    }

    public void onUidRemoved(int uid) {
    }

    public void onPackagesAvailable(String[] packages) {
    }

    public void onPackagesUnavailable(String[] packages) {
    }

    public void onPackageDisappeared(String packageName, int reason) {
    }

    public void onPackageAppeared(String packageName, int reason) {
    }

    public void onPackageModified(String packageName) {
    }

    public boolean didSomePackagesChange() {
        return this.mSomePackagesChanged;
    }

    public int isPackageAppearing(String packageName) {
        if (this.mAppearingPackages != null) {
            for (int i = this.mAppearingPackages.length - 1; i >= 0; i--) {
                if (packageName.equals(this.mAppearingPackages[i])) {
                    return this.mChangeType;
                }
            }
        }
        return 0;
    }

    public boolean anyPackagesAppearing() {
        return this.mAppearingPackages != null;
    }

    public int isPackageDisappearing(String packageName) {
        if (this.mDisappearingPackages != null) {
            for (int i = this.mDisappearingPackages.length - 1; i >= 0; i--) {
                if (packageName.equals(this.mDisappearingPackages[i])) {
                    return this.mChangeType;
                }
            }
        }
        return 0;
    }

    public boolean anyPackagesDisappearing() {
        return this.mDisappearingPackages != null;
    }

    public boolean isReplacing() {
        return this.mChangeType == 1;
    }

    public boolean isPackageModified(String packageName) {
        if (this.mModifiedPackages != null) {
            for (int i = this.mModifiedPackages.length - 1; i >= 0; i--) {
                if (packageName.equals(this.mModifiedPackages[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    public void onSomePackagesChanged() {
    }

    public void onFinishPackageChanges() {
    }

    public int getChangingUserId() {
        return this.mChangeUserId;
    }

    String getPackageName(Intent intent) {
        Uri uri = intent.getData();
        if (uri == null) {
            return null;
        }
        String pkg = uri.getSchemeSpecificPart();
        return pkg;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        this.mChangeUserId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, -10000);
        if (this.mChangeUserId == -10000) {
            Slog.w("PackageMonitor", "Intent broadcast does not contain user handle: " + intent);
            return;
        }
        onBeginPackageChanges();
        this.mAppearingPackages = null;
        this.mDisappearingPackages = null;
        this.mSomePackagesChanged = false;
        String action = intent.getAction();
        if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
            String pkg = getPackageName(intent);
            int uid = intent.getIntExtra(Intent.EXTRA_UID, 0);
            this.mSomePackagesChanged = true;
            if (pkg != null) {
                this.mAppearingPackages = this.mTempArray;
                this.mTempArray[0] = pkg;
                if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                    this.mModifiedPackages = this.mTempArray;
                    this.mChangeType = 1;
                    onPackageUpdateFinished(pkg, uid);
                    onPackageModified(pkg);
                } else {
                    this.mChangeType = 3;
                    onPackageAdded(pkg, uid);
                }
                onPackageAppeared(pkg, this.mChangeType);
                if (this.mChangeType == 1) {
                    synchronized (this.mUpdatingPackages) {
                        this.mUpdatingPackages.remove(pkg);
                    }
                }
            }
        } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
            String pkg2 = getPackageName(intent);
            int uid2 = intent.getIntExtra(Intent.EXTRA_UID, 0);
            if (pkg2 != null) {
                this.mDisappearingPackages = this.mTempArray;
                this.mTempArray[0] = pkg2;
                if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                    this.mChangeType = 1;
                    synchronized (this.mUpdatingPackages) {
                    }
                    onPackageUpdateStarted(pkg2, uid2);
                } else {
                    this.mChangeType = 3;
                    this.mSomePackagesChanged = true;
                    onPackageRemoved(pkg2, uid2);
                    if (intent.getBooleanExtra(Intent.EXTRA_REMOVED_FOR_ALL_USERS, false)) {
                        onPackageRemovedAllUsers(pkg2, uid2);
                    }
                }
                onPackageDisappeared(pkg2, this.mChangeType);
            }
        } else if (Intent.ACTION_PACKAGE_CHANGED.equals(action)) {
            String pkg3 = getPackageName(intent);
            int uid3 = intent.getIntExtra(Intent.EXTRA_UID, 0);
            String[] components = intent.getStringArrayExtra(Intent.EXTRA_CHANGED_COMPONENT_NAME_LIST);
            if (pkg3 != null) {
                this.mModifiedPackages = this.mTempArray;
                this.mTempArray[0] = pkg3;
                this.mChangeType = 3;
                if (onPackageChanged(pkg3, uid3, components)) {
                    this.mSomePackagesChanged = true;
                }
                onPackageModified(pkg3);
            }
        } else if (Intent.ACTION_QUERY_PACKAGE_RESTART.equals(action)) {
            this.mDisappearingPackages = intent.getStringArrayExtra(Intent.EXTRA_PACKAGES);
            this.mChangeType = 2;
            boolean canRestart = onHandleForceStop(intent, this.mDisappearingPackages, intent.getIntExtra(Intent.EXTRA_UID, 0), false);
            if (canRestart) {
                setResultCode(-1);
            }
        } else if (Intent.ACTION_PACKAGE_RESTARTED.equals(action)) {
            this.mDisappearingPackages = new String[]{getPackageName(intent)};
            this.mChangeType = 2;
            onHandleForceStop(intent, this.mDisappearingPackages, intent.getIntExtra(Intent.EXTRA_UID, 0), true);
        } else if (Intent.ACTION_UID_REMOVED.equals(action)) {
            onUidRemoved(intent.getIntExtra(Intent.EXTRA_UID, 0));
        } else if (Intent.ACTION_USER_STOPPED.equals(action)) {
            if (intent.hasExtra(Intent.EXTRA_USER_HANDLE)) {
                onHandleUserStop(intent, intent.getIntExtra(Intent.EXTRA_USER_HANDLE, 0));
            }
        } else if (Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE.equals(action)) {
            String[] pkgList = intent.getStringArrayExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST);
            this.mAppearingPackages = pkgList;
            this.mChangeType = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false) ? 1 : 2;
            this.mSomePackagesChanged = true;
            if (pkgList != null) {
                onPackagesAvailable(pkgList);
                for (String str : pkgList) {
                    onPackageAppeared(str, this.mChangeType);
                }
            }
        } else if (Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE.equals(action)) {
            String[] pkgList2 = intent.getStringArrayExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST);
            this.mDisappearingPackages = pkgList2;
            this.mChangeType = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false) ? 1 : 2;
            this.mSomePackagesChanged = true;
            if (pkgList2 != null) {
                onPackagesUnavailable(pkgList2);
                for (String str2 : pkgList2) {
                    onPackageDisappeared(str2, this.mChangeType);
                }
            }
        }
        if (this.mSomePackagesChanged) {
            onSomePackagesChanged();
        }
        onFinishPackageChanges();
        this.mChangeUserId = -10000;
    }
}
