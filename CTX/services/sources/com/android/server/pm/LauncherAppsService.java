package com.android.server.pm;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AppGlobals;
import android.app.IApplicationThread;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ILauncherApps;
import android.content.pm.IOnAppsChangedListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutServiceInternal;
import android.content.pm.UserInfo;
import android.graphics.Rect;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IInterface;
import android.os.ParcelFileDescriptor;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManagerInternal;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.Preconditions;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.mediatek.server.MtkSystemServiceFactory;
import com.mediatek.server.pm.PmsExt;
import java.util.Collections;
import java.util.List;

public class LauncherAppsService extends SystemService {
    private final LauncherAppsImpl mLauncherAppsImpl;

    public LauncherAppsService(Context context) {
        super(context);
        this.mLauncherAppsImpl = new LauncherAppsImpl(context);
    }

    @Override
    public void onStart() {
        publishBinderService("launcherapps", this.mLauncherAppsImpl);
    }

    static class BroadcastCookie {
        public final int callingPid;
        public final int callingUid;
        public final String packageName;
        public final UserHandle user;

        BroadcastCookie(UserHandle userHandle, String str, int i, int i2) {
            this.user = userHandle;
            this.packageName = str;
            this.callingUid = i2;
            this.callingPid = i;
        }
    }

    @VisibleForTesting
    static class LauncherAppsImpl extends ILauncherApps.Stub {
        private static final boolean DEBUG = false;
        private static final String TAG = "LauncherAppsService";
        private final Handler mCallbackHandler;
        private final Context mContext;
        private final UserManager mUm;
        private final PackageCallbackList<IOnAppsChangedListener> mListeners = new PackageCallbackList<>();
        private final MyPackageMonitor mPackageMonitor = new MyPackageMonitor();
        private PmsExt mPmsExt = MtkSystemServiceFactory.getInstance().makePmsExt();
        private final UserManagerInternal mUserManagerInternal = (UserManagerInternal) Preconditions.checkNotNull((UserManagerInternal) LocalServices.getService(UserManagerInternal.class));
        private final ActivityManagerInternal mActivityManagerInternal = (ActivityManagerInternal) Preconditions.checkNotNull((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class));
        private final ShortcutServiceInternal mShortcutServiceInternal = (ShortcutServiceInternal) Preconditions.checkNotNull((ShortcutServiceInternal) LocalServices.getService(ShortcutServiceInternal.class));

        public LauncherAppsImpl(Context context) {
            this.mContext = context;
            this.mUm = (UserManager) this.mContext.getSystemService("user");
            this.mShortcutServiceInternal.addListener(this.mPackageMonitor);
            this.mCallbackHandler = BackgroundThread.getHandler();
        }

        @VisibleForTesting
        int injectBinderCallingUid() {
            return getCallingUid();
        }

        @VisibleForTesting
        int injectBinderCallingPid() {
            return getCallingPid();
        }

        final int injectCallingUserId() {
            return UserHandle.getUserId(injectBinderCallingUid());
        }

        @VisibleForTesting
        long injectClearCallingIdentity() {
            return Binder.clearCallingIdentity();
        }

        @VisibleForTesting
        void injectRestoreCallingIdentity(long j) {
            Binder.restoreCallingIdentity(j);
        }

        private int getCallingUserId() {
            return UserHandle.getUserId(injectBinderCallingUid());
        }

        public void addOnAppsChangedListener(String str, IOnAppsChangedListener iOnAppsChangedListener) throws RemoteException {
            verifyCallingPackage(str);
            synchronized (this.mListeners) {
                if (this.mListeners.getRegisteredCallbackCount() == 0) {
                    startWatchingPackageBroadcasts();
                }
                this.mListeners.unregister(iOnAppsChangedListener);
                this.mListeners.register(iOnAppsChangedListener, new BroadcastCookie(UserHandle.of(getCallingUserId()), str, injectBinderCallingPid(), injectBinderCallingUid()));
            }
        }

