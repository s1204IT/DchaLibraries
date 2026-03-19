package com.android.server.appwidget;

import android.R;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.IApplicationThread;
import android.app.IServiceConnection;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManagerInternal;
import android.appwidget.AppWidgetManagerInternal;
import android.appwidget.AppWidgetProviderInfo;
import android.appwidget.PendingHostUpdate;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.ShortcutServiceInternal;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.BenesseExtension;
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
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.AttributeSet;
import android.util.LongSparseArray;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;
import android.util.TypedValue;
import android.util.Xml;
import android.util.proto.ProtoOutputStream;
import android.view.Display;
import android.view.WindowManager;
import android.widget.RemoteViews;
import com.android.internal.app.SuspendedAppActivity;
import com.android.internal.app.UnlaunchableAppActivity;
import com.android.internal.appwidget.IAppWidgetHost;
import com.android.internal.appwidget.IAppWidgetService;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.widget.IRemoteViewsFactory;
import com.android.server.LocalServices;
import com.android.server.WidgetBackupProvider;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.backup.BackupManagerService;
import com.android.server.pm.DumpState;
import com.android.server.pm.Settings;
import com.android.server.policy.IconUtilities;
import com.android.server.utils.PriorityDump;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class AppWidgetServiceImpl extends IAppWidgetService.Stub implements WidgetBackupProvider, DevicePolicyManagerInternal.OnCrossProfileWidgetProvidersChangeListener {
    private static final int CURRENT_VERSION = 1;
    private static boolean DEBUG = false;
    private static final int ID_PROVIDER_CHANGED = 1;
    private static final int ID_VIEWS_UPDATE = 0;
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
    private static final AtomicLong UPDATE_COUNTER;
    private AlarmManager mAlarmManager;
    private AppOpsManager mAppOpsManager;
    private BackupRestoreController mBackupRestoreController;
    private Handler mCallbackHandler;
    private final Context mContext;
    private DevicePolicyManagerInternal mDevicePolicyManagerInternal;
    private IconUtilities mIconUtilities;
    private KeyguardManager mKeyguardManager;
    private Locale mLocale;
    private int mMaxWidgetBitmapMemory;
    private IPackageManager mPackageManager;
    private PackageManagerInternal mPackageManagerInternal;
    private boolean mSafeMode;
    private Handler mSaveStateHandler;
    private SecurityPolicy mSecurityPolicy;
    private UserManager mUserManager;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int intExtra;
            String action = intent.getAction();
            intExtra = intent.getIntExtra("android.intent.extra.user_handle", -10000);
            if (AppWidgetServiceImpl.DEBUG) {
                Slog.i(AppWidgetServiceImpl.TAG, "Received broadcast: " + action + " on user " + intExtra);
            }
            switch (action) {
                case "android.intent.action.CONFIGURATION_CHANGED":
                    AppWidgetServiceImpl.this.onConfigurationChanged();
                    return;
                case "android.intent.action.MANAGED_PROFILE_AVAILABLE":
                case "android.intent.action.MANAGED_PROFILE_UNAVAILABLE":
                    synchronized (AppWidgetServiceImpl.this.mLock) {
                        AppWidgetServiceImpl.this.reloadWidgetsMaskedState(intExtra);
                        break;
                    }
                    return;
                case "android.intent.action.PACKAGES_SUSPENDED":
                    AppWidgetServiceImpl.this.onPackageBroadcastReceived(intent, getSendingUserId());
                    AppWidgetServiceImpl.this.updateWidgetPackageSuspensionMaskedState(intent, true, getSendingUserId());
                    return;
                case "android.intent.action.PACKAGES_UNSUSPENDED":
                    AppWidgetServiceImpl.this.onPackageBroadcastReceived(intent, getSendingUserId());
                    AppWidgetServiceImpl.this.updateWidgetPackageSuspensionMaskedState(intent, false, getSendingUserId());
                    return;
                default:
                    AppWidgetServiceImpl.this.onPackageBroadcastReceived(intent, getSendingUserId());
                    return;
            }
        }
    };
    private final HashMap<Pair<Integer, Intent.FilterComparison>, HashSet<Integer>> mRemoteViewsServicesAppWidgets = new HashMap<>();
    private final Object mLock = new Object();
    private final ArrayList<Widget> mWidgets = new ArrayList<>();
    private final ArrayList<Host> mHosts = new ArrayList<>();
    private final ArrayList<Provider> mProviders = new ArrayList<>();
    private final ArraySet<Pair<Integer, String>> mPackagesWithBindWidgetPermission = new ArraySet<>();
    private final SparseIntArray mLoadedUserIds = new SparseIntArray();
    private final SparseArray<ArraySet<String>> mWidgetPackages = new SparseArray<>();
    private final SparseIntArray mNextAppWidgetIds = new SparseIntArray();

    static {
        MIN_UPDATE_PERIOD = DEBUG ? 0 : 1800000;
        UPDATE_COUNTER = new AtomicLong();
    }

    AppWidgetServiceImpl(Context context) {
        this.mContext = context;
    }

    public void onStart() {
        this.mPackageManager = AppGlobals.getPackageManager();
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mAppOpsManager = (AppOpsManager) this.mContext.getSystemService("appops");
        this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        this.mDevicePolicyManagerInternal = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        this.mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        this.mSaveStateHandler = BackgroundThread.getHandler();
        this.mCallbackHandler = new CallbackHandler(this.mContext.getMainLooper());
        this.mBackupRestoreController = new BackupRestoreController();
        this.mSecurityPolicy = new SecurityPolicy();
        this.mIconUtilities = new IconUtilities(this.mContext);
        computeMaximumWidgetBitmapMemory();
        registerBroadcastReceiver();
        registerOnCrossProfileProvidersChangedListener();
        LocalServices.addService(AppWidgetManagerInternal.class, new AppWidgetManagerLocal());
    }

    private void computeMaximumWidgetBitmapMemory() {
        Display defaultDisplay = ((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay();
        Point point = new Point();
        defaultDisplay.getRealSize(point);
        this.mMaxWidgetBitmapMemory = 6 * point.x * point.y;
    }

    private void registerBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, intentFilter, null, null);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter2.addAction("android.intent.action.PACKAGE_CHANGED");
        intentFilter2.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter2.addDataScheme(Settings.ATTR_PACKAGE);
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, intentFilter2, null, null);
        IntentFilter intentFilter3 = new IntentFilter();
        intentFilter3.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
        intentFilter3.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, intentFilter3, null, null);
        IntentFilter intentFilter4 = new IntentFilter();
        intentFilter4.addAction("android.intent.action.MANAGED_PROFILE_AVAILABLE");
        intentFilter4.addAction("android.intent.action.MANAGED_PROFILE_UNAVAILABLE");
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, intentFilter4, null, null);
        IntentFilter intentFilter5 = new IntentFilter();
        intentFilter5.addAction("android.intent.action.PACKAGES_SUSPENDED");
        intentFilter5.addAction("android.intent.action.PACKAGES_UNSUSPENDED");
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, intentFilter5, null, null);
    }

    private void registerOnCrossProfileProvidersChangedListener() {
        if (this.mDevicePolicyManagerInternal != null) {
            this.mDevicePolicyManagerInternal.addOnCrossProfileWidgetProvidersChangeListener(this);
        }
    }

    public void setSafeMode(boolean z) {
        this.mSafeMode = z;
    }

    private void onConfigurationChanged() {
        if (DEBUG) {
            Slog.i(TAG, "onConfigurationChanged()");
        }
        Locale locale = Locale.getDefault();
        if (locale == null || this.mLocale == null || !locale.equals(this.mLocale)) {
            this.mLocale = locale;
            synchronized (this.mLock) {
                SparseIntArray sparseIntArray = null;
                ArrayList arrayList = new ArrayList(this.mProviders);
                HashSet hashSet = new HashSet();
                for (int size = arrayList.size() - 1; size >= 0; size--) {
                    Provider provider = (Provider) arrayList.get(size);
                    int userId = provider.getUserId();
                    if (this.mUserManager.isUserUnlockingOrUnlocked(userId) && !isProfileWithLockedParent(userId)) {
                        ensureGroupStateLoadedLocked(userId);
                        if (!hashSet.contains(provider.id) && updateProvidersForPackageLocked(provider.id.componentName.getPackageName(), provider.getUserId(), hashSet)) {
                            if (sparseIntArray == null) {
                                sparseIntArray = new SparseIntArray();
                            }
                            int groupParent = this.mSecurityPolicy.getGroupParent(provider.getUserId());
                            sparseIntArray.put(groupParent, groupParent);
                        }
                    }
                }
                if (sparseIntArray != null) {
                    int size2 = sparseIntArray.size();
                    for (int i = 0; i < size2; i++) {
                        saveGroupStateAsync(sparseIntArray.get(i));
                    }
                }
            }
        }
    }

    private void onPackageBroadcastReceived(Intent intent, int i) {
        byte b;
        String[] stringArrayExtra;
        boolean zEquals;
        boolean z;
        boolean zUpdateProvidersForPackageLocked;
        int uidForPackage;
        String schemeSpecificPart;
        String action = intent.getAction();
        int iHashCode = action.hashCode();
        boolean z2 = true;
        int i2 = 0;
        if (iHashCode != -1403934493) {
            if (iHashCode != -1338021860) {
                if (iHashCode != -1001645458) {
                    b = (iHashCode == 1290767157 && action.equals("android.intent.action.PACKAGES_UNSUSPENDED")) ? (byte) 1 : (byte) -1;
                } else if (action.equals("android.intent.action.PACKAGES_SUSPENDED")) {
                    b = 0;
                }
            } else if (action.equals("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE")) {
                b = 2;
            }
        } else if (action.equals("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE")) {
            b = 3;
        }
        switch (b) {
            case 0:
            case 1:
                stringArrayExtra = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                zEquals = true;
                z = false;
                if (stringArrayExtra != null || stringArrayExtra.length == 0) {
                    return;
                }
                synchronized (this.mLock) {
                    if (this.mUserManager.isUserUnlockingOrUnlocked(i) && !isProfileWithLockedParent(i)) {
                        ensureGroupStateLoadedLocked(i, false);
                        Bundle extras = intent.getExtras();
                        if (z || zEquals) {
                            if (!z || (extras != null && extras.getBoolean("android.intent.extra.REPLACING", false))) {
                                z2 = false;
                            }
                            int length = stringArrayExtra.length;
                            zUpdateProvidersForPackageLocked = false;
                            while (i2 < length) {
                                String str = stringArrayExtra[i2];
                                zUpdateProvidersForPackageLocked |= updateProvidersForPackageLocked(str, i, null);
                                if (z2 && i == 0 && (uidForPackage = getUidForPackage(str, i)) >= 0) {
                                    resolveHostUidLocked(str, uidForPackage);
                                }
                                i2++;
                            }
                        } else {
                            if (extras != null && extras.getBoolean("android.intent.extra.REPLACING", false)) {
                                z2 = false;
                            }
                            if (z2) {
                                int length2 = stringArrayExtra.length;
                                zUpdateProvidersForPackageLocked = false;
                                while (i2 < length2) {
                                    zUpdateProvidersForPackageLocked |= removeHostsAndProvidersForPackageLocked(stringArrayExtra[i2], i);
                                    i2++;
                                }
                            } else {
                                zUpdateProvidersForPackageLocked = false;
                            }
                        }
                        if (zUpdateProvidersForPackageLocked) {
                            saveGroupStateAsync(i);
                            scheduleNotifyGroupHostsForProvidersChangedLocked(i);
                        }
                        return;
                    }
                    return;
                }
            case 2:
                z = true;
                stringArrayExtra = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                zEquals = false;
                if (stringArrayExtra != null) {
                    return;
                } else {
                    return;
                }
            case 3:
                z = false;
                stringArrayExtra = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                zEquals = false;
                if (stringArrayExtra != null) {
                }
                break;
            default:
                Uri data = intent.getData();
                if (data == null || (schemeSpecificPart = data.getSchemeSpecificPart()) == null) {
                    return;
                }
                String[] strArr = {schemeSpecificPart};
                boolean zEquals2 = "android.intent.action.PACKAGE_ADDED".equals(action);
                zEquals = "android.intent.action.PACKAGE_CHANGED".equals(action);
                z = zEquals2;
                stringArrayExtra = strArr;
                if (stringArrayExtra != null) {
                }
                break;
        }
    }

    void reloadWidgetsMaskedStateForGroup(int i) {
        if (!this.mUserManager.isUserUnlockingOrUnlocked(i)) {
            return;
        }
        synchronized (this.mLock) {
            reloadWidgetsMaskedState(i);
            for (int i2 : this.mUserManager.getEnabledProfileIds(i)) {
                reloadWidgetsMaskedState(i2);
            }
        }
    }

    private void reloadWidgetsMaskedState(int i) {
        boolean zIsPackageSuspendedForUser;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            UserInfo userInfo = this.mUserManager.getUserInfo(i);
            boolean z = !this.mUserManager.isUserUnlockingOrUnlocked(i);
            boolean zIsQuietModeEnabled = userInfo.isQuietModeEnabled();
            int size = this.mProviders.size();
            for (int i2 = 0; i2 < size; i2++) {
                Provider provider = this.mProviders.get(i2);
                if (provider.getUserId() == i) {
                    boolean maskedByLockedProfileLocked = provider.setMaskedByLockedProfileLocked(z) | provider.setMaskedByQuietProfileLocked(zIsQuietModeEnabled);
                    try {
                        try {
                            zIsPackageSuspendedForUser = this.mPackageManager.isPackageSuspendedForUser(provider.info.provider.getPackageName(), provider.getUserId());
                        } catch (RemoteException e) {
                            Slog.e(TAG, "Failed to query application info", e);
                        }
                    } catch (IllegalArgumentException e2) {
                        zIsPackageSuspendedForUser = false;
                    }
                    maskedByLockedProfileLocked |= provider.setMaskedBySuspendedPackageLocked(zIsPackageSuspendedForUser);
                    if (maskedByLockedProfileLocked) {
                        if (provider.isMaskedLocked()) {
                            maskWidgetsViewsLocked(provider, null);
                        } else {
                            unmaskWidgetsViewsLocked(provider);
                        }
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void updateWidgetPackageSuspensionMaskedState(Intent intent, boolean z, int i) {
        String[] stringArrayExtra = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
        if (stringArrayExtra == null) {
            return;
        }
        ArraySet arraySet = new ArraySet(Arrays.asList(stringArrayExtra));
        synchronized (this.mLock) {
            int size = this.mProviders.size();
            for (int i2 = 0; i2 < size; i2++) {
                Provider provider = this.mProviders.get(i2);
                if (provider.getUserId() == i && arraySet.contains(provider.info.provider.getPackageName()) && provider.setMaskedBySuspendedPackageLocked(z)) {
                    if (provider.isMaskedLocked()) {
                        maskWidgetsViewsLocked(provider, null);
                    } else {
                        unmaskWidgetsViewsLocked(provider);
                    }
                }
            }
        }
    }

    private Bitmap createMaskedWidgetBitmap(String str, int i) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            PackageManager packageManager = this.mContext.createPackageContextAsUser(str, 0, UserHandle.of(i)).getPackageManager();
            Drawable drawableMutate = packageManager.getApplicationInfo(str, 0).loadUnbadgedIcon(packageManager).mutate();
            drawableMutate.setColorFilter(this.mIconUtilities.getDisabledColorFilter());
            return this.mIconUtilities.createIconBitmap(drawableMutate);
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "Fail to get application icon", e);
            return null;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private RemoteViews createMaskedWidgetRemoteViews(Bitmap bitmap, boolean z, PendingIntent pendingIntent) {
        RemoteViews remoteViews = new RemoteViews(this.mContext.getPackageName(), R.layout.progress_dialog);
        if (bitmap != null) {
            remoteViews.setImageViewBitmap(R.id.resolver_tab_divider, bitmap);
        }
        if (!z) {
            remoteViews.setViewVisibility(R.id.resourcesUnused, 4);
        }
        if (pendingIntent != null) {
            remoteViews.setOnClickPendingIntent(R.id.restart, pendingIntent);
        }
        return remoteViews;
    }

    private void maskWidgetsViewsLocked(Provider provider, Widget widget) {
        String packageName;
        int userId;
        Bitmap bitmapCreateMaskedWidgetBitmap;
        Intent intentCreateConfirmDeviceCredentialIntent;
        PendingIntent activity;
        int size = provider.widgets.size();
        if (size == 0 || (bitmapCreateMaskedWidgetBitmap = createMaskedWidgetBitmap((packageName = provider.info.provider.getPackageName()), (userId = provider.getUserId()))) == null) {
            return;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            boolean z = true;
            if (provider.maskedBySuspendedPackage) {
                boolean zIsManagedProfile = this.mUserManager.getUserInfo(userId).isManagedProfile();
                String suspendingPackage = this.mPackageManagerInternal.getSuspendingPackage(packageName, userId);
                if ("android".equals(suspendingPackage)) {
                    intentCreateConfirmDeviceCredentialIntent = this.mDevicePolicyManagerInternal.createShowAdminSupportIntent(userId, true);
                } else {
                    intentCreateConfirmDeviceCredentialIntent = SuspendedAppActivity.createSuspendedAppInterceptIntent(packageName, suspendingPackage, this.mPackageManagerInternal.getSuspendedDialogMessage(packageName, userId), userId);
                }
                z = zIsManagedProfile;
            } else if (provider.maskedByQuietProfile) {
                intentCreateConfirmDeviceCredentialIntent = UnlaunchableAppActivity.createInQuietModeDialogIntent(userId);
            } else {
                intentCreateConfirmDeviceCredentialIntent = this.mKeyguardManager.createConfirmDeviceCredentialIntent(null, null, userId);
                if (intentCreateConfirmDeviceCredentialIntent != null) {
                    intentCreateConfirmDeviceCredentialIntent.setFlags(276824064);
                }
            }
            for (int i = 0; i < size; i++) {
                Widget widget2 = provider.widgets.get(i);
                if (widget == null || widget == widget2) {
                    if (intentCreateConfirmDeviceCredentialIntent != null) {
                        activity = PendingIntent.getActivity(this.mContext, widget2.appWidgetId, intentCreateConfirmDeviceCredentialIntent, 134217728);
                    } else {
                        activity = null;
                    }
                    if (widget2.replaceWithMaskedViewsLocked(createMaskedWidgetRemoteViews(bitmapCreateMaskedWidgetBitmap, z, activity))) {
                        scheduleNotifyUpdateAppWidgetLocked(widget2, widget2.getEffectiveViewsLocked());
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void unmaskWidgetsViewsLocked(Provider provider) {
        int size = provider.widgets.size();
        for (int i = 0; i < size; i++) {
            Widget widget = provider.widgets.get(i);
            if (widget.clearMaskedViewsLocked()) {
                scheduleNotifyUpdateAppWidgetLocked(widget, widget.getEffectiveViewsLocked());
            }
        }
    }

    private void resolveHostUidLocked(String str, int i) {
        int size = this.mHosts.size();
        for (int i2 = 0; i2 < size; i2++) {
            Host host = this.mHosts.get(i2);
            if (host.id.uid == -1 && str.equals(host.id.packageName)) {
                if (DEBUG) {
                    Slog.i(TAG, "host " + host.id + " resolved to uid " + i);
                }
                host.id = new HostId(i, host.id.hostId, host.id.packageName);
                return;
            }
        }
    }

    private void ensureGroupStateLoadedLocked(int i) throws Exception {
        ensureGroupStateLoadedLocked(i, true);
    }

    private void ensureGroupStateLoadedLocked(int i, boolean z) throws Exception {
        if (z && !isUserRunningAndUnlocked(i)) {
            throw new IllegalStateException("User " + i + " must be unlocked for widgets to be available");
        }
        if (z && isProfileWithLockedParent(i)) {
            throw new IllegalStateException("Profile " + i + " must have unlocked parent");
        }
        int[] enabledGroupProfileIds = this.mSecurityPolicy.getEnabledGroupProfileIds(i);
        int length = enabledGroupProfileIds.length;
        int i2 = 0;
        for (int i3 = 0; i3 < length; i3++) {
            if (this.mLoadedUserIds.indexOfKey(enabledGroupProfileIds[i3]) >= 0) {
                enabledGroupProfileIds[i3] = -1;
            } else {
                i2++;
            }
        }
        if (i2 <= 0) {
            return;
        }
        int[] iArr = new int[i2];
        int i4 = 0;
        for (int i5 : enabledGroupProfileIds) {
            if (i5 != -1) {
                this.mLoadedUserIds.put(i5, i5);
                iArr[i4] = i5;
                i4++;
            }
        }
        clearProvidersAndHostsTagsLocked();
        loadGroupWidgetProvidersLocked(iArr);
        loadGroupStateLocked(iArr);
    }

    private boolean isUserRunningAndUnlocked(int i) {
        return this.mUserManager.isUserUnlockingOrUnlocked(i);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            synchronized (this.mLock) {
                if (strArr.length > 0 && PriorityDump.PROTO_ARG.equals(strArr[0])) {
                    dumpProto(fileDescriptor);
                } else {
                    dumpInternal(printWriter);
                }
            }
        }
    }

    private void dumpProto(FileDescriptor fileDescriptor) {
        Slog.i(TAG, "dump proto for " + this.mWidgets.size() + " widgets");
        ProtoOutputStream protoOutputStream = new ProtoOutputStream(fileDescriptor);
        int size = this.mWidgets.size();
        for (int i = 0; i < size; i++) {
            dumpProtoWidget(protoOutputStream, this.mWidgets.get(i));
        }
        protoOutputStream.flush();
    }

    private void dumpProtoWidget(ProtoOutputStream protoOutputStream, Widget widget) {
        if (widget.host == null || widget.provider == null) {
            Slog.d(TAG, "skip dumping widget because host or provider is null: widget.host=" + widget.host + " widget.provider=" + widget.provider);
            return;
        }
        long jStart = protoOutputStream.start(2246267895809L);
        protoOutputStream.write(1133871366145L, widget.host.getUserId() != widget.provider.getUserId());
        protoOutputStream.write(1133871366146L, widget.host.callbacks == null);
        protoOutputStream.write(1138166333443L, widget.host.id.packageName);
        protoOutputStream.write(1138166333444L, widget.provider.id.componentName.getPackageName());
        protoOutputStream.write(1138166333445L, widget.provider.id.componentName.getClassName());
        if (widget.options != null) {
            protoOutputStream.write(1120986464262L, widget.options.getInt("appWidgetMinWidth", 0));
            protoOutputStream.write(1120986464263L, widget.options.getInt("appWidgetMinHeight", 0));
            protoOutputStream.write(1120986464264L, widget.options.getInt("appWidgetMaxWidth", 0));
            protoOutputStream.write(1120986464265L, widget.options.getInt("appWidgetMaxHeight", 0));
        }
        protoOutputStream.end(jStart);
    }

    private void dumpInternal(PrintWriter printWriter) {
        int size = this.mProviders.size();
        printWriter.println("Providers:");
        for (int i = 0; i < size; i++) {
            dumpProvider(this.mProviders.get(i), i, printWriter);
        }
        int size2 = this.mWidgets.size();
        printWriter.println(" ");
        printWriter.println("Widgets:");
        for (int i2 = 0; i2 < size2; i2++) {
            dumpWidget(this.mWidgets.get(i2), i2, printWriter);
        }
        int size3 = this.mHosts.size();
        printWriter.println(" ");
        printWriter.println("Hosts:");
        for (int i3 = 0; i3 < size3; i3++) {
            dumpHost(this.mHosts.get(i3), i3, printWriter);
        }
        int size4 = this.mPackagesWithBindWidgetPermission.size();
        printWriter.println(" ");
        printWriter.println("Grants:");
        for (int i4 = 0; i4 < size4; i4++) {
            dumpGrant(this.mPackagesWithBindWidgetPermission.valueAt(i4), i4, printWriter);
        }
    }

    public ParceledListSlice<PendingHostUpdate> startListening(IAppWidgetHost iAppWidgetHost, String str, int i, int[] iArr) {
        int callingUserId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "startListening() " + callingUserId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(str);
        synchronized (this.mLock) {
            if (this.mSecurityPolicy.isInstantAppLocked(str, callingUserId)) {
                Slog.w(TAG, "Instant package " + str + " cannot host app widgets");
                return ParceledListSlice.emptyList();
            }
            ensureGroupStateLoadedLocked(callingUserId);
            Host hostLookupOrAddHostLocked = lookupOrAddHostLocked(new HostId(Binder.getCallingUid(), i, str));
            hostLookupOrAddHostLocked.callbacks = iAppWidgetHost;
            long jIncrementAndGet = UPDATE_COUNTER.incrementAndGet();
            ArrayList arrayList = new ArrayList(iArr.length);
            LongSparseArray<PendingHostUpdate> longSparseArray = new LongSparseArray<>();
            for (int i2 : iArr) {
                if (hostLookupOrAddHostLocked.getPendingUpdatesForId(i2, longSparseArray)) {
                    int size = longSparseArray.size();
                    for (int i3 = 0; i3 < size; i3++) {
                        arrayList.add(longSparseArray.valueAt(i3));
                    }
                }
            }
            hostLookupOrAddHostLocked.lastWidgetUpdateSequenceNo = jIncrementAndGet;
            return new ParceledListSlice<>(arrayList);
        }
    }

    public void stopListening(String str, int i) {
        int callingUserId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "stopListening() " + callingUserId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(str);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(callingUserId, false);
            Host hostLookupHostLocked = lookupHostLocked(new HostId(Binder.getCallingUid(), i, str));
            if (hostLookupHostLocked != null) {
                hostLookupHostLocked.callbacks = null;
                pruneHostLocked(hostLookupHostLocked);
            }
        }
    }

    public int allocateAppWidgetId(String str, int i) {
        int callingUserId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "allocateAppWidgetId() " + callingUserId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(str);
        synchronized (this.mLock) {
            if (this.mSecurityPolicy.isInstantAppLocked(str, callingUserId)) {
                Slog.w(TAG, "Instant package " + str + " cannot host app widgets");
                return 0;
            }
            ensureGroupStateLoadedLocked(callingUserId);
            if (this.mNextAppWidgetIds.indexOfKey(callingUserId) < 0) {
                this.mNextAppWidgetIds.put(callingUserId, 1);
            }
            int iIncrementAndGetAppWidgetIdLocked = incrementAndGetAppWidgetIdLocked(callingUserId);
            Host hostLookupOrAddHostLocked = lookupOrAddHostLocked(new HostId(Binder.getCallingUid(), i, str));
            Widget widget = new Widget();
            widget.appWidgetId = iIncrementAndGetAppWidgetIdLocked;
            widget.host = hostLookupOrAddHostLocked;
            hostLookupOrAddHostLocked.widgets.add(widget);
            addWidgetLocked(widget);
            saveGroupStateAsync(callingUserId);
            if (DEBUG) {
                Slog.i(TAG, "Allocated widget id " + iIncrementAndGetAppWidgetIdLocked + " for host " + hostLookupOrAddHostLocked.id);
            }
            return iIncrementAndGetAppWidgetIdLocked;
        }
    }

    public void deleteAppWidgetId(String str, int i) {
        int callingUserId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "deleteAppWidgetId() " + callingUserId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(str);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(callingUserId);
            Widget widgetLookupWidgetLocked = lookupWidgetLocked(i, Binder.getCallingUid(), str);
            if (widgetLookupWidgetLocked == null) {
                return;
            }
            deleteAppWidgetLocked(widgetLookupWidgetLocked);
            saveGroupStateAsync(callingUserId);
            if (DEBUG) {
                Slog.i(TAG, "Deleted widget id " + i + " for host " + widgetLookupWidgetLocked.host.id);
            }
        }
    }

    public boolean hasBindAppWidgetPermission(String str, int i) {
        if (DEBUG) {
            Slog.i(TAG, "hasBindAppWidgetPermission() " + UserHandle.getCallingUserId());
        }
        this.mSecurityPolicy.enforceModifyAppWidgetBindPermissions(str);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(i);
            if (getUidForPackage(str, i) < 0) {
                return false;
            }
            return this.mPackagesWithBindWidgetPermission.contains(Pair.create(Integer.valueOf(i), str));
        }
    }

    public void setBindAppWidgetPermission(String str, int i, boolean z) {
        if (DEBUG) {
            Slog.i(TAG, "setBindAppWidgetPermission() " + UserHandle.getCallingUserId());
        }
        this.mSecurityPolicy.enforceModifyAppWidgetBindPermissions(str);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(i);
            if (getUidForPackage(str, i) < 0) {
                return;
            }
            Pair<Integer, String> pairCreate = Pair.create(Integer.valueOf(i), str);
            if (z) {
                this.mPackagesWithBindWidgetPermission.add(pairCreate);
            } else {
                this.mPackagesWithBindWidgetPermission.remove(pairCreate);
            }
            saveGroupStateAsync(i);
        }
    }

    public IntentSender createAppWidgetConfigIntentSender(String str, int i, int i2) {
        IntentSender intentSender;
        int callingUserId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "createAppWidgetConfigIntentSender() " + callingUserId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(str);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(callingUserId);
            Widget widgetLookupWidgetLocked = lookupWidgetLocked(i, Binder.getCallingUid(), str);
            if (widgetLookupWidgetLocked == null) {
                throw new IllegalArgumentException("Bad widget id " + i);
            }
            Provider provider = widgetLookupWidgetLocked.provider;
            if (provider == null) {
                throw new IllegalArgumentException("Widget not bound " + i);
            }
            Intent intent = new Intent("android.appwidget.action.APPWIDGET_CONFIGURE");
            intent.putExtra("appWidgetId", i);
            intent.setComponent(provider.info.configure);
            intent.setFlags(i2 & (-196));
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                intentSender = PendingIntent.getActivityAsUser(this.mContext, 0, intent, 1409286144, null, new UserHandle(provider.getUserId())).getIntentSender();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
        return intentSender;
    }

    public boolean bindAppWidgetId(String str, int i, int i2, ComponentName componentName, Bundle bundle) {
        int callingUserId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "bindAppWidgetId() " + callingUserId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(str);
        if (!this.mSecurityPolicy.isEnabledGroupProfile(i2) || !this.mSecurityPolicy.isProviderInCallerOrInProfileAndWhitelListed(componentName.getPackageName(), i2)) {
            return false;
        }
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(callingUserId);
            if (!this.mSecurityPolicy.hasCallerBindPermissionOrBindWhiteListedLocked(str)) {
                return false;
            }
            Widget widgetLookupWidgetLocked = lookupWidgetLocked(i, Binder.getCallingUid(), str);
            if (widgetLookupWidgetLocked == null) {
                Slog.e(TAG, "Bad widget id " + i);
                return false;
            }
            if (widgetLookupWidgetLocked.provider != null) {
                Slog.e(TAG, "Widget id " + i + " already bound to: " + widgetLookupWidgetLocked.provider.id);
                return false;
            }
            int uidForPackage = getUidForPackage(componentName.getPackageName(), i2);
            if (uidForPackage < 0) {
                Slog.e(TAG, "Package " + componentName.getPackageName() + " not installed  for profile " + i2);
                return false;
            }
            Provider providerLookupProviderLocked = lookupProviderLocked(new ProviderId(uidForPackage, componentName));
            if (providerLookupProviderLocked == null) {
                Slog.e(TAG, "No widget provider " + componentName + " for profile " + i2);
                return false;
            }
            if (providerLookupProviderLocked.zombie) {
                Slog.e(TAG, "Can't bind to a 3rd party provider in safe mode " + providerLookupProviderLocked);
                return false;
            }
            widgetLookupWidgetLocked.provider = providerLookupProviderLocked;
            widgetLookupWidgetLocked.options = bundle != null ? cloneIfLocalBinder(bundle) : new Bundle();
            if (!widgetLookupWidgetLocked.options.containsKey("appWidgetCategory")) {
                widgetLookupWidgetLocked.options.putInt("appWidgetCategory", 1);
            }
            providerLookupProviderLocked.widgets.add(widgetLookupWidgetLocked);
            onWidgetProviderAddedOrChangedLocked(widgetLookupWidgetLocked);
            if (providerLookupProviderLocked.widgets.size() == 1) {
                sendEnableIntentLocked(providerLookupProviderLocked);
            }
            sendUpdateIntentLocked(providerLookupProviderLocked, new int[]{i});
            registerForBroadcastsLocked(providerLookupProviderLocked, getWidgetIds(providerLookupProviderLocked.widgets));
            saveGroupStateAsync(callingUserId);
            if (DEBUG) {
                Slog.i(TAG, "Bound widget " + i + " to provider " + providerLookupProviderLocked.id);
            }
            return true;
        }
    }

    public int[] getAppWidgetIds(ComponentName componentName) {
        int callingUserId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "getAppWidgetIds() " + callingUserId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(componentName.getPackageName());
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(callingUserId);
            Provider providerLookupProviderLocked = lookupProviderLocked(new ProviderId(Binder.getCallingUid(), componentName));
            if (providerLookupProviderLocked != null) {
                return getWidgetIds(providerLookupProviderLocked.widgets);
            }
            return new int[0];
        }
    }

    public int[] getAppWidgetIdsForHost(String str, int i) {
        int callingUserId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "getAppWidgetIdsForHost() " + callingUserId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(str);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(callingUserId);
            Host hostLookupHostLocked = lookupHostLocked(new HostId(Binder.getCallingUid(), i, str));
            if (hostLookupHostLocked != null) {
                return getWidgetIds(hostLookupHostLocked.widgets);
            }
            return new int[0];
        }
    }

    public boolean bindRemoteViewsService(String str, int i, Intent intent, IApplicationThread iApplicationThread, IBinder iBinder, IServiceConnection iServiceConnection, int i2) {
        int callingUserId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "bindRemoteViewsService() " + callingUserId);
        }
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(callingUserId);
            Widget widgetLookupWidgetLocked = lookupWidgetLocked(i, Binder.getCallingUid(), str);
            if (widgetLookupWidgetLocked == null) {
                throw new IllegalArgumentException("Bad widget id");
            }
            if (widgetLookupWidgetLocked.provider == null) {
                throw new IllegalArgumentException("No provider for widget " + i);
            }
            ComponentName component = intent.getComponent();
            if (!component.getPackageName().equals(widgetLookupWidgetLocked.provider.id.componentName.getPackageName())) {
                throw new SecurityException("The taget service not in the same package as the widget provider");
            }
            this.mSecurityPolicy.enforceServiceExistsAndRequiresBindRemoteViewsPermission(component, widgetLookupWidgetLocked.provider.getUserId());
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                if (ActivityManager.getService().bindService(iApplicationThread, iBinder, intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), iServiceConnection, i2, this.mContext.getOpPackageName(), widgetLookupWidgetLocked.provider.getUserId()) != 0) {
                    incrementAppWidgetServiceRefCount(i, Pair.create(Integer.valueOf(widgetLookupWidgetLocked.provider.id.uid), new Intent.FilterComparison(intent)));
                    return true;
                }
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            } catch (RemoteException e) {
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
            return false;
        }
    }

    public void deleteHost(String str, int i) {
        int callingUserId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "deleteHost() " + callingUserId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(str);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(callingUserId);
            Host hostLookupHostLocked = lookupHostLocked(new HostId(Binder.getCallingUid(), i, str));
            if (hostLookupHostLocked == null) {
                return;
            }
            deleteHostLocked(hostLookupHostLocked);
            saveGroupStateAsync(callingUserId);
            if (DEBUG) {
                Slog.i(TAG, "Deleted host " + hostLookupHostLocked.id);
            }
        }
    }

    public void deleteAllHosts() {
        int callingUserId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "deleteAllHosts() " + callingUserId);
        }
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(callingUserId);
            boolean z = false;
            for (int size = this.mHosts.size() - 1; size >= 0; size--) {
                Host host = this.mHosts.get(size);
                if (host.id.uid == Binder.getCallingUid()) {
                    deleteHostLocked(host);
                    if (DEBUG) {
                        Slog.i(TAG, "Deleted host " + host.id);
                    }
                    z = true;
                }
            }
            if (z) {
                saveGroupStateAsync(callingUserId);
            }
        }
    }

    public AppWidgetProviderInfo getAppWidgetInfo(String str, int i) {
        int callingUserId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "getAppWidgetInfo() " + callingUserId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(str);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(callingUserId);
            Widget widgetLookupWidgetLocked = lookupWidgetLocked(i, Binder.getCallingUid(), str);
            if (widgetLookupWidgetLocked != null && widgetLookupWidgetLocked.provider != null && !widgetLookupWidgetLocked.provider.zombie) {
                return cloneIfLocalBinder(widgetLookupWidgetLocked.provider.info);
            }
            return null;
        }
    }

    public RemoteViews getAppWidgetViews(String str, int i) {
        int callingUserId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "getAppWidgetViews() " + callingUserId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(str);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(callingUserId);
            Widget widgetLookupWidgetLocked = lookupWidgetLocked(i, Binder.getCallingUid(), str);
            if (widgetLookupWidgetLocked != null) {
                return cloneIfLocalBinder(widgetLookupWidgetLocked.getEffectiveViewsLocked());
            }
            return null;
        }
    }

    public void updateAppWidgetOptions(String str, int i, Bundle bundle) {
        int callingUserId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "updateAppWidgetOptions() " + callingUserId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(str);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(callingUserId);
            Widget widgetLookupWidgetLocked = lookupWidgetLocked(i, Binder.getCallingUid(), str);
            if (widgetLookupWidgetLocked == null) {
                return;
            }
            widgetLookupWidgetLocked.options.putAll(bundle);
            sendOptionsChangedIntentLocked(widgetLookupWidgetLocked);
            saveGroupStateAsync(callingUserId);
        }
    }

    public Bundle getAppWidgetOptions(String str, int i) {
        int callingUserId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "getAppWidgetOptions() " + callingUserId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(str);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(callingUserId);
            Widget widgetLookupWidgetLocked = lookupWidgetLocked(i, Binder.getCallingUid(), str);
            if (widgetLookupWidgetLocked != null && widgetLookupWidgetLocked.options != null) {
                return cloneIfLocalBinder(widgetLookupWidgetLocked.options);
            }
            return Bundle.EMPTY;
        }
    }

    public void updateAppWidgetIds(String str, int[] iArr, RemoteViews remoteViews) {
        if (DEBUG) {
            Slog.i(TAG, "updateAppWidgetIds() " + UserHandle.getCallingUserId());
        }
        updateAppWidgetIds(str, iArr, remoteViews, false);
    }

    public void partiallyUpdateAppWidgetIds(String str, int[] iArr, RemoteViews remoteViews) {
        if (DEBUG) {
            Slog.i(TAG, "partiallyUpdateAppWidgetIds() " + UserHandle.getCallingUserId());
        }
        updateAppWidgetIds(str, iArr, remoteViews, true);
    }

    public void notifyAppWidgetViewDataChanged(String str, int[] iArr, int i) {
        int callingUserId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "notifyAppWidgetViewDataChanged() " + callingUserId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(str);
        if (iArr == null || iArr.length == 0) {
            return;
        }
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(callingUserId);
            for (int i2 : iArr) {
                Widget widgetLookupWidgetLocked = lookupWidgetLocked(i2, Binder.getCallingUid(), str);
                if (widgetLookupWidgetLocked != null) {
                    scheduleNotifyAppWidgetViewDataChanged(widgetLookupWidgetLocked, i);
                }
            }
        }
    }

    public void updateAppWidgetProvider(ComponentName componentName, RemoteViews remoteViews) {
        int callingUserId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "updateAppWidgetProvider() " + callingUserId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(componentName.getPackageName());
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(callingUserId);
            ProviderId providerId = new ProviderId(Binder.getCallingUid(), componentName);
            Provider providerLookupProviderLocked = lookupProviderLocked(providerId);
            if (providerLookupProviderLocked == null) {
                Slog.w(TAG, "Provider doesn't exist " + providerId);
                return;
            }
            ArrayList<Widget> arrayList = providerLookupProviderLocked.widgets;
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                updateAppWidgetInstanceLocked(arrayList.get(i), remoteViews, false);
            }
        }
    }

    public void updateAppWidgetProviderInfo(ComponentName componentName, String str) {
        String str2;
        int callingUserId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "updateAppWidgetProvider() " + callingUserId);
        }
        this.mSecurityPolicy.enforceCallFromPackage(componentName.getPackageName());
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(callingUserId);
            ProviderId providerId = new ProviderId(Binder.getCallingUid(), componentName);
            Provider providerLookupProviderLocked = lookupProviderLocked(providerId);
            if (providerLookupProviderLocked == null) {
                throw new IllegalArgumentException(componentName + " is not a valid AppWidget provider");
            }
            if (Objects.equals(providerLookupProviderLocked.infoTag, str)) {
                return;
            }
            if (str == null) {
                str2 = "android.appwidget.provider";
            } else {
                str2 = str;
            }
            AppWidgetProviderInfo appWidgetProviderInfo = parseAppWidgetProviderInfo(providerId, providerLookupProviderLocked.info.providerInfo, str2);
            if (appWidgetProviderInfo == null) {
                throw new IllegalArgumentException("Unable to parse " + str2 + " meta-data to a valid AppWidget provider");
            }
            providerLookupProviderLocked.info = appWidgetProviderInfo;
            providerLookupProviderLocked.infoTag = str;
            int size = providerLookupProviderLocked.widgets.size();
            for (int i = 0; i < size; i++) {
                Widget widget = providerLookupProviderLocked.widgets.get(i);
                scheduleNotifyProviderChangedLocked(widget);
                updateAppWidgetInstanceLocked(widget, widget.views, false);
            }
            saveGroupStateAsync(callingUserId);
            scheduleNotifyGroupHostsForProvidersChangedLocked(callingUserId);
        }
    }

    public boolean isRequestPinAppWidgetSupported() {
        synchronized (this.mLock) {
            if (this.mSecurityPolicy.isCallerInstantAppLocked()) {
                Slog.w(TAG, "Instant uid " + Binder.getCallingUid() + " query information about app widgets");
                return false;
            }
            return ((ShortcutServiceInternal) LocalServices.getService(ShortcutServiceInternal.class)).isRequestPinItemSupported(UserHandle.getCallingUserId(), 2);
        }
    }

    public boolean requestPinAppWidget(String str, ComponentName componentName, Bundle bundle, IntentSender intentSender) {
        int callingUid = Binder.getCallingUid();
        int userId = UserHandle.getUserId(callingUid);
        if (DEBUG) {
            Slog.i(TAG, "requestPinAppWidget() " + userId);
        }
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(userId);
            Provider providerLookupProviderLocked = lookupProviderLocked(new ProviderId(callingUid, componentName));
            if (providerLookupProviderLocked != null && !providerLookupProviderLocked.zombie) {
                AppWidgetProviderInfo appWidgetProviderInfo = providerLookupProviderLocked.info;
                if ((appWidgetProviderInfo.widgetCategory & 1) == 0) {
                    return false;
                }
                return ((ShortcutServiceInternal) LocalServices.getService(ShortcutServiceInternal.class)).requestPinAppWidget(str, appWidgetProviderInfo, bundle, intentSender, userId);
            }
            return false;
        }
    }

    public ParceledListSlice<AppWidgetProviderInfo> getInstalledProvidersForProfile(int i, int i2, String str) {
        int identifier;
        int callingUserId = UserHandle.getCallingUserId();
        if (DEBUG) {
            Slog.i(TAG, "getInstalledProvidersForProfiles() " + callingUserId);
        }
        if (!this.mSecurityPolicy.isEnabledGroupProfile(i2)) {
            return null;
        }
        synchronized (this.mLock) {
            if (this.mSecurityPolicy.isCallerInstantAppLocked()) {
                Slog.w(TAG, "Instant uid " + Binder.getCallingUid() + " cannot access widget providers");
                return ParceledListSlice.emptyList();
            }
            ensureGroupStateLoadedLocked(callingUserId);
            ArrayList arrayList = new ArrayList();
            int size = this.mProviders.size();
            for (int i3 = 0; i3 < size; i3++) {
                Provider provider = this.mProviders.get(i3);
                AppWidgetProviderInfo appWidgetProviderInfo = provider.info;
                boolean z = str == null || provider.id.componentName.getPackageName().equals(str);
                if (!provider.zombie && (appWidgetProviderInfo.widgetCategory & i) != 0 && z && (identifier = appWidgetProviderInfo.getProfile().getIdentifier()) == i2 && this.mSecurityPolicy.isProviderInCallerOrInProfileAndWhitelListed(provider.id.componentName.getPackageName(), identifier)) {
                    arrayList.add(cloneIfLocalBinder(appWidgetProviderInfo));
                }
            }
            return new ParceledListSlice<>(arrayList);
        }
    }

    private void updateAppWidgetIds(String str, int[] iArr, RemoteViews remoteViews, boolean z) {
        int callingUserId = UserHandle.getCallingUserId();
        if (iArr == null || iArr.length == 0) {
            return;
        }
        this.mSecurityPolicy.enforceCallFromPackage(str);
        synchronized (this.mLock) {
            ensureGroupStateLoadedLocked(callingUserId);
            for (int i : iArr) {
                Widget widgetLookupWidgetLocked = lookupWidgetLocked(i, Binder.getCallingUid(), str);
                if (widgetLookupWidgetLocked != null) {
                    updateAppWidgetInstanceLocked(widgetLookupWidgetLocked, remoteViews, z);
                }
            }
        }
    }

    private int incrementAndGetAppWidgetIdLocked(int i) {
        int iPeekNextAppWidgetIdLocked = peekNextAppWidgetIdLocked(i) + 1;
        this.mNextAppWidgetIds.put(i, iPeekNextAppWidgetIdLocked);
        return iPeekNextAppWidgetIdLocked;
    }

    private void setMinAppWidgetIdLocked(int i, int i2) {
        if (peekNextAppWidgetIdLocked(i) < i2) {
            this.mNextAppWidgetIds.put(i, i2);
        }
    }

    private int peekNextAppWidgetIdLocked(int i) {
        if (this.mNextAppWidgetIds.indexOfKey(i) < 0) {
            return 1;
        }
        return this.mNextAppWidgetIds.get(i);
    }

    private Host lookupOrAddHostLocked(HostId hostId) {
        Host hostLookupHostLocked = lookupHostLocked(hostId);
        if (hostLookupHostLocked != null) {
            return hostLookupHostLocked;
        }
        Host host = new Host();
        host.id = hostId;
        this.mHosts.add(host);
        return host;
    }

    private void deleteHostLocked(Host host) {
        for (int size = host.widgets.size() - 1; size >= 0; size--) {
            deleteAppWidgetLocked(host.widgets.remove(size));
        }
        this.mHosts.remove(host);
        host.callbacks = null;
    }

    private void deleteAppWidgetLocked(Widget widget) {
        decrementAppWidgetServiceRefCount(widget);
        Host host = widget.host;
        host.widgets.remove(widget);
        pruneHostLocked(host);
        removeWidgetLocked(widget);
        Provider provider = widget.provider;
        if (provider != null) {
            provider.widgets.remove(widget);
            if (!provider.zombie) {
                sendDeletedIntentLocked(widget);
                if (provider.widgets.isEmpty()) {
                    cancelBroadcasts(provider);
                    sendDisabledIntentLocked(provider);
                }
            }
        }
    }

    private void cancelBroadcasts(Provider provider) {
        if (DEBUG) {
            Slog.i(TAG, "cancelBroadcasts() for " + provider);
        }
        if (provider.broadcast != null) {
            this.mAlarmManager.cancel(provider.broadcast);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                provider.broadcast.cancel();
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                provider.broadcast = null;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        }
    }

    private void destroyRemoteViewsService(final Intent intent, Widget widget) {
        ServiceConnection serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                try {
                    IRemoteViewsFactory.Stub.asInterface(iBinder).onDestroy(intent);
                } catch (RemoteException e) {
                    Slog.e(AppWidgetServiceImpl.TAG, "Error calling remove view factory", e);
                }
                AppWidgetServiceImpl.this.mContext.unbindService(this);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
            }
        };
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mContext.bindServiceAsUser(intent, serviceConnection, 33554433, widget.provider.info.getProfile());
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void incrementAppWidgetServiceRefCount(int i, Pair<Integer, Intent.FilterComparison> pair) {
        HashSet<Integer> hashSet;
        if (this.mRemoteViewsServicesAppWidgets.containsKey(pair)) {
            hashSet = this.mRemoteViewsServicesAppWidgets.get(pair);
        } else {
            HashSet<Integer> hashSet2 = new HashSet<>();
            this.mRemoteViewsServicesAppWidgets.put(pair, hashSet2);
            hashSet = hashSet2;
        }
        hashSet.add(Integer.valueOf(i));
    }

    private void decrementAppWidgetServiceRefCount(Widget widget) {
        Iterator<Pair<Integer, Intent.FilterComparison>> it = this.mRemoteViewsServicesAppWidgets.keySet().iterator();
        while (it.hasNext()) {
            Pair<Integer, Intent.FilterComparison> next = it.next();
            HashSet<Integer> hashSet = this.mRemoteViewsServicesAppWidgets.get(next);
            if (hashSet.remove(Integer.valueOf(widget.appWidgetId)) && hashSet.isEmpty()) {
                destroyRemoteViewsService(((Intent.FilterComparison) next.second).getIntent(), widget);
                it.remove();
            }
        }
    }

    private void saveGroupStateAsync(int i) {
        this.mSaveStateHandler.post(new SaveStateRunnable(i));
    }

    private void updateAppWidgetInstanceLocked(Widget widget, RemoteViews remoteViews, boolean z) {
        int iEstimateMemoryUsage;
        if (widget != null && widget.provider != null && !widget.provider.zombie && !widget.host.zombie) {
            if (z && widget.views != null) {
                widget.views.mergeRemoteViews(remoteViews);
            } else {
                widget.views = remoteViews;
            }
            if (UserHandle.getAppId(Binder.getCallingUid()) != 1000 && widget.views != null && (iEstimateMemoryUsage = widget.views.estimateMemoryUsage()) > this.mMaxWidgetBitmapMemory) {
                widget.views = null;
                throw new IllegalArgumentException("RemoteViews for widget update exceeds maximum bitmap memory usage (used: " + iEstimateMemoryUsage + ", max: " + this.mMaxWidgetBitmapMemory + ")");
            }
            scheduleNotifyUpdateAppWidgetLocked(widget, widget.getEffectiveViewsLocked());
        }
    }

    private void scheduleNotifyAppWidgetViewDataChanged(Widget widget, int i) {
        if (i == 0 || i == 1) {
            return;
        }
        long jIncrementAndGet = UPDATE_COUNTER.incrementAndGet();
        if (widget != null) {
            widget.updateSequenceNos.put(i, jIncrementAndGet);
        }
        if (widget == null || widget.host == null || widget.host.zombie || widget.host.callbacks == null || widget.provider == null || widget.provider.zombie) {
            return;
        }
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.arg1 = widget.host;
        someArgsObtain.arg2 = widget.host.callbacks;
        someArgsObtain.arg3 = Long.valueOf(jIncrementAndGet);
        someArgsObtain.argi1 = widget.appWidgetId;
        someArgsObtain.argi2 = i;
        this.mCallbackHandler.obtainMessage(4, someArgsObtain).sendToTarget();
    }

    private void handleNotifyAppWidgetViewDataChanged(Host host, IAppWidgetHost iAppWidgetHost, int i, int i2, long j) {
        try {
            iAppWidgetHost.viewDataChanged(i, i2);
            host.lastWidgetUpdateSequenceNo = j;
        } catch (RemoteException e) {
            iAppWidgetHost = null;
        }
        synchronized (this.mLock) {
            if (iAppWidgetHost == null) {
                try {
                    host.callbacks = null;
                    for (Pair<Integer, Intent.FilterComparison> pair : this.mRemoteViewsServicesAppWidgets.keySet()) {
                        if (this.mRemoteViewsServicesAppWidgets.get(pair).contains(Integer.valueOf(i))) {
                            bindService(((Intent.FilterComparison) pair.second).getIntent(), new ServiceConnection() {
                                @Override
                                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                                    try {
                                        IRemoteViewsFactory.Stub.asInterface(iBinder).onDataSetChangedAsync();
                                    } catch (RemoteException e2) {
                                        Slog.e(AppWidgetServiceImpl.TAG, "Error calling onDataSetChangedAsync()", e2);
                                    }
                                    AppWidgetServiceImpl.this.mContext.unbindService(this);
                                }

                                @Override
                                public void onServiceDisconnected(ComponentName componentName) {
                                }
                            }, new UserHandle(UserHandle.getUserId(((Integer) pair.first).intValue())));
                        }
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
    }

    private void scheduleNotifyUpdateAppWidgetLocked(Widget widget, RemoteViews remoteViews) {
        long jIncrementAndGet = UPDATE_COUNTER.incrementAndGet();
        if (widget != null) {
            widget.updateSequenceNos.put(0, jIncrementAndGet);
        }
        if (widget == null || widget.provider == null || widget.provider.zombie || widget.host.callbacks == null || widget.host.zombie) {
            return;
        }
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.arg1 = widget.host;
        someArgsObtain.arg2 = widget.host.callbacks;
        someArgsObtain.arg3 = remoteViews != null ? remoteViews.clone() : null;
        someArgsObtain.arg4 = Long.valueOf(jIncrementAndGet);
        someArgsObtain.argi1 = widget.appWidgetId;
        this.mCallbackHandler.obtainMessage(1, someArgsObtain).sendToTarget();
    }

    private void handleNotifyUpdateAppWidget(Host host, IAppWidgetHost iAppWidgetHost, int i, RemoteViews remoteViews, long j) {
        try {
            iAppWidgetHost.updateAppWidget(i, remoteViews);
            host.lastWidgetUpdateSequenceNo = j;
        } catch (RemoteException e) {
            synchronized (this.mLock) {
                Slog.e(TAG, "Widget host dead: " + host.id, e);
                host.callbacks = null;
            }
        }
    }

    private void scheduleNotifyProviderChangedLocked(Widget widget) {
        long jIncrementAndGet = UPDATE_COUNTER.incrementAndGet();
        if (widget != null) {
            widget.updateSequenceNos.clear();
            widget.updateSequenceNos.append(1, jIncrementAndGet);
        }
        if (widget == null || widget.provider == null || widget.provider.zombie || widget.host.callbacks == null || widget.host.zombie) {
            return;
        }
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.arg1 = widget.host;
        someArgsObtain.arg2 = widget.host.callbacks;
        someArgsObtain.arg3 = widget.provider.info;
        someArgsObtain.arg4 = Long.valueOf(jIncrementAndGet);
        someArgsObtain.argi1 = widget.appWidgetId;
        this.mCallbackHandler.obtainMessage(2, someArgsObtain).sendToTarget();
    }

    private void handleNotifyProviderChanged(Host host, IAppWidgetHost iAppWidgetHost, int i, AppWidgetProviderInfo appWidgetProviderInfo, long j) {
        try {
            iAppWidgetHost.providerChanged(i, appWidgetProviderInfo);
            host.lastWidgetUpdateSequenceNo = j;
        } catch (RemoteException e) {
            synchronized (this.mLock) {
                Slog.e(TAG, "Widget host dead: " + host.id, e);
                host.callbacks = null;
            }
        }
    }

    private void scheduleNotifyGroupHostsForProvidersChangedLocked(int i) {
        int[] enabledGroupProfileIds = this.mSecurityPolicy.getEnabledGroupProfileIds(i);
        for (int size = this.mHosts.size() - 1; size >= 0; size--) {
            Host host = this.mHosts.get(size);
            int length = enabledGroupProfileIds.length;
            boolean z = false;
            int i2 = 0;
            while (true) {
                if (i2 >= length) {
                    break;
                }
                if (host.getUserId() != enabledGroupProfileIds[i2]) {
                    i2++;
                } else {
                    z = true;
                    break;
                }
            }
            if (z && host != null && !host.zombie && host.callbacks != null) {
                SomeArgs someArgsObtain = SomeArgs.obtain();
                someArgsObtain.arg1 = host;
                someArgsObtain.arg2 = host.callbacks;
                this.mCallbackHandler.obtainMessage(3, someArgsObtain).sendToTarget();
            }
        }
    }

    private void handleNotifyProvidersChanged(Host host, IAppWidgetHost iAppWidgetHost) {
        try {
            iAppWidgetHost.providersChanged();
        } catch (RemoteException e) {
            synchronized (this.mLock) {
                Slog.e(TAG, "Widget host dead: " + host.id, e);
                host.callbacks = null;
            }
        }
    }

    private static boolean isLocalBinder() {
        return Process.myPid() == Binder.getCallingPid();
    }

    private static RemoteViews cloneIfLocalBinder(RemoteViews remoteViews) {
        if (isLocalBinder() && remoteViews != null) {
            return remoteViews.clone();
        }
        return remoteViews;
    }

    private static AppWidgetProviderInfo cloneIfLocalBinder(AppWidgetProviderInfo appWidgetProviderInfo) {
        if (isLocalBinder() && appWidgetProviderInfo != null) {
            return appWidgetProviderInfo.clone();
        }
        return appWidgetProviderInfo;
    }

    private static Bundle cloneIfLocalBinder(Bundle bundle) {
        if (isLocalBinder() && bundle != null) {
            return (Bundle) bundle.clone();
        }
        return bundle;
    }

    private Widget lookupWidgetLocked(int i, int i2, String str) {
        int size = this.mWidgets.size();
        for (int i3 = 0; i3 < size; i3++) {
            Widget widget = this.mWidgets.get(i3);
            if (widget.appWidgetId == i && this.mSecurityPolicy.canAccessAppWidget(widget, i2, str)) {
                return widget;
            }
        }
        return null;
    }

    private Provider lookupProviderLocked(ProviderId providerId) {
        int size = this.mProviders.size();
        for (int i = 0; i < size; i++) {
            Provider provider = this.mProviders.get(i);
            if (provider.id.equals(providerId)) {
                return provider;
            }
        }
        return null;
    }

    private Host lookupHostLocked(HostId hostId) {
        int size = this.mHosts.size();
        for (int i = 0; i < size; i++) {
            Host host = this.mHosts.get(i);
            if (host.id.equals(hostId)) {
                return host;
            }
        }
        return null;
    }

    private void pruneHostLocked(Host host) {
        if (host.widgets.size() == 0 && host.callbacks == null) {
            if (DEBUG) {
                Slog.i(TAG, "Pruning host " + host.id);
            }
            this.mHosts.remove(host);
        }
    }

    private void loadGroupWidgetProvidersLocked(int[] iArr) {
        int size;
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_UPDATE");
        ArrayList arrayList = null;
        for (int i : iArr) {
            List<ResolveInfo> listQueryIntentReceivers = queryIntentReceivers(intent, i);
            if (listQueryIntentReceivers != null && !listQueryIntentReceivers.isEmpty()) {
                if (arrayList == null) {
                    arrayList = new ArrayList();
                }
                arrayList.addAll(listQueryIntentReceivers);
            }
        }
        if (arrayList != null) {
            size = arrayList.size();
        } else {
            size = 0;
        }
        for (int i2 = 0; i2 < size; i2++) {
            addProviderLocked((ResolveInfo) arrayList.get(i2));
        }
    }

    private boolean addProviderLocked(ResolveInfo resolveInfo) {
        ComponentName componentName;
        ProviderId providerId;
        AnonymousClass1 anonymousClass1;
        Provider providerInfoXml;
        if ((resolveInfo.activityInfo.applicationInfo.flags & DumpState.DUMP_DOMAIN_PREFERRED) != 0 || !resolveInfo.activityInfo.isEnabled() || (providerInfoXml = parseProviderInfoXml((providerId = new ProviderId(resolveInfo.activityInfo.applicationInfo.uid, (componentName = new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name)))), resolveInfo, null)) == null) {
            return false;
        }
        Provider providerLookupProviderLocked = lookupProviderLocked(providerId);
        if (providerLookupProviderLocked == null) {
            providerLookupProviderLocked = lookupProviderLocked(new ProviderId(-1, componentName));
        }
        if (providerLookupProviderLocked != null) {
            if (providerLookupProviderLocked.zombie && !this.mSafeMode) {
                providerLookupProviderLocked.id = providerId;
                providerLookupProviderLocked.zombie = false;
                providerLookupProviderLocked.info = providerInfoXml.info;
                if (DEBUG) {
                    Slog.i(TAG, "Provider placeholder now reified: " + providerLookupProviderLocked);
                    return true;
                }
                return true;
            }
            return true;
        }
        this.mProviders.add(providerInfoXml);
        return true;
    }

    private void deleteWidgetsLocked(Provider provider, int i) {
        for (int size = provider.widgets.size() - 1; size >= 0; size--) {
            Widget widget = provider.widgets.get(size);
            if (i == -1 || i == widget.host.getUserId()) {
                provider.widgets.remove(size);
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

    private void sendEnableIntentLocked(Provider provider) {
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_ENABLED");
        intent.setComponent(provider.info.provider);
        sendBroadcastAsUser(intent, provider.info.getProfile());
    }

    private void sendUpdateIntentLocked(Provider provider, int[] iArr) {
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_UPDATE");
        intent.putExtra("appWidgetIds", iArr);
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

    private void registerForBroadcastsLocked(Provider provider, int[] iArr) {
        boolean z;
        if (provider.info.updatePeriodMillis > 0) {
            if (provider.broadcast == null) {
                z = false;
            } else {
                z = true;
            }
            Intent intent = new Intent("android.appwidget.action.APPWIDGET_UPDATE");
            intent.putExtra("appWidgetIds", iArr);
            intent.setComponent(provider.info.provider);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                provider.broadcast = PendingIntent.getBroadcastAsUser(this.mContext, 1, intent, 134217728, provider.info.getProfile());
                if (BenesseExtension.getDchaState() != 0) {
                    provider.broadcast = null;
                }
                if (!z) {
                    long j = provider.info.updatePeriodMillis;
                    if (j < MIN_UPDATE_PERIOD) {
                        j = MIN_UPDATE_PERIOD;
                    }
                    long j2 = j;
                    jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        this.mAlarmManager.setInexactRepeating(2, SystemClock.elapsedRealtime() + j2, j2, provider.broadcast);
                    } finally {
                    }
                }
            } finally {
            }
        }
    }

    private static int[] getWidgetIds(ArrayList<Widget> arrayList) {
        int size = arrayList.size();
        int[] iArr = new int[size];
        for (int i = 0; i < size; i++) {
            iArr[i] = arrayList.get(i).appWidgetId;
        }
        return iArr;
    }

    private static void dumpProvider(Provider provider, int i, PrintWriter printWriter) {
        AppWidgetProviderInfo appWidgetProviderInfo = provider.info;
        printWriter.print("  [");
        printWriter.print(i);
        printWriter.print("] provider ");
        printWriter.println(provider.id);
        printWriter.print("    min=(");
        printWriter.print(appWidgetProviderInfo.minWidth);
        printWriter.print("x");
        printWriter.print(appWidgetProviderInfo.minHeight);
        printWriter.print(")   minResize=(");
        printWriter.print(appWidgetProviderInfo.minResizeWidth);
        printWriter.print("x");
        printWriter.print(appWidgetProviderInfo.minResizeHeight);
        printWriter.print(") updatePeriodMillis=");
        printWriter.print(appWidgetProviderInfo.updatePeriodMillis);
        printWriter.print(" resizeMode=");
        printWriter.print(appWidgetProviderInfo.resizeMode);
        printWriter.print(" widgetCategory=");
        printWriter.print(appWidgetProviderInfo.widgetCategory);
        printWriter.print(" autoAdvanceViewId=");
        printWriter.print(appWidgetProviderInfo.autoAdvanceViewId);
        printWriter.print(" initialLayout=#");
        printWriter.print(Integer.toHexString(appWidgetProviderInfo.initialLayout));
        printWriter.print(" initialKeyguardLayout=#");
        printWriter.print(Integer.toHexString(appWidgetProviderInfo.initialKeyguardLayout));
        printWriter.print(" zombie=");
        printWriter.println(provider.zombie);
    }

    private static void dumpHost(Host host, int i, PrintWriter printWriter) {
        printWriter.print("  [");
        printWriter.print(i);
        printWriter.print("] hostId=");
        printWriter.println(host.id);
        printWriter.print("    callbacks=");
        printWriter.println(host.callbacks);
        printWriter.print("    widgets.size=");
        printWriter.print(host.widgets.size());
        printWriter.print(" zombie=");
        printWriter.println(host.zombie);
    }

    private static void dumpGrant(Pair<Integer, String> pair, int i, PrintWriter printWriter) {
        printWriter.print("  [");
        printWriter.print(i);
        printWriter.print(']');
        printWriter.print(" user=");
        printWriter.print(pair.first);
        printWriter.print(" package=");
        printWriter.println((String) pair.second);
    }

    private static void dumpWidget(Widget widget, int i, PrintWriter printWriter) {
        printWriter.print("  [");
        printWriter.print(i);
        printWriter.print("] id=");
        printWriter.println(widget.appWidgetId);
        printWriter.print("    host=");
        printWriter.println(widget.host.id);
        if (widget.provider != null) {
            printWriter.print("    provider=");
            printWriter.println(widget.provider.id);
        }
        if (widget.host != null) {
            printWriter.print("    host.callbacks=");
            printWriter.println(widget.host.callbacks);
        }
        if (widget.views != null) {
            printWriter.print("    views=");
            printWriter.println(widget.views);
        }
    }

    private static void serializeProvider(XmlSerializer xmlSerializer, Provider provider) throws IOException {
        xmlSerializer.startTag(null, "p");
        xmlSerializer.attribute(null, "pkg", provider.info.provider.getPackageName());
        xmlSerializer.attribute(null, "cl", provider.info.provider.getClassName());
        xmlSerializer.attribute(null, "tag", Integer.toHexString(provider.tag));
        if (!TextUtils.isEmpty(provider.infoTag)) {
            xmlSerializer.attribute(null, "info_tag", provider.infoTag);
        }
        xmlSerializer.endTag(null, "p");
    }

    private static void serializeHost(XmlSerializer xmlSerializer, Host host) throws IOException {
        xmlSerializer.startTag(null, "h");
        xmlSerializer.attribute(null, "pkg", host.id.packageName);
        xmlSerializer.attribute(null, "id", Integer.toHexString(host.id.hostId));
        xmlSerializer.attribute(null, "tag", Integer.toHexString(host.tag));
        xmlSerializer.endTag(null, "h");
    }

    private static void serializeAppWidget(XmlSerializer xmlSerializer, Widget widget) throws IOException {
        xmlSerializer.startTag(null, "g");
        xmlSerializer.attribute(null, "id", Integer.toHexString(widget.appWidgetId));
        xmlSerializer.attribute(null, "rid", Integer.toHexString(widget.restoredId));
        xmlSerializer.attribute(null, "h", Integer.toHexString(widget.host.tag));
        if (widget.provider != null) {
            xmlSerializer.attribute(null, "p", Integer.toHexString(widget.provider.tag));
        }
        if (widget.options != null) {
            int i = widget.options.getInt("appWidgetMinWidth");
            int i2 = widget.options.getInt("appWidgetMinHeight");
            int i3 = widget.options.getInt("appWidgetMaxWidth");
            int i4 = widget.options.getInt("appWidgetMaxHeight");
            if (i <= 0) {
                i = 0;
            }
            xmlSerializer.attribute(null, "min_width", Integer.toHexString(i));
            if (i2 <= 0) {
                i2 = 0;
            }
            xmlSerializer.attribute(null, "min_height", Integer.toHexString(i2));
            if (i3 <= 0) {
                i3 = 0;
            }
            xmlSerializer.attribute(null, "max_width", Integer.toHexString(i3));
            if (i4 <= 0) {
                i4 = 0;
            }
            xmlSerializer.attribute(null, "max_height", Integer.toHexString(i4));
            xmlSerializer.attribute(null, "host_category", Integer.toHexString(widget.options.getInt("appWidgetCategory")));
        }
        xmlSerializer.endTag(null, "g");
    }

    public List<String> getWidgetParticipants(int i) {
        return this.mBackupRestoreController.getWidgetParticipants(i);
    }

    public byte[] getWidgetState(String str, int i) {
        return this.mBackupRestoreController.getWidgetState(str, i);
    }

    public void restoreStarting(int i) {
        this.mBackupRestoreController.restoreStarting(i);
    }

    public void restoreWidgetState(String str, byte[] bArr, int i) {
        this.mBackupRestoreController.restoreWidgetState(str, bArr, i);
    }

    public void restoreFinished(int i) {
        this.mBackupRestoreController.restoreFinished(i);
    }

    private Provider parseProviderInfoXml(ProviderId providerId, ResolveInfo resolveInfo, Provider provider) {
        AppWidgetProviderInfo appWidgetProviderInfo;
        if (provider != null && !TextUtils.isEmpty(provider.infoTag)) {
            appWidgetProviderInfo = parseAppWidgetProviderInfo(providerId, resolveInfo.activityInfo, provider.infoTag);
        } else {
            appWidgetProviderInfo = null;
        }
        if (appWidgetProviderInfo == null) {
            appWidgetProviderInfo = parseAppWidgetProviderInfo(providerId, resolveInfo.activityInfo, "android.appwidget.provider");
        }
        if (appWidgetProviderInfo == null) {
            return null;
        }
        Provider provider2 = new Provider();
        provider2.id = providerId;
        provider2.info = appWidgetProviderInfo;
        return provider2;
    }

    private AppWidgetProviderInfo parseAppWidgetProviderInfo(ProviderId providerId, ActivityInfo activityInfo, String str) {
        Throwable th;
        int next;
        try {
            XmlResourceParser xmlResourceParserLoadXmlMetaData = activityInfo.loadXmlMetaData(this.mContext.getPackageManager(), str);
            try {
                if (xmlResourceParserLoadXmlMetaData == null) {
                    Slog.w(TAG, "No " + str + " meta-data for AppWidget provider '" + providerId + '\'');
                    if (xmlResourceParserLoadXmlMetaData != null) {
                        $closeResource(null, xmlResourceParserLoadXmlMetaData);
                    }
                    return null;
                }
                AttributeSet attributeSetAsAttributeSet = Xml.asAttributeSet(xmlResourceParserLoadXmlMetaData);
                do {
                    next = xmlResourceParserLoadXmlMetaData.next();
                    if (next == 1) {
                        break;
                    }
                } while (next != 2);
                if (!"appwidget-provider".equals(xmlResourceParserLoadXmlMetaData.getName())) {
                    Slog.w(TAG, "Meta-data does not start with appwidget-provider tag for AppWidget provider " + providerId.componentName + " for user " + providerId.uid);
                    if (xmlResourceParserLoadXmlMetaData != null) {
                        $closeResource(null, xmlResourceParserLoadXmlMetaData);
                    }
                    return null;
                }
                AppWidgetProviderInfo appWidgetProviderInfo = new AppWidgetProviderInfo();
                appWidgetProviderInfo.provider = providerId.componentName;
                appWidgetProviderInfo.providerInfo = activityInfo;
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    PackageManager packageManager = this.mContext.getPackageManager();
                    Resources resourcesForApplication = packageManager.getResourcesForApplication(packageManager.getApplicationInfoAsUser(activityInfo.packageName, 0, UserHandle.getUserId(providerId.uid)));
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    TypedArray typedArrayObtainAttributes = resourcesForApplication.obtainAttributes(attributeSetAsAttributeSet, com.android.internal.R.styleable.AppWidgetProviderInfo);
                    TypedValue typedValuePeekValue = typedArrayObtainAttributes.peekValue(0);
                    appWidgetProviderInfo.minWidth = typedValuePeekValue != null ? typedValuePeekValue.data : 0;
                    TypedValue typedValuePeekValue2 = typedArrayObtainAttributes.peekValue(1);
                    appWidgetProviderInfo.minHeight = typedValuePeekValue2 != null ? typedValuePeekValue2.data : 0;
                    TypedValue typedValuePeekValue3 = typedArrayObtainAttributes.peekValue(8);
                    appWidgetProviderInfo.minResizeWidth = typedValuePeekValue3 != null ? typedValuePeekValue3.data : appWidgetProviderInfo.minWidth;
                    TypedValue typedValuePeekValue4 = typedArrayObtainAttributes.peekValue(9);
                    appWidgetProviderInfo.minResizeHeight = typedValuePeekValue4 != null ? typedValuePeekValue4.data : appWidgetProviderInfo.minHeight;
                    appWidgetProviderInfo.updatePeriodMillis = typedArrayObtainAttributes.getInt(2, 0);
                    appWidgetProviderInfo.initialLayout = typedArrayObtainAttributes.getResourceId(3, 0);
                    appWidgetProviderInfo.initialKeyguardLayout = typedArrayObtainAttributes.getResourceId(10, 0);
                    String string = typedArrayObtainAttributes.getString(4);
                    if (string != null) {
                        appWidgetProviderInfo.configure = new ComponentName(providerId.componentName.getPackageName(), string);
                    }
                    appWidgetProviderInfo.label = activityInfo.loadLabel(this.mContext.getPackageManager()).toString();
                    appWidgetProviderInfo.icon = activityInfo.getIconResource();
                    appWidgetProviderInfo.previewImage = typedArrayObtainAttributes.getResourceId(5, 0);
                    appWidgetProviderInfo.autoAdvanceViewId = typedArrayObtainAttributes.getResourceId(6, -1);
                    appWidgetProviderInfo.resizeMode = typedArrayObtainAttributes.getInt(7, 0);
                    appWidgetProviderInfo.widgetCategory = typedArrayObtainAttributes.getInt(11, 1);
                    appWidgetProviderInfo.widgetFeatures = typedArrayObtainAttributes.getInt(12, 0);
                    typedArrayObtainAttributes.recycle();
                    if (xmlResourceParserLoadXmlMetaData != null) {
                        $closeResource(null, xmlResourceParserLoadXmlMetaData);
                    }
                    return appWidgetProviderInfo;
                } catch (Throwable th2) {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    throw th2;
                }
            } catch (Throwable th3) {
                th = th3;
                th = null;
                if (xmlResourceParserLoadXmlMetaData != null) {
                }
            }
        } catch (PackageManager.NameNotFoundException | IOException | XmlPullParserException e) {
            Slog.w(TAG, "XML parsing failed for AppWidget provider " + providerId.componentName + " for user " + providerId.uid, e);
            return null;
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    private int getUidForPackage(String str, int i) {
        PackageInfo packageInfo;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            packageInfo = this.mPackageManager.getPackageInfo(str, 0, i);
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        } catch (RemoteException e) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            packageInfo = null;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
        if (packageInfo == null || packageInfo.applicationInfo == null) {
            return -1;
        }
        return packageInfo.applicationInfo.uid;
    }

    private ActivityInfo getProviderInfo(ComponentName componentName, int i) {
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_UPDATE");
        intent.setComponent(componentName);
        List<ResolveInfo> listQueryIntentReceivers = queryIntentReceivers(intent, i);
        if (!listQueryIntentReceivers.isEmpty()) {
            return listQueryIntentReceivers.get(0).activityInfo;
        }
        return null;
    }

    private List<ResolveInfo> queryIntentReceivers(Intent intent, int i) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return this.mPackageManager.queryIntentReceivers(intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), (isProfileWithUnlockedParent(i) ? 269222016 : 268435584) | 1024, i).getList();
        } catch (RemoteException e) {
            return Collections.emptyList();
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    void onUserUnlocked(int i) {
        if (isProfileWithLockedParent(i)) {
            return;
        }
        if (!this.mUserManager.isUserUnlockingOrUnlocked(i)) {
            Slog.w(TAG, "User " + i + " is no longer unlocked - exiting");
            return;
        }
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        synchronized (this.mLock) {
            Trace.traceBegin(64L, "appwidget ensure");
            ensureGroupStateLoadedLocked(i);
            Trace.traceEnd(64L);
            Trace.traceBegin(64L, "appwidget reload");
            reloadWidgetsMaskedStateForGroup(this.mSecurityPolicy.getGroupParent(i));
            Trace.traceEnd(64L);
            int size = this.mProviders.size();
            for (int i2 = 0; i2 < size; i2++) {
                Provider provider = this.mProviders.get(i2);
                if (provider.getUserId() == i && provider.widgets.size() > 0) {
                    Trace.traceBegin(64L, "appwidget init " + provider.info.provider.getPackageName());
                    sendEnableIntentLocked(provider);
                    int[] widgetIds = getWidgetIds(provider.widgets);
                    sendUpdateIntentLocked(provider, widgetIds);
                    registerForBroadcastsLocked(provider, widgetIds);
                    Trace.traceEnd(64L);
                }
            }
        }
        Slog.i(TAG, "Async processing of onUserUnlocked u" + i + " took " + (SystemClock.elapsedRealtime() - jElapsedRealtime) + " ms");
    }

    private void loadGroupStateLocked(int[] iArr) throws Exception {
        FileInputStream fileInputStreamOpenRead;
        Throwable th;
        ArrayList arrayList = new ArrayList();
        int i = 0;
        for (int i2 : iArr) {
            try {
                fileInputStreamOpenRead = getSavedStateFile(i2).openRead();
                th = null;
            } catch (IOException e) {
                e = e;
            }
            try {
                int profileStateFromFileLocked = readProfileStateFromFileLocked(fileInputStreamOpenRead, i2, arrayList);
                if (fileInputStreamOpenRead != null) {
                    try {
                        $closeResource(null, fileInputStreamOpenRead);
                    } catch (IOException e2) {
                        e = e2;
                        i = profileStateFromFileLocked;
                        Slog.w(TAG, "Failed to read state: " + e);
                    }
                }
                i = profileStateFromFileLocked;
            } catch (Throwable th2) {
                if (fileInputStreamOpenRead != null) {
                    $closeResource(th, fileInputStreamOpenRead);
                }
                throw th2;
            }
        }
        if (i >= 0) {
            bindLoadedWidgetsLocked(arrayList);
            performUpgradeLocked(i);
            return;
        }
        Slog.w(TAG, "Failed to read state, clearing widgets and hosts.");
        clearWidgetsLocked();
        this.mHosts.clear();
        int size = this.mProviders.size();
        for (int i3 = 0; i3 < size; i3++) {
            this.mProviders.get(i3).widgets.clear();
        }
    }

    private void bindLoadedWidgetsLocked(List<LoadedWidgetState> list) {
        for (int size = list.size() - 1; size >= 0; size--) {
            LoadedWidgetState loadedWidgetStateRemove = list.remove(size);
            Widget widget = loadedWidgetStateRemove.widget;
            widget.provider = findProviderByTag(loadedWidgetStateRemove.providerTag);
            if (widget.provider != null) {
                widget.host = findHostByTag(loadedWidgetStateRemove.hostTag);
                if (widget.host != null) {
                    widget.provider.widgets.add(widget);
                    widget.host.widgets.add(widget);
                    addWidgetLocked(widget);
                }
            }
        }
    }

    private Provider findProviderByTag(int i) {
        if (i < 0) {
            return null;
        }
        int size = this.mProviders.size();
        for (int i2 = 0; i2 < size; i2++) {
            Provider provider = this.mProviders.get(i2);
            if (provider.tag == i) {
                return provider;
            }
        }
        return null;
    }

    private Host findHostByTag(int i) {
        if (i < 0) {
            return null;
        }
        int size = this.mHosts.size();
        for (int i2 = 0; i2 < size; i2++) {
            Host host = this.mHosts.get(i2);
            if (host.tag == i) {
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
        ArraySet<String> arraySet = this.mWidgetPackages.get(userId);
        if (arraySet == null) {
            SparseArray<ArraySet<String>> sparseArray = this.mWidgetPackages;
            ArraySet<String> arraySet2 = new ArraySet<>();
            sparseArray.put(userId, arraySet2);
            arraySet = arraySet2;
        }
        arraySet.add(widget.provider.info.provider.getPackageName());
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
        ArraySet<String> arraySet = this.mWidgetPackages.get(userId);
        if (arraySet == null) {
            return;
        }
        int size = this.mWidgets.size();
        for (int i = 0; i < size; i++) {
            Widget widget2 = this.mWidgets.get(i);
            if (widget2.provider != null && widget2.provider.getUserId() == userId && packageName.equals(widget2.provider.info.provider.getPackageName())) {
                return;
            }
        }
        arraySet.remove(packageName);
    }

    void clearWidgetsLocked() {
        this.mWidgets.clear();
        onWidgetsClearedLocked();
    }

    private void onWidgetsClearedLocked() {
        this.mWidgetPackages.clear();
    }

    public boolean isBoundWidgetPackage(String str, int i) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system process can call this");
        }
        synchronized (this.mLock) {
            ArraySet<String> arraySet = this.mWidgetPackages.get(i);
            if (arraySet != null) {
                return arraySet.contains(str);
            }
            return false;
        }
    }

    private void saveStateLocked(int i) {
        tagProvidersAndHosts();
        for (int i2 : this.mSecurityPolicy.getEnabledGroupProfileIds(i)) {
            AtomicFile savedStateFile = getSavedStateFile(i2);
            try {
                FileOutputStream fileOutputStreamStartWrite = savedStateFile.startWrite();
                if (writeProfileStateToFileLocked(fileOutputStreamStartWrite, i2)) {
                    savedStateFile.finishWrite(fileOutputStreamStartWrite);
                } else {
                    savedStateFile.failWrite(fileOutputStreamStartWrite);
                    Slog.w(TAG, "Failed to save state, restoring backup.");
                }
            } catch (IOException e) {
                Slog.w(TAG, "Failed open state file for write: " + e);
            }
        }
    }

    private void tagProvidersAndHosts() {
        int size = this.mProviders.size();
        for (int i = 0; i < size; i++) {
            this.mProviders.get(i).tag = i;
        }
        int size2 = this.mHosts.size();
        for (int i2 = 0; i2 < size2; i2++) {
            this.mHosts.get(i2).tag = i2;
        }
    }

    private void clearProvidersAndHostsTagsLocked() {
        int size = this.mProviders.size();
        for (int i = 0; i < size; i++) {
            this.mProviders.get(i).tag = -1;
        }
        int size2 = this.mHosts.size();
        for (int i2 = 0; i2 < size2; i2++) {
            this.mHosts.get(i2).tag = -1;
        }
    }

    private boolean writeProfileStateToFileLocked(FileOutputStream fileOutputStream, int i) {
        try {
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(fileOutputStream, StandardCharsets.UTF_8.name());
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.startTag(null, "gs");
            fastXmlSerializer.attribute(null, "version", String.valueOf(1));
            int size = this.mProviders.size();
            for (int i2 = 0; i2 < size; i2++) {
                Provider provider = this.mProviders.get(i2);
                if (provider.getUserId() == i && provider.shouldBePersisted()) {
                    serializeProvider(fastXmlSerializer, provider);
                }
            }
            int size2 = this.mHosts.size();
            for (int i3 = 0; i3 < size2; i3++) {
                Host host = this.mHosts.get(i3);
                if (host.getUserId() == i) {
                    serializeHost(fastXmlSerializer, host);
                }
            }
            int size3 = this.mWidgets.size();
            for (int i4 = 0; i4 < size3; i4++) {
                Widget widget = this.mWidgets.get(i4);
                if (widget.host.getUserId() == i) {
                    serializeAppWidget(fastXmlSerializer, widget);
                }
            }
            for (Pair<Integer, String> pair : this.mPackagesWithBindWidgetPermission) {
                if (((Integer) pair.first).intValue() == i) {
                    fastXmlSerializer.startTag(null, "b");
                    fastXmlSerializer.attribute(null, BackupManagerService.BACKUP_FINISHED_PACKAGE_EXTRA, (String) pair.second);
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

    private int readProfileStateFromFileLocked(FileInputStream fileInputStream, int i, List<LoadedWidgetState> list) {
        int next;
        int i2;
        int i3;
        int uidForPackage;
        ComponentName componentName;
        ActivityInfo providerInfo;
        int i4;
        AppWidgetProviderInfo appWidgetProviderInfo;
        try {
            XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
            xmlPullParserNewPullParser.setInput(fileInputStream, StandardCharsets.UTF_8.name());
            int i5 = -1;
            int i6 = -1;
            int i7 = -1;
            do {
                next = xmlPullParserNewPullParser.next();
                if (next == 2) {
                    String name = xmlPullParserNewPullParser.getName();
                    int i8 = 0;
                    if ("gs".equals(name)) {
                        try {
                            i8 = Integer.parseInt(xmlPullParserNewPullParser.getAttributeValue(null, "version"));
                        } catch (NumberFormatException e) {
                        }
                        i5 = i8;
                    } else if ("p".equals(name)) {
                        i6++;
                        String attributeValue = xmlPullParserNewPullParser.getAttributeValue(null, "pkg");
                        String attributeValue2 = xmlPullParserNewPullParser.getAttributeValue(null, "cl");
                        String canonicalPackageName = getCanonicalPackageName(attributeValue, attributeValue2, i);
                        if (canonicalPackageName != null && (uidForPackage = getUidForPackage(canonicalPackageName, i)) >= 0 && (providerInfo = getProviderInfo((componentName = new ComponentName(canonicalPackageName, attributeValue2)), i)) != null) {
                            ProviderId providerId = new ProviderId(uidForPackage, componentName);
                            Provider providerLookupProviderLocked = lookupProviderLocked(providerId);
                            if (providerLookupProviderLocked == null && this.mSafeMode) {
                                providerLookupProviderLocked = new Provider();
                                providerLookupProviderLocked.info = new AppWidgetProviderInfo();
                                providerLookupProviderLocked.info.provider = providerId.componentName;
                                providerLookupProviderLocked.info.providerInfo = providerInfo;
                                providerLookupProviderLocked.zombie = true;
                                providerLookupProviderLocked.id = providerId;
                                this.mProviders.add(providerLookupProviderLocked);
                            }
                            String attributeValue3 = xmlPullParserNewPullParser.getAttributeValue(null, "tag");
                            if (!TextUtils.isEmpty(attributeValue3)) {
                                i4 = Integer.parseInt(attributeValue3, 16);
                            } else {
                                i4 = i6;
                            }
                            providerLookupProviderLocked.tag = i4;
                            providerLookupProviderLocked.infoTag = xmlPullParserNewPullParser.getAttributeValue(null, "info_tag");
                            if (!TextUtils.isEmpty(providerLookupProviderLocked.infoTag) && !this.mSafeMode && (appWidgetProviderInfo = parseAppWidgetProviderInfo(providerId, providerInfo, providerLookupProviderLocked.infoTag)) != null) {
                                providerLookupProviderLocked.info = appWidgetProviderInfo;
                            }
                        }
                    } else if ("h".equals(name)) {
                        i7++;
                        Host host = new Host();
                        String attributeValue4 = xmlPullParserNewPullParser.getAttributeValue(null, "pkg");
                        int uidForPackage2 = getUidForPackage(attributeValue4, i);
                        if (uidForPackage2 < 0) {
                            host.zombie = true;
                        }
                        if (!host.zombie || this.mSafeMode) {
                            int i9 = Integer.parseInt(xmlPullParserNewPullParser.getAttributeValue(null, "id"), 16);
                            String attributeValue5 = xmlPullParserNewPullParser.getAttributeValue(null, "tag");
                            if (!TextUtils.isEmpty(attributeValue5)) {
                                i3 = Integer.parseInt(attributeValue5, 16);
                            } else {
                                i3 = i7;
                            }
                            host.tag = i3;
                            host.id = new HostId(uidForPackage2, i9, attributeValue4);
                            this.mHosts.add(host);
                        }
                    } else if ("b".equals(name)) {
                        String attributeValue6 = xmlPullParserNewPullParser.getAttributeValue(null, BackupManagerService.BACKUP_FINISHED_PACKAGE_EXTRA);
                        if (getUidForPackage(attributeValue6, i) >= 0) {
                            this.mPackagesWithBindWidgetPermission.add(Pair.create(Integer.valueOf(i), attributeValue6));
                        }
                    } else if ("g".equals(name)) {
                        Widget widget = new Widget();
                        widget.appWidgetId = Integer.parseInt(xmlPullParserNewPullParser.getAttributeValue(null, "id"), 16);
                        setMinAppWidgetIdLocked(i, widget.appWidgetId + 1);
                        String attributeValue7 = xmlPullParserNewPullParser.getAttributeValue(null, "rid");
                        if (attributeValue7 != null) {
                            i8 = Integer.parseInt(attributeValue7, 16);
                        }
                        widget.restoredId = i8;
                        Bundle bundle = new Bundle();
                        String attributeValue8 = xmlPullParserNewPullParser.getAttributeValue(null, "min_width");
                        if (attributeValue8 != null) {
                            bundle.putInt("appWidgetMinWidth", Integer.parseInt(attributeValue8, 16));
                        }
                        String attributeValue9 = xmlPullParserNewPullParser.getAttributeValue(null, "min_height");
                        if (attributeValue9 != null) {
                            bundle.putInt("appWidgetMinHeight", Integer.parseInt(attributeValue9, 16));
                        }
                        String attributeValue10 = xmlPullParserNewPullParser.getAttributeValue(null, "max_width");
                        if (attributeValue10 != null) {
                            bundle.putInt("appWidgetMaxWidth", Integer.parseInt(attributeValue10, 16));
                        }
                        String attributeValue11 = xmlPullParserNewPullParser.getAttributeValue(null, "max_height");
                        if (attributeValue11 != null) {
                            bundle.putInt("appWidgetMaxHeight", Integer.parseInt(attributeValue11, 16));
                        }
                        String attributeValue12 = xmlPullParserNewPullParser.getAttributeValue(null, "host_category");
                        if (attributeValue12 != null) {
                            bundle.putInt("appWidgetCategory", Integer.parseInt(attributeValue12, 16));
                        }
                        widget.options = bundle;
                        int i10 = Integer.parseInt(xmlPullParserNewPullParser.getAttributeValue(null, "h"), 16);
                        if (xmlPullParserNewPullParser.getAttributeValue(null, "p") != null) {
                            i2 = Integer.parseInt(xmlPullParserNewPullParser.getAttributeValue(null, "p"), 16);
                        } else {
                            i2 = -1;
                        }
                        list.add(new LoadedWidgetState(widget, i10, i2));
                    }
                }
            } while (next != 1);
            return i5;
        } catch (IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException e2) {
            Slog.w(TAG, "failed parsing " + e2);
            return -1;
        }
    }

    private void performUpgradeLocked(int i) {
        int uidForPackage;
        if (i < 1) {
            Slog.v(TAG, "Upgrading widget database from " + i + " to 1");
        }
        if (i == 0) {
            Host hostLookupHostLocked = lookupHostLocked(new HostId(Process.myUid(), KEYGUARD_HOST_ID, "android"));
            if (hostLookupHostLocked != null && (uidForPackage = getUidForPackage(NEW_KEYGUARD_HOST_PACKAGE, 0)) >= 0) {
                hostLookupHostLocked.id = new HostId(uidForPackage, KEYGUARD_HOST_ID, NEW_KEYGUARD_HOST_PACKAGE);
            }
            i = 1;
        }
        if (i != 1) {
            throw new IllegalStateException("Failed to upgrade widget database");
        }
    }

    private static File getStateFile(int i) {
        return new File(Environment.getUserSystemDirectory(i), STATE_FILENAME);
    }

    private static AtomicFile getSavedStateFile(int i) {
        File userSystemDirectory = Environment.getUserSystemDirectory(i);
        File stateFile = getStateFile(i);
        if (!stateFile.exists() && i == 0) {
            if (!userSystemDirectory.exists()) {
                userSystemDirectory.mkdirs();
            }
            new File("/data/system/appwidgets.xml").renameTo(stateFile);
        }
        return new AtomicFile(stateFile);
    }

    void onUserStopped(int i) {
        boolean z;
        synchronized (this.mLock) {
            int size = this.mWidgets.size() - 1;
            while (true) {
                z = false;
                if (size < 0) {
                    break;
                }
                Widget widget = this.mWidgets.get(size);
                boolean z2 = widget.host.getUserId() == i;
                boolean z3 = widget.provider != null;
                if (z3 && widget.provider.getUserId() == i) {
                    z = true;
                }
                if (z2 && (!z3 || z)) {
                    removeWidgetLocked(widget);
                    widget.host.widgets.remove(widget);
                    widget.host = null;
                    if (z3) {
                        widget.provider.widgets.remove(widget);
                        widget.provider = null;
                    }
                }
                size--;
            }
            for (int size2 = this.mHosts.size() - 1; size2 >= 0; size2--) {
                Host host = this.mHosts.get(size2);
                if (host.getUserId() == i) {
                    z |= !host.widgets.isEmpty();
                    deleteHostLocked(host);
                }
            }
            for (int size3 = this.mPackagesWithBindWidgetPermission.size() - 1; size3 >= 0; size3--) {
                if (((Integer) this.mPackagesWithBindWidgetPermission.valueAt(size3).first).intValue() == i) {
                    this.mPackagesWithBindWidgetPermission.removeAt(size3);
                }
            }
            int iIndexOfKey = this.mLoadedUserIds.indexOfKey(i);
            if (iIndexOfKey >= 0) {
                this.mLoadedUserIds.removeAt(iIndexOfKey);
            }
            int iIndexOfKey2 = this.mNextAppWidgetIds.indexOfKey(i);
            if (iIndexOfKey2 >= 0) {
                this.mNextAppWidgetIds.removeAt(iIndexOfKey2);
            }
            if (z) {
                saveGroupStateAsync(i);
            }
        }
    }

    private boolean updateProvidersForPackageLocked(String str, int i, Set<ProviderId> set) {
        HashSet hashSet = new HashSet();
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_UPDATE");
        intent.setPackage(str);
        List<ResolveInfo> listQueryIntentReceivers = queryIntentReceivers(intent, i);
        int size = listQueryIntentReceivers == null ? 0 : listQueryIntentReceivers.size();
        boolean z = false;
        for (int i2 = 0; i2 < size; i2++) {
            ResolveInfo resolveInfo = listQueryIntentReceivers.get(i2);
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            if ((activityInfo.applicationInfo.flags & DumpState.DUMP_DOMAIN_PREFERRED) == 0 && str.equals(activityInfo.packageName)) {
                ProviderId providerId = new ProviderId(activityInfo.applicationInfo.uid, new ComponentName(activityInfo.packageName, activityInfo.name));
                Provider providerLookupProviderLocked = lookupProviderLocked(providerId);
                if (providerLookupProviderLocked != null) {
                    Provider providerInfoXml = parseProviderInfoXml(providerId, resolveInfo, providerLookupProviderLocked);
                    if (providerInfoXml != null) {
                        hashSet.add(providerId);
                        providerLookupProviderLocked.info = providerInfoXml.info;
                        int size2 = providerLookupProviderLocked.widgets.size();
                        if (size2 > 0) {
                            int[] widgetIds = getWidgetIds(providerLookupProviderLocked.widgets);
                            cancelBroadcasts(providerLookupProviderLocked);
                            registerForBroadcastsLocked(providerLookupProviderLocked, widgetIds);
                            for (int i3 = 0; i3 < size2; i3++) {
                                Widget widget = providerLookupProviderLocked.widgets.get(i3);
                                widget.views = null;
                                scheduleNotifyProviderChangedLocked(widget);
                            }
                            sendUpdateIntentLocked(providerLookupProviderLocked, widgetIds);
                        }
                    }
                } else if (addProviderLocked(resolveInfo)) {
                    hashSet.add(providerId);
                }
                z = true;
            }
        }
        for (int size3 = this.mProviders.size() - 1; size3 >= 0; size3--) {
            Provider provider = this.mProviders.get(size3);
            if (str.equals(provider.info.provider.getPackageName()) && provider.getUserId() == i && !hashSet.contains(provider.id)) {
                if (set != null) {
                    set.add(provider.id);
                }
                deleteProviderLocked(provider);
                z = true;
            }
        }
        return z;
    }

    private void removeWidgetsForPackageLocked(String str, int i, int i2) {
        int size = this.mProviders.size();
        for (int i3 = 0; i3 < size; i3++) {
            Provider provider = this.mProviders.get(i3);
            if (str.equals(provider.info.provider.getPackageName()) && provider.getUserId() == i && provider.widgets.size() > 0) {
                deleteWidgetsLocked(provider, i2);
            }
        }
    }

    private boolean removeProvidersForPackageLocked(String str, int i) {
        boolean z = false;
        for (int size = this.mProviders.size() - 1; size >= 0; size--) {
            Provider provider = this.mProviders.get(size);
            if (str.equals(provider.info.provider.getPackageName()) && provider.getUserId() == i) {
                deleteProviderLocked(provider);
                z = true;
            }
        }
        return z;
    }

    private boolean removeHostsAndProvidersForPackageLocked(String str, int i) {
        boolean zRemoveProvidersForPackageLocked = removeProvidersForPackageLocked(str, i);
        for (int size = this.mHosts.size() - 1; size >= 0; size--) {
            Host host = this.mHosts.get(size);
            if (str.equals(host.id.packageName) && host.getUserId() == i) {
                deleteHostLocked(host);
                zRemoveProvidersForPackageLocked = true;
            }
        }
        return zRemoveProvidersForPackageLocked;
    }

    private String getCanonicalPackageName(String str, String str2, int i) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            AppGlobals.getPackageManager().getReceiverInfo(new ComponentName(str, str2), 0, i);
            return str;
        } catch (RemoteException e) {
            String[] strArrCurrentToCanonicalPackageNames = this.mContext.getPackageManager().currentToCanonicalPackageNames(new String[]{str});
            if (strArrCurrentToCanonicalPackageNames != null && strArrCurrentToCanonicalPackageNames.length > 0) {
                return strArrCurrentToCanonicalPackageNames[0];
            }
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            return null;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void sendBroadcastAsUser(Intent intent, UserHandle userHandle) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mContext.sendBroadcastAsUser(intent, userHandle);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void bindService(Intent intent, ServiceConnection serviceConnection, UserHandle userHandle) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mContext.bindServiceAsUser(intent, serviceConnection, 33554433, userHandle);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void unbindService(ServiceConnection serviceConnection) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mContext.unbindService(serviceConnection);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void onCrossProfileWidgetProvidersChanged(int i, List<String> list) {
        int profileParent = this.mSecurityPolicy.getProfileParent(i);
        if (profileParent != i) {
            synchronized (this.mLock) {
                ArraySet arraySet = new ArraySet();
                int size = this.mProviders.size();
                for (int i2 = 0; i2 < size; i2++) {
                    Provider provider = this.mProviders.get(i2);
                    if (provider.getUserId() == i) {
                        arraySet.add(provider.id.componentName.getPackageName());
                    }
                }
                int size2 = list.size();
                boolean zUpdateProvidersForPackageLocked = false;
                for (int i3 = 0; i3 < size2; i3++) {
                    String str = list.get(i3);
                    arraySet.remove(str);
                    zUpdateProvidersForPackageLocked |= updateProvidersForPackageLocked(str, i, null);
                }
                int size3 = arraySet.size();
                for (int i4 = 0; i4 < size3; i4++) {
                    removeWidgetsForPackageLocked((String) arraySet.valueAt(i4), i, profileParent);
                }
                if (zUpdateProvidersForPackageLocked || size3 > 0) {
                    saveGroupStateAsync(i);
                    scheduleNotifyGroupHostsForProvidersChangedLocked(i);
                }
            }
        }
    }

    public List<ComponentName> getAppWidgetOfHost(String str, int i) {
        ArrayList arrayList = new ArrayList();
        for (int i2 = 0; i2 < this.mHosts.size(); i2++) {
            Host host = this.mHosts.get(i2);
            if (UserHandle.getUserId(host.id.uid) == i && str.equals(host.id.packageName)) {
                for (int i3 = 0; i3 < host.widgets.size(); i3++) {
                    if (host.widgets.get(i3).provider != null) {
                        arrayList.add(host.widgets.get(i3).provider.id.componentName);
                    }
                }
            }
        }
        return arrayList;
    }

    private boolean isProfileWithLockedParent(int i) {
        UserInfo profileParent;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            UserInfo userInfo = this.mUserManager.getUserInfo(i);
            if (userInfo != null && userInfo.isManagedProfile() && (profileParent = this.mUserManager.getProfileParent(i)) != null) {
                if (!isUserRunningAndUnlocked(profileParent.getUserHandle().getIdentifier())) {
                    return true;
                }
            }
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            return false;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private boolean isProfileWithUnlockedParent(int i) {
        UserInfo profileParent;
        UserInfo userInfo = this.mUserManager.getUserInfo(i);
        if (userInfo != null && userInfo.isManagedProfile() && (profileParent = this.mUserManager.getProfileParent(i)) != null && this.mUserManager.isUserUnlockingOrUnlocked(profileParent.getUserHandle())) {
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
                    SomeArgs someArgs = (SomeArgs) message.obj;
                    Host host = (Host) someArgs.arg1;
                    IAppWidgetHost iAppWidgetHost = (IAppWidgetHost) someArgs.arg2;
                    RemoteViews remoteViews = (RemoteViews) someArgs.arg3;
                    long jLongValue = ((Long) someArgs.arg4).longValue();
                    int i = someArgs.argi1;
                    someArgs.recycle();
                    AppWidgetServiceImpl.this.handleNotifyUpdateAppWidget(host, iAppWidgetHost, i, remoteViews, jLongValue);
                    break;
                case 2:
                    SomeArgs someArgs2 = (SomeArgs) message.obj;
                    Host host2 = (Host) someArgs2.arg1;
                    IAppWidgetHost iAppWidgetHost2 = (IAppWidgetHost) someArgs2.arg2;
                    AppWidgetProviderInfo appWidgetProviderInfo = (AppWidgetProviderInfo) someArgs2.arg3;
                    long jLongValue2 = ((Long) someArgs2.arg4).longValue();
                    int i2 = someArgs2.argi1;
                    someArgs2.recycle();
                    AppWidgetServiceImpl.this.handleNotifyProviderChanged(host2, iAppWidgetHost2, i2, appWidgetProviderInfo, jLongValue2);
                    break;
                case 3:
                    SomeArgs someArgs3 = (SomeArgs) message.obj;
                    Host host3 = (Host) someArgs3.arg1;
                    IAppWidgetHost iAppWidgetHost3 = (IAppWidgetHost) someArgs3.arg2;
                    someArgs3.recycle();
                    AppWidgetServiceImpl.this.handleNotifyProvidersChanged(host3, iAppWidgetHost3);
                    break;
                case 4:
                    SomeArgs someArgs4 = (SomeArgs) message.obj;
                    Host host4 = (Host) someArgs4.arg1;
                    IAppWidgetHost iAppWidgetHost4 = (IAppWidgetHost) someArgs4.arg2;
                    long jLongValue3 = ((Long) someArgs4.arg3).longValue();
                    int i3 = someArgs4.argi1;
                    int i4 = someArgs4.argi2;
                    someArgs4.recycle();
                    AppWidgetServiceImpl.this.handleNotifyAppWidgetViewDataChanged(host4, iAppWidgetHost4, i3, i4, jLongValue3);
                    break;
            }
        }
    }

    private final class SecurityPolicy {
        private SecurityPolicy() {
        }

        public boolean isEnabledGroupProfile(int i) {
            return isParentOrProfile(UserHandle.getCallingUserId(), i) && isProfileEnabled(i);
        }

        public int[] getEnabledGroupProfileIds(int i) {
            int groupParent = getGroupParent(i);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return AppWidgetServiceImpl.this.mUserManager.getEnabledProfileIds(groupParent);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void enforceServiceExistsAndRequiresBindRemoteViewsPermission(ComponentName componentName, int i) {
            ServiceInfo serviceInfo;
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                serviceInfo = AppWidgetServiceImpl.this.mPackageManager.getServiceInfo(componentName, 4096, i);
            } catch (RemoteException e) {
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
            if (serviceInfo == null) {
                throw new SecurityException("Service " + componentName + " not installed for user " + i);
            }
            if (!"android.permission.BIND_REMOTEVIEWS".equals(serviceInfo.permission)) {
                throw new SecurityException("Service " + componentName + " in user " + i + "does not require android.permission.BIND_REMOTEVIEWS");
            }
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }

        public void enforceModifyAppWidgetBindPermissions(String str) {
            AppWidgetServiceImpl.this.mContext.enforceCallingPermission("android.permission.MODIFY_APPWIDGET_BIND_PERMISSIONS", "hasBindAppWidgetPermission packageName=" + str);
        }

        public boolean isCallerInstantAppLocked() {
            int callingUid = Binder.getCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                String[] packagesForUid = AppWidgetServiceImpl.this.mPackageManager.getPackagesForUid(callingUid);
                if (!ArrayUtils.isEmpty(packagesForUid)) {
                    boolean zIsInstantApp = AppWidgetServiceImpl.this.mPackageManager.isInstantApp(packagesForUid[0], UserHandle.getCallingUserId());
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    return zIsInstantApp;
                }
            } catch (RemoteException e) {
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            return false;
        }

        public boolean isInstantAppLocked(String str, int i) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                boolean zIsInstantApp = AppWidgetServiceImpl.this.mPackageManager.isInstantApp(str, i);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                return zIsInstantApp;
            } catch (RemoteException e) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                return false;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        }

        public void enforceCallFromPackage(String str) {
            AppWidgetServiceImpl.this.mAppOpsManager.checkPackage(Binder.getCallingUid(), str);
        }

        public boolean hasCallerBindPermissionOrBindWhiteListedLocked(String str) {
            try {
                AppWidgetServiceImpl.this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_APPWIDGET", null);
                return true;
            } catch (SecurityException e) {
                if (!isCallerBindAppWidgetWhiteListedLocked(str)) {
                    return false;
                }
                return true;
            }
        }

        private boolean isCallerBindAppWidgetWhiteListedLocked(String str) {
            int callingUserId = UserHandle.getCallingUserId();
            if (AppWidgetServiceImpl.this.getUidForPackage(str, callingUserId) >= 0) {
                synchronized (AppWidgetServiceImpl.this.mLock) {
                    AppWidgetServiceImpl.this.ensureGroupStateLoadedLocked(callingUserId);
                    if (AppWidgetServiceImpl.this.mPackagesWithBindWidgetPermission.contains(Pair.create(Integer.valueOf(callingUserId), str))) {
                        return true;
                    }
                    return false;
                }
            }
            throw new IllegalArgumentException("No package " + str + " for user " + callingUserId);
        }

        public boolean canAccessAppWidget(Widget widget, int i, String str) {
            if (isHostInPackageForUid(widget.host, i, str) || isProviderInPackageForUid(widget.provider, i, str) || isHostAccessingProvider(widget.host, widget.provider, i, str)) {
                return true;
            }
            int userId = UserHandle.getUserId(i);
            return (widget.host.getUserId() == userId || (widget.provider != null && widget.provider.getUserId() == userId)) && AppWidgetServiceImpl.this.mContext.checkCallingPermission("android.permission.BIND_APPWIDGET") == 0;
        }

        private boolean isParentOrProfile(int i, int i2) {
            return i == i2 || getProfileParent(i2) == i;
        }

        public boolean isProviderInCallerOrInProfileAndWhitelListed(String str, int i) {
            int callingUserId = UserHandle.getCallingUserId();
            if (i == callingUserId) {
                return true;
            }
            if (getProfileParent(i) != callingUserId) {
                return false;
            }
            return isProviderWhiteListed(str, i);
        }

        public boolean isProviderWhiteListed(String str, int i) {
            if (AppWidgetServiceImpl.this.mDevicePolicyManagerInternal == null) {
                return false;
            }
            return AppWidgetServiceImpl.this.mDevicePolicyManagerInternal.getCrossProfileWidgetProviders(i).contains(str);
        }

        public int getProfileParent(int i) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                UserInfo profileParent = AppWidgetServiceImpl.this.mUserManager.getProfileParent(i);
                if (profileParent != null) {
                    return profileParent.getUserHandle().getIdentifier();
                }
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                return -10;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public int getGroupParent(int i) {
            int profileParent = AppWidgetServiceImpl.this.mSecurityPolicy.getProfileParent(i);
            return profileParent != -10 ? profileParent : i;
        }

        public boolean isHostInPackageForUid(Host host, int i, String str) {
            return host.id.uid == i && host.id.packageName.equals(str);
        }

        public boolean isProviderInPackageForUid(Provider provider, int i, String str) {
            return provider != null && provider.id.uid == i && provider.id.componentName.getPackageName().equals(str);
        }

        public boolean isHostAccessingProvider(Host host, Provider provider, int i, String str) {
            return host.id.uid == i && provider != null && provider.id.componentName.getPackageName().equals(str);
        }

        private boolean isProfileEnabled(int i) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                UserInfo userInfo = AppWidgetServiceImpl.this.mUserManager.getUserInfo(i);
                if (userInfo != null) {
                    if (userInfo.isEnabled()) {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                        return true;
                    }
                }
                return false;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    private static final class Provider {
        PendingIntent broadcast;
        ProviderId id;
        AppWidgetProviderInfo info;
        String infoTag;
        boolean maskedByLockedProfile;
        boolean maskedByQuietProfile;
        boolean maskedBySuspendedPackage;
        int tag;
        ArrayList<Widget> widgets;
        boolean zombie;

        private Provider() {
            this.widgets = new ArrayList<>();
            this.tag = -1;
        }

        public int getUserId() {
            return UserHandle.getUserId(this.id.uid);
        }

        public boolean isInPackageForUser(String str, int i) {
            return getUserId() == i && this.id.componentName.getPackageName().equals(str);
        }

        public boolean hostedByPackageForUser(String str, int i) {
            int size = this.widgets.size();
            for (int i2 = 0; i2 < size; i2++) {
                Widget widget = this.widgets.get(i2);
                if (str.equals(widget.host.id.packageName) && widget.host.getUserId() == i) {
                    return true;
                }
            }
            return false;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Provider{");
            sb.append(this.id);
            sb.append(this.zombie ? " Z" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            sb.append('}');
            return sb.toString();
        }

        public boolean setMaskedByQuietProfileLocked(boolean z) {
            boolean z2 = this.maskedByQuietProfile;
            this.maskedByQuietProfile = z;
            return z != z2;
        }

        public boolean setMaskedByLockedProfileLocked(boolean z) {
            boolean z2 = this.maskedByLockedProfile;
            this.maskedByLockedProfile = z;
            return z != z2;
        }

        public boolean setMaskedBySuspendedPackageLocked(boolean z) {
            boolean z2 = this.maskedBySuspendedPackage;
            this.maskedBySuspendedPackage = z;
            return z != z2;
        }

        public boolean isMaskedLocked() {
            return this.maskedByQuietProfile || this.maskedByLockedProfile || this.maskedBySuspendedPackage;
        }

        public boolean shouldBePersisted() {
            return (this.widgets.isEmpty() && TextUtils.isEmpty(this.infoTag)) ? false : true;
        }
    }

    private static final class ProviderId {
        final ComponentName componentName;
        final int uid;

        private ProviderId(int i, ComponentName componentName) {
            this.uid = i;
            this.componentName = componentName;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            ProviderId providerId = (ProviderId) obj;
            if (this.uid != providerId.uid) {
                return false;
            }
            if (this.componentName == null) {
                if (providerId.componentName != null) {
                    return false;
                }
            } else if (!this.componentName.equals(providerId.componentName)) {
                return false;
            }
            return true;
        }

        public int hashCode() {
            return (31 * this.uid) + (this.componentName != null ? this.componentName.hashCode() : 0);
        }

        public String toString() {
            return "ProviderId{user:" + UserHandle.getUserId(this.uid) + ", app:" + UserHandle.getAppId(this.uid) + ", cmp:" + this.componentName + '}';
        }
    }

    private static final class Host {
        IAppWidgetHost callbacks;
        HostId id;
        long lastWidgetUpdateSequenceNo;
        int tag;
        ArrayList<Widget> widgets;
        boolean zombie;

        private Host() {
            this.widgets = new ArrayList<>();
            this.tag = -1;
        }

        public int getUserId() {
            return UserHandle.getUserId(this.id.uid);
        }

        public boolean isInPackageForUser(String str, int i) {
            return getUserId() == i && this.id.packageName.equals(str);
        }

        private boolean hostsPackageForUser(String str, int i) {
            int size = this.widgets.size();
            for (int i2 = 0; i2 < size; i2++) {
                Provider provider = this.widgets.get(i2).provider;
                if (provider != null && provider.getUserId() == i && provider.info != null && str.equals(provider.info.provider.getPackageName())) {
                    return true;
                }
            }
            return false;
        }

        public boolean getPendingUpdatesForId(int i, LongSparseArray<PendingHostUpdate> longSparseArray) {
            PendingHostUpdate pendingHostUpdateUpdateAppWidget;
            long j = this.lastWidgetUpdateSequenceNo;
            int size = this.widgets.size();
            for (int i2 = 0; i2 < size; i2++) {
                Widget widget = this.widgets.get(i2);
                if (widget.appWidgetId == i) {
                    longSparseArray.clear();
                    for (int size2 = widget.updateSequenceNos.size() - 1; size2 >= 0; size2--) {
                        long jValueAt = widget.updateSequenceNos.valueAt(size2);
                        if (jValueAt > j) {
                            int iKeyAt = widget.updateSequenceNos.keyAt(size2);
                            switch (iKeyAt) {
                                case 0:
                                    pendingHostUpdateUpdateAppWidget = PendingHostUpdate.updateAppWidget(i, AppWidgetServiceImpl.cloneIfLocalBinder(widget.getEffectiveViewsLocked()));
                                    break;
                                case 1:
                                    pendingHostUpdateUpdateAppWidget = PendingHostUpdate.providerChanged(i, widget.provider.info);
                                    break;
                                default:
                                    pendingHostUpdateUpdateAppWidget = PendingHostUpdate.viewDataChanged(i, iKeyAt);
                                    break;
                            }
                            longSparseArray.put(jValueAt, pendingHostUpdateUpdateAppWidget);
                        }
                    }
                    return true;
                }
            }
            return false;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Host{");
            sb.append(this.id);
            sb.append(this.zombie ? " Z" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            sb.append('}');
            return sb.toString();
        }
    }

    private static final class HostId {
        final int hostId;
        final String packageName;
        final int uid;

        public HostId(int i, int i2, String str) {
            this.uid = i;
            this.hostId = i2;
            this.packageName = str;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            HostId hostId = (HostId) obj;
            if (this.uid != hostId.uid || this.hostId != hostId.hostId) {
                return false;
            }
            if (this.packageName == null) {
                if (hostId.packageName != null) {
                    return false;
                }
            } else if (!this.packageName.equals(hostId.packageName)) {
                return false;
            }
            return true;
        }

        public int hashCode() {
            return (31 * ((this.uid * 31) + this.hostId)) + (this.packageName != null ? this.packageName.hashCode() : 0);
        }

        public String toString() {
            return "HostId{user:" + UserHandle.getUserId(this.uid) + ", app:" + UserHandle.getAppId(this.uid) + ", hostId:" + this.hostId + ", pkg:" + this.packageName + '}';
        }
    }

    private static final class Widget {
        int appWidgetId;
        Host host;
        RemoteViews maskedViews;
        Bundle options;
        Provider provider;
        int restoredId;
        SparseLongArray updateSequenceNos;
        RemoteViews views;

        private Widget() {
            this.updateSequenceNos = new SparseLongArray(2);
        }

        public String toString() {
            return "AppWidgetId{" + this.appWidgetId + ':' + this.host + ':' + this.provider + '}';
        }

        private boolean replaceWithMaskedViewsLocked(RemoteViews remoteViews) {
            this.maskedViews = remoteViews;
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

    private class LoadedWidgetState {
        final int hostTag;
        final int providerTag;
        final Widget widget;

        public LoadedWidgetState(Widget widget, int i, int i2) {
            this.widget = widget;
            this.hostTag = i;
            this.providerTag = i2;
        }
    }

    private final class SaveStateRunnable implements Runnable {
        final int mUserId;

        public SaveStateRunnable(int i) {
            this.mUserId = i;
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

        private BackupRestoreController() {
            this.mPrunedApps = new HashSet<>();
            this.mUpdatesByProvider = new HashMap<>();
            this.mUpdatesByHost = new HashMap<>();
        }

        public List<String> getWidgetParticipants(int i) {
            Slog.i(TAG, "Getting widget participants for user: " + i);
            HashSet hashSet = new HashSet();
            synchronized (AppWidgetServiceImpl.this.mLock) {
                int size = AppWidgetServiceImpl.this.mWidgets.size();
                for (int i2 = 0; i2 < size; i2++) {
                    Widget widget = (Widget) AppWidgetServiceImpl.this.mWidgets.get(i2);
                    if (isProviderAndHostInUser(widget, i)) {
                        hashSet.add(widget.host.id.packageName);
                        Provider provider = widget.provider;
                        if (provider != null) {
                            hashSet.add(provider.id.componentName.getPackageName());
                        }
                    }
                }
            }
            return new ArrayList(hashSet);
        }

        public byte[] getWidgetState(String str, int i) {
            Slog.i(TAG, "Getting widget state for user: " + i);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            synchronized (AppWidgetServiceImpl.this.mLock) {
                if (!packageNeedsWidgetBackupLocked(str, i)) {
                    return null;
                }
                try {
                    FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                    fastXmlSerializer.setOutput(byteArrayOutputStream, StandardCharsets.UTF_8.name());
                    fastXmlSerializer.startDocument(null, true);
                    fastXmlSerializer.startTag(null, "ws");
                    fastXmlSerializer.attribute(null, "version", String.valueOf(2));
                    fastXmlSerializer.attribute(null, "pkg", str);
                    int size = AppWidgetServiceImpl.this.mProviders.size();
                    int i2 = 0;
                    for (int i3 = 0; i3 < size; i3++) {
                        Provider provider = (Provider) AppWidgetServiceImpl.this.mProviders.get(i3);
                        if (provider.shouldBePersisted() && (provider.isInPackageForUser(str, i) || provider.hostedByPackageForUser(str, i))) {
                            provider.tag = i2;
                            AppWidgetServiceImpl.serializeProvider(fastXmlSerializer, provider);
                            i2++;
                        }
                    }
                    int size2 = AppWidgetServiceImpl.this.mHosts.size();
                    int i4 = 0;
                    for (int i5 = 0; i5 < size2; i5++) {
                        Host host = (Host) AppWidgetServiceImpl.this.mHosts.get(i5);
                        if (!host.widgets.isEmpty() && (host.isInPackageForUser(str, i) || host.hostsPackageForUser(str, i))) {
                            host.tag = i4;
                            AppWidgetServiceImpl.serializeHost(fastXmlSerializer, host);
                            i4++;
                        }
                    }
                    int size3 = AppWidgetServiceImpl.this.mWidgets.size();
                    for (int i6 = 0; i6 < size3; i6++) {
                        Widget widget = (Widget) AppWidgetServiceImpl.this.mWidgets.get(i6);
                        Provider provider2 = widget.provider;
                        if (widget.host.isInPackageForUser(str, i) || (provider2 != null && provider2.isInPackageForUser(str, i))) {
                            AppWidgetServiceImpl.serializeAppWidget(fastXmlSerializer, widget);
                        }
                    }
                    fastXmlSerializer.endTag(null, "ws");
                    fastXmlSerializer.endDocument();
                    return byteArrayOutputStream.toByteArray();
                } catch (IOException e) {
                    Slog.w(TAG, "Unable to save widget state for " + str);
                    return null;
                }
            }
        }

        public void restoreStarting(int i) {
            Slog.i(TAG, "Restore starting for user: " + i);
            synchronized (AppWidgetServiceImpl.this.mLock) {
                this.mPrunedApps.clear();
                this.mUpdatesByProvider.clear();
                this.mUpdatesByHost.clear();
            }
        }

        public void restoreWidgetState(String str, byte[] bArr, int i) {
            ArrayList arrayList;
            ArrayList arrayList2;
            XmlPullParser xmlPullParserNewPullParser;
            int next;
            Slog.i(TAG, "Restoring widget state for user:" + i + " package: " + str);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bArr);
            try {
                try {
                    arrayList = new ArrayList();
                    arrayList2 = new ArrayList();
                    xmlPullParserNewPullParser = Xml.newPullParser();
                    xmlPullParserNewPullParser.setInput(byteArrayInputStream, StandardCharsets.UTF_8.name());
                } catch (IOException | XmlPullParserException e) {
                    Slog.w(TAG, "Unable to restore widget state for " + str);
                }
                synchronized (AppWidgetServiceImpl.this.mLock) {
                    do {
                        next = xmlPullParserNewPullParser.next();
                        if (next == 2) {
                            String name = xmlPullParserNewPullParser.getName();
                            if ("ws".equals(name)) {
                                String attributeValue = xmlPullParserNewPullParser.getAttributeValue(null, "version");
                                if (Integer.parseInt(attributeValue) > 2) {
                                    Slog.w(TAG, "Unable to process state version " + attributeValue);
                                    return;
                                }
                                if (!str.equals(xmlPullParserNewPullParser.getAttributeValue(null, "pkg"))) {
                                    Slog.w(TAG, "Package mismatch in ws");
                                    return;
                                }
                            } else if ("p".equals(name)) {
                                ComponentName componentName = new ComponentName(xmlPullParserNewPullParser.getAttributeValue(null, "pkg"), xmlPullParserNewPullParser.getAttributeValue(null, "cl"));
                                Provider providerFindProviderLocked = findProviderLocked(componentName, i);
                                if (providerFindProviderLocked == null) {
                                    providerFindProviderLocked = new Provider();
                                    providerFindProviderLocked.id = new ProviderId(-1, componentName);
                                    providerFindProviderLocked.info = new AppWidgetProviderInfo();
                                    providerFindProviderLocked.info.provider = componentName;
                                    providerFindProviderLocked.zombie = true;
                                    AppWidgetServiceImpl.this.mProviders.add(providerFindProviderLocked);
                                }
                                Slog.i(TAG, "   provider " + providerFindProviderLocked.id);
                                arrayList.add(providerFindProviderLocked);
                            } else if ("h".equals(name)) {
                                String attributeValue2 = xmlPullParserNewPullParser.getAttributeValue(null, "pkg");
                                Host hostLookupOrAddHostLocked = AppWidgetServiceImpl.this.lookupOrAddHostLocked(new HostId(AppWidgetServiceImpl.this.getUidForPackage(attributeValue2, i), Integer.parseInt(xmlPullParserNewPullParser.getAttributeValue(null, "id"), 16), attributeValue2));
                                arrayList2.add(hostLookupOrAddHostLocked);
                                Slog.i(TAG, "   host[" + arrayList2.size() + "]: {" + hostLookupOrAddHostLocked.id + "}");
                            } else if ("g".equals(name)) {
                                int i2 = Integer.parseInt(xmlPullParserNewPullParser.getAttributeValue(null, "id"), 16);
                                Host host = (Host) arrayList2.get(Integer.parseInt(xmlPullParserNewPullParser.getAttributeValue(null, "h"), 16));
                                String attributeValue3 = xmlPullParserNewPullParser.getAttributeValue(null, "p");
                                Provider provider = attributeValue3 != null ? (Provider) arrayList.get(Integer.parseInt(attributeValue3, 16)) : null;
                                pruneWidgetStateLocked(host.id.packageName, i);
                                if (provider != null) {
                                    pruneWidgetStateLocked(provider.id.componentName.getPackageName(), i);
                                }
                                Widget widgetFindRestoredWidgetLocked = findRestoredWidgetLocked(i2, host, provider);
                                if (widgetFindRestoredWidgetLocked == null) {
                                    widgetFindRestoredWidgetLocked = new Widget();
                                    widgetFindRestoredWidgetLocked.appWidgetId = AppWidgetServiceImpl.this.incrementAndGetAppWidgetIdLocked(i);
                                    widgetFindRestoredWidgetLocked.restoredId = i2;
                                    widgetFindRestoredWidgetLocked.options = parseWidgetIdOptions(xmlPullParserNewPullParser);
                                    widgetFindRestoredWidgetLocked.host = host;
                                    widgetFindRestoredWidgetLocked.host.widgets.add(widgetFindRestoredWidgetLocked);
                                    widgetFindRestoredWidgetLocked.provider = provider;
                                    if (widgetFindRestoredWidgetLocked.provider != null) {
                                        widgetFindRestoredWidgetLocked.provider.widgets.add(widgetFindRestoredWidgetLocked);
                                    }
                                    Slog.i(TAG, "New restored id " + i2 + " now " + widgetFindRestoredWidgetLocked);
                                    AppWidgetServiceImpl.this.addWidgetLocked(widgetFindRestoredWidgetLocked);
                                }
                                if (widgetFindRestoredWidgetLocked.provider == null || widgetFindRestoredWidgetLocked.provider.info == null) {
                                    Slog.w(TAG, "Missing provider for restored widget " + widgetFindRestoredWidgetLocked);
                                } else {
                                    stashProviderRestoreUpdateLocked(widgetFindRestoredWidgetLocked.provider, i2, widgetFindRestoredWidgetLocked.appWidgetId);
                                }
                                stashHostRestoreUpdateLocked(widgetFindRestoredWidgetLocked.host, i2, widgetFindRestoredWidgetLocked.appWidgetId);
                                Slog.i(TAG, "   instance: " + i2 + " -> " + widgetFindRestoredWidgetLocked.appWidgetId + " :: p=" + widgetFindRestoredWidgetLocked.provider);
                            }
                        }
                    } while (next != 1);
                }
            } finally {
                AppWidgetServiceImpl.this.saveGroupStateAsync(i);
            }
        }

        public void restoreFinished(int i) {
            Slog.i(TAG, "restoreFinished for " + i);
            UserHandle userHandle = new UserHandle(i);
            synchronized (AppWidgetServiceImpl.this.mLock) {
                for (Map.Entry<Provider, ArrayList<RestoreUpdateRecord>> entry : this.mUpdatesByProvider.entrySet()) {
                    Provider key = entry.getKey();
                    ArrayList<RestoreUpdateRecord> value = entry.getValue();
                    int iCountPendingUpdates = countPendingUpdates(value);
                    Slog.i(TAG, "Provider " + key + " pending: " + iCountPendingUpdates);
                    if (iCountPendingUpdates > 0) {
                        int[] iArr = new int[iCountPendingUpdates];
                        int[] iArr2 = new int[iCountPendingUpdates];
                        int size = value.size();
                        int i2 = 0;
                        for (int i3 = 0; i3 < size; i3++) {
                            RestoreUpdateRecord restoreUpdateRecord = value.get(i3);
                            if (!restoreUpdateRecord.notified) {
                                restoreUpdateRecord.notified = true;
                                iArr[i2] = restoreUpdateRecord.oldId;
                                iArr2[i2] = restoreUpdateRecord.newId;
                                i2++;
                                Slog.i(TAG, "   " + restoreUpdateRecord.oldId + " => " + restoreUpdateRecord.newId);
                            }
                        }
                        sendWidgetRestoreBroadcastLocked("android.appwidget.action.APPWIDGET_RESTORED", key, null, iArr, iArr2, userHandle);
                    }
                }
                for (Map.Entry<Host, ArrayList<RestoreUpdateRecord>> entry2 : this.mUpdatesByHost.entrySet()) {
                    Host key2 = entry2.getKey();
                    if (key2.id.uid != -1) {
                        ArrayList<RestoreUpdateRecord> value2 = entry2.getValue();
                        int iCountPendingUpdates2 = countPendingUpdates(value2);
                        Slog.i(TAG, "Host " + key2 + " pending: " + iCountPendingUpdates2);
                        if (iCountPendingUpdates2 > 0) {
                            int[] iArr3 = new int[iCountPendingUpdates2];
                            int[] iArr4 = new int[iCountPendingUpdates2];
                            int size2 = value2.size();
                            int i4 = 0;
                            for (int i5 = 0; i5 < size2; i5++) {
                                RestoreUpdateRecord restoreUpdateRecord2 = value2.get(i5);
                                if (!restoreUpdateRecord2.notified) {
                                    restoreUpdateRecord2.notified = true;
                                    iArr3[i4] = restoreUpdateRecord2.oldId;
                                    iArr4[i4] = restoreUpdateRecord2.newId;
                                    i4++;
                                    Slog.i(TAG, "   " + restoreUpdateRecord2.oldId + " => " + restoreUpdateRecord2.newId);
                                }
                            }
                            sendWidgetRestoreBroadcastLocked("android.appwidget.action.APPWIDGET_HOST_RESTORED", null, key2, iArr3, iArr4, userHandle);
                        }
                    }
                }
            }
        }

        private Provider findProviderLocked(ComponentName componentName, int i) {
            int size = AppWidgetServiceImpl.this.mProviders.size();
            for (int i2 = 0; i2 < size; i2++) {
                Provider provider = (Provider) AppWidgetServiceImpl.this.mProviders.get(i2);
                if (provider.getUserId() == i && provider.id.componentName.equals(componentName)) {
                    return provider;
                }
            }
            return null;
        }

        private Widget findRestoredWidgetLocked(int i, Host host, Provider provider) {
            Slog.i(TAG, "Find restored widget: id=" + i + " host=" + host + " provider=" + provider);
            if (provider != null && host != null) {
                int size = AppWidgetServiceImpl.this.mWidgets.size();
                for (int i2 = 0; i2 < size; i2++) {
                    Widget widget = (Widget) AppWidgetServiceImpl.this.mWidgets.get(i2);
                    if (widget.restoredId == i && widget.host.id.equals(host.id) && widget.provider.id.equals(provider.id)) {
                        Slog.i(TAG, "   Found at " + i2 + " : " + widget);
                        return widget;
                    }
                }
                return null;
            }
            return null;
        }

        private boolean packageNeedsWidgetBackupLocked(String str, int i) {
            int size = AppWidgetServiceImpl.this.mWidgets.size();
            for (int i2 = 0; i2 < size; i2++) {
                Widget widget = (Widget) AppWidgetServiceImpl.this.mWidgets.get(i2);
                if (isProviderAndHostInUser(widget, i)) {
                    if (widget.host.isInPackageForUser(str, i)) {
                        return true;
                    }
                    Provider provider = widget.provider;
                    if (provider != null && provider.isInPackageForUser(str, i)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private void stashProviderRestoreUpdateLocked(Provider provider, int i, int i2) {
            ArrayList<RestoreUpdateRecord> arrayList = this.mUpdatesByProvider.get(provider);
            if (arrayList == null) {
                arrayList = new ArrayList<>();
                this.mUpdatesByProvider.put(provider, arrayList);
            } else if (alreadyStashed(arrayList, i, i2)) {
                Slog.i(TAG, "ID remap " + i + " -> " + i2 + " already stashed for " + provider);
                return;
            }
            arrayList.add(new RestoreUpdateRecord(i, i2));
        }

        private boolean alreadyStashed(ArrayList<RestoreUpdateRecord> arrayList, int i, int i2) {
            int size = arrayList.size();
            for (int i3 = 0; i3 < size; i3++) {
                RestoreUpdateRecord restoreUpdateRecord = arrayList.get(i3);
                if (restoreUpdateRecord.oldId == i && restoreUpdateRecord.newId == i2) {
                    return true;
                }
            }
            return false;
        }

        private void stashHostRestoreUpdateLocked(Host host, int i, int i2) {
            ArrayList<RestoreUpdateRecord> arrayList = this.mUpdatesByHost.get(host);
            if (arrayList == null) {
                arrayList = new ArrayList<>();
                this.mUpdatesByHost.put(host, arrayList);
            } else if (alreadyStashed(arrayList, i, i2)) {
                Slog.i(TAG, "ID remap " + i + " -> " + i2 + " already stashed for " + host);
                return;
            }
            arrayList.add(new RestoreUpdateRecord(i, i2));
        }

        private void sendWidgetRestoreBroadcastLocked(String str, Provider provider, Host host, int[] iArr, int[] iArr2, UserHandle userHandle) {
            Intent intent = new Intent(str);
            intent.putExtra("appWidgetOldIds", iArr);
            intent.putExtra("appWidgetIds", iArr2);
            if (provider != null) {
                intent.setComponent(provider.info.provider);
                AppWidgetServiceImpl.this.sendBroadcastAsUser(intent, userHandle);
            }
            if (host != null) {
                intent.setComponent(null);
                intent.setPackage(host.id.packageName);
                intent.putExtra("hostId", host.id.hostId);
                AppWidgetServiceImpl.this.sendBroadcastAsUser(intent, userHandle);
            }
        }

        private void pruneWidgetStateLocked(String str, int i) {
            if (!this.mPrunedApps.contains(str)) {
                Slog.i(TAG, "pruning widget state for restoring package " + str);
                for (int size = AppWidgetServiceImpl.this.mWidgets.size() + (-1); size >= 0; size--) {
                    Widget widget = (Widget) AppWidgetServiceImpl.this.mWidgets.get(size);
                    Host host = widget.host;
                    Provider provider = widget.provider;
                    if (host.hostsPackageForUser(str, i) || (provider != null && provider.isInPackageForUser(str, i))) {
                        host.widgets.remove(widget);
                        provider.widgets.remove(widget);
                        AppWidgetServiceImpl.this.decrementAppWidgetServiceRefCount(widget);
                        AppWidgetServiceImpl.this.removeWidgetLocked(widget);
                    }
                }
                this.mPrunedApps.add(str);
                return;
            }
            Slog.i(TAG, "already pruned " + str + ", continuing normally");
        }

        private boolean isProviderAndHostInUser(Widget widget, int i) {
            return widget.host.getUserId() == i && (widget.provider == null || widget.provider.getUserId() == i);
        }

        private Bundle parseWidgetIdOptions(XmlPullParser xmlPullParser) {
            Bundle bundle = new Bundle();
            String attributeValue = xmlPullParser.getAttributeValue(null, "min_width");
            if (attributeValue != null) {
                bundle.putInt("appWidgetMinWidth", Integer.parseInt(attributeValue, 16));
            }
            String attributeValue2 = xmlPullParser.getAttributeValue(null, "min_height");
            if (attributeValue2 != null) {
                bundle.putInt("appWidgetMinHeight", Integer.parseInt(attributeValue2, 16));
            }
            String attributeValue3 = xmlPullParser.getAttributeValue(null, "max_width");
            if (attributeValue3 != null) {
                bundle.putInt("appWidgetMaxWidth", Integer.parseInt(attributeValue3, 16));
            }
            String attributeValue4 = xmlPullParser.getAttributeValue(null, "max_height");
            if (attributeValue4 != null) {
                bundle.putInt("appWidgetMaxHeight", Integer.parseInt(attributeValue4, 16));
            }
            String attributeValue5 = xmlPullParser.getAttributeValue(null, "host_category");
            if (attributeValue5 != null) {
                bundle.putInt("appWidgetCategory", Integer.parseInt(attributeValue5, 16));
            }
            return bundle;
        }

        private int countPendingUpdates(ArrayList<RestoreUpdateRecord> arrayList) {
            int size = arrayList.size();
            int i = 0;
            for (int i2 = 0; i2 < size; i2++) {
                if (!arrayList.get(i2).notified) {
                    i++;
                }
            }
            return i;
        }

        private class RestoreUpdateRecord {
            public int newId;
            public boolean notified = false;
            public int oldId;

            public RestoreUpdateRecord(int i, int i2) {
                this.oldId = i;
                this.newId = i2;
            }
        }
    }

    private class AppWidgetManagerLocal extends AppWidgetManagerInternal {
        private AppWidgetManagerLocal() {
        }

        public ArraySet<String> getHostedWidgetPackages(int i) {
            ArraySet<String> arraySet;
            synchronized (AppWidgetServiceImpl.this.mLock) {
                arraySet = null;
                int size = AppWidgetServiceImpl.this.mWidgets.size();
                for (int i2 = 0; i2 < size; i2++) {
                    Widget widget = (Widget) AppWidgetServiceImpl.this.mWidgets.get(i2);
                    if (widget.host.id.uid == i) {
                        if (arraySet == null) {
                            arraySet = new ArraySet<>();
                        }
                        arraySet.add(widget.provider.id.componentName.getPackageName());
                    }
                }
            }
            return arraySet;
        }
    }
}
