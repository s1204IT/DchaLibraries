package android.content.pm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IOnAppsChangedListener;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LauncherApps {
    static final boolean DEBUG = false;
    static final String TAG = "LauncherApps";
    private Context mContext;
    private PackageManager mPm;
    private ILauncherApps mService;
    private List<CallbackMessageHandler> mCallbacks = new ArrayList();
    private IOnAppsChangedListener.Stub mAppsChangedListener = new IOnAppsChangedListener.Stub() {
        @Override
        public void onPackageRemoved(UserHandle user, String packageName) throws RemoteException {
            synchronized (LauncherApps.this) {
                for (CallbackMessageHandler callback : LauncherApps.this.mCallbacks) {
                    callback.postOnPackageRemoved(packageName, user);
                }
            }
        }

        @Override
        public void onPackageChanged(UserHandle user, String packageName) throws RemoteException {
            synchronized (LauncherApps.this) {
                for (CallbackMessageHandler callback : LauncherApps.this.mCallbacks) {
                    callback.postOnPackageChanged(packageName, user);
                }
            }
        }

        @Override
        public void onPackageAdded(UserHandle user, String packageName) throws RemoteException {
            synchronized (LauncherApps.this) {
                for (CallbackMessageHandler callback : LauncherApps.this.mCallbacks) {
                    callback.postOnPackageAdded(packageName, user);
                }
            }
        }

        @Override
        public void onPackagesAvailable(UserHandle user, String[] packageNames, boolean replacing) throws RemoteException {
            synchronized (LauncherApps.this) {
                for (CallbackMessageHandler callback : LauncherApps.this.mCallbacks) {
                    callback.postOnPackagesAvailable(packageNames, user, replacing);
                }
            }
        }

        @Override
        public void onPackagesUnavailable(UserHandle user, String[] packageNames, boolean replacing) throws RemoteException {
            synchronized (LauncherApps.this) {
                for (CallbackMessageHandler callback : LauncherApps.this.mCallbacks) {
                    callback.postOnPackagesUnavailable(packageNames, user, replacing);
                }
            }
        }
    };

    public static abstract class Callback {
        public abstract void onPackageAdded(String str, UserHandle userHandle);

        public abstract void onPackageChanged(String str, UserHandle userHandle);

        public abstract void onPackageRemoved(String str, UserHandle userHandle);

        public abstract void onPackagesAvailable(String[] strArr, UserHandle userHandle, boolean z);

        public abstract void onPackagesUnavailable(String[] strArr, UserHandle userHandle, boolean z);
    }

    public LauncherApps(Context context, ILauncherApps service) {
        this.mContext = context;
        this.mService = service;
        this.mPm = context.getPackageManager();
    }

    public List<LauncherActivityInfo> getActivityList(String packageName, UserHandle user) {
        List<ResolveInfo> activities = null;
        try {
            activities = this.mService.getLauncherActivities(packageName, user);
        } catch (RemoteException e) {
        }
        if (activities == null) {
            return Collections.EMPTY_LIST;
        }
        ArrayList<LauncherActivityInfo> lais = new ArrayList<>();
        int count = activities.size();
        for (int i = 0; i < count; i++) {
            ResolveInfo ri = activities.get(i);
            long firstInstallTime = 0;
            try {
                firstInstallTime = this.mPm.getPackageInfo(ri.activityInfo.packageName, 8192).firstInstallTime;
            } catch (PackageManager.NameNotFoundException e2) {
            }
            LauncherActivityInfo lai = new LauncherActivityInfo(this.mContext, ri, user, firstInstallTime);
            lais.add(lai);
        }
        return lais;
    }

    static ComponentName getComponentName(ResolveInfo ri) {
        return new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
    }

    public LauncherActivityInfo resolveActivity(Intent intent, UserHandle user) {
        try {
            ResolveInfo ri = this.mService.resolveActivity(intent, user);
            if (ri != null) {
                long firstInstallTime = 0;
                try {
                    firstInstallTime = this.mPm.getPackageInfo(ri.activityInfo.packageName, 8192).firstInstallTime;
                } catch (PackageManager.NameNotFoundException e) {
                }
                return new LauncherActivityInfo(this.mContext, ri, user, firstInstallTime);
            }
            return null;
        } catch (RemoteException e2) {
            throw new RuntimeException("Failed to call LauncherAppsService");
        }
    }

    public void startMainActivity(ComponentName component, UserHandle user, Rect sourceBounds, Bundle opts) {
        try {
            this.mService.startActivityAsUser(component, sourceBounds, opts, user);
        } catch (RemoteException e) {
        }
    }

    public void startAppDetailsActivity(ComponentName component, UserHandle user, Rect sourceBounds, Bundle opts) {
        try {
            this.mService.showAppDetailsAsUser(component, sourceBounds, opts, user);
        } catch (RemoteException e) {
        }
    }

    public boolean isPackageEnabled(String packageName, UserHandle user) {
        try {
            return this.mService.isPackageEnabled(packageName, user);
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to call LauncherAppsService");
        }
    }

    public boolean isActivityEnabled(ComponentName component, UserHandle user) {
        try {
            return this.mService.isActivityEnabled(component, user);
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to call LauncherAppsService");
        }
    }

    public void registerCallback(Callback callback) {
        registerCallback(callback, null);
    }

    public void registerCallback(Callback callback, Handler handler) {
        synchronized (this) {
            if (callback != null) {
                if (!this.mCallbacks.contains(callback)) {
                    boolean addedFirstCallback = this.mCallbacks.size() == 0;
                    addCallbackLocked(callback, handler);
                    if (addedFirstCallback) {
                        try {
                            this.mService.addOnAppsChangedListener(this.mAppsChangedListener);
                        } catch (RemoteException e) {
                        }
                    }
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
                }
            }
        }
    }

    private void removeCallbackLocked(Callback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Callback cannot be null");
        }
        int size = this.mCallbacks.size();
        for (int i = 0; i < size; i++) {
            if (this.mCallbacks.get(i).mCallback == callback) {
                this.mCallbacks.remove(i);
                return;
            }
        }
    }

    private void addCallbackLocked(Callback callback, Handler handler) {
        removeCallbackLocked(callback);
        if (handler == null) {
            handler = new Handler();
        }
        CallbackMessageHandler toAdd = new CallbackMessageHandler(handler.getLooper(), callback);
        this.mCallbacks.add(toAdd);
    }

    private static class CallbackMessageHandler extends Handler {
        private static final int MSG_ADDED = 1;
        private static final int MSG_AVAILABLE = 4;
        private static final int MSG_CHANGED = 3;
        private static final int MSG_REMOVED = 2;
        private static final int MSG_UNAVAILABLE = 5;
        private Callback mCallback;

        private static class CallbackInfo {
            String packageName;
            String[] packageNames;
            boolean replacing;
            UserHandle user;

            private CallbackInfo() {
            }
        }

        public CallbackMessageHandler(Looper looper, Callback callback) {
            super(looper, null, true);
            this.mCallback = callback;
        }

        @Override
        public void handleMessage(Message msg) {
            if (this.mCallback != null && (msg.obj instanceof CallbackInfo)) {
                CallbackInfo info = (CallbackInfo) msg.obj;
                switch (msg.what) {
                    case 1:
                        this.mCallback.onPackageAdded(info.packageName, info.user);
                        break;
                    case 2:
                        this.mCallback.onPackageRemoved(info.packageName, info.user);
                        break;
                    case 3:
                        this.mCallback.onPackageChanged(info.packageName, info.user);
                        break;
                    case 4:
                        this.mCallback.onPackagesAvailable(info.packageNames, info.user, info.replacing);
                        break;
                    case 5:
                        this.mCallback.onPackagesUnavailable(info.packageNames, info.user, info.replacing);
                        break;
                }
            }
        }

        public void postOnPackageAdded(String packageName, UserHandle user) {
            CallbackInfo info = new CallbackInfo();
            info.packageName = packageName;
            info.user = user;
            obtainMessage(1, info).sendToTarget();
        }

        public void postOnPackageRemoved(String packageName, UserHandle user) {
            CallbackInfo info = new CallbackInfo();
            info.packageName = packageName;
            info.user = user;
            obtainMessage(2, info).sendToTarget();
        }

        public void postOnPackageChanged(String packageName, UserHandle user) {
            CallbackInfo info = new CallbackInfo();
            info.packageName = packageName;
            info.user = user;
            obtainMessage(3, info).sendToTarget();
        }

        public void postOnPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing) {
            CallbackInfo info = new CallbackInfo();
            info.packageNames = packageNames;
            info.replacing = replacing;
            info.user = user;
            obtainMessage(4, info).sendToTarget();
        }

        public void postOnPackagesUnavailable(String[] packageNames, UserHandle user, boolean replacing) {
            CallbackInfo info = new CallbackInfo();
            info.packageNames = packageNames;
            info.replacing = replacing;
            info.user = user;
            obtainMessage(5, info).sendToTarget();
        }
    }
}
