package com.android.server.om;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.IApplicationThread;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManagerInternal;
import android.content.pm.UserInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.util.ConcurrentUtils;
import com.android.server.FgThread;
import com.android.server.IoThread;
import com.android.server.LocalServices;
import com.android.server.SystemServerInitThreadPool;
import com.android.server.SystemService;
import com.android.server.om.OverlayManagerService;
import com.android.server.om.OverlayManagerServiceImpl;
import com.android.server.pm.Installer;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.Settings;
import com.android.server.pm.UserManagerService;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import libcore.util.EmptyArray;
import org.xmlpull.v1.XmlPullParserException;

public final class OverlayManagerService extends SystemService {
    static final boolean DEBUG = false;
    private static final String DEFAULT_OVERLAYS_PROP = "ro.boot.vendor.overlay.theme";
    static final String TAG = "OverlayManager";
    private final OverlayManagerServiceImpl mImpl;
    private Future<?> mInitCompleteSignal;
    private final Object mLock;
    private final PackageManagerHelper mPackageManager;
    private final AtomicBoolean mPersistSettingsScheduled;
    private final IBinder mService;
    private final OverlayManagerSettings mSettings;
    private final AtomicFile mSettingsFile;
    private final UserManagerService mUserManager;

    public OverlayManagerService(Context context, Installer installer) {
        super(context);
        this.mLock = new Object();
        this.mPersistSettingsScheduled = new AtomicBoolean(false);
        this.mService = new IOverlayManager.Stub() {
            public Map<String, List<OverlayInfo>> getAllOverlays(int i) throws RemoteException {
                Map<String, List<OverlayInfo>> overlaysForUser;
                int iHandleIncomingUser = handleIncomingUser(i, "getAllOverlays");
                synchronized (OverlayManagerService.this.mLock) {
                    overlaysForUser = OverlayManagerService.this.mImpl.getOverlaysForUser(iHandleIncomingUser);
                }
                return overlaysForUser;
            }

            public List<OverlayInfo> getOverlayInfosForTarget(String str, int i) throws RemoteException {
                List<OverlayInfo> overlayInfosForTarget;
                int iHandleIncomingUser = handleIncomingUser(i, "getOverlayInfosForTarget");
                if (str != null) {
                    synchronized (OverlayManagerService.this.mLock) {
                        overlayInfosForTarget = OverlayManagerService.this.mImpl.getOverlayInfosForTarget(str, iHandleIncomingUser);
                    }
                    return overlayInfosForTarget;
                }
                return Collections.emptyList();
            }

            public OverlayInfo getOverlayInfo(String str, int i) throws RemoteException {
                OverlayInfo overlayInfo;
                int iHandleIncomingUser = handleIncomingUser(i, "getOverlayInfo");
                if (str != null) {
                    synchronized (OverlayManagerService.this.mLock) {
                        overlayInfo = OverlayManagerService.this.mImpl.getOverlayInfo(str, iHandleIncomingUser);
                    }
                    return overlayInfo;
                }
                return null;
            }

            public boolean setEnabled(String str, boolean z, int i) throws RemoteException {
                boolean enabled;
                enforceChangeOverlayPackagesPermission("setEnabled");
                int iHandleIncomingUser = handleIncomingUser(i, "setEnabled");
                if (str == null) {
                    return false;
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    synchronized (OverlayManagerService.this.mLock) {
                        enabled = OverlayManagerService.this.mImpl.setEnabled(str, z, iHandleIncomingUser);
                    }
                    return enabled;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public boolean setEnabledExclusive(String str, boolean z, int i) throws RemoteException {
                boolean enabledExclusive;
                enforceChangeOverlayPackagesPermission("setEnabled");
                int iHandleIncomingUser = handleIncomingUser(i, "setEnabled");
                if (str == null || !z) {
                    return false;
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    synchronized (OverlayManagerService.this.mLock) {
                        enabledExclusive = OverlayManagerService.this.mImpl.setEnabledExclusive(str, false, iHandleIncomingUser);
                    }
                    return enabledExclusive;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public boolean setEnabledExclusiveInCategory(String str, int i) throws RemoteException {
                boolean enabledExclusive;
                enforceChangeOverlayPackagesPermission("setEnabled");
                int iHandleIncomingUser = handleIncomingUser(i, "setEnabled");
                if (str == null) {
                    return false;
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    synchronized (OverlayManagerService.this.mLock) {
                        enabledExclusive = OverlayManagerService.this.mImpl.setEnabledExclusive(str, true, iHandleIncomingUser);
                    }
                    return enabledExclusive;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public boolean setPriority(String str, String str2, int i) throws RemoteException {
                boolean priority;
                enforceChangeOverlayPackagesPermission("setPriority");
                int iHandleIncomingUser = handleIncomingUser(i, "setPriority");
                if (str == null || str2 == null) {
                    return false;
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    synchronized (OverlayManagerService.this.mLock) {
                        priority = OverlayManagerService.this.mImpl.setPriority(str, str2, iHandleIncomingUser);
                    }
                    return priority;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public boolean setHighestPriority(String str, int i) throws RemoteException {
                boolean highestPriority;
                enforceChangeOverlayPackagesPermission("setHighestPriority");
                int iHandleIncomingUser = handleIncomingUser(i, "setHighestPriority");
                if (str == null) {
                    return false;
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    synchronized (OverlayManagerService.this.mLock) {
                        highestPriority = OverlayManagerService.this.mImpl.setHighestPriority(str, iHandleIncomingUser);
                    }
                    return highestPriority;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public boolean setLowestPriority(String str, int i) throws RemoteException {
                boolean lowestPriority;
                enforceChangeOverlayPackagesPermission("setLowestPriority");
                int iHandleIncomingUser = handleIncomingUser(i, "setLowestPriority");
                if (str == null) {
                    return false;
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    synchronized (OverlayManagerService.this.mLock) {
                        lowestPriority = OverlayManagerService.this.mImpl.setLowestPriority(str, iHandleIncomingUser);
                    }
                    return lowestPriority;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }

            public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) {
                new OverlayManagerShellCommand(this).exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
            }

            protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
                enforceDumpPermission("dump");
                boolean z = false;
                if (strArr.length > 0 && "--verbose".equals(strArr[0])) {
                    z = true;
                }
                synchronized (OverlayManagerService.this.mLock) {
                    OverlayManagerService.this.mImpl.onDump(printWriter);
                    OverlayManagerService.this.mPackageManager.dump(printWriter, z);
                }
            }

            private int handleIncomingUser(int i, String str) {
                return ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), i, false, true, str, null);
            }

            private void enforceChangeOverlayPackagesPermission(String str) {
                OverlayManagerService.this.getContext().enforceCallingPermission("android.permission.CHANGE_OVERLAY_PACKAGES", str);
            }

            private void enforceDumpPermission(String str) {
                OverlayManagerService.this.getContext().enforceCallingPermission("android.permission.DUMP", str);
            }
        };
        this.mSettingsFile = new AtomicFile(new File(Environment.getDataSystemDirectory(), "overlays.xml"), "overlays");
        this.mPackageManager = new PackageManagerHelper();
        this.mUserManager = UserManagerService.getInstance();
        IdmapManager idmapManager = new IdmapManager(installer);
        this.mSettings = new OverlayManagerSettings();
        this.mImpl = new OverlayManagerServiceImpl(this.mPackageManager, idmapManager, this.mSettings, getDefaultOverlayPackages(), new OverlayChangeListener());
        this.mInitCompleteSignal = SystemServerInitThreadPool.get().submit(new Runnable() {
            @Override
            public final void run() {
                OverlayManagerService.lambda$new$0(this.f$0);
            }
        }, "Init OverlayManagerService");
    }

    public static void lambda$new$0(OverlayManagerService overlayManagerService) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addDataScheme(Settings.ATTR_PACKAGE);
        overlayManagerService.getContext().registerReceiverAsUser(new PackageReceiver(), UserHandle.ALL, intentFilter, null, null);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.USER_ADDED");
        intentFilter2.addAction("android.intent.action.USER_REMOVED");
        overlayManagerService.getContext().registerReceiverAsUser(new UserReceiver(), UserHandle.ALL, intentFilter2, null, null);
        overlayManagerService.restoreSettings();
        overlayManagerService.initIfNeeded();
        overlayManagerService.onSwitchUser(0);
        overlayManagerService.publishBinderService("overlay", overlayManagerService.mService);
        overlayManagerService.publishLocalService(OverlayManagerService.class, overlayManagerService);
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onBootPhase(int i) {
        if (i == 500) {
            ConcurrentUtils.waitForFutureNoInterrupt(this.mInitCompleteSignal, "Wait for OverlayManagerService init");
            this.mInitCompleteSignal = null;
        }
    }

    private void initIfNeeded() {
        List users = ((UserManager) getContext().getSystemService(UserManager.class)).getUsers(true);
        synchronized (this.mLock) {
            int size = users.size();
            for (int i = 0; i < size; i++) {
                UserInfo userInfo = (UserInfo) users.get(i);
                if (!userInfo.supportsSwitchTo() && userInfo.id != 0) {
                    updateOverlayPaths(((UserInfo) users.get(i)).id, this.mImpl.updateOverlaysForUser(((UserInfo) users.get(i)).id));
                }
            }
        }
    }

    @Override
    public void onSwitchUser(int i) {
        synchronized (this.mLock) {
            updateAssets(i, this.mImpl.updateOverlaysForUser(i));
        }
        schedulePersistSettings();
    }

    private static String[] getDefaultOverlayPackages() {
        String str = SystemProperties.get(DEFAULT_OVERLAYS_PROP);
        if (TextUtils.isEmpty(str)) {
            return EmptyArray.STRING;
        }
        ArraySet arraySet = new ArraySet();
        for (String str2 : str.split(";")) {
            if (!TextUtils.isEmpty(str2)) {
                arraySet.add(str2);
            }
        }
        return (String[]) arraySet.toArray(new String[arraySet.size()]);
    }

    private final class PackageReceiver extends BroadcastReceiver {
        private PackageReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            int[] userIds;
            Uri data = intent.getData();
            if (data == null) {
                Slog.e(OverlayManagerService.TAG, "Cannot handle package broadcast with null data");
            }
            String schemeSpecificPart = data.getSchemeSpecificPart();
            byte b = 0;
            boolean booleanExtra = intent.getBooleanExtra("android.intent.extra.REPLACING", false);
            int intExtra = intent.getIntExtra("android.intent.extra.UID", -10000);
            if (intExtra == -10000) {
                userIds = OverlayManagerService.this.mUserManager.getUserIds();
            } else {
                userIds = new int[]{UserHandle.getUserId(intExtra)};
            }
            String action = intent.getAction();
            int iHashCode = action.hashCode();
            if (iHashCode != 172491798) {
                if (iHashCode != 525384130) {
                    if (iHashCode != 1544582882 || !action.equals("android.intent.action.PACKAGE_ADDED")) {
                        b = -1;
                    }
                } else if (action.equals("android.intent.action.PACKAGE_REMOVED")) {
                    b = 2;
                }
            } else if (action.equals("android.intent.action.PACKAGE_CHANGED")) {
                b = 1;
            }
            switch (b) {
                case 0:
                    if (booleanExtra) {
                        onPackageUpgraded(schemeSpecificPart, userIds);
                    } else {
                        onPackageAdded(schemeSpecificPart, userIds);
                    }
                    break;
                case 1:
                    onPackageChanged(schemeSpecificPart, userIds);
                    break;
                case 2:
                    if (booleanExtra) {
                        onPackageUpgrading(schemeSpecificPart, userIds);
                    } else {
                        onPackageRemoved(schemeSpecificPart, userIds);
                    }
                    break;
            }
        }

        private void onPackageAdded(String str, int[] iArr) {
            for (int i : iArr) {
                synchronized (OverlayManagerService.this.mLock) {
                    PackageInfo packageInfo = OverlayManagerService.this.mPackageManager.getPackageInfo(str, i, false);
                    if (packageInfo != null) {
                        OverlayManagerService.this.mPackageManager.cachePackageInfo(str, i, packageInfo);
                        if (packageInfo.isOverlayPackage()) {
                            OverlayManagerService.this.mImpl.onOverlayPackageAdded(str, i);
                        } else {
                            OverlayManagerService.this.mImpl.onTargetPackageAdded(str, i);
                        }
                    }
                }
            }
        }

        private void onPackageChanged(String str, int[] iArr) {
            for (int i : iArr) {
                synchronized (OverlayManagerService.this.mLock) {
                    PackageInfo packageInfo = OverlayManagerService.this.mPackageManager.getPackageInfo(str, i, false);
                    if (packageInfo != null) {
                        OverlayManagerService.this.mPackageManager.cachePackageInfo(str, i, packageInfo);
                        if (packageInfo.isOverlayPackage()) {
                            OverlayManagerService.this.mImpl.onOverlayPackageChanged(str, i);
                        } else {
                            OverlayManagerService.this.mImpl.onTargetPackageChanged(str, i);
                        }
                    }
                }
            }
        }

        private void onPackageUpgrading(String str, int[] iArr) {
            for (int i : iArr) {
                synchronized (OverlayManagerService.this.mLock) {
                    OverlayManagerService.this.mPackageManager.forgetPackageInfo(str, i);
                    if (OverlayManagerService.this.mImpl.getOverlayInfo(str, i) != null) {
                        OverlayManagerService.this.mImpl.onOverlayPackageUpgrading(str, i);
                    } else {
                        OverlayManagerService.this.mImpl.onTargetPackageUpgrading(str, i);
                    }
                }
            }
        }

        private void onPackageUpgraded(String str, int[] iArr) {
            for (int i : iArr) {
                synchronized (OverlayManagerService.this.mLock) {
                    PackageInfo packageInfo = OverlayManagerService.this.mPackageManager.getPackageInfo(str, i, false);
                    if (packageInfo != null) {
                        OverlayManagerService.this.mPackageManager.cachePackageInfo(str, i, packageInfo);
                        if (packageInfo.isOverlayPackage()) {
                            OverlayManagerService.this.mImpl.onOverlayPackageUpgraded(str, i);
                        } else {
                            OverlayManagerService.this.mImpl.onTargetPackageUpgraded(str, i);
                        }
                    }
                }
            }
        }

        private void onPackageRemoved(String str, int[] iArr) {
            for (int i : iArr) {
                synchronized (OverlayManagerService.this.mLock) {
                    OverlayManagerService.this.mPackageManager.forgetPackageInfo(str, i);
                    if (OverlayManagerService.this.mImpl.getOverlayInfo(str, i) != null) {
                        OverlayManagerService.this.mImpl.onOverlayPackageRemoved(str, i);
                    } else {
                        OverlayManagerService.this.mImpl.onTargetPackageRemoved(str, i);
                    }
                }
            }
        }
    }

    private final class UserReceiver extends BroadcastReceiver {
        private UserReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            byte b;
            ArrayList<String> arrayListUpdateOverlaysForUser;
            int intExtra = intent.getIntExtra("android.intent.extra.user_handle", -10000);
            String action = intent.getAction();
            int iHashCode = action.hashCode();
            if (iHashCode != -2061058799) {
                b = (iHashCode == 1121780209 && action.equals("android.intent.action.USER_ADDED")) ? (byte) 0 : (byte) -1;
            } else if (action.equals("android.intent.action.USER_REMOVED")) {
                b = 1;
            }
            switch (b) {
                case 0:
                    if (intExtra != -10000) {
                        synchronized (OverlayManagerService.this.mLock) {
                            arrayListUpdateOverlaysForUser = OverlayManagerService.this.mImpl.updateOverlaysForUser(intExtra);
                            break;
                        }
                        OverlayManagerService.this.updateOverlayPaths(intExtra, arrayListUpdateOverlaysForUser);
                        return;
                    }
                    return;
                case 1:
                    if (intExtra != -10000) {
                        synchronized (OverlayManagerService.this.mLock) {
                            OverlayManagerService.this.mImpl.onUserRemoved(intExtra);
                            OverlayManagerService.this.mPackageManager.forgetAllPackageInfos(intExtra);
                            break;
                        }
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    private final class OverlayChangeListener implements OverlayManagerServiceImpl.OverlayChangeListener {
        private OverlayChangeListener() {
        }

        @Override
        public void onOverlaysChanged(final String str, final int i) {
            OverlayManagerService.this.schedulePersistSettings();
            FgThread.getHandler().post(new Runnable() {
                @Override
                public final void run() {
                    OverlayManagerService.OverlayChangeListener.lambda$onOverlaysChanged$0(this.f$0, i, str);
                }
            });
        }

        public static void lambda$onOverlaysChanged$0(OverlayChangeListener overlayChangeListener, int i, String str) {
            OverlayManagerService.this.updateAssets(i, str);
            Intent intent = new Intent("android.intent.action.OVERLAY_CHANGED", Uri.fromParts(Settings.ATTR_PACKAGE, str, null));
            intent.setFlags(67108864);
            try {
                ActivityManager.getService().broadcastIntent((IApplicationThread) null, intent, (String) null, (IIntentReceiver) null, 0, (String) null, (Bundle) null, (String[]) null, -1, (Bundle) null, false, false, i);
            } catch (RemoteException e) {
            }
        }
    }

    private void updateOverlayPaths(int i, List<String> list) {
        PackageManagerInternal packageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        if (list.contains(PackageManagerService.PLATFORM_PACKAGE_NAME)) {
            list = packageManagerInternal.getTargetPackageNames(i);
        }
        ArrayMap arrayMap = new ArrayMap(list.size());
        synchronized (this.mLock) {
            List<String> enabledOverlayPackageNames = this.mImpl.getEnabledOverlayPackageNames(PackageManagerService.PLATFORM_PACKAGE_NAME, i);
            int size = list.size();
            for (int i2 = 0; i2 < size; i2++) {
                String str = list.get(i2);
                ArrayList arrayList = new ArrayList();
                if (!PackageManagerService.PLATFORM_PACKAGE_NAME.equals(str)) {
                    arrayList.addAll(enabledOverlayPackageNames);
                }
                arrayList.addAll(this.mImpl.getEnabledOverlayPackageNames(str, i));
                arrayMap.put(str, arrayList);
            }
        }
        int size2 = list.size();
        for (int i3 = 0; i3 < size2; i3++) {
            String str2 = list.get(i3);
            if (!packageManagerInternal.setEnabledOverlayPackages(i, str2, (List) arrayMap.get(str2))) {
                Slog.e(TAG, String.format("Failed to change enabled overlays for %s user %d", str2, Integer.valueOf(i)));
            }
        }
    }

    private void updateAssets(int i, String str) {
        updateAssets(i, Collections.singletonList(str));
    }

    private void updateAssets(int i, List<String> list) {
        updateOverlayPaths(i, list);
        try {
            ActivityManager.getService().scheduleApplicationInfoChanged(list, i);
        } catch (RemoteException e) {
        }
    }

    private void schedulePersistSettings() {
        if (this.mPersistSettingsScheduled.getAndSet(true)) {
            return;
        }
        IoThread.getHandler().post(new Runnable() {
            @Override
            public final void run() {
                OverlayManagerService.lambda$schedulePersistSettings$1(this.f$0);
            }
        });
    }

    public static void lambda$schedulePersistSettings$1(OverlayManagerService overlayManagerService) {
        FileOutputStream fileOutputStreamStartWrite;
        Throwable e;
        overlayManagerService.mPersistSettingsScheduled.set(false);
        synchronized (overlayManagerService.mLock) {
            try {
                fileOutputStreamStartWrite = overlayManagerService.mSettingsFile.startWrite();
            } catch (IOException | XmlPullParserException e2) {
                fileOutputStreamStartWrite = null;
                e = e2;
            }
            try {
                overlayManagerService.mSettings.persist(fileOutputStreamStartWrite);
                overlayManagerService.mSettingsFile.finishWrite(fileOutputStreamStartWrite);
            } catch (IOException | XmlPullParserException e3) {
                e = e3;
                overlayManagerService.mSettingsFile.failWrite(fileOutputStreamStartWrite);
                Slog.e(TAG, "failed to persist overlay state", e);
            }
        }
    }

    private void restoreSettings() {
        synchronized (this.mLock) {
            if (this.mSettingsFile.getBaseFile().exists()) {
                try {
                    FileInputStream fileInputStreamOpenRead = this.mSettingsFile.openRead();
                    Throwable th = null;
                    try {
                        this.mSettings.restore(fileInputStreamOpenRead);
                        List<UserInfo> users = this.mUserManager.getUsers(true);
                        int[] iArr = new int[users.size()];
                        for (int i = 0; i < users.size(); i++) {
                            iArr[i] = users.get(i).getUserHandle().getIdentifier();
                        }
                        Arrays.sort(iArr);
                        for (int i2 : this.mSettings.getUsers()) {
                            if (Arrays.binarySearch(iArr, i2) < 0) {
                                this.mSettings.removeUser(i2);
                            }
                        }
                        if (fileInputStreamOpenRead != null) {
                            fileInputStreamOpenRead.close();
                        }
                    } catch (Throwable th2) {
                        if (fileInputStreamOpenRead != null) {
                            if (0 != 0) {
                                try {
                                    fileInputStreamOpenRead.close();
                                } catch (Throwable th3) {
                                    th.addSuppressed(th3);
                                }
                            } else {
                                fileInputStreamOpenRead.close();
                            }
                        }
                        throw th2;
                    }
                } catch (IOException | XmlPullParserException e) {
                    Slog.e(TAG, "failed to restore overlay state", e);
                }
            }
        }
    }

    private static final class PackageManagerHelper implements OverlayManagerServiceImpl.PackageManagerHelper {
        private static final String TAB1 = "    ";
        private static final String TAB2 = "        ";
        private final SparseArray<HashMap<String, PackageInfo>> mCache = new SparseArray<>();
        private final IPackageManager mPackageManager = AppGlobals.getPackageManager();
        private final PackageManagerInternal mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);

        PackageManagerHelper() {
        }

        public PackageInfo getPackageInfo(String str, int i, boolean z) {
            PackageInfo cachedPackageInfo;
            if (z && (cachedPackageInfo = getCachedPackageInfo(str, i)) != null) {
                return cachedPackageInfo;
            }
            try {
                PackageInfo packageInfo = this.mPackageManager.getPackageInfo(str, 0, i);
                if (z && packageInfo != null) {
                    cachePackageInfo(str, i, packageInfo);
                }
                return packageInfo;
            } catch (RemoteException e) {
                return null;
            }
        }

        @Override
        public PackageInfo getPackageInfo(String str, int i) {
            return getPackageInfo(str, i, true);
        }

        @Override
        public boolean signaturesMatching(String str, String str2, int i) {
            try {
                return this.mPackageManager.checkSignatures(str, str2) == 0;
            } catch (RemoteException e) {
                return false;
            }
        }

        @Override
        public List<PackageInfo> getOverlayPackages(int i) {
            return this.mPackageManagerInternal.getOverlayPackages(i);
        }

        public PackageInfo getCachedPackageInfo(String str, int i) {
            HashMap<String, PackageInfo> map = this.mCache.get(i);
            if (map == null) {
                return null;
            }
            return map.get(str);
        }

        public void cachePackageInfo(String str, int i, PackageInfo packageInfo) {
            HashMap<String, PackageInfo> map = this.mCache.get(i);
            if (map == null) {
                map = new HashMap<>();
                this.mCache.put(i, map);
            }
            map.put(str, packageInfo);
        }

        public void forgetPackageInfo(String str, int i) {
            HashMap<String, PackageInfo> map = this.mCache.get(i);
            if (map == null) {
                return;
            }
            map.remove(str);
            if (map.isEmpty()) {
                this.mCache.delete(i);
            }
        }

        public void forgetAllPackageInfos(int i) {
            this.mCache.delete(i);
        }

        public void dump(PrintWriter printWriter, boolean z) {
            printWriter.println("PackageInfo cache");
            int i = 0;
            if (!z) {
                int size = this.mCache.size();
                int size2 = 0;
                while (i < size) {
                    size2 += this.mCache.get(this.mCache.keyAt(i)).size();
                    i++;
                }
                printWriter.println(TAB1 + size2 + " package(s)");
                return;
            }
            if (this.mCache.size() == 0) {
                printWriter.println("    <empty>");
                return;
            }
            int size3 = this.mCache.size();
            while (i < size3) {
                int iKeyAt = this.mCache.keyAt(i);
                printWriter.println("    User " + iKeyAt);
                for (Map.Entry<String, PackageInfo> entry : this.mCache.get(iKeyAt).entrySet()) {
                    printWriter.println(TAB2 + entry.getKey() + ": " + entry.getValue());
                }
                i++;
            }
        }
    }
}