        public void removeOnAppsChangedListener(IOnAppsChangedListener iOnAppsChangedListener) throws RemoteException {
            synchronized (this.mListeners) {
                this.mListeners.unregister(iOnAppsChangedListener);
                if (this.mListeners.getRegisteredCallbackCount() == 0) {
                    stopWatchingPackageBroadcasts();
                }
            }
        }

        private void startWatchingPackageBroadcasts() {
            this.mPackageMonitor.register(this.mContext, UserHandle.ALL, true, this.mCallbackHandler);
        }

        private void stopWatchingPackageBroadcasts() {
            this.mPackageMonitor.unregister();
        }

        void checkCallbackCount() {
            synchronized (this.mListeners) {
                if (this.mListeners.getRegisteredCallbackCount() == 0) {
                    stopWatchingPackageBroadcasts();
                }
            }
        }

        private boolean canAccessProfile(int i, String str) {
            int iInjectCallingUserId = injectCallingUserId();
            if (i == iInjectCallingUserId) {
                return true;
            }
            long jInjectClearCallingIdentity = injectClearCallingIdentity();
            try {
                UserInfo userInfo = this.mUm.getUserInfo(iInjectCallingUserId);
                if (userInfo != null && userInfo.isManagedProfile()) {
                    Slog.w(TAG, str + " for another profile " + i + " from " + iInjectCallingUserId + " not allowed");
                    return false;
                }
                injectRestoreCallingIdentity(jInjectClearCallingIdentity);
                return this.mUserManagerInternal.isProfileAccessible(injectCallingUserId(), i, str, true);
            } finally {
                injectRestoreCallingIdentity(jInjectClearCallingIdentity);
            }
        }

        @VisibleForTesting
        void verifyCallingPackage(String str) {
            int packageUid;
            try {
                packageUid = AppGlobals.getPackageManager().getPackageUid(str, 794624, UserHandle.getUserId(getCallingUid()));
            } catch (RemoteException e) {
                packageUid = -1;
            }
            if (packageUid < 0) {
                Log.e(TAG, "Package not found: " + str);
            }
            if (packageUid != injectBinderCallingUid()) {
                throw new SecurityException("Calling package name mismatch");
            }
        }

        public ParceledListSlice<ResolveInfo> getLauncherActivities(String str, String str2, UserHandle userHandle) throws RemoteException {
            return queryActivitiesForUser(str, new Intent("android.intent.action.MAIN").addCategory("android.intent.category.LAUNCHER").setPackage(str2), userHandle);
        }

        public ActivityInfo resolveActivity(String str, ComponentName componentName, UserHandle userHandle) throws RemoteException {
            if (!canAccessProfile(userHandle.getIdentifier(), "Cannot resolve activity")) {
                return null;
            }
            int iInjectBinderCallingUid = injectBinderCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                ActivityInfo activityInfo = ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getActivityInfo(componentName, 786432, iInjectBinderCallingUid, userHandle.getIdentifier());
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                this.mPmsExt.updateActivityInfoForRemovable(activityInfo);
                return activityInfo;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        }

        public ParceledListSlice getShortcutConfigActivities(String str, String str2, UserHandle userHandle) throws RemoteException {
            return queryActivitiesForUser(str, new Intent("android.intent.action.CREATE_SHORTCUT").setPackage(str2), userHandle);
        }

        private ParceledListSlice<ResolveInfo> queryActivitiesForUser(String str, Intent intent, UserHandle userHandle) throws RemoteException {
            if (!canAccessProfile(userHandle.getIdentifier(), "Cannot retrieve activities")) {
                return null;
            }
            int iInjectBinderCallingUid = injectBinderCallingUid();
            long jInjectClearCallingIdentity = injectClearCallingIdentity();
            try {
                List<ResolveInfo> listQueryIntentActivities = ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).queryIntentActivities(intent, 786432, iInjectBinderCallingUid, userHandle.getIdentifier());
                injectRestoreCallingIdentity(jInjectClearCallingIdentity);
                this.mPmsExt.updateResolveInfoListForRemovable(listQueryIntentActivities);
                return new ParceledListSlice<>(listQueryIntentActivities);
            } catch (Throwable th) {
                injectRestoreCallingIdentity(jInjectClearCallingIdentity);
                throw th;
            }
        }

