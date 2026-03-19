package android.content.pm;

import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ILauncherApps;
import android.content.pm.IOnAppsChangedListener;
import android.content.pm.IPinItemRequest;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import com.android.internal.util.Preconditions;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class LauncherApps {
    public static final String ACTION_CONFIRM_PIN_APPWIDGET = "android.content.pm.action.CONFIRM_PIN_APPWIDGET";
    public static final String ACTION_CONFIRM_PIN_SHORTCUT = "android.content.pm.action.CONFIRM_PIN_SHORTCUT";
    static final boolean DEBUG = false;
    public static final String EXTRA_PIN_ITEM_REQUEST = "android.content.pm.extra.PIN_ITEM_REQUEST";
    static final String TAG = "LauncherApps";
    private IOnAppsChangedListener.Stub mAppsChangedListener;
    private List<CallbackMessageHandler> mCallbacks;
    private final Context mContext;
    private final PackageManager mPm;
    private final ILauncherApps mService;
    private final UserManager mUserManager;

    public static abstract class Callback {
        public abstract void onPackageAdded(String str, UserHandle userHandle);

        public abstract void onPackageChanged(String str, UserHandle userHandle);

        public abstract void onPackageRemoved(String str, UserHandle userHandle);

        public abstract void onPackagesAvailable(String[] strArr, UserHandle userHandle, boolean z);

        public abstract void onPackagesUnavailable(String[] strArr, UserHandle userHandle, boolean z);

        public void onPackagesSuspended(String[] strArr, UserHandle userHandle) {
        }

        public void onPackagesSuspended(String[] strArr, UserHandle userHandle, Bundle bundle) {
            onPackagesSuspended(strArr, userHandle);
        }

        public void onPackagesUnsuspended(String[] strArr, UserHandle userHandle) {
        }

        public void onShortcutsChanged(String str, List<ShortcutInfo> list, UserHandle userHandle) {
        }
    }

    public static class ShortcutQuery {

        @Deprecated
        public static final int FLAG_GET_ALL_KINDS = 11;

        @Deprecated
        public static final int FLAG_GET_DYNAMIC = 1;
        public static final int FLAG_GET_KEY_FIELDS_ONLY = 4;

        @Deprecated
        public static final int FLAG_GET_MANIFEST = 8;

        @Deprecated
        public static final int FLAG_GET_PINNED = 2;
        public static final int FLAG_MATCH_ALL_KINDS = 11;
        public static final int FLAG_MATCH_ALL_KINDS_WITH_ALL_PINNED = 1035;
        public static final int FLAG_MATCH_DYNAMIC = 1;
        public static final int FLAG_MATCH_MANIFEST = 8;
        public static final int FLAG_MATCH_PINNED = 2;
        public static final int FLAG_MATCH_PINNED_BY_ANY_LAUNCHER = 1024;
        ComponentName mActivity;
        long mChangedSince;
        String mPackage;
        int mQueryFlags;
        List<String> mShortcutIds;

        @Retention(RetentionPolicy.SOURCE)
        public @interface QueryFlags {
        }

        public ShortcutQuery setChangedSince(long j) {
            this.mChangedSince = j;
            return this;
        }

        public ShortcutQuery setPackage(String str) {
            this.mPackage = str;
            return this;
        }

        public ShortcutQuery setShortcutIds(List<String> list) {
            this.mShortcutIds = list;
            return this;
        }

        public ShortcutQuery setActivity(ComponentName componentName) {
            this.mActivity = componentName;
            return this;
        }

        public ShortcutQuery setQueryFlags(int i) {
            this.mQueryFlags = i;
            return this;
        }
    }

    public LauncherApps(Context context, ILauncherApps iLauncherApps) {
        this.mCallbacks = new ArrayList();
        this.mAppsChangedListener = new IOnAppsChangedListener.Stub() {
            @Override
            public void onPackageRemoved(UserHandle userHandle, String str) throws RemoteException {
                synchronized (LauncherApps.this) {
                    Iterator it = LauncherApps.this.mCallbacks.iterator();
                    while (it.hasNext()) {
                        ((CallbackMessageHandler) it.next()).postOnPackageRemoved(str, userHandle);
                    }
                }
            }

            @Override
            public void onPackageChanged(UserHandle userHandle, String str) throws RemoteException {
                synchronized (LauncherApps.this) {
                    Iterator it = LauncherApps.this.mCallbacks.iterator();
                    while (it.hasNext()) {
                        ((CallbackMessageHandler) it.next()).postOnPackageChanged(str, userHandle);
                    }
                }
            }

            @Override
            public void onPackageAdded(UserHandle userHandle, String str) throws RemoteException {
                synchronized (LauncherApps.this) {
                    Iterator it = LauncherApps.this.mCallbacks.iterator();
                    while (it.hasNext()) {
                        ((CallbackMessageHandler) it.next()).postOnPackageAdded(str, userHandle);
                    }
                }
            }

            @Override
            public void onPackagesAvailable(UserHandle userHandle, String[] strArr, boolean z) throws RemoteException {
                synchronized (LauncherApps.this) {
                    Iterator it = LauncherApps.this.mCallbacks.iterator();
                    while (it.hasNext()) {
                        ((CallbackMessageHandler) it.next()).postOnPackagesAvailable(strArr, userHandle, z);
                    }
                }
            }

            @Override
            public void onPackagesUnavailable(UserHandle userHandle, String[] strArr, boolean z) throws RemoteException {
                synchronized (LauncherApps.this) {
                    Iterator it = LauncherApps.this.mCallbacks.iterator();
                    while (it.hasNext()) {
                        ((CallbackMessageHandler) it.next()).postOnPackagesUnavailable(strArr, userHandle, z);
                    }
                }
            }

            @Override
            public void onPackagesSuspended(UserHandle userHandle, String[] strArr, Bundle bundle) throws RemoteException {
                synchronized (LauncherApps.this) {
                    Iterator it = LauncherApps.this.mCallbacks.iterator();
                    while (it.hasNext()) {
                        ((CallbackMessageHandler) it.next()).postOnPackagesSuspended(strArr, bundle, userHandle);
                    }
                }
            }

            @Override
            public void onPackagesUnsuspended(UserHandle userHandle, String[] strArr) throws RemoteException {
                synchronized (LauncherApps.this) {
                    Iterator it = LauncherApps.this.mCallbacks.iterator();
                    while (it.hasNext()) {
                        ((CallbackMessageHandler) it.next()).postOnPackagesUnsuspended(strArr, userHandle);
                    }
                }
            }

            @Override
            public void onShortcutChanged(UserHandle userHandle, String str, ParceledListSlice parceledListSlice) {
                List<ShortcutInfo> list = parceledListSlice.getList();
                synchronized (LauncherApps.this) {
                    Iterator it = LauncherApps.this.mCallbacks.iterator();
                    while (it.hasNext()) {
                        ((CallbackMessageHandler) it.next()).postOnShortcutChanged(str, userHandle, list);
                    }
                }
            }
        };
        this.mContext = context;
        this.mService = iLauncherApps;
        this.mPm = context.getPackageManager();
        this.mUserManager = (UserManager) context.getSystemService(UserManager.class);
    }

    public LauncherApps(Context context) {
        this(context, ILauncherApps.Stub.asInterface(ServiceManager.getService(Context.LAUNCHER_APPS_SERVICE)));
    }

    private void logErrorForInvalidProfileAccess(UserHandle userHandle) {
        if (UserHandle.myUserId() != userHandle.getIdentifier() && this.mUserManager.isManagedProfile()) {
            Log.w(TAG, "Accessing other profiles/users from managed profile is no longer allowed.");
        }
    }

    public List<UserHandle> getProfiles() {
        if (this.mUserManager.isManagedProfile()) {
            ArrayList arrayList = new ArrayList(1);
            arrayList.add(Process.myUserHandle());
            return arrayList;
        }
        return this.mUserManager.getUserProfiles();
    }

    public List<LauncherActivityInfo> getActivityList(String str, UserHandle userHandle) {
        logErrorForInvalidProfileAccess(userHandle);
        try {
            return convertToActivityList(this.mService.getLauncherActivities(this.mContext.getPackageName(), str, userHandle), userHandle);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public LauncherActivityInfo resolveActivity(Intent intent, UserHandle userHandle) {
        logErrorForInvalidProfileAccess(userHandle);
        try {
            ActivityInfo activityInfoResolveActivity = this.mService.resolveActivity(this.mContext.getPackageName(), intent.getComponent(), userHandle);
            if (activityInfoResolveActivity != null) {
                return new LauncherActivityInfo(this.mContext, activityInfoResolveActivity, userHandle);
            }
            return null;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void startMainActivity(ComponentName componentName, UserHandle userHandle, Rect rect, Bundle bundle) {
        logErrorForInvalidProfileAccess(userHandle);
        try {
            this.mService.startActivityAsUser(this.mContext.getIApplicationThread(), this.mContext.getPackageName(), componentName, rect, bundle, userHandle);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void startAppDetailsActivity(ComponentName componentName, UserHandle userHandle, Rect rect, Bundle bundle) {
        logErrorForInvalidProfileAccess(userHandle);
        try {
            this.mService.showAppDetailsAsUser(this.mContext.getIApplicationThread(), this.mContext.getPackageName(), componentName, rect, bundle, userHandle);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<LauncherActivityInfo> getShortcutConfigActivityList(String str, UserHandle userHandle) {
        logErrorForInvalidProfileAccess(userHandle);
        try {
            return convertToActivityList(this.mService.getShortcutConfigActivities(this.mContext.getPackageName(), str, userHandle), userHandle);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private List<LauncherActivityInfo> convertToActivityList(ParceledListSlice<ResolveInfo> parceledListSlice, UserHandle userHandle) {
        if (parceledListSlice == null) {
            return Collections.EMPTY_LIST;
        }
        ArrayList arrayList = new ArrayList();
        Iterator it = parceledListSlice.getList().iterator();
        while (it.hasNext()) {
            arrayList.add(new LauncherActivityInfo(this.mContext, ((ResolveInfo) it.next()).activityInfo, userHandle));
        }
        return arrayList;
    }

    public IntentSender getShortcutConfigActivityIntent(LauncherActivityInfo launcherActivityInfo) {
        try {
            return this.mService.getShortcutConfigActivityIntent(this.mContext.getPackageName(), launcherActivityInfo.getComponentName(), launcherActivityInfo.getUser());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isPackageEnabled(String str, UserHandle userHandle) {
        logErrorForInvalidProfileAccess(userHandle);
        try {
            return this.mService.isPackageEnabled(this.mContext.getPackageName(), str, userHandle);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Bundle getSuspendedPackageLauncherExtras(String str, UserHandle userHandle) {
        logErrorForInvalidProfileAccess(userHandle);
        try {
            return this.mService.getSuspendedPackageLauncherExtras(str, userHandle);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ApplicationInfo getApplicationInfo(String str, int i, UserHandle userHandle) throws PackageManager.NameNotFoundException {
        Preconditions.checkNotNull(str, "packageName");
        Preconditions.checkNotNull(userHandle, "user");
        logErrorForInvalidProfileAccess(userHandle);
        try {
            ApplicationInfo applicationInfo = this.mService.getApplicationInfo(this.mContext.getPackageName(), str, i, userHandle);
            if (applicationInfo == null) {
                throw new PackageManager.NameNotFoundException("Package " + str + " not found for user " + userHandle.getIdentifier());
            }
            return applicationInfo;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isActivityEnabled(ComponentName componentName, UserHandle userHandle) {
        logErrorForInvalidProfileAccess(userHandle);
        try {
            return this.mService.isActivityEnabled(this.mContext.getPackageName(), componentName, userHandle);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean hasShortcutHostPermission() {
        try {
            return this.mService.hasShortcutHostPermission(this.mContext.getPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private List<ShortcutInfo> maybeUpdateDisabledMessage(List<ShortcutInfo> list) {
        if (list == null) {
            return null;
        }
        for (int size = list.size() - 1; size >= 0; size--) {
            ShortcutInfo shortcutInfo = list.get(size);
            String disabledReasonForRestoreIssue = ShortcutInfo.getDisabledReasonForRestoreIssue(this.mContext, shortcutInfo.getDisabledReason());
            if (disabledReasonForRestoreIssue != null) {
                shortcutInfo.setDisabledMessage(disabledReasonForRestoreIssue);
            }
        }
        return list;
    }

    public List<ShortcutInfo> getShortcuts(ShortcutQuery shortcutQuery, UserHandle userHandle) {
        logErrorForInvalidProfileAccess(userHandle);
        try {
            return maybeUpdateDisabledMessage(this.mService.getShortcuts(this.mContext.getPackageName(), shortcutQuery.mChangedSince, shortcutQuery.mPackage, shortcutQuery.mShortcutIds, shortcutQuery.mActivity, shortcutQuery.mQueryFlags, userHandle).getList());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public List<ShortcutInfo> getShortcutInfo(String str, List<String> list, UserHandle userHandle) {
        ShortcutQuery shortcutQuery = new ShortcutQuery();
        shortcutQuery.setPackage(str);
        shortcutQuery.setShortcutIds(list);
        shortcutQuery.setQueryFlags(11);
        return getShortcuts(shortcutQuery, userHandle);
    }

    public void pinShortcuts(String str, List<String> list, UserHandle userHandle) {
        logErrorForInvalidProfileAccess(userHandle);
        try {
            this.mService.pinShortcuts(this.mContext.getPackageName(), str, list, userHandle);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public int getShortcutIconResId(ShortcutInfo shortcutInfo) {
        return shortcutInfo.getIconResourceId();
    }

    @Deprecated
    public int getShortcutIconResId(String str, String str2, UserHandle userHandle) {
        ShortcutQuery shortcutQuery = new ShortcutQuery();
        shortcutQuery.setPackage(str);
        shortcutQuery.setShortcutIds(Arrays.asList(str2));
        shortcutQuery.setQueryFlags(11);
        List<ShortcutInfo> shortcuts = getShortcuts(shortcutQuery, userHandle);
        if (shortcuts.size() > 0) {
            return shortcuts.get(0).getIconResourceId();
        }
        return 0;
    }

    public ParcelFileDescriptor getShortcutIconFd(ShortcutInfo shortcutInfo) {
        return getShortcutIconFd(shortcutInfo.getPackage(), shortcutInfo.getId(), shortcutInfo.getUserId());
    }

    public ParcelFileDescriptor getShortcutIconFd(String str, String str2, UserHandle userHandle) {
        return getShortcutIconFd(str, str2, userHandle.getIdentifier());
    }

    private ParcelFileDescriptor getShortcutIconFd(String str, String str2, int i) {
        try {
            return this.mService.getShortcutIconFd(this.mContext.getPackageName(), str, str2, i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Drawable getShortcutIconDrawable(ShortcutInfo shortcutInfo, int i) {
        if (!shortcutInfo.hasIconFile()) {
            if (shortcutInfo.hasIconResource()) {
                return loadDrawableResourceFromPackage(shortcutInfo.getPackage(), shortcutInfo.getIconResourceId(), shortcutInfo.getUserHandle(), i);
            }
            if (shortcutInfo.getIcon() == null) {
                return null;
            }
            Icon icon = shortcutInfo.getIcon();
            int type = icon.getType();
            if (type != 5) {
                switch (type) {
                    case 1:
                        break;
                    case 2:
                        return loadDrawableResourceFromPackage(shortcutInfo.getPackage(), icon.getResId(), shortcutInfo.getUserHandle(), i);
                    default:
                        return null;
                }
            }
            return icon.loadDrawable(this.mContext);
        }
        ParcelFileDescriptor shortcutIconFd = getShortcutIconFd(shortcutInfo);
        if (shortcutIconFd == null) {
            return null;
        }
        try {
            Bitmap bitmapDecodeFileDescriptor = BitmapFactory.decodeFileDescriptor(shortcutIconFd.getFileDescriptor());
            if (bitmapDecodeFileDescriptor == null) {
                try {
                    shortcutIconFd.close();
                } catch (IOException e) {
                }
                return null;
            }
            BitmapDrawable bitmapDrawable = new BitmapDrawable(this.mContext.getResources(), bitmapDecodeFileDescriptor);
            if (shortcutInfo.hasAdaptiveBitmap()) {
                return new AdaptiveIconDrawable((Drawable) null, bitmapDrawable);
            }
            try {
                shortcutIconFd.close();
            } catch (IOException e2) {
            }
            return bitmapDrawable;
        } finally {
            try {
                shortcutIconFd.close();
            } catch (IOException e3) {
            }
        }
    }

    private Drawable loadDrawableResourceFromPackage(String str, int i, UserHandle userHandle, int i2) {
        if (i == 0) {
            return null;
        }
        try {
            return this.mContext.getPackageManager().getResourcesForApplication(getApplicationInfo(str, 0, userHandle)).getDrawableForDensity(i, i2);
        } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e) {
            return null;
        }
    }

    public Drawable getShortcutBadgedIconDrawable(ShortcutInfo shortcutInfo, int i) {
        Drawable shortcutIconDrawable = getShortcutIconDrawable(shortcutInfo, i);
        if (shortcutIconDrawable == null) {
            return null;
        }
        return this.mContext.getPackageManager().getUserBadgedIcon(shortcutIconDrawable, shortcutInfo.getUserHandle());
    }

    public void startShortcut(String str, String str2, Rect rect, Bundle bundle, UserHandle userHandle) {
        logErrorForInvalidProfileAccess(userHandle);
        startShortcut(str, str2, rect, bundle, userHandle.getIdentifier());
    }

    public void startShortcut(ShortcutInfo shortcutInfo, Rect rect, Bundle bundle) {
        startShortcut(shortcutInfo.getPackage(), shortcutInfo.getId(), rect, bundle, shortcutInfo.getUserId());
    }

    private void startShortcut(String str, String str2, Rect rect, Bundle bundle, int i) {
        try {
            if (!this.mService.startShortcut(this.mContext.getPackageName(), str, str2, rect, bundle, i)) {
                throw new ActivityNotFoundException("Shortcut could not be started");
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void registerCallback(Callback callback) {
        registerCallback(callback, null);
    }

    public void registerCallback(Callback callback, Handler handler) {
        synchronized (this) {
            if (callback != null) {
                try {
                    if (findCallbackLocked(callback) < 0) {
                        boolean z = this.mCallbacks.size() == 0;
                        addCallbackLocked(callback, handler);
                        if (z) {
                            try {
                                this.mService.addOnAppsChangedListener(this.mContext.getPackageName(), this.mAppsChangedListener);
                            } catch (RemoteException e) {
                                throw e.rethrowFromSystemServer();
                            }
                        }
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
    }

    public void unregisterCallback(Callback callback) {
        synchronized (this) {
            removeCallbackLocked(callback);
            if (this.mCallbacks.size() == 0) {
                try {
                    this.mService.removeOnAppsChangedListener(this.mAppsChangedListener);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
        }
    }

    private int findCallbackLocked(Callback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Callback cannot be null");
        }
        int size = this.mCallbacks.size();
        for (int i = 0; i < size; i++) {
            if (this.mCallbacks.get(i).mCallback == callback) {
                return i;
            }
        }
        return -1;
    }

    private void removeCallbackLocked(Callback callback) {
        int iFindCallbackLocked = findCallbackLocked(callback);
        if (iFindCallbackLocked >= 0) {
            this.mCallbacks.remove(iFindCallbackLocked);
        }
    }

    private void addCallbackLocked(Callback callback, Handler handler) {
        removeCallbackLocked(callback);
        if (handler == null) {
            handler = new Handler();
        }
        this.mCallbacks.add(new CallbackMessageHandler(handler.getLooper(), callback));
    }

    private static class CallbackMessageHandler extends Handler {
        private static final int MSG_ADDED = 1;
        private static final int MSG_AVAILABLE = 4;
        private static final int MSG_CHANGED = 3;
        private static final int MSG_REMOVED = 2;
        private static final int MSG_SHORTCUT_CHANGED = 8;
        private static final int MSG_SUSPENDED = 6;
        private static final int MSG_UNAVAILABLE = 5;
        private static final int MSG_UNSUSPENDED = 7;
        private Callback mCallback;

        private static class CallbackInfo {
            Bundle launcherExtras;
            String packageName;
            String[] packageNames;
            boolean replacing;
            List<ShortcutInfo> shortcuts;
            UserHandle user;

            private CallbackInfo() {
            }
        }

        public CallbackMessageHandler(Looper looper, Callback callback) {
            super(looper, null, true);
            this.mCallback = callback;
        }

        @Override
        public void handleMessage(Message message) {
            if (this.mCallback == null || !(message.obj instanceof CallbackInfo)) {
                return;
            }
            CallbackInfo callbackInfo = (CallbackInfo) message.obj;
            switch (message.what) {
                case 1:
                    this.mCallback.onPackageAdded(callbackInfo.packageName, callbackInfo.user);
                    break;
                case 2:
                    this.mCallback.onPackageRemoved(callbackInfo.packageName, callbackInfo.user);
                    break;
                case 3:
                    this.mCallback.onPackageChanged(callbackInfo.packageName, callbackInfo.user);
                    break;
                case 4:
                    this.mCallback.onPackagesAvailable(callbackInfo.packageNames, callbackInfo.user, callbackInfo.replacing);
                    break;
                case 5:
                    this.mCallback.onPackagesUnavailable(callbackInfo.packageNames, callbackInfo.user, callbackInfo.replacing);
                    break;
                case 6:
                    this.mCallback.onPackagesSuspended(callbackInfo.packageNames, callbackInfo.user, callbackInfo.launcherExtras);
                    break;
                case 7:
                    this.mCallback.onPackagesUnsuspended(callbackInfo.packageNames, callbackInfo.user);
                    break;
                case 8:
                    this.mCallback.onShortcutsChanged(callbackInfo.packageName, callbackInfo.shortcuts, callbackInfo.user);
                    break;
            }
        }

        public void postOnPackageAdded(String str, UserHandle userHandle) {
            CallbackInfo callbackInfo = new CallbackInfo();
            callbackInfo.packageName = str;
            callbackInfo.user = userHandle;
            obtainMessage(1, callbackInfo).sendToTarget();
        }

        public void postOnPackageRemoved(String str, UserHandle userHandle) {
            CallbackInfo callbackInfo = new CallbackInfo();
            callbackInfo.packageName = str;
            callbackInfo.user = userHandle;
            obtainMessage(2, callbackInfo).sendToTarget();
        }

        public void postOnPackageChanged(String str, UserHandle userHandle) {
            CallbackInfo callbackInfo = new CallbackInfo();
            callbackInfo.packageName = str;
            callbackInfo.user = userHandle;
            obtainMessage(3, callbackInfo).sendToTarget();
        }

        public void postOnPackagesAvailable(String[] strArr, UserHandle userHandle, boolean z) {
            CallbackInfo callbackInfo = new CallbackInfo();
            callbackInfo.packageNames = strArr;
            callbackInfo.replacing = z;
            callbackInfo.user = userHandle;
            obtainMessage(4, callbackInfo).sendToTarget();
        }

        public void postOnPackagesUnavailable(String[] strArr, UserHandle userHandle, boolean z) {
            CallbackInfo callbackInfo = new CallbackInfo();
            callbackInfo.packageNames = strArr;
            callbackInfo.replacing = z;
            callbackInfo.user = userHandle;
            obtainMessage(5, callbackInfo).sendToTarget();
        }

        public void postOnPackagesSuspended(String[] strArr, Bundle bundle, UserHandle userHandle) {
            CallbackInfo callbackInfo = new CallbackInfo();
            callbackInfo.packageNames = strArr;
            callbackInfo.user = userHandle;
            callbackInfo.launcherExtras = bundle;
            obtainMessage(6, callbackInfo).sendToTarget();
        }

        public void postOnPackagesUnsuspended(String[] strArr, UserHandle userHandle) {
            CallbackInfo callbackInfo = new CallbackInfo();
            callbackInfo.packageNames = strArr;
            callbackInfo.user = userHandle;
            obtainMessage(7, callbackInfo).sendToTarget();
        }

        public void postOnShortcutChanged(String str, UserHandle userHandle, List<ShortcutInfo> list) {
            CallbackInfo callbackInfo = new CallbackInfo();
            callbackInfo.packageName = str;
            callbackInfo.user = userHandle;
            callbackInfo.shortcuts = list;
            obtainMessage(8, callbackInfo).sendToTarget();
        }
    }

    public PinItemRequest getPinItemRequest(Intent intent) {
        return (PinItemRequest) intent.getParcelableExtra(EXTRA_PIN_ITEM_REQUEST);
    }

    public static final class PinItemRequest implements Parcelable {
        public static final Parcelable.Creator<PinItemRequest> CREATOR = new Parcelable.Creator<PinItemRequest>() {
            @Override
            public PinItemRequest createFromParcel(Parcel parcel) {
                return new PinItemRequest(parcel);
            }

            @Override
            public PinItemRequest[] newArray(int i) {
                return new PinItemRequest[i];
            }
        };
        public static final int REQUEST_TYPE_APPWIDGET = 2;
        public static final int REQUEST_TYPE_SHORTCUT = 1;
        private final IPinItemRequest mInner;
        private final int mRequestType;

        @Retention(RetentionPolicy.SOURCE)
        public @interface RequestType {
        }

        public PinItemRequest(IPinItemRequest iPinItemRequest, int i) {
            this.mInner = iPinItemRequest;
            this.mRequestType = i;
        }

        public int getRequestType() {
            return this.mRequestType;
        }

        public ShortcutInfo getShortcutInfo() {
            try {
                return this.mInner.getShortcutInfo();
            } catch (RemoteException e) {
                throw e.rethrowAsRuntimeException();
            }
        }

        public AppWidgetProviderInfo getAppWidgetProviderInfo(Context context) {
            try {
                AppWidgetProviderInfo appWidgetProviderInfo = this.mInner.getAppWidgetProviderInfo();
                if (appWidgetProviderInfo == null) {
                    return null;
                }
                appWidgetProviderInfo.updateDimensions(context.getResources().getDisplayMetrics());
                return appWidgetProviderInfo;
            } catch (RemoteException e) {
                throw e.rethrowAsRuntimeException();
            }
        }

        public Bundle getExtras() {
            try {
                return this.mInner.getExtras();
            } catch (RemoteException e) {
                throw e.rethrowAsRuntimeException();
            }
        }

        public boolean isValid() {
            try {
                return this.mInner.isValid();
            } catch (RemoteException e) {
                return false;
            }
        }

        public boolean accept(Bundle bundle) {
            try {
                return this.mInner.accept(bundle);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        public boolean accept() {
            return accept(null);
        }

        private PinItemRequest(Parcel parcel) {
            getClass().getClassLoader();
            this.mRequestType = parcel.readInt();
            this.mInner = IPinItemRequest.Stub.asInterface(parcel.readStrongBinder());
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.mRequestType);
            parcel.writeStrongBinder(this.mInner.asBinder());
        }

        @Override
        public int describeContents() {
            return 0;
        }
    }
}
