package com.android.server.appwidget;

import android.R;
import android.app.AlarmManager;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManagerInternal;
import android.appwidget.AppWidgetManagerInternal;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.AttributeSet;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.util.Xml;
import android.view.Display;
import android.view.WindowManager;
import android.widget.RemoteViews;
import com.android.internal.app.UnlaunchableAppActivity;
import com.android.internal.appwidget.IAppWidgetHost;
import com.android.internal.appwidget.IAppWidgetService;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.widget.IRemoteViewsAdapterConnection;
import com.android.internal.widget.IRemoteViewsFactory;
import com.android.server.LocalServices;
import com.android.server.WidgetBackupProvider;
import com.android.server.am.ProcessList;
import com.android.server.pm.PackageManagerService;
import com.android.server.policy.IconUtilities;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class AppWidgetServiceImpl extends IAppWidgetService.Stub implements WidgetBackupProvider, DevicePolicyManagerInternal.OnCrossProfileWidgetProvidersChangeListener {
    private static final int CURRENT_VERSION = 1;
    private static boolean DEBUG = true;
    private static final int KEYGUARD_HOST_ID = 1262836039;
    private static final int LOADED_PROFILE_ID = -1;
    private static final int MIN_UPDATE_PERIOD;
    private static final String NEW_KEYGUARD_HOST_PACKAGE = "com.android.keyguard";
    private static final String OLD_KEYGUARD_HOST_PACKAGE = "android";
    private static final String STATE_FILENAME = "appwidgets.xml";
    private static final String TAG = "AppWidgetServiceImpl";
    private static final int TAG_UNDEFINED = -1;
    private static final int UNKNOWN_UID = -1;
    private static final int UNKNOWN_USER_ID = -10;
    private final AlarmManager mAlarmManager;
    private final AppOpsManager mAppOpsManager;
    private final Handler mCallbackHandler;
    private final Context mContext;
    private final IconUtilities mIconUtilities;
    private final KeyguardManager mKeyguardManager;
    private Locale mLocale;
    private int mMaxWidgetBitmapMemory;
    private boolean mSafeMode;
    private final UserManager mUserManager;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) throws Throwable {
            String action = intent.getAction();
            int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
            if (AppWidgetServiceImpl.DEBUG) {
                Slog.i(AppWidgetServiceImpl.TAG, "Received broadcast: " + action + " on user " + userId);
            }
            if ("android.intent.action.CONFIGURATION_CHANGED".equals(action)) {
                AppWidgetServiceImpl.this.onConfigurationChanged();
                return;
            }
            if ("android.intent.action.MANAGED_PROFILE_AVAILABLE".equals(action) || "android.intent.action.MANAGED_PROFILE_UNAVAILABLE".equals(action)) {
                synchronized (AppWidgetServiceImpl.this.mLock) {
                    AppWidgetServiceImpl.this.reloadWidgetsMaskedState(userId);
                }
            } else if ("android.intent.action.PACKAGES_SUSPENDED".equals(action)) {
                String[] packages = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                AppWidgetServiceImpl.this.updateWidgetPackageSuspensionMaskedState(packages, true, getSendingUserId());
            } else if ("android.intent.action.PACKAGES_UNSUSPENDED".equals(action)) {
                String[] packages2 = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                AppWidgetServiceImpl.this.updateWidgetPackageSuspensionMaskedState(packages2, false, getSendingUserId());
            } else {
                AppWidgetServiceImpl.this.onPackageBroadcastReceived(intent, userId);
            }
        }
    };
    private final HashMap<Pair<Integer, Intent.FilterComparison>, ServiceConnection> mBoundRemoteViewsServices = new HashMap<>();
    private final HashMap<Pair<Integer, Intent.FilterComparison>, HashSet<Integer>> mRemoteViewsServicesAppWidgets = new HashMap<>();
    private final Object mLock = new Object();
    private final ArrayList<Widget> mWidgets = new ArrayList<>();
    private final ArrayList<Host> mHosts = new ArrayList<>();
    private final ArrayList<Provider> mProviders = new ArrayList<>();
    private final ArraySet<Pair<Integer, String>> mPackagesWithBindWidgetPermission = new ArraySet<>();
    private final SparseIntArray mLoadedUserIds = new SparseIntArray();
    private final SparseArray<ArraySet<String>> mWidgetPackages = new SparseArray<>();
    private final SparseIntArray mNextAppWidgetIds = new SparseIntArray();
    private final IPackageManager mPackageManager = AppGlobals.getPackageManager();
    private final DevicePolicyManagerInternal mDevicePolicyManagerInternal = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
    private final Handler mSaveStateHandler = BackgroundThread.getHandler();
    private final BackupRestoreController mBackupRestoreController = new BackupRestoreController(this, null);
    private final SecurityPolicy mSecurityPolicy = new SecurityPolicy(this, 0 == true ? 1 : 0);

    static {
        MIN_UPDATE_PERIOD = DEBUG ? 0 : ProcessList.PSS_MAX_INTERVAL;
    }

    AppWidgetServiceImpl(Context context) {
        this.mContext = context;
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mAppOpsManager = (AppOpsManager) this.mContext.getSystemService("appops");
        this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        this.mCallbackHandler = new CallbackHandler(this.mContext.getMainLooper());
        this.mIconUtilities = new IconUtilities(context);
        computeMaximumWidgetBitmapMemory();
        registerBroadcastReceiver();
        registerOnCrossProfileProvidersChangedListener();
        LocalServices.addService(AppWidgetManagerInternal.class, new AppWidgetManagerLocal(this, 0 == true ? 1 : 0));
    }

    private void computeMaximumWidgetBitmapMemory() {
        WindowManager wm = (WindowManager) this.mContext.getSystemService("window");
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getRealSize(size);
        this.mMaxWidgetBitmapMemory = size.x * 6 * size.y;
    }

    private void registerBroadcastReceiver() {
        IntentFilter configFilter = new IntentFilter();
        configFilter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, configFilter, null, null);
        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction("android.intent.action.PACKAGE_ADDED");
        packageFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        packageFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        packageFilter.addDataScheme("package");
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, packageFilter, null, null);
        IntentFilter sdFilter = new IntentFilter();
        sdFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
        sdFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, sdFilter, null, null);
        IntentFilter offModeFilter = new IntentFilter();
        offModeFilter.addAction("android.intent.action.MANAGED_PROFILE_AVAILABLE");
        offModeFilter.addAction("android.intent.action.MANAGED_PROFILE_UNAVAILABLE");
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, offModeFilter, null, null);
        IntentFilter suspendPackageFilter = new IntentFilter();
        suspendPackageFilter.addAction("android.intent.action.PACKAGES_SUSPENDED");
        suspendPackageFilter.addAction("android.intent.action.PACKAGES_UNSUSPENDED");
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, suspendPackageFilter, null, null);
    }

    private void registerOnCrossProfileProvidersChangedListener() {
        if (this.mDevicePolicyManagerInternal == null) {
            return;
        }
        this.mDevicePolicyManagerInternal.addOnCrossProfileWidgetProvidersChangeListener(this);
    }

    public void setSafeMode(boolean safeMode) {
        this.mSafeMode = safeMode;
    }

    private void onConfigurationChanged() throws Throwable {
        SparseIntArray changedGroups;
        if (DEBUG) {
            Slog.i(TAG, "onConfigurationChanged()");
        }
        Locale revised = Locale.getDefault();
        if (revised != null && this.mLocale != null && revised.equals(this.mLocale)) {
            return;
        }
        this.mLocale = revised;
        synchronized (this.mLock) {
            try {
                ArrayList<Provider> installedProviders = new ArrayList<>(this.mProviders);
                HashSet<ProviderId> removedProviders = new HashSet<>();
                int N = installedProviders.size();
                int i = N - 1;
                SparseIntArray changedGroups2 = null;
                while (i >= 0) {
                    try {
                        Provider provider = installedProviders.get(i);
                        int userId = provider.getUserId();
                        if (!this.mUserManager.isUserUnlockingOrUnlocked(userId) || isProfileWithLockedParent(userId)) {
                            changedGroups = changedGroups2;
                        } else {
                            ensureGroupStateLoadedLocked(userId);
                            if (!removedProviders.contains(provider.id)) {
                                boolean changed = updateProvidersForPackageLocked(provider.id.componentName.getPackageName(), provider.getUserId(), removedProviders);
                                if (changed) {
                                    changedGroups = changedGroups2 == null ? new SparseIntArray() : changedGroups2;
                                    int groupId = this.mSecurityPolicy.getGroupParent(provider.getUserId());
                                    changedGroups.put(groupId, groupId);
                                } else {
                                    changedGroups = changedGroups2;
                                }
                            }
                        }
                        i--;
                        changedGroups2 = changedGroups;
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                }
                if (changedGroups2 != null) {
                    int groupCount = changedGroups2.size();
                    for (int i2 = 0; i2 < groupCount; i2++) {
                        saveGroupStateAsync(changedGroups2.get(i2));
                    }
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    private void onPackageBroadcastReceived(Intent intent, int userId) {
        String pkgName;
        String[] pkgList;
        boolean added;
        int uid;
        if (!this.mUserManager.isUserUnlockingOrUnlocked(userId) || isProfileWithLockedParent(userId)) {
            return;
        }
        String action = intent.getAction();
        boolean changed = false;
        boolean componentsModified = false;
        if ("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE".equals(action)) {
            pkgList = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
            added = true;
        } else if ("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE".equals(action)) {
            pkgList = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
            added = false;
        } else {
            Uri uri = intent.getData();
            if (uri == null || (pkgName = uri.getSchemeSpecificPart()) == null) {
                return;
            }
            pkgList = new String[]{pkgName};
            added = "android.intent.action.PACKAGE_ADDED".equals(action);
            changed = "android.intent.action.PACKAGE_CHANGED".equals(action);
        }
        if (pkgList == null || pkgList.length == 0) {
            return;
        }
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            Bundle extras = intent.getExtras();
            if (added || changed) {
                boolean newPackageAdded = added ? extras == null || !extras.getBoolean("android.intent.extra.REPLACING", false) : false;
                for (String pkgName2 : pkgList) {
                    componentsModified |= updateProvidersForPackageLocked(pkgName2, userId, null);
                    if (newPackageAdded && userId == 0 && (uid = getUidForPackage(pkgName2, userId)) >= 0) {
                        resolveHostUidLocked(pkgName2, uid);
                    }
                }
            } else {
                boolean packageRemovedPermanently = extras == null || !extras.getBoolean("android.intent.extra.REPLACING", false);
                if (packageRemovedPermanently) {
                    for (String str : pkgList) {
                        componentsModified |= removeHostsAndProvidersForPackageLocked(str, userId);
                    }
                }
            }
            if (componentsModified) {
                saveGroupStateAsync(userId);
                scheduleNotifyGroupHostsForProvidersChangedLocked(userId);
            }
        }
    }

    void reloadWidgetsMaskedStateForGroup(int userId) {
        if (!this.mUserManager.isUserUnlockingOrUnlocked(userId)) {
            return;
        }
        synchronized (this.mLock) {
            reloadWidgetsMaskedState(userId);
            int[] profileIds = this.mUserManager.getEnabledProfileIds(userId);
            for (int profileId : profileIds) {
                reloadWidgetsMaskedState(profileId);
            }
        }
    }

    private void reloadWidgetsMaskedState(int userId) {
        boolean zIsPackageSuspendedForUser;
        long identity = Binder.clearCallingIdentity();
        try {
            UserInfo user = this.mUserManager.getUserInfo(userId);
            boolean lockedProfile = !this.mUserManager.isUserUnlockingOrUnlocked(userId);
            boolean quietProfile = user.isQuietModeEnabled();
            int N = this.mProviders.size();
            for (int i = 0; i < N; i++) {
                Provider provider = this.mProviders.get(i);
                int providerUserId = provider.getUserId();
                if (providerUserId == userId) {
                    boolean changed = provider.setMaskedByLockedProfileLocked(lockedProfile) | provider.setMaskedByQuietProfileLocked(quietProfile);
                    try {
                        try {
                            zIsPackageSuspendedForUser = this.mPackageManager.isPackageSuspendedForUser(provider.info.provider.getPackageName(), provider.getUserId());
                        } catch (RemoteException e) {
                            Slog.e(TAG, "Failed to query application info", e);
                        }
                    } catch (IllegalArgumentException e2) {
                        zIsPackageSuspendedForUser = false;
                    }
                    changed |= provider.setMaskedBySuspendedPackageLocked(zIsPackageSuspendedForUser);
                    if (changed) {
                        if (provider.isMaskedLocked()) {
                            maskWidgetsViewsLocked(provider, null);
                        } else {
                            unmaskWidgetsViewsLocked(provider);
                        }
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void updateWidgetPackageSuspensionMaskedState(String[] packagesArray, boolean suspended, int profileId) {
        if (packagesArray == null) {
            return;
        }
        Set<String> packages = new ArraySet<>(Arrays.asList(packagesArray));
        synchronized (this.mLock) {
            int N = this.mProviders.size();
            for (int i = 0; i < N; i++) {
                Provider provider = this.mProviders.get(i);
                int providerUserId = provider.getUserId();
                if (providerUserId == profileId && packages.contains(provider.info.provider.getPackageName()) && provider.setMaskedBySuspendedPackageLocked(suspended)) {
                    if (provider.isMaskedLocked()) {
                        maskWidgetsViewsLocked(provider, null);
                    } else {
                        unmaskWidgetsViewsLocked(provider);
                    }
                }
            }
        }
    }

    private Bitmap createMaskedWidgetBitmap(String providerPackage, int providerUserId) {
        long identity = Binder.clearCallingIdentity();
        try {
            Context userContext = this.mContext.createPackageContextAsUser(providerPackage, 0, UserHandle.of(providerUserId));
            PackageManager pm = userContext.getPackageManager();
            Drawable icon = pm.getApplicationInfo(providerPackage, 0).loadUnbadgedIcon(pm);
            return this.mIconUtilities.createIconBitmap(icon);
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "Fail to get application icon", e);
            return null;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private RemoteViews createMaskedWidgetRemoteViews(Bitmap icon, boolean showBadge, PendingIntent onClickIntent) {
        RemoteViews views = new RemoteViews(this.mContext.getPackageName(), R.layout.preference_child_material);
        if (icon != null) {
            views.setImageViewBitmap(R.id.option3, icon);
        }
        if (!showBadge) {
            views.setViewVisibility(R.id.orientation, 4);
        }
        if (onClickIntent != null) {
            views.setOnClickPendingIntent(R.id.option2, onClickIntent);
        }
        return views;
    }

    private void maskWidgetsViewsLocked(Provider provider, Widget targetWidget) {
        String providerPackage;
        int providerUserId;
        Bitmap iconBitmap;
        boolean showBadge;
        Intent onClickIntent;
        int widgetCount = provider.widgets.size();
        if (widgetCount == 0 || (iconBitmap = createMaskedWidgetBitmap((providerPackage = provider.info.provider.getPackageName()), (providerUserId = provider.getUserId()))) == null) {
            return;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            if (provider.maskedBySuspendedPackage) {
                UserInfo userInfo = this.mUserManager.getUserInfo(providerUserId);
                showBadge = userInfo.isManagedProfile();
                onClickIntent = this.mDevicePolicyManagerInternal.createPackageSuspendedDialogIntent(providerPackage, providerUserId);
            } else if (provider.maskedByQuietProfile) {
                showBadge = true;
                onClickIntent = UnlaunchableAppActivity.createInQuietModeDialogIntent(providerUserId);
            } else {
                showBadge = true;
                onClickIntent = this.mKeyguardManager.createConfirmDeviceCredentialIntent(null, null, providerUserId);
                if (onClickIntent != null) {
                    onClickIntent.setFlags(276824064);
                }
            }
            for (int j = 0; j < widgetCount; j++) {
                Widget widget = provider.widgets.get(j);
                if (targetWidget == null || targetWidget == widget) {
                    PendingIntent intent = null;
                    if (onClickIntent != null) {
                        intent = PendingIntent.getActivity(this.mContext, widget.appWidgetId, onClickIntent, 134217728);
                    }
                    RemoteViews views = createMaskedWidgetRemoteViews(iconBitmap, showBadge, intent);
                    if (widget.replaceWithMaskedViewsLocked(views)) {
                        scheduleNotifyUpdateAppWidgetLocked(widget, widget.getEffectiveViewsLocked());
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void unmaskWidgetsViewsLocked(Provider provider) {
        int widgetCount = provider.widgets.size();
        for (int j = 0; j < widgetCount; j++) {
            Widget widget = provider.widgets.get(j);
            if (widget.clearMaskedViewsLocked()) {
                scheduleNotifyUpdateAppWidgetLocked(widget, widget.getEffectiveViewsLocked());
            }
        }
    }

    private void resolveHostUidLocked(String pkg, int uid) {
        int N = this.mHosts.size();
        for (int i = 0; i < N; i++) {
            Host host = this.mHosts.get(i);
            if (host.id.uid == -1 && pkg.equals(host.id.packageName)) {
                if (DEBUG) {
                    Slog.i(TAG, "host " + host.id + " resolved to uid " + uid);
                }
                host.id = new HostId(uid, host.id.hostId, host.id.packageName);
                return;
            }
        }
    }

    private void ensureGroupStateLoadedLocked(int userId) {
        ensureGroupStateLoadedLocked(userId, true);
    }

    private void ensureGroupStateLoadedLocked(int userId, boolean enforceUserUnlockingOrUnlocked) {
        if (enforceUserUnlockingOrUnlocked && !isUserRunningAndUnlocked(userId)) {
            throw new IllegalStateException("User " + userId + " must be unlocked for widgets to be available");
        }
        if (enforceUserUnlockingOrUnlocked && isProfileWithLockedParent(userId)) {
            throw new IllegalStateException("Profile " + userId + " must have unlocked parent");
        }
        int[] profileIds = this.mSecurityPolicy.getEnabledGroupProfileIds(userId);
        int newMemberCount = 0;
        int profileIdCount = profileIds.length;
        for (int i = 0; i < profileIdCount; i++) {
            if (this.mLoadedUserIds.indexOfKey(profileIds[i]) >= 0) {
                profileIds[i] = -1;
            } else {
                newMemberCount++;
            }
        }
        if (newMemberCount <= 0) {
            return;
        }
        int newMemberIndex = 0;
        int[] newProfileIds = new int[newMemberCount];
        for (int profileId : profileIds) {
            if (profileId != -1) {
                this.mLoadedUserIds.put(profileId, profileId);
                newProfileIds[newMemberIndex] = profileId;
                newMemberIndex++;
            }
        }
        clearProvidersAndHostsTagsLocked();
        loadGroupWidgetProvidersLocked(newProfileIds);
        loadGroupStateLocked(newProfileIds);
    }

    private boolean isUserRunningAndUnlocked(int userId) {
        if (this.mUserManager.isUserRunning(userId)) {
            return StorageManager.isUserKeyUnlocked(userId);
        }
        return false;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", "Permission Denial: can't dump from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
        synchronized (this.mLock) {
            int N = this.mProviders.size();
            pw.println("Providers:");
            for (int i = 0; i < N; i++) {
                dumpProvider(this.mProviders.get(i), i, pw);
            }
            int N2 = this.mWidgets.size();
            pw.println(" ");
            pw.println("Widgets:");
            for (int i2 = 0; i2 < N2; i2++) {
                dumpWidget(this.mWidgets.get(i2), i2, pw);
            }
            int N3 = this.mHosts.size();
            pw.println(" ");
            pw.println("Hosts:");
            for (int i3 = 0; i3 < N3; i3++) {
                dumpHost(this.mHosts.get(i3), i3, pw);
            }
            int N4 = this.mPackagesWithBindWidgetPermission.size();
            pw.println(" ");
            pw.println("Grants:");
            for (int i4 = 0; i4 < N4; i4++) {
                Pair<Integer, String> grant = this.mPackagesWithBindWidgetPermission.valueAt(i4);
                dumpGrant(grant, i4, pw);
            }
        }
    }

    public ParceledListSlice<RemoteViews> startListening(IAppWidgetHost callbacks, String callingPackage, int hostId, int[] appWidgetIds, int[] updatedIds) {
        ParceledListSlice<RemoteViews> parceledListSlice;
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "startListening() " + userId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            HostId id = new HostId(Binder.getCallingUid(), hostId, callingPackage);
            Host host = lookupOrAddHostLocked(id);
            host.callbacks = callbacks;
            int N = appWidgetIds.length;
            ArrayList<RemoteViews> outViews = new ArrayList<>(N);
            int added = 0;
            for (int i = 0; i < N; i++) {
                RemoteViews rv = host.getPendingViewsForId(appWidgetIds[i]);
                if (rv != null) {
                    updatedIds[added] = appWidgetIds[i];
                    outViews.add(rv);
                    added++;
                }
            }
            parceledListSlice = new ParceledListSlice<>(outViews);
        }
        return parceledListSlice;
    }

    public void stopListening(String callingPackage, int hostId) {
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "stopListening() " + userId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            HostId id = new HostId(Binder.getCallingUid(), hostId, callingPackage);
            Host host = lookupHostLocked(id);
            if (host != null) {
                host.callbacks = null;
                pruneHostLocked(host);
            }
        }
    }

    public int allocateAppWidgetId(String callingPackage, int hostId) {
        int appWidgetId;
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "allocateAppWidgetId() " + userId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            if (this.mNextAppWidgetIds.indexOfKey(userId) < 0) {
                this.mNextAppWidgetIds.put(userId, 1);
            }
            appWidgetId = incrementAndGetAppWidgetIdLocked(userId);
            HostId id = new HostId(Binder.getCallingUid(), hostId, callingPackage);
            Host host = lookupOrAddHostLocked(id);
            Widget widget = new Widget(null);
            widget.appWidgetId = appWidgetId;
            widget.host = host;
            host.widgets.add(widget);
            addWidgetLocked(widget);
            saveGroupStateAsync(userId);
            if (DEBUG) {
                Slog.i(TAG, "Allocated widget id " + appWidgetId + " for host " + host.id);
            }
        }
        return appWidgetId;
    }

    public void deleteAppWidgetId(String callingPackage, int appWidgetId) {
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "deleteAppWidgetId() " + userId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            Widget widget = lookupWidgetLocked(appWidgetId, Binder.getCallingUid(), callingPackage);
            if (widget == null) {
                return;
            }
            deleteAppWidgetLocked(widget);
            saveGroupStateAsync(userId);
            if (DEBUG) {
                Slog.i(TAG, "Deleted widget id " + appWidgetId + " for host " + widget.host.id);
            }
        }
    }

    public boolean hasBindAppWidgetPermission(String packageName, int grantId) {
        if (DEBUG) {
            Slog.i(TAG, "hasBindAppWidgetPermission() " + UserHandle.getCallingUserId());
        }
        this.mSecurityPolicy.enforceModifyAppWidgetBindPermissions(packageName);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(grantId);
            int packageUid = getUidForPackage(packageName, grantId);
            if (packageUid < 0) {
                return false;
            }
            Pair<Integer, String> packageId = Pair.create(Integer.valueOf(grantId), packageName);
            return this.mPackagesWithBindWidgetPermission.contains(packageId);
        }
    }

    public void setBindAppWidgetPermission(String packageName, int grantId, boolean grantPermission) {
        if (DEBUG) {
            Slog.i(TAG, "setBindAppWidgetPermission() " + UserHandle.getCallingUserId());
        }
        this.mSecurityPolicy.enforceModifyAppWidgetBindPermissions(packageName);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(grantId);
            int packageUid = getUidForPackage(packageName, grantId);
            if (packageUid < 0) {
                return;
            }
            Pair<Integer, String> packageId = Pair.create(Integer.valueOf(grantId), packageName);
            if (grantPermission) {
                this.mPackagesWithBindWidgetPermission.add(packageId);
            } else {
                this.mPackagesWithBindWidgetPermission.remove(packageId);
            }
            saveGroupStateAsync(grantId);
        }
    }

    public IntentSender createAppWidgetConfigIntentSender(String callingPackage, int appWidgetId, int intentFlags) {
        IntentSender intentSender;
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "createAppWidgetConfigIntentSender() " + userId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            Widget widget = lookupWidgetLocked(appWidgetId, Binder.getCallingUid(), callingPackage);
            if (widget == null) {
                throw new IllegalArgumentException("Bad widget id " + appWidgetId);
            }
            Provider provider = widget.provider;
            if (provider == null) {
                throw new IllegalArgumentException("Widget not bound " + appWidgetId);
            }
            int secureFlags = intentFlags & (-196);
            Intent intent = new Intent("android.appwidget.action.APPWIDGET_CONFIGURE");
            intent.putExtra("appWidgetId", appWidgetId);
            intent.setComponent(provider.info.configure);
            intent.setFlags(secureFlags);
            long identity = Binder.clearCallingIdentity();
            try {
                intentSender = PendingIntent.getActivityAsUser(this.mContext, 0, intent, 1409286144, null, new UserHandle(provider.getUserId())).getIntentSender();
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
        return intentSender;
    }

    public boolean bindAppWidgetId(String callingPackage, int appWidgetId, int providerProfileId, ComponentName providerComponent, Bundle options) {
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "bindAppWidgetId() " + userId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        if (!this.mSecurityPolicy.isEnabledGroupProfile(providerProfileId) || !this.mSecurityPolicy.isProviderInCallerOrInProfileAndWhitelListed(providerComponent.getPackageName(), providerProfileId)) {
            return false;
        }
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            if (!this.mSecurityPolicy.hasCallerBindPermissionOrBindWhiteListedLocked(callingPackage)) {
                return false;
            }
            Widget widget = lookupWidgetLocked(appWidgetId, Binder.getCallingUid(), callingPackage);
            if (widget == null) {
                Slog.e(TAG, "Bad widget id " + appWidgetId);
                return false;
            }
            if (widget.provider != null) {
                Slog.e(TAG, "Widget id " + appWidgetId + " already bound to: " + widget.provider.id);
                return false;
            }
            int providerUid = getUidForPackage(providerComponent.getPackageName(), providerProfileId);
            if (providerUid < 0) {
                Slog.e(TAG, "Package " + providerComponent.getPackageName() + " not installed  for profile " + providerProfileId);
                return false;
            }
            ProviderId providerId = new ProviderId(providerUid, providerComponent, null);
            Provider provider = lookupProviderLocked(providerId);
            if (provider == null) {
                Slog.e(TAG, "No widget provider " + providerComponent + " for profile " + providerProfileId);
                return false;
            }
            if (provider.zombie) {
                Slog.e(TAG, "Can't bind to a 3rd party provider in safe mode " + provider);
                return false;
            }
            widget.provider = provider;
            widget.options = options != null ? cloneIfLocalBinder(options) : new Bundle();
            widget.options.setDefusable(true);
            if (!widget.options.containsKey("appWidgetCategory")) {
                widget.options.putInt("appWidgetCategory", 1);
            }
            provider.widgets.add(widget);
            onWidgetProviderAddedOrChangedLocked(widget);
            int widgetCount = provider.widgets.size();
            if (widgetCount == 1) {
                sendEnableIntentLocked(provider);
            }
            sendUpdateIntentLocked(provider, new int[]{appWidgetId});
            registerForBroadcastsLocked(provider, getWidgetIds(provider.widgets));
            saveGroupStateAsync(userId);
            if (DEBUG) {
                Slog.i(TAG, "Bound widget " + appWidgetId + " to provider " + provider.id);
            }
            return true;
        }
    }

    public int[] getAppWidgetIds(ComponentName componentName) {
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "getAppWidgetIds() " + userId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(componentName.getPackageName());
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            ProviderId providerId = new ProviderId(Binder.getCallingUid(), componentName, null);
            Provider provider = lookupProviderLocked(providerId);
            if (provider != null) {
                return getWidgetIds(provider.widgets);
            }
            return new int[0];
        }
    }

    public int[] getAppWidgetIdsForHost(String callingPackage, int hostId) {
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "getAppWidgetIdsForHost() " + userId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            HostId id = new HostId(Binder.getCallingUid(), hostId, callingPackage);
            Host host = lookupHostLocked(id);
            if (host != null) {
                return getWidgetIds(host.widgets);
            }
            return new int[0];
        }
    }

    public void bindRemoteViewsService(String callingPackage, int appWidgetId, Intent intent, IBinder callbacks) {
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "bindRemoteViewsService() " + userId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            Widget widget = lookupWidgetLocked(appWidgetId, Binder.getCallingUid(), callingPackage);
            if (widget == null) {
                throw new IllegalArgumentException("Bad widget id");
            }
            if (widget.provider == null) {
                throw new IllegalArgumentException("No provider for widget " + appWidgetId);
            }
            ComponentName componentName = intent.getComponent();
            String providerPackage = widget.provider.id.componentName.getPackageName();
            String servicePackage = componentName.getPackageName();
            if (!servicePackage.equals(providerPackage)) {
                throw new SecurityException("The taget service not in the same package as the widget provider");
            }
            this.mSecurityPolicy.enforceServiceExistsAndRequiresBindRemoteViewsPermission(componentName, widget.provider.getUserId());
            Intent.FilterComparison fc = new Intent.FilterComparison(intent);
            Pair<Integer, Intent.FilterComparison> key = Pair.create(Integer.valueOf(appWidgetId), fc);
            if (this.mBoundRemoteViewsServices.containsKey(key)) {
                ServiceConnectionProxy connection = (ServiceConnectionProxy) this.mBoundRemoteViewsServices.get(key);
                connection.disconnect();
                unbindService(connection);
                this.mBoundRemoteViewsServices.remove(key);
            }
            ServiceConnection connection2 = new ServiceConnectionProxy(callbacks);
            bindService(intent, connection2, widget.provider.info.getProfile());
            this.mBoundRemoteViewsServices.put(key, connection2);
            Pair<Integer, Intent.FilterComparison> serviceId = Pair.create(Integer.valueOf(widget.provider.id.uid), fc);
            incrementAppWidgetServiceRefCount(appWidgetId, serviceId);
        }
    }

    public void unbindRemoteViewsService(String callingPackage, int appWidgetId, Intent intent) {
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "unbindRemoteViewsService() " + userId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            Pair<Integer, Intent.FilterComparison> key = Pair.create(Integer.valueOf(appWidgetId), new Intent.FilterComparison(intent));
            if (this.mBoundRemoteViewsServices.containsKey(key)) {
                Widget widget = lookupWidgetLocked(appWidgetId, Binder.getCallingUid(), callingPackage);
                if (widget == null) {
                    throw new IllegalArgumentException("Bad widget id " + appWidgetId);
                }
                ServiceConnectionProxy connection = (ServiceConnectionProxy) this.mBoundRemoteViewsServices.get(key);
                connection.disconnect();
                this.mContext.unbindService(connection);
                this.mBoundRemoteViewsServices.remove(key);
            }
        }
    }

    public void deleteHost(String callingPackage, int hostId) {
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "deleteHost() " + userId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            HostId id = new HostId(Binder.getCallingUid(), hostId, callingPackage);
            Host host = lookupHostLocked(id);
            if (host == null) {
                return;
            }
            deleteHostLocked(host);
            saveGroupStateAsync(userId);
            if (DEBUG) {
                Slog.i(TAG, "Deleted host " + host.id);
            }
        }
    }

    public void deleteAllHosts() {
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "deleteAllHosts() " + userId);
        }
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            boolean changed = false;
            int N = this.mHosts.size();
            for (int i = N - 1; i >= 0; i--) {
                Host host = this.mHosts.get(i);
                if (host.id.uid == Binder.getCallingUid()) {
                    deleteHostLocked(host);
                    changed = true;
                    if (DEBUG) {
                        Slog.i(TAG, "Deleted host " + host.id);
                    }
                }
            }
            if (changed) {
                saveGroupStateAsync(userId);
            }
        }
    }

    public AppWidgetProviderInfo getAppWidgetInfo(String callingPackage, int appWidgetId) {
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "getAppWidgetInfo() " + userId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            Widget widget = lookupWidgetLocked(appWidgetId, Binder.getCallingUid(), callingPackage);
            if (widget == null || widget.provider == null || widget.provider.zombie) {
                return null;
            }
            return cloneIfLocalBinder(widget.provider.info);
        }
    }

    public RemoteViews getAppWidgetViews(String callingPackage, int appWidgetId) {
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "getAppWidgetViews() " + userId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            Widget widget = lookupWidgetLocked(appWidgetId, Binder.getCallingUid(), callingPackage);
            if (widget == null) {
                return null;
            }
            return cloneIfLocalBinder(widget.getEffectiveViewsLocked());
        }
    }

    public void updateAppWidgetOptions(String callingPackage, int appWidgetId, Bundle options) {
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "updateAppWidgetOptions() " + userId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            Widget widget = lookupWidgetLocked(appWidgetId, Binder.getCallingUid(), callingPackage);
            if (widget == null) {
                return;
            }
            widget.options.setDefusable(true);
            options.setDefusable(true);
            widget.options.putAll(options);
            sendOptionsChangedIntentLocked(widget);
            saveGroupStateAsync(userId);
        }
    }

    public Bundle getAppWidgetOptions(String callingPackage, int appWidgetId) {
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "getAppWidgetOptions() " + userId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            Widget widget = lookupWidgetLocked(appWidgetId, Binder.getCallingUid(), callingPackage);
            if (widget != null && widget.options != null) {
                return cloneIfLocalBinder(widget.options);
            }
            return Bundle.EMPTY;
        }
    }

    public void updateAppWidgetIds(String callingPackage, int[] appWidgetIds, RemoteViews views) {
        if (DEBUG) {
            Slog.i(TAG, "updateAppWidgetIds() " + UserHandle.getCallingUserId());
        }
        updateAppWidgetIds(callingPackage, appWidgetIds, views, false);
    }

    public void partiallyUpdateAppWidgetIds(String callingPackage, int[] appWidgetIds, RemoteViews views) {
        if (DEBUG) {
            Slog.i(TAG, "partiallyUpdateAppWidgetIds() " + UserHandle.getCallingUserId());
        }
        updateAppWidgetIds(callingPackage, appWidgetIds, views, true);
    }

    public void notifyAppWidgetViewDataChanged(String callingPackage, int[] appWidgetIds, int viewId) {
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "notifyAppWidgetViewDataChanged() " + userId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        if (appWidgetIds == null || appWidgetIds.length == 0) {
            return;
        }
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            for (int appWidgetId : appWidgetIds) {
                Widget widget = lookupWidgetLocked(appWidgetId, Binder.getCallingUid(), callingPackage);
                if (widget != null) {
                    scheduleNotifyAppWidgetViewDataChanged(widget, viewId);
                }
            }
        }
    }

    public void updateAppWidgetProvider(ComponentName componentName, RemoteViews views) {
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "updateAppWidgetProvider() " + userId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(componentName.getPackageName());
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            ProviderId providerId = new ProviderId(Binder.getCallingUid(), componentName, null);
            Provider provider = lookupProviderLocked(providerId);
            if (provider == null) {
                Slog.w(TAG, "Provider doesn't exist " + providerId);
                return;
            }
            ArrayList<Widget> instances = provider.widgets;
            int N = instances.size();
            for (int i = 0; i < N; i++) {
                Widget widget = instances.get(i);
                updateAppWidgetInstanceLocked(widget, views, false);
            }
        }
    }

    public ParceledListSlice<AppWidgetProviderInfo> getInstalledProvidersForProfile(int categoryFilter, int profileId) {
        ParceledListSlice<AppWidgetProviderInfo> parceledListSlice;
        int providerProfileId;
        int userId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "getInstalledProvidersForProfiles() " + userId);
        }
        if (!this.mSecurityPolicy.isEnabledGroupProfile(profileId)) {
            return null;
        }
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            ArrayList<AppWidgetProviderInfo> result = new ArrayList<>();
            int providerCount = this.mProviders.size();
            for (int i = 0; i < providerCount; i++) {
                Provider provider = this.mProviders.get(i);
                AppWidgetProviderInfo info = provider.info;
                if (!provider.zombie && (info.widgetCategory & categoryFilter) != 0 && (providerProfileId = info.getProfile().getIdentifier()) == profileId && this.mSecurityPolicy.isProviderInCallerOrInProfileAndWhitelListed(provider.id.componentName.getPackageName(), providerProfileId)) {
                    result.add(cloneIfLocalBinder(info));
                }
            }
            parceledListSlice = new ParceledListSlice<>(result);
        }
        return parceledListSlice;
    }

    private void updateAppWidgetIds(String callingPackage, int[] appWidgetIds, RemoteViews views, boolean partially) {
        int userId = UserHandle.getCallingUserId();
        if (appWidgetIds == null || appWidgetIds.length == 0) {
            return;
        }
        this.mSecurityPolicy.enforceCallFromPackage(callingPackage);
        int bitmapMemoryUsage = views != null ? views.estimateMemoryUsage() : 0;
        if (bitmapMemoryUsage > this.mMaxWidgetBitmapMemory) {
            throw new IllegalArgumentException("RemoteViews for widget update exceeds maximum bitmap memory usage (used: " + bitmapMemoryUsage + ", max: " + this.mMaxWidgetBitmapMemory + ")");
        }
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            for (int appWidgetId : appWidgetIds) {
                Widget widget = lookupWidgetLocked(appWidgetId, Binder.getCallingUid(), callingPackage);
                if (widget != null) {
                    updateAppWidgetInstanceLocked(widget, views, partially);
                }
            }
        }
    }

    private int incrementAndGetAppWidgetIdLocked(int userId) {
        int appWidgetId = peekNextAppWidgetIdLocked(userId) + 1;
        this.mNextAppWidgetIds.put(userId, appWidgetId);
        return appWidgetId;
    }

    private void setMinAppWidgetIdLocked(int userId, int minWidgetId) {
        int nextAppWidgetId = peekNextAppWidgetIdLocked(userId);
        if (nextAppWidgetId >= minWidgetId) {
            return;
        }
        this.mNextAppWidgetIds.put(userId, minWidgetId);
    }

    private int peekNextAppWidgetIdLocked(int userId) {
        if (this.mNextAppWidgetIds.indexOfKey(userId) < 0) {
            return 1;
        }
        return this.mNextAppWidgetIds.get(userId);
    }

    private Host lookupOrAddHostLocked(HostId id) {
        Host host = null;
        Host host2 = lookupHostLocked(id);
        if (host2 != null) {
            return host2;
        }
        Host host3 = new Host(host);
        host3.id = id;
        this.mHosts.add(host3);
        return host3;
    }

    private void deleteHostLocked(Host host) {
        int N = host.widgets.size();
        for (int i = N - 1; i >= 0; i--) {
            Widget widget = host.widgets.remove(i);
            deleteAppWidgetLocked(widget);
        }
        this.mHosts.remove(host);
        host.callbacks = null;
    }

    private void deleteAppWidgetLocked(Widget widget) {
        unbindAppWidgetRemoteViewsServicesLocked(widget);
        Host host = widget.host;
        host.widgets.remove(widget);
        pruneHostLocked(host);
        removeWidgetLocked(widget);
        Provider provider = widget.provider;
        if (provider == null) {
            return;
        }
        provider.widgets.remove(widget);
        if (provider.zombie) {
            return;
        }
        sendDeletedIntentLocked(widget);
        if (!provider.widgets.isEmpty()) {
            return;
        }
        cancelBroadcasts(provider);
        sendDisabledIntentLocked(provider);
    }

    private void cancelBroadcasts(Provider provider) {
        if (DEBUG) {
            Slog.i(TAG, "cancelBroadcasts() for " + provider);
        }
        if (provider.broadcast == null) {
            return;
        }
        this.mAlarmManager.cancel(provider.broadcast);
        long token = Binder.clearCallingIdentity();
        try {
            provider.broadcast.cancel();
            Binder.restoreCallingIdentity(token);
            provider.broadcast = null;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    private void unbindAppWidgetRemoteViewsServicesLocked(Widget widget) {
        int appWidgetId = widget.appWidgetId;
        Iterator<Pair<Integer, Intent.FilterComparison>> it = this.mBoundRemoteViewsServices.keySet().iterator();
        while (it.hasNext()) {
            Pair<Integer, Intent.FilterComparison> key = it.next();
            if (((Integer) key.first).intValue() == appWidgetId) {
                ServiceConnectionProxy conn = (ServiceConnectionProxy) this.mBoundRemoteViewsServices.get(key);
                conn.disconnect();
                this.mContext.unbindService(conn);
                it.remove();
            }
        }
        decrementAppWidgetServiceRefCount(widget);
    }

    private void destroyRemoteViewsService(final Intent intent, Widget widget) {
        ServiceConnection conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                IRemoteViewsFactory cb = IRemoteViewsFactory.Stub.asInterface(service);
                try {
                    cb.onDestroy(intent);
                } catch (RemoteException re) {
                    Slog.e(AppWidgetServiceImpl.TAG, "Error calling remove view factory", re);
                }
                AppWidgetServiceImpl.this.mContext.unbindService(this);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };
        long token = Binder.clearCallingIdentity();
        try {
            this.mContext.bindServiceAsUser(intent, conn, 33554433, widget.provider.info.getProfile());
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void incrementAppWidgetServiceRefCount(int appWidgetId, Pair<Integer, Intent.FilterComparison> serviceId) {
        HashSet<Integer> appWidgetIds;
        if (this.mRemoteViewsServicesAppWidgets.containsKey(serviceId)) {
            appWidgetIds = this.mRemoteViewsServicesAppWidgets.get(serviceId);
        } else {
            appWidgetIds = new HashSet<>();
            this.mRemoteViewsServicesAppWidgets.put(serviceId, appWidgetIds);
        }
        appWidgetIds.add(Integer.valueOf(appWidgetId));
    }

    private void decrementAppWidgetServiceRefCount(Widget widget) {
        Iterator<Pair<Integer, Intent.FilterComparison>> it = this.mRemoteViewsServicesAppWidgets.keySet().iterator();
        while (it.hasNext()) {
            Pair<Integer, Intent.FilterComparison> key = it.next();
            HashSet<Integer> ids = this.mRemoteViewsServicesAppWidgets.get(key);
            if (ids.remove(Integer.valueOf(widget.appWidgetId)) && ids.isEmpty()) {
                destroyRemoteViewsService(((Intent.FilterComparison) key.second).getIntent(), widget);
                it.remove();
            }
        }
    }

    private void saveGroupStateAsync(int groupId) {
        this.mSaveStateHandler.post(new SaveStateRunnable(groupId));
    }

    private void updateAppWidgetInstanceLocked(Widget widget, RemoteViews views, boolean isPartialUpdate) {
        if (widget == null || widget.provider == null || widget.provider.zombie || widget.host.zombie) {
            return;
        }
        if (isPartialUpdate && widget.views != null) {
            widget.views.mergeRemoteViews(views);
        } else {
            widget.views = views;
        }
        scheduleNotifyUpdateAppWidgetLocked(widget, widget.getEffectiveViewsLocked());
    }

    private void scheduleNotifyAppWidgetViewDataChanged(Widget widget, int viewId) {
        if (widget == null || widget.host == null || widget.host.zombie || widget.host.callbacks == null || widget.provider == null || widget.provider.zombie) {
            return;
        }
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = widget.host;
        args.arg2 = widget.host.callbacks;
        args.argi1 = widget.appWidgetId;
        args.argi2 = viewId;
        this.mCallbackHandler.obtainMessage(4, args).sendToTarget();
    }

    private void handleNotifyAppWidgetViewDataChanged(Host host, IAppWidgetHost callbacks, int appWidgetId, int viewId) {
        try {
            callbacks.viewDataChanged(appWidgetId, viewId);
        } catch (RemoteException e) {
            callbacks = null;
        }
        synchronized (this.mLock) {
            if (callbacks == null) {
                host.callbacks = null;
                Set<Pair<Integer, Intent.FilterComparison>> keys = this.mRemoteViewsServicesAppWidgets.keySet();
                for (Pair<Integer, Intent.FilterComparison> key : keys) {
                    if (this.mRemoteViewsServicesAppWidgets.get(key).contains(Integer.valueOf(appWidgetId))) {
                        ServiceConnection connection = new ServiceConnection() {
                            @Override
                            public void onServiceConnected(ComponentName name, IBinder service) {
                                IRemoteViewsFactory cb = IRemoteViewsFactory.Stub.asInterface(service);
                                try {
                                    cb.onDataSetChangedAsync();
                                } catch (RemoteException e2) {
                                    Slog.e(AppWidgetServiceImpl.TAG, "Error calling onDataSetChangedAsync()", e2);
                                }
                                AppWidgetServiceImpl.this.mContext.unbindService(this);
                            }

                            @Override
                            public void onServiceDisconnected(ComponentName name) {
                            }
                        };
                        int userId = UserHandle.getUserId(((Integer) key.first).intValue());
                        Intent intent = ((Intent.FilterComparison) key.second).getIntent();
                        bindService(intent, connection, new UserHandle(userId));
                    }
                }
            }
        }
    }

    private void scheduleNotifyUpdateAppWidgetLocked(Widget widget, RemoteViews updateViews) {
        long requestTime = SystemClock.uptimeMillis();
        if (widget != null) {
            widget.lastUpdateTime = requestTime;
        }
        if (widget == null || widget.provider == null || widget.provider.zombie || widget.host.callbacks == null || widget.host.zombie) {
            return;
        }
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = widget.host;
        args.arg2 = widget.host.callbacks;
        if (updateViews != null) {
            args.arg3 = updateViews.clone();
        } else {
            args.arg3 = null;
        }
        args.arg4 = Long.valueOf(requestTime);
        args.argi1 = widget.appWidgetId;
        this.mCallbackHandler.obtainMessage(1, args).sendToTarget();
    }

    private void handleNotifyUpdateAppWidget(Host host, IAppWidgetHost callbacks, int appWidgetId, RemoteViews views, long requestTime) {
        try {
            callbacks.updateAppWidget(appWidgetId, views);
            host.lastWidgetUpdateTime = requestTime;
        } catch (RemoteException re) {
            synchronized (this.mLock) {
                Slog.e(TAG, "Widget host dead: " + host.id, re);
                host.callbacks = null;
            }
        }
    }

    private void scheduleNotifyProviderChangedLocked(Widget widget) {
        if (widget == null || widget.provider == null || widget.provider.zombie || widget.host.callbacks == null || widget.host.zombie) {
            return;
        }
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = widget.host;
        args.arg2 = widget.host.callbacks;
        args.arg3 = widget.provider.info;
        args.argi1 = widget.appWidgetId;
        this.mCallbackHandler.obtainMessage(2, args).sendToTarget();
    }

    private void handleNotifyProviderChanged(Host host, IAppWidgetHost callbacks, int appWidgetId, AppWidgetProviderInfo info) {
        try {
            callbacks.providerChanged(appWidgetId, info);
        } catch (RemoteException re) {
            synchronized (this.mLock) {
                Slog.e(TAG, "Widget host dead: " + host.id, re);
                host.callbacks = null;
            }
        }
    }

    private void scheduleNotifyGroupHostsForProvidersChangedLocked(int userId) {
        int[] profileIds = this.mSecurityPolicy.getEnabledGroupProfileIds(userId);
        int N = this.mHosts.size();
        for (int i = N - 1; i >= 0; i--) {
            Host host = this.mHosts.get(i);
            boolean hostInGroup = false;
            int M = profileIds.length;
            int j = 0;
            while (true) {
                if (j >= M) {
                    break;
                }
                int profileId = profileIds[j];
                if (host.getUserId() != profileId) {
                    j++;
                } else {
                    hostInGroup = true;
                    break;
                }
            }
            if (hostInGroup && host != null && !host.zombie && host.callbacks != null) {
                SomeArgs args = SomeArgs.obtain();
                args.arg1 = host;
                args.arg2 = host.callbacks;
                this.mCallbackHandler.obtainMessage(3, args).sendToTarget();
            }
        }
    }

    private void handleNotifyProvidersChanged(Host host, IAppWidgetHost callbacks) {
        try {
            callbacks.providersChanged();
        } catch (RemoteException re) {
            synchronized (this.mLock) {
                Slog.e(TAG, "Widget host dead: " + host.id, re);
                host.callbacks = null;
            }
        }
    }

    private static boolean isLocalBinder() {
        return Process.myPid() == Binder.getCallingPid();
    }

    private static RemoteViews cloneIfLocalBinder(RemoteViews rv) {
        if (isLocalBinder() && rv != null) {
            return rv.clone();
        }
        return rv;
    }

    private static AppWidgetProviderInfo cloneIfLocalBinder(AppWidgetProviderInfo info) {
        if (isLocalBinder() && info != null) {
            return info.clone();
        }
        return info;
    }

    private static Bundle cloneIfLocalBinder(Bundle bundle) {
        if (isLocalBinder() && bundle != null) {
            return (Bundle) bundle.clone();
        }
        return bundle;
    }

    private Widget lookupWidgetLocked(int appWidgetId, int uid, String packageName) {
        int N = this.mWidgets.size();
        for (int i = 0; i < N; i++) {
            Widget widget = this.mWidgets.get(i);
            if (widget.appWidgetId == appWidgetId && this.mSecurityPolicy.canAccessAppWidget(widget, uid, packageName)) {
                return widget;
            }
        }
        return null;
    }

    private Provider lookupProviderLocked(ProviderId id) {
        int N = this.mProviders.size();
        for (int i = 0; i < N; i++) {
            Provider provider = this.mProviders.get(i);
            if (provider.id.equals(id)) {
                return provider;
            }
        }
        return null;
    }

    private Host lookupHostLocked(HostId hostId) {
        int N = this.mHosts.size();
        for (int i = 0; i < N; i++) {
            Host host = this.mHosts.get(i);
            if (host.id.equals(hostId)) {
                return host;
            }
        }
        return null;
    }

    private void pruneHostLocked(Host host) {
        if (host.widgets.size() != 0 || host.callbacks != null) {
            return;
        }
        if (DEBUG) {
            Slog.i(TAG, "Pruning host " + host.id);
        }
        this.mHosts.remove(host);
    }

    private void loadGroupWidgetProvidersLocked(int[] profileIds) {
        List<ResolveInfo> allReceivers = null;
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_UPDATE");
        for (int profileId : profileIds) {
            List<ResolveInfo> receivers = queryIntentReceivers(intent, profileId);
            if (receivers != null && !receivers.isEmpty()) {
                if (allReceivers == null) {
                    allReceivers = new ArrayList<>();
                }
                allReceivers.addAll(receivers);
            }
        }
        int N = allReceivers == null ? 0 : allReceivers.size();
        for (int i = 0; i < N; i++) {
            ResolveInfo receiver = allReceivers.get(i);
            addProviderLocked(receiver);
        }
    }

    private boolean addProviderLocked(ResolveInfo ri) {
        ComponentName componentName;
        ProviderId providerId;
        Provider provider;
        ProviderId providerId2 = null;
        if ((ri.activityInfo.applicationInfo.flags & PackageManagerService.DumpState.DUMP_DOMAIN_PREFERRED) != 0 || !ri.activityInfo.isEnabled() || (provider = parseProviderInfoXml((providerId = new ProviderId(ri.activityInfo.applicationInfo.uid, (componentName = new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name)), providerId2)), ri)) == null) {
            return false;
        }
        Provider existing = lookupProviderLocked(providerId);
        if (existing == null) {
            ProviderId restoredProviderId = new ProviderId(-1, componentName, providerId2);
            existing = lookupProviderLocked(restoredProviderId);
        }
        if (existing != null) {
            if (existing.zombie && !this.mSafeMode) {
                existing.id = providerId;
                existing.zombie = false;
                existing.info = provider.info;
                if (DEBUG) {
                    Slog.i(TAG, "Provider placeholder now reified: " + existing);
                    return true;
                }
                return true;
            }
            return true;
        }
        this.mProviders.add(provider);
        return true;
    }

    private void deleteWidgetsLocked(Provider provider, int userId) {
        int N = provider.widgets.size();
        for (int i = N - 1; i >= 0; i--) {
            Widget widget = provider.widgets.get(i);
            if (userId == -1 || userId == widget.host.getUserId()) {
                provider.widgets.remove(i);
                updateAppWidgetInstanceLocked(widget, null, false);
                widget.host.widgets.remove(widget);
                removeWidgetLocked(widget);
                widget.provider = null;
                pruneHostLocked(widget.host);
                widget.host = null;
            }
        }
    }

    private void deleteProviderLocked(Provider provider) {
        deleteWidgetsLocked(provider, -1);
        this.mProviders.remove(provider);
        cancelBroadcasts(provider);
    }

    private void sendEnableIntentLocked(Provider p) {
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_ENABLED");
        intent.setComponent(p.info.provider);
        sendBroadcastAsUser(intent, p.info.getProfile());
    }

    private void sendUpdateIntentLocked(Provider provider, int[] appWidgetIds) {
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_UPDATE");
        intent.putExtra("appWidgetIds", appWidgetIds);
        intent.setComponent(provider.info.provider);
        sendBroadcastAsUser(intent, provider.info.getProfile());
    }

    private void sendDeletedIntentLocked(Widget widget) {
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_DELETED");
        intent.setComponent(widget.provider.info.provider);
        intent.putExtra("appWidgetId", widget.appWidgetId);
        sendBroadcastAsUser(intent, widget.provider.info.getProfile());
    }

    private void sendDisabledIntentLocked(Provider provider) {
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_DISABLED");
        intent.setComponent(provider.info.provider);
        sendBroadcastAsUser(intent, provider.info.getProfile());
    }

    public void sendOptionsChangedIntentLocked(Widget widget) {
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_UPDATE_OPTIONS");
        intent.setComponent(widget.provider.info.provider);
        intent.putExtra("appWidgetId", widget.appWidgetId);
        intent.putExtra("appWidgetOptions", widget.options);
        sendBroadcastAsUser(intent, widget.provider.info.getProfile());
    }

    private void registerForBroadcastsLocked(Provider provider, int[] appWidgetIds) {
        if (provider.info.updatePeriodMillis <= 0) {
            return;
        }
        boolean alreadyRegistered = provider.broadcast != null;
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_UPDATE");
        intent.putExtra("appWidgetIds", appWidgetIds);
        intent.setComponent(provider.info.provider);
        long oldId = Binder.clearCallingIdentity();
        try {
            provider.broadcast = PendingIntent.getBroadcastAsUser(this.mContext, 1, intent, 134217728, provider.info.getProfile());
            if (alreadyRegistered) {
                return;
            }
            long period = provider.info.updatePeriodMillis;
            if (period < MIN_UPDATE_PERIOD) {
                period = MIN_UPDATE_PERIOD;
            }
            oldId = Binder.clearCallingIdentity();
            try {
                this.mAlarmManager.setInexactRepeating(2, SystemClock.elapsedRealtime() + period, period, provider.broadcast);
            } finally {
            }
        } finally {
        }
    }

    private static int[] getWidgetIds(ArrayList<Widget> widgets) {
        int instancesSize = widgets.size();
        int[] appWidgetIds = new int[instancesSize];
        for (int i = 0; i < instancesSize; i++) {
            appWidgetIds[i] = widgets.get(i).appWidgetId;
        }
        return appWidgetIds;
    }

    private static void dumpProvider(Provider provider, int index, PrintWriter pw) {
        AppWidgetProviderInfo info = provider.info;
        pw.print("  [");
        pw.print(index);
        pw.print("] provider ");
        pw.println(provider.id);
        pw.print("    min=(");
        pw.print(info.minWidth);
        pw.print("x");
        pw.print(info.minHeight);
        pw.print(")   minResize=(");
        pw.print(info.minResizeWidth);
        pw.print("x");
        pw.print(info.minResizeHeight);
        pw.print(") updatePeriodMillis=");
        pw.print(info.updatePeriodMillis);
        pw.print(" resizeMode=");
        pw.print(info.resizeMode);
        pw.print(info.widgetCategory);
        pw.print(" autoAdvanceViewId=");
        pw.print(info.autoAdvanceViewId);
        pw.print(" initialLayout=#");
        pw.print(Integer.toHexString(info.initialLayout));
        pw.print(" initialKeyguardLayout=#");
        pw.print(Integer.toHexString(info.initialKeyguardLayout));
        pw.print(" zombie=");
        pw.println(provider.zombie);
    }

    private static void dumpHost(Host host, int index, PrintWriter pw) {
        pw.print("  [");
        pw.print(index);
        pw.print("] hostId=");
        pw.println(host.id);
        pw.print("    callbacks=");
        pw.println(host.callbacks);
        pw.print("    widgets.size=");
        pw.print(host.widgets.size());
        pw.print(" zombie=");
        pw.println(host.zombie);
    }

    private static void dumpGrant(Pair<Integer, String> grant, int index, PrintWriter pw) {
        pw.print("  [");
        pw.print(index);
        pw.print(']');
        pw.print(" user=");
        pw.print(grant.first);
        pw.print(" package=");
        pw.println((String) grant.second);
    }

    private static void dumpWidget(Widget widget, int index, PrintWriter pw) {
        pw.print("  [");
        pw.print(index);
        pw.print("] id=");
        pw.println(widget.appWidgetId);
        pw.print("    host=");
        pw.println(widget.host.id);
        if (widget.provider != null) {
            pw.print("    provider=");
            pw.println(widget.provider.id);
        }
        if (widget.host != null) {
            pw.print("    host.callbacks=");
            pw.println(widget.host.callbacks);
        }
        if (widget.views == null) {
            return;
        }
        pw.print("    views=");
        pw.println(widget.views);
    }

    private static void serializeProvider(XmlSerializer out, Provider p) throws IOException {
        out.startTag(null, "p");
        out.attribute(null, "pkg", p.info.provider.getPackageName());
        out.attribute(null, "cl", p.info.provider.getClassName());
        out.attribute(null, "tag", Integer.toHexString(p.tag));
        out.endTag(null, "p");
    }

    private static void serializeHost(XmlSerializer out, Host host) throws IOException {
        out.startTag(null, "h");
        out.attribute(null, "pkg", host.id.packageName);
        out.attribute(null, "id", Integer.toHexString(host.id.hostId));
        out.attribute(null, "tag", Integer.toHexString(host.tag));
        out.endTag(null, "h");
    }

    private static void serializeAppWidget(XmlSerializer out, Widget widget) throws IOException {
        out.startTag(null, "g");
        out.attribute(null, "id", Integer.toHexString(widget.appWidgetId));
        out.attribute(null, "rid", Integer.toHexString(widget.restoredId));
        out.attribute(null, "h", Integer.toHexString(widget.host.tag));
        if (widget.provider != null) {
            out.attribute(null, "p", Integer.toHexString(widget.provider.tag));
        }
        if (widget.options != null) {
            out.attribute(null, "min_width", Integer.toHexString(widget.options.getInt("appWidgetMinWidth")));
            out.attribute(null, "min_height", Integer.toHexString(widget.options.getInt("appWidgetMinHeight")));
            out.attribute(null, "max_width", Integer.toHexString(widget.options.getInt("appWidgetMaxWidth")));
            out.attribute(null, "max_height", Integer.toHexString(widget.options.getInt("appWidgetMaxHeight")));
            out.attribute(null, "host_category", Integer.toHexString(widget.options.getInt("appWidgetCategory")));
        }
        out.endTag(null, "g");
    }

    public List<String> getWidgetParticipants(int userId) {
        return this.mBackupRestoreController.getWidgetParticipants(userId);
    }

    public byte[] getWidgetState(String packageName, int userId) {
        return this.mBackupRestoreController.getWidgetState(packageName, userId);
    }

    public void restoreStarting(int userId) {
        this.mBackupRestoreController.restoreStarting(userId);
    }

    public void restoreWidgetState(String packageName, byte[] restoredState, int userId) {
        this.mBackupRestoreController.restoreWidgetState(packageName, restoredState, userId);
    }

    public void restoreFinished(int userId) {
        this.mBackupRestoreController.restoreFinished(userId);
    }

    private Provider parseProviderInfoXml(ProviderId providerId, ResolveInfo ri) throws Throwable {
        int type;
        ActivityInfo activityInfo = ri.activityInfo;
        XmlResourceParser parser = null;
        try {
            try {
                parser = activityInfo.loadXmlMetaData(this.mContext.getPackageManager(), "android.appwidget.provider");
                if (parser == null) {
                    Slog.w(TAG, "No android.appwidget.provider meta-data for AppWidget provider '" + providerId + '\'');
                    if (parser != null) {
                        parser.close();
                    }
                    return null;
                }
                AttributeSet attrs = Xml.asAttributeSet(parser);
                do {
                    type = parser.next();
                    if (type == 1) {
                        break;
                    }
                } while (type != 2);
                String nodeName = parser.getName();
                if (!"appwidget-provider".equals(nodeName)) {
                    Slog.w(TAG, "Meta-data does not start with appwidget-provider tag for AppWidget provider " + providerId.componentName + " for user " + providerId.uid);
                    if (parser != null) {
                        parser.close();
                    }
                    return null;
                }
                Provider provider = new Provider(null);
                try {
                    try {
                        provider.id = providerId;
                        AppWidgetProviderInfo info = new AppWidgetProviderInfo();
                        provider.info = info;
                        info.provider = providerId.componentName;
                        info.providerInfo = activityInfo;
                        long identity = Binder.clearCallingIdentity();
                        try {
                            PackageManager pm = this.mContext.getPackageManager();
                            int userId = UserHandle.getUserId(providerId.uid);
                            ApplicationInfo app = pm.getApplicationInfoAsUser(activityInfo.packageName, 0, userId);
                            Resources resources = pm.getResourcesForApplication(app);
                            Binder.restoreCallingIdentity(identity);
                            TypedArray sa = resources.obtainAttributes(attrs, com.android.internal.R.styleable.AppWidgetProviderInfo);
                            TypedValue value = sa.peekValue(0);
                            info.minWidth = value != null ? value.data : 0;
                            TypedValue value2 = sa.peekValue(1);
                            info.minHeight = value2 != null ? value2.data : 0;
                            TypedValue value3 = sa.peekValue(8);
                            info.minResizeWidth = value3 != null ? value3.data : info.minWidth;
                            TypedValue value4 = sa.peekValue(9);
                            info.minResizeHeight = value4 != null ? value4.data : info.minHeight;
                            info.updatePeriodMillis = sa.getInt(2, 0);
                            info.initialLayout = sa.getResourceId(3, 0);
                            info.initialKeyguardLayout = sa.getResourceId(10, 0);
                            String className = sa.getString(4);
                            if (className != null) {
                                info.configure = new ComponentName(providerId.componentName.getPackageName(), className);
                            }
                            info.label = activityInfo.loadLabel(this.mContext.getPackageManager()).toString();
                            info.icon = ri.getIconResource();
                            info.previewImage = sa.getResourceId(5, 0);
                            info.autoAdvanceViewId = sa.getResourceId(6, -1);
                            info.resizeMode = sa.getInt(7, 0);
                            info.widgetCategory = sa.getInt(11, 1);
                            sa.recycle();
                            if (parser != null) {
                                parser.close();
                            }
                            return provider;
                        } catch (Throwable th) {
                            Binder.restoreCallingIdentity(identity);
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        if (parser != null) {
                            parser.close();
                        }
                        throw th;
                    }
                } catch (PackageManager.NameNotFoundException | IOException | XmlPullParserException e) {
                    e = e;
                    Slog.w(TAG, "XML parsing failed for AppWidget provider " + providerId.componentName + " for user " + providerId.uid, e);
                    if (parser != null) {
                        parser.close();
                    }
                    return null;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        } catch (PackageManager.NameNotFoundException | IOException | XmlPullParserException e2) {
            e = e2;
        }
    }

    private int getUidForPackage(String packageName, int userId) {
        PackageInfo pkgInfo = null;
        long identity = Binder.clearCallingIdentity();
        try {
            pkgInfo = this.mPackageManager.getPackageInfo(packageName, 0, userId);
        } catch (RemoteException e) {
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
        if (pkgInfo == null || pkgInfo.applicationInfo == null) {
            return -1;
        }
        return pkgInfo.applicationInfo.uid;
    }

    private ActivityInfo getProviderInfo(ComponentName componentName, int userId) {
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_UPDATE");
        intent.setComponent(componentName);
        List<ResolveInfo> receivers = queryIntentReceivers(intent, userId);
        if (!receivers.isEmpty()) {
            return receivers.get(0).activityInfo;
        }
        return null;
    }

    private List<ResolveInfo> queryIntentReceivers(Intent intent, int userId) {
        long identity = Binder.clearCallingIdentity();
        try {
            int flags = isProfileWithUnlockedParent(userId) ? 268435584 | 786432 : 268435584;
            return this.mPackageManager.queryIntentReceivers(intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), flags | 1024, userId).getList();
        } catch (RemoteException e) {
            return Collections.emptyList();
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    void onUserUnlocked(int userId) {
        if (isProfileWithLockedParent(userId)) {
            return;
        }
        if (!this.mUserManager.isUserUnlockingOrUnlocked(userId)) {
            Slog.w(TAG, "User " + userId + " is no longer unlocked - exiting");
            return;
        }
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            reloadWidgetsMaskedStateForGroup(this.mSecurityPolicy.getGroupParent(userId));
            int N = this.mProviders.size();
            for (int i = 0; i < N; i++) {
                Provider provider = this.mProviders.get(i);
                if (provider.getUserId() == userId && provider.widgets.size() > 0) {
                    sendEnableIntentLocked(provider);
                    int[] appWidgetIds = getWidgetIds(provider.widgets);
                    sendUpdateIntentLocked(provider, appWidgetIds);
                    registerForBroadcastsLocked(provider, appWidgetIds);
                }
            }
        }
    }

    private void loadGroupStateLocked(int[] profileIds) {
        List<LoadedWidgetState> loadedWidgets = new ArrayList<>();
        int version = 0;
        for (int profileId : profileIds) {
            AtomicFile file = getSavedStateFile(profileId);
            try {
                FileInputStream stream = file.openRead();
                version = readProfileStateFromFileLocked(stream, profileId, loadedWidgets);
                IoUtils.closeQuietly(stream);
            } catch (FileNotFoundException e) {
                Slog.w(TAG, "Failed to read state: " + e);
            }
        }
        if (version >= 0) {
            bindLoadedWidgetsLocked(loadedWidgets);
            performUpgradeLocked(version);
            return;
        }
        Slog.w(TAG, "Failed to read state, clearing widgets and hosts.");
        clearWidgetsLocked();
        this.mHosts.clear();
        int N = this.mProviders.size();
        for (int i = 0; i < N; i++) {
            this.mProviders.get(i).widgets.clear();
        }
    }

    private void bindLoadedWidgetsLocked(List<LoadedWidgetState> loadedWidgets) {
        int loadedWidgetCount = loadedWidgets.size();
        for (int i = loadedWidgetCount - 1; i >= 0; i--) {
            LoadedWidgetState loadedWidget = loadedWidgets.remove(i);
            Widget widget = loadedWidget.widget;
            widget.provider = findProviderByTag(loadedWidget.providerTag);
            if (widget.provider != null) {
                widget.host = findHostByTag(loadedWidget.hostTag);
                if (widget.host != null) {
                    widget.provider.widgets.add(widget);
                    widget.host.widgets.add(widget);
                    addWidgetLocked(widget);
                }
            }
        }
    }

    private Provider findProviderByTag(int tag) {
        if (tag < 0) {
            return null;
        }
        int providerCount = this.mProviders.size();
        for (int i = 0; i < providerCount; i++) {
            Provider provider = this.mProviders.get(i);
            if (provider.tag == tag) {
                return provider;
            }
        }
        return null;
    }

    private Host findHostByTag(int tag) {
        if (tag < 0) {
            return null;
        }
        int hostCount = this.mHosts.size();
        for (int i = 0; i < hostCount; i++) {
            Host host = this.mHosts.get(i);
            if (host.tag == tag) {
                return host;
            }
        }
        return null;
    }

    void addWidgetLocked(Widget widget) {
        this.mWidgets.add(widget);
        onWidgetProviderAddedOrChangedLocked(widget);
    }

    void onWidgetProviderAddedOrChangedLocked(Widget widget) {
        if (widget.provider == null) {
            return;
        }
        int userId = widget.provider.getUserId();
        ArraySet<String> packages = this.mWidgetPackages.get(userId);
        if (packages == null) {
            SparseArray<ArraySet<String>> sparseArray = this.mWidgetPackages;
            packages = new ArraySet<>();
            sparseArray.put(userId, packages);
        }
        packages.add(widget.provider.info.provider.getPackageName());
        if (widget.provider.isMaskedLocked()) {
            maskWidgetsViewsLocked(widget.provider, widget);
        } else {
            widget.clearMaskedViewsLocked();
        }
    }

    void removeWidgetLocked(Widget widget) {
        this.mWidgets.remove(widget);
        onWidgetRemovedLocked(widget);
    }

    private void onWidgetRemovedLocked(Widget widget) {
        if (widget.provider == null) {
            return;
        }
        int userId = widget.provider.getUserId();
        String packageName = widget.provider.info.provider.getPackageName();
        ArraySet<String> packages = this.mWidgetPackages.get(userId);
        if (packages == null) {
            return;
        }
        int N = this.mWidgets.size();
        for (int i = 0; i < N; i++) {
            Widget w = this.mWidgets.get(i);
            if (w.provider != null && w.provider.getUserId() == userId && packageName.equals(w.provider.info.provider.getPackageName())) {
                return;
            }
        }
        packages.remove(packageName);
    }

    void clearWidgetsLocked() {
        this.mWidgets.clear();
        onWidgetsClearedLocked();
    }

    private void onWidgetsClearedLocked() {
        this.mWidgetPackages.clear();
    }

    public boolean isBoundWidgetPackage(String packageName, int userId) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system process can call this");
        }
        synchronized (this.mLock) {
            ArraySet<String> packages = this.mWidgetPackages.get(userId);
            if (packages != null) {
                return packages.contains(packageName);
            }
            return false;
        }
    }

    private void saveStateLocked(int userId) {
        tagProvidersAndHosts();
        int[] profileIds = this.mSecurityPolicy.getEnabledGroupProfileIds(userId);
        for (int profileId : profileIds) {
            AtomicFile file = getSavedStateFile(profileId);
            try {
                FileOutputStream stream = file.startWrite();
                if (writeProfileStateToFileLocked(stream, profileId)) {
                    file.finishWrite(stream);
                } else {
                    file.failWrite(stream);
                    Slog.w(TAG, "Failed to save state, restoring backup.");
                }
            } catch (IOException e) {
                Slog.w(TAG, "Failed open state file for write: " + e);
            }
        }
    }

    private void tagProvidersAndHosts() {
        int providerCount = this.mProviders.size();
        for (int i = 0; i < providerCount; i++) {
            Provider provider = this.mProviders.get(i);
            provider.tag = i;
        }
        int hostCount = this.mHosts.size();
        for (int i2 = 0; i2 < hostCount; i2++) {
            Host host = this.mHosts.get(i2);
            host.tag = i2;
        }
    }

    private void clearProvidersAndHostsTagsLocked() {
        int providerCount = this.mProviders.size();
        for (int i = 0; i < providerCount; i++) {
            Provider provider = this.mProviders.get(i);
            provider.tag = -1;
        }
        int hostCount = this.mHosts.size();
        for (int i2 = 0; i2 < hostCount; i2++) {
            Host host = this.mHosts.get(i2);
            host.tag = -1;
        }
    }

    private boolean writeProfileStateToFileLocked(FileOutputStream stream, int userId) {
        try {
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(stream, StandardCharsets.UTF_8.name());
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.startTag(null, "gs");
            fastXmlSerializer.attribute(null, "version", String.valueOf(1));
            int N = this.mProviders.size();
            for (int i = 0; i < N; i++) {
                Provider provider = this.mProviders.get(i);
                if (provider.getUserId() == userId && provider.widgets.size() > 0) {
                    serializeProvider(fastXmlSerializer, provider);
                }
            }
            int N2 = this.mHosts.size();
            for (int i2 = 0; i2 < N2; i2++) {
                Host host = this.mHosts.get(i2);
                if (host.getUserId() == userId) {
                    serializeHost(fastXmlSerializer, host);
                }
            }
            int N3 = this.mWidgets.size();
            for (int i3 = 0; i3 < N3; i3++) {
                Widget widget = this.mWidgets.get(i3);
                if (widget.host.getUserId() == userId) {
                    serializeAppWidget(fastXmlSerializer, widget);
                }
            }
            for (Pair<Integer, String> binding : this.mPackagesWithBindWidgetPermission) {
                if (((Integer) binding.first).intValue() == userId) {
                    fastXmlSerializer.startTag(null, "b");
                    fastXmlSerializer.attribute(null, "packageName", (String) binding.second);
                    fastXmlSerializer.endTag(null, "b");
                }
            }
            fastXmlSerializer.endTag(null, "gs");
            fastXmlSerializer.endDocument();
            return true;
        } catch (IOException e) {
            Slog.w(TAG, "Failed to write state: " + e);
            return false;
        }
    }

    private int readProfileStateFromFileLocked(FileInputStream stream, int userId, List<LoadedWidgetState> outLoadedWidgets) {
        int type;
        int providerTag;
        int uid;
        ComponentName componentName;
        ActivityInfo providerInfo;
        int version = -1;
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(stream, StandardCharsets.UTF_8.name());
            int legacyProviderIndex = -1;
            int legacyHostIndex = -1;
            do {
                type = parser.next();
                if (type == 2) {
                    String tag = parser.getName();
                    if ("gs".equals(tag)) {
                        String attributeValue = parser.getAttributeValue(null, "version");
                        try {
                            version = Integer.parseInt(attributeValue);
                        } catch (NumberFormatException e) {
                            version = 0;
                        }
                    } else if ("p".equals(tag)) {
                        legacyProviderIndex++;
                        String pkg = parser.getAttributeValue(null, "pkg");
                        String cl = parser.getAttributeValue(null, "cl");
                        String pkg2 = getCanonicalPackageName(pkg, cl, userId);
                        if (pkg2 != null && (uid = getUidForPackage(pkg2, userId)) >= 0 && (providerInfo = getProviderInfo((componentName = new ComponentName(pkg2, cl)), userId)) != null) {
                            ProviderId providerId = new ProviderId(uid, componentName, null);
                            Provider provider = lookupProviderLocked(providerId);
                            if (provider == null && this.mSafeMode) {
                                provider = new Provider(null);
                                provider.info = new AppWidgetProviderInfo();
                                provider.info.provider = providerId.componentName;
                                provider.info.providerInfo = providerInfo;
                                provider.zombie = true;
                                provider.id = providerId;
                                this.mProviders.add(provider);
                            }
                            String tagAttribute = parser.getAttributeValue(null, "tag");
                            int providerTag2 = !TextUtils.isEmpty(tagAttribute) ? Integer.parseInt(tagAttribute, 16) : legacyProviderIndex;
                            provider.tag = providerTag2;
                        }
                    } else if ("h".equals(tag)) {
                        legacyHostIndex++;
                        Host host = new Host(null);
                        String pkg3 = parser.getAttributeValue(null, "pkg");
                        int uid2 = getUidForPackage(pkg3, userId);
                        if (uid2 < 0) {
                            host.zombie = true;
                        }
                        if (!host.zombie || this.mSafeMode) {
                            int hostId = Integer.parseInt(parser.getAttributeValue(null, "id"), 16);
                            String tagAttribute2 = parser.getAttributeValue(null, "tag");
                            int hostTag = !TextUtils.isEmpty(tagAttribute2) ? Integer.parseInt(tagAttribute2, 16) : legacyHostIndex;
                            host.tag = hostTag;
                            host.id = new HostId(uid2, hostId, pkg3);
                            this.mHosts.add(host);
                        }
                    } else if ("b".equals(tag)) {
                        String packageName = parser.getAttributeValue(null, "packageName");
                        if (getUidForPackage(packageName, userId) >= 0) {
                            Pair<Integer, String> packageId = Pair.create(Integer.valueOf(userId), packageName);
                            this.mPackagesWithBindWidgetPermission.add(packageId);
                        }
                    } else if ("g".equals(tag)) {
                        Widget widget = new Widget(null);
                        widget.appWidgetId = Integer.parseInt(parser.getAttributeValue(null, "id"), 16);
                        setMinAppWidgetIdLocked(userId, widget.appWidgetId + 1);
                        String restoredIdString = parser.getAttributeValue(null, "rid");
                        widget.restoredId = restoredIdString == null ? 0 : Integer.parseInt(restoredIdString, 16);
                        Bundle options = new Bundle();
                        options.setDefusable(true);
                        String minWidthString = parser.getAttributeValue(null, "min_width");
                        if (minWidthString != null) {
                            options.putInt("appWidgetMinWidth", Integer.parseInt(minWidthString, 16));
                        }
                        String minHeightString = parser.getAttributeValue(null, "min_height");
                        if (minHeightString != null) {
                            options.putInt("appWidgetMinHeight", Integer.parseInt(minHeightString, 16));
                        }
                        String maxWidthString = parser.getAttributeValue(null, "max_width");
                        if (maxWidthString != null) {
                            options.putInt("appWidgetMaxWidth", Integer.parseInt(maxWidthString, 16));
                        }
                        String maxHeightString = parser.getAttributeValue(null, "max_height");
                        if (maxHeightString != null) {
                            options.putInt("appWidgetMaxHeight", Integer.parseInt(maxHeightString, 16));
                        }
                        String categoryString = parser.getAttributeValue(null, "host_category");
                        if (categoryString != null) {
                            options.putInt("appWidgetCategory", Integer.parseInt(categoryString, 16));
                        }
                        widget.options = options;
                        int hostTag2 = Integer.parseInt(parser.getAttributeValue(null, "h"), 16);
                        String providerString = parser.getAttributeValue(null, "p");
                        if (providerString != null) {
                            providerTag = Integer.parseInt(parser.getAttributeValue(null, "p"), 16);
                        } else {
                            providerTag = -1;
                        }
                        LoadedWidgetState loadedWidgets = new LoadedWidgetState(widget, hostTag2, providerTag);
                        outLoadedWidgets.add(loadedWidgets);
                    }
                }
            } while (type != 1);
            return version;
        } catch (IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException e2) {
            Slog.w(TAG, "failed parsing " + e2);
            return -1;
        }
    }

    private void performUpgradeLocked(int fromVersion) {
        int uid;
        if (fromVersion < 1) {
            Slog.v(TAG, "Upgrading widget database from " + fromVersion + " to 1");
        }
        int version = fromVersion;
        if (fromVersion == 0) {
            HostId oldHostId = new HostId(Process.myUid(), KEYGUARD_HOST_ID, OLD_KEYGUARD_HOST_PACKAGE);
            Host host = lookupHostLocked(oldHostId);
            if (host != null && (uid = getUidForPackage(NEW_KEYGUARD_HOST_PACKAGE, 0)) >= 0) {
                host.id = new HostId(uid, KEYGUARD_HOST_ID, NEW_KEYGUARD_HOST_PACKAGE);
            }
            version = 1;
        }
        if (version == 1) {
        } else {
            throw new IllegalStateException("Failed to upgrade widget database");
        }
    }

    private static File getStateFile(int userId) {
        return new File(Environment.getUserSystemDirectory(userId), STATE_FILENAME);
    }

    private static AtomicFile getSavedStateFile(int userId) {
        File dir = Environment.getUserSystemDirectory(userId);
        File settingsFile = getStateFile(userId);
        if (!settingsFile.exists() && userId == 0) {
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File oldFile = new File("/data/system/appwidgets.xml");
            oldFile.renameTo(settingsFile);
        }
        return new AtomicFile(settingsFile);
    }

    void onUserStopped(int userId) {
        synchronized (this.mLock) {
            boolean crossProfileWidgetsChanged = false;
            int widgetCount = this.mWidgets.size();
            for (int i = widgetCount - 1; i >= 0; i--) {
                Widget widget = this.mWidgets.get(i);
                boolean hostInUser = widget.host.getUserId() == userId;
                boolean hasProvider = widget.provider != null;
                boolean providerInUser = hasProvider && widget.provider.getUserId() == userId;
                if (hostInUser && (!hasProvider || providerInUser)) {
                    removeWidgetLocked(widget);
                    widget.host.widgets.remove(widget);
                    widget.host = null;
                    if (hasProvider) {
                        widget.provider.widgets.remove(widget);
                        widget.provider = null;
                    }
                }
            }
            int hostCount = this.mHosts.size();
            for (int i2 = hostCount - 1; i2 >= 0; i2--) {
                Host host = this.mHosts.get(i2);
                if (host.getUserId() == userId) {
                    crossProfileWidgetsChanged |= !host.widgets.isEmpty();
                    deleteHostLocked(host);
                }
            }
            int grantCount = this.mPackagesWithBindWidgetPermission.size();
            for (int i3 = grantCount - 1; i3 >= 0; i3--) {
                Pair<Integer, String> packageId = this.mPackagesWithBindWidgetPermission.valueAt(i3);
                if (((Integer) packageId.first).intValue() == userId) {
                    this.mPackagesWithBindWidgetPermission.removeAt(i3);
                }
            }
            int userIndex = this.mLoadedUserIds.indexOfKey(userId);
            if (userIndex >= 0) {
                this.mLoadedUserIds.removeAt(userIndex);
            }
            int nextIdIndex = this.mNextAppWidgetIds.indexOfKey(userId);
            if (nextIdIndex >= 0) {
                this.mNextAppWidgetIds.removeAt(nextIdIndex);
            }
            if (crossProfileWidgetsChanged) {
                saveGroupStateAsync(userId);
            }
        }
    }

    private boolean updateProvidersForPackageLocked(String packageName, int userId, Set<ProviderId> removedProviders) throws Throwable {
        boolean providersUpdated = false;
        HashSet<ProviderId> keep = new HashSet<>();
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_UPDATE");
        intent.setPackage(packageName);
        List<ResolveInfo> broadcastReceivers = queryIntentReceivers(intent, userId);
        int N = broadcastReceivers == null ? 0 : broadcastReceivers.size();
        for (int i = 0; i < N; i++) {
            ResolveInfo ri = broadcastReceivers.get(i);
            ActivityInfo ai = ri.activityInfo;
            if ((ai.applicationInfo.flags & PackageManagerService.DumpState.DUMP_DOMAIN_PREFERRED) == 0 && packageName.equals(ai.packageName)) {
                ProviderId providerId = new ProviderId(ai.applicationInfo.uid, new ComponentName(ai.packageName, ai.name), null);
                Provider provider = lookupProviderLocked(providerId);
                if (provider == null) {
                    if (addProviderLocked(ri)) {
                        keep.add(providerId);
                        providersUpdated = true;
                    }
                } else {
                    Provider parsed = parseProviderInfoXml(providerId, ri);
                    if (parsed != null) {
                        keep.add(providerId);
                        provider.info = parsed.info;
                        int M = provider.widgets.size();
                        if (M > 0) {
                            int[] appWidgetIds = getWidgetIds(provider.widgets);
                            cancelBroadcasts(provider);
                            registerForBroadcastsLocked(provider, appWidgetIds);
                            for (int j = 0; j < M; j++) {
                                Widget widget = provider.widgets.get(j);
                                widget.views = null;
                                scheduleNotifyProviderChangedLocked(widget);
                            }
                            sendUpdateIntentLocked(provider, appWidgetIds);
                        }
                    }
                    providersUpdated = true;
                }
            }
        }
        int N2 = this.mProviders.size();
        for (int i2 = N2 - 1; i2 >= 0; i2--) {
            Provider provider2 = this.mProviders.get(i2);
            if (packageName.equals(provider2.info.provider.getPackageName()) && provider2.getUserId() == userId && !keep.contains(provider2.id)) {
                if (removedProviders != null) {
                    removedProviders.add(provider2.id);
                }
                deleteProviderLocked(provider2);
                providersUpdated = true;
            }
        }
        return providersUpdated;
    }

    private void removeWidgetsForPackageLocked(String pkgName, int userId, int parentUserId) {
        int N = this.mProviders.size();
        for (int i = 0; i < N; i++) {
            Provider provider = this.mProviders.get(i);
            if (pkgName.equals(provider.info.provider.getPackageName()) && provider.getUserId() == userId && provider.widgets.size() > 0) {
                deleteWidgetsLocked(provider, parentUserId);
            }
        }
    }

    private boolean removeProvidersForPackageLocked(String pkgName, int userId) {
        boolean removed = false;
        int N = this.mProviders.size();
        for (int i = N - 1; i >= 0; i--) {
            Provider provider = this.mProviders.get(i);
            if (pkgName.equals(provider.info.provider.getPackageName()) && provider.getUserId() == userId) {
                deleteProviderLocked(provider);
                removed = true;
            }
        }
        return removed;
    }

    private boolean removeHostsAndProvidersForPackageLocked(String pkgName, int userId) {
        boolean removed = removeProvidersForPackageLocked(pkgName, userId);
        int N = this.mHosts.size();
        for (int i = N - 1; i >= 0; i--) {
            Host host = this.mHosts.get(i);
            if (pkgName.equals(host.id.packageName) && host.getUserId() == userId) {
                deleteHostLocked(host);
                removed = true;
            }
        }
        return removed;
    }

    private String getCanonicalPackageName(String packageName, String className, int userId) {
        long identity = Binder.clearCallingIdentity();
        try {
            AppGlobals.getPackageManager().getReceiverInfo(new ComponentName(packageName, className), 0, userId);
            return packageName;
        } catch (RemoteException e) {
            String[] packageNames = this.mContext.getPackageManager().currentToCanonicalPackageNames(new String[]{packageName});
            if (packageNames == null || packageNames.length <= 0) {
                return null;
            }
            return packageNames[0];
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void sendBroadcastAsUser(Intent intent, UserHandle userHandle) {
        long identity = Binder.clearCallingIdentity();
        try {
            this.mContext.sendBroadcastAsUser(intent, userHandle);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void bindService(Intent intent, ServiceConnection connection, UserHandle userHandle) {
        long token = Binder.clearCallingIdentity();
        try {
            this.mContext.bindServiceAsUser(intent, connection, 33554433, userHandle);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void unbindService(ServiceConnection connection) {
        long token = Binder.clearCallingIdentity();
        try {
            this.mContext.unbindService(connection);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void onCrossProfileWidgetProvidersChanged(int userId, List<String> packages) {
        int parentId = this.mSecurityPolicy.getProfileParent(userId);
        if (parentId == userId) {
            return;
        }
        synchronized (this.mLock) {
            boolean providersChanged = false;
            ArraySet<String> previousPackages = new ArraySet<>();
            int providerCount = this.mProviders.size();
            for (int i = 0; i < providerCount; i++) {
                Provider provider = this.mProviders.get(i);
                if (provider.getUserId() == userId) {
                    previousPackages.add(provider.id.componentName.getPackageName());
                }
            }
            int packageCount = packages.size();
            for (int i2 = 0; i2 < packageCount; i2++) {
                String packageName = packages.get(i2);
                previousPackages.remove(packageName);
                providersChanged |= updateProvidersForPackageLocked(packageName, userId, null);
            }
            int removedCount = previousPackages.size();
            for (int i3 = 0; i3 < removedCount; i3++) {
                removeWidgetsForPackageLocked(previousPackages.valueAt(i3), userId, parentId);
            }
            if (providersChanged || removedCount > 0) {
                saveGroupStateAsync(userId);
                scheduleNotifyGroupHostsForProvidersChangedLocked(userId);
            }
        }
    }

    public List<ComponentName> getAppWidgetOfHost(String pkg, int uid) {
        if (isRunningBoosterSupport()) {
            int N = this.mHosts.size();
            List<ComponentName> hostProviders = new ArrayList<>();
            Slog.i(TAG, "total hosts " + N);
            int i = 0;
            while (true) {
                if (i >= N) {
                    break;
                }
                Host host = this.mHosts.get(i);
                Slog.i(TAG, "pkg =" + pkg + "host.id.packageName = " + host.id.packageName);
                if (UserHandle.getUserId(host.id.uid) != uid || !pkg.equals(host.id.packageName)) {
                    i++;
                } else {
                    if (DEBUG) {
                        Slog.i(TAG, "host " + host.id + " resolved to uid " + uid);
                    }
                    int M = host.widgets.size();
                    Slog.i(TAG, "total widgets " + M);
                    for (int j = 0; j < M; j++) {
                        if (host.widgets.get(j).provider != null) {
                            Slog.i(TAG, "host.widgets.get(" + j + ").provider.id.componentName");
                            ComponentName componentName = host.widgets.get(j).provider.id.componentName;
                            hostProviders.add(host.widgets.get(j).provider.id.componentName);
                        }
                    }
                }
            }
            return hostProviders;
        }
        return Collections.emptyList();
    }

    private boolean isRunningBoosterSupport() {
        return SystemProperties.get("persist.runningbooster.support").equals("1");
    }

    private boolean isProfileWithLockedParent(int userId) {
        UserInfo parentInfo;
        long token = Binder.clearCallingIdentity();
        try {
            UserInfo userInfo = this.mUserManager.getUserInfo(userId);
            if (userInfo != null && userInfo.isManagedProfile() && (parentInfo = this.mUserManager.getProfileParent(userId)) != null) {
                if (!isUserRunningAndUnlocked(parentInfo.getUserHandle().getIdentifier())) {
                    return true;
                }
            }
            Binder.restoreCallingIdentity(token);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private boolean isProfileWithUnlockedParent(int userId) {
        UserInfo parentInfo;
        UserInfo userInfo = this.mUserManager.getUserInfo(userId);
        if (userInfo != null && userInfo.isManagedProfile() && (parentInfo = this.mUserManager.getProfileParent(userId)) != null && this.mUserManager.isUserUnlockingOrUnlocked(parentInfo.getUserHandle())) {
            return true;
        }
        return false;
    }

    private final class CallbackHandler extends Handler {
        public static final int MSG_NOTIFY_PROVIDERS_CHANGED = 3;
        public static final int MSG_NOTIFY_PROVIDER_CHANGED = 2;
        public static final int MSG_NOTIFY_UPDATE_APP_WIDGET = 1;
        public static final int MSG_NOTIFY_VIEW_DATA_CHANGED = 4;

        public CallbackHandler(Looper looper) {
            super(looper, null, false);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    SomeArgs args = (SomeArgs) message.obj;
                    Host host = (Host) args.arg1;
                    IAppWidgetHost callbacks = (IAppWidgetHost) args.arg2;
                    RemoteViews views = (RemoteViews) args.arg3;
                    long requestTime = ((Long) args.arg4).longValue();
                    int appWidgetId = args.argi1;
                    args.recycle();
                    AppWidgetServiceImpl.this.handleNotifyUpdateAppWidget(host, callbacks, appWidgetId, views, requestTime);
                    break;
                case 2:
                    SomeArgs args2 = (SomeArgs) message.obj;
                    Host host2 = (Host) args2.arg1;
                    IAppWidgetHost callbacks2 = (IAppWidgetHost) args2.arg2;
                    AppWidgetProviderInfo info = (AppWidgetProviderInfo) args2.arg3;
                    int appWidgetId2 = args2.argi1;
                    args2.recycle();
                    AppWidgetServiceImpl.this.handleNotifyProviderChanged(host2, callbacks2, appWidgetId2, info);
                    break;
                case 3:
                    SomeArgs args3 = (SomeArgs) message.obj;
                    Host host3 = (Host) args3.arg1;
                    IAppWidgetHost callbacks3 = (IAppWidgetHost) args3.arg2;
                    args3.recycle();
                    AppWidgetServiceImpl.this.handleNotifyProvidersChanged(host3, callbacks3);
                    break;
                case 4:
                    SomeArgs args4 = (SomeArgs) message.obj;
                    Host host4 = (Host) args4.arg1;
                    IAppWidgetHost callbacks4 = (IAppWidgetHost) args4.arg2;
                    int appWidgetId3 = args4.argi1;
                    int viewId = args4.argi2;
                    args4.recycle();
                    AppWidgetServiceImpl.this.handleNotifyAppWidgetViewDataChanged(host4, callbacks4, appWidgetId3, viewId);
                    break;
            }
        }
    }

    private final class SecurityPolicy {
        SecurityPolicy(AppWidgetServiceImpl this$0, SecurityPolicy securityPolicy) {
            this();
        }

        private SecurityPolicy() {
        }

        public boolean isEnabledGroupProfile(int profileId) {
            int parentId = UserHandle.getCallingUserId();
            if (isParentOrProfile(parentId, profileId)) {
                return isProfileEnabled(profileId);
            }
            return false;
        }

        public int[] getEnabledGroupProfileIds(int userId) {
            int parentId = getGroupParent(userId);
            long identity = Binder.clearCallingIdentity();
            try {
                return AppWidgetServiceImpl.this.mUserManager.getEnabledProfileIds(parentId);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void enforceServiceExistsAndRequiresBindRemoteViewsPermission(ComponentName componentName, int userId) {
            long identity = Binder.clearCallingIdentity();
            try {
                ServiceInfo serviceInfo = AppWidgetServiceImpl.this.mPackageManager.getServiceInfo(componentName, 4096, userId);
                if (serviceInfo == null) {
                    throw new SecurityException("Service " + componentName + " not installed for user " + userId);
                }
                if (!"android.permission.BIND_REMOTEVIEWS".equals(serviceInfo.permission)) {
                    throw new SecurityException("Service " + componentName + " in user " + userId + "does not require android.permission.BIND_REMOTEVIEWS");
                }
            } catch (RemoteException e) {
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void enforceModifyAppWidgetBindPermissions(String packageName) {
            AppWidgetServiceImpl.this.mContext.enforceCallingPermission("android.permission.MODIFY_APPWIDGET_BIND_PERMISSIONS", "hasBindAppWidgetPermission packageName=" + packageName);
        }

        public void enforceCallFromPackage(String packageName) {
            AppWidgetServiceImpl.this.mAppOpsManager.checkPackage(Binder.getCallingUid(), packageName);
        }

        public boolean hasCallerBindPermissionOrBindWhiteListedLocked(String packageName) {
            try {
                AppWidgetServiceImpl.this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_APPWIDGET", null);
                return true;
            } catch (SecurityException e) {
                if (!isCallerBindAppWidgetWhiteListedLocked(packageName)) {
                    return false;
                }
                return true;
            }
        }

        private boolean isCallerBindAppWidgetWhiteListedLocked(String packageName) {
            int userId = UserHandle.getCallingUserId();
            int packageUid = AppWidgetServiceImpl.this.getUidForPackage(packageName, userId);
            if (packageUid < 0) {
                throw new IllegalArgumentException("No package " + packageName + " for user " + userId);
            }
            synchronized (AppWidgetServiceImpl.this.mLock) {
                AppWidgetServiceImpl.this.ensureGroupStateLoadedLocked(userId);
                Pair<Integer, String> packageId = Pair.create(Integer.valueOf(userId), packageName);
                return AppWidgetServiceImpl.this.mPackagesWithBindWidgetPermission.contains(packageId);
            }
        }

        public boolean canAccessAppWidget(Widget widget, int uid, String packageName) {
            if (isHostInPackageForUid(widget.host, uid, packageName) || isProviderInPackageForUid(widget.provider, uid, packageName) || isHostAccessingProvider(widget.host, widget.provider, uid, packageName)) {
                return true;
            }
            int userId = UserHandle.getUserId(uid);
            return (widget.host.getUserId() == userId || (widget.provider != null && widget.provider.getUserId() == userId)) && AppWidgetServiceImpl.this.mContext.checkCallingPermission("android.permission.BIND_APPWIDGET") == 0;
        }

        private boolean isParentOrProfile(int parentId, int profileId) {
            return parentId == profileId || getProfileParent(profileId) == parentId;
        }

        public boolean isProviderInCallerOrInProfileAndWhitelListed(String packageName, int profileId) {
            int callerId = UserHandle.getCallingUserId();
            if (profileId == callerId) {
                return true;
            }
            int parentId = getProfileParent(profileId);
            if (parentId != callerId) {
                return false;
            }
            return isProviderWhiteListed(packageName, profileId);
        }

        public boolean isProviderWhiteListed(String packageName, int profileId) {
            if (AppWidgetServiceImpl.this.mDevicePolicyManagerInternal == null) {
                return false;
            }
            List<String> crossProfilePackages = AppWidgetServiceImpl.this.mDevicePolicyManagerInternal.getCrossProfileWidgetProviders(profileId);
            return crossProfilePackages.contains(packageName);
        }

        public int getProfileParent(int profileId) {
            long identity = Binder.clearCallingIdentity();
            try {
                UserInfo parent = AppWidgetServiceImpl.this.mUserManager.getProfileParent(profileId);
                if (parent != null) {
                    return parent.getUserHandle().getIdentifier();
                }
                Binder.restoreCallingIdentity(identity);
                return AppWidgetServiceImpl.UNKNOWN_USER_ID;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public int getGroupParent(int profileId) {
            int parentId = AppWidgetServiceImpl.this.mSecurityPolicy.getProfileParent(profileId);
            return parentId != AppWidgetServiceImpl.UNKNOWN_USER_ID ? parentId : profileId;
        }

        public boolean isHostInPackageForUid(Host host, int uid, String packageName) {
            if (host.id.uid == uid) {
                return host.id.packageName.equals(packageName);
            }
            return false;
        }

        public boolean isProviderInPackageForUid(Provider provider, int uid, String packageName) {
            if (provider == null || provider.id.uid != uid) {
                return false;
            }
            return provider.id.componentName.getPackageName().equals(packageName);
        }

        public boolean isHostAccessingProvider(Host host, Provider provider, int uid, String packageName) {
            if (host.id.uid != uid || provider == null) {
                return false;
            }
            return provider.id.componentName.getPackageName().equals(packageName);
        }

        private boolean isProfileEnabled(int profileId) {
            long identity = Binder.clearCallingIdentity();
            try {
                UserInfo userInfo = AppWidgetServiceImpl.this.mUserManager.getUserInfo(profileId);
                if (userInfo != null) {
                    if (userInfo.isEnabled()) {
                        Binder.restoreCallingIdentity(identity);
                        return true;
                    }
                }
                return false;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    private static final class Provider {
        PendingIntent broadcast;
        ProviderId id;
        AppWidgetProviderInfo info;
        boolean maskedByLockedProfile;
        boolean maskedByQuietProfile;
        boolean maskedBySuspendedPackage;
        int tag;
        ArrayList<Widget> widgets;
        boolean zombie;

        Provider(Provider provider) {
            this();
        }

        private Provider() {
            this.widgets = new ArrayList<>();
            this.tag = -1;
        }

        public int getUserId() {
            return UserHandle.getUserId(this.id.uid);
        }

        public boolean isInPackageForUser(String packageName, int userId) {
            if (getUserId() == userId) {
                return this.id.componentName.getPackageName().equals(packageName);
            }
            return false;
        }

        public boolean hostedByPackageForUser(String packageName, int userId) {
            int N = this.widgets.size();
            for (int i = 0; i < N; i++) {
                Widget widget = this.widgets.get(i);
                if (packageName.equals(widget.host.id.packageName) && widget.host.getUserId() == userId) {
                    return true;
                }
            }
            return false;
        }

        public String toString() {
            return "Provider{" + this.id + (this.zombie ? " Z" : "") + '}';
        }

        public boolean setMaskedByQuietProfileLocked(boolean masked) {
            boolean oldState = this.maskedByQuietProfile;
            this.maskedByQuietProfile = masked;
            return masked != oldState;
        }

        public boolean setMaskedByLockedProfileLocked(boolean masked) {
            boolean oldState = this.maskedByLockedProfile;
            this.maskedByLockedProfile = masked;
            return masked != oldState;
        }

        public boolean setMaskedBySuspendedPackageLocked(boolean masked) {
            boolean oldState = this.maskedBySuspendedPackage;
            this.maskedBySuspendedPackage = masked;
            return masked != oldState;
        }

        public boolean isMaskedLocked() {
            if (this.maskedByQuietProfile || this.maskedByLockedProfile) {
                return true;
            }
            return this.maskedBySuspendedPackage;
        }
    }

    private static final class ProviderId {
        final ComponentName componentName;
        final int uid;

        ProviderId(int uid, ComponentName componentName, ProviderId providerId) {
            this(uid, componentName);
        }

        private ProviderId(int uid, ComponentName componentName) {
            this.uid = uid;
            this.componentName = componentName;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            ProviderId other = (ProviderId) obj;
            if (this.uid != other.uid) {
                return false;
            }
            if (this.componentName == null) {
                if (other.componentName != null) {
                    return false;
                }
            } else if (!this.componentName.equals(other.componentName)) {
                return false;
            }
            return true;
        }

        public int hashCode() {
            int result = this.uid;
            return (result * 31) + (this.componentName != null ? this.componentName.hashCode() : 0);
        }

        public String toString() {
            return "ProviderId{user:" + UserHandle.getUserId(this.uid) + ", app:" + UserHandle.getAppId(this.uid) + ", cmp:" + this.componentName + '}';
        }
    }

    private static final class Host {
        IAppWidgetHost callbacks;
        HostId id;
        long lastWidgetUpdateTime;
        int tag;
        ArrayList<Widget> widgets;
        boolean zombie;

        Host(Host host) {
            this();
        }

        private Host() {
            this.widgets = new ArrayList<>();
            this.tag = -1;
        }

        public int getUserId() {
            return UserHandle.getUserId(this.id.uid);
        }

        public boolean isInPackageForUser(String packageName, int userId) {
            if (getUserId() == userId) {
                return this.id.packageName.equals(packageName);
            }
            return false;
        }

        private boolean hostsPackageForUser(String pkg, int userId) {
            int N = this.widgets.size();
            for (int i = 0; i < N; i++) {
                Provider provider = this.widgets.get(i).provider;
                if (provider != null && provider.getUserId() == userId && provider.info != null && pkg.equals(provider.info.provider.getPackageName())) {
                    return true;
                }
            }
            return false;
        }

        public RemoteViews getPendingViewsForId(int appWidgetId) {
            long updateTime = this.lastWidgetUpdateTime;
            int N = this.widgets.size();
            for (int i = 0; i < N; i++) {
                Widget widget = this.widgets.get(i);
                if (widget.appWidgetId == appWidgetId && widget.lastUpdateTime > updateTime) {
                    return AppWidgetServiceImpl.cloneIfLocalBinder(widget.getEffectiveViewsLocked());
                }
            }
            return null;
        }

        public String toString() {
            return "Host{" + this.id + (this.zombie ? " Z" : "") + '}';
        }
    }

    private static final class HostId {
        final int hostId;
        final String packageName;
        final int uid;

        public HostId(int uid, int hostId, String packageName) {
            this.uid = uid;
            this.hostId = hostId;
            this.packageName = packageName;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            HostId other = (HostId) obj;
            if (this.uid != other.uid || this.hostId != other.hostId) {
                return false;
            }
            if (this.packageName == null) {
                if (other.packageName != null) {
                    return false;
                }
            } else if (!this.packageName.equals(other.packageName)) {
                return false;
            }
            return true;
        }

        public int hashCode() {
            int result = this.uid;
            return (((result * 31) + this.hostId) * 31) + (this.packageName != null ? this.packageName.hashCode() : 0);
        }

        public String toString() {
            return "HostId{user:" + UserHandle.getUserId(this.uid) + ", app:" + UserHandle.getAppId(this.uid) + ", hostId:" + this.hostId + ", pkg:" + this.packageName + '}';
        }
    }

    private static final class Widget {
        int appWidgetId;
        Host host;
        long lastUpdateTime;
        RemoteViews maskedViews;
        Bundle options;
        Provider provider;
        int restoredId;
        RemoteViews views;

        Widget(Widget widget) {
            this();
        }

        private Widget() {
        }

        public String toString() {
            return "AppWidgetId{" + this.appWidgetId + ':' + this.host + ':' + this.provider + '}';
        }

        private boolean replaceWithMaskedViewsLocked(RemoteViews views) {
            this.maskedViews = views;
            return true;
        }

        private boolean clearMaskedViewsLocked() {
            if (this.maskedViews != null) {
                this.maskedViews = null;
                return true;
            }
            return false;
        }

        public RemoteViews getEffectiveViewsLocked() {
            return this.maskedViews != null ? this.maskedViews : this.views;
        }
    }

    private static final class ServiceConnectionProxy implements ServiceConnection {
        private final IRemoteViewsAdapterConnection mConnectionCb;

        ServiceConnectionProxy(IBinder connectionCb) {
            this.mConnectionCb = IRemoteViewsAdapterConnection.Stub.asInterface(connectionCb);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                this.mConnectionCb.onServiceConnected(service);
            } catch (RemoteException re) {
                Slog.e(AppWidgetServiceImpl.TAG, "Error passing service interface", re);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            disconnect();
        }

        public void disconnect() {
            if (AppWidgetServiceImpl.DEBUG) {
                Slog.i(AppWidgetServiceImpl.TAG, "disconnect");
            }
            try {
                this.mConnectionCb.onServiceDisconnected();
            } catch (RemoteException re) {
                Slog.e(AppWidgetServiceImpl.TAG, "Error clearing service interface", re);
            }
        }
    }

    private class LoadedWidgetState {
        final int hostTag;
        final int providerTag;
        final Widget widget;

        public LoadedWidgetState(Widget widget, int hostTag, int providerTag) {
            this.widget = widget;
            this.hostTag = hostTag;
            this.providerTag = providerTag;
        }
    }

    private final class SaveStateRunnable implements Runnable {
        final int mUserId;

        public SaveStateRunnable(int userId) {
            this.mUserId = userId;
        }

        @Override
        public void run() {
            synchronized (AppWidgetServiceImpl.this.mLock) {
                AppWidgetServiceImpl.this.ensureGroupStateLoadedLocked(this.mUserId, false);
                AppWidgetServiceImpl.this.saveStateLocked(this.mUserId);
            }
        }
    }

    private final class BackupRestoreController {
        private static final boolean DEBUG = true;
        private static final String TAG = "BackupRestoreController";
        private static final int WIDGET_STATE_VERSION = 2;
        private final HashSet<String> mPrunedApps;
        private final HashMap<Host, ArrayList<RestoreUpdateRecord>> mUpdatesByHost;
        private final HashMap<Provider, ArrayList<RestoreUpdateRecord>> mUpdatesByProvider;

        BackupRestoreController(AppWidgetServiceImpl this$0, BackupRestoreController backupRestoreController) {
            this();
        }

        private BackupRestoreController() {
            this.mPrunedApps = new HashSet<>();
            this.mUpdatesByProvider = new HashMap<>();
            this.mUpdatesByHost = new HashMap<>();
        }

        public List<String> getWidgetParticipants(int userId) {
            Slog.i(TAG, "Getting widget participants for user: " + userId);
            HashSet<String> packages = new HashSet<>();
            synchronized (AppWidgetServiceImpl.this.mLock) {
                int N = AppWidgetServiceImpl.this.mWidgets.size();
                for (int i = 0; i < N; i++) {
                    Widget widget = (Widget) AppWidgetServiceImpl.this.mWidgets.get(i);
                    if (isProviderAndHostInUser(widget, userId)) {
                        packages.add(widget.host.id.packageName);
                        Provider provider = widget.provider;
                        if (provider != null) {
                            packages.add(provider.id.componentName.getPackageName());
                        }
                    }
                }
            }
            return new ArrayList(packages);
        }

        public byte[] getWidgetState(String backedupPackage, int userId) {
            Slog.i(TAG, "Getting widget state for user: " + userId);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            synchronized (AppWidgetServiceImpl.this.mLock) {
                if (!packageNeedsWidgetBackupLocked(backedupPackage, userId)) {
                    return null;
                }
                try {
                    FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                    fastXmlSerializer.setOutput(stream, StandardCharsets.UTF_8.name());
                    fastXmlSerializer.startDocument(null, true);
                    fastXmlSerializer.startTag(null, "ws");
                    fastXmlSerializer.attribute(null, "version", String.valueOf(2));
                    fastXmlSerializer.attribute(null, "pkg", backedupPackage);
                    int index = 0;
                    int N = AppWidgetServiceImpl.this.mProviders.size();
                    for (int i = 0; i < N; i++) {
                        Provider provider = (Provider) AppWidgetServiceImpl.this.mProviders.get(i);
                        if (!provider.widgets.isEmpty() && (provider.isInPackageForUser(backedupPackage, userId) || provider.hostedByPackageForUser(backedupPackage, userId))) {
                            provider.tag = index;
                            AppWidgetServiceImpl.serializeProvider(fastXmlSerializer, provider);
                            index++;
                        }
                    }
                    int N2 = AppWidgetServiceImpl.this.mHosts.size();
                    int index2 = 0;
                    for (int i2 = 0; i2 < N2; i2++) {
                        Host host = (Host) AppWidgetServiceImpl.this.mHosts.get(i2);
                        if (!host.widgets.isEmpty() && (host.isInPackageForUser(backedupPackage, userId) || host.hostsPackageForUser(backedupPackage, userId))) {
                            host.tag = index2;
                            AppWidgetServiceImpl.serializeHost(fastXmlSerializer, host);
                            index2++;
                        }
                    }
                    int N3 = AppWidgetServiceImpl.this.mWidgets.size();
                    for (int i3 = 0; i3 < N3; i3++) {
                        Widget widget = (Widget) AppWidgetServiceImpl.this.mWidgets.get(i3);
                        Provider provider2 = widget.provider;
                        if (widget.host.isInPackageForUser(backedupPackage, userId) || (provider2 != null && provider2.isInPackageForUser(backedupPackage, userId))) {
                            AppWidgetServiceImpl.serializeAppWidget(fastXmlSerializer, widget);
                        }
                    }
                    fastXmlSerializer.endTag(null, "ws");
                    fastXmlSerializer.endDocument();
                    return stream.toByteArray();
                } catch (IOException e) {
                    Slog.w(TAG, "Unable to save widget state for " + backedupPackage);
                    return null;
                }
            }
        }

        public void restoreStarting(int userId) {
            Slog.i(TAG, "Restore starting for user: " + userId);
            synchronized (AppWidgetServiceImpl.this.mLock) {
                this.mPrunedApps.clear();
                this.mUpdatesByProvider.clear();
                this.mUpdatesByHost.clear();
            }
        }

        public void restoreWidgetState(String packageName, byte[] restoredState, int userId) {
            int type;
            Slog.i(TAG, "Restoring widget state for user:" + userId + " package: " + packageName);
            ByteArrayInputStream stream = new ByteArrayInputStream(restoredState);
            try {
                ArrayList<Provider> restoredProviders = new ArrayList<>();
                ArrayList<Host> restoredHosts = new ArrayList<>();
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(stream, StandardCharsets.UTF_8.name());
                synchronized (AppWidgetServiceImpl.this.mLock) {
                    do {
                        type = parser.next();
                        if (type == 2) {
                            String tag = parser.getName();
                            if ("ws".equals(tag)) {
                                String version = parser.getAttributeValue(null, "version");
                                int versionNumber = Integer.parseInt(version);
                                if (versionNumber > 2) {
                                    Slog.w(TAG, "Unable to process state version " + version);
                                    return;
                                } else if (!packageName.equals(parser.getAttributeValue(null, "pkg"))) {
                                    Slog.w(TAG, "Package mismatch in ws");
                                    return;
                                }
                            } else if ("p".equals(tag)) {
                                String pkg = parser.getAttributeValue(null, "pkg");
                                String cl = parser.getAttributeValue(null, "cl");
                                ComponentName componentName = new ComponentName(pkg, cl);
                                Provider p = findProviderLocked(componentName, userId);
                                if (p == null) {
                                    p = new Provider(null);
                                    p.id = new ProviderId(-1, componentName, null);
                                    p.info = new AppWidgetProviderInfo();
                                    p.info.provider = componentName;
                                    p.zombie = true;
                                    AppWidgetServiceImpl.this.mProviders.add(p);
                                }
                                Slog.i(TAG, "   provider " + p.id);
                                restoredProviders.add(p);
                            } else if ("h".equals(tag)) {
                                String pkg2 = parser.getAttributeValue(null, "pkg");
                                int uid = AppWidgetServiceImpl.this.getUidForPackage(pkg2, userId);
                                int hostId = Integer.parseInt(parser.getAttributeValue(null, "id"), 16);
                                Host h = AppWidgetServiceImpl.this.lookupOrAddHostLocked(new HostId(uid, hostId, pkg2));
                                restoredHosts.add(h);
                                Slog.i(TAG, "   host[" + restoredHosts.size() + "]: {" + h.id + "}");
                            } else if ("g".equals(tag)) {
                                int restoredId = Integer.parseInt(parser.getAttributeValue(null, "id"), 16);
                                int hostIndex = Integer.parseInt(parser.getAttributeValue(null, "h"), 16);
                                Host host = restoredHosts.get(hostIndex);
                                Provider p2 = null;
                                String prov = parser.getAttributeValue(null, "p");
                                if (prov != null) {
                                    int which = Integer.parseInt(prov, 16);
                                    Provider p3 = restoredProviders.get(which);
                                    p2 = p3;
                                }
                                pruneWidgetStateLocked(host.id.packageName, userId);
                                if (p2 != null) {
                                    pruneWidgetStateLocked(p2.id.componentName.getPackageName(), userId);
                                }
                                Widget id = findRestoredWidgetLocked(restoredId, host, p2);
                                if (id == null) {
                                    id = new Widget(null);
                                    id.appWidgetId = AppWidgetServiceImpl.this.incrementAndGetAppWidgetIdLocked(userId);
                                    id.restoredId = restoredId;
                                    id.options = parseWidgetIdOptions(parser);
                                    id.host = host;
                                    id.host.widgets.add(id);
                                    id.provider = p2;
                                    if (id.provider != null) {
                                        id.provider.widgets.add(id);
                                    }
                                    Slog.i(TAG, "New restored id " + restoredId + " now " + id);
                                    AppWidgetServiceImpl.this.addWidgetLocked(id);
                                }
                                if (id.provider.info != null) {
                                    stashProviderRestoreUpdateLocked(id.provider, restoredId, id.appWidgetId);
                                } else {
                                    Slog.w(TAG, "Missing provider for restored widget " + id);
                                }
                                stashHostRestoreUpdateLocked(id.host, restoredId, id.appWidgetId);
                                Slog.i(TAG, "   instance: " + restoredId + " -> " + id.appWidgetId + " :: p=" + id.provider);
                            }
                        }
                    } while (type != 1);
                }
            } catch (IOException | XmlPullParserException e) {
                Slog.w(TAG, "Unable to restore widget state for " + packageName);
            } finally {
                AppWidgetServiceImpl.this.saveGroupStateAsync(userId);
            }
        }

        public void restoreFinished(int userId) {
            Slog.i(TAG, "restoreFinished for " + userId);
            UserHandle userHandle = new UserHandle(userId);
            synchronized (AppWidgetServiceImpl.this.mLock) {
                Set<Map.Entry<Provider, ArrayList<RestoreUpdateRecord>>> providerEntries = this.mUpdatesByProvider.entrySet();
                for (Map.Entry<Provider, ArrayList<RestoreUpdateRecord>> e : providerEntries) {
                    Provider provider = e.getKey();
                    ArrayList<RestoreUpdateRecord> updates = e.getValue();
                    int pending = countPendingUpdates(updates);
                    Slog.i(TAG, "Provider " + provider + " pending: " + pending);
                    if (pending > 0) {
                        int[] oldIds = new int[pending];
                        int[] newIds = new int[pending];
                        int N = updates.size();
                        int nextPending = 0;
                        for (int i = 0; i < N; i++) {
                            RestoreUpdateRecord r = updates.get(i);
                            if (!r.notified) {
                                r.notified = true;
                                oldIds[nextPending] = r.oldId;
                                newIds[nextPending] = r.newId;
                                nextPending++;
                                Slog.i(TAG, "   " + r.oldId + " => " + r.newId);
                            }
                        }
                        sendWidgetRestoreBroadcastLocked("android.appwidget.action.APPWIDGET_RESTORED", provider, null, oldIds, newIds, userHandle);
                    }
                }
                Set<Map.Entry<Host, ArrayList<RestoreUpdateRecord>>> hostEntries = this.mUpdatesByHost.entrySet();
                for (Map.Entry<Host, ArrayList<RestoreUpdateRecord>> e2 : hostEntries) {
                    Host host = e2.getKey();
                    if (host.id.uid != -1) {
                        ArrayList<RestoreUpdateRecord> updates2 = e2.getValue();
                        int pending2 = countPendingUpdates(updates2);
                        Slog.i(TAG, "Host " + host + " pending: " + pending2);
                        if (pending2 > 0) {
                            int[] oldIds2 = new int[pending2];
                            int[] newIds2 = new int[pending2];
                            int N2 = updates2.size();
                            int nextPending2 = 0;
                            for (int i2 = 0; i2 < N2; i2++) {
                                RestoreUpdateRecord r2 = updates2.get(i2);
                                if (!r2.notified) {
                                    r2.notified = true;
                                    oldIds2[nextPending2] = r2.oldId;
                                    newIds2[nextPending2] = r2.newId;
                                    nextPending2++;
                                    Slog.i(TAG, "   " + r2.oldId + " => " + r2.newId);
                                }
                            }
                            sendWidgetRestoreBroadcastLocked("android.appwidget.action.APPWIDGET_HOST_RESTORED", null, host, oldIds2, newIds2, userHandle);
                        }
                    }
                }
            }
        }

        private Provider findProviderLocked(ComponentName componentName, int userId) {
            int providerCount = AppWidgetServiceImpl.this.mProviders.size();
            for (int i = 0; i < providerCount; i++) {
                Provider provider = (Provider) AppWidgetServiceImpl.this.mProviders.get(i);
                if (provider.getUserId() == userId && provider.id.componentName.equals(componentName)) {
                    return provider;
                }
            }
            return null;
        }

        private Widget findRestoredWidgetLocked(int restoredId, Host host, Provider p) {
            Slog.i(TAG, "Find restored widget: id=" + restoredId + " host=" + host + " provider=" + p);
            if (p == null || host == null) {
                return null;
            }
            int N = AppWidgetServiceImpl.this.mWidgets.size();
            for (int i = 0; i < N; i++) {
                Widget widget = (Widget) AppWidgetServiceImpl.this.mWidgets.get(i);
                if (widget.restoredId == restoredId && widget.host.id.equals(host.id) && widget.provider.id.equals(p.id)) {
                    Slog.i(TAG, "   Found at " + i + " : " + widget);
                    return widget;
                }
            }
            return null;
        }

        private boolean packageNeedsWidgetBackupLocked(String packageName, int userId) {
            int N = AppWidgetServiceImpl.this.mWidgets.size();
            for (int i = 0; i < N; i++) {
                Widget widget = (Widget) AppWidgetServiceImpl.this.mWidgets.get(i);
                if (isProviderAndHostInUser(widget, userId)) {
                    if (widget.host.isInPackageForUser(packageName, userId)) {
                        return true;
                    }
                    Provider provider = widget.provider;
                    if (provider != null && provider.isInPackageForUser(packageName, userId)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private void stashProviderRestoreUpdateLocked(Provider provider, int oldId, int newId) {
            ArrayList<RestoreUpdateRecord> r = this.mUpdatesByProvider.get(provider);
            if (r == null) {
                r = new ArrayList<>();
                this.mUpdatesByProvider.put(provider, r);
            } else if (alreadyStashed(r, oldId, newId)) {
                Slog.i(TAG, "ID remap " + oldId + " -> " + newId + " already stashed for " + provider);
                return;
            }
            r.add(new RestoreUpdateRecord(oldId, newId));
        }

        private boolean alreadyStashed(ArrayList<RestoreUpdateRecord> stash, int oldId, int newId) {
            int N = stash.size();
            for (int i = 0; i < N; i++) {
                RestoreUpdateRecord r = stash.get(i);
                if (r.oldId == oldId && r.newId == newId) {
                    return true;
                }
            }
            return false;
        }

        private void stashHostRestoreUpdateLocked(Host host, int oldId, int newId) {
            ArrayList<RestoreUpdateRecord> r = this.mUpdatesByHost.get(host);
            if (r == null) {
                r = new ArrayList<>();
                this.mUpdatesByHost.put(host, r);
            } else if (alreadyStashed(r, oldId, newId)) {
                Slog.i(TAG, "ID remap " + oldId + " -> " + newId + " already stashed for " + host);
                return;
            }
            r.add(new RestoreUpdateRecord(oldId, newId));
        }

        private void sendWidgetRestoreBroadcastLocked(String action, Provider provider, Host host, int[] oldIds, int[] newIds, UserHandle userHandle) {
            Intent intent = new Intent(action);
            intent.putExtra("appWidgetOldIds", oldIds);
            intent.putExtra("appWidgetIds", newIds);
            if (provider != null) {
                intent.setComponent(provider.info.provider);
                AppWidgetServiceImpl.this.sendBroadcastAsUser(intent, userHandle);
            }
            if (host == null) {
                return;
            }
            intent.setComponent(null);
            intent.setPackage(host.id.packageName);
            intent.putExtra("hostId", host.id.hostId);
            AppWidgetServiceImpl.this.sendBroadcastAsUser(intent, userHandle);
        }

        private void pruneWidgetStateLocked(String pkg, int userId) {
            if (!this.mPrunedApps.contains(pkg)) {
                Slog.i(TAG, "pruning widget state for restoring package " + pkg);
                for (int i = AppWidgetServiceImpl.this.mWidgets.size() - 1; i >= 0; i--) {
                    Widget widget = (Widget) AppWidgetServiceImpl.this.mWidgets.get(i);
                    Host host = widget.host;
                    Provider provider = widget.provider;
                    if (host.hostsPackageForUser(pkg, userId) || (provider != null && provider.isInPackageForUser(pkg, userId))) {
                        host.widgets.remove(widget);
                        provider.widgets.remove(widget);
                        AppWidgetServiceImpl.this.unbindAppWidgetRemoteViewsServicesLocked(widget);
                        AppWidgetServiceImpl.this.removeWidgetLocked(widget);
                    }
                }
                this.mPrunedApps.add(pkg);
                return;
            }
            Slog.i(TAG, "already pruned " + pkg + ", continuing normally");
        }

        private boolean isProviderAndHostInUser(Widget widget, int userId) {
            if (widget.host.getUserId() == userId) {
                return widget.provider == null || widget.provider.getUserId() == userId;
            }
            return false;
        }

        private Bundle parseWidgetIdOptions(XmlPullParser parser) {
            Bundle options = new Bundle();
            options.setDefusable(true);
            String minWidthString = parser.getAttributeValue(null, "min_width");
            if (minWidthString != null) {
                options.putInt("appWidgetMinWidth", Integer.parseInt(minWidthString, 16));
            }
            String minHeightString = parser.getAttributeValue(null, "min_height");
            if (minHeightString != null) {
                options.putInt("appWidgetMinHeight", Integer.parseInt(minHeightString, 16));
            }
            String maxWidthString = parser.getAttributeValue(null, "max_width");
            if (maxWidthString != null) {
                options.putInt("appWidgetMaxWidth", Integer.parseInt(maxWidthString, 16));
            }
            String maxHeightString = parser.getAttributeValue(null, "max_height");
            if (maxHeightString != null) {
                options.putInt("appWidgetMaxHeight", Integer.parseInt(maxHeightString, 16));
            }
            String categoryString = parser.getAttributeValue(null, "host_category");
            if (categoryString != null) {
                options.putInt("appWidgetCategory", Integer.parseInt(categoryString, 16));
            }
            return options;
        }

        private int countPendingUpdates(ArrayList<RestoreUpdateRecord> updates) {
            int pending = 0;
            int N = updates.size();
            for (int i = 0; i < N; i++) {
                RestoreUpdateRecord r = updates.get(i);
                if (!r.notified) {
                    pending++;
                }
            }
            return pending;
        }

        private class RestoreUpdateRecord {
            public int newId;
            public boolean notified = false;
            public int oldId;

            public RestoreUpdateRecord(int theOldId, int theNewId) {
                this.oldId = theOldId;
                this.newId = theNewId;
            }
        }
    }

    private class AppWidgetManagerLocal extends AppWidgetManagerInternal {
        AppWidgetManagerLocal(AppWidgetServiceImpl this$0, AppWidgetManagerLocal appWidgetManagerLocal) {
            this();
        }

        private AppWidgetManagerLocal() {
        }

        public ArraySet<String> getHostedWidgetPackages(int uid) throws Throwable {
            ArraySet<String> widgetPackages;
            synchronized (AppWidgetServiceImpl.this.mLock) {
                try {
                    int widgetCount = AppWidgetServiceImpl.this.mWidgets.size();
                    int i = 0;
                    ArraySet<String> widgetPackages2 = null;
                    while (i < widgetCount) {
                        try {
                            Widget widget = (Widget) AppWidgetServiceImpl.this.mWidgets.get(i);
                            if (widget.host.id.uid == uid) {
                                widgetPackages = widgetPackages2 == null ? new ArraySet<>() : widgetPackages2;
                                widgetPackages.add(widget.provider.id.componentName.getPackageName());
                            } else {
                                widgetPackages = widgetPackages2;
                            }
                            i++;
                            widgetPackages2 = widgetPackages;
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    }
                    return widgetPackages2;
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        }
    }
}