        public IntentSender getShortcutConfigActivityIntent(String str, ComponentName componentName, UserHandle userHandle) throws RemoteException {
            ensureShortcutPermission(str);
            if (!canAccessProfile(userHandle.getIdentifier(), "Cannot check package")) {
                return null;
            }
            Preconditions.checkNotNull(componentName);
            Intent component = new Intent("android.intent.action.CREATE_SHORTCUT").setComponent(componentName);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                PendingIntent activityAsUser = PendingIntent.getActivityAsUser(this.mContext, 0, component, 1409286144, null, userHandle);
                if (BenesseExtension.getDchaState() != 0) {
                    return null;
                }
                return activityAsUser != null ? activityAsUser.getIntentSender() : null;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public boolean isPackageEnabled(String str, String str2, UserHandle userHandle) throws RemoteException {
            boolean z = false;
            if (!canAccessProfile(userHandle.getIdentifier(), "Cannot check package")) {
                return false;
            }
            int iInjectBinderCallingUid = injectBinderCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                PackageInfo packageInfo = ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getPackageInfo(str2, 786432, iInjectBinderCallingUid, userHandle.getIdentifier());
                if (packageInfo != null) {
                    if (packageInfo.applicationInfo.enabled) {
                        z = true;
                    }
                }
                return z;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public Bundle getSuspendedPackageLauncherExtras(String str, UserHandle userHandle) {
            if (!canAccessProfile(userHandle.getIdentifier(), "Cannot get launcher extras")) {
                return null;
            }
            return ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getSuspendedPackageLauncherExtras(str, userHandle.getIdentifier());
        }

        public ApplicationInfo getApplicationInfo(String str, String str2, int i, UserHandle userHandle) throws RemoteException {
            if (!canAccessProfile(userHandle.getIdentifier(), "Cannot check package")) {
                return null;
            }
            int iInjectBinderCallingUid = injectBinderCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                ApplicationInfo applicationInfo = ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getApplicationInfo(str2, i, iInjectBinderCallingUid, userHandle.getIdentifier());
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                return this.mPmsExt.updateApplicationInfoForRemovable(AppGlobals.getPackageManager().getNameForUid(Binder.getCallingUid()), applicationInfo);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        }

        private void ensureShortcutPermission(String str) {
            verifyCallingPackage(str);
            if (!this.mShortcutServiceInternal.hasShortcutHostPermission(getCallingUserId(), str, injectBinderCallingPid(), injectBinderCallingUid())) {
                throw new SecurityException("Caller can't access shortcut information");
            }
        }

        public ParceledListSlice getShortcuts(String str, long j, String str2, List list, ComponentName componentName, int i, UserHandle userHandle) {
            ensureShortcutPermission(str);
            if (!canAccessProfile(userHandle.getIdentifier(), "Cannot get shortcuts")) {
                return new ParceledListSlice(Collections.EMPTY_LIST);
            }
            if (list != null && str2 == null) {
                throw new IllegalArgumentException("To query by shortcut ID, package name must also be set");
            }
            return new ParceledListSlice(this.mShortcutServiceInternal.getShortcuts(getCallingUserId(), str, j, str2, list, componentName, i, userHandle.getIdentifier(), injectBinderCallingPid(), injectBinderCallingUid()));
        }

        public void pinShortcuts(String str, String str2, List<String> list, UserHandle userHandle) {
            ensureShortcutPermission(str);
            if (!canAccessProfile(userHandle.getIdentifier(), "Cannot pin shortcuts")) {
                return;
            }
            this.mShortcutServiceInternal.pinShortcuts(getCallingUserId(), str, str2, list, userHandle.getIdentifier());
        }

        public int getShortcutIconResId(String str, String str2, String str3, int i) {
            ensureShortcutPermission(str);
            if (!canAccessProfile(i, "Cannot access shortcuts")) {
                return 0;
            }
            return this.mShortcutServiceInternal.getShortcutIconResId(getCallingUserId(), str, str2, str3, i);
        }

        public ParcelFileDescriptor getShortcutIconFd(String str, String str2, String str3, int i) {
            ensureShortcutPermission(str);
            if (!canAccessProfile(i, "Cannot access shortcuts")) {
                return null;
            }
            return this.mShortcutServiceInternal.getShortcutIconFd(getCallingUserId(), str, str2, str3, i);
        }

        public boolean hasShortcutHostPermission(String str) {
            verifyCallingPackage(str);
            return this.mShortcutServiceInternal.hasShortcutHostPermission(getCallingUserId(), str, injectBinderCallingPid(), injectBinderCallingUid());
        }

        public boolean startShortcut(String str, String str2, String str3, Rect rect, Bundle bundle, int i) {
            verifyCallingPackage(str);
            if (!canAccessProfile(i, "Cannot start activity")) {
                return false;
            }
            if (!this.mShortcutServiceInternal.isPinnedByCaller(getCallingUserId(), str, str2, str3, i)) {
                ensureShortcutPermission(str);
            }
            Intent[] intentArrCreateShortcutIntents = this.mShortcutServiceInternal.createShortcutIntents(getCallingUserId(), str, str2, str3, i, injectBinderCallingPid(), injectBinderCallingUid());
            if (intentArrCreateShortcutIntents == null || intentArrCreateShortcutIntents.length == 0) {
                return false;
            }
            intentArrCreateShortcutIntents[0].addFlags(268435456);
            intentArrCreateShortcutIntents[0].setSourceBounds(rect);
            return startShortcutIntentsAsPublisher(intentArrCreateShortcutIntents, str2, bundle, i);
        }

        private boolean startShortcutIntentsAsPublisher(Intent[] intentArr, String str, Bundle bundle, int i) {
            try {
                int iStartActivitiesAsPackage = this.mActivityManagerInternal.startActivitiesAsPackage(str, i, intentArr, bundle);
                if (ActivityManager.isStartResultSuccessful(iStartActivitiesAsPackage)) {
                    return true;
                }
                Log.e(TAG, "Couldn't start activity, code=" + iStartActivitiesAsPackage);
                return false;
            } catch (SecurityException e) {
                return false;
            }
        }

        public boolean isActivityEnabled(String str, ComponentName componentName, UserHandle userHandle) throws RemoteException {
            if (!canAccessProfile(userHandle.getIdentifier(), "Cannot check component")) {
                return false;
            }
            int iInjectBinderCallingUid = injectBinderCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getActivityInfo(componentName, 786432, iInjectBinderCallingUid, userHandle.getIdentifier()) != null;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void startActivityAsUser(IApplicationThread iApplicationThread, String str, ComponentName componentName, Rect rect, Bundle bundle, UserHandle userHandle) throws RemoteException {
            if (!canAccessProfile(userHandle.getIdentifier(), "Cannot start activity")) {
                return;
            }
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.addCategory("android.intent.category.LAUNCHER");
            intent.setSourceBounds(rect);
            intent.addFlags(270532608);
            intent.setPackage(componentName.getPackageName());
            int iInjectBinderCallingUid = injectBinderCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                PackageManagerInternal packageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
                if (!packageManagerInternal.getActivityInfo(componentName, 786432, iInjectBinderCallingUid, userHandle.getIdentifier()).exported) {
                    throw new SecurityException("Cannot launch non-exported components " + componentName);
                }
                List listQueryIntentActivities = packageManagerInternal.queryIntentActivities(intent, 786432, iInjectBinderCallingUid, userHandle.getIdentifier());
                int size = listQueryIntentActivities.size();
                boolean z = false;
                int i = 0;
                while (true) {
                    if (i >= size) {
                        break;
                    }
                    ActivityInfo activityInfo = ((ResolveInfo) listQueryIntentActivities.get(i)).activityInfo;
                    if (!activityInfo.packageName.equals(componentName.getPackageName()) || !activityInfo.name.equals(componentName.getClassName())) {
                        i++;
                    } else {
                        intent.setPackage(null);
                        intent.setComponent(componentName);
                        z = true;
                        break;
                    }
                }
                if (!z) {
                    throw new SecurityException("Attempt to launch activity without  category Intent.CATEGORY_LAUNCHER " + componentName);
                }
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                this.mActivityManagerInternal.startActivityAsUser(iApplicationThread, str, intent, bundle, userHandle.getIdentifier());
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        }

        public void showAppDetailsAsUser(IApplicationThread iApplicationThread, String str, ComponentName componentName, Rect rect, Bundle bundle, UserHandle userHandle) throws RemoteException {
            if (BenesseExtension.getDchaState() != 0 || !canAccessProfile(userHandle.getIdentifier(), "Cannot show app details")) {
                return;
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.fromParts(Settings.ATTR_PACKAGE, componentName.getPackageName(), null));
                intent.setFlags(268468224);
                intent.setSourceBounds(rect);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                this.mActivityManagerInternal.startActivityAsUser(iApplicationThread, str, intent, bundle, userHandle.getIdentifier());
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        }

        private boolean isEnabledProfileOf(UserHandle userHandle, UserHandle userHandle2, String str) {
            return this.mUserManagerInternal.isProfileAccessible(userHandle.getIdentifier(), userHandle2.getIdentifier(), str, false);
        }

        @VisibleForTesting
        void postToPackageMonitorHandler(Runnable runnable) {
            this.mCallbackHandler.post(runnable);
        }

        private class MyPackageMonitor extends PackageMonitor implements ShortcutServiceInternal.ShortcutChangeListener {
            private MyPackageMonitor() {
            }

            public void onPackageAdded(String str, int i) {
                UserHandle userHandle = new UserHandle(getChangingUserId());
                int iBeginBroadcast = LauncherAppsImpl.this.mListeners.beginBroadcast();
                for (int i2 = 0; i2 < iBeginBroadcast; i2++) {
                    try {
                        IOnAppsChangedListener iOnAppsChangedListener = (IOnAppsChangedListener) LauncherAppsImpl.this.mListeners.getBroadcastItem(i2);
                        if (LauncherAppsImpl.this.isEnabledProfileOf(((BroadcastCookie) LauncherAppsImpl.this.mListeners.getBroadcastCookie(i2)).user, userHandle, "onPackageAdded")) {
                            try {
                                iOnAppsChangedListener.onPackageAdded(userHandle, str);
                            } catch (RemoteException e) {
                                Slog.d(LauncherAppsImpl.TAG, "Callback failed ", e);
                            }
                        }
                    } catch (Throwable th) {
                        LauncherAppsImpl.this.mListeners.finishBroadcast();
                        throw th;
                    }
                }
                LauncherAppsImpl.this.mListeners.finishBroadcast();
                super.onPackageAdded(str, i);
            }

            public void onPackageRemoved(String str, int i) {
                UserHandle userHandle = new UserHandle(getChangingUserId());
                int iBeginBroadcast = LauncherAppsImpl.this.mListeners.beginBroadcast();
                for (int i2 = 0; i2 < iBeginBroadcast; i2++) {
                    try {
                        IOnAppsChangedListener iOnAppsChangedListener = (IOnAppsChangedListener) LauncherAppsImpl.this.mListeners.getBroadcastItem(i2);
                        if (LauncherAppsImpl.this.isEnabledProfileOf(((BroadcastCookie) LauncherAppsImpl.this.mListeners.getBroadcastCookie(i2)).user, userHandle, "onPackageRemoved")) {
                            try {
                                iOnAppsChangedListener.onPackageRemoved(userHandle, str);
                            } catch (RemoteException e) {
                                Slog.d(LauncherAppsImpl.TAG, "Callback failed ", e);
                            }
                        }
                    } catch (Throwable th) {
                        LauncherAppsImpl.this.mListeners.finishBroadcast();
                        throw th;
                    }
                }
                LauncherAppsImpl.this.mListeners.finishBroadcast();
                super.onPackageRemoved(str, i);
            }

            public void onPackageModified(String str) {
                UserHandle userHandle = new UserHandle(getChangingUserId());
                int iBeginBroadcast = LauncherAppsImpl.this.mListeners.beginBroadcast();
                for (int i = 0; i < iBeginBroadcast; i++) {
                    try {
                        IOnAppsChangedListener iOnAppsChangedListener = (IOnAppsChangedListener) LauncherAppsImpl.this.mListeners.getBroadcastItem(i);
                        if (LauncherAppsImpl.this.isEnabledProfileOf(((BroadcastCookie) LauncherAppsImpl.this.mListeners.getBroadcastCookie(i)).user, userHandle, "onPackageModified")) {
                            try {
                                iOnAppsChangedListener.onPackageChanged(userHandle, str);
                            } catch (RemoteException e) {
                                Slog.d(LauncherAppsImpl.TAG, "Callback failed ", e);
                            }
                        }
                    } catch (Throwable th) {
                        LauncherAppsImpl.this.mListeners.finishBroadcast();
                        throw th;
                    }
                }
                LauncherAppsImpl.this.mListeners.finishBroadcast();
                super.onPackageModified(str);
            }

            public void onPackagesAvailable(String[] strArr) {
                UserHandle userHandle = new UserHandle(getChangingUserId());
                int iBeginBroadcast = LauncherAppsImpl.this.mListeners.beginBroadcast();
                for (int i = 0; i < iBeginBroadcast; i++) {
                    try {
                        IOnAppsChangedListener iOnAppsChangedListener = (IOnAppsChangedListener) LauncherAppsImpl.this.mListeners.getBroadcastItem(i);
                        if (LauncherAppsImpl.this.isEnabledProfileOf(((BroadcastCookie) LauncherAppsImpl.this.mListeners.getBroadcastCookie(i)).user, userHandle, "onPackagesAvailable")) {
                            try {
                                iOnAppsChangedListener.onPackagesAvailable(userHandle, strArr, isReplacing());
                            } catch (RemoteException e) {
                                Slog.d(LauncherAppsImpl.TAG, "Callback failed ", e);
                            }
                        }
                    } catch (Throwable th) {
                        LauncherAppsImpl.this.mListeners.finishBroadcast();
                        throw th;
                    }
                }
                LauncherAppsImpl.this.mListeners.finishBroadcast();
                super.onPackagesAvailable(strArr);
            }

            public void onPackagesUnavailable(String[] strArr) {
                UserHandle userHandle = new UserHandle(getChangingUserId());
                int iBeginBroadcast = LauncherAppsImpl.this.mListeners.beginBroadcast();
                for (int i = 0; i < iBeginBroadcast; i++) {
                    try {
                        IOnAppsChangedListener iOnAppsChangedListener = (IOnAppsChangedListener) LauncherAppsImpl.this.mListeners.getBroadcastItem(i);
                        if (LauncherAppsImpl.this.isEnabledProfileOf(((BroadcastCookie) LauncherAppsImpl.this.mListeners.getBroadcastCookie(i)).user, userHandle, "onPackagesUnavailable")) {
                            try {
                                iOnAppsChangedListener.onPackagesUnavailable(userHandle, strArr, isReplacing());
                            } catch (RemoteException e) {
                                Slog.d(LauncherAppsImpl.TAG, "Callback failed ", e);
                            }
                        }
                    } catch (Throwable th) {
                        LauncherAppsImpl.this.mListeners.finishBroadcast();
                        throw th;
                    }
                }
                LauncherAppsImpl.this.mListeners.finishBroadcast();
                super.onPackagesUnavailable(strArr);
            }

            public void onPackagesSuspended(String[] strArr, Bundle bundle) {
                UserHandle userHandle = new UserHandle(getChangingUserId());
                int iBeginBroadcast = LauncherAppsImpl.this.mListeners.beginBroadcast();
                for (int i = 0; i < iBeginBroadcast; i++) {
                    try {
                        IOnAppsChangedListener iOnAppsChangedListener = (IOnAppsChangedListener) LauncherAppsImpl.this.mListeners.getBroadcastItem(i);
                        if (LauncherAppsImpl.this.isEnabledProfileOf(((BroadcastCookie) LauncherAppsImpl.this.mListeners.getBroadcastCookie(i)).user, userHandle, "onPackagesSuspended")) {
                            try {
                                iOnAppsChangedListener.onPackagesSuspended(userHandle, strArr, bundle);
                            } catch (RemoteException e) {
                                Slog.d(LauncherAppsImpl.TAG, "Callback failed ", e);
                            }
                        }
                    } catch (Throwable th) {
                        LauncherAppsImpl.this.mListeners.finishBroadcast();
                        throw th;
                    }
                }
                LauncherAppsImpl.this.mListeners.finishBroadcast();
                super.onPackagesSuspended(strArr, bundle);
            }

            public void onPackagesUnsuspended(String[] strArr) {
                UserHandle userHandle = new UserHandle(getChangingUserId());
                int iBeginBroadcast = LauncherAppsImpl.this.mListeners.beginBroadcast();
                for (int i = 0; i < iBeginBroadcast; i++) {
                    try {
                        IOnAppsChangedListener iOnAppsChangedListener = (IOnAppsChangedListener) LauncherAppsImpl.this.mListeners.getBroadcastItem(i);
                        if (LauncherAppsImpl.this.isEnabledProfileOf(((BroadcastCookie) LauncherAppsImpl.this.mListeners.getBroadcastCookie(i)).user, userHandle, "onPackagesUnsuspended")) {
                            try {
                                iOnAppsChangedListener.onPackagesUnsuspended(userHandle, strArr);
                            } catch (RemoteException e) {
                                Slog.d(LauncherAppsImpl.TAG, "Callback failed ", e);
                            }
                        }
                    } catch (Throwable th) {
                        LauncherAppsImpl.this.mListeners.finishBroadcast();
                        throw th;
                    }
                }
                LauncherAppsImpl.this.mListeners.finishBroadcast();
                super.onPackagesUnsuspended(strArr);
            }

            public void onShortcutChanged(final String str, final int i) {
                LauncherAppsImpl.this.postToPackageMonitorHandler(new Runnable() {
                    @Override
                    public final void run() {
                        this.f$0.onShortcutChangedInner(str, i);
                    }
                });
            }

            private void onShortcutChangedInner(String str, int i) {
                int iBeginBroadcast = LauncherAppsImpl.this.mListeners.beginBroadcast();
                try {
                    try {
                        UserHandle userHandleOf = UserHandle.of(i);
                        for (int i2 = 0; i2 < iBeginBroadcast; i2++) {
                            IOnAppsChangedListener iOnAppsChangedListener = (IOnAppsChangedListener) LauncherAppsImpl.this.mListeners.getBroadcastItem(i2);
                            BroadcastCookie broadcastCookie = (BroadcastCookie) LauncherAppsImpl.this.mListeners.getBroadcastCookie(i2);
                            if (LauncherAppsImpl.this.isEnabledProfileOf(broadcastCookie.user, userHandleOf, "onShortcutChanged")) {
                                int identifier = broadcastCookie.user.getIdentifier();
                                if (LauncherAppsImpl.this.mShortcutServiceInternal.hasShortcutHostPermission(identifier, broadcastCookie.packageName, broadcastCookie.callingPid, broadcastCookie.callingUid)) {
                                    try {
                                    } catch (RemoteException e) {
                                        e = e;
                                    }
                                    try {
                                        iOnAppsChangedListener.onShortcutChanged(userHandleOf, str, new ParceledListSlice(LauncherAppsImpl.this.mShortcutServiceInternal.getShortcuts(identifier, broadcastCookie.packageName, 0L, str, (List) null, (ComponentName) null, 1039, i, broadcastCookie.callingPid, broadcastCookie.callingUid)));
                                    } catch (RemoteException e2) {
                                        e = e2;
                                        Slog.d(LauncherAppsImpl.TAG, "Callback failed ", e);
                                    }
                                }
                            }
                        }
                    } catch (RuntimeException e3) {
                        Log.w(LauncherAppsImpl.TAG, e3.getMessage(), e3);
                    }
                } finally {
                    LauncherAppsImpl.this.mListeners.finishBroadcast();
                }
            }
        }

        class PackageCallbackList<T extends IInterface> extends RemoteCallbackList<T> {
            PackageCallbackList() {
            }

            @Override
            public void onCallbackDied(T t, Object obj) {
                LauncherAppsImpl.this.checkCallbackCount();
            }
        }
    }
}
